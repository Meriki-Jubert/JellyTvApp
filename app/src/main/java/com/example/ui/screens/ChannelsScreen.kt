package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChannelSchedule
import com.example.data.model.ChannelType
import com.example.data.model.ConnectionState
import com.example.data.model.ScheduleBlock
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    schedules: Map<ChannelType, ChannelSchedule>,
    currentTimeMs: Long,
    connectionState: ConnectionState,
    onTuneInChannel: (ChannelType) -> Unit,
    onViewSchedule: (ChannelType) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilterChannel by remember { mutableStateOf<ChannelType?>(null) }

    // Pulsing animation for network status dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_net")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Scaffold(
        topBar = {
            // Sleek Header with gradient from #1C1B1F to transparent
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Home Station",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onOpenSettings() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (connectionState) {
                                            is ConnectionState.Connected -> EmeraldLive.copy(alpha = pulseAlpha)
                                            is ConnectionState.Connecting -> AmberAccent.copy(alpha = pulseAlpha)
                                            else -> CrimsonLive
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (connectionState) {
                                    is ConnectionState.Connected -> "LOCAL NETWORK: ${connectionState.serverName.uppercase()}"
                                    is ConnectionState.Connecting -> "CONNECTING TO STATION..."
                                    else -> "STANDALONE / DEMO NETWORK"
                                },
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.2.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(42.dp)
                                .background(SurfaceElevated, CircleShape)
                                .testTag("refresh_channels_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(42.dp)
                                .background(SurfaceElevated, CircleShape)
                                .testTag("server_status_pill")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = VoidBlack,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
        ) {
            // Hero Highlight Channel Banner (Blade Runner style featured card)
            item {
                val featuredChannel = selectedFilterChannel ?: ChannelType.MOVIES
                val featuredSchedule = schedules[featuredChannel]
                val activeBlock = featuredSchedule?.getCurrentBlock(currentTimeMs)
                val mediaItem = if (activeBlock?.isBreak == true) activeBlock.breakNextItem else activeBlock?.item
                val brandColor = getChannelColor(featuredChannel)

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clickable { onTuneInChannel(featuredChannel) }
                        .testTag("featured_channel_card")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background backdrop image
                        val backdropUrl = mediaItem?.backdropUrl ?: mediaItem?.posterUrl
                        if (backdropUrl != null) {
                            AsyncImage(
                                model = backdropUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.65f)
                            )
                        }

                        // Gradient overlays for crisp text readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.9f)
                                        )
                                    )
                                )
                        )

                        // Top LIVE badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                        ) {
                            if (activeBlock?.isBreak == true) {
                                BreakBadge(durationMin = activeBlock.breakDurationMinutes)
                            } else {
                                LiveBadge(isLive = true)
                            }
                        }

                        // Top-right Quick Tune button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceElevated.copy(alpha = 0.8f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier.clickable { onTuneInChannel(featuredChannel) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = TextWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "WATCH",
                                        color = TextWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }

                        // Bottom Metadata & Progress
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "${featuredChannel.displayName.uppercase()} CHANNEL",
                                color = IndigoLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mediaItem?.title ?: "Linear Broadcast Stream",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (activeBlock != null && !activeBlock.isBreak) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val fraction = activeBlock.progressFraction(currentTimeMs)
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = IndigoAccent,
                                    trackColor = Color.White.copy(alpha = 0.2f)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatTime(activeBlock.startTimeEpochMs),
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "-${formatCountdown(activeBlock.remainingTimeMs(currentTimeMs))}",
                                        color = TextSecondary,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sleek Channel Filter Pills Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selectedFilterChannel == null) IndigoAccent else SurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedFilterChannel == null) IndigoLight.copy(alpha = 0.4f) else BorderSubtle
                            ),
                            modifier = Modifier
                                .clickable { selectedFilterChannel = null }
                                .testTag("filter_all_channels")
                        ) {
                            Text(
                                text = "All Channels",
                                color = if (selectedFilterChannel == null) TextWhite else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                    }
                    items(ChannelType.entries) { channelType ->
                        val isSelected = selectedFilterChannel == channelType
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) IndigoAccent else SurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) IndigoLight.copy(alpha = 0.4f) else BorderSubtle
                            ),
                            modifier = Modifier
                                .clickable { selectedFilterChannel = channelType }
                                .testTag("filter_channel_${channelType.id}")
                        ) {
                            Text(
                                text = channelType.displayName,
                                color = if (isSelected) TextWhite else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Sleek Schedule / Channels Surface Container with rounded-t-[32px]
            item {
                val channelsToShow = selectedFilterChannel?.let { listOf(it) } ?: ChannelType.entries

                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "UPCOMING SCHEDULE",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = formatTime(currentTimeMs),
                                color = IndigoGlow,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        channelsToShow.forEachIndexed { index, channelType ->
                            val schedule = schedules[channelType]
                            val currentBlock = schedule?.getCurrentBlock(currentTimeMs)
                            val nextBlock = schedule?.getNextBlock(currentTimeMs)

                            SleekChannelItemRow(
                                channelType = channelType,
                                currentBlock = currentBlock,
                                nextBlock = nextBlock,
                                currentTimeMs = currentTimeMs,
                                onTuneIn = { onTuneInChannel(channelType) },
                                onViewSchedule = { onViewSchedule(channelType) }
                            )

                            if (index < channelsToShow.size - 1) {
                                Divider(
                                    color = BorderSubtle,
                                    thickness = 0.8.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SleekChannelItemRow(
    channelType: ChannelType,
    currentBlock: ScheduleBlock?,
    nextBlock: ScheduleBlock?,
    currentTimeMs: Long,
    onTuneIn: () -> Unit,
    onViewSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandColor = getChannelColor(channelType)
    val isBreak = currentBlock?.isBreak == true
    val currentItem = if (isBreak) currentBlock?.breakNextItem else currentBlock?.item

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onTuneIn() }
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .testTag("channel_card_${channelType.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time Indicator (Monospace)
        Text(
            text = if (currentBlock != null) formatTime(currentBlock.startTimeEpochMs) else "--:--",
            color = TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.width(46.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Channel Info & Title
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        isBreak -> "Intermission (${currentBlock?.breakDurationMinutes ?: 30} min)"
                        currentItem != null -> currentItem.title
                        else -> "${channelType.displayName} Airing"
                    },
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isBreak) "INTERMISSION" else channelType.displayName.uppercase(),
                    color = if (isBreak) AmberGlow else IndigoGlow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )

                if (currentItem?.runtimeMinutes != null && !isBreak) {
                    Text(
                        text = " • ${currentItem.runtimeMinutes}m",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Media thumbnail preview
        val posterUrl = currentItem?.posterUrl
        if (posterUrl != null) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(36.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceElevated)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Sleek Tune In Icon button
        IconButton(
            onClick = onTuneIn,
            modifier = Modifier
                .size(36.dp)
                .background(SurfaceElevated, CircleShape)
                .testTag("tune_in_${channelType.id}")
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Watch",
                tint = TextWhite,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


