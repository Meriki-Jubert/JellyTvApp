package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChannelSchedule
import com.example.data.model.ChannelType
import com.example.ui.theme.*

@Composable
fun ChannelSurfBar(
    selectedChannel: ChannelType,
    schedules: Map<ChannelType, ChannelSchedule>,
    currentTimeMs: Long,
    onSelectChannel: (ChannelType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("channel_surf_bar"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(ChannelType.entries) { channelType ->
            val isSelected = channelType == selectedChannel
            val brandColor = getChannelColor(channelType)
            val schedule = schedules[channelType]
            val currentBlock = schedule?.getCurrentBlock(currentTimeMs)
            val currentTitle = when {
                currentBlock == null -> "Loading guide..."
                currentBlock.isBreak -> "Intermission (${currentBlock.breakDurationMinutes}m)"
                else -> currentBlock.item?.title ?: "On Air"
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) SurfaceElevated else SurfaceDark.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) IndigoGlow else BorderSubtle
                ),
                modifier = Modifier
                    .width(165.dp)
                    .clickable { onSelectChannel(channelType) }
                    .testTag("surf_channel_${channelType.id}")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = getChannelIcon(channelType),
                                contentDescription = null,
                                tint = if (isSelected) IndigoGlow else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = channelType.displayName,
                                color = if (isSelected) TextWhite else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(IndigoGlow)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentTitle,
                        color = if (isSelected) TextWhite else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

