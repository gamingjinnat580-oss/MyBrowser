package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ORBIT_HOME_URL
import com.example.model.SearchEngine
import com.example.model.ShareToChatPayload
import com.example.ui.theme.OrbitCyan
import com.example.ui.theme.OrbitViolet
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.CloudListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onAskAiAboutPage: (prefillPrompt: String?) -> Unit,
    onShareToChat: (ShareToChatPayload) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val navRequest by viewModel.navigationRequest.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val webController = remember { WebViewController() }
    val snackbarHostState = remember { SnackbarHostState() }

    var addressInput by remember(activeTab.id, activeTab.url) {
        mutableStateOf(if (activeTab.url == ORBIT_HOME_URL) "" else activeTab.url)
    }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var showLibrarySheet by remember { mutableStateOf(false) }
    var showDownloadsSheet by remember { mutableStateOf(false) }
    var showEngineDialog by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Back button navigates inside WebView first
    BackHandler(enabled = activeTab.url != ORBIT_HOME_URL) {
        if (activeTab.canGoBack) {
            webController.goBack(activeTab.id)
        } else {
            viewModel.openUrlInActiveTab(ORBIT_HOME_URL)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (activeTab.isIncognito) Color(0xFF1A0F33) else MaterialTheme.colorScheme.surface
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.openUrlInActiveTab(ORBIT_HOME_URL) },
                        modifier = Modifier.testTag("browser_home_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Browser Home",
                            tint = if (activeTab.isIncognito) OrbitViolet else OrbitCyan
                        )
                    }

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        placeholder = {
                            Text(
                                text = if (activeTab.isIncognito) "Incognito search or URL" else "Search ${prefs.searchEngine.displayName} or enter URL",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (activeTab.isIncognito) Icons.Default.Security else Icons.Default.Lock,
                                contentDescription = "Security state",
                                tint = if (activeTab.isIncognito) OrbitViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (activeTab.isLoading) {
                                IconButton(onClick = { webController.stop(activeTab.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Stop loading")
                                }
                            } else if (addressInput.isNotEmpty() && activeTab.url != ORBIT_HOME_URL) {
                                IconButton(onClick = { webController.reload(activeTab.id) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reload page")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                viewModel.openUrlInActiveTab(addressInput)
                            }
                        ),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (activeTab.isIncognito) OrbitViolet else OrbitCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("browser_address_bar")
                    )

                    // Tab Switcher Pill
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .minimumInteractiveComponentSize()
                            .clickable { showTabSwitcher = true }
                            .testTag("browser_tabs_btn")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${tabs.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("browser_menu_btn")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Browser tools")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Back") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                                enabled = activeTab.canGoBack && activeTab.url != ORBIT_HOME_URL,
                                onClick = { webController.goBack(activeTab.id); showOverflowMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Forward") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                                enabled = activeTab.canGoForward && activeTab.url != ORBIT_HOME_URL,
                                onClick = { webController.goForward(activeTab.id); showOverflowMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Ask Orbit AI about Page") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    val prompt = if (activeTab.url.startsWith("http")) {
                                        "Summarize and explain key points from ${activeTab.title} (${activeTab.url})"
                                    } else null
                                    onAskAiAboutPage(prompt)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share to Orbit Chat") },
                                leadingIcon = { Icon(Icons.Default.Send, contentDescription = null) },
                                enabled = activeTab.url.startsWith("http"),
                                onClick = {
                                    showOverflowMenu = false
                                    onShareToChat(ShareToChatPayload(activeTab.title, activeTab.url))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Externally") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                enabled = activeTab.url.startsWith("http"),
                                onClick = {
                                    showOverflowMenu = false
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, activeTab.title)
                                        putExtra(Intent.EXTRA_TEXT, activeTab.url)
                                    }
                                    context.startActivity(Intent.createChooser(send, "Share Web Page"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bookmark Page") },
                                leadingIcon = { Icon(Icons.Default.BookmarkAdd, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.bookmarkCurrentPage(activeTab.title, activeTab.url)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Bookmarks & History") },
                                leadingIcon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                                onClick = { showOverflowMenu = false; showLibrarySheet = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Downloads") },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.refreshDownloads(context)
                                    showDownloadsSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Find in Page") },
                                leadingIcon = { Icon(Icons.Default.FindInPage, contentDescription = null) },
                                enabled = activeTab.url != ORBIT_HOME_URL,
                                onClick = { showOverflowMenu = false; showFindBar = !showFindBar }
                            )
                            DropdownMenuItem(
                                text = { Text(if (activeTab.desktopMode) "Mobile Site" else "Desktop Site") },
                                leadingIcon = { Icon(Icons.Default.DesktopWindows, contentDescription = null) },
                                enabled = activeTab.url != ORBIT_HOME_URL,
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleDesktopMode(activeTab.id)
                                    webController.reload(activeTab.id)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (prefs.darkModeWeb) "Web Dark Mode: ON" else "Web Dark Mode: OFF") },
                                leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleWebDarkMode()
                                    if (activeTab.url != ORBIT_HOME_URL) webController.reload(activeTab.id)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Search Engine (${prefs.searchEngine.displayName})") },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                onClick = { showOverflowMenu = false; showEngineDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Print Page") },
                                leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                enabled = activeTab.url != ORBIT_HOME_URL,
                                onClick = {
                                    showOverflowMenu = false
                                    webController.printCurrentPage(context, activeTab.id, activeTab.title)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Incognito Tab") },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.openNewTab(ORBIT_HOME_URL, incognito = true)
                                }
                            )
                        }
                    }
                }

                // Find in Page Bar
                AnimatedVisibility(visible = showFindBar && activeTab.url != ORBIT_HOME_URL) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = findQuery,
                            onValueChange = {
                                findQuery = it
                                webController.findInPage(activeTab.id, it)
                            },
                            placeholder = { Text("Find in page...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { webController.findNext(activeTab.id, false) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
                        }
                        IconButton(onClick = { webController.findNext(activeTab.id, true) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
                        }
                        IconButton(onClick = {
                            showFindBar = false
                            findQuery = ""
                            webController.clearFind(activeTab.id)
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close find")
                        }
                    }
                }

                if (activeTab.isLoading && activeTab.url != ORBIT_HOME_URL) {
                    LinearProgressIndicator(
                        progress = { (activeTab.progress / 100f).coerceIn(0.05f, 1f) },
                        color = if (activeTab.isIncognito) OrbitViolet else OrbitCyan,
                        modifier = Modifier.fillMaxWidth().height(2.5.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeTab.url == ORBIT_HOME_URL) {
                BrowserStartPage(
                    isIncognito = activeTab.isIncognito,
                    selectedEngine = prefs.searchEngine,
                    onOpenUrl = { viewModel.openUrlInActiveTab(it) },
                    onOpenEnginePicker = { showEngineDialog = true },
                    onOpenAiAssistant = onAskAiAboutPage
                )
            } else {
                BrowserWebViewHost(
                    activeTab = activeTab,
                    allTabIds = tabs.map { it.id }.toSet(),
                    controller = webController,
                    navigationRequest = navRequest,
                    darkModeWeb = prefs.darkModeWeb,
                    autoplayMedia = prefs.autoplayMedia,
                    onConsumeNavigationRequest = { viewModel.consumeNavigationRequest() },
                    onUpdateTab = { id, url, title, loading, prog, back, fwd, err, failed, clearErr ->
                        viewModel.updateTabState(id, url, title, loading, prog, back, fwd, err, failed, clearErr)
                    },
                    onOpenNewTab = { url, incognito -> viewModel.openNewTab(url, incognito) },
                    onRecordVisit = { id, title, url -> viewModel.recordPageVisit(id, title, url) },
                    onDownload = { url, ua, cd, mime -> viewModel.enqueueDownload(context, url, ua, cd, mime) },
                    onShareToChat = onShareToChat,
                    onShowMessage = { msg ->
                        // Show snackbar safely
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onGoHome = { viewModel.openUrlInActiveTab(ORBIT_HOME_URL) }
                )
            }
        }
    }

    // Tab Switcher Bottom Sheet
    if (showTabSwitcher) {
        ModalBottomSheet(onDismissRequest = { showTabSwitcher = false }) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Open Tabs (${tabs.size})", style = MaterialTheme.typography.titleLarge)
                    Row {
                        TextButton(onClick = {
                            viewModel.openNewTab(ORBIT_HOME_URL, incognito = true)
                            showTabSwitcher = false
                        }) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Incognito")
                        }
                        Button(onClick = {
                            viewModel.openNewTab(ORBIT_HOME_URL, incognito = false)
                            showTabSwitcher = false
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Tab")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(340.dp)
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        val selected = tab.id == activeTab.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    tab.isIncognito && selected -> Color(0xFF2D1B54)
                                    tab.isIncognito -> Color(0xFF1A0F33)
                                    selected -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchTab(tab.id)
                                    showTabSwitcher = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (tab.isIncognito) Icons.Default.Security else Icons.Default.Tab,
                                    contentDescription = null,
                                    tint = if (tab.isIncognito) OrbitViolet else OrbitCyan
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tab.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { viewModel.closeTab(tab.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close tab")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Bookmarks & History Sheet
    if (showLibrarySheet) {
        var libraryTab by remember { mutableIntStateOf(0) }
        ModalBottomSheet(onDismissRequest = { showLibrarySheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().height(440.dp)) {
                TabRow(selectedTabIndex = libraryTab) {
                    Tab(
                        selected = libraryTab == 0,
                        onClick = { libraryTab = 0 },
                        text = { Text("Bookmarks") },
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = null) }
                    )
                    Tab(
                        selected = libraryTab == 1,
                        onClick = { libraryTab = 1 },
                        text = { Text("History") },
                        icon = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                }
                if (libraryTab == 0) {
                    when (val state = bookmarksState) {
                        CloudListState.Loading -> Text("Loading bookmarks...", Modifier.padding(20.dp))
                        is CloudListState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp))
                        is CloudListState.Success -> {
                            if (state.items.isEmpty()) {
                                Text("No bookmarks saved yet. Tap menu -> Bookmark Page while browsing.", Modifier.padding(20.dp))
                            } else {
                                LazyColumn {
                                    items(state.items, key = { it.id }) { bm ->
                                        ListItem(
                                            headlineContent = { Text(bm.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            supportingContent = { Text(bm.url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            trailingContent = {
                                                IconButton(onClick = { viewModel.deleteBookmark(bm.id) }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Delete bookmark")
                                                }
                                            },
                                            modifier = Modifier.clickable {
                                                viewModel.openUrlInActiveTab(bm.url)
                                                showLibrarySheet = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { viewModel.clearAllBrowsingData() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Browsing Data")
                        }
                    }
                    when (val state = historyState) {
                        CloudListState.Loading -> Text("Loading history...", Modifier.padding(20.dp))
                        is CloudListState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(20.dp))
                        is CloudListState.Success -> {
                            if (state.items.isEmpty()) {
                                Text("Browsing history is empty.", Modifier.padding(20.dp))
                            } else {
                                LazyColumn {
                                    items(state.items, key = { it.id }) { entry ->
                                        ListItem(
                                            headlineContent = { Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            supportingContent = { Text(entry.url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            modifier = Modifier.clickable {
                                                viewModel.openUrlInActiveTab(entry.url)
                                                showLibrarySheet = false
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

    // Downloads Manager Sheet
    if (showDownloadsSheet) {
        ModalBottomSheet(onDismissRequest = { showDownloadsSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().height(380.dp).padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Downloads", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { viewModel.refreshDownloads(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh downloads")
                    }
                }
                if (downloads.isEmpty()) {
                    Text(
                        text = "No downloads yet. Files, images, audio, and videos saved from websites appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn {
                        items(downloads, key = { it.id }) { dl ->
                            ListItem(
                                headlineContent = { Text(dl.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text("${dl.statusText} • ${dl.bytesDownloaded / 1024} KB") },
                                leadingContent = { Icon(Icons.Default.Download, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Search Engine Selector Dialog
    if (showEngineDialog) {
        AlertDialog(
            onDismissRequest = { showEngineDialog = false },
            title = { Text("Select Search Engine") },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(SearchEngine.entries) { engine ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSearchEngine(engine)
                                    showEngineDialog = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = prefs.searchEngine == engine,
                                onClick = {
                                    viewModel.setSearchEngine(engine)
                                    showEngineDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(engine.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEngineDialog = false }) { Text("Done") }
            }
        )
    }
}
