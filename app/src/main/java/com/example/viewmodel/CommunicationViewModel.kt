package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.friendlyFirestoreMessage
import com.example.data.CommunicationRepository
import com.example.data.UserRepository
import com.example.model.CallHistoryEntry
import com.example.model.CallSession
import com.example.model.CallType
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.MessageType
import com.example.model.PublicUserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CommunicationViewModel(
    private val commRepo: CommunicationRepository,
    private val userRepo: UserRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    // Active Call Controls
    private val _activeCallId = MutableStateFlow<String?>(null)
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isCameraOn = MutableStateFlow(true)
    val isCameraOn: StateFlow<Boolean> = _isCameraOn.asStateFlow()

    private val _useFrontCamera = MutableStateFlow(true)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera.asStateFlow()

    private val _isScreenSharing = MutableStateFlow(false)
    val isScreenSharing: StateFlow<Boolean> = _isScreenSharing.asStateFlow()

    private val _callDurationSec = MutableStateFlow(0)
    val callDurationSec: StateFlow<Int> = _callDurationSec.asStateFlow()

    val contactsState: StateFlow<CloudListState<List<PublicUserProfile>>> =
        userRepo.observeSearchableContacts()
            .map<List<PublicUserProfile>, CloudListState<List<PublicUserProfile>>> { list ->
                CloudListState.Success(list.filter { it.uid != currentUserId })
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                Log.w(TAG, "Contacts error", root)
                emit(CloudListState.Error(friendlyFirestoreMessage(root, "Could not load contacts")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CloudListState.Loading)

    val conversationsState: StateFlow<CloudListState<List<Conversation>>> =
        commRepo.observeConversations(currentUserId)
            .map<List<Conversation>, CloudListState<List<Conversation>>> { CloudListState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                Log.w(TAG, "Conversations error", root)
                emit(CloudListState.Error(friendlyFirestoreMessage(root, "Could not load conversations")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CloudListState.Loading)

    val activeMessagesState: StateFlow<CloudListState<List<ChatMessage>>> =
        _selectedConversationId
            .flatMapLatest { convId ->
                if (convId.isNullOrBlank()) {
                    flowOf(CloudListState.Success(emptyList()))
                } else {
                    commRepo.observeMessages(currentUserId, convId)
                        .map<List<ChatMessage>, CloudListState<List<ChatMessage>>> { CloudListState.Success(it) }
                        .catch { error ->
                            val fsErr = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (fsErr == null && error is CancellationException) throw error
                            val root = fsErr ?: error
                            Log.w(TAG, "Messages error", root)
                            emit(CloudListState.Error(friendlyFirestoreMessage(root, "Could not load messages")))
                        }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CloudListState.Loading)

    val activeCalls: StateFlow<List<CallSession>> =
        commRepo.observeActiveCalls(currentUserId)
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                Log.w(TAG, "Calls error", fsErr ?: error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val callHistoryState: StateFlow<CloudListState<List<CallHistoryEntry>>> =
        commRepo.observeCallHistory(currentUserId)
            .map<List<CallHistoryEntry>, CloudListState<List<CallHistoryEntry>>> { CloudListState.Success(it) }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                val root = fsErr ?: error
                emit(CloudListState.Error(friendlyFirestoreMessage(root, "Could not load call history")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CloudListState.Loading)

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val calls = activeCalls.value
                val current = calls.firstOrNull { it.id == _activeCallId.value }
                    ?: calls.firstOrNull { it.status == "ACTIVE" || it.callerUid == currentUserId }
                if (current != null && (current.status == "ACTIVE" || current.status == "RINGING")) {
                    if (current.status == "ACTIVE") {
                        _callDurationSec.update { it + 1 }
                    }
                } else if (_activeCallId.value == null) {
                    _callDurationSec.value = 0
                }
            }
        }
    }

    fun selectConversation(conversationId: String?) {
        _selectedConversationId.value = conversationId
    }

    fun startDirectChat(currentName: String, peer: PublicUserProfile) {
        viewModelScope.launch {
            val res = commRepo.createOrOpenDirectConversation(currentUserId, currentName, peer)
            if (res.isSuccess) {
                _selectedConversationId.value = res.getOrNull()
            } else {
                _statusBanner.value = res.exceptionOrNull()?.let {
                    friendlyFirestoreMessage(it, "Unable to open chat")
                }
            }
        }
    }

    fun startGroupChat(currentName: String, title: String, peers: List<PublicUserProfile>) {
        if (peers.isEmpty()) {
            _statusBanner.value = "Select at least one contact for a group chat."
            return
        }
        viewModelScope.launch {
            val res = commRepo.createGroupConversation(currentUserId, currentName, title, peers)
            if (res.isSuccess) {
                _selectedConversationId.value = res.getOrNull()
            } else {
                _statusBanner.value = res.exceptionOrNull()?.let {
                    friendlyFirestoreMessage(it, "Unable to create group chat")
                }
            }
        }
    }

    fun sendMessage(
        conversation: Conversation,
        senderName: String,
        messageType: MessageType,
        text: String,
        mediaUrl: String? = null,
        sharedTitle: String? = null,
        fileName: String? = null,
        durationSec: Int? = null
    ) {
        viewModelScope.launch {
            val res = commRepo.sendMessage(
                conversation = conversation,
                senderName = senderName,
                messageType = messageType,
                text = text,
                mediaUrl = mediaUrl,
                sharedTitle = sharedTitle,
                fileName = fileName,
                durationSec = durationSec
            )
            if (res.isFailure) {
                _statusBanner.value = res.exceptionOrNull()?.let {
                    friendlyFirestoreMessage(it, "Failed to send message")
                }
            }
        }
    }

    fun markMessagesSeen(conversationId: String, messages: List<ChatMessage>) {
        val unseen = messages.filter { currentUserId !in it.readBy && it.senderUid != currentUserId }
        if (unseen.isEmpty()) return
        viewModelScope.launch {
            unseen.takeLast(15).forEach { msg ->
                commRepo.markMessageSeen(conversationId, msg, currentUserId)
            }
        }
    }

    fun startCall(
        callerName: String,
        title: String,
        callType: CallType,
        calleeUids: List<String>
    ) {
        if (calleeUids.isEmpty()) {
            _statusBanner.value = "Select at least one person to call."
            return
        }
        if (calleeUids.size > 7) {
            _statusBanner.value = "Group calls support up to 8 participants including you."
            return
        }
        viewModelScope.launch {
            _callDurationSec.value = 0
            _isMuted.value = false
            _isScreenSharing.value = false
            _isCameraOn.value = (callType == CallType.VIDEO || callType == CallType.GROUP_VIDEO)
            val res = commRepo.startCall(
                callerUid = currentUserId,
                callerName = callerName,
                title = title,
                callType = callType,
                calleeUids = calleeUids
            )
            if (res.isSuccess) {
                _activeCallId.value = res.getOrNull()?.id
            } else {
                _statusBanner.value = res.exceptionOrNull()?.let {
                    friendlyFirestoreMessage(it, "Could not start call")
                }
            }
        }
    }

    fun answerIncomingCall(call: CallSession) {
        viewModelScope.launch {
            _callDurationSec.value = 0
            _isMuted.value = false
            _isCameraOn.value = call.callType.contains("VIDEO")
            _activeCallId.value = call.id
            val res = commRepo.answerCall(call, currentUserId)
            if (res.isFailure) {
                _statusBanner.value = res.exceptionOrNull()?.let {
                    friendlyFirestoreMessage(it, "Could not answer call")
                }
            }
        }
    }

    fun declineOrEndCall(call: CallSession, decline: Boolean) {
        viewModelScope.launch {
            val elapsed = _callDurationSec.value
            val res = commRepo.endOrDeclineCall(call, currentUserId, decline, elapsed)
            _activeCallId.value = null
            _callDurationSec.value = 0
            _isScreenSharing.value = false
            if (res.isFailure) {
                _statusBanner.value = res.exceptionOrNull()?.let {
                    friendlyFirestoreMessage(it, "Could not update call state")
                }
            }
        }
    }

    fun toggleMute() { _isMuted.update { !it } }
    fun toggleSpeaker() { _isSpeakerOn.update { !it } }
    fun toggleCamera() { _isCameraOn.update { !it } }
    fun flipCamera() { _useFrontCamera.update { !it } }
    fun toggleScreenShare() { _isScreenSharing.update { !it } }
    fun clearStatusBanner() { _statusBanner.value = null }

    private companion object {
        const val TAG = "CommVM"
    }
}
