package com.thatwaz.raterwise.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.thatwaz.raterwise.data.model.TaskTimeEntry
import kotlinx.coroutines.flow.Flow



@Dao
interface TaskTimeTrackingDao {

    // Retrieve all tasks for a specific date
    @Query("SELECT * FROM task_time_entries WHERE date = :date")
    fun getTaskTimeEntriesByDate(date: String): Flow<List<TaskTimeEntry>>

    // Insert a new task time entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTimeEntry(taskTimeEntry: TaskTimeEntry): Long

    // Retrieve all task time entries
    @Query("SELECT * FROM task_time_entries")
    fun getAllTaskTimeEntries(): Flow<List<TaskTimeEntry>>

    // Update an existing task time entry
    @Update
    suspend fun updateTaskTimeEntry(taskTimeEntry: TaskTimeEntry)

    // Retrieve the active running task
    @Query("SELECT * FROM task_time_entries WHERE isTaskRunning = 1 LIMIT 1")
    suspend fun getActiveTask(): TaskTimeEntry?

    // Delete a specific task time entry
    @Delete
    suspend fun deleteTaskTimeEntry(taskTimeEntry: TaskTimeEntry)

    // Clear all task time entries
    @Query("DELETE FROM task_time_entries")
    suspend fun deleteAllTaskTimeEntries()

    // New: Retrieve tasks by session ID (if session tracking is needed)
    @Query("SELECT * FROM task_time_entries WHERE sessionId = :sessionId")
    fun getTasksBySessionId(sessionId: Int): Flow<List<TaskTimeEntry>>
}

//@Dao
//interface TaskTimeTrackingDao {
//
//    // Retrieve all tasks for a specific date
//    @Query("SELECT * FROM task_time_entries WHERE date = :date")
//    fun getTaskTimeEntriesByDate(date: String): Flow<List<TaskTimeEntry>>
//
//    // Insert a new task time entry
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertTaskTimeEntry(taskTimeEntry: TaskTimeEntry): Long
//
//    // Retrieve all task time entries
//    @Query("SELECT * FROM task_time_entries")
//    fun getAllTaskTimeEntries(): Flow<List<TaskTimeEntry>>
//
//    // Update an existing task time entry
//    @Update
//    suspend fun updateTaskTimeEntry(taskTimeEntry: TaskTimeEntry)
//
//    @Query("SELECT * FROM task_time_entries WHERE isTaskRunning = 1 LIMIT 1")
//    suspend fun getActiveTask(): TaskTimeEntry?
//
//    // Delete a specific task time entry
//    @Delete
//    suspend fun deleteTaskTimeEntry(taskTimeEntry: TaskTimeEntry)
//
//    // Clear all task time entries
//    @Query("DELETE FROM task_time_entries")
//    suspend fun deleteAllTaskTimeEntries()
//}


//@Dao
//interface TaskTimeTrackingDao {
//
//    // TimeEntry DAO operations
//    @Query("SELECT * FROM time_entries WHERE date = :date")
//    fun getTimeEntriesByDate(date: String): Flow<List<TaskTimeEntry>>
//
////    @Insert(onConflict = OnConflictStrategy.REPLACE)
////    suspend fun insertTimeEntry(timeEntry: TimeEntry)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertTimeEntry(taskTimeEntry: TaskTimeEntry): Long
//
//    @Query("SELECT * FROM time_entries")
//    fun getAllTimeEntries(): Flow<List<TaskTimeEntry>>
//
//    @Update
//    suspend fun updateTimeEntry(taskTimeEntry: TaskTimeEntry)
//
//    @Delete
//    suspend fun deleteTimeEntry(taskTimeEntry: TaskTimeEntry)
//
//    @Query("DELETE FROM time_entries") // Replace `time_entry` with your table name
//    suspend fun deleteAllTimeEntries()
//
//
//    @Query("SELECT * FROM daily_work_summaries WHERE date = :date")
//    fun getDailyWorkSummary(date: String): Flow<DailyWorkSummary>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertDailyWorkSummary(summary: DailyWorkSummary)
//
//    @Update
//    suspend fun updateDailyWorkSummary(summary: DailyWorkSummary)
//
//    @Delete
//    suspend fun deleteDailyWorkSummary(summary: DailyWorkSummary)
//
//    // WorkPeriod DAO operations
//    @Query("SELECT * FROM work_periods WHERE startDate = :startDate AND endDate = :endDate")
//    fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod>
//
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertWorkPeriod(workPeriod: WorkPeriod)
//
//    @Update
//    suspend fun updateWorkPeriod(workPeriod: WorkPeriod)
//
//    @Delete
//    suspend fun deleteWorkPeriod(workPeriod: WorkPeriod)
//}
