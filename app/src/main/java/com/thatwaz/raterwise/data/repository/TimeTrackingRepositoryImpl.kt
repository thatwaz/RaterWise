package com.thatwaz.raterwise.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.thatwaz.raterwise.data.local.dao.TimeTrackingDao
import com.thatwaz.raterwise.data.model.DailyWorkSummary
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.data.model.WorkPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import javax.inject.Inject

class TimeTrackingRepositoryImpl @Inject constructor(
    private val dao: TimeTrackingDao
) : TimeTrackingRepository {

    // TimeEntry operations
    override fun getTimeEntriesByDate(date: String): Flow<List<TimeEntry>> {
        Log.d("TimeTrackingRepo", "Fetching time entries for date: $date")
        return dao.getTimeEntriesByDate(date)
    }

    override fun getAllTimeEntries(): Flow<List<TimeEntry>> = dao.getAllTimeEntries()



    override suspend fun insertTimeEntry(timeEntry: TimeEntry) {
        Log.d("TimeTrackingRepo", "Inserting TimeEntry: Start: ${timeEntry.startTime}, End: ${timeEntry.endTime}, Date: ${timeEntry.date}")
        dao.insertTimeEntry(timeEntry)
        Log.d("TimeTrackingRepo", "Inserted TimeEntry with ID: ${timeEntry.id}")
    }

    override suspend fun updateTimeEntry(timeEntry: TimeEntry) {
        dao.updateTimeEntry(timeEntry)
    }
    override suspend fun deleteAllTimeEntries() {
        dao.deleteAllTimeEntries()
    }

    // DailyWorkSummary operations
    override fun getDailySummary(date: String): Flow<DailyWorkSummary> {
        Log.d("TimeTrackingRepo", "Fetching daily summary for date: $date")
        return dao.getDailyWorkSummary(date)
    }

    override suspend fun insertDailyWorkSummary(summary: DailyWorkSummary) {
        Log.d("TimeTrackingRepo", "Inserting DailyWorkSummary for date: ${summary.date}")
        dao.insertDailyWorkSummary(summary)
        Log.d("TimeTrackingRepo", "Inserted DailyWorkSummary with ID: ${summary.id}")
    }

    // WorkPeriod operations
    override fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod> {
        Log.d("TimeTrackingRepo", "Fetching work period from $startDate to $endDate")
        return dao.getWorkPeriod(startDate, endDate)
    }

    override suspend fun insertWorkPeriod(workPeriod: WorkPeriod) {
        Log.d("TimeTrackingRepo", "Inserting WorkPeriod: Start: ${workPeriod.startDate}, End: ${workPeriod.endDate}")
        dao.insertWorkPeriod(workPeriod)
        Log.d("TimeTrackingRepo", "Inserted WorkPeriod with ID: ${workPeriod.id}")
    }

    // Retrieve current work period based on the current week (Monday - Sunday)
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod> {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1) // Calculate Monday of the current week
        val endOfWeek = startOfWeek.plusDays(6) // Calculate Sunday of the current week

        Log.d("TimeTrackingRepo", "Calculating current work period: $startOfWeek to $endOfWeek")

        // Return the work period as a Flow
        return flow {
            val currentWorkPeriod = dao.getWorkPeriod(startOfWeek.toString(), endOfWeek.toString()).first()
            Log.d("TimeTrackingRepo", "Current work period retrieved: $currentWorkPeriod")
            emit(currentWorkPeriod)
        }
    }

    override suspend fun submitTimeEntry(date: String, entry: TimeEntry) {
        val updatedEntry = entry.copy(isSubmitted = true)
        Log.d("TimeTrackingRepo", "Submitting TimeEntry for date: $date, ID: ${entry.id}")
        dao.updateTimeEntry(updatedEntry)
        Log.d("TimeTrackingRepo", "TimeEntry submitted: ID: ${updatedEntry.id}, Start: ${updatedEntry.startTime}, End: ${updatedEntry.endTime}")
    }
}

//class TimeTrackingRepositoryImpl @Inject constructor(
//    private val dao: TimeTrackingDao
//) : TimeTrackingRepository {
//
//    // TimeEntry operations
//    override fun getTimeEntriesByDate(date: String): Flow<List<TimeEntry>> = dao.getTimeEntriesByDate(date)
//
//    override suspend fun insertTimeEntry(timeEntry: TimeEntry) = dao.insertTimeEntry(timeEntry)
//
//    // DailyWorkSummary operations
//    override fun getDailySummary(date: String): Flow<DailyWorkSummary> = dao.getDailyWorkSummary(date)
//
//    override suspend fun insertDailyWorkSummary(summary: DailyWorkSummary) = dao.insertDailyWorkSummary(summary)
//
//    // WorkPeriod operations
//    override fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod> = dao.getWorkPeriod(startDate, endDate)
//
//    override suspend fun insertWorkPeriod(workPeriod: WorkPeriod) = dao.insertWorkPeriod(workPeriod)
//
//    // Retrieve current work period based on the current week (Monday - Sunday)
//    @RequiresApi(Build.VERSION_CODES.O)
//    override suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod> {
//        val today = LocalDate.now()
//        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1) // Calculate Monday of the current week
//        val endOfWeek = startOfWeek.plusDays(6) // Calculate Sunday of the current week
//
//        // Return the work period as a Flow
//        return flow {
//            emit(dao.getWorkPeriod(startOfWeek.toString(), endOfWeek.toString()).first())
//        }
//    }
//
//    override suspend fun submitTimeEntry(date: String, entry: TimeEntry) {
//        val updatedEntry = entry.copy(isSubmitted = true)
//        dao.updateTimeEntry(updatedEntry)
//    }
//
//
//}


