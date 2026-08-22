package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "home_station_settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_DEMO_MODE = booleanPreferencesKey("demo_mode")
        private val KEY_MOVIES_LIB = stringPreferencesKey("movies_lib")
        private val KEY_SERIES_LIB = stringPreferencesKey("series_lib")
        private val KEY_CARTOONS_LIB = stringPreferencesKey("cartoons_lib")
        private val KEY_ANIME_LIB = stringPreferencesKey("anime_lib")

        const val DEFAULT_API_KEY = "481bec17d65f43f593b7d1b4fc0f58b9"
        const val DEFAULT_SERVER_URL = "http://192.168.1.100:8096"
    }

    val serverConfigFlow: Flow<ServerConfig> = context.dataStore.data.map { preferences ->
        ServerConfig(
            serverUrl = preferences[KEY_SERVER_URL] ?: DEFAULT_SERVER_URL,
            apiKey = preferences[KEY_API_KEY] ?: DEFAULT_API_KEY,
            isDemoMode = preferences[KEY_DEMO_MODE] ?: false,
            moviesLibraryId = preferences[KEY_MOVIES_LIB] ?: "",
            seriesLibraryId = preferences[KEY_SERIES_LIB] ?: "",
            cartoonsLibraryId = preferences[KEY_CARTOONS_LIB] ?: "",
            animeLibraryId = preferences[KEY_ANIME_LIB] ?: ""
        )
    }

    suspend fun saveConfig(
        serverUrl: String,
        apiKey: String,
        isDemoMode: Boolean = false,
        moviesLib: String = "",
        seriesLib: String = "",
        cartoonsLib: String = "",
        animeLib: String = ""
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SERVER_URL] = serverUrl.trimEnd('/')
            preferences[KEY_API_KEY] = apiKey.trim()
            preferences[KEY_DEMO_MODE] = isDemoMode
            preferences[KEY_MOVIES_LIB] = moviesLib
            preferences[KEY_SERIES_LIB] = seriesLib
            preferences[KEY_CARTOONS_LIB] = cartoonsLib
            preferences[KEY_ANIME_LIB] = animeLib
        }
    }

    suspend fun setDemoMode(isDemo: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEMO_MODE] = isDemo
        }
    }
}
