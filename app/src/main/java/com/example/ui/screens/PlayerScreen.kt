package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelSchedule
import com.example.data.model.ChannelType
import com.example.data.model.ConnectionState
import com.example.data.model.ScheduleBlock
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    channelType: ChannelType,
    schedules: Map<ChannelType, ChannelSchedule>,
    currentTimeMs: Long,
    connectionState: ConnectionState = ConnectionState.Idle,
    isLoading: Boolean = false,
    onSelectChannel: (ChannelType) -> Unit,
    onBackToChannels: () -> Unit,
    onOpenScheduleGuide: (ChannelType) -> Unit,
    onOpenSettings: () -> Unit = {},
    onRetryConnection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentSchedule = schedules[channelType]
    val activeBlock = currentSchedule?.getCurrentBlock(currentTimeMs)
    val nextBlock = currentSchedule?.getNextBlock(currentTimeMs)
    val isBreakActive = activeBlock?.isBreak == true

    var showQuickGuideSheet by remember { mutableStateOf(false) }
    val brandColor = getChannelColor(channelType)

    // Calculate current live offset in milliseconds (evaluated once per block to avoid restarting player on clock ticks)
    val liveOffsetMs = remember(activeBlock?.blockId) {
        activeBlock?.currentOffsetMs(currentTimeMs) ?: 0L
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .testTag("player_screen_root")
    ) {
        // Main Screen: Video Player or Break Screen
        if (isBreakActive && activeBlock != null) {
            ComingUpNextBreakView(
                channelType = channelType,
                breakBlock = activeBlock,
                currentTimeMs = currentTimeMs,
                modifier = Modifier.fillMaxSize()
            )
        } else if (activeBlock?.item != null) {
            VideoPlayerView(
                streamUrl = activeBlock.item.streamUrl,
                initialOffsetMs = liveOffsetMs,
                mediaItem = activeBlock.item,
                onGetLiveOffsetMs = {
                    activeBlock.currentOffsetMs(currentTimeMs)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Standby / Connecting / Error State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VoidBlack)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (connectionState) {
                    is ConnectionState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonLive.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 450.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = CrimsonLive,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "JELLYFIN SERVER UNREACHABLE",
                                    color = TextWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = connectionState.message,
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = onRetryConnection,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("RETRY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = onOpenSettings,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent, contentColor = TextWhite),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SETTINGS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = IndigoGlow,
                                modifier = Modifier.size(44.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "CONNECTING TO JELLYFIN...",
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Pulling library media & building broadcast stream",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            TextButton(
                                onClick = onOpenSettings
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = IndigoGlow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SERVER CONFIGURATION", color = IndigoGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Top Overlay Header: Back button, Channel Brand, Quick Guide
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VoidBlack.copy(alpha = 0.9f),
                            VoidBlack.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToChannels,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceElevated, CircleShape)
                        .testTag("player_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = getChannelIcon(channelType),
                            contentDescription = null,
                            tint = IndigoGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = channelType.displayName,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showQuickGuideSheet = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceElevated, CircleShape)
                        .testTag("open_quick_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewAgenda,
                        contentDescription = "Quick Guide",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Overlay: Channel Surf Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            VoidBlack.copy(alpha = 0.95f)
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            ChannelSurfBar(
                selectedChannel = channelType,
                schedules = schedules,
                currentTimeMs = currentTimeMs,
                onSelectChannel = onSelectChannel
            )
        }

        // Quick EPG Guide Bottom Sheet
        if (showQuickGuideSheet) {
            ModalBottomSheet(
                onDismissRequest = { showQuickGuideSheet = false },
                containerColor = SurfaceDark,
                contentColor = TextWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = getChannelIcon(channelType),
                                contentDescription = null,
                                tint = IndigoGlow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${channelType.displayName.uppercase()} LINEUP",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        TextButton(
                            onClick = {
                                showQuickGuideSheet = false
                                onOpenScheduleGuide(channelType)
                            }
                        ) {
                            Text("FULL GUIDE", color = IndigoGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val upcoming = currentSchedule?.getUpcomingBlocks(currentTimeMs, limit = 8) ?: emptyList()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(upcoming) { block ->
                            val isCurrent = block.isCurrentlyAiring(currentTimeMs)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) SurfaceElevated else VoidBlack,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isCurrent) IndigoGlow.copy(alpha = 0.6f) else BorderSubtle
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = formatTime(block.startTimeEpochMs),
                                                color = if (isCurrent) IndigoGlow else TextMuted,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            if (isCurrent) {
                                                LiveBadge(isLive = true)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = if (block.isBreak) "Intermission Break (${block.breakDurationMinutes}m)" else (block.item?.title ?: "Program"),
                                            color = TextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!block.isBreak && block.item?.seriesName != null) {
                                            Text(
                                                text = block.item.displaySubtitle,
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${block.durationMinutes}m",
                                        color = TextMuted,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
