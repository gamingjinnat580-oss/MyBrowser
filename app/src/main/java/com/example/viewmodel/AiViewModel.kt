package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiAssistantRepository
import com.example.data.AiQaEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data object Streaming : AiUiState
    data class Error(val message: String) : AiUiState
}

class AiViewModel(
    private val repository: AiAssistantRepository = AiAssistantRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<AiQaEntry>>(emptyList())
    val messages: StateFlow<List<AiQaEntry>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _includePageContext = MutableStateFlow(false)
    val includePageContext: StateFlow<Boolean> = _includePageContext.asStateFlow()

    fun togglePageContext() {
        _includePageContext.update { !it }
    }

    fun clearConversation() {
        _messages.value = emptyList()
        _uiState.value = AiUiState.Idle
    }

    fun askQuestion(
        question: String,
        activePageTitle: String?,
        activePageUrl: String?
    ) {
        val cleanQuestion = question.trim()
        if (cleanQuestion.isEmpty()) return
        if (_uiState.value is AiUiState.Loading || _uiState.value is AiUiState.Streaming) return

        val attachContext = _includePageContext.value && !activePageUrl.isNullOrBlank() && activePageUrl.startsWith("http")
        val priorHistory = _messages.value
        val userEntry = AiQaEntry(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = cleanQuestion,
            pageContextTitle = if (attachContext) activePageTitle else null
        )
        val assistantId = UUID.randomUUID().toString()
        val placeholderEntry = AiQaEntry(
            id = assistantId,
            isUser = false,
            text = "",
            isStreaming = true
        )

        _messages.value = priorHistory + userEntry + placeholderEntry
        _uiState.value = AiUiState.Loading

        viewModelScope.launch {
            val buffer = StringBuilder()
            try {
                withTimeout(60_000L) {
                    repository.askQuestionStream(
                        question = cleanQuestion,
                        history = priorHistory,
                        pageTitle = if (attachContext) activePageTitle else null,
                        pageUrl = if (attachContext) activePageUrl else null
                    ).collect { chunk ->
                        buffer.append(chunk)
                        _uiState.value = AiUiState.Streaming
                        _messages.update { list ->
                            list.map {
                                if (it.id == assistantId) it.copy(text = buffer.toString(), isStreaming = true)
                                else it
                            }
                        }
                    }
                }
                val finalAnswer = buffer.toString().trim()
                if (finalAnswer.isEmpty()) {
                    _messages.update { list -> list.filterNot { it.id == assistantId } }
                    _uiState.value = AiUiState.Error("Orbit AI returned an empty answer. Please try again.")
                } else {
                    _messages.update { list ->
                        list.map {
                            if (it.id == assistantId) it.copy(text = finalAnswer, isStreaming = false)
                            else it
                        }
                    }
                    _uiState.value = AiUiState.Idle
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI generation timed out", e)
                _messages.update { list -> list.filterNot { it.id == assistantId && it.text.isBlank() } }
                _uiState.value = AiUiState.Error("Orbit AI timed out after 60 seconds. Check your connection and retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI generation failed", e)
                _messages.update { list -> list.filterNot { it.id == assistantId && it.text.isBlank() } }
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "Orbit AI could not answer right now.")
            }
        }
    }

    private companion object {
        const val TAG = "AiViewModel"
    }
}
