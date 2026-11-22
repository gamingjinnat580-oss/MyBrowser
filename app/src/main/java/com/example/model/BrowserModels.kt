package com.example.model

import java.util.UUID

const val ORBIT_HOME_URL = "orbit://home"

enum class SearchEngine(
    val displayName: String,
    val searchUrlTemplate: String,
    val homeUrl: String
) {
    GOOGLE("Google", "https://www.google.com/search?q=", "https://www.google.com"),
    BING("Bing", "https://www.bing.com/search?q=", "https://www.bing.com"),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p=", "https://www.yahoo.com"),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=", "https://duckduckgo.com"),
    YANDEX("Yandex", "https://yandex.com/search/?text=", "https://yandex.com"),
    BAIDU("Baidu", "https://www.baidu.com/s?wd=", "https://www.baidu.com"),
    BRAVE("Brave", "https://search.brave.com/search?q=", "https://search.brave.com"),
    WIKIPEDIA("Wikipedia", "https://en.wikipedia.org/wiki/Special:Search?search=", "https://www.wikipedia.org"),
    YOUTUBE("YouTube", "https://m.youtube.com/results?search_query=", "https://m.youtube.com");

    companion object {
        fun safeFrom(name: String?): SearchEngine =
            entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
                ?: GOOGLE
    }
}

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = ORBIT_HOME_URL,
    val title: String = "Orbit Start",
    val isIncognito: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val desktopMode: Boolean = false,
    val errorDescription: String? = null,
    val failedUrl: String? = null
)

data class DownloadItem(
    val id: Long,
    val title: String,
    val url: String,
    val statusText: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean,
    val isFailed: Boolean,
    val localUri: String?
)

data class QuickSite(
    val name: String,
    val url: String,
    val category: SiteCategory,
    val region: String,
    val badge: String,
    val tagline: String
)

enum class SiteCategory(val label: String) {
    ALL("All"),
    SEARCH("Search"),
    GOOGLE_CHROME("Google"),
    SOCIAL("Social"),
    VIDEO("Video"),
    NEWS("News"),
    SHOPPING("Shopping"),
    LEARNING("Learning"),
    AI("AI"),
    MAIL("Mail"),
    TRAVEL("Travel"),
    GAMES("Games"),
    FINANCE("Finance"),
    SPORTS("Sports")
}

data class ShareToChatPayload(
    val title: String,
    val url: String,
    val isImage: Boolean = false
)
