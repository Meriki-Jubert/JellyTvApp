package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ChannelType
import com.example.ui.components.LiveBadge
import com.example.ui.components.getChannelColor
import com.example.ui.components.getChannelIcon
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.ScheduleScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.HomeStationViewModel

enum class MainDestination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CHANNELS("Channels", Icons.Default.Tv),
    PLAYER("Live TV", Icons.Default.PlayCircle),
    GUIDE("TV Guide", Icons.Default.CalendarMonth),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HomeStationApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeStationApp(
    viewModel: HomeStationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentDestination by remember { mutableStateOf(MainDestination.PLAYER) }
    var guideInitialChannel by remember { mutableStateOf(ChannelType.MOVIES) }
    val snackbarHostState = remember { SnackbarHostState() }

    // User message snackbar trigger
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        bottomBar = {
            // Hide bottom bar when on full-screen player to give full cinematic TV immersion, or show slim bar
            if (currentDestination != MainDestination.PLAYER) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    contentColor = IndigoGlow,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .testTag("bottom_nav_bar")
                ) {
                    MainDestination.entries.forEach { destination ->
                        val isSelected = currentDestination == destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label.uppercase(),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.2.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = IndigoGlow,
                                selectedTextColor = IndigoGlow,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = SurfaceElevated
                            )
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = VoidBlack,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (currentDestination == MainDestination.PLAYER) PaddingValues(0.dp) else innerPadding)
        ) {
            when (currentDestination) {
                MainDestination.CHANNELS -> {
                    ChannelsScreen(
                        schedules = uiState.schedules,
                        currentTimeMs = uiState.currentTimeMs,
                        connectionState = uiState.connectionState,
                        onTuneInChannel = { channel ->
                            viewModel.selectChannel(channel)
                            currentDestination = MainDestination.PLAYER
                        },
                        onViewSchedule = { channel ->
                            guideInitialChannel = channel
                            currentDestination = MainDestination.GUIDE
                        },
                        onOpenSettings = {
                            currentDestination = MainDestination.SETTINGS
                        },
                        onRefresh = {
                            viewModel.loadSchedules()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                MainDestination.PLAYER -> {
                    PlayerScreen(
                        channelType = uiState.selectedChannel,
                        schedules = uiState.schedules,
                        currentTimeMs = uiState.currentTimeMs,
                        connectionState = uiState.connectionState,
                        isLoading = uiState.isLoading,
                        onSelectChannel = { channel ->
                            viewModel.selectChannel(channel)
                        },
                        onBackToChannels = {
                            currentDestination = MainDestination.CHANNELS
                        },
                        onOpenScheduleGuide = { channel ->
                            guideInitialChannel = channel
                            currentDestination = MainDestination.GUIDE
                        },
                        onOpenSettings = {
                            currentDestination = MainDestination.SETTINGS
                        },
                        onRetryConnection = {
                            viewModel.testConnection()
                            viewModel.loadSchedules()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                MainDestination.GUIDE -> {
                    ScheduleScreen(
                        schedules = uiState.schedules,
                        initialChannel = guideInitialChannel,
                        currentTimeMs = uiState.currentTimeMs,
                        isRegenerating = uiState.isRegenerating,
                        onTuneInChannel = { channel ->
                            viewModel.selectChannel(channel)
                            currentDestination = MainDestination.PLAYER
                        },
                        onBack = {
                            currentDestination = MainDestination.CHANNELS
                        },
                        onRegenerate = {
                            viewModel.forceRegenerateSchedules()
                        },
                        onOpenSettings = {
                            currentDestination = MainDestination.SETTINGS
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                MainDestination.SETTINGS -> {
                    SettingsScreen(
                        currentConfig = uiState.serverConfig,
                        connectionState = uiState.connectionState,
                        isRegenerating = uiState.isRegenerating,
                        onSaveSettings = { url, key, demo, mLib, sLib, cLib, aLib ->
                            viewModel.saveSettings(url, key, demo, mLib, sLib, cLib, aLib)
                        },
                        onTestConnection = {
                            viewModel.testConnection()
                        },
                        onForceRegenerate = {
                            viewModel.forceRegenerateSchedules()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
