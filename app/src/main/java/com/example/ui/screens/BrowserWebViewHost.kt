package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.model.BrowserTab
import com.example.model.ORBIT_HOME_URL
import com.example.model.ShareToChatPayload

private const val DESKTOP_UA =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

private sealed interface JsDialogState {
    data class Alert(val message: String, val result: JsResult) : JsDialogState
    data class Confirm(val message: String, val result: JsResult) : JsDialogState
    data class Prompt(val message: String, val defaultVal: String, val result: JsPromptResult) : JsDialogState
}

private data class SslDialogState(val url: String, val handler: SslErrorHandler)

private data class HitMenuState(val url: String, val isImage: Boolean)

class WebViewController {
    val pool = mutableStateMapOf<String, WebView>()

    fun goBack(tabId: String) = pool[tabId]?.let { if (it.canGoBack()) it.goBack() }
    fun goForward(tabId: String) = pool[tabId]?.let { if (it.canGoForward()) it.goForward() }
    fun reload(tabId: String) = pool[tabId]?.reload()
    fun stop(tabId: String) = pool[tabId]?.stopLoading()

    fun findInPage(tabId: String, query: String) {
        val wv = pool[tabId] ?: return
        if (query.isBlank()) wv.clearMatches() else wv.findAllAsync(query)
    }

    fun findNext(tabId: String, forward: Boolean) {
        pool[tabId]?.findNext(forward)
    }

    fun clearFind(tabId: String) {
        pool[tabId]?.clearMatches()
    }

    fun printCurrentPage(context: Context, tabId: String, title: String) {
        val wv = pool[tabId] ?: return
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = wv.createPrintDocumentAdapter(title.ifBlank { "Orbit Page" })
        printManager.print("Orbit_${System.currentTimeMillis()}", adapter, PrintAttributes.Builder().build())
    }

    fun releaseClosedTabs(activeTabIds: Set<String>) {
        val toRemove = pool.keys.filterNot { it in activeTabIds }
        toRemove.forEach { id ->
            pool.remove(id)?.apply {
                stopLoading()
                destroy()
            }
        }
    }

    fun destroyAll() {
        pool.values.forEach { wv ->
            wv.stopLoading()
            wv.destroy()
        }
        pool.clear()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebViewHost(
    activeTab: BrowserTab,
    allTabIds: Set<String>,
    controller: WebViewController,
    navigationRequest: Pair<String, String>?,
    darkModeWeb: Boolean,
    autoplayMedia: Boolean,
    onConsumeNavigationRequest: () -> Unit,
    onUpdateTab: (
        tabId: String,
        url: String?,
        title: String?,
        isLoading: Boolean?,
        progress: Int?,
        canGoBack: Boolean?,
        canGoForward: Boolean?,
        errorDesc: String?,
        failedUrl: String?,
        clearError: Boolean
    ) -> Unit,
    onOpenNewTab: (url: String, incognito: Boolean) -> Unit,
    onRecordVisit: (tabId: String, title: String, url: String) -> Unit,
    onDownload: (url: String, userAgent: String?, contentDisposition: String?, mimeType: String?) -> Unit,
    onShareToChat: (ShareToChatPayload) -> Unit,
    onShowMessage: (String) -> Unit,
    onGoHome: () -> Unit
) {
    val context = LocalContext.current

    var jsDialog by remember { mutableStateOf<JsDialogState?>(null) }
    var sslDialog by remember { mutableStateOf<SslDialogState?>(null) }
    var hitMenu by remember { mutableStateOf<HitMenuState?>(null) }
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingWebPermission by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingGeoOrigin by remember { mutableStateOf<String?>(null) }
    var pendingGeoCallback by remember { mutableStateOf<GeolocationPermissions.Callback?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        pendingFileCallback?.onReceiveValue(uris.toTypedArray())
        pendingFileCallback = null
    }

    val mediaPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val req = pendingWebPermission
        pendingWebPermission = null
        if (req != null) {
            if (results.values.all { it }) {
                req.grant(req.resources)
            } else {
                req.deny()
                onShowMessage("Camera/Microphone permission denied for this website.")
            }
        }
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val cb = pendingGeoCallback
        val origin = pendingGeoOrigin
        pendingGeoCallback = null
        pendingGeoOrigin = null
        if (cb != null && origin != null) {
            cb.invoke(origin, granted, false)
        }
    }

    LaunchedEffect(allTabIds) {
        controller.releaseClosedTabs(allTabIds)
    }

    DisposableEffect(Unit) {
        onDispose {
            controller.destroyAll()
        }
    }

    // Handle fullscreen exit via back button
    BackHandler(enabled = fullscreenView != null) {
        fullscreenCallback?.onCustomViewHidden()
        fullscreenView = null
        fullscreenCallback = null
    }

    // Load requested URLs into the right WebView
    LaunchedEffect(navigationRequest, activeTab.id) {
        val req = navigationRequest
        if (req != null && req.first == activeTab.id && req.second != ORBIT_HOME_URL) {
            controller.pool[activeTab.id]?.loadUrl(req.second)
            onConsumeNavigationRequest()
        }
    }

    // Apply dynamic WebSettings whenever desktopMode / darkMode / autoplay changes
    LaunchedEffect(activeTab.id, activeTab.desktopMode, darkModeWeb, autoplayMedia) {
        val wv = controller.pool[activeTab.id] ?: return@LaunchedEffect
        wv.settings.apply {
            mediaPlaybackRequiresUserGesture = !autoplayMedia
            userAgentString = if (activeTab.desktopMode) DESKTOP_UA else null
            useWideViewPort = activeTab.desktopMode
            loadWithOverviewMode = activeTab.desktopMode
        }
    }

    val webView = remember(activeTab.id) {
        controller.pool.getOrPut(activeTab.id) {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = false
                    allowContentAccess = true
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = !autoplayMedia
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    setGeolocationEnabled(true)
                }

                if (activeTab.isIncognito) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                } else {
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                }

                setOnLongClickListener {
                    val result = hitTestResult
                    val extra = result.extra
                    if (!extra.isNullOrBlank()) {
                        when (result.type) {
                            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                                hitMenu = HitMenuState(extra, isImage = false)
                                true
                            }
                            WebView.HitTestResult.IMAGE_TYPE,
                            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                                hitMenu = HitMenuState(extra, isImage = true)
                                true
                            }
                            else -> false
                        }
                    } else false
                }

                setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    onDownload(url, userAgent, contentDisposition, mimeType)
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url ?: return false
                        val scheme = uri.scheme?.lowercase() ?: return false
                        if (scheme == "http" || scheme == "https") {
                            // Never leave Orbit Browser for web links!
                            return false
                        }
                        return try {
                            val intent = if (scheme == "intent") {
                                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                            } else {
                                Intent(Intent.ACTION_VIEW, uri)
                            }
                            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            true
                        } catch (_: Exception) {
                            onShowMessage("No app available to open $scheme link.")
                            true
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onUpdateTab(
                            activeTab.id,
                            url,
                            view?.title,
                            true,
                            10,
                            view?.canGoBack(),
                            view?.canGoForward(),
                            null,
                            null,
                            true
                        )
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        val finalUrl = url ?: activeTab.url
                        val finalTitle = view?.title?.takeIf { it.isNotBlank() } ?: finalUrl
                        onUpdateTab(
                            activeTab.id,
                            finalUrl,
                            finalTitle,
                            false,
                            100,
                            view?.canGoBack(),
                            view?.canGoForward(),
                            null,
                            null,
                            false
                        )
                        onRecordVisit(activeTab.id, finalTitle, finalUrl)
                        if (darkModeWeb && finalUrl.startsWith("http")) {
                            view?.evaluateJavascript(
                                "(function(){if(!document.getElementById('orbit-dark-css')){var s=document.createElement('style');s.id='orbit-dark-css';s.innerHTML='html{color-scheme:dark;}';document.head&&document.head.appendChild(s);}})();",
                                null
                            )
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onUpdateTab(
                                activeTab.id,
                                null,
                                null,
                                false,
                                0,
                                view?.canGoBack(),
                                view?.canGoForward(),
                                error?.description?.toString() ?: "Failed to load webpage.",
                                request.url?.toString(),
                                false
                            )
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        if (handler != null) {
                            sslDialog = SslDialogState(
                                url = error?.url ?: activeTab.url,
                                handler = handler
                            )
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onUpdateTab(
                            activeTab.id,
                            null,
                            view?.title,
                            newProgress < 100,
                            newProgress,
                            view?.canGoBack(),
                            view?.canGoForward(),
                            null,
                            null,
                            false
                        )
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        onUpdateTab(activeTab.id, null, title, null, null, null, null, null, null, false)
                    }

                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?
                    ): Boolean {
                        val href = view?.hitTestResult?.extra
                        if (!href.isNullOrBlank()) {
                            onOpenNewTab(href, activeTab.isIncognito)
                            return true
                        }
                        val transport = resultMsg?.obj as? WebView.WebViewTransport
                        if (transport != null && view != null) {
                            transport.webView = view
                            resultMsg.sendToTarget()
                            return true
                        }
                        return false
                    }

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        fullscreenView = view
                        fullscreenCallback = callback
                    }

                    override fun onHideCustomView() {
                        fullscreenView = null
                        fullscreenCallback = null
                    }

                    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                        if (result != null) {
                            jsDialog = JsDialogState.Alert(message ?: "", result)
                            return true
                        }
                        return false
                    }

                    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                        if (result != null) {
                            jsDialog = JsDialogState.Confirm(message ?: "", result)
                            return true
                        }
                        return false
                    }

                    override fun onJsPrompt(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        defaultValue: String?,
                        result: JsPromptResult?
                    ): Boolean {
                        if (result != null) {
                            jsDialog = JsDialogState.Prompt(message ?: "", defaultValue ?: "", result)
                            return true
                        }
                        return false
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        pendingFileCallback?.onReceiveValue(null)
                        pendingFileCallback = filePathCallback
                        val mimeTypes = fileChooserParams?.acceptTypes
                            ?.filter { it.isNotBlank() }
                            ?.toTypedArray()
                            ?.takeIf { it.isNotEmpty() }
                            ?: arrayOf("*/*")
                        fileChooserLauncher.launch(mimeTypes)
                        return true
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        if (request == null) return
                        val needed = mutableListOf<String>()
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in request.resources &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                        ) {
                            needed += Manifest.permission.CAMERA
                        }
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in request.resources &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                        ) {
                            needed += Manifest.permission.RECORD_AUDIO
                        }
                        if (needed.isEmpty()) {
                            request.grant(request.resources)
                        } else {
                            pendingWebPermission = request
                            mediaPermLauncher.launch(needed.toTypedArray())
                        }
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: GeolocationPermissions.Callback?
                    ) {
                        if (origin == null || callback == null) return
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            callback.invoke(origin, true, false)
                        } else {
                            pendingGeoOrigin = origin
                            pendingGeoCallback = callback
                            locationPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                    }
                }

                if (activeTab.url != ORBIT_HOME_URL) {
                    loadUrl(activeTab.url)
                }
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = activeTab.isLoading && activeTab.progress < 90,
        onRefresh = { webView.reload() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = {
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView
                },
                update = { view ->
                    (view.parent as? ViewGroup)?.let { parent ->
                        if (parent !is FrameLayout) {
                            parent.removeView(view)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Friendly Error Overlay
            if (activeTab.errorDescription != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Page failed to load",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to Load Page",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeTab.errorDescription,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    activeTab.failedUrl?.let { failed ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = failed,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onGoHome) {
                            Icon(Icons.Default.Home, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Page")
                        }
                        Button(onClick = { webView.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }

            // Fullscreen Video Overlay
            fullscreenView?.let { fsView ->
                AndroidView(
                    factory = {
                        FrameLayout(context).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            (fsView.parent as? ViewGroup)?.removeView(fsView)
                            addView(fsView)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }

    // JS Dialogs
    when (val dlg = jsDialog) {
        is JsDialogState.Alert -> {
            AlertDialog(
                onDismissRequest = { dlg.result.cancel(); jsDialog = null },
                title = { Text("Page Message") },
                text = { Text(dlg.message) },
                confirmButton = {
                    TextButton(onClick = { dlg.result.confirm(); jsDialog = null }) { Text("OK") }
                }
            )
        }
        is JsDialogState.Confirm -> {
            AlertDialog(
                onDismissRequest = { dlg.result.cancel(); jsDialog = null },
                title = { Text("Confirm Action") },
                text = { Text(dlg.message) },
                dismissButton = {
                    TextButton(onClick = { dlg.result.cancel(); jsDialog = null }) { Text("Cancel") }
                },
                confirmButton = {
                    TextButton(onClick = { dlg.result.confirm(); jsDialog = null }) { Text("Confirm") }
                }
            )
        }
        is JsDialogState.Prompt -> {
            var input by remember(dlg) { mutableStateOf(dlg.defaultVal) }
            AlertDialog(
                onDismissRequest = { dlg.result.cancel(); jsDialog = null },
                title = { Text("Page Prompt") },
                text = {
                    Column {
                        Text(dlg.message)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = input, onValueChange = { input = it }, singleLine = true)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dlg.result.cancel(); jsDialog = null }) { Text("Cancel") }
                },
                confirmButton = {
                    TextButton(onClick = { dlg.result.confirm(input); jsDialog = null }) { Text("OK") }
                }
            )
        }
        null -> Unit
    }

    // SSL Warning Dialog
    sslDialog?.let { ssl ->
        AlertDialog(
            onDismissRequest = { ssl.handler.cancel(); sslDialog = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Security Certificate Warning") },
            text = {
                Text("This site's SSL certificate could not be verified (${ssl.url}). Proceeding may expose sensitive data.")
            },
            dismissButton = {
                Button(onClick = { ssl.handler.cancel(); sslDialog = null }) { Text("Go Back Safely") }
            },
            confirmButton = {
                TextButton(onClick = { ssl.handler.proceed(); sslDialog = null }) {
                    Text("Proceed Anyway", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Long-press Link / Image Context Sheet
    hitMenu?.let { hit ->
        ModalBottomSheet(onDismissRequest = { hitMenu = null }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = hit.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                ListItem(
                    headlineContent = { Text("Open in New Tab") },
                    leadingContent = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                    modifier = Modifier.clickableNoRippleCompat {
                        onOpenNewTab(hit.url, false)
                        hitMenu = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Open in Incognito Tab") },
                    leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                    modifier = Modifier.clickableNoRippleCompat {
                        onOpenNewTab(hit.url, true)
                        hitMenu = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Share to Orbit Chat") },
                    leadingContent = { Icon(Icons.Default.Send, contentDescription = null) },
                    modifier = Modifier.clickableNoRippleCompat {
                        onShareToChat(
                            ShareToChatPayload(
                                title = if (hit.isImage) "Shared Image" else activeTab.title,
                                url = hit.url,
                                isImage = hit.isImage
                            )
                        )
                        hitMenu = null
                    }
                )
                ListItem(
                    headlineContent = { Text(if (hit.isImage) "Download Image" else "Download Link") },
                    leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                    modifier = Modifier.clickableNoRippleCompat {
                        onDownload(hit.url, null, null, null)
                        hitMenu = null
                    }
                )
                ListItem(
                    headlineContent = { Text("Copy URL") },
                    leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    modifier = Modifier.clickableNoRippleCompat {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("URL", hit.url))
                        onShowMessage("Copied URL to clipboard")
                        hitMenu = null
                    }
                )
            }
        }
    }
}

@Composable
private fun Modifier.clickableNoRippleCompat(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
