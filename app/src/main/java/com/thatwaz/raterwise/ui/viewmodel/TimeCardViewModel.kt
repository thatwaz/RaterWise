package com.thatwaz.raterwise.ui.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.data.repository.TimeTrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TimeCardViewModel @Inject constructor(
    private val repository: TimeTrackingRepository
) : ViewModel() {

    var isTaskStarted by mutableStateOf(false) // Track whether a task is active
    var totalWorkTime by mutableStateOf(0L)
    var currentTaskTime by mutableStateOf(0L) // Track the time for the current task

    private val _isClockedIn = MutableStateFlow(false)
    val isClockedIn: StateFlow<Boolean> = _isClockedIn

    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<TimeEntry>>>(emptyMap())
    val timeEntriesByDay: StateFlow<Map<String, List<TimeEntry>>> = _timeEntriesByDay

    private var clockInTime: String = "" // Track the clock-in time
    private var currentDate: String = "" // Track the current date
    private var taskStartTime: String = "" // Track task start time

    init {
        updateTimeEntriesByDay() // Load initial data
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startWorkSession() {
        _isClockedIn.value = true
        clockInTime = getCurrentTimeFormatted()
        currentDate = getCurrentDateFormatted()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun endWorkSession() {
        _isClockedIn.value = false
        val clockOutTime = getCurrentTimeFormatted()

        // Check if `clockInTime` is not empty to prevent unintended entries
        if (clockInTime.isNotEmpty()) {
            val timeEntry = TimeEntry(
                startTime = clockInTime,
                endTime = clockOutTime,
                date = currentDate,
                duration = calculateDuration(clockInTime, clockOutTime),
                isSubmitted = false, // Set false by default
                expectedDuration = 0, // Set default expected duration
                isOverUnderAET = false // Default AET status
            )

            viewModelScope.launch {
                repository.insertTimeEntry(timeEntry)
                updateTimeEntriesByDay() // Refresh data after inserting a new entry
            }
        }

        totalWorkTime = 0L // Reset work time
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun startTask() {
        if (isClockedIn.value) {
            isTaskStarted = true
            taskStartTime = getCurrentTimeFormatted() // Record the task start time
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun completeTask() {
        if (isClockedIn.value && isTaskStarted) {
            isTaskStarted = false
            val taskEndTime = getCurrentTimeFormatted() // Get the task end time
            val taskDuration = calculateDuration(taskStartTime, taskEndTime)

            val taskEntry = TimeEntry(
                startTime = taskStartTime,
                endTime = taskEndTime,
                date = currentDate,
                duration = taskDuration,
                isSubmitted = false, // Initially mark as not submitted
                expectedDuration = 0,
                isOverUnderAET = false
            )

            viewModelScope.launch {
                repository.insertTimeEntry(taskEntry)
                updateTimeEntriesByDay() // Refresh data to reflect the new task
            }
        }
    }

    fun updateWorkTime() {
        totalWorkTime += 1
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentTimeFormatted(): String {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return LocalTime.now().format(formatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDateFormatted(): String {
        return LocalDate.now().toString()
    }

    // Calculate duration between two times in minutes
    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateDuration(start: String, end: String): Int {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        val startTime = LocalTime.parse(start, formatter)
        val endTime = LocalTime.parse(end, formatter)
        return java.time.Duration.between(startTime, endTime).toMinutes().toInt()
    }

    fun submitTimeEntry(updatedEntry: TimeEntry) {
        viewModelScope.launch {
            // Update the entry with the new submission state
            repository.updateTimeEntry(updatedEntry)
            updateTimeEntriesByDay() // Refresh the entries list after updating
        }
    }



    // Toggling submission state of a single entry
    fun toggleTimeEntrySubmission(entry: TimeEntry) {
        viewModelScope.launch {
            val updatedEntry = entry.copy(isSubmitted = !entry.isSubmitted)
            Log.d("TimeCardViewModel", "Toggling entry: $updatedEntry") // Debug log
            repository.updateTimeEntry(updatedEntry) // Update the entry in the database
            updateTimeEntriesByDay() // Refresh the list
        }
    }

    // Load and group time entries by date
    fun updateTimeEntriesByDay() {
        viewModelScope.launch {
            repository.getAllTimeEntries().collect { entries ->
                _timeEntriesByDay.value = entries.groupBy { it.date }
                Log.d("TimeCardViewModel", "Updated time entries by day: ${_timeEntriesByDay.value}")
            }
        }
    }




    fun deleteAllEntries() {
        viewModelScope.launch {
            repository.deleteAllTimeEntries()
            updateTimeEntriesByDay() // Refresh the entries after deletion
        }
    }






}

