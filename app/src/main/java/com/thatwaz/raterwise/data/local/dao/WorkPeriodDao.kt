package com.thatwaz.raterwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thatwaz.raterwise.data.model.WorkPeriod
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkPeriodDao {
    @Query("SELECT * FROM work_periods WHERE startDate = :startDate AND endDate = :endDate")
    fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkPeriod(workPeriod: WorkPeriod)
}
