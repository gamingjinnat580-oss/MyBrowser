package com.example.data

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.PrivateUserAccount
import com.example.model.PublicUserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing user data.")

    fun observePrivateAccount(userId: String): Flow<PrivateUserAccount?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    PrivateUserAccount::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeSearchableContacts(): Flow<List<PublicUserProfile>> =
        db.collection("users_public")
            .whereEqualTo("searchable", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PublicUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun ensureUserInitialized(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val privatePath = "users/$uid"
        val publicPath = "users_public/$uid"
        return try {
            val privateRef = db.collection("users").document(uid)
            val publicRef = db.collection("users_public").document(uid)

            val rawName = user.displayName?.takeIf { it.isNotBlank() } ?: "Orbit Explorer"
            val displayName = rawName.take(80)
            val sanitizedHandle = (user.email?.substringBefore("@") ?: displayName)
                .lowercase()
                .replace(Regex("[^a-z0-9_\\-]"), "_")
                .take(28)
                .ifBlank { "user_${uid.take(6)}" }
            val username = if (sanitizedHandle.length >= 2) sanitizedHandle else "user_${uid.take(6)}"
            val photoUrl = user.photoUrl?.toString()?.take(1000)

            val privateSnap = privateRef.get().await()
            if (!privateSnap.exists()) {
                val privatePayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to displayName,
                    "username" to username,
                    "email" to (user.email ?: "").take(200),
                    "phoneNumber" to (user.phoneNumber ?: "").take(30),
                    "bio" to "Exploring the web on Orbit Browser",
                    "status" to "Online",
                    "defaultSearchEngine" to "Google",
                    "darkModeWeb" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) {
                    privatePayload["photoUrl"] = photoUrl
                }
                privateRef.set(privatePayload).await()
            }

            val publicSnap = publicRef.get().await()
            if (!publicSnap.exists()) {
                val publicPayload = mutableMapOf<String, Any>(
                    "uid" to uid,
                    "displayName" to displayName,
                    "username" to username,
                    "bio" to "Exploring the web on Orbit Browser",
                    "status" to "Online",
                    "searchable" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!photoUrl.isNullOrBlank()) {
                    publicPayload["photoUrl"] = photoUrl
                }
                publicRef.set(publicPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$privatePath | $publicPath")
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        status: String,
        phoneNumber: String,
        photoUrl: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanName = displayName.trim().take(80)
        val cleanUser = username.trim().lowercase().replace(Regex("[^a-z0-9_\\-]"), "_").take(40)
        if (cleanName.isEmpty()) return Result.failure(IllegalArgumentException("Display name cannot be empty."))
        if (cleanUser.length < 2) return Result.failure(IllegalArgumentException("Username must be at least 2 characters."))

        val cleanBio = bio.trim().take(240)
        val cleanStatus = status.trim().ifEmpty { "Online" }.take(80)
        val cleanPhone = phoneNumber.trim().take(30)
        val cleanPhoto = photoUrl?.trim()?.take(1000)?.takeIf { it.isNotEmpty() }

        return try {
            requireUserId()
            val privateUpdates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "username" to cleanUser,
                "bio" to cleanBio,
                "status" to cleanStatus,
                "phoneNumber" to cleanPhone,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanPhoto != null) {
                privateUpdates["photoUrl"] = cleanPhoto
            }
            db.collection("users").document(uid).update(privateUpdates).await()

            val publicUpdates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "username" to cleanUser,
                "bio" to cleanBio,
                "status" to cleanStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanPhoto != null) {
                publicUpdates["photoUrl"] = cleanPhoto
            }
            db.collection("users_public").document(uid).update(publicUpdates).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateBrowserPreferences(searchEngine: String, darkModeWeb: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            requireUserId()
            db.collection("users").document(uid).update(
                mapOf(
                    "defaultSearchEngine" to searchEngine.take(40),
                    "darkModeWeb" to darkModeWeb,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun deleteCurrentAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        return try {
            db.collection("users_public").document(uid).delete().await()
            db.collection("users").document(uid).delete().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
