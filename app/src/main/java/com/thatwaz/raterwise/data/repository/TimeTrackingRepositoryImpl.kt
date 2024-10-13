package com.thatwaz.raterwise.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.thatwaz.raterwise.data.local.dao.SessionDao
import com.thatwaz.raterwise.data.local.dao.TimeTrackingDao
import com.thatwaz.raterwise.data.model.DailyWorkSummary
import com.thatwaz.raterwise.data.model.Session
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.data.model.WorkPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import javax.inject.Inject

class TimeTrackingRepositoryImpl @Inject constructor(
    private val timeTrackingDao: TimeTrackingDao,  // For time entries and summaries
    private val sessionDao: SessionDao  // For session management
) : TimeTrackingRepository {

    // TimeEntry operations
    override fun getTimeEntriesByDate(date: String): Flow<List<TimeEntry>> {
        Log.d("TimeTrackingRepo", "Fetching time entries for date: $date")
        return timeTrackingDao.getTimeEntriesByDate(date)
    }

    override fun getAllTimeEntries(): Flow<List<TimeEntry>> =
        timeTrackingDao.getAllTimeEntries()
    override suspend fun insertTimeEntry(timeEntry: TimeEntry) {
        Log.d(
            "TimeTrackingRepo",
            "Inserting TimeEntry: Start: ${timeEntry.startTime}, End: ${timeEntry.endTime}, Date: ${timeEntry.date}"
        )
        val insertedId = timeTrackingDao.insertTimeEntry(timeEntry) // Returns Long
        Log.d("TimeTrackingRepo", "Inserted TimeEntry with ID: $insertedId")
    }

//    override suspend fun insertTimeEntry(timeEntry: TimeEntry) {
//        val insertedId = timeTrackingDao.insertTimeEntry(timeEntry)
//        Log.d("TimeTrackingRepo", "Inserted TimeEntry with ID: $insertedId")
//    }

//    override suspend fun insertTimeEntry(timeEntry: TimeEntry) {
//        Log.d(
//            "TimeTrackingRepo",
//            "Inserting TimeEntry: Start: ${timeEntry.startTime}, " +
//                    "End: ${timeEntry.endTime}, Date: ${timeEntry.date}"
//        )
//        timeTrackingDao.insertTimeEntry(timeEntry)  // Ensure it works independently
//        Log.d("TimeTrackingRepo", "Inserted TimeEntry with ID: ${timeEntry.id}")
//    }

    override suspend fun submitTimeEntry(date: String, entry: TimeEntry) {
        val updatedEntry = entry.copy(isSubmitted = true)
        Log.d("TimeTrackingRepo", "Submitting TimeEntry for date: $date, ID: ${entry.id}")
        timeTrackingDao.updateTimeEntry(updatedEntry)
    }

    override suspend fun updateTimeEntry(timeEntry: TimeEntry) {
        timeTrackingDao.updateTimeEntry(timeEntry)
    }

    override suspend fun deleteAllTimeEntries() {
        timeTrackingDao.deleteAllTimeEntries()
    }

    // DailyWorkSummary operations
    override fun getDailySummary(date: String): Flow<DailyWorkSummary> {
        Log.d("TimeTrackingRepo", "Fetching daily summary for date: $date")
        return timeTrackingDao.getDailyWorkSummary(date)
    }

    override suspend fun insertDailyWorkSummary(summary: DailyWorkSummary) {
        Log.d("TimeTrackingRepo", "Inserting DailyWorkSummary for date: ${summary.date}")
        timeTrackingDao.insertDailyWorkSummary(summary)
    }

    // WorkPeriod operations
    override fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod> {
        return timeTrackingDao.getWorkPeriod(startDate, endDate)
    }

    override suspend fun insertWorkPeriod(workPeriod: WorkPeriod) {
        timeTrackingDao.insertWorkPeriod(workPeriod)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod> = flow {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)  // Monday
        val endOfWeek = startOfWeek.plusDays(6)  // Sunday

        val currentWorkPeriod = timeTrackingDao
            .getWorkPeriod(startOfWeek.toString(), endOfWeek.toString())
            .first()
        emit(currentWorkPeriod)
    }

    // Session operations
    override suspend fun getSession(): Session? {
        val session = sessionDao.getSession()
        Log.d("TimeTrackingRepo", "Fetched session: $session")
        return session
    }

    override suspend fun saveSession(session: Session) {
        Log.d("TimeTrackingRepo", "Saving session: $session")
        sessionDao.saveSession(session)
    }

    override suspend fun clearSession() {
        Log.d("TimeTrackingRepo", "Clearing session")
        sessionDao.clearSession()
    }
}


//class TimeTrackingRepositoryImpl @Inject constructor(
//    private val timeTrackingDao: TimeTrackingDao, // For TimeEntry and related operations
//    private val sessionDao: SessionDao // For Session operations
//) : TimeTrackingRepository {
//
//    // TimeEntry operations
//    override fun getTimeEntriesByDate(date: String): Flow<List<TimeEntry>> {
//        Log.d("TimeTrackingRepo", "Fetching time entries for date: $date")
//        return timeTrackingDao.getTimeEntriesByDate(date)
//    }
//
//    override fun getAllTimeEntries(): Flow<List<TimeEntry>> = timeTrackingDao.getAllTimeEntries()
//
//    override suspend fun insertTimeEntry(timeEntry: TimeEntry) {
//        Log.d(
//            "TimeTrackingRepo",
//            "Inserting TimeEntry: Start: ${timeEntry.startTime}, End: ${timeEntry.endTime}, Date: ${timeEntry.date}"
//        )
//        timeTrackingDao.insertTimeEntry(timeEntry)
//        Log.d("TimeTrackingRepo", "Inserted TimeEntry with ID: ${timeEntry.id}")
//    }
//
//    override suspend fun updateTimeEntry(timeEntry: TimeEntry) {
//        timeTrackingDao.updateTimeEntry(timeEntry)
//    }
//
//    override suspend fun deleteAllTimeEntries() {
//        timeTrackingDao.deleteAllTimeEntries()
//    }
//
//    // DailyWorkSummary operations
//    override fun getDailySummary(date: String): Flow<DailyWorkSummary> {
//        Log.d("TimeTrackingRepo", "Fetching daily summary for date: $date")
//        return timeTrackingDao.getDailyWorkSummary(date)
//    }
//
//    override suspend fun insertDailyWorkSummary(summary: DailyWorkSummary) {
//        Log.d("TimeTrackingRepo", "Inserting DailyWorkSummary for date: ${summary.date}")
//        timeTrackingDao.insertDailyWorkSummary(summary)
//        Log.d("TimeTrackingRepo", "Inserted DailyWorkSummary with ID: ${summary.id}")
//    }
//
//    // WorkPeriod operations
//    override fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod> {
//        Log.d("TimeTrackingRepo", "Fetching work period from $startDate to $endDate")
//        return timeTrackingDao.getWorkPeriod(startDate, endDate)
//    }
//
//    override suspend fun insertWorkPeriod(workPeriod: WorkPeriod) {
//        Log.d("TimeTrackingRepo", "Inserting WorkPeriod: Start: ${workPeriod.startDate}, End: ${workPeriod.endDate}")
//        timeTrackingDao.insertWorkPeriod(workPeriod)
//        Log.d("TimeTrackingRepo", "Inserted WorkPeriod with ID: ${workPeriod.id}")
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod> {
//        val today = LocalDate.now()
//        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1) // Monday
//        val endOfWeek = startOfWeek.plusDays(6) // Sunday
//
//        Log.d("TimeTrackingRepo", "Calculating current work period: $startOfWeek to $endOfWeek")
//
//        return flow {
//            val currentWorkPeriod = timeTrackingDao.getWorkPeriod(
//                startOfWeek.toString(), endOfWeek.toString()
//            ).first()
//            Log.d("TimeTrackingRepo", "Current work period retrieved: $currentWorkPeriod")
//            emit(currentWorkPeriod)
//        }
//    }
//
//    override suspend fun submitTimeEntry(date: String, entry: TimeEntry) {
//        val updatedEntry = entry.copy(isSubmitted = true)
//        Log.d("TimeTrackingRepo", "Submitting TimeEntry for date: $date, ID: ${entry.id}")
//        timeTrackingDao.updateTimeEntry(updatedEntry)
//        Log.d(
//            "TimeTrackingRepo",
//            "TimeEntry submitted: ID: ${updatedEntry.id}, Start: ${updatedEntry.startTime}, End: ${updatedEntry.endTime}"
//        )
//    }
//
//    // Session operations
//    override suspend fun getSession(): Session? {
//        val session = sessionDao.getSession()
//        Log.d("TimeTrackingRepo", "Fetched session: $session")
//        return session
//    }
//
//    override suspend fun saveSession(session: Session) {
//        Log.d("TimeTrackingRepo", "Saving session: $session")
//        sessionDao.saveSession(session)
//    }
//
//    override suspend fun clearSession() {
//        Log.d("TimeTrackingRepo", "Clearing session")
//        sessionDao.clearSession()
//    }
//}






