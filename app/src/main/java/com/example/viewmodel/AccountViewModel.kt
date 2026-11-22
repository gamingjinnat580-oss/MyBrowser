package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.friendlyFirestoreMessage
import com.example.data.UserRepository
import com.example.model.PrivateUserAccount
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AccountUiState {
    data object Loading : AccountUiState
    data class Success(val account: PrivateUserAccount?) : AccountUiState
    data class Error(val message: String) : AccountUiState
}

class AccountViewModel(
    private val userRepo: UserRepository,
    private val currentUserId: String,
    firebaseUser: FirebaseUser?
) : ViewModel() {

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val accountState: StateFlow<AccountUiState> = userRepo.observePrivateAccount(currentUserId)
        .map<PrivateUserAccount?, AccountUiState> { AccountUiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Account stream error", root)
            emit(AccountUiState.Error(friendlyFirestoreMessage(root, "Could not load account profile")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AccountUiState.Loading)

    init {
        if (firebaseUser != null) {
            viewModelScope.launch {
                val res = userRepo.ensureUserInitialized(firebaseUser)
                if (res.isFailure) {
                    _feedbackMessage.value = res.exceptionOrNull()?.let {
                        friendlyFirestoreMessage(it, "Could not sync user profile")
                    }
                }
            }
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        status: String,
        phoneNumber: String,
        photoUrl: String?
    ) {
        viewModelScope.launch {
            val res = userRepo.updateProfile(displayName, username, bio, status, phoneNumber, photoUrl)
            _feedbackMessage.value = if (res.isSuccess) {
                "Profile updated successfully."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to update profile."
            }
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val res = userRepo.deleteCurrentAccount()
            if (res.isSuccess) {
                onDeleted()
            } else {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage
                    ?: "Re-authenticate recently to delete your account."
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    private companion object {
        const val TAG = "AccountVM"
    }
}
