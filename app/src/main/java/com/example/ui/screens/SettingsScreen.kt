package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionState
import com.example.data.model.ServerConfig
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentConfig: ServerConfig,
    connectionState: ConnectionState,
    isRegenerating: Boolean = false,
    onSaveSettings: (String, String, Boolean, String, String, String, String) -> Unit,
    onTestConnection: () -> Unit,
    onForceRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var serverUrl by remember(currentConfig) { mutableStateOf(currentConfig.serverUrl) }
    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig.apiKey) }
    var isDemoMode by remember(currentConfig) { mutableStateOf(currentConfig.isDemoMode) }
    var moviesLib by remember(currentConfig) { mutableStateOf(currentConfig.moviesLibraryId) }
    var seriesLib by remember(currentConfig) { mutableStateOf(currentConfig.seriesLibraryId) }
    var cartoonsLib by remember(currentConfig) { mutableStateOf(currentConfig.cartoonsLibraryId) }
    var animeLib by remember(currentConfig) { mutableStateOf(currentConfig.animeLibraryId) }

    var showAdvancedLibraries by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Sleek Header with gradient from SurfaceDark to transparent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SurfaceDark,
                                SurfaceDark.copy(alpha = 0.7f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Column {
                    Text(
                        text = "Settings",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SERVER & CHANNEL ROUTING",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        },
        containerColor = VoidBlack,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Connection Status Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (connectionState) {
                        is ConnectionState.Connected -> EmeraldLive.copy(alpha = 0.4f)
                        is ConnectionState.Connecting -> AmberAccent.copy(alpha = 0.4f)
                        else -> BorderSubtle
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (connectionState) {
                                            is ConnectionState.Connected -> EmeraldLive
                                            is ConnectionState.Connecting -> AmberAccent
                                            else -> CrimsonLive
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when (connectionState) {
                                    is ConnectionState.Connected -> "Server Connected"
                                    is ConnectionState.Connecting -> "Testing Connection..."
                                    else -> "Disconnected / Standalone"
                                },
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onTestConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = TextWhite),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("test_connection_btn")
                        ) {
                            Text("TEST SIGNAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }

                    if (connectionState is ConnectionState.Connected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${connectionState.serverName} (v${connectionState.version}) • Direct Stream Ready",
                            color = EmeraldLive,
                            fontSize = 12.sp
                        )
                    } else if (connectionState is ConnectionState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = connectionState.message,
                            color = CrimsonLive,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Jellyfin Connection Configuration Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LOCAL JELLYFIN CONFIGURATION",
                        color = IndigoGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("Server Base URL") },
                        placeholder = { Text("http://192.168.1.100:8096") },
                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = IndigoGlow) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedLabelColor = IndigoGlow,
                            unfocusedLabelColor = TextMuted
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Jellyfin API Key") },
                        placeholder = { Text("Your Jellyfin API Key") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = AmberGlow) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedLabelColor = AmberGlow,
                            unfocusedLabelColor = TextMuted
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Advanced Library IDs Accordion
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceElevated.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAdvancedLibraries = !showAdvancedLibraries },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Channel Library Parent IDs (Optional)",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = if (showAdvancedLibraries) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }

                            if (showAdvancedLibraries) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = moviesLib,
                                    onValueChange = { moviesLib = it },
                                    label = { Text("Movies Library/Parent ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = seriesLib,
                                    onValueChange = { seriesLib = it },
                                    label = { Text("TV Series Library/Parent ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = cartoonsLib,
                                    onValueChange = { cartoonsLib = it },
                                    label = { Text("Cartoons Library/Parent ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = animeLib,
                                    onValueChange = { animeLib = it },
                                    label = { Text("Anime Library/Parent ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            onSaveSettings(
                                serverUrl,
                                apiKey,
                                isDemoMode,
                                moviesLib,
                                seriesLib,
                                cartoonsLib,
                                animeLib
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_settings_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent, contentColor = TextWhite),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SAVE CONFIGURATION", fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
                    }
                }
            }

            // Schedule Actions & Engine Details
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LINEAR SCHEDULING ENGINE",
                        color = AmberGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Movie Channel: 12h First Half (least-recently-played + 60m breaks) mirrored into duration A -> 2A.\n" +
                                "• Series Channels: 2 eps for short shows (<30m), 3 eps for long shows (30m+) + 30m breaks.\n" +
                                "• Zero Live Transcoding: Plays native Jellyfin streams seeked to live broadcast offset.\n" +
                                "• 100% Local / Self-Hosted: No cloud telemetry.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onForceRegenerate,
                        enabled = !isRegenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("force_regenerate_btn"),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isRegenerating) IndigoGlow.copy(alpha = 0.5f) else BorderSubtle
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextWhite,
                            disabledContentColor = IndigoGlow
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isRegenerating) {
                            CircularProgressIndicator(
                                color = IndigoGlow,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("REGENERATING ALL SCHEDULES...", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IndigoGlow)
                        } else {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = IndigoGlow)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("REGENERATE ALL SCHEDULES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

