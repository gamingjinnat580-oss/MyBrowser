package com.example.data

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.CallHistoryEntry
import com.example.model.CallSession
import com.example.model.CallType
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.MessageType
import com.example.model.PublicUserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CommunicationRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before communicating.")

    fun observeConversations(userId: String): Flow<List<Conversation>> =
        db.collection("conversations")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    Conversation::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> =
        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
            }

    suspend fun createOrOpenDirectConversation(
        currentUid: String,
        currentName: String,
        peer: PublicUserProfile
    ): Result<String> {
        val sortedIds = listOf(currentUid, peer.uid).sorted()
        val deterministicId = "dm_${sortedIds[0].take(20)}_${sortedIds[1].take(20)}"
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val path = "conversations/$deterministicId"
        return try {
            requireUserId()
            val docRef = db.collection("conversations").document(deterministicId)
            val snap = try {
                docRef.get().await()
            } catch (_: Exception) {
                null
            }
            if (snap == null || !snap.exists()) {
                val payload = mapOf(
                    "title" to peer.displayName.take(80),
                    "groupChat" to false,
                    "participantUids" to sortedIds,
                    "participantSummary" to "$currentName, ${peer.displayName}".take(300),
                    "createdBy" to currentUid,
                    "lastMessageText" to "Conversation started",
                    "lastMessageType" to "TEXT",
                    "lastSenderUid" to currentUid,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(deterministicId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroupConversation(
        currentUid: String,
        currentName: String,
        title: String,
        peers: List<PublicUserProfile>
    ): Result<String> {
        val path = "conversations"
        return try {
            requireUserId()
            val cleanTitle = title.trim().ifEmpty { "Orbit Group" }.take(80)
            val participantUids = (listOf(currentUid) + peers.map { it.uid }).distinct().take(20)
            val summary = (listOf(currentName) + peers.map { it.displayName }).distinct().joinToString(", ").take(300)
            val docRef = db.collection("conversations").document()
            docRef.set(
                mapOf(
                    "title" to cleanTitle,
                    "groupChat" to true,
                    "participantUids" to participantUids,
                    "participantSummary" to summary,
                    "createdBy" to currentUid,
                    "lastMessageText" to "Group created by $currentName",
                    "lastMessageType" to "TEXT",
                    "lastSenderUid" to currentUid,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        conversation: Conversation,
        senderName: String,
        messageType: MessageType,
        text: String,
        mediaUrl: String? = null,
        sharedTitle: String? = null,
        fileName: String? = null,
        durationSec: Int? = null
    ): Result<Unit> {
        val path = "conversations/${conversation.id}/messages"
        return try {
            val uid = requireUserId()
            require(conversation.id.isNotBlank()) { "Conversation ID missing" }
            val cleanText = text.trim().take(4000)
            val cleanMedia = mediaUrl?.trim()?.take(2000)?.takeIf { it.isNotEmpty() }
            val cleanSharedTitle = sharedTitle?.trim()?.take(200)?.takeIf { it.isNotEmpty() }
            val cleanFileName = fileName?.trim()?.take(200)?.takeIf { it.isNotEmpty() }

            val msgRef = db.collection("conversations")
                .document(conversation.id)
                .collection("messages")
                .document()

            val payload = mutableMapOf<String, Any>(
                "conversationId" to conversation.id,
                "senderUid" to uid,
                "senderName" to senderName.trim().ifEmpty { "Orbit User" }.take(80),
                "participantUids" to conversation.participantUids.take(20),
                "messageType" to messageType.name,
                "text" to cleanText,
                "readBy" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanMedia != null) payload["mediaUrl"] = cleanMedia
            if (cleanSharedTitle != null) payload["sharedTitle"] = cleanSharedTitle
            if (cleanFileName != null) payload["fileName"] = cleanFileName
            if (durationSec != null) payload["durationSec"] = durationSec.coerceIn(0, 3600)

            msgRef.set(payload).await()

            val preview = when (messageType) {
                MessageType.IMAGE -> "Photo: ${cleanText.ifEmpty { cleanFileName ?: "Image" }}"
                MessageType.VOICE -> "Voice message (${durationSec ?: 0}s)"
                MessageType.FILE -> "File: ${cleanFileName ?: "Attachment"}"
                MessageType.WEB_SHARE -> "Shared link: ${cleanSharedTitle ?: cleanMedia ?: cleanText}"
                MessageType.SMS_NOTE -> "[SMS] $cleanText"
                MessageType.TEXT -> cleanText
            }.take(500)

            db.collection("conversations").document(conversation.id).update(
                mapOf(
                    "lastMessageText" to preview,
                    "lastMessageType" to messageType.name,
                    "lastSenderUid" to uid,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markMessageSeen(conversationId: String, message: ChatMessage, uid: String): Result<Unit> {
        if (uid in message.readBy || message.id.isBlank()) return Result.success(Unit)
        val path = "conversations/$conversationId/messages/${message.id}"
        return try {
            requireUserId()
            val updatedReadBy = (message.readBy + uid).distinct().take(20)
            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(message.id)
                .update(
                    mapOf(
                        "readBy" to updatedReadBy,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Calls (1-to-1 and Group up to 8 participants) ---

    fun observeActiveCalls(userId: String): Flow<List<CallSession>> =
        db.collection("calls")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CallSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filter { it.status == "RINGING" || it.status == "ACTIVE" }
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }

    fun observeCallHistory(userId: String): Flow<List<CallHistoryEntry>> =
        db.collection("users").document(userId).collection("call_history")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CallHistoryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun startCall(
        callerUid: String,
        callerName: String,
        title: String,
        callType: CallType,
        calleeUids: List<String>
    ): Result<CallSession> {
        val path = "calls"
        return try {
            requireUserId()
            val participants = (listOf(callerUid) + calleeUids).distinct().take(8)
            val docRef = db.collection("calls").document()
            val sdpPlaceholder = "v=0\r\no=orbit-${docRef.id} 1 2 IN IP4 127.0.0.1\r\ns=OrbitCall"
            val payload = mapOf(
                "callerUid" to callerUid,
                "callerName" to callerName.trim().ifEmpty { "Orbit User" }.take(80),
                "callType" to callType.name,
                "status" to "RINGING",
                "participantUids" to participants,
                "activeUids" to listOf(callerUid),
                "title" to title.trim().ifEmpty { "Orbit Call" }.take(80),
                "sdpOffer" to sdpPlaceholder,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val session = CallSession(
                id = docRef.id,
                callerUid = callerUid,
                callerName = callerName,
                callType = callType.name,
                status = "RINGING",
                participantUids = participants,
                activeUids = listOf(callerUid),
                title = title,
                sdpOffer = sdpPlaceholder
            )
            Result.success(session)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun answerCall(call: CallSession, uid: String): Result<Unit> {
        val path = "calls/${call.id}"
        return try {
            requireUserId()
            val newActive = (call.activeUids + uid).distinct().take(8)
            db.collection("calls").document(call.id).update(
                mapOf(
                    "status" to "ACTIVE",
                    "activeUids" to newActive,
                    "sdpAnswer" to "v=0\r\no=orbit-answer-$uid 1 2 IN IP4 127.0.0.1",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun endOrDeclineCall(
        call: CallSession,
        uid: String,
        decline: Boolean,
        durationSec: Int
    ): Result<Unit> {
        val path = "calls/${call.id}"
        return try {
            requireUserId()
            val finalStatus = if (decline) "DECLINED" else "ENDED"
            db.collection("calls").document(call.id).update(
                mapOf(
                    "status" to finalStatus,
                    "activeUids" to emptyList<String>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val direction = if (call.callerUid == uid) "OUTGOING" else if (decline) "MISSED" else "INCOMING"
            db.collection("users").document(uid).collection("call_history").document().set(
                mapOf(
                    "userId" to uid,
                    "peerName" to call.title.ifEmpty { call.callerName }.take(80),
                    "callType" to call.callType,
                    "direction" to direction,
                    "status" to finalStatus,
                    "durationSec" to durationSec.coerceIn(0, 86400),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
