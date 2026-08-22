package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_blocks WHERE channelId = :channelId ORDER BY startTimeEpochMs ASC")
    fun getScheduleForChannel(channelId: String): Flow<List<ScheduleBlockEntity>>

    @Query("SELECT * FROM schedule_blocks WHERE channelId = :channelId ORDER BY startTimeEpochMs ASC")
    suspend fun getScheduleForChannelOnce(channelId: String): List<ScheduleBlockEntity>

    @Query("SELECT * FROM schedule_blocks ORDER BY startTimeEpochMs ASC")
    fun getAllScheduleBlocks(): Flow<List<ScheduleBlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<ScheduleBlockEntity>)

    @Query("DELETE FROM schedule_blocks WHERE channelId = :channelId")
    suspend fun deleteScheduleForChannel(channelId: String)

    @Query("DELETE FROM schedule_blocks")
    suspend fun clearAllSchedules()

    // Channel metadata
    @Query("SELECT * FROM channel_metadata WHERE channelId = :channelId")
    suspend fun getChannelMeta(channelId: String): ChannelScheduleMetaEntity?

    @Query("SELECT * FROM channel_metadata")
    fun getAllChannelMeta(): Flow<List<ChannelScheduleMetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChannelMeta(meta: ChannelScheduleMetaEntity)

    @Query("DELETE FROM channel_metadata")
    suspend fun clearAllChannelMeta()

    @Query("DELETE FROM channel_metadata WHERE channelId = :channelId")
    suspend fun deleteChannelMeta(channelId: String)
}

@Dao
interface ShowProgressDao {
    @Query("SELECT * FROM show_progress WHERE channelId = :channelId ORDER BY lastCompletedEpochMs ASC")
    suspend fun getShowProgressForChannel(channelId: String): List<ShowProgressEntity>

    @Query("SELECT * FROM show_progress WHERE showId = :showId AND channelId = :channelId")
    suspend fun getProgressForShow(showId: String, channelId: String): ShowProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveShowProgress(progress: ShowProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAllShowProgress(progressList: List<ShowProgressEntity>)
}

@Dao
interface MovieHistoryDao {
    @Query("SELECT * FROM movie_history WHERE channelId = :channelId ORDER BY lastPlayedEpochMs ASC")
    suspend fun getMovieHistoryForChannel(channelId: String): List<MovieHistoryEntity>

    @Query("SELECT * FROM movie_history WHERE movieId = :movieId AND channelId = :channelId")
    suspend fun getMovieHistory(movieId: String, channelId: String): MovieHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMovieHistory(history: MovieHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAllMovieHistory(historyList: List<MovieHistoryEntity>)
}
