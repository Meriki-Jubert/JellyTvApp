package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.engine.TvSchedulingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

class ScheduleRepository(
    private val appDatabase: AppDatabase,
    private val jellyfinRepository: JellyfinRepository
) {
    private val scheduleDao = appDatabase.scheduleDao()
    private val showProgressDao = appDatabase.showProgressDao()
    private val movieHistoryDao = appDatabase.movieHistoryDao()

    fun observeChannelSchedule(channelType: ChannelType): Flow<ChannelSchedule?> {
        return scheduleDao.getScheduleForChannel(channelType.id).map { entities ->
            if (entities.isEmpty()) null
            else {
                val blocks = entities.map { it.toDomainModel() }
                val start = blocks.minOfOrNull { it.startTimeEpochMs } ?: 0L
                val end = blocks.maxOfOrNull { it.endTimeEpochMs } ?: 0L
                ChannelSchedule(
                    channelType = channelType,
                    dayStartEpochMs = start,
                    dayEndEpochMs = end,
                    firstHalfDurationMs = (end - start) / 2,
                    blocks = blocks
                )
            }
        }
    }

    suspend fun getOrGenerateAllSchedules(currentTimeMs: Long = System.currentTimeMillis()): Map<ChannelType, ChannelSchedule> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<ChannelType, ChannelSchedule>()
        for (type in ChannelType.entries) {
            val schedule = getOrGenerateScheduleForChannel(type, currentTimeMs)
            result[type] = schedule
        }
        result
    }

    suspend fun forceRegenerateAllSchedules(currentTimeMs: Long = System.currentTimeMillis()): Map<ChannelType, ChannelSchedule> = withContext(Dispatchers.IO) {
        scheduleDao.clearAllSchedules()
        scheduleDao.clearAllChannelMeta()
        val result = mutableMapOf<ChannelType, ChannelSchedule>()
        for (type in ChannelType.entries) {
            val schedule = getOrGenerateScheduleForChannel(type, currentTimeMs, forceFresh = true)
            result[type] = schedule
        }
        result
    }

    suspend fun getOrGenerateScheduleForChannel(
        channelType: ChannelType,
        currentTimeMs: Long = System.currentTimeMillis(),
        forceFresh: Boolean = false
    ): ChannelSchedule = withContext(Dispatchers.IO) {
        val meta = if (forceFresh) null else scheduleDao.getChannelMeta(channelType.id)
        val cachedEntities = if (forceFresh) emptyList() else scheduleDao.getScheduleForChannelOnce(channelType.id)

        // If cached schedule covers current time and has blocks, return it
        if (!forceFresh && meta != null && cachedEntities.isNotEmpty() && currentTimeMs in meta.dayStartEpochMs until meta.dayEndEpochMs) {
            val blocks = cachedEntities.map { it.toDomainModel() }
            return@withContext ChannelSchedule(
                channelType = channelType,
                dayStartEpochMs = meta.dayStartEpochMs,
                dayEndEpochMs = meta.dayEndEpochMs,
                firstHalfDurationMs = meta.firstHalfDurationMs,
                blocks = blocks
            )
        }

        // Otherwise generate a new schedule starting from reference base time
        val referenceStartTime = calculateDayStartBoundary(if (forceFresh) null else meta?.dayEndEpochMs, currentTimeMs)
        val newSchedule = when (channelType) {
            ChannelType.MOVIES -> {
                val catalog = jellyfinRepository.fetchMovies()
                val history = movieHistoryDao.getMovieHistoryForChannel(channelType.id)
                TvSchedulingEngine.generateMovieSchedule(
                    channelType = channelType,
                    catalog = catalog,
                    history = history,
                    referenceEpochMs = referenceStartTime,
                    onUpdateHistory = { updatedHistory ->
                        // Persist rotation history
                        kotlinx.coroutines.runBlocking {
                            movieHistoryDao.saveAllMovieHistory(updatedHistory)
                        }
                    }
                )
            }
            ChannelType.SERIES, ChannelType.CARTOONS, ChannelType.ANIME -> {
                val showsPool = jellyfinRepository.fetchSeries(channelType)
                val progressList = showProgressDao.getShowProgressForChannel(channelType.id)
                TvSchedulingEngine.generateSeriesSchedule(
                    channelType = channelType,
                    showsPool = showsPool,
                    showProgressList = progressList,
                    referenceEpochMs = referenceStartTime,
                    onUpdateProgress = { updatedProgress ->
                        kotlinx.coroutines.runBlocking {
                            showProgressDao.saveAllShowProgress(updatedProgress)
                        }
                    }
                )
            }
        }

        // Save generated schedule blocks and meta to Room
        scheduleDao.deleteScheduleForChannel(channelType.id)
        scheduleDao.deleteChannelMeta(channelType.id)
        val entities = newSchedule.blocks.map { it.toEntity() }
        scheduleDao.insertBlocks(entities)
        scheduleDao.saveChannelMeta(
            ChannelScheduleMetaEntity(
                channelId = channelType.id,
                dayStartEpochMs = newSchedule.dayStartEpochMs,
                dayEndEpochMs = newSchedule.dayEndEpochMs,
                firstHalfDurationMs = newSchedule.firstHalfDurationMs,
                generatedAtEpochMs = currentTimeMs
            )
        )

        newSchedule
    }

    private fun calculateDayStartBoundary(lastEndEpochMs: Long?, currentTimeMs: Long): Long {
        if (lastEndEpochMs != null && lastEndEpochMs > 0L) {
            // Chain smoothly from last schedule boundary
            var chainedStart = lastEndEpochMs
            while (chainedStart + (24L * 60 * 60 * 1000) < currentTimeMs) {
                chainedStart += (24L * 60 * 60 * 1000)
            }
            return chainedStart
        }

        // Start from beginning of current day (midnight 00:00)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun ScheduleBlockEntity.toDomainModel(): ScheduleBlock {
        val mediaItem = if (!isBreak && itemId != null) {
            MediaItem(
                id = itemId,
                title = itemTitle ?: "Untitled",
                seriesName = itemSeriesName,
                seriesId = itemSeriesId,
                seasonNumber = itemSeasonNumber,
                episodeNumber = itemEpisodeNumber,
                overview = itemOverview ?: "",
                runtimeMinutes = itemRuntimeMinutes ?: 30,
                posterUrl = itemPosterUrl,
                backdropUrl = itemBackdropUrl,
                streamUrl = itemStreamUrl ?: "",
                mediaType = MediaType.valueOf(itemMediaType ?: "MOVIE"),
                channelType = ChannelType.fromId(channelId),
                genres = itemGenres?.split(",") ?: emptyList(),
                releaseYear = itemReleaseYear
            )
        } else null

        val nextItem = if (isBreak && nextItemId != null) {
            MediaItem(
                id = nextItemId,
                title = nextItemTitle ?: "Upcoming",
                seriesName = nextItemSeriesName,
                seasonNumber = nextItemSeasonNumber,
                episodeNumber = nextItemEpisodeNumber,
                overview = nextItemOverview ?: "",
                runtimeMinutes = nextItemRuntimeMinutes ?: 30,
                posterUrl = nextItemPosterUrl,
                backdropUrl = nextItemBackdropUrl,
                mediaType = MediaType.valueOf(itemMediaType ?: "MOVIE"),
                channelType = ChannelType.fromId(channelId)
            )
        } else null

        return ScheduleBlock(
            blockId = blockId,
            channelId = channelId,
            startTimeEpochMs = startTimeEpochMs,
            endTimeEpochMs = endTimeEpochMs,
            item = mediaItem,
            isBreak = isBreak,
            breakNextItem = nextItem,
            breakDurationMinutes = breakDurationMinutes
        )
    }

    private fun ScheduleBlock.toEntity(): ScheduleBlockEntity {
        return ScheduleBlockEntity(
            blockId = blockId,
            channelId = channelId,
            startTimeEpochMs = startTimeEpochMs,
            endTimeEpochMs = endTimeEpochMs,
            isBreak = isBreak,
            breakDurationMinutes = breakDurationMinutes,
            itemId = item?.id,
            itemTitle = item?.title,
            itemSeriesName = item?.seriesName,
            itemSeriesId = item?.seriesId,
            itemSeasonNumber = item?.seasonNumber,
            itemEpisodeNumber = item?.episodeNumber,
            itemOverview = item?.overview,
            itemRuntimeMinutes = item?.runtimeMinutes,
            itemPosterUrl = item?.posterUrl,
            itemBackdropUrl = item?.backdropUrl,
            itemStreamUrl = item?.streamUrl,
            itemMediaType = item?.mediaType?.name,
            itemGenres = item?.genres?.joinToString(","),
            itemReleaseYear = item?.releaseYear,
            nextItemId = breakNextItem?.id,
            nextItemTitle = breakNextItem?.title,
            nextItemSeriesName = breakNextItem?.seriesName,
            nextItemSeasonNumber = breakNextItem?.seasonNumber,
            nextItemEpisodeNumber = breakNextItem?.episodeNumber,
            nextItemOverview = breakNextItem?.overview,
            nextItemPosterUrl = breakNextItem?.posterUrl,
            nextItemBackdropUrl = breakNextItem?.backdropUrl,
            nextItemRuntimeMinutes = breakNextItem?.runtimeMinutes
        )
    }
}
