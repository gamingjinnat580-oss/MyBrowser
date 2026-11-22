package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CallSession
import com.example.model.CallType
import com.example.model.PublicUserProfile
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.OrbitRose
import com.example.ui.theme.OrbitSurface
import com.example.ui.theme.OrbitViolet
import com.example.ui.theme.OrbitVoid
import com.example.viewmodel.CloudListState
import com.example.viewmodel.CommunicationViewModel

@Composable
fun CallsScreen(
    viewModel: CommunicationViewModel,
    currentUserId: String,
    currentUserName: String
) {
    val activeCalls by viewModel.activeCalls.collectAsStateWithLifecycle()
    val callHistoryState by viewModel.callHistoryState.collectAsStateWithLifecycle()
    val contactsState by viewModel.contactsState.collectAsStateWithLifecycle()

    var showGroupCallDialog by remember { mutableStateOf(false) }

    val ongoingCall = activeCalls.firstOrNull {
        it.status == "ACTIVE" || (it.status == "RINGING" && it.callerUid == currentUserId)
    }

    if (ongoingCall != null) {
        ActiveCallStage(
            call = ongoingCall,
            viewModel = viewModel,
            currentUserId = currentUserId
        )
        return
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Voice & Video Calls", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "1-to-1 & Group Rooms (up to 8 participants)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = { showGroupCallDialog = true }) {
                        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Group Call")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            item {
                Text("Quick Call Contacts", style = MaterialTheme.typography.titleMedium)
            }
            when (val cState = contactsState) {
                CloudListState.Loading -> item { Text("Loading contacts...") }
                is CloudListState.Error -> item { Text(cState.message, color = MaterialTheme.colorScheme.error) }
                is CloudListState.Success -> {
                    if (cState.items.isEmpty()) {
                        item {
                            Text(
                                text = "Contacts appear here once other users sign in with Google.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(cState.items, key = { it.uid }) { user ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                ListItem(
                                    headlineContent = { Text(user.displayName) },
                                    supportingContent = { Text("@${user.username} • ${user.status}") },
                                    trailingContent = {
                                        Row {
                                            IconButton(onClick = {
                                                viewModel.startCall(currentUserName, user.displayName, CallType.VOICE, listOf(user.uid))
                                            }) {
                                                Icon(Icons.Default.Call, contentDescription = "Voice call ${user.displayName}", tint = OrbitEmerald)
                                            }
                                            IconButton(onClick = {
                                                viewModel.startCall(currentUserName, user.displayName, CallType.VIDEO, listOf(user.uid))
                                            }) {
                                                Icon(Icons.Default.Videocam, contentDescription = "Video call ${user.displayName}", tint = OrbitCyan)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Recent Call History", style = MaterialTheme.typography.titleMedium)
            }
            when (val hState = callHistoryState) {
                CloudListState.Loading -> item { Text("Loading call history...") }
                is CloudListState.Error -> item { Text(hState.message, color = MaterialTheme.colorScheme.error) }
                is CloudListState.Success -> {
                    if (hState.items.isEmpty()) {
                        item {
                            Text(
                                text = "No call history recorded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(hState.items, key = { it.id }) { entry ->
                            ListItem(
                                headlineContent = { Text(entry.peerName) },
                                supportingContent = { Text("${entry.callType} • ${entry.direction} • ${entry.durationSec}s") },
                                leadingContent = {
                                    Icon(
                                        imageVector = if (entry.callType.contains("VIDEO")) Icons.Default.Videocam else Icons.Default.Call,
                                        contentDescription = null,
                                        tint = OrbitCyan
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGroupCallDialog) {
        val contacts = (contactsState as? CloudListState.Success)?.items ?: emptyList()
        var roomTitle by remember { mutableStateOf("Orbit Group Room") }
        val selected = remember { mutableStateListOf<PublicUserProfile>() }
        var isVideoRoom by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showGroupCallDialog = false },
            title = { Text("Start Group Call (Max 8)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = roomTitle,
                        onValueChange = { roomTitle = it },
                        label = { Text("Room Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isVideoRoom, onCheckedChange = { isVideoRoom = it })
                        Text("Enable Video Call")
                    }
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(contacts, key = { it.uid }) { user ->
                            val isSel = selected.any { it.uid == user.uid }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSel) selected.removeAll { it.uid == user.uid }
                                        else if (selected.size < 7) selected.add(user)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(checked = isSel, onCheckedChange = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${user.displayName} (@${user.username})")
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupCallDialog = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    enabled = selected.isNotEmpty(),
                    onClick = {
                        viewModel.startCall(
                            callerName = currentUserName,
                            title = roomTitle,
                            callType = if (isVideoRoom) CallType.GROUP_VIDEO else CallType.GROUP_VOICE,
                            calleeUids = selected.map { it.uid }
                        )
                        showGroupCallDialog = false
                    }
                ) { Text("Start Call") }
            }
        )
    }
}

@Composable
private fun ActiveCallStage(
    call: CallSession,
    viewModel: CommunicationViewModel,
    currentUserId: String
) {
    val context = LocalContext.current
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isSpeaker by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    val isCameraOn by viewModel.isCameraOn.collectAsStateWithLifecycle()
    val useFrontCamera by viewModel.useFrontCamera.collectAsStateWithLifecycle()
    val isScreenSharing by viewModel.isScreenSharing.collectAsStateWithLifecycle()
    val durationSec by viewModel.callDurationSec.collectAsStateWithLifecycle()

    var hasCamMicPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        hasCamMicPerm = map.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!hasCamMicPerm) {
            permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    DisposableEffect(isSpeaker, isMuted) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.apply {
            mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            isSpeakerphoneOn = isSpeaker
            isMicrophoneMute = isMuted
        }
        onDispose {
            audioManager?.apply {
                mode = AudioManager.MODE_NORMAL
                isMicrophoneMute = false
            }
        }
    }

    val formattedTime = remember(durationSec) {
        val mins = durationSec / 60
        val secs = durationSec % 60
        "%02d:%02d".format(mins, secs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrbitVoid)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Call Header
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(call.title.ifEmpty { call.callerName }, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    isScreenSharing -> "Screen Sharing Active • $formattedTime"
                    call.status == "RINGING" -> "Ringing (${call.participantUids.size} invited)..."
                    else -> "${call.callType} • $formattedTime • ${call.activeUids.size.coerceAtLeast(1)}/${call.participantUids.size} connected"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = OrbitCyan
            )
        }

        // Participant Grid + CameraX Preview
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (call.participantUids.size <= 2) 1 else 2),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).padding(vertical = 16.dp)
        ) {
            items(call.participantUids) { uid ->
                val isSelf = uid == currentUserId
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = OrbitSurface),
                    modifier = Modifier.fillMaxWidth().height(if (call.participantUids.size <= 2) 220.dp else 160.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isSelf && isCameraOn && hasCamMicPerm) {
                            CameraXPreview(useFrontCamera = useFrontCamera)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier.size(56.dp).clip(CircleShape).background(OrbitViolet.copy(0.24f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = OrbitCyan, modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isSelf) "You" else "Participant ${uid.take(6)}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // Call Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallControlButton(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                active = isMuted,
                onClick = { viewModel.toggleMute() }
            )
            CallControlButton(
                icon = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                label = "Camera",
                active = isCameraOn,
                onClick = { viewModel.toggleCamera() }
            )
            CallControlButton(
                icon = Icons.Default.Cameraswitch,
                label = "Flip",
                active = false,
                onClick = { viewModel.flipCamera() }
            )
            CallControlButton(
                icon = Icons.Default.VolumeUp,
                label = "Speaker",
                active = isSpeaker,
                onClick = { viewModel.toggleSpeaker() }
            )
            CallControlButton(
                icon = if (isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                label = "Share",
                active = isScreenSharing,
                onClick = { viewModel.toggleScreenShare() }
            )
            IconButton(
                onClick = { viewModel.declineOrEndCall(call, decline = false) },
                modifier = Modifier.size(56.dp).clip(CircleShape).background(OrbitRose)
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = "End call", tint = Color.White)
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (active) OrbitCyan else OrbitSurface)
        ) {
            Icon(icon, contentDescription = label, tint = if (active) OrbitVoid else Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun CameraXPreview(useFrontCamera: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(useFrontCamera) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val cameraProvider = future.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
fun IncomingCallOverlay(
    call: CallSession,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(
        color = OrbitSurface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Incoming ${call.callType} Call", style = MaterialTheme.typography.labelLarge, color = OrbitCyan)
                Text(call.callerName, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(call.title, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCBD5E1))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrbitRose)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Decline call")
                }
                Button(
                    onClick = onAnswer,
                    colors = ButtonDefaults.buttonColors(containerColor = OrbitEmerald, contentColor = OrbitVoid)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Answer call")
                }
            }
        }
    }
}
