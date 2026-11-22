package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PublicUserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val photoUrl: String? = null,
    val bio: String = "",
    val status: String = "Online",
    val searchable: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateUserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUrl: String? = null,
    val bio: String = "",
    val status: String = "Online",
    val defaultSearchEngine: String = "Google",
    val darkModeWeb: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BrowserBookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val url: String = "",
    val category: String = "General",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BrowserHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val url: String = "",
    @ServerTimestamp val visitedAt: Timestamp? = null
)

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    FILE,
    WEB_SHARE,
    SMS_NOTE;

    companion object {
        fun safeFrom(raw: String?): MessageType =
            entries.firstOrNull { it.name == raw } ?: TEXT
    }
}

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val title: String = "",
    val groupChat: Boolean = false,
    val participantUids: List<String> = emptyList(),
    val participantSummary: String = "",
    val createdBy: String = "",
    val lastMessageText: String = "",
    val lastMessageType: String = "TEXT",
    val lastSenderUid: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val participantUids: List<String> = emptyList(),
    val messageType: String = "TEXT",
    val text: String = "",
    val mediaUrl: String? = null,
    val sharedTitle: String? = null,
    val fileName: String? = null,
    val durationSec: Long? = null,
    val readBy: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class CallType {
    VOICE,
    VIDEO,
    GROUP_VOICE,
    GROUP_VIDEO;

    companion object {
        fun safeFrom(raw: String?): CallType =
            entries.firstOrNull { it.name == raw } ?: VOICE
    }
}

enum class CallStatus {
    RINGING,
    ACTIVE,
    ENDED,
    DECLINED;

    companion object {
        fun safeFrom(raw: String?): CallStatus =
            entries.firstOrNull { it.name == raw } ?: ENDED
    }
}

@Keep
@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerUid: String = "",
    val callerName: String = "",
    val callType: String = "VOICE",
    val status: String = "RINGING",
    val participantUids: List<String> = emptyList(),
    val activeUids: List<String> = emptyList(),
    val title: String = "",
    val sdpOffer: String? = null,
    val sdpAnswer: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val peerName: String = "",
    val callType: String = "VOICE",
    val direction: String = "OUTGOING",
    val status: String = "ENDED",
    val durationSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
