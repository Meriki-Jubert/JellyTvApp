package com.example.engine

import com.example.data.local.MovieHistoryEntity
import com.example.data.local.ShowProgressEntity
import com.example.data.model.ChannelSchedule
import com.example.data.model.ChannelType
import com.example.data.model.MediaItem
import com.example.data.model.ScheduleBlock
import java.util.UUID

object TvSchedulingEngine {

    private const val TWELVE_HOURS_MS = 12L * 60 * 60 * 1000 // 12h = 43,200,000 ms
    private const val ONE_DAY_MS = 24L * 60 * 60 * 1000

    /**
     * Generates a movie channel schedule adhering strictly to the 12h first half + mirror 2A logic.
     * Uses least-recently-played rotation so movies cycle evenly.
     */
    fun generateMovieSchedule(
        channelType: ChannelType = ChannelType.MOVIES,
        catalog: List<MediaItem>,
        history: List<MovieHistoryEntity>,
        referenceEpochMs: Long,
        onUpdateHistory: (List<MovieHistoryEntity>) -> Unit = {}
    ): ChannelSchedule {
        if (catalog.isEmpty()) {
            return ChannelSchedule(
                channelType = channelType,
                dayStartEpochMs = referenceEpochMs,
                dayEndEpochMs = referenceEpochMs + ONE_DAY_MS,
                blocks = emptyList()
            )
        }

        // Sort catalog by least-recently-played (items not in history come first, then oldest lastPlayedEpochMs)
        val historyMap = history.associateBy { it.movieId }
        val sortedMovies = catalog.sortedWith(
            compareBy(
                { historyMap[it.id]?.lastPlayedEpochMs ?: 0L },
                { it.id }
            )
        ).toMutableList()

        val breakDurationMs = channelType.defaultBreakMinutes * 60 * 1000L
        val firstHalfBlocks = mutableListOf<ScheduleBlock>()
        var runningTimeMs = referenceEpochMs
        var poolIndex = 0
        val usedMovieIds = mutableListOf<String>()

        // Build first half until running duration from start >= 12h
        while ((runningTimeMs - referenceEpochMs) < TWELVE_HOURS_MS) {
            val movie = sortedMovies[poolIndex % sortedMovies.size]
            val movieDurationMs = (movie.runtimeMinutes.coerceAtLeast(10)) * 60 * 1000L
            val movieStart = runningTimeMs
            val movieEnd = movieStart + movieDurationMs

            // Movie block
            val movieBlockId = "blk_mov_${channelType.id}_${movie.id}_$movieStart"
            firstHalfBlocks.add(
                ScheduleBlock(
                    blockId = movieBlockId,
                    channelId = channelType.id,
                    startTimeEpochMs = movieStart,
                    endTimeEpochMs = movieEnd,
                    item = movie,
                    isBreak = false
                )
            )
            usedMovieIds.add(movie.id)
            runningTimeMs = movieEnd

            // Look ahead for next movie (for break preview)
            poolIndex++
            val nextMovie = sortedMovies[poolIndex % sortedMovies.size]

            // 60-min Break block
            val breakStart = runningTimeMs
            val breakEnd = breakStart + breakDurationMs
            val breakBlockId = "blk_brk_${channelType.id}_$breakStart"
            firstHalfBlocks.add(
                ScheduleBlock(
                    blockId = breakBlockId,
                    channelId = channelType.id,
                    startTimeEpochMs = breakStart,
                    endTimeEpochMs = breakEnd,
                    item = null,
                    isBreak = true,
                    breakNextItem = nextMovie,
                    breakDurationMinutes = channelType.defaultBreakMinutes
                )
            )
            runningTimeMs = breakEnd
        }

        // Duration of first half is A = (runningTimeMs - referenceEpochMs)
        val durationA = runningTimeMs - referenceEpochMs

        // Mirror: duplicate that exact sequence starting immediately at timestamp A, running until 2A
        val secondHalfBlocks = firstHalfBlocks.map { block ->
            val shiftedStart = block.startTimeEpochMs + durationA
            val shiftedEnd = block.endTimeEpochMs + durationA
            block.copy(
                blockId = "${block.blockId}_mirrored",
                startTimeEpochMs = shiftedStart,
                endTimeEpochMs = shiftedEnd
            )
        }

        // Update movie history for least recently played tracking
        val updatedHistory = usedMovieIds.distinct().map { id ->
            MovieHistoryEntity(
                movieId = id,
                channelId = channelType.id,
                movieTitle = catalog.find { it.id == id }?.title ?: id,
                lastPlayedEpochMs = referenceEpochMs
            )
        }
        onUpdateHistory(updatedHistory)

        val fullBlocks = firstHalfBlocks + secondHalfBlocks
        val dayEndEpochMs = referenceEpochMs + (durationA * 2)

        return ChannelSchedule(
            channelType = channelType,
            dayStartEpochMs = referenceEpochMs,
            dayEndEpochMs = dayEndEpochMs,
            firstHalfDurationMs = durationA,
            blocks = fullBlocks
        )
    }

    /**
     * Generates a series channel schedule (TV / Cartoons / Anime) adhering to:
     * - Multi-show rotation across the broadcast day (prevents 24h repeats of a single show).
     * - Channel-specific starting show offsets to ensure Prime TV, Toon Wave, and Anime Pulse don't clash.
     * - Short show (<30 min) -> 2 consecutive episodes per session.
     * - Long show (>=30 min) -> 1-2 consecutive episodes per session.
     * - 30 min break after each session with preview of the next rotating show.
     * - Sequential episode progression preserved per show.
     */
    fun generateSeriesSchedule(
        channelType: ChannelType,
        showsPool: Map<String, List<MediaItem>>,
        showProgressList: List<ShowProgressEntity>,
        referenceEpochMs: Long,
        targetDurationMs: Long = ONE_DAY_MS,
        onUpdateProgress: (List<ShowProgressEntity>) -> Unit = {}
    ): ChannelSchedule {
        if (showsPool.isEmpty()) {
            return ChannelSchedule(
                channelType = channelType,
                dayStartEpochMs = referenceEpochMs,
                dayEndEpochMs = referenceEpochMs + targetDurationMs,
                blocks = emptyList()
            )
        }

        val rawShowIds = showsPool.keys.filter { (showsPool[it]?.size ?: 0) > 0 }.toList()
        if (rawShowIds.isEmpty()) {
            return ChannelSchedule(
                channelType = channelType,
                dayStartEpochMs = referenceEpochMs,
                dayEndEpochMs = referenceEpochMs + targetDurationMs,
                blocks = emptyList()
            )
        }

        // Channel-specific offset ensures that if shows are shared across pools, each channel starts on a different show
        val channelOffset = when (channelType) {
            ChannelType.SERIES -> 0
            ChannelType.CARTOONS -> 1
            ChannelType.ANIME -> 2
            else -> 0
        } % rawShowIds.size

        val orderedShowIds = rawShowIds.indices.map { i ->
            rawShowIds[(i + channelOffset) % rawShowIds.size]
        }

        // Maintain working progress state map for this channel
        val progressMap = showProgressList.associateBy { it.showId }.toMutableMap()

        val breakDurationMs = channelType.defaultBreakMinutes * 60 * 1000L
        val blocks = mutableListOf<ScheduleBlock>()
        var runningTimeMs = referenceEpochMs
        var rotationSlotIndex = 0

        while ((runningTimeMs - referenceEpochMs) < targetDurationMs) {
            val currentShowId = orderedShowIds[rotationSlotIndex % orderedShowIds.size]
            val episodes = showsPool[currentShowId] ?: emptyList()

            if (episodes.isEmpty()) {
                rotationSlotIndex++
                continue
            }

            var currentProgress = progressMap[currentShowId] ?: ShowProgressEntity(
                showId = currentShowId,
                channelId = channelType.id,
                showName = episodes.firstOrNull()?.seriesName ?: currentShowId,
                currentEpisodeIndex = 0,
                totalEpisodes = episodes.size,
                lastCompletedEpochMs = 0L,
                isCompleted = false
            )

            // Determine if short (<30 min) or long (>=30 min) show
            val avgRuntime = episodes.map { it.runtimeMinutes }.average().toInt().coerceAtLeast(10)
            val sessionEpisodeCount = when {
                avgRuntime < 30 -> 2 // 2 eps for cartoons/anime/sitcoms (~20-25m each)
                avgRuntime >= 42 -> 1 // 1 ep for 45-60m dramas
                else -> 2 // 2 eps for 30m shows
            }

            // Extract consecutive episodes for this session block
            val sessionEpisodes = mutableListOf<MediaItem>()
            var epIndex = currentProgress.currentEpisodeIndex

            for (i in 0 until sessionEpisodeCount) {
                if (epIndex >= episodes.size) {
                    // Loop back to beginning if completed the show
                    epIndex = 0
                }
                sessionEpisodes.add(episodes[epIndex])
                epIndex++
            }

            // Update show progress
            val showJustCompleted = (epIndex >= episodes.size)
            currentProgress = currentProgress.copy(
                currentEpisodeIndex = if (showJustCompleted) 0 else epIndex,
                totalEpisodes = episodes.size,
                lastCompletedEpochMs = if (showJustCompleted) runningTimeMs else currentProgress.lastCompletedEpochMs,
                isCompleted = showJustCompleted
            )
            progressMap[currentShowId] = currentProgress

            // Add episode blocks to schedule
            for (ep in sessionEpisodes) {
                val epDurationMs = (ep.runtimeMinutes.coerceAtLeast(5)) * 60 * 1000L
                val epStart = runningTimeMs
                val epEnd = epStart + epDurationMs
                val blockId = "blk_ep_${channelType.id}_${ep.id}_$epStart"

                blocks.add(
                    ScheduleBlock(
                        blockId = blockId,
                        channelId = channelType.id,
                        startTimeEpochMs = epStart,
                        endTimeEpochMs = epEnd,
                        item = ep,
                        isBreak = false
                    )
                )
                runningTimeMs = epEnd
            }

            // Determine next show in rotation for break preview
            val nextSlotIndex = rotationSlotIndex + 1
            val nextShowId = orderedShowIds[nextSlotIndex % orderedShowIds.size]
            val nextShowEpisodes = showsPool[nextShowId] ?: emptyList()
            val nextShowProgress = progressMap[nextShowId]
            val nextEpIdx = nextShowProgress?.currentEpisodeIndex ?: 0
            val nextItemForBreak = nextShowEpisodes.getOrNull(nextEpIdx) ?: nextShowEpisodes.firstOrNull()

            // 30-min Break block after each program session
            val breakStart = runningTimeMs
            val breakEnd = breakStart + breakDurationMs
            val breakBlockId = "blk_brk_${channelType.id}_$breakStart"

            blocks.add(
                ScheduleBlock(
                    blockId = breakBlockId,
                    channelId = channelType.id,
                    startTimeEpochMs = breakStart,
                    endTimeEpochMs = breakEnd,
                    item = null,
                    isBreak = true,
                    breakNextItem = nextItemForBreak,
                    breakDurationMinutes = channelType.defaultBreakMinutes
                )
            )
            runningTimeMs = breakEnd

            // Move to the next rotating show in the lineup
            rotationSlotIndex++
        }

        onUpdateProgress(progressMap.values.toList())

        return ChannelSchedule(
            channelType = channelType,
            dayStartEpochMs = referenceEpochMs,
            dayEndEpochMs = runningTimeMs,
            blocks = blocks
        )
    }
}

