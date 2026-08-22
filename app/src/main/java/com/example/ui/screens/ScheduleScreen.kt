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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.data.model.ScheduleBlock
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    schedules: Map<ChannelType, ChannelSchedule>,
    initialChannel: ChannelType = ChannelType.MOVIES,
    currentTimeMs: Long,
    isRegenerating: Boolean = false,
    onTuneInChannel: (ChannelType) -> Unit,
    onBack: () -> Unit,
    onRegenerate: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedChannel by remember { mutableStateOf(initialChannel) }
    var selectedViewMode by remember { mutableIntStateOf(0) } // 0: Channel Lineup, 1: Full EPG Grid

    val currentSchedule = schedules[selectedChannel]
    val brandColor = getChannelColor(selectedChannel)

    // Infinite rotation for spinning regenerate icon
    val infiniteTransition = rememberInfiniteTransition(label = "regenerate_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TV Guide",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "LINEAR 24H BROADCAST ROTATION",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.2.sp
                        )
                    }

                    // Top Bar Regenerate Action Button with spinner indicator
                    FilledTonalButton(
                        onClick = onRegenerate,
                        enabled = !isRegenerating,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isRegenerating) IndigoAccent.copy(alpha = 0.25f) else SurfaceElevated,
                            contentColor = IndigoGlow,
                            disabledContainerColor = SurfaceElevated.copy(alpha = 0.5f),
                            disabledContentColor = IndigoGlow
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("regenerate_schedule_button")
                    ) {
                        if (isRegenerating) {
                            CircularProgressIndicator(
                                color = IndigoGlow,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "REBUILDING...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Regenerate Schedules",
                                tint = IndigoGlow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "REGENERATE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
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
        ) {
            // Live Regeneration Banner Indicator
            if (isRegenerating) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IndigoAccent.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndigoGlow.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("regenerating_indicator_banner")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                color = IndigoGlow,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "REGENERATING BROADCAST SCHEDULES...",
                                color = TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            color = IndigoGlow,
                            trackColor = SurfaceHighlight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                        )
                    }
                }
            }

            // View Mode Tab Switcher (Single Channel Lineup vs All Channels EPG Grid)
            TabRow(
                selectedTabIndex = selectedViewMode,
                containerColor = SurfaceDark,
                contentColor = IndigoGlow,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedViewMode]),
                        color = IndigoGlow
                    )
                },
                divider = { Divider(color = BorderSubtle, thickness = 0.8.dp) }
            ) {
                Tab(
                    selected = selectedViewMode == 0,
                    onClick = { selectedViewMode = 0 },
                    text = {
                        Text(
                            "CHANNEL LINEUP",
                            fontWeight = if (selectedViewMode == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                )
                Tab(
                    selected = selectedViewMode == 1,
                    onClick = { selectedViewMode = 1 },
                    text = {
                        Text(
                            "FULL EPG GRID",
                            fontWeight = if (selectedViewMode == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                )
            }

            if (selectedViewMode == 0) {
                // Sleek Channel Selection Pills Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ChannelType.entries) { channelType ->
                        ChannelPill(
                            channelType = channelType,
                            isSelected = channelType == selectedChannel,
                            onClick = { selectedChannel = channelType }
                        )
                    }
                }

                // Channel Info Header Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = brandColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, brandColor.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = getChannelIcon(selectedChannel),
                                    contentDescription = null,
                                    tint = brandColor,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = selectedChannel.displayName,
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (selectedChannel) {
                                        ChannelType.MOVIES -> "12h First Half + Mirrored 2A Schedule"
                                        else -> "Show sessions (2-3 eps) + 30m Intermissions"
                                    },
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { onTuneInChannel(selectedChannel) },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent, contentColor = TextWhite),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WATCH", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }

                // Ordered Schedule Blocks List
                val blocks = currentSchedule?.blocks ?: emptyList()
                if (blocks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRegenerating) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = IndigoGlow,
                                    modifier = Modifier.size(44.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "COMPILING BROADCAST SCHEDULE...",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Querying Jellyfin library & generating 24h timeline",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SurfaceDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 400.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TvOff,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "NO SCHEDULE GENERATED",
                                        color = TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Check Jellyfin server connection or trigger a full schedule regeneration.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = onRegenerate,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("REGENERATE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = onOpenSettings,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent, contentColor = TextWhite),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SETTINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(blocks) { block ->
                            val isAiring = block.isCurrentlyAiring(currentTimeMs)
                            val isPast = block.endTimeEpochMs <= currentTimeMs

                            ScheduleBlockListItem(
                                block = block,
                                channelType = selectedChannel,
                                isAiring = isAiring,
                                isPast = isPast,
                                currentTimeMs = currentTimeMs,
                                onTuneIn = { onTuneInChannel(selectedChannel) }
                            )
                        }
                    }
                }
            } else {
                // Cross-Channel Full EPG Grid Timeline
                FullEpgGridView(
                    schedules = schedules,
                    currentTimeMs = currentTimeMs,
                    onTuneInChannel = onTuneInChannel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ScheduleBlockListItem(
    block: ScheduleBlock,
    channelType: ChannelType,
    isAiring: Boolean,
    isPast: Boolean,
    currentTimeMs: Long,
    onTuneIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandColor = getChannelColor(channelType)
    val isBreak = block.isBreak
    val mediaItem = if (isBreak) block.breakNextItem else block.item

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            isAiring -> SurfaceElevated
            isPast -> VoidBlack.copy(alpha = 0.5f)
            else -> SurfaceDark
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (isAiring) 1.2.dp else 1.dp,
            color = when {
                isAiring -> IndigoGlow.copy(alpha = 0.7f)
                else -> BorderSubtle
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { if (isAiring) onTuneIn() }
            .testTag("schedule_item_${block.blockId}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Column (Monospace)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(54.dp)
            ) {
                Text(
                    text = formatTime(block.startTimeEpochMs),
                    color = if (isAiring) IndigoGlow else if (isPast) TextMuted else TextWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${block.durationMinutes}m",
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Poster Thumbnail
            if (mediaItem?.posterUrl != null) {
                AsyncImage(
                    model = mediaItem.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(44.dp)
                        .height(60.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevated)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Title & Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isBreak) {
                        BreakBadge(durationMin = block.breakDurationMinutes)
                    } else if (isAiring) {
                        LiveBadge(isLive = true)
                    } else {
                        Text(
                            text = if (isPast) "AIRED" else "UPCOMING",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (isBreak) "Intermission • Next: ${mediaItem?.title ?: "Program"}" else (mediaItem?.title ?: "Untitled"),
                    color = if (isPast) TextMuted else TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (mediaItem?.seriesName != null && !isBreak) {
                    Text(
                        text = mediaItem.displaySubtitle,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // Airing progress bar
                if (isAiring && !isBreak) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { block.progressFraction(currentTimeMs) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = IndigoAccent,
                        trackColor = SurfaceHighlight
                    )
                }
            }

            if (isAiring) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onTuneIn,
                    modifier = Modifier
                        .size(34.dp)
                        .background(IndigoAccent, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Tune In",
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FullEpgGridView(
    schedules: Map<ChannelType, ChannelSchedule>,
    currentTimeMs: Long,
    onTuneInChannel: (ChannelType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "CROSS-CHANNEL 24H BROADCAST GRID",
                color = IndigoGlow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        items(ChannelType.entries) { channelType ->
            val brandColor = getChannelColor(channelType)
            val schedule = schedules[channelType]
            val blocks = schedule?.blocks ?: emptyList()

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = getChannelIcon(channelType),
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = channelType.displayName,
                                color = TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { onTuneInChannel(channelType) },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent, contentColor = TextWhite),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("WATCH", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(blocks) { block ->
                            val isAiring = block.isCurrentlyAiring(currentTimeMs)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAiring) SurfaceElevated else VoidBlack,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isAiring) IndigoGlow.copy(alpha = 0.6f) else BorderSubtle
                                ),
                                modifier = Modifier.width(160.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = formatTime(block.startTimeEpochMs),
                                            color = if (isAiring) IndigoGlow else TextMuted,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${block.durationMinutes}m",
                                            color = TextMuted,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (block.isBreak) "Intermission" else (block.item?.title ?: "Program"),
                                        color = TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

