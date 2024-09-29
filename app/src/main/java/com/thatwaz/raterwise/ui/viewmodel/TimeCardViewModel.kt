package com.thatwaz.raterwise.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thatwaz.raterwise.data.model.DailyWorkSummary
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.data.model.WorkPeriod
import com.thatwaz.raterwise.data.repository.TimeTrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject


@HiltViewModel
class TimeCardViewModel @Inject constructor(
    private val repository: TimeTrackingRepository
) : ViewModel() {
    var isClockedIn by mutableStateOf(false)
    var isTaskStarted by mutableStateOf(false)
    var totalWorkTime by mutableStateOf(0L)
    var currentTaskTime by mutableStateOf(0L)

    private val _currentWorkPeriod = mutableStateOf<WorkPeriod?>(null)
    val currentWorkPeriod: State<WorkPeriod?> = _currentWorkPeriod

    init {
        loadCurrentWorkPeriod()
    }

    fun loadCurrentWorkPeriod() {
        viewModelScope.launch {
            repository.getCurrentWorkPeriod().collect { period ->
                _currentWorkPeriod.value = period
            }
        }
    }

    fun startWorkSession() {
        isClockedIn = true
    }

    fun endWorkSession() {
        isClockedIn = false
        totalWorkTime = 0L
    }

    fun updateWorkTime() {
        totalWorkTime++
    }

    fun startTask(maxTime: Long) {
        isTaskStarted = true
        currentTaskTime = 0L
    }

    fun completeTask(taskTime: Long) {
        isTaskStarted = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentTimeFormatted(): String {
        val current = LocalTime.now()
        return current.format(DateTimeFormatter.ofPattern("hh:mm a"))
    }



    private val _selectedDaySummary = mutableStateOf<DailyWorkSummary?>(null)
    val selectedDaySummary: State<DailyWorkSummary?> = _selectedDaySummary

    private val hourlyWage = 20.0 // Set an example hourly wage, should be configurable



    // Calculate earnings for the entire work period based on the hourly wage
    private fun calculateEarningsForPeriod(period: WorkPeriod): WorkPeriod {
        var totalEarnings = 0.0

        // Calculate daily earnings and aggregate them for the period
        val updatedDailySummaries = period.dailySummaries.map { dailySummary ->
            val dailyEarnings = dailySummary.timeWorked / 60.0 * hourlyWage
            totalEarnings += dailyEarnings

            dailySummary.copy(totalEarnings = dailyEarnings)
        }

        // Return an updated WorkPeriod with calculated earnings
        return period.copy(
            totalEarnings = totalEarnings,
            dailySummaries = updatedDailySummaries
        )
    }

    // Load a specific day's summary and calculate its earnings
    fun loadDailySummary(date: String) {
        viewModelScope.launch {
            repository.getDailySummary(date).collectLatest { dailySummary ->
                // Calculate the total earnings for the day
                val dailyEarnings = dailySummary.timeWorked / 60.0 * hourlyWage
                _selectedDaySummary.value = dailySummary.copy(totalEarnings = dailyEarnings)
            }
        }
    }

    // Submit a time entry and refresh the corresponding daily summary
    fun submitTimeEntry(date: String, entry: TimeEntry) {
        viewModelScope.launch {
            repository.submitTimeEntry(date, entry)
            loadDailySummary(date) // Refresh the data after submission
        }
    }
}
