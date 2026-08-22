package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_blocks")
data class ScheduleBlockEntity(
    @PrimaryKey val blockId: String,
    val channelId: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val isBreak: Boolean,
    val breakDurationMinutes: Int,
    // Media item data (flattened for fast offline retrieval)
    val itemId: String?,
    val itemTitle: String?,
    val itemSeriesName: String?,
    val itemSeriesId: String?,
    val itemSeasonNumber: Int?,
    val itemEpisodeNumber: Int?,
    val itemOverview: String?,
    val itemRuntimeMinutes: Int?,
    val itemPosterUrl: String?,
    val itemBackdropUrl: String?,
    val itemStreamUrl: String?,
    val itemMediaType: String?,
    val itemGenres: String?,
    val itemReleaseYear: Int?,
    // Next item metadata (for break screens)
    val nextItemId: String?,
    val nextItemTitle: String?,
    val nextItemSeriesName: String?,
    val nextItemSeasonNumber: Int?,
    val nextItemEpisodeNumber: Int?,
    val nextItemOverview: String?,
    val nextItemPosterUrl: String?,
    val nextItemBackdropUrl: String?,
    val nextItemRuntimeMinutes: Int?
)

@Entity(tableName = "show_progress", primaryKeys = ["showId", "channelId"])
data class ShowProgressEntity(
    val showId: String,
    val channelId: String,
    val showName: String,
    val currentEpisodeIndex: Int = 0,
    val totalEpisodes: Int = 0,
    val lastCompletedEpochMs: Long = 0L,
    val isCompleted: Boolean = false
)

@Entity(tableName = "movie_history", primaryKeys = ["movieId", "channelId"])
data class MovieHistoryEntity(
    val movieId: String,
    val channelId: String,
    val movieTitle: String,
    val lastPlayedEpochMs: Long = 0L
)

@Entity(tableName = "channel_metadata")
data class ChannelScheduleMetaEntity(
    @PrimaryKey val channelId: String,
    val dayStartEpochMs: Long,
    val dayEndEpochMs: Long,
    val firstHalfDurationMs: Long,
    val generatedAtEpochMs: Long
)
