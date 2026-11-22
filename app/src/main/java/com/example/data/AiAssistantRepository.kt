package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class AiQaEntry(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val pageContextTitle: String? = null,
    val isStreaming: Boolean = false
)

class AiAssistantRepository {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are Orbit AI, a fast, accurate, multilingual Q&A assistant built inside Orbit Browser. " +
                        "Always answer every question directly, truthfully, and clearly in the exact language the user asked " +
                        "(such as Bengali, English, Hindi, Arabic, Spanish, or any other language). " +
                        "If web page context is provided, use it accurately to summarize or answer questions about that page. " +
                        "Keep explanations structured, helpful, and easy to read on mobile."
                )
            }
        )
    }

    fun askQuestionStream(
        question: String,
        history: List<AiQaEntry>,
        pageTitle: String?,
        pageUrl: String?
    ): Flow<String> = flow {
        val chatHistory = history
            .filter { !it.isStreaming && it.text.isNotBlank() }
            .takeLast(12)
            .map { entry ->
                content(role = if (entry.isUser) "user" else "model") {
                    text(entry.text)
                }
            }

        val chat = model.startChat(history = chatHistory)
        val prompt = buildString {
            if (!pageUrl.isNullOrBlank() && pageUrl.startsWith("http")) {
                append("[Active Browser Page Context]\n")
                append("Title: ${pageTitle ?: "Web Page"}\n")
                append("URL: $pageUrl\n\n")
            }
            append(question.trim())
        }

        chat.sendMessageStream(prompt).collect { chunk ->
            val piece = chunk.text
            if (!piece.isNullOrEmpty()) {
                emit(piece)
            }
        }
    }
}
