package com.example.viewmodel

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebStorage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.friendlyFirestoreMessage
import com.example.data.BrowserCloudRepository
import com.example.data.BrowserPreferences
import com.example.data.BrowserPreferencesRepository
import com.example.model.BrowserBookmark
import com.example.model.BrowserHistoryEntry
import com.example.model.BrowserTab
import com.example.model.DownloadItem
import com.example.model.ORBIT_HOME_URL
import com.example.model.SearchEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder

sealed interface CloudListState<out T> {
    data object Loading : CloudListState<Nothing>
    data class Success<T>(val items: T) : CloudListState<T>
    data class Error(val message: String) : CloudListState<Nothing>
}

class BrowserViewModel(
    private val cloudRepo: BrowserCloudRepository,
    private val prefsRepo: BrowserPreferencesRepository,
    private val currentUserId: String
) : ViewModel() {

    private val initialTab = BrowserTab()
    private val _tabs = MutableStateFlow(listOf(initialTab))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTab.id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _navigationRequest = MutableStateFlow<Pair<String, String>?>(null)
    val navigationRequest: StateFlow<Pair<String, String>?> = _navigationRequest.asStateFlow()

    val preferences: StateFlow<BrowserPreferences> = prefsRepo.preferences
        .catch { e ->
            if (e is CancellationException) throw e
            Log.w(TAG, "Prefs error", e)
            emit(BrowserPreferences())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BrowserPreferences())

    val bookmarksState: StateFlow<CloudListState<List<BrowserBookmark>>> =
        cloudRepo.observeBookmarks(currentUserId)
            .map<List<BrowserBookmark>, CloudListState<List<BrowserBookmark>>> { CloudListState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Bookmarks stream error", root)
                emit(CloudListState.Error(friendlyFirestoreMessage(root, "Unable to load bookmarks")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CloudListState.Loading)

    val historyState: StateFlow<CloudListState<List<BrowserHistoryEntry>>> =
        cloudRepo.observeHistory(currentUserId)
            .map<List<BrowserHistoryEntry>, CloudListState<List<BrowserHistoryEntry>>> { CloudListState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "History stream error", root)
                emit(CloudListState.Error(friendlyFirestoreMessage(root, "Unable to load history")))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CloudListState.Loading)

    fun activeTab(): BrowserTab =
        _tabs.value.firstOrNull { it.id == _activeTabId.value } ?: _tabs.value.first()

    fun resolveInputToUrl(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ORBIT_HOME_URL
        if (trimmed.equals("orbit://home", ignoreCase = true)) return ORBIT_HOME_URL
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        if (!trimmed.contains(" ") && trimmed.contains(".") && !trimmed.endsWith(".")) {
            return "https://$trimmed"
        }
        val engine = preferences.value.searchEngine
        val encoded = try {
            URLEncoder.encode(trimmed, "UTF-8")
        } catch (_: Exception) {
            trimmed
        }
        return "${engine.searchUrlTemplate}$encoded"
    }

    fun openUrlInActiveTab(rawInput: String) {
        val resolved = resolveInputToUrl(rawInput)
        val tabId = activeTab().id
        updateTab(tabId) {
            it.copy(
                url = resolved,
                title = if (resolved == ORBIT_HOME_URL) "Orbit Start" else resolved,
                errorDescription = null,
                failedUrl = null
            )
        }
        _navigationRequest.value = tabId to resolved
    }

    fun consumeNavigationRequest() {
        _navigationRequest.value = null
    }

    fun openNewTab(url: String = ORBIT_HOME_URL, incognito: Boolean = false) {
        val newTab = BrowserTab(
            url = url,
            title = if (url == ORBIT_HOME_URL) (if (incognito) "Incognito Tab" else "Orbit Start") else url,
            isIncognito = incognito
        )
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        if (url != ORBIT_HOME_URL) {
            _navigationRequest.value = newTab.id to url
        }
    }

    fun switchTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
        }
    }

    fun closeTab(tabId: String) {
        val currentList = _tabs.value
        if (currentList.size <= 1) {
            val replacement = BrowserTab(isIncognito = currentList.first().isIncognito)
            _tabs.value = listOf(replacement)
            _activeTabId.value = replacement.id
            return
        }
        val idx = currentList.indexOfFirst { it.id == tabId }
        val updated = currentList.filterNot { it.id == tabId }
        _tabs.value = updated
        if (_activeTabId.value == tabId) {
            val nextIdx = (idx - 1).coerceIn(0, updated.lastIndex)
            _activeTabId.value = updated[nextIdx].id
        }
    }

    fun updateTabState(
        tabId: String,
        url: String? = null,
        title: String? = null,
        isLoading: Boolean? = null,
        progress: Int? = null,
        canGoBack: Boolean? = null,
        canGoForward: Boolean? = null,
        errorDescription: String? = null,
        failedUrl: String? = null,
        clearError: Boolean = false
    ) {
        updateTab(tabId) { tab ->
            tab.copy(
                url = url ?: tab.url,
                title = title?.takeIf { it.isNotBlank() } ?: tab.title,
                isLoading = isLoading ?: tab.isLoading,
                progress = progress ?: tab.progress,
                canGoBack = canGoBack ?: tab.canGoBack,
                canGoForward = canGoForward ?: tab.canGoForward,
                errorDescription = if (clearError) null else (errorDescription ?: tab.errorDescription),
                failedUrl = if (clearError) null else (failedUrl ?: tab.failedUrl)
            )
        }
    }

    fun toggleDesktopMode(tabId: String) {
        updateTab(tabId) { it.copy(desktopMode = !it.desktopMode) }
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) transform(it) else it }
        }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch {
            prefsRepo.setSearchEngine(engine)
            _actionMessage.value = "Default search set to ${engine.displayName}"
        }
    }

    fun toggleWebDarkMode() {
        viewModelScope.launch {
            val next = !preferences.value.darkModeWeb
            prefsRepo.setDarkModeWeb(next)
            _actionMessage.value = if (next) "Web Dark Mode enabled" else "Web Dark Mode disabled"
        }
    }

    fun toggleAutoplayMedia() {
        viewModelScope.launch {
            prefsRepo.setAutoplayMedia(!preferences.value.autoplayMedia)
        }
    }

    fun toggleBackgroundAudio() {
        viewModelScope.launch {
            prefsRepo.setBackgroundAudio(!preferences.value.backgroundAudio)
        }
    }

    fun bookmarkCurrentPage(title: String, url: String, category: String = "General") {
        if (!url.startsWith("http")) {
            _actionMessage.value = "Open a webpage first to bookmark it."
            return
        }
        viewModelScope.launch {
            val res = cloudRepo.addBookmark(currentUserId, title, url, category)
            _actionMessage.value = if (res.isSuccess) {
                "Bookmarked ${title.take(30)}"
            } else {
                res.exceptionOrNull()?.let { friendlyFirestoreMessage(it, "Could not save bookmark") }
            }
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            val res = cloudRepo.deleteBookmark(currentUserId, bookmarkId)
            if (res.isFailure) {
                _actionMessage.value = res.exceptionOrNull()?.let {
                    friendlyFirestoreMessage(it, "Could not delete bookmark")
                }
            }
        }
    }

    fun recordPageVisit(tabId: String, title: String, url: String) {
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        if (tab.isIncognito) return
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        viewModelScope.launch {
            cloudRepo.recordHistory(currentUserId, title, url)
        }
    }

    fun clearAllBrowsingData() {
        viewModelScope.launch {
            try {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
            } catch (e: Exception) {
                Log.w(TAG, "Local WebView storage clear warning", e)
            }
            val res = cloudRepo.clearHistory(currentUserId)
            _actionMessage.value = if (res.isSuccess) {
                "Cleared cookies, site storage, and browsing history."
            } else {
                "Cleared local cookies; cloud history clear failed."
            }
        }
    }

    fun enqueueDownload(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm == null) {
                _actionMessage.value = "Download service unavailable on this device."
                return
            }
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                if (!userAgent.isNullOrBlank()) addRequestHeader("User-Agent", userAgent)
                setTitle(fileName)
                setDescription("Downloading via Orbit Browser")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
            dm.enqueue(request)
            _actionMessage.value = "Downloading $fileName..."
            refreshDownloads(context)
        } catch (e: Exception) {
            Log.e(TAG, "Download enqueue failed", e)
            _actionMessage.value = "Download failed: ${e.localizedMessage ?: "Unsupported URL"}"
        }
    }

    fun refreshDownloads(context: Context) {
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            val cursor: Cursor = dm.query(DownloadManager.Query()) ?: return
            val list = mutableListOf<DownloadItem>()
            cursor.use { c ->
                val idIdx = c.getColumnIndex(DownloadManager.COLUMN_ID)
                val titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE)
                val uriIdx = c.getColumnIndex(DownloadManager.COLUMN_URI)
                val statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val bytesIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val localUriIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)

                while (c.moveToNext() && list.size < 50) {
                    val status = if (statusIdx >= 0) c.getInt(statusIdx) else DownloadManager.STATUS_PENDING
                    val statusLabel = when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> "Completed"
                        DownloadManager.STATUS_FAILED -> "Failed"
                        DownloadManager.STATUS_PAUSED -> "Paused"
                        DownloadManager.STATUS_RUNNING -> "Downloading"
                        else -> "Queued"
                    }
                    list += DownloadItem(
                        id = if (idIdx >= 0) c.getLong(idIdx) else 0L,
                        title = if (titleIdx >= 0) c.getString(titleIdx) ?: "Download" else "Download",
                        url = if (uriIdx >= 0) c.getString(uriIdx) ?: "" else "",
                        statusText = statusLabel,
                        bytesDownloaded = if (bytesIdx >= 0) c.getLong(bytesIdx) else 0L,
                        totalBytes = if (totalIdx >= 0) c.getLong(totalIdx) else -1L,
                        isComplete = status == DownloadManager.STATUS_SUCCESSFUL,
                        isFailed = status == DownloadManager.STATUS_FAILED,
                        localUri = if (localUriIdx >= 0) c.getString(localUriIdx) else null
                    )
                }
            }
            _downloads.value = list.sortedByDescending { it.id }
        } catch (e: Exception) {
            Log.w(TAG, "Could not query downloads", e)
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private companion object {
        const val TAG = "BrowserVM"
    }
}
