package com.thatwaz.raterwise.data.repository

import com.thatwaz.raterwise.data.model.DailyWorkSummary
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.data.model.WorkPeriod
import kotlinx.coroutines.flow.Flow

// File: repository/TimeTrackingRepository.kt
interface TimeTrackingRepository {

    // TimeEntry operations
    fun getTimeEntriesByDate(date: String): Flow<List<TimeEntry>>
    suspend fun insertTimeEntry(timeEntry: TimeEntry)
    suspend fun submitTimeEntry(date: String, entry: TimeEntry)

    // DailyWorkSummary operations
    fun getDailySummary(date: String): Flow<DailyWorkSummary> // Change return type to Flow
    suspend fun insertDailyWorkSummary(summary: DailyWorkSummary)

    // WorkPeriod operations
    fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod>
    suspend fun insertWorkPeriod(workPeriod: WorkPeriod)

    suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod> // Change return type to Flow
}



