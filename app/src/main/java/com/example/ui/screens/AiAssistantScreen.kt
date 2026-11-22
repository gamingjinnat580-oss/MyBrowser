package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiQaEntry
import com.example.model.ShareToChatPayload
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitViolet
import com.example.ui.theme.OrbitVoid
import com.example.viewmodel.AiUiState
import com.example.viewmodel.AiViewModel

@Composable
fun AiAssistantScreen(
    viewModel: AiViewModel,
    activePageTitle: String,
    activePageUrl: String,
    prefillQuestion: String?,
    onConsumePrefill: () -> Unit,
    onShareAnswerToChat: (ShareToChatPayload) -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val includePageContext by viewModel.includePageContext.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var input by remember { mutableStateOf("") }

    LaunchedEffect(prefillQuestion) {
        if (!prefillQuestion.isNullOrBlank()) {
            input = prefillQuestion
            onConsumePrefill()
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val starterQuestions = remember {
        listOf(
            "বাংলাদেশের ইতিহাস সংক্ষেপে বলো",
            "Explain quantum computing simply",
            "Write a professional email template",
            "How does HTTPS keep browsing safe?"
        )
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(OrbitCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OrbitCyan)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Orbit AI Assistant", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "Ask anything — accurate answers in any language",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (messages.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearConversation() }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear AI chat")
                            }
                        }
                    }
                    if (activePageUrl.startsWith("http")) {
                        Spacer(modifier = Modifier.height(6.dp))
                        FilterChip(
                            selected = includePageContext,
                            onClick = { viewModel.togglePageContext() },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = {
                                Text(
                                    text = "Page context: ${activePageTitle.take(28)}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState is AiUiState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = (uiState as AiUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = OrbitCyan,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "যেকোনো প্রশ্ন করুন, সঠিক উত্তর পান",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ask factual questions, coding help, translations, summaries, or math in Bengali, English, or any language.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(starterQuestions) { prompt ->
                            SuggestionChip(
                                onClick = { viewModel.askQuestion(prompt, activePageTitle, activePageUrl) },
                                label = { Text(prompt) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(messages, key = { it.id }) { entry ->
                        AiMessageBubble(
                            entry = entry,
                            onShare = {
                                onShareAnswerToChat(
                                    ShareToChatPayload(
                                        title = "Orbit AI Answer",
                                        url = entry.text.take(400)
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Input Bar
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Ask Orbit AI any question...") },
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (input.isNotBlank()) {
                                    viewModel.askQuestion(input, activePageTitle, activePageUrl)
                                    input = ""
                                }
                            }
                        ),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_question_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val busy = uiState is AiUiState.Loading || uiState is AiUiState.Streaming
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (busy) OrbitViolet else OrbitCyan)
                            .clickable(enabled = !busy && input.isNotBlank()) {
                                viewModel.askQuestion(input, activePageTitle, activePageUrl)
                                input = ""
                            }
                            .testTag("ai_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send question to AI",
                                tint = OrbitVoid
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    entry: AiQaEntry,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (entry.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (entry.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!entry.pageContextTitle.isNullOrBlank()) {
                    Text(
                        text = "Context: ${entry.pageContextTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrbitCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = if (entry.text.isEmpty() && entry.isStreaming) "Thinking..." else entry.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!entry.isUser && !entry.isStreaming && entry.text.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share AI answer to chat",
                                modifier = Modifier.size(16.dp),
                                tint = OrbitCyan
                            )
                        }
                    }
                }
            }
        }
    }
}
