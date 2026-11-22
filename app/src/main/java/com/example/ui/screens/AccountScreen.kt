package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.signOut
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitEmerald
import com.example.ui.theme.OrbitRose
import com.example.viewmodel.AccountUiState
import com.example.viewmodel.AccountViewModel
import com.example.viewmodel.BrowserViewModel

@Composable
fun AccountScreen(
    accountViewModel: AccountViewModel,
    browserViewModel: BrowserViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val accountState by accountViewModel.accountState.collectAsStateWithLifecycle()
    val feedback by accountViewModel.feedbackMessage.collectAsStateWithLifecycle()
    val browserPrefs by browserViewModel.preferences.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            accountViewModel.clearFeedback()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Account & Settings", style = MaterialTheme.typography.titleLarge)
                    if (accountState is AccountUiState.Success) {
                        OutlinedButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val state = accountState) {
                AccountUiState.Loading -> Text("Syncing account profile...")
                is AccountUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                is AccountUiState.Success -> {
                    val acct = state.account
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!acct?.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = acct?.photoUrl,
                                    contentDescription = "Profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp).clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(64.dp).clip(CircleShape).background(OrbitCyan.copy(0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = OrbitCyan, modifier = Modifier.size(34.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = acct?.displayName ?: "Orbit Explorer",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.Verified, contentDescription = "Verified Google Account", tint = OrbitEmerald, modifier = Modifier.size(18.dp))
                                }
                                Text("@${acct?.username ?: "orbit"} • ${acct?.status ?: "Online"}", color = OrbitCyan, style = MaterialTheme.typography.labelLarge)
                                if (!acct?.email.isNullOrBlank()) {
                                    Text(acct?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!acct?.phoneNumber.isNullOrBlank()) {
                                    Text(acct?.phoneNumber ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(acct?.bio ?: "", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (showEditDialog && acct != null) {
                        var name by remember { mutableStateOf(acct.displayName) }
                        var username by remember { mutableStateOf(acct.username) }
                        var bio by remember { mutableStateOf(acct.bio) }
                        var status by remember { mutableStateOf(acct.status) }
                        var phone by remember { mutableStateOf(acct.phoneNumber) }
                        var photoUrl by remember { mutableStateOf(acct.photoUrl ?: "") }

                        AlertDialog(
                            onDismissRequest = { showEditDialog = false },
                            title = { Text("Edit Profile") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true)
                                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true)
                                    OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status") }, singleLine = true)
                                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, maxLines = 3)
                                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Phone") }, singleLine = true)
                                    OutlinedTextField(value = photoUrl, onValueChange = { photoUrl = it }, label = { Text("Avatar Photo URL") }, singleLine = true)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    accountViewModel.updateProfile(name, username, bio, status, phone, photoUrl)
                                    showEditDialog = false
                                }) { Text("Save") }
                            }
                        )
                    }
                }
            }

            // Browser Preferences Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Browser Engine Settings", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Web Dark Mode", style = MaterialTheme.typography.bodyLarge)
                            Text("Prefer dark color scheme on websites", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = browserPrefs.darkModeWeb, onCheckedChange = { browserViewModel.toggleWebDarkMode() })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Media Autoplay", style = MaterialTheme.typography.bodyLarge)
                            Text("Allow videos to start automatically", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = browserPrefs.autoplayMedia, onCheckedChange = { browserViewModel.toggleAutoplayMedia() })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Audio", style = MaterialTheme.typography.bodyLarge)
                            Text("Keep web audio playing while switching tabs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = browserPrefs.backgroundAudio, onCheckedChange = { browserViewModel.toggleBackgroundAudio() })
                    }
                }
            }

            // Session Actions
            Button(
                onClick = { signOut(credentialManager, scope) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }

            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OrbitRose),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Account")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Permanently?") },
            text = { Text("This deletes your Orbit profile and Firebase account. This action cannot be undone.") },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = OrbitRose),
                    onClick = {
                        showDeleteConfirm = false
                        accountViewModel.deleteAccount {
                            signOut(credentialManager, scope)
                        }
                    }
                ) { Text("Delete") }
            }
        )
    }
}
