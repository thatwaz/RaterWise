package com.thatwaz.raterwise.data.repository

import com.thatwaz.raterwise.data.model.DailyWorkSummary
import com.thatwaz.raterwise.data.model.Session
import com.thatwaz.raterwise.data.model.TaskTimeEntry
import com.thatwaz.raterwise.data.model.WorkPeriod
import kotlinx.coroutines.flow.Flow

// CLean up unused items and refactor for the db update



interface TimeTrackingRepository {

    // Task Time Entry operations
    fun getTimeEntriesByDate(date: String): Flow<List<TaskTimeEntry>>
    suspend fun insertTaskTimeEntry(taskTimeEntry: TaskTimeEntry)
    fun getAllTimeEntries(): Flow<List<TaskTimeEntry>>
    suspend fun updateTimeEntry(taskTimeEntry: TaskTimeEntry)
    suspend fun deleteAllTimeEntries()
    suspend fun deleteAllSessions()

    // Daily Work Summary operations
    fun getDailySummary(date: String): Flow<DailyWorkSummary>
    suspend fun insertDailyWorkSummary(summary: DailyWorkSummary)

    // Work Period operations
    fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod>
    suspend fun insertWorkPeriod(workPeriod: WorkPeriod)
    suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod>

    // Session operations
    suspend fun getActiveSession(): Session?
    suspend fun saveSession(session: Session)
    suspend fun clearSession()

    fun getAllSessions(): Flow<List<Session>>


    suspend fun getActiveTask(): TaskTimeEntry? // Define the function to fetch the active task

    // Utility methods
    fun calculateOverUnderAET(duration: Long, expectedDuration: Int): Long
}

//interface TimeTrackingRepository {
//
//    // Task Time Entry operations
//    fun getTimeEntriesByDate(date: String): Flow<List<TaskTimeEntry>>
//    suspend fun insertTaskTimeEntry(taskTimeEntry: TaskTimeEntry)
//    suspend fun submitTimeEntry(date: String, entry: TaskTimeEntry)
//    fun getAllTimeEntries(): Flow<List<TaskTimeEntry>>
//    suspend fun updateTimeEntry(taskTimeEntry: TaskTimeEntry)
//    suspend fun deleteAllTimeEntries()
//
//    // Daily Work Summary operations
//    fun getDailySummary(date: String): Flow<DailyWorkSummary>
//    suspend fun insertDailyWorkSummary(summary: DailyWorkSummary)
//
//    // Work Period operations
//    fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod>
//    suspend fun insertWorkPeriod(workPeriod: WorkPeriod)
//    suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod>
//
//    // Session operations
//    suspend fun getSession(): Session?
//    suspend fun saveSession(session: Session)
//    suspend fun clearSession()
//
//    suspend fun getActiveTask(): TaskTimeEntry? // Define the function to fetch the active task
//
//    // Utility methods
//    fun calculateOverUnderAET(duration: Long, expectedDuration: Int): Long
//
//}

//interface TimeTrackingRepository {
//
//    // Time Entry operations
//    fun getTimeEntriesByDate(date: String): Flow<List<TaskTimeEntry>>
//    suspend fun insertTimeEntry(taskTimeEntry: TaskTimeEntry)  // Re-add this method
//    suspend fun submitTimeEntry(date: String, entry: TaskTimeEntry)
//    fun getAllTimeEntries(): Flow<List<TaskTimeEntry>>
//    suspend fun updateTimeEntry(taskTimeEntry: TaskTimeEntry)
//    suspend fun deleteAllTimeEntries()
//
//    // Daily Work Summary operations
//    fun getDailySummary(date: String): Flow<DailyWorkSummary>
//    suspend fun insertDailyWorkSummary(summary: DailyWorkSummary)
//
//    // Work Period operations
//    fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod>
//    suspend fun insertWorkPeriod(workPeriod: WorkPeriod)
//    suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod>
//
//    // Session operations
//    suspend fun getSession(): Session?
//    suspend fun saveSession(session: Session)
//    suspend fun clearSession()
//
//    fun calculateOverUnderAET(duration: Int, expectedDuration: Int): Int
//}

//
//interface TimeTrackingRepository {
//
//    // Existing methods
//    fun getTimeEntriesByDate(date: String): Flow<List<TimeEntry>>
////    suspend fun insertTimeEntry(timeEntry: TimeEntry)
//    suspend fun submitTimeEntry(date: String, entry: TimeEntry)
//    fun getAllTimeEntries(): Flow<List<TimeEntry>>
//    suspend fun updateTimeEntry(timeEntry: TimeEntry)
//    suspend fun deleteAllTimeEntries()
//
//    fun getDailySummary(date: String): Flow<DailyWorkSummary>
//    suspend fun insertDailyWorkSummary(summary: DailyWorkSummary)
//    fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod>
//    suspend fun insertWorkPeriod(workPeriod: WorkPeriod)
//    suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod>
//
//    // New Session operations
//    suspend fun getSession(): Session?
//    suspend fun saveSession(session: Session)
//    suspend fun clearSession()
//}


//interface TimeTrackingRepository {
//    fun getTimeEntriesByDate(date: String): Flow<List<TimeEntry>>
//    suspend fun insertTimeEntry(timeEntry: TimeEntry)
//    suspend fun submitTimeEntry(date: String, entry: TimeEntry)
//
//    fun getAllTimeEntries(): Flow<List<TimeEntry>>
//
//    // Update the time entry
//    suspend fun updateTimeEntry(timeEntry: TimeEntry)
//
//    suspend fun deleteAllTimeEntries()
//
//    // DailyWorkSummary operations
//    fun getDailySummary(date: String): Flow<DailyWorkSummary>
//    suspend fun insertDailyWorkSummary(summary: DailyWorkSummary)
//
//    // WorkPeriod operations
//    fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod>
//    suspend fun insertWorkPeriod(workPeriod: WorkPeriod)
//
//    suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod>
//}





