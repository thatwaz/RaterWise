package com.thatwaz.raterwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thatwaz.raterwise.data.model.DailyWorkSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyWorkSummaryDao {
    @Query("SELECT * FROM daily_work_summaries WHERE date = :date")
    fun getDailyWorkSummary(date: String): Flow<DailyWorkSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyWorkSummary(summary: DailyWorkSummary)
}
