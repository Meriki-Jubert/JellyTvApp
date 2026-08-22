package com.example.data.model

enum class ChannelType(
    val id: String,
    val displayName: String,
    val tagline: String,
    val iconName: String,
    val defaultBreakMinutes: Int
) {
    MOVIES("movies", "Cinema Station", "Blockbuster & Indie Movies", "movie", 60),
    SERIES("series", "Prime TV", "Drama, Comedy & Sci-Fi", "tv", 30),
    CARTOONS("cartoons", "Toon Wave", "Animation & Classic Cartoons", "animation", 30),
    ANIME("anime", "Anime Pulse", "Shonen, Isekai & Slice-of-Life", "bolt", 30);

    companion object {
        fun fromId(id: String): ChannelType = entries.find { it.id == id } ?: MOVIES
    }
}

enum class MediaType {
    MOVIE,
    EPISODE,
    SERIES
}

data class MediaItem(
    val id: String,
    val title: String,
    val originalTitle: String? = null,
    val seriesName: String? = null,
    val seriesId: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val overview: String = "",
    val runtimeMinutes: Int = 30,
    val runtimeTicks: Long = runtimeMinutes * 60L * 10_000_000L,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val streamUrl: String = "",
    val mediaType: MediaType = MediaType.MOVIE,
    val channelType: ChannelType = ChannelType.MOVIES,
    val genres: List<String> = emptyList(),
    val releaseYear: Int? = null,
    val rating: String? = null
) {
    val isShortEpisode: Boolean
        get() = runtimeMinutes < 30

    val formattedEpisodeTag: String
        get() = if (seasonNumber != null && episodeNumber != null) {
            "S%02dE%02d".format(seasonNumber, episodeNumber)
        } else if (seriesName != null) {
            seriesName
        } else {
            ""
        }

    val displaySubtitle: String
        get() = when {
            seriesName != null && formattedEpisodeTag.isNotEmpty() -> "$seriesName • $formattedEpisodeTag"
            seriesName != null -> seriesName
            releaseYear != null -> "$releaseYear • ${runtimeMinutes}m"
            else -> "${runtimeMinutes} min"
        }
}

data class ScheduleBlock(
    val blockId: String,
    val channelId: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val item: MediaItem? = null,
    val isBreak: Boolean = false,
    val breakNextItem: MediaItem? = null,
    val breakDurationMinutes: Int = 0
) {
    val durationMinutes: Int
        get() = ((endTimeEpochMs - startTimeEpochMs) / 60000L).toInt()

    fun isCurrentlyAiring(currentTimeMs: Long): Boolean =
        currentTimeMs in startTimeEpochMs until endTimeEpochMs

    fun currentOffsetMs(currentTimeMs: Long): Long {
        if (!isCurrentlyAiring(currentTimeMs)) return 0L
        return (currentTimeMs - startTimeEpochMs).coerceAtLeast(0L)
    }

    fun remainingTimeMs(currentTimeMs: Long): Long {
        if (currentTimeMs >= endTimeEpochMs) return 0L
        return (endTimeEpochMs - currentTimeMs).coerceAtLeast(0L)
    }

    fun progressFraction(currentTimeMs: Long): Float {
        val total = (endTimeEpochMs - startTimeEpochMs).toFloat()
        if (total <= 0f) return 0f
        val elapsed = (currentTimeMs - startTimeEpochMs).toFloat()
        return (elapsed / total).coerceIn(0f, 1f)
    }
}

data class ChannelSchedule(
    val channelType: ChannelType,
    val dayStartEpochMs: Long,
    val dayEndEpochMs: Long,
    val firstHalfDurationMs: Long = (dayEndEpochMs - dayStartEpochMs) / 2,
    val blocks: List<ScheduleBlock> = emptyList()
) {
    fun getCurrentBlock(currentTimeMs: Long): ScheduleBlock? {
        return blocks.find { it.isCurrentlyAiring(currentTimeMs) }
    }

    fun getNextBlock(currentTimeMs: Long): ScheduleBlock? {
        val current = getCurrentBlock(currentTimeMs) ?: return blocks.firstOrNull { it.startTimeEpochMs > currentTimeMs }
        val idx = blocks.indexOf(current)
        return if (idx >= 0 && idx < blocks.size - 1) blocks[idx + 1] else null
    }

    fun getUpcomingBlocks(currentTimeMs: Long, limit: Int = 10): List<ScheduleBlock> {
        return blocks.filter { it.endTimeEpochMs > currentTimeMs }.take(limit)
    }
}

data class ServerConfig(
    val serverUrl: String = "http://192.168.1.100:8096",
    val apiKey: String = "481bec17d65f43f593b7d1b4fc0f58b9",
    val isDemoMode: Boolean = false,
    val moviesLibraryId: String = "",
    val seriesLibraryId: String = "",
    val cartoonsLibraryId: String = "",
    val animeLibraryId: String = ""
)

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val serverName: String, val version: String, val isLocal: Boolean) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
