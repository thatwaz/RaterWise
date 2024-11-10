package com.thatwaz.raterwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.thatwaz.raterwise.data.model.SessionWithTasks
import com.thatwaz.raterwise.data.model.SessionWithTasksAndEntries
import com.thatwaz.raterwise.data.model.TaskEntry

@Dao
interface TimeTrackingDao {

    // Insert a new session with tasks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionWithTasks(session: SessionWithTasks): Long


//    // Insert a single task entry
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertTaskEntry(taskEntry: TaskEntry)

    // Insert multiple task entries for a session
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskEntries(taskEntries: List<TaskEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskEntry(taskEntry: TaskEntry): Long

    @Update
    suspend fun updateTaskEntry(taskEntry: TaskEntry)


    @Query("SELECT * FROM session_with_tasks WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: kotlin.Long): SessionWithTasks?

    @Query("SELECT * FROM session_with_tasks ORDER BY sessionId DESC LIMIT 1")
    suspend fun getMostRecentSession(): SessionWithTasks?

    // Update an existing session with tasks
    @Update
    suspend fun updateSessionWithTasks(sessionWithTasks: SessionWithTasks)



    @Transaction
    @Query("SELECT * FROM session_with_tasks WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getSessionsWithTasksInDateRange(startDate: String, endDate: String): List<SessionWithTasksAndEntries>


    // Retrieve all sessions with tasks for the current week or custom grouping
    @Transaction
    @Query("SELECT * FROM session_with_tasks ORDER BY date DESC")
    suspend fun getAllSessions(): List<SessionWithTasks>

    // Retrieve task entries by session ID
    @Query("SELECT * FROM task_entries WHERE sessionId = :sessionId")
    suspend fun getTaskEntriesBySessionId(sessionId: Int): List<TaskEntry>



    @Query("SELECT * FROM session_with_tasks WHERE sessionId = :sessionId")
    suspend fun getSessionWithTasksById(sessionId: Long): SessionWithTasks?



    // Delete all sessions
    @Query("DELETE FROM session_with_tasks")
    suspend fun deleteAllSessions()

    // Delete all task entries
    @Query("DELETE FROM task_entries")
    suspend fun deleteAllTaskEntries()



}

