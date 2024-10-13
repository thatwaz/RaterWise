package com.thatwaz.raterwise.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thatwaz.raterwise.data.local.dao.SessionDao
import com.thatwaz.raterwise.data.model.Session
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.data.repository.TimeTrackingRepository
import com.thatwaz.raterwise.ui.utils.TimerService
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
    private val repository: TimeTrackingRepository,
    private val sessionDao: SessionDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var isTaskStarted by mutableStateOf(false)
    var totalWorkTime by mutableStateOf(0L)
    var currentTaskTime by mutableStateOf(0L)

    private val _isClockedIn = MutableStateFlow(false)
    val isClockedIn: StateFlow<Boolean> = _isClockedIn

    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<TimeEntry>>>(emptyMap())
    val timeEntriesByDay: StateFlow<Map<String, List<TimeEntry>>> = _timeEntriesByDay

    private var currentEntryId: Int? = null // Track the ongoing entry


    var clockInTime: String? = null
    var taskStartTime: String? = null

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    private var isServiceBound = false

    init {
        Log.d("TimeCardViewModel", "Initializing ViewModel")
        restoreSessionState()
        updateTimeEntriesByDay()
    }


    // Restores the session state when the ViewModel is initialized
    fun restoreSessionState() {
        viewModelScope.launch {
            val session = sessionDao.getSession()
            session?.let {
                _isClockedIn.value = it.isClockedIn
                clockInTime = it.clockInTime
                taskStartTime = it.taskStartTime
                totalWorkTime = it.totalWorkTime
                Log.d("TimeCardViewModel", "Restored session: $session")
            } ?: Log.d("TimeCardViewModel", "No previous session found.")
        }
    }

    // Save the current session state to the database
    private fun saveSessionState() {
        viewModelScope.launch {
            val session = Session(
                clockInTime = clockInTime ?: "",
                isClockedIn = _isClockedIn.value,
                taskStartTime = taskStartTime,
                totalWorkTime = totalWorkTime
            )
            sessionDao.saveSession(session)
            Log.d("TimeCardViewModel", "Saved session: $session")
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun startWorkSession() {
        _isClockedIn.value = true
        clockInTime = getCurrentTimeFormatted()

        val newEntry = TimeEntry(
            startTime = clockInTime ?: "00:00 AM",
            endTime = "",
            duration = 0,
            date = getCurrentDateFormatted(),
            isSubmitted = false,
            expectedDuration = 0,
            isOverUnderAET = false
        )

        viewModelScope.launch {
            repository.insertTimeEntry(newEntry)
            updateTimeEntriesByDay()
        }

        saveSessionState()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private var currentDate: String = getCurrentDateFormatted() // Initialize with the current date


    @RequiresApi(Build.VERSION_CODES.O)
    fun endWorkSession() {
        _isClockedIn.value = false
        val clockOutTime = getCurrentTimeFormatted() // Get the current time as endTime

        val startTime = clockInTime ?: "00:00 AM" // Fallback value
        val date = currentDate ?: getCurrentDateFormatted()

        val timeEntry = TimeEntry(
            startTime = startTime,
            endTime = clockOutTime,
            duration = calculateDuration(startTime, clockOutTime),
            date = date,
            isSubmitted = false,
            expectedDuration = 0,
            isOverUnderAET = false
        )

        viewModelScope.launch {
            repository.insertTimeEntry(timeEntry) // Insert the entry with both start and end times
            updateTimeEntriesByDay() // Refresh the UI state
//            clearSessionState() // Clear session if necessary
        }

        saveSessionState() // Save the final session state
    }



//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startWorkSession() {
//        _isClockedIn.value = true
//        clockInTime = getCurrentTimeFormatted()
//
//        // Save session state and insert time entry without needing Context
//        saveSessionState()
//
//        val timeEntry = TimeEntry(
//            startTime = clockInTime ?: "00:00 AM",
//            endTime = "",  // No end time yet
//            date = getCurrentDateFormatted(),
//            duration = 0,
//            isSubmitted = false,
//            expectedDuration = 0,
//            isOverUnderAET = false
//        )
//
//        viewModelScope.launch {
//            repository.insertTimeEntry(timeEntry)
//            updateTimeEntriesByDay()
//        }
//    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    fun endWorkSession() {
//        _isClockedIn.value = false
//        val clockOutTime = getCurrentTimeFormatted()
//
//        val timeEntry = TimeEntry(
//            startTime = clockInTime ?: "00:00 AM",
//            endTime = clockOutTime,
//            date = getCurrentDateFormatted(),
//            duration = calculateDuration(clockInTime ?: "00:00 AM", clockOutTime),
//            isSubmitted = false,
//            expectedDuration = 0,
//            isOverUnderAET = false
//        )
//
//        viewModelScope.launch {
//            repository.insertTimeEntry(timeEntry)
//            updateTimeEntriesByDay()
//        }
//
//        totalWorkTime = 0L
//        saveSessionState()  // Save the final state
//    }


//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startWorkSession() {
//        _isClockedIn.value = true
//        clockInTime = getCurrentTimeFormatted()
//        saveSessionState()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun endWorkSession() {
//        _isClockedIn.value = false
//        totalWorkTime = 0L
//        saveSessionState()
//    }



    // Handle service connection logic
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? TimerService.TimerBinder
            val timerService = localBinder?.getService()

            if (timerService != null) {
                isServiceBound = true

                // Use callback to update elapsed time
                timerService.timerCallback = object : TimerService.TimerCallback {
                    override fun onTimeUpdate(elapsedTime: Long, clockIn: String) {
                        _elapsedTime.value = elapsedTime
                        clockInTime = clockIn
                        Log.d("TimeCardViewModel", "Elapsed Time: $elapsedTime, ClockIn: $clockIn")
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }
    }

    // Bind and unbind the service
    fun bindToService(context: Context) {
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindFromService(context: Context) {
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
            Log.d("TimeCardViewModel", "Service unbound")
        }
    }

    // Start the foreground service
    fun startForegroundService(context: Context) {
        val serviceIntent = Intent(context, TimerService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
        bindToService(context) // Bind after starting the service
    }

    // Stop the foreground service
    fun stopForegroundService(context: Context) {
        val serviceIntent = Intent(context, TimerService::class.java)
        context.stopService(serviceIntent)
        unbindFromService(context) // Unbind after stopping the service
    }





    @RequiresApi(Build.VERSION_CODES.O)
    fun startTask() {
        isTaskStarted = true
        taskStartTime = getCurrentTimeFormatted()
        saveSessionState() // Save the session with task state
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun completeTask() {
        isTaskStarted = false
        val taskEndTime = getCurrentTimeFormatted()

        val taskEntry = TimeEntry(
            startTime = taskStartTime ?: "00:00 AM",
            endTime = taskEndTime,
            date = getCurrentDateFormatted(),
            duration = calculateDuration(taskStartTime ?: "00:00 AM", taskEndTime),
            isSubmitted = false,
            expectedDuration = 0,
            isOverUnderAET = false
        )

        viewModelScope.launch {
            repository.insertTimeEntry(taskEntry)
            updateTimeEntriesByDay()
        }

        saveSessionState() // Save the session without interfering with work sessions
    }


//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startWorkSession(context: Context) {
//        _isClockedIn.value = true
//        clockInTime = getCurrentTimeFormatted()
//
//        viewModelScope.launch {
//            saveSessionState() // Save state after starting work session
//        }
//        startForegroundService(context)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun endWorkSession(context: Context) {
//        _isClockedIn.value = false
//        totalWorkTime = 0L
//
//        viewModelScope.launch {
//            saveSessionState() // Save state after ending work session
//        }
//        stopForegroundService(context)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startTask() {
//        isTaskStarted = true
//        taskStartTime = getCurrentTimeFormatted()
//
//        viewModelScope.launch {
//            saveSessionState()
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun completeTask() {
//        isTaskStarted = false
//        val taskEndTime = getCurrentTimeFormatted()
//
//        val taskEntry = TimeEntry(
//            startTime = taskStartTime ?: "00:00 AM",
//            endTime = taskEndTime,
//            date = getCurrentDateFormatted(),
//            duration = calculateDuration(taskStartTime ?: "00:00 AM", taskEndTime),
//            isSubmitted = false,
//            expectedDuration = 0,
//            isOverUnderAET = false
//        )
//
//        viewModelScope.launch {
//            repository.insertTimeEntry(taskEntry)
//            updateTimeEntriesByDay()
//            saveSessionState()
//        }
//    }

    fun updateTimeEntriesByDay() {
        viewModelScope.launch {
            repository.getAllTimeEntries().collect { entries ->
                _timeEntriesByDay.value = entries.groupBy { it.date }
                Log.d("TimeCardViewModel", "Updated time entries by day")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateDuration(start: String, end: String): Int {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        val startTime = LocalTime.parse(start, formatter)
        val endTime = LocalTime.parse(end, formatter)
        return java.time.Duration.between(startTime, endTime).toMinutes().toInt()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentTimeFormatted(): String {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return LocalTime.now().format(formatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDateFormatted(): String = LocalDate.now().toString()



    // Delete all entries from the repository
    fun deleteAllEntries() {
        viewModelScope.launch {
            repository.deleteAllTimeEntries()
            updateTimeEntriesByDay()
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


}


//@HiltViewModel
//class TimeCardViewModel @Inject constructor(
//    private val repository: TimeTrackingRepository,
//    private val savedStateHandle: SavedStateHandle // Use SavedStateHandle for automatic state saving
//) : ViewModel() {
//
//    var isTaskStarted by mutableStateOf(false)
//    var totalWorkTime by mutableStateOf(0L)
//    var currentTaskTime by mutableStateOf(0L)
//
//    private val _isClockedIn = MutableStateFlow(false)
//    val isClockedIn: StateFlow<Boolean> = _isClockedIn
//
//    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<TimeEntry>>>(emptyMap())
//    val timeEntriesByDay: StateFlow<Map<String, List<TimeEntry>>> = _timeEntriesByDay
//
//    private var clockInTime: String = ""
//    private var taskStartTime: String = ""
//    private var currentDate: String = ""
//
//    init {
//        Log.d("TimeCardViewModel", "Initializing ViewModel")
//        restoreSessionState()
//        updateTimeEntriesByDay()
//    }
//
//    // Restore session state when ViewModel is recreated
//    fun restoreSessionState() {
//        clockInTime = savedStateHandle["clockInTime"] ?: ""
//        currentDate = savedStateHandle["currentDate"] ?: ""
//        totalWorkTime = savedStateHandle["totalWorkTime"] ?: 0L
//        _isClockedIn.value = savedStateHandle["isClockedIn"] ?: false
//        isTaskStarted = savedStateHandle["isTaskStarted"] ?: false
//        taskStartTime = savedStateHandle["taskStartTime"] ?: ""
//        Log.d("TimeCardViewModel", "State restored: clockInTime=$clockInTime, currentDate=$currentDate, isClockedIn=${_isClockedIn.value}, isTaskStarted=$isTaskStarted")
//    }
//
//    // Save the current session state to SavedStateHandle
//    fun saveSessionState() {
//        Log.d("TimeCardViewModel", "Saving state: clockInTime=$clockInTime, currentDate=$currentDate, isClockedIn=${_isClockedIn.value}")
//        savedStateHandle["clockInTime"] = clockInTime
//        savedStateHandle["currentDate"] = currentDate
//        savedStateHandle["totalWorkTime"] = totalWorkTime
//        savedStateHandle["isClockedIn"] = _isClockedIn.value
//        savedStateHandle["isTaskStarted"] = isTaskStarted
//        savedStateHandle["taskStartTime"] = taskStartTime
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startWorkSession() {
//        _isClockedIn.value = true
//        clockInTime = getCurrentTimeFormatted()
//        currentDate = getCurrentDateFormatted()
//        Log.d("TimeCardViewModel", "Work session started: clockInTime=$clockInTime, currentDate=$currentDate")
//        saveSessionState()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun endWorkSession() {
//        _isClockedIn.value = false
//        val clockOutTime = getCurrentTimeFormatted()
//        val timeEntry = TimeEntry(
//            startTime = clockInTime,
//            endTime = clockOutTime,
//            date = currentDate,
//            duration = calculateDuration(clockInTime, clockOutTime),
//            isSubmitted = false,
//            expectedDuration = 0,
//            isOverUnderAET = false
//        )
//
//        viewModelScope.launch {
//            repository.insertTimeEntry(timeEntry)
//            updateTimeEntriesByDay()
//        }
//        totalWorkTime = 0L
//        Log.d("TimeCardViewModel", "Work session ended: clockOutTime=$clockOutTime")
//        saveSessionState()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startTask() {
//        isTaskStarted = true
//        taskStartTime = getCurrentTimeFormatted()
//        Log.d("TimeCardViewModel", "Task started: taskStartTime=$taskStartTime")
//        saveSessionState()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun completeTask() {
//        isTaskStarted = false
//        val taskEndTime = getCurrentTimeFormatted()
//        val taskDuration = calculateDuration(taskStartTime, taskEndTime)
//
//        val taskEntry = TimeEntry(
//            startTime = taskStartTime,
//            endTime = taskEndTime,
//            date = getCurrentDateFormatted(),
//            duration = taskDuration,
//            isSubmitted = false,
//            expectedDuration = 0,
//            isOverUnderAET = false
//        )
//
//        viewModelScope.launch {
//            repository.insertTimeEntry(taskEntry)
//            updateTimeEntriesByDay()
//        }
//        Log.d("TimeCardViewModel", "Task completed: taskEndTime=$taskEndTime, duration=$taskDuration")
//        saveSessionState()
//    }
//
//    // Foreground service control for tracking sessions
//    fun startForegroundService(context: Context) {
//        val serviceIntent = Intent(context, TimerService::class.java)
//        ContextCompat.startForegroundService(context, serviceIntent)
//    }
//
//    fun stopForegroundService(context: Context) {
//        val serviceIntent = Intent(context, TimerService::class.java)
//        context.stopService(serviceIntent)
//    }
//
//    // Periodic update of total work time
//    fun updateWorkTime() {
//        totalWorkTime += 1
//        saveSessionState()
//    }
//
//    // Update the entries by grouping them by date
//    fun updateTimeEntriesByDay() {
//        Log.d("TimeCardViewModel", "Updating time entries by day...")
//        viewModelScope.launch {
//            repository.getAllTimeEntries().collect { entries ->
//                val groupedEntries = entries.groupBy { it.date }
//                _timeEntriesByDay.value = groupedEntries
//                Log.d("TimeCardViewModel", "Time entries updated: $groupedEntries")
//            }
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun calculateDuration(start: String, end: String): Int {
//        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
//        val startTime = LocalTime.parse(start, formatter)
//        val endTime = LocalTime.parse(end, formatter)
//        return java.time.Duration.between(startTime, endTime).toMinutes().toInt()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentTimeFormatted(): String {
//        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
//        return LocalTime.now().format(formatter)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentDateFormatted(): String {
//        return LocalDate.now().toString()
//    }
//
//    // Delete all entries from the repository
//    fun deleteAllEntries() {
//        viewModelScope.launch {
//            repository.deleteAllTimeEntries()
//            updateTimeEntriesByDay()
//        }
//    }
//
//    // Toggle the submission state of a time entry
//    fun toggleTimeEntrySubmission(entry: TimeEntry) {
//        viewModelScope.launch {
//            val updatedEntry = entry.copy(isSubmitted = !entry.isSubmitted)
//            repository.updateTimeEntry(updatedEntry)
//            updateTimeEntriesByDay()
//        }
//    }
//}


//@HiltViewModel
//class TimeCardViewModel @Inject constructor(
//    private val repository: TimeTrackingRepository,
//    private val savedStateHandle: SavedStateHandle // Use SavedStateHandle for automatic state saving
//) : ViewModel() {
//
//    var isTaskStarted by mutableStateOf(false)
//    var totalWorkTime by mutableStateOf(0L)
//    var currentTaskTime by mutableStateOf(0L)
//
//    private val _isClockedIn = MutableStateFlow(false)
//    val isClockedIn: StateFlow<Boolean> = _isClockedIn
//
//    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<TimeEntry>>>(emptyMap())
//    val timeEntriesByDay: StateFlow<Map<String, List<TimeEntry>>> = _timeEntriesByDay
//
//
//    private var clockInTime: String = ""
//    private var taskStartTime: String = ""
//    private var currentDate: String = ""
//
//    init {
//        Log.d("TimeCardViewModel", "Initializing ViewModel")
//        restoreSessionState()
//        updateTimeEntriesByDay() // Ensure data is updated correctly after restoring session state
//    }
//    // Restore session state when ViewModel is recreated
//     fun restoreSessionState() {
//        clockInTime = savedStateHandle["clockInTime"] ?: ""
//        currentDate = savedStateHandle["currentDate"] ?: ""
//        totalWorkTime = savedStateHandle["totalWorkTime"] ?: 0L
//        _isClockedIn.value = savedStateHandle["isClockedIn"] ?: false
//        isTaskStarted = savedStateHandle["isTaskStarted"] ?: false
//        taskStartTime = savedStateHandle["taskStartTime"] ?: ""
//        Log.d("TimeCardViewModel", "State restored: clockInTime=$clockInTime, currentDate=$currentDate, isClockedIn=${_isClockedIn.value}, isTaskStarted=$isTaskStarted")
//    }
//
//
//    // Save the current session state to SavedStateHandlw
//    private fun saveSessionState() {
//        Log.d("TimeCardViewModel", "Saving state: clockInTime=$clockInTime, currentDate=$currentDate, isClockedIn=${_isClockedIn.value}")
//        savedStateHandle["clockInTime"] = clockInTime
//        savedStateHandle["currentDate"] = currentDate
//        savedStateHandle["totalWorkTime"] = totalWorkTime
//        savedStateHandle["isClockedIn"] = _isClockedIn.value
//        savedStateHandle["isTaskStarted"] = isTaskStarted
//        savedStateHandle["taskStartTime"] = taskStartTime
//    }
//
////    private fun restoreSessionState() {
////        Log.d("TimeCardViewModel", "Restoring session state...")
////        clockInTime = savedStateHandle["clockInTime"] ?: ""
////        currentDate = savedStateHandle["currentDate"] ?: ""
////        totalWorkTime = savedStateHandle["totalWorkTime"] ?: 0L
////        _isClockedIn.value = savedStateHandle["isClockedIn"] ?: false
////        isTaskStarted = savedStateHandle["isTaskStarted"] ?: false
////        taskStartTime = savedStateHandle["taskStartTime"] ?: ""
////        Log.d("TimeCardViewModel", "Restored state: clockInTime=$clockInTime, currentDate=$currentDate, isClockedIn=${_isClockedIn.value}")
////    }
//
//    fun updateTimeEntriesByDay() {
//        Log.d("TimeCardViewModel", "Updating time entries by day...")
//        viewModelScope.launch {
//            repository.getAllTimeEntries().collect { entries ->
//                val groupedEntries = entries.groupBy { it.date }
//                _timeEntriesByDay.value = groupedEntries
//                Log.d("TimeCardViewModel", "Time entries updated: $groupedEntries")
//            }
//        }
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startWorkSession() {
//        _isClockedIn.value = true
//        clockInTime = getCurrentTimeFormatted()
//        currentDate = getCurrentDateFormatted()
//        saveSessionState()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun endWorkSession() {
//        _isClockedIn.value = false
//        val clockOutTime = getCurrentTimeFormatted()
//        val timeEntry = TimeEntry(
//            startTime = clockInTime,
//            endTime = clockOutTime,
//            date = currentDate,
//            duration = calculateDuration(clockInTime, clockOutTime),
//            isSubmitted = false,
//            expectedDuration = 0,
//            isOverUnderAET = false
//        )
//
//        viewModelScope.launch {
//            repository.insertTimeEntry(timeEntry)
//            updateTimeEntriesByDay()
//        }
//        totalWorkTime = 0L
//        saveSessionState()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startTask() {
//        isTaskStarted = true
//        taskStartTime = getCurrentTimeFormatted()
//        saveSessionState()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun completeTask() {
//        isTaskStarted = false
//        val taskEndTime = getCurrentTimeFormatted()
//        val taskDuration = calculateDuration(taskStartTime, taskEndTime)
//
//        val taskEntry = TimeEntry(
//            startTime = taskStartTime,
//            endTime = taskEndTime,
//            date = getCurrentDateFormatted(),
//            duration = taskDuration,
//            isSubmitted = false,
//            expectedDuration = 0,
//            isOverUnderAET = false
//        )
//
//        viewModelScope.launch {
//            repository.insertTimeEntry(taskEntry)
//            updateTimeEntriesByDay()
//        }
//        saveSessionState()
//    }
//
//
//    fun startForegroundService(context: Context) {
//        val serviceIntent = Intent(context, TimerService::class.java)
//        ContextCompat.startForegroundService(context, serviceIntent)
//    }
//
//    fun stopForegroundService(context: Context) {
//        val serviceIntent = Intent(context, TimerService::class.java)
//        context.stopService(serviceIntent)
//    }
//    // Updating work time periodically
//    fun updateWorkTime() {
//        totalWorkTime += 1
//        saveSessionState()
//    }
//
////    // Update the entries by grouping them by date
////    fun updateTimeEntriesByDay() {
////        viewModelScope.launch {
////            repository.getAllTimeEntries().collect { entries ->
////                val groupedEntries = entries.groupBy { it.date }
////                _timeEntriesByDay.value = groupedEntries
////            }
////        }
////    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentTimeFormatted(): String {
//        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
//        return LocalTime.now().format(formatter)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentDateFormatted(): String {
//        return LocalDate.now().toString()
//    }
//
//
//
//
//    // Restore the session state from SavedStateHandle
////    private fun restoreSessionState() {
////        clockInTime = savedStateHandle["clockInTime"] ?: ""
////        currentDate = savedStateHandle["currentDate"] ?: ""
////        totalWorkTime = savedStateHandle["totalWorkTime"] ?: 0L
////        _isClockedIn.value = savedStateHandle["isClockedIn"] ?: false
////        isTaskStarted = savedStateHandle["isTaskStarted"] ?: false
////        taskStartTime = savedStateHandle["taskStartTime"] ?: ""
////    }
//
//    // Calculate duration between two times in minutes
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun calculateDuration(start: String, end: String): Int {
//        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
//        val startTime = LocalTime.parse(start, formatter)
//        val endTime = LocalTime.parse(end, formatter)
//        return java.time.Duration.between(startTime, endTime).toMinutes().toInt()
//    }
//
//    // Delete all entries from the repository
//    fun deleteAllEntries() {
//        viewModelScope.launch {
//            repository.deleteAllTimeEntries()
//            updateTimeEntriesByDay()
//        }
//    }
//
//    // Toggle the submission state of a time entry
//    fun toggleTimeEntrySubmission(entry: TimeEntry) {
//        viewModelScope.launch {
//            val updatedEntry = entry.copy(isSubmitted = !entry.isSubmitted)
//            repository.updateTimeEntry(updatedEntry)
//            updateTimeEntriesByDay()
//        }
//    }
//}





//@HiltViewModel
//class TimeCardViewModel @Inject constructor(
//    private val repository: TimeTrackingRepository
//) : ViewModel() {
//
//    var isTaskStarted by mutableStateOf(false) // Track whether a task is active
//    var totalWorkTime by mutableStateOf(0L)
//    var currentTaskTime by mutableStateOf(0L) // Track the time for the current task
//
//    private val _isClockedIn = MutableStateFlow(false)
//    val isClockedIn: StateFlow<Boolean> = _isClockedIn
//
//    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<TimeEntry>>>(emptyMap())
//    val timeEntriesByDay: StateFlow<Map<String, List<TimeEntry>>> = _timeEntriesByDay
//
//    private var clockInTime: String = "" // Track the clock-in time
//    private var currentDate: String = "" // Track the current date
//    private var taskStartTime: String = "" // Track task start time
//
//    init {
//        updateTimeEntriesByDay() // Load initial data
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startWorkSession() {
//        _isClockedIn.value = true
//        clockInTime = getCurrentTimeFormatted()
//        currentDate = getCurrentDateFormatted()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun endWorkSession() {
//        _isClockedIn.value = false
//        val clockOutTime = getCurrentTimeFormatted()
//
//        // Check if `clockInTime` is not empty to prevent unintended entries
//        if (clockInTime.isNotEmpty()) {
//            val timeEntry = TimeEntry(
//                startTime = clockInTime,
//                endTime = clockOutTime,
//                date = currentDate,
//                duration = calculateDuration(clockInTime, clockOutTime),
//                isSubmitted = false, // Set false by default
//                expectedDuration = 0, // Set default expected duration
//                isOverUnderAET = false // Default AET status
//            )
//
//            viewModelScope.launch {
//                repository.insertTimeEntry(timeEntry)
//                updateTimeEntriesByDay() // Refresh data after inserting a new entry
//            }
//        }
//
//        totalWorkTime = 0L // Reset work time
//    }
//
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startTask() {
//        if (isClockedIn.value) {
//            isTaskStarted = true
//            taskStartTime = getCurrentTimeFormatted() // Record the task start time
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun completeTask() {
//        if (isClockedIn.value && isTaskStarted) {
//            isTaskStarted = false
//            val taskEndTime = getCurrentTimeFormatted() // Get the task end time
//            val taskDuration = calculateDuration(taskStartTime, taskEndTime)
//
//            val taskEntry = TimeEntry(
//                startTime = taskStartTime,
//                endTime = taskEndTime,
//                date = currentDate,
//                duration = taskDuration,
//                isSubmitted = false, // Initially mark as not submitted
//                expectedDuration = 0,
//                isOverUnderAET = false
//            )
//
//            viewModelScope.launch {
//                repository.insertTimeEntry(taskEntry)
//                updateTimeEntriesByDay() // Refresh data to reflect the new task
//            }
//        }
//    }
//
//    fun updateWorkTime() {
//        totalWorkTime += 1
//    }
//
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentTimeFormatted(): String {
//        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
//        return LocalTime.now().format(formatter)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentDateFormatted(): String {
//        return LocalDate.now().toString()
//    }
//
//    // Calculate duration between two times in minutes
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun calculateDuration(start: String, end: String): Int {
//        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
//        val startTime = LocalTime.parse(start, formatter)
//        val endTime = LocalTime.parse(end, formatter)
//        return java.time.Duration.between(startTime, endTime).toMinutes().toInt()
//    }
//
//    fun submitTimeEntry(updatedEntry: TimeEntry) {
//        viewModelScope.launch {
//            // Update the entry with the new submission state
//            repository.updateTimeEntry(updatedEntry)
//            updateTimeEntriesByDay() // Refresh the entries list after updating
//        }
//    }
//
//
//
//    // Toggling submission state of a single entry
//    fun toggleTimeEntrySubmission(entry: TimeEntry) {
//        viewModelScope.launch {
//            val updatedEntry = entry.copy(isSubmitted = !entry.isSubmitted)
//            Log.d("TimeCardViewModel", "Toggling entry: $updatedEntry") // Debug log
//            repository.updateTimeEntry(updatedEntry) // Update the entry in the database
//            updateTimeEntriesByDay() // Refresh the list
//        }
//    }
//
//    // Load and group time entries by date
//    fun updateTimeEntriesByDay() {
//        viewModelScope.launch {
//            repository.getAllTimeEntries().collect { entries ->
//                _timeEntriesByDay.value = entries.groupBy { it.date }
//                Log.d("TimeCardViewModel", "Updated time entries by day: ${_timeEntriesByDay.value}")
//            }
//        }
//    }
//
//
//
//
//    fun deleteAllEntries() {
//        viewModelScope.launch {
//            repository.deleteAllTimeEntries()
//            updateTimeEntriesByDay() // Refresh the entries after deletion
//        }
//    }
//
//
//
//
//
//
//}
//
