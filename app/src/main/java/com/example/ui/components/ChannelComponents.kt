package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelType
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

fun getChannelColor(channelType: ChannelType): Color {
    return when (channelType) {
        ChannelType.MOVIES -> MovieChannelColor
        ChannelType.SERIES -> SeriesChannelColor
        ChannelType.CARTOONS -> CartoonChannelColor
        ChannelType.ANIME -> AnimeChannelColor
    }
}

fun getChannelIcon(channelType: ChannelType): ImageVector {
    return when (channelType) {
        ChannelType.MOVIES -> Icons.Default.Movie
        ChannelType.SERIES -> Icons.Default.Tv
        ChannelType.CARTOONS -> Icons.Default.SmartDisplay
        ChannelType.ANIME -> Icons.Default.FlashOn
    }
}

fun formatTime(epochMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

fun formatDurationMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

fun formatCountdown(remainingMs: Long): String {
    val totalSec = (remainingMs / 1000).coerceAtLeast(0)
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) {
        "%02d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "%02d:%02d".format(mins, secs)
    }
}

@Composable
fun LiveBadge(
    modifier: Modifier = Modifier,
    isLive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        color = LiveBadgeRed,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.testTag("live_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (isLive) alpha else 1f))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "LIVE",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun BreakBadge(
    modifier: Modifier = Modifier,
    durationMin: Int = 30
) {
    Surface(
        color = SurfaceElevated,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier.testTag("break_badge")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = AmberGlow,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "INTERMISSION (${durationMin}M)",
                color = AmberGlow,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ChannelPill(
    channelType: ChannelType,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val brandColor = getChannelColor(channelType)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) IndigoAccent else SurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) IndigoLight.copy(alpha = 0.4f) else BorderSubtle
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("channel_pill_${channelType.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = getChannelIcon(channelType),
                contentDescription = null,
                tint = if (isSelected) Color.White else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = channelType.displayName,
                color = if (isSelected) Color.White else TextSecondary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}
