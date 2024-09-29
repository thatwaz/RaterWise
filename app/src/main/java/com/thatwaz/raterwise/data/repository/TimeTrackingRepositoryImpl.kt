package com.thatwaz.raterwise.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.thatwaz.raterwise.data.local.dao.TimeTrackingDao
import com.thatwaz.raterwise.data.model.DailyWorkSummary
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.data.model.WorkPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

import kotlinx.coroutines.flow.flow


class TimeTrackingRepositoryImpl @Inject constructor(
    private val dao: TimeTrackingDao
) : TimeTrackingRepository {

    // TimeEntry operations
    override fun getTimeEntriesByDate(date: String): Flow<List<TimeEntry>> = dao.getTimeEntriesByDate(date)

    override suspend fun insertTimeEntry(timeEntry: TimeEntry) = dao.insertTimeEntry(timeEntry)

    // DailyWorkSummary operations
    override fun getDailySummary(date: String): Flow<DailyWorkSummary> = dao.getDailyWorkSummary(date)

    override suspend fun insertDailyWorkSummary(summary: DailyWorkSummary) = dao.insertDailyWorkSummary(summary)

    // WorkPeriod operations
    override fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod> = dao.getWorkPeriod(startDate, endDate)

    override suspend fun insertWorkPeriod(workPeriod: WorkPeriod) = dao.insertWorkPeriod(workPeriod)

    // Retrieve current work period based on the current week (Monday - Sunday)
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod> {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1) // Calculate Monday of the current week
        val endOfWeek = startOfWeek.plusDays(6) // Calculate Sunday of the current week

        // Return the work period as a Flow
        return flow {
            emit(dao.getWorkPeriod(startOfWeek.toString(), endOfWeek.toString()).first())
        }
    }

    override suspend fun submitTimeEntry(date: String, entry: TimeEntry) {
        val updatedEntry = entry.copy(isSubmitted = true)
        dao.updateTimeEntry(updatedEntry)
    }
}


