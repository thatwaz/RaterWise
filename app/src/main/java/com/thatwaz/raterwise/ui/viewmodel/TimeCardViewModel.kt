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
import com.thatwaz.raterwise.data.model.Session
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.data.repository.TimeTrackingRepository
import com.thatwaz.raterwise.ui.utils.TimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject


@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class TimeCardViewModel @Inject constructor(
    private val repository: TimeTrackingRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Observable state variables
    var isTaskRunning by mutableStateOf(savedStateHandle["isTaskRunning"] ?: false)
        private set
    var taskSeconds by mutableStateOf(savedStateHandle["taskSeconds"] ?: 0L)
        private set
    var taskStartTime: String? = null
    var maxTaskTime by mutableStateOf(savedStateHandle["maxTaskTime"] ?: "")
        private set

    var totalWorkTime by mutableStateOf(0L)
    var clockInTime: String? = null

    private val _isClockedIn = MutableStateFlow(false)
    val isClockedIn: StateFlow<Boolean> = _isClockedIn

    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<TimeEntry>>>(emptyMap())
    val timeEntriesByDay: StateFlow<Map<String, List<TimeEntry>>> = _timeEntriesByDay

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    private var isServiceBound = false
    private var timerService: TimerService? = null

    private var taskTimerJob: Job? = null // Track the job for the task timer

    init {
//        restoreSessionState()
//        restoreTaskState()
        updateTimeEntriesByDay()
    }

//    fun restoreTaskState() {
//        Log.d("TimeCardViewModel", "Restoring task state...") // Log restoration
//
//        isTaskRunning = savedStateHandle["isTaskRunning"] ?: false
//        taskSeconds = savedStateHandle["taskSeconds"] ?: 0L
//        taskStartTime = savedStateHandle["taskStartTime"]
//
//        if (isTaskRunning) {
//            Log.d("TimeCardViewModel", "Task was running, resuming timer.") // Log if task is resumed
//            startTaskTimer() // Resume the task timer if a task was running
//        }
//    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun startTask(context: Context) {
        if (isTaskRunning) {
            Log.d("TimeCardViewModel", "Task is already running, skipping task start.")
            return // Prevent starting the task if it's already running
        }

        // Set the current time as the new task start time
        taskStartTime = getCurrentTimeFormatted()
        isTaskRunning = true
        taskSeconds = 0L // Reset the task seconds for the new task

        // Save the new task start time and task running state
        savedStateHandle["isTaskRunning"] = true
        savedStateHandle["taskStartTime"] = taskStartTime
        savedStateHandle["taskSeconds"] = 0L

        Log.d("TimeCardViewModel", "Task started at $taskStartTime, taskSeconds reset to 0.")

        // Save the session state to persist the task start time
        saveSessionState()

        // Start the foreground service and start the task timer
        startForegroundService(context)
        bindToService(context) {
            timerService?.startTaskTimer() // Resume the task timer in the service
        }
    }















    fun updateTaskSeconds(seconds: Long) {
        taskSeconds = seconds
        savedStateHandle["taskSeconds"] = seconds
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun completeTask(context: Context) {
        if (!isTaskRunning) return

        isTaskRunning = false // Mark task as completed
        val taskEndTime = getCurrentTimeFormatted()

        // Calculate task duration
        val taskDuration = (taskSeconds / 60).toInt()
        val expectedDuration = maxTaskTime.toIntOrNull() ?: 0
        val overUnderAET = repository.calculateOverUnderAET(taskDuration, expectedDuration)

        val taskEntry = TimeEntry(
            startTime = taskStartTime ?: "00:00 AM",
            endTime = taskEndTime,
            date = getCurrentDateFormatted(),
            duration = taskDuration,
            isSubmitted = false,
            expectedDuration = expectedDuration,
            isOverUnderAET = overUnderAET != 0,
            minutesOverUnderAET = overUnderAET
        )

        viewModelScope.launch {
            repository.insertTimeEntry(taskEntry)
            stopTask(context) // Stop the task and reset state

            // Save session state after completing the task
            Log.d("TimeCardViewModel", "Task completed and state saved: isTaskRunning = false.")
            saveSessionState()
        }

        // Reset the task timer
        taskSeconds = 0L
        savedStateHandle["taskSeconds"] = 0L
        savedStateHandle["taskStartTime"] = null

        Log.d("TimeCardViewModel", "Task completed, timer reset to 0.")
    }







    fun stopTask(context: Context) {
        if (!isTaskRunning) return

        isTaskRunning = false
        taskTimerJob?.cancel() // Cancel the running timer job
        taskTimerJob = null // Clear the job to avoid redundant tasks

        savedStateHandle["isTaskRunning"] = false
        savedStateHandle["taskSeconds"] = 0L // Reset task seconds when the task stops

        Log.d("TimeCardViewModel", "Task stopped and timer reset.")

        stopForegroundService(context) // Stop the foreground service
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun restoreSessionState(context: Context) {
        viewModelScope.launch {
            val session = repository.getSession() // Fetch saved session from the database
            session?.let {
                _isClockedIn.value = it.isClockedIn
                clockInTime = it.clockInTime
                taskStartTime = it.taskStartTime
                totalWorkTime = it.totalWorkTime
                isTaskRunning = it.isTaskRunning

                Log.d("TimeCardViewModel", "Restored state -> isTaskRunning: $isTaskRunning, taskStartTime: $taskStartTime")

                if (isTaskRunning) {
                    // Calculate the elapsed time since the task started
                    val elapsedTime = calculateElapsedTime(taskStartTime ?: "00:00 AM")
                    updateTaskSeconds(elapsedTime)

                    // Resume the timer in the service if the task is still running
                    bindToService(context) {
                        timerService?.startTaskTimer() // Resume task timer
                    }
                } else {
                    Log.d("TimeCardViewModel", "No running task, timer reset to 0.")
                    updateTaskSeconds(0L) // Reset the task timer in UI
                }
            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun saveSessionState() {
        viewModelScope.launch {
            val session = Session(
                clockInTime = clockInTime ?: "",
                isClockedIn = _isClockedIn.value,
                taskStartTime = taskStartTime,
                totalWorkTime = totalWorkTime,
                isTaskRunning = isTaskRunning // Ensure the correct state is saved
            )

            Log.d("TimeCardViewModel", "Saving session state: isTaskRunning = $isTaskRunning")

            repository.saveSession(session)

            Log.d("TimeCardViewModel", "Session saved successfully.")
        }
    }



    // Foreground Service Management
    fun bindToService(context: Context, onServiceConnected: (() -> Unit)? = null) {
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val localBinder = binder as? TimerService.TimerBinder
                timerService = localBinder?.getService()

                isServiceBound = true
                Log.d("TimeCardViewModel", "Service bound successfully")

                onServiceConnected?.invoke() // Perform any action after service is bound
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
                isServiceBound = false
            }
        }, Context.BIND_AUTO_CREATE)
    }


    fun unbindFromService(context: Context) {
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    fun startForegroundService(context: Context) {
        val serviceIntent = Intent(context, TimerService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
        bindToService(context) // Bind to the service after starting it
    }

    fun stopForegroundService(context: Context) {
        val serviceIntent = Intent(context, TimerService::class.java)
        context.stopService(serviceIntent)
        unbindFromService(context) // Unbind from the service after stopping it
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? TimerService.TimerBinder
            timerService = localBinder?.getService()

            if (timerService != null) {
                isServiceBound = true

                // Use callback to update elapsed and task time
                timerService?.timerCallback = object : TimerService.TimerCallback {
                    override fun onTimeUpdate(elapsedTime: Long, clockIn: String) {
                        // Update the elapsed time and log the clock in time
                        _elapsedTime.value = elapsedTime
                        Log.d("TimeCardViewModel", "Elapsed Time: $elapsedTime, ClockIn: $clockIn")

                        // If you want to update the clockInTime variable safely:
                        if (clockInTime == null) {
                            clockInTime = clockIn // Assign only if it's not set already
                        }
                    }

                    override fun onTaskTimeUpdate(taskSeconds: Long) {
                        updateTaskSeconds(taskSeconds) // Update task timer in ViewModel
                        Log.d("TimeCardViewModel", "Task Time: $taskSeconds seconds")
                    }
                }

                // Check if a task is running and continue the task timer
//                if (isTaskRunning) {
//                    timerService?.startTaskTimer() // Continue task timer if task is running
//                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }
    }





    // Work session-related functions
    @RequiresApi(Build.VERSION_CODES.O)
    fun startWorkSession(context: Context) {
        _isClockedIn.value = true
        clockInTime = getCurrentTimeFormatted()

        val newEntry = TimeEntry(
            startTime = clockInTime ?: "00:00 AM",
            endTime = "",
            duration = 0,
            date = getCurrentDateFormatted(),
            isSubmitted = false,
            expectedDuration = 0, // No expected duration at the start
            isOverUnderAET = false, // Not applicable at the start
            minutesOverUnderAET = 0 // Default value as no task has been completed yet
        )

        viewModelScope.launch {
            repository.insertTimeEntry(newEntry)
            updateTimeEntriesByDay()
            saveSessionState()
            startForegroundService(context) // Start the foreground service
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun endWorkSession(context: Context) {
        _isClockedIn.value = false
        val clockOutTime = getCurrentTimeFormatted()

        val duration = calculateDuration(clockInTime ?: "00:00 AM", clockOutTime)

        val timeEntry = TimeEntry(
            startTime = clockInTime ?: "00:00 AM",
            endTime = clockOutTime,
            duration = duration,
            date = getCurrentDateFormatted(),
            isSubmitted = false,
            expectedDuration = 0,
            isOverUnderAET = false,
            minutesOverUnderAET = 0 // Default value
        )

        viewModelScope.launch {
            repository.insertTimeEntry(timeEntry)
            updateTimeEntriesByDay()
            saveSessionState()
            stopForegroundService(context) // Stop the foreground service
        }
    }








    // Time entry-related functions
    fun updateTimeEntriesByDay() {
        viewModelScope.launch {
            repository.getAllTimeEntries().collect { entries ->
                _timeEntriesByDay.value = entries.groupBy { it.date }
            }
        }
    }
        fun updateMaxTaskTime(time: String) {
        maxTaskTime = time
        savedStateHandle["maxTaskTime"] = time
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateElapsedTime(startTime: String): Long {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        val start = LocalTime.parse(startTime, formatter)
        val current = LocalTime.now()
        return java.time.Duration.between(start, current).seconds
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentTimeFormatted(): String {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return LocalTime.now().format(formatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDateFormatted(): String = LocalDate.now().toString()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateDuration(start: String, end: String): Int {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        val startTime = LocalTime.parse(start, formatter)
        val endTime = LocalTime.parse(end, formatter)
        return java.time.Duration.between(startTime, endTime).toMinutes().toInt()
    }

    fun deleteAllEntries() {
        viewModelScope.launch {
            repository.deleteAllTimeEntries()
            updateTimeEntriesByDay()
        }
    }

    fun toggleTimeEntrySubmission(entry: TimeEntry) {
        viewModelScope.launch {
            val updatedEntry = entry.copy(isSubmitted = !entry.isSubmitted)
            repository.updateTimeEntry(updatedEntry)
            updateTimeEntriesByDay()
        }
    }
}




//@HiltViewModel
//class TimeCardViewModel @Inject constructor(
//    private val repository: TimeTrackingRepository,
//    private val sessionDao: SessionDao,
//    private val savedStateHandle: SavedStateHandle
//) : ViewModel() {
//
//    // Observable state variables
//    var isTaskRunning by mutableStateOf(savedStateHandle["isTaskRunning"] ?: false)
//        private set
//    var taskSeconds by mutableStateOf(savedStateHandle["taskSeconds"] ?: 0L)
//        private set
//    var maxTaskTime by mutableStateOf(savedStateHandle["maxTaskTime"] ?: "")
//        private set
//
//
//    var taskStartTime: String? = null
//    var totalWorkTime by mutableStateOf(0L)
//    var currentTaskTime by mutableStateOf(0L)
//    var clockInTime: String? = null
//    var isTaskStarted by mutableStateOf(false)
//        private set
//
//    private val _isClockedIn = MutableStateFlow(false)
//    val isClockedIn: StateFlow<Boolean> = _isClockedIn
//
//    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<TimeEntry>>>(emptyMap())
//    val timeEntriesByDay: StateFlow<Map<String, List<TimeEntry>>> = _timeEntriesByDay
//
//    private val _elapsedTime = MutableStateFlow(0L)
//    val elapsedTime: StateFlow<Long> = _elapsedTime
//
//    private var currentEntryId: Int? = null
//    private var selectedTaskDuration: Int = 0
//    private var isServiceBound = false
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private var currentDate: String = getCurrentDateFormatted()
//
//    init {
//        restoreSessionState()
//        restoreTaskState()
//        updateTimeEntriesByDay()
//    }
//
//    // Task-related functions
//    fun restoreTaskState() {
//        isTaskRunning = savedStateHandle["isTaskRunning"] ?: false
//        taskSeconds = savedStateHandle["taskSeconds"] ?: 0L
//        taskStartTime = savedStateHandle["taskStartTime"] ?: null
//
//        if (isTaskRunning) {
//            startTaskTimer() // Continue the task timer when the app is reopened
//        }
//    }
//
//    fun startTaskTimer() {
//        viewModelScope.launch {
//            while (isTaskRunning) {
//                delay(1000L) // Delay for 1 second
//                updateTaskSeconds(taskSeconds + 1) // Increment taskSeconds
//            }
//        }
//    }
//
//
//    fun updateTaskSeconds(seconds: Long) {
//        taskSeconds = seconds
//        savedStateHandle["taskSeconds"] = seconds
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startTask() {
//        isTaskRunning = true
//        taskStartTime = getCurrentTimeFormatted()
//        savedStateHandle["isTaskRunning"] = true
//        savedStateHandle["taskStartTime"] = taskStartTime
//
//        // Start task timer in background
//        viewModelScope.launch {
//            while (isTaskRunning) {
//                delay(1000L) // 1-second delay
//                updateTaskSeconds(taskSeconds + 1) // Increment taskSeconds every second
//            }
//        }
//    }
//
//
//    fun stopTask() {
//        isTaskRunning = false
//        savedStateHandle["isTaskRunning"] = false
//    }
//
//    fun updateMaxTaskTime(time: String) {
//        maxTaskTime = time
//        savedStateHandle["maxTaskTime"] = time
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun completeTask() {
//        if (!isTaskRunning) return
//
//        isTaskRunning = false
//        val taskEndTime = getCurrentTimeFormatted()
//
//        val taskEntry = TimeEntry(
//            startTime = taskStartTime ?: "00:00 AM",
//            endTime = taskEndTime,
//            date = getCurrentDateFormatted(),
//            duration = taskSeconds.toInt() / 60,
//            isSubmitted = false,
//            expectedDuration = 0,
//            isOverUnderAET = false
//        )
//
//        viewModelScope.launch {
//            repository.insertTimeEntry(taskEntry)
//            updateTimeEntriesByDay()
//        }
//
//        taskSeconds = 0L
//        savedStateHandle["taskSeconds"] = 0L
//        savedStateHandle["taskStartTime"] = null
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startTaskWithDuration(minute: Int) {
//        selectedTaskDuration = minute
//        isTaskStarted = true
//        taskStartTime = getCurrentTimeFormatted()
//
//        viewModelScope.launch {
//            saveSessionState()
//        }
//
//        Log.d("TimeCardViewModel", "Task started with duration $minute minutes")
//    }
//
//    // Work session-related functions
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startWorkSession() {
//        _isClockedIn.value = true
//        clockInTime = getCurrentTimeFormatted()
//
//        val newEntry = TimeEntry(
//            startTime = clockInTime ?: "00:00 AM",
//            endTime = "",
//            duration = 0,
//            date = getCurrentDateFormatted(),
//            isSubmitted = false,
//            expectedDuration = 0,
//            isOverUnderAET = false
//        )
//
//        viewModelScope.launch {
//            repository.insertTimeEntry(newEntry)
//            updateTimeEntriesByDay()
//        }
//
//        saveSessionState()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun endWorkSession() {
//        _isClockedIn.value = false
//        val clockOutTime = getCurrentTimeFormatted()
//
//        val timeEntry = TimeEntry(
//            startTime = clockInTime ?: "00:00 AM",
//            endTime = clockOutTime,
//            duration = calculateDuration(clockInTime ?: "00:00 AM", clockOutTime),
//            date = currentDate,
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
//        saveSessionState()
//    }
//
//    // Helper functions for time and date formatting
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentTimeFormatted(): String {
//        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
//        return LocalTime.now().format(formatter)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentDateFormatted(): String = LocalDate.now().toString()
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun calculateDuration(start: String, end: String): Int {
//        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
//        val startTime = LocalTime.parse(start, formatter)
//        val endTime = LocalTime.parse(end, formatter)
//        return java.time.Duration.between(startTime, endTime).toMinutes().toInt()
//    }
//
//    // Session state-related functions
//    fun restoreSessionState() {
//        viewModelScope.launch {
//            val session = sessionDao.getSession()
//            session?.let {
//                _isClockedIn.value = it.isClockedIn
//                clockInTime = it.clockInTime
//                taskStartTime = it.taskStartTime
//                totalWorkTime = it.totalWorkTime
//                Log.d("TimeCardViewModel", "Restored session: $session")
//            } ?: Log.d("TimeCardViewModel", "No previous session found.")
//        }
//    }
//
//    private fun saveSessionState() {
//        viewModelScope.launch {
//            val session = Session(
//                clockInTime = clockInTime ?: "",
//                isClockedIn = _isClockedIn.value,
//                taskStartTime = taskStartTime,
//                totalWorkTime = totalWorkTime
//            )
//            sessionDao.saveSession(session)
//            Log.d("TimeCardViewModel", "Saved session: $session")
//        }
//    }
//
//    // Time entry-related functions
//    fun updateTimeEntriesByDay() {
//        viewModelScope.launch {
//            repository.getAllTimeEntries().collect { entries ->
//                _timeEntriesByDay.value = entries.groupBy { it.date }
//                Log.d("TimeCardViewModel", "Updated time entries by day")
//            }
//        }
//    }
//
//    fun deleteAllEntries() {
//        viewModelScope.launch {
//            repository.deleteAllTimeEntries()
//            updateTimeEntriesByDay()
//        }
//    }
//
//    fun toggleTimeEntrySubmission(entry: TimeEntry) {
//        viewModelScope.launch {
//            val updatedEntry = entry.copy(isSubmitted = !entry.isSubmitted)
//            Log.d("TimeCardViewModel", "Toggling entry: $updatedEntry")
//            repository.updateTimeEntry(updatedEntry)
//            updateTimeEntriesByDay()
//        }
//    }
//
//    // Bind to the TimerService
//    fun bindToService(context: Context) {
//        val intent = Intent(context, TimerService::class.java)
//        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
//    }
//
//    // Unbind when the service is not needed
//    fun unbindFromService(context: Context) {
//        if (isServiceBound) {
//            context.unbindService(serviceConnection)
//            isServiceBound = false
//            Log.d("TimeCardViewModel", "Service unbound")
//        }
//    }
//
//    // Start the foreground service when the task begins
//    fun startForegroundService(context: Context) {
//        val serviceIntent = Intent(context, TimerService::class.java)
//        ContextCompat.startForegroundService(context, serviceIntent)
//        bindToService(context) // Bind to the service after starting it
//    }
//
//    // Stop the foreground service when the task ends
//    fun stopForegroundService(context: Context) {
//        val serviceIntent = Intent(context, TimerService::class.java)
//        context.stopService(serviceIntent)
//        unbindFromService(context) // Unbind from the service after stopping it
//    }
//
//    // Handle service connection logic
//    private val serviceConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
//            val localBinder = binder as? TimerService.TimerBinder
//            val timerService = localBinder?.getService()
//
//            if (timerService != null) {
//                isServiceBound = true
//
//                // Use callback to update elapsed and task time
//                timerService.timerCallback = object : TimerService.TimerCallback {
//                    override fun onTimeUpdate(elapsedTime: Long, clockIn: String) {
//                        _elapsedTime.value = elapsedTime
//                        clockInTime = clockIn
//                        Log.d("TimeCardViewModel", "Elapsed Time: $elapsedTime, ClockIn: $clockIn")
//                    }
//
//                    override fun onTaskTimeUpdate(taskSeconds: Long) {
//                        updateTaskSeconds(taskSeconds) // Update task timer in ViewModel
//                        Log.d("TimeCardViewModel", "Task Time: $taskSeconds seconds")
//                    }
//                }
//
//                // Check if a task is running and continue the task timer
//                if (isTaskRunning) {
//                    timerService.startTaskTimer() // Continue task timer if task is running
//                }
//            }
//        }
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            isServiceBound = false
//        }
//    }
//
//}






