package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.*
import com.example.data.repository.JellyfinRepository
import com.example.data.repository.ScheduleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeStationUiState(
    val selectedChannel: ChannelType = ChannelType.MOVIES,
    val schedules: Map<ChannelType, ChannelSchedule> = emptyMap(),
    val serverConfig: ServerConfig = ServerConfig(),
    val connectionState: ConnectionState = ConnectionState.Idle,
    val isLoading: Boolean = true,
    val isRegenerating: Boolean = false,
    val currentTimeMs: Long = System.currentTimeMillis(),
    val userMessage: String? = null
)

class HomeStationViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val appDatabase = AppDatabase.getInstance(application)
    private val jellyfinRepository = JellyfinRepository(ServerConfig())
    private val scheduleRepository = ScheduleRepository(appDatabase, jellyfinRepository)

    private val _uiState = MutableStateFlow(HomeStationUiState())
    val uiState: StateFlow<HomeStationUiState> = _uiState.asStateFlow()

    init {
        // Observe server configuration
        viewModelScope.launch {
            settingsDataStore.serverConfigFlow.collect { config ->
                jellyfinRepository.updateConfig(config)
                _uiState.update { it.copy(serverConfig = config) }
                loadSchedules()
                testConnection()
            }
        }

        // Real-time broadcast clock ticker (updates every 1 second)
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _uiState.update { it.copy(currentTimeMs = now) }

                // Check if any schedule needs day roll-over (e.g. passed dayEndEpochMs)
                checkScheduleBoundaries(now)

                delay(1000)
            }
        }
    }

    private fun checkScheduleBoundaries(now: Long) {
        val currentSchedules = _uiState.value.schedules
        if (currentSchedules.isEmpty()) return

        var needsRegeneration = false
        for ((_, schedule) in currentSchedules) {
            if (now >= schedule.dayEndEpochMs || schedule.blocks.isEmpty()) {
                needsRegeneration = true
                break
            }
        }

        if (needsRegeneration) {
            viewModelScope.launch {
                loadSchedules()
            }
        }
    }

    fun selectChannel(channelType: ChannelType) {
        _uiState.update { it.copy(selectedChannel = channelType) }
    }

    fun loadSchedules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val schedules = scheduleRepository.getOrGenerateAllSchedules(_uiState.value.currentTimeMs)
                _uiState.update {
                    it.copy(
                        schedules = schedules,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "Failed to update broadcast guide: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun forceRegenerateSchedules() {
        if (_uiState.value.isRegenerating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRegenerating = true, userMessage = "Regenerating broadcast schedules...") }
            try {
                val schedules = scheduleRepository.forceRegenerateAllSchedules(_uiState.value.currentTimeMs)
                _uiState.update {
                    it.copy(
                        schedules = schedules,
                        isRegenerating = false,
                        userMessage = "Broadcast schedules regenerated successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRegenerating = false,
                        userMessage = "Regeneration error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(connectionState = ConnectionState.Connecting) }
            val state = jellyfinRepository.testConnection()
            _uiState.update { it.copy(connectionState = state) }
        }
    }

    fun saveSettings(
        serverUrl: String,
        apiKey: String,
        isDemoMode: Boolean,
        moviesLib: String,
        seriesLib: String,
        cartoonsLib: String,
        animeLib: String
    ) {
        viewModelScope.launch {
            settingsDataStore.saveConfig(
                serverUrl = serverUrl,
                apiKey = apiKey,
                isDemoMode = isDemoMode,
                moviesLib = moviesLib,
                seriesLib = seriesLib,
                cartoonsLib = cartoonsLib,
                animeLib = animeLib
            )
            _uiState.update { it.copy(userMessage = "Settings saved") }
        }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDemoMode(enabled)
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
