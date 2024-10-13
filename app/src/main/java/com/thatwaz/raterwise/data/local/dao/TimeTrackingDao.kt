package com.thatwaz.raterwise.data.local.dao



import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.thatwaz.raterwise.data.model.DailyWorkSummary
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.data.model.WorkPeriod
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeTrackingDao {

    // TimeEntry DAO operations
    @Query("SELECT * FROM time_entries WHERE date = :date")
    fun getTimeEntriesByDate(date: String): Flow<List<TimeEntry>>

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertTimeEntry(timeEntry: TimeEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeEntry(timeEntry: TimeEntry): Long

    @Query("SELECT * FROM time_entries")
    fun getAllTimeEntries(): Flow<List<TimeEntry>>

    @Update
    suspend fun updateTimeEntry(timeEntry: TimeEntry)

    @Delete
    suspend fun deleteTimeEntry(timeEntry: TimeEntry)

    @Query("DELETE FROM time_entries") // Replace `time_entry` with your table name
    suspend fun deleteAllTimeEntries()


    @Query("SELECT * FROM daily_work_summaries WHERE date = :date")
    fun getDailyWorkSummary(date: String): Flow<DailyWorkSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyWorkSummary(summary: DailyWorkSummary)

    @Update
    suspend fun updateDailyWorkSummary(summary: DailyWorkSummary)

    @Delete
    suspend fun deleteDailyWorkSummary(summary: DailyWorkSummary)

    // WorkPeriod DAO operations
    @Query("SELECT * FROM work_periods WHERE startDate = :startDate AND endDate = :endDate")
    fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkPeriod(workPeriod: WorkPeriod)

    @Update
    suspend fun updateWorkPeriod(workPeriod: WorkPeriod)

    @Delete
    suspend fun deleteWorkPeriod(workPeriod: WorkPeriod)
}
