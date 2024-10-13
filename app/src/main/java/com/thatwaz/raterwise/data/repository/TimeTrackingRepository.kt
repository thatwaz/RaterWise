package com.thatwaz.raterwise.data.repository

import com.thatwaz.raterwise.data.model.DailyWorkSummary
import com.thatwaz.raterwise.data.model.Session
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.data.model.WorkPeriod
import kotlinx.coroutines.flow.Flow


interface TimeTrackingRepository {

    // Time Entry operations
    fun getTimeEntriesByDate(date: String): Flow<List<TimeEntry>>
    suspend fun insertTimeEntry(timeEntry: TimeEntry)  // Re-add this method
    suspend fun submitTimeEntry(date: String, entry: TimeEntry)
    fun getAllTimeEntries(): Flow<List<TimeEntry>>
    suspend fun updateTimeEntry(timeEntry: TimeEntry)
    suspend fun deleteAllTimeEntries()

    // Daily Work Summary operations
    fun getDailySummary(date: String): Flow<DailyWorkSummary>
    suspend fun insertDailyWorkSummary(summary: DailyWorkSummary)

    // Work Period operations
    fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod>
    suspend fun insertWorkPeriod(workPeriod: WorkPeriod)
    suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod>

    // Session operations
    suspend fun getSession(): Session?
    suspend fun saveSession(session: Session)
    suspend fun clearSession()
}

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





