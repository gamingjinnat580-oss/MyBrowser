package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.SearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.browserPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "orbit_browser_prefs"
)

data class BrowserPreferences(
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val darkModeWeb: Boolean = true,
    val autoplayMedia: Boolean = true,
    val backgroundAudio: Boolean = true
)

class BrowserPreferencesRepository(private val context: Context) {
    private object Keys {
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val DARK_MODE_WEB = booleanPreferencesKey("dark_mode_web")
        val AUTOPLAY_MEDIA = booleanPreferencesKey("autoplay_media")
        val BACKGROUND_AUDIO = booleanPreferencesKey("background_audio")
    }

    val preferences: Flow<BrowserPreferences> = context.browserPrefsDataStore.data.map { prefs ->
        BrowserPreferences(
            searchEngine = SearchEngine.safeFrom(prefs[Keys.SEARCH_ENGINE]),
            darkModeWeb = prefs[Keys.DARK_MODE_WEB] ?: true,
            autoplayMedia = prefs[Keys.AUTOPLAY_MEDIA] ?: true,
            backgroundAudio = prefs[Keys.BACKGROUND_AUDIO] ?: true
        )
    }

    suspend fun setSearchEngine(engine: SearchEngine) {
        context.browserPrefsDataStore.edit { prefs ->
            prefs[Keys.SEARCH_ENGINE] = engine.displayName
        }
    }

    suspend fun setDarkModeWeb(enabled: Boolean) {
        context.browserPrefsDataStore.edit { prefs ->
            prefs[Keys.DARK_MODE_WEB] = enabled
        }
    }

    suspend fun setAutoplayMedia(enabled: Boolean) {
        context.browserPrefsDataStore.edit { prefs ->
            prefs[Keys.AUTOPLAY_MEDIA] = enabled
        }
    }

    suspend fun setBackgroundAudio(enabled: Boolean) {
        context.browserPrefsDataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_AUDIO] = enabled
        }
    }
}
