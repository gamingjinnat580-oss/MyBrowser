package com.example.data

import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.BrowserBookmark
import com.example.model.BrowserHistoryEntry
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

class BrowserCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing browser data.")

    fun observeBookmarks(userId: String): Flow<List<BrowserBookmark>> =
        db.collection("users").document(userId).collection("bookmarks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    BrowserBookmark::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun addBookmark(userId: String, title: String, url: String, category: String = "General"): Result<Unit> {
        val path = "users/$userId/bookmarks"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed." }
            val cleanTitle = title.trim().ifEmpty { url }.take(200)
            val cleanUrl = url.trim().take(2000)
            require(cleanUrl.isNotEmpty()) { "URL cannot be empty." }

            val docRef = db.collection("users").document(uid).collection("bookmarks").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "url" to cleanUrl,
                    "category" to category.take(60),
                    "createdAt" to FieldValue.serverTimestamp(),
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

    suspend fun deleteBookmark(userId: String, bookmarkId: String): Result<Unit> {
        val path = "users/$userId/bookmarks/$bookmarkId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed." }
            require(bookmarkId.isNotBlank())
            db.collection("users").document(uid).collection("bookmarks").document(bookmarkId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeHistory(userId: String): Flow<List<BrowserHistoryEntry>> =
        db.collection("users").document(userId).collection("history")
            .orderBy("visitedAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    BrowserHistoryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun recordHistory(userId: String, title: String, url: String): Result<Unit> {
        val path = "users/$userId/history"
        return try {
            val uid = requireUserId()
            if (uid != userId) return Result.success(Unit)
            if (!url.startsWith("http://") && !url.startsWith("https://")) return Result.success(Unit)

            val docRef = db.collection("users").document(uid).collection("history").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to title.trim().ifEmpty { url }.take(200),
                    "url" to url.trim().take(2000),
                    "visitedAt" to FieldValue.serverTimestamp()
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

    suspend fun clearHistory(userId: String): Result<Unit> {
        val path = "users/$userId/history"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docs = db.collection("users").document(uid).collection("history").limit(200).get().await()
            val batch = db.batch()
            for (doc in docs.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
