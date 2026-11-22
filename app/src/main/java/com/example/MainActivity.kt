package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.data.AiAssistantRepository
import com.example.data.BrowserCloudRepository
import com.example.data.BrowserPreferencesRepository
import com.example.data.CommunicationRepository
import com.example.data.UserRepository
import com.example.model.MessageType
import com.example.model.ShareToChatPayload
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.IncomingCallOverlay
import com.example.ui.screens.MessagesScreen
import com.example.ui.theme.OrbitBrowserTheme
import com.example.viewmodel.AccountUiState
import com.example.viewmodel.AccountViewModel
import com.example.viewmodel.AiViewModel
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.CloudListState
import com.example.viewmodel.CommunicationViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class OrbitTab(val label: String, val icon: ImageVector) {
    BROWSER("Browser", Icons.Default.Public),
    AI("Orbit AI", Icons.Default.AutoAwesome),
    MESSAGES("Chat", Icons.Default.Chat),
    CALLS("Calls", Icons.Default.Call),
    ACCOUNT("Account", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            OrbitBrowserTheme {
                OrbitAppNavigation()
            }
        }
    }
}

@Composable
fun OrbitAppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener updates automatically */ })
    } else {
        key(user.uid) {
            OrbitAuthenticatedWorkspace(
                currentUserId = user.uid,
                firebaseUser = user
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrbitAuthenticatedWorkspace(
    currentUserId: String,
    firebaseUser: FirebaseUser
) {
    val browserViewModel: BrowserViewModel = viewModel(
        key = "browser_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                BrowserViewModel(
                    cloudRepo = BrowserCloudRepository(db),
                    prefsRepo = BrowserPreferencesRepository(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val aiViewModel: AiViewModel = viewModel(
        key = "ai_$currentUserId",
        factory = viewModelFactory {
            initializer { AiViewModel(AiAssistantRepository()) }
        }
    )

    val commViewModel: CommunicationViewModel = viewModel(
        key = "comm_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                CommunicationViewModel(
                    commRepo = CommunicationRepository(db),
                    userRepo = UserRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val accountViewModel: AccountViewModel = viewModel(
        key = "account_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                AccountViewModel(
                    userRepo = UserRepository(db),
                    currentUserId = currentUserId,
                    firebaseUser = firebaseUser
                )
            }
        }
    )

    var selectedTab by rememberSaveable { mutableStateOf(OrbitTab.BROWSER) }
    var pendingAiPrompt by remember { mutableStateOf<String?>(null) }
    var pendingSharePayload by remember { mutableStateOf<ShareToChatPayload?>(null) }

    val browserTabs by browserViewModel.tabs.collectAsStateWithLifecycle()
    val activeBrowserTabId by browserViewModel.activeTabId.collectAsStateWithLifecycle()
    val activeBrowserTab = browserTabs.firstOrNull { it.id == activeBrowserTabId } ?: browserTabs.first()

    val accountState by accountViewModel.accountState.collectAsStateWithLifecycle()
    val currentUserName = (accountState as? AccountUiState.Success)?.account?.displayName
        ?.takeIf { it.isNotBlank() }
        ?: firebaseUser.displayName
        ?: "Orbit Explorer"

    val activeCalls by commViewModel.activeCalls.collectAsStateWithLifecycle()
    val incomingCall = activeCalls.firstOrNull {
        it.status == "RINGING" && it.callerUid != currentUserId && currentUserId in it.participantUids
    }

    val conversationsState by commViewModel.conversationsState.collectAsStateWithLifecycle()
    val contactsState by commViewModel.contactsState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = selectedTab != OrbitTab.BROWSER) {
        selectedTab = OrbitTab.BROWSER
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                        OrbitTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        Spacer(modifier = Modifier.height(12.dp))
                        OrbitTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        OrbitTab.BROWSER -> BrowserScreen(
                            viewModel = browserViewModel,
                            onAskAiAboutPage = { prompt ->
                                pendingAiPrompt = prompt
                                selectedTab = OrbitTab.AI
                            },
                            onShareToChat = { payload ->
                                pendingSharePayload = payload
                            }
                        )
                        OrbitTab.AI -> AiAssistantScreen(
                            viewModel = aiViewModel,
                            activePageTitle = activeBrowserTab.title,
                            activePageUrl = activeBrowserTab.url,
                            prefillQuestion = pendingAiPrompt,
                            onConsumePrefill = { pendingAiPrompt = null },
                            onShareAnswerToChat = { payload ->
                                pendingSharePayload = payload
                            }
                        )
                        OrbitTab.MESSAGES -> MessagesScreen(
                            viewModel = commViewModel,
                            currentUserId = currentUserId,
                            currentUserName = currentUserName,
                            onOpenUrlInBrowser = { url ->
                                browserViewModel.openUrlInActiveTab(url)
                                selectedTab = OrbitTab.BROWSER
                            },
                            onCallStarted = {
                                selectedTab = OrbitTab.CALLS
                            }
                        )
                        OrbitTab.CALLS -> CallsScreen(
                            viewModel = commViewModel,
                            currentUserId = currentUserId,
                            currentUserName = currentUserName
                        )
                        OrbitTab.ACCOUNT -> AccountScreen(
                            accountViewModel = accountViewModel,
                            browserViewModel = browserViewModel
                        )
                    }

                    // Global Incoming Call Banner
                    incomingCall?.let { call ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                        ) {
                            IncomingCallOverlay(
                                call = call,
                                onAnswer = {
                                    commViewModel.answerIncomingCall(call)
                                    selectedTab = OrbitTab.CALLS
                                },
                                onDecline = {
                                    commViewModel.declineOrEndCall(call, decline = true)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Share-to-Chat Sheet (from Browser page, long-press image/link, or Orbit AI)
    pendingSharePayload?.let { payload ->
        val convList = (conversationsState as? CloudListState.Success)?.items ?: emptyList()
        val contactList = (contactsState as? CloudListState.Success)?.items ?: emptyList()

        ModalBottomSheet(onDismissRequest = { pendingSharePayload = null }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Share to Orbit Chat", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${payload.title} • ${payload.url}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    if (convList.isNotEmpty()) {
                        item {
                            Text("Recent Chats", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 6.dp))
                        }
                        items(convList, key = { it.id }) { conv ->
                            ListItem(
                                headlineContent = { Text(conv.title.ifEmpty { "Chat" }) },
                                supportingContent = { Text(conv.lastMessageText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.clickable {
                                    val msgType = when {
                                        payload.isImage -> MessageType.IMAGE
                                        payload.url.startsWith("http") -> MessageType.WEB_SHARE
                                        else -> MessageType.TEXT
                                    }
                                    commViewModel.sendMessage(
                                        conversation = conv,
                                        senderName = currentUserName,
                                        messageType = msgType,
                                        text = if (msgType == MessageType.TEXT) payload.url else payload.title,
                                        mediaUrl = if (msgType == MessageType.TEXT) null else payload.url,
                                        sharedTitle = payload.title
                                    )
                                    commViewModel.selectConversation(conv.id)
                                    pendingSharePayload = null
                                    selectedTab = OrbitTab.MESSAGES
                                }
                            )
                        }
                    }
                    if (contactList.isNotEmpty()) {
                        item {
                            Text("Contacts", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 6.dp))
                        }
                        items(contactList, key = { it.uid }) { user ->
                            ListItem(
                                headlineContent = { Text(user.displayName) },
                                supportingContent = { Text("@${user.username}") },
                                modifier = Modifier.clickable {
                                    scope.launch {
                                        commViewModel.startDirectChat(currentUserName, user)
                                        pendingSharePayload = null
                                        selectedTab = OrbitTab.MESSAGES
                                    }
                                }
                            )
                        }
                    }
                    if (convList.isEmpty() && contactList.isEmpty()) {
                        item {
                            Text(
                                text = "No chats or contacts available yet. Open Chat -> Contacts once another user signs in.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
