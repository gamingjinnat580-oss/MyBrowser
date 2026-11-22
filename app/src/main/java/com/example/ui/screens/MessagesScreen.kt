package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.CallType
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.MessageType
import com.example.model.PublicUserProfile
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.OrbitRose
import com.example.ui.theme.OrbitViolet
import com.example.ui.theme.OrbitVoid
import com.example.viewmodel.CloudListState
import com.example.viewmodel.CommunicationViewModel
import java.io.File

@Composable
fun MessagesScreen(
    viewModel: CommunicationViewModel,
    currentUserId: String,
    currentUserName: String,
    onOpenUrlInBrowser: (String) -> Unit,
    onCallStarted: () -> Unit
) {
    val context = LocalContext.current
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()
    val selectedConvId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val messagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val banner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var activeTab by remember { mutableIntStateOf(0) }
    var contactQuery by remember { mutableStateOf("") }
    var showNewGroupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(banner) {
        banner?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusBanner()
        }
    }

    val activeConversation = (conversationsState as? CloudListState.Success)?.items
        ?.firstOrNull { it.id == selectedConvId }

    BackHandler(enabled = activeConversation != null) {
        viewModel.selectConversation(null)
    }

    if (activeConversation != null) {
        ChatRoomView(
            conversation = activeConversation,
            messagesState = messagesState,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            onBack = { viewModel.selectConversation(null) },
            onSend = { type, text, mediaUrl, sharedTitle, fileName, duration ->
                viewModel.sendMessage(
                    conversation = activeConversation,
                    senderName = currentUserName,
                    messageType = type,
                    text = text,
                    mediaUrl = mediaUrl,
                    sharedTitle = sharedTitle,
                    fileName = fileName,
                    durationSec = duration
                )
            },
            onMarkSeen = { msgs -> viewModel.markMessagesSeen(activeConversation.id, msgs) },
            onOpenUrl = onOpenUrlInBrowser,
            onStartCall = { isVideo ->
                val peers = activeConversation.participantUids.filter { it != currentUserId }
                val type = when {
                    activeConversation.groupChat && isVideo -> CallType.GROUP_VIDEO
                    activeConversation.groupChat && !isVideo -> CallType.GROUP_VOICE
                    isVideo -> CallType.VIDEO
                    else -> CallType.VOICE
                }
                viewModel.startCall(currentUserName, activeConversation.title, type, peers)
                onCallStarted()
            },
            onLaunchCarrierSms = { msgText ->
                try {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                        putExtra("sms_body", msgText)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Fallback handled gracefully
                }
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Messages & Contacts", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { showNewGroupDialog = true }) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Group")
                    }
                }
                TabRow(selectedTabIndex = activeTab) {
                    Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Chats") })
                    Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Contacts") })
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (activeTab == 0) {
                when (val state = conversationsState) {
                    CloudListState.Loading -> Text("Loading conversations...", Modifier.padding(20.dp))
                    is CloudListState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp))
                    is CloudListState.Success -> {
                        if (state.items.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("No Chats Yet", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Switch to Contacts to find friends by username and start 1:1 or group messaging.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(onClick = { activeTab = 1 }) { Text("Browse Contacts") }
                            }
                        } else {
                            LazyColumn {
                                items(state.items, key = { it.id }) { conv ->
                                    ListItem(
                                        headlineContent = { Text(conv.title.ifEmpty { "Direct Chat" }, fontWeight = FontWeight.SemiBold) },
                                        supportingContent = {
                                            Text(
                                                text = conv.lastMessageText.ifEmpty { conv.participantSummary },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingContent = {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (conv.groupChat) OrbitViolet.copy(0.2f) else OrbitCyan.copy(0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (conv.groupChat) Icons.Default.GroupAdd else Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = if (conv.groupChat) OrbitViolet else OrbitCyan
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .clickable { viewModel.selectConversation(conv.id) }
                                            .testTag("conversation_row_${conv.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = contactQuery,
                        onValueChange = { contactQuery = it },
                        placeholder = { Text("Search users by username or name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                    )
                    when (val state = contactsState) {
                        CloudListState.Loading -> Text("Searching Orbit directory...", Modifier.padding(20.dp))
                        is CloudListState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp))
                        is CloudListState.Success -> {
                            val filtered = state.items.filter {
                                val q = contactQuery.trim().lowercase()
                                q.isEmpty() || it.displayName.lowercase().contains(q) || it.username.lowercase().contains(q)
                            }
                            if (filtered.isEmpty()) {
                                Text(
                                    text = "No matching contacts found yet. Other users appear here after signing in with Google.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            } else {
                                LazyColumn {
                                    items(filtered, key = { it.uid }) { user ->
                                        ListItem(
                                            headlineContent = { Text(user.displayName) },
                                            supportingContent = { Text("@${user.username} • ${user.status}") },
                                            trailingContent = {
                                                Row {
                                                    IconButton(onClick = {
                                                        viewModel.startCall(currentUserName, user.displayName, CallType.VOICE, listOf(user.uid))
                                                        onCallStarted()
                                                    }) {
                                                        Icon(Icons.Default.Call, contentDescription = "Voice call ${user.displayName}")
                                                    }
                                                    IconButton(onClick = {
                                                        viewModel.startDirectChat(currentUserName, user)
                                                    }) {
                                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Message ${user.displayName}")
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewGroupDialog) {
        val allContacts = (contactsState as? CloudListState.Success)?.items ?: emptyList()
        var groupName by remember { mutableStateOf("") }
        val selectedUsers = remember { mutableStateListOf<PublicUserProfile>() }

        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = { Text("Create Group Chat") },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Select Participants", style = MaterialTheme.typography.labelLarge)
                    if (allContacts.isEmpty()) {
                        Text("No other users available yet.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                    } else {
                        LazyColumn(modifier = Modifier.height(220.dp)) {
                            items(allContacts, key = { it.uid }) { user ->
                                val checked = selectedUsers.any { it.uid == user.uid }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (checked) selectedUsers.removeAll { it.uid == user.uid }
                                            else selectedUsers.add(user)
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${user.displayName} (@${user.username})")
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startGroupChat(currentUserName, groupName, selectedUsers.toList())
                        showNewGroupDialog = false
                    },
                    enabled = selectedUsers.isNotEmpty()
                ) { Text("Create") }
            }
        )
    }
}

@Composable
private fun ChatRoomView(
    conversation: Conversation,
    messagesState: CloudListState<List<ChatMessage>>,
    currentUserId: String,
    currentUserName: String,
    onBack: () -> Unit,
    onSend: (
        type: MessageType,
        text: String,
        mediaUrl: String?,
        sharedTitle: String?,
        fileName: String?,
        durationSec: Int?
    ) -> Unit,
    onMarkSeen: (List<ChatMessage>) -> Unit,
    onOpenUrl: (String) -> Unit,
    onStartCall: (isVideo: Boolean) -> Unit,
    onLaunchCarrierSms: (String) -> Unit
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var smsMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var recordStartMs by remember { mutableStateOf(0L) }
    val listState = rememberLazyListState()

    val quickEmojis = remember { listOf("👍", "❤️", "😂", "🔥", "🎉", "🙏", "🌐") }

    // Zero-permission Photo Picker (Play Policy Compliant)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSend(MessageType.IMAGE, "Shared photo", uri.toString(), null, "photo.jpg", null)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var name = "attachment"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && idx >= 0) name = c.getString(idx) ?: name
                }
            } catch (_: Exception) {}
            onSend(MessageType.FILE, "Shared file: $name", uri.toString(), null, name, null)
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val outFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                mr.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(outFile.absolutePath)
                    prepare()
                    start()
                }
                recorder = mr
                recordedFile = outFile
                recordStartMs = System.currentTimeMillis()
                isRecording = true
            } catch (_: Exception) {
                isRecording = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {}
        }
    }

    val msgList = (messagesState as? CloudListState.Success)?.items ?: emptyList()

    LaunchedEffect(msgList.size) {
        if (msgList.isNotEmpty()) {
            listState.animateScrollToItem(msgList.lastIndex)
            onMarkSeen(msgList)
        }
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to chats")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conversation.title.ifEmpty { "Chat" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${conversation.participantUids.size} participants",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onStartCall(false) }) {
                        Icon(Icons.Default.Call, contentDescription = "Voice call", tint = OrbitEmerald)
                    }
                    IconButton(onClick = { onStartCall(true) }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video call", tint = OrbitCyan)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(msgList, key = { it.id }) { msg ->
                    val isMine = msg.senderUid == currentUserId
                    val isSeenByOther = msg.readBy.any { it != msg.senderUid }
                    MessageBubble(
                        msg = msg,
                        isMine = isMine,
                        isSeenByOther = isSeenByOther,
                        onOpenUrl = onOpenUrl,
                        onPlayVoice = { path ->
                            try {
                                MediaPlayer().apply {
                                    setDataSource(context, Uri.parse(path))
                                    prepare()
                                    start()
                                    setOnCompletionListener { release() }
                                }
                            } catch (_: Exception) {}
                        }
                    )
                }
            }

            // Emoji & SMS Mode Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = smsMode,
                        onClick = { smsMode = !smsMode },
                        leadingIcon = { Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("In-App SMS") }
                    )
                }
                if (smsMode && textInput.isNotBlank()) {
                    item {
                        TextButton(onClick = { onLaunchCarrierSms(textInput) }) {
                            Text("Carrier SMS")
                        }
                    }
                }
                items(quickEmojis) { emoji ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { textInput += emoji }
                    ) {
                        Text(emoji, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }

            // Composer
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Default.Image, contentDescription = "Pick photo")
                    }
                    IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach file")
                    }
                    IconButton(onClick = {
                        if (isRecording) {
                            try {
                                recorder?.stop()
                                recorder?.release()
                            } catch (_: Exception) {}
                            recorder = null
                            isRecording = false
                            val dur = ((System.currentTimeMillis() - recordStartMs) / 1000L).toInt().coerceAtLeast(1)
                            recordedFile?.let { f ->
                                onSend(MessageType.VOICE, "Voice note", Uri.fromFile(f).toString(), null, f.name, dur)
                            }
                        } else {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    val outFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                                    val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
                                    mr.apply {
                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                        setOutputFile(outFile.absolutePath)
                                        prepare()
                                        start()
                                    }
                                    recorder = mr
                                    recordedFile = outFile
                                    recordStartMs = System.currentTimeMillis()
                                    isRecording = true
                                } catch (_: Exception) {}
                            } else {
                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop recording" else "Record voice note",
                            tint = if (isRecording) OrbitRose else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text(if (smsMode) "Send in-app SMS..." else "Message...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        enabled = textInput.isNotBlank(),
                        onClick = {
                            val type = if (smsMode) MessageType.SMS_NOTE else MessageType.TEXT
                            onSend(type, textInput, null, null, null, null)
                            textInput = ""
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = OrbitCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    msg: ChatMessage,
    isMine: Boolean,
    isSeenByOther: Boolean,
    onOpenUrl: (String) -> Unit,
    onPlayVoice: (String) -> Unit
) {
    val type = MessageType.safeFrom(msg.messageType)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (!isMine) {
                    Text(msg.senderName, style = MaterialTheme.typography.labelSmall, color = OrbitCyan)
                }
                when (type) {
                    MessageType.IMAGE -> {
                        if (!msg.mediaUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = msg.mediaUrl,
                                contentDescription = "Shared photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        Text(msg.text, style = MaterialTheme.typography.bodyMedium)
                    }
                    MessageType.VOICE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { msg.mediaUrl?.let(onPlayVoice) }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play voice note", tint = OrbitCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Voice Note (${msg.durationSec ?: 0}s)", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    MessageType.WEB_SHARE -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { msg.mediaUrl?.let { if (it.startsWith("http")) onOpenUrl(it) } }
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = OrbitCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(msg.sharedTitle ?: "Shared Web Page", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(msg.mediaUrl ?: msg.text, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    MessageType.SMS_NOTE -> {
                        Text("[SMS] ${msg.text}", style = MaterialTheme.typography.bodyLarge, color = OrbitEmerald)
                    }
                    MessageType.FILE -> {
                        Text("Attachment: ${msg.fileName ?: msg.text}", style = MaterialTheme.typography.bodyMedium)
                    }
                    MessageType.TEXT -> {
                        Text(msg.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (isMine) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSeenByOther) Icons.Default.DoneAll else Icons.Default.Check,
                            contentDescription = if (isSeenByOther) "Seen" else "Delivered",
                            tint = if (isSeenByOther) OrbitCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
