package com.thatwaz.raterwise.ui.viewmodel

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thatwaz.raterwise.data.model.Session
import com.thatwaz.raterwise.data.model.TaskTimeEntry
import com.thatwaz.raterwise.data.repository.TimeTrackingRepository
import com.thatwaz.raterwise.ui.utils.getCurrentTimeFormatted
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
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

    private val formatterWithoutSeconds = DateTimeFormatter.ofPattern("hh:mm a")

    // Observable state variables
    var isTaskRunning by mutableStateOf(savedStateHandle["isTaskRunning"] ?: false)
        private set
    var taskSeconds by mutableStateOf(savedStateHandle["taskSeconds"] ?: 0L)
        private set

    var taskStartTime: String? = savedStateHandle["taskStartTime"]
    var maxTaskTime by mutableStateOf(savedStateHandle["maxTaskTime"] ?: "")

    var totalWorkTime by mutableStateOf(savedStateHandle["totalWorkTime"] ?: 0L)
    var clockInTime: String? = savedStateHandle["clockInTime"]

    private val formatterWithSeconds = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val _isClockedIn = MutableStateFlow(savedStateHandle["isClockedIn"] ?: false)
    val isClockedIn: StateFlow<Boolean> = _isClockedIn

    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<Session>>>(emptyMap())
    val timeEntriesByDay: StateFlow<Map<String, List<Session>>> = _timeEntriesByDay





    init {
        viewModelScope.launch {
            restoreSessionState()
            updateTimeEntriesByDay()
            restoreTaskTimerState()
            taskStartTime = savedStateHandle["taskStartTime"]
        }
    }


    private fun updateStateHandle() {
        savedStateHandle["isTaskRunning"] = isTaskRunning
        savedStateHandle["taskSeconds"] = taskSeconds
        savedStateHandle["taskStartTime"] = taskStartTime
        savedStateHandle["clockInTime"] = clockInTime
        savedStateHandle["totalWorkTime"] = totalWorkTime
        savedStateHandle["isClockedIn"] = _isClockedIn.value

        Log.d("TimeCardViewModel", "Updated SavedStateHandle - isTaskRunning: $isTaskRunning, taskSeconds: $taskSeconds, taskStartTime: $taskStartTime, clockInTime: $clockInTime, totalWorkTime: $totalWorkTime, isClockedIn: ${_isClockedIn.value}")
    }

//
//    private fun restoreSessionState() {
//        Log.d("TimeCardViewModel", "Restoring session state from SavedStateHandle...")
//
//        // Attempt to retrieve all state variables from SavedStateHandle
//        isTaskRunning = savedStateHandle["isTaskRunning"] ?: false
//        taskSeconds = savedStateHandle["taskSeconds"] ?: 0L
//        taskStartTime = savedStateHandle["taskStartTime"]
//        clockInTime = savedStateHandle["clockInTime"]
//        totalWorkTime = savedStateHandle["totalWorkTime"] ?: 0L
//        _isClockedIn.value = savedStateHandle["isClockedIn"] ?: false
//
//        Log.d("TimeCardViewModel", "Restored from SavedStateHandle - isTaskRunning: $isTaskRunning, taskSeconds: $taskSeconds, taskStartTime: $taskStartTime, clockInTime: $clockInTime, totalWorkTime: $totalWorkTime, isClockedIn: ${_isClockedIn.value}")
//
//        // Check if we need to fetch from the database
//        viewModelScope.launch {
//            if (clockInTime == null || !_isClockedIn.value) {
//                val activeSession = repository.getActiveSession()
//                activeSession?.let {
//                    _isClockedIn.value = it.isClockedIn
//                    clockInTime = it.clockInTime
//                    totalWorkTime = it.totalWorkTime
//                    updateStateHandle() // Ensure values are stored back to SavedStateHandle
//                    Log.d("TimeCardViewModel", "Restored active session from database - isClockedIn: ${it.isClockedIn}, clockInTime: ${it.clockInTime}, totalWorkTime: ${it.totalWorkTime}")
//                }
//            }
//
//            // Restore active task if no task is currently running
//            if (!isTaskRunning) {
//                val activeTask = repository.getActiveTask()
//                activeTask?.let {
//                    isTaskRunning = it.isTaskRunning
//                    taskStartTime = it.startTime
//                    taskSeconds = it.duration.toLong()
//                    updateStateHandle() // Save the active task state to SavedStateHandle
//                    Log.d("TimeCardViewModel", "Restored active task from database - startTime: $taskStartTime, duration: $taskSeconds, isTaskRunning: $isTaskRunning")
//                }
//            }
//        }
//    }
private fun restoreSessionState() {
    // Log initial restore attempt
    Log.d("TimeCardViewModel", "Restoring session state from SavedStateHandle...")

    // Retrieve initial values from SavedStateHandle
    isTaskRunning = savedStateHandle["isTaskRunning"] ?: false
    taskSeconds = savedStateHandle["taskSeconds"] ?: 0L
    taskStartTime = savedStateHandle["taskStartTime"]
    clockInTime = savedStateHandle["clockInTime"]
    totalWorkTime = savedStateHandle["totalWorkTime"] ?: 0L
    _isClockedIn.value = savedStateHandle["isClockedIn"] ?: false

    // Log the values retrieved from SavedStateHandle
    Log.d("TimeCardViewModel", "SavedStateHandle - isTaskRunning: $isTaskRunning, taskSeconds: $taskSeconds, taskStartTime: $taskStartTime, clockInTime: $clockInTime, totalWorkTime: $totalWorkTime, isClockedIn: ${_isClockedIn.value}")

    viewModelScope.launch {
        if (clockInTime == null || !_isClockedIn.value) {
            // Retrieve active session from database if not stored in SavedStateHandle
            val activeSession = repository.getActiveSession()
            activeSession?.let {
                _isClockedIn.value = it.isClockedIn
                clockInTime = it.clockInTime
                totalWorkTime = it.totalWorkTime
                updateStateHandle() // Save restored session to SavedStateHandle

                // Log that we restored the session from the database
                Log.d("TimeCardViewModel", "Restored active session from database - isClockedIn: ${it.isClockedIn}, clockInTime: ${it.clockInTime}, totalWorkTime: ${it.totalWorkTime}")
            }
        }

        // Check for an active task as well
        val activeTask = repository.getActiveTask()
        if (activeTask != null) {
            isTaskRunning = true
            taskStartTime = activeTask.startTime

            // Parse taskStartTime and calculate the elapsed time
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            val startTime = LocalTime.parse(taskStartTime, formatter)
            val now = LocalTime.now()
            val elapsed = Duration.between(startTime, now).seconds

            // Log how many seconds have passed since the task was started
            Log.d("TimeCardViewModel", "Seconds since task started: $elapsed seconds (from $taskStartTime to $now)")

            // Update taskSeconds based on elapsed time
            taskSeconds = elapsed + activeTask.duration
            updateStateHandle() // Save restored task state to SavedStateHandle

            // Log restored task details with calculated elapsed time
            Log.d("TimeCardViewModel", "Restored active task from database - isTaskRunning: $isTaskRunning, taskStartTime: $taskStartTime, calculated taskSeconds: $taskSeconds")
        } else {
            Log.d("TimeCardViewModel", "No active task found in database.")
        }
    }
}


//    private fun restoreSessionState() {
//        // Log initial restore attempt
//        Log.d("TimeCardViewModel", "Restoring session state from SavedStateHandle...")
//
//        // Retrieve initial values from SavedStateHandle
//        isTaskRunning = savedStateHandle["isTaskRunning"] ?: false
//        taskSeconds = savedStateHandle["taskSeconds"] ?: 0L
//        taskStartTime = savedStateHandle["taskStartTime"]
//        clockInTime = savedStateHandle["clockInTime"]
//        totalWorkTime = savedStateHandle["totalWorkTime"] ?: 0L
//        _isClockedIn.value = savedStateHandle["isClockedIn"] ?: false
//
//        // Log the values retrieved from SavedStateHandle
//        Log.d("TimeCardViewModel", "SavedStateHandle - isTaskRunning: $isTaskRunning, taskSeconds: $taskSeconds, taskStartTime: $taskStartTime, clockInTime: $clockInTime, totalWorkTime: $totalWorkTime, isClockedIn: ${_isClockedIn.value}")
//
//        viewModelScope.launch {
//            if (clockInTime == null || !_isClockedIn.value) {
//                // Retrieve active session from database if not stored in SavedStateHandle
//                val activeSession = repository.getActiveSession()
//                activeSession?.let {
//                    _isClockedIn.value = it.isClockedIn
//                    clockInTime = it.clockInTime
//                    totalWorkTime = it.totalWorkTime
//                    updateStateHandle() // Save restored session to SavedStateHandle
//
//                    // Log that we restored the session from the database
//                    Log.d("TimeCardViewModel", "Restored active session from database - isClockedIn: ${it.isClockedIn}, clockInTime: ${it.clockInTime}, totalWorkTime: ${it.totalWorkTime}")
//                }
//            }
//
//            // Check for an active task as well
//            val activeTask = repository.getActiveTask()
//            if (activeTask != null) {
//                // Restore task state if an active task is found
//                isTaskRunning = true
//                taskSeconds = activeTask.duration.toLong()
//                taskStartTime = activeTask.startTime
//                updateStateHandle() // Save restored task state to SavedStateHandle
//
//                // Log restored task details
//                Log.d("TimeCardViewModel", "Restored active task from database - isTaskRunning: $isTaskRunning, taskSeconds: $taskSeconds, taskStartTime: $taskStartTime")
//            } else {
//                Log.d("TimeCardViewModel", "No active task found in database.")
//            }
//        }
//    }








    // Function to start the work session (clock-in)
    @RequiresApi(Build.VERSION_CODES.O)
    fun startWorkSession(context: Context) {
        _isClockedIn.value = true
        clockInTime = getCurrentTimeFormattedWithoutSeconds()
        updateStateHandle()
        savedStateHandle["clockInTime"] = clockInTime
        savedStateHandle["isClockedIn"] = true

        viewModelScope.launch {
            val session = Session(
                date = LocalDate.now().toString(),
                clockInTime = clockInTime ?: "00:00 AM",
                isClockedIn = true,
                totalWorkTime = 0L,
                isSubmitted = false
            )
            repository.saveSession(session)
            Log.d("TimeCardViewModel", "Started work session at $clockInTime")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun endWorkSession(context: Context) {
        if (!_isClockedIn.value) return

        _isClockedIn.value = false
        savedStateHandle["isClockedIn"] = false
        val clockOutTime = getCurrentTimeFormattedWithoutSeconds()
        val sessionDurationSeconds = calculateSessionDuration(clockInTime ?: "00:00 AM", clockOutTime)
        totalWorkTime = sessionDurationSeconds
        updateStateHandle()

        viewModelScope.launch {
            val activeSession = repository.getActiveSession()?.copy(
                isClockedIn = false,
                clockOutTime = clockOutTime,
                totalWorkTime = sessionDurationSeconds
            )

            activeSession?.let {
                repository.saveSession(it)
                Log.d("TimeCardViewModel", "Ended work session at $clockOutTime with duration ${formatDuration(sessionDurationSeconds)}")
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateSessionDuration(startTime: String, endTime: String): Long {
        val start = LocalTime.parse(startTime, formatterWithoutSeconds)
        val end = LocalTime.parse(endTime, formatterWithoutSeconds)
        return java.time.Duration.between(start, end).seconds
    }


    fun updateMaxTaskTime(time: String) {
        maxTaskTime = time
        savedStateHandle["maxTaskTime"] = time
    }

    // Update taskSeconds and save it to savedStateHandle for persistence
    fun updateTaskSeconds(newSeconds: Long) {
        taskSeconds = newSeconds
        savedStateHandle["taskSeconds"] = newSeconds
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startTask(context: Context) {
        if (!_isClockedIn.value) {
            Log.d("TimeCardViewModel", "Cannot start a task when not clocked in.")
            return
        }

        viewModelScope.launch {
            val activeSession = repository.getActiveSession()
            if (activeSession == null) {
                Log.e("TimeCardViewModel", "No active session found. Cannot start a task.")
                return@launch
            }

            taskStartTime = LocalTime.now().format(formatterWithSeconds) // Format with seconds
            isTaskRunning = true
            taskSeconds = 0L
            updateStateHandle()

            // Create new task entry with seconds included in startTime
            val newTask = TaskTimeEntry(
                sessionId = activeSession.id,
                startTime = taskStartTime ?: "00:00:00",
                duration = 0,
                date = getCurrentDateFormatted(),
                expectedDuration = maxTaskTime.toIntOrNull() ?: 0,
                isTaskRunning = true
            )
            repository.insertTaskTimeEntry(newTask)

            val updatedSession = activeSession.copy(
                numberOfTasks = activeSession.numberOfTasks + 1
            )
            repository.saveSession(updatedSession)

            Log.d("TimeCardViewModel", "Started new task at $taskStartTime linked to session ID ${activeSession.id}")
        }
    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startTask(context: Context) {
//        viewModelScope.launch {
//            val activeSession = repository.getActiveSession() ?: return@launch
//
//            // Check if there's already an active task
//            val activeTask = repository.getActiveTask()
//
//            if (activeTask == null) {
//                // Start a new task
//                val taskStartTime = getCurrentTimeFormatted()
//                isTaskRunning = true
//                taskSeconds = 0L
//                updateStateHandle()
//
//                val newTask = TaskTimeEntry(
//                    sessionId = activeSession.id,
//                    startTime = taskStartTime,
//                    date = getCurrentDateFormatted(),
//                    expectedDuration = maxTaskTime.toIntOrNull() ?: 0,
//                    isTaskRunning = true
//                )
//                repository.insertTaskTimeEntry(newTask)
//            } else {
//                // Task is already running
//                isTaskRunning = true
//                taskStartTime = activeTask.startTime
//                taskSeconds = activeTask.duration
//                updateStateHandle()
//            }
//        }
//    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun restoreTaskTimerState() {
        viewModelScope.launch {
            val activeTask = repository.getActiveTask()
            if (activeTask != null && activeTask.isTaskRunning) {
                isTaskRunning = true
                taskStartTime = activeTask.startTime

                // Ensure taskStartTime is in the correct format with seconds
                val timeString = if (taskStartTime?.length == 5) "$taskStartTime:00" else taskStartTime
                val startTime = LocalTime.parse(timeString, formatterWithSeconds)

                val elapsed = Duration.between(startTime, LocalTime.now()).seconds
                taskSeconds = elapsed + activeTask.duration
                updateStateHandle()

                Log.d("TimeCardViewModel", "Restored active task with start time $taskStartTime and total elapsed seconds $taskSeconds.")
            }
        }
    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    fun restoreTaskTimerState() {
//        viewModelScope.launch {
//            Log.d("TimeCardViewModel", "Attempting to restore task timer state...")
//
//            val activeTask = repository.getActiveTask()
//            if (activeTask != null) {
//                Log.d("TimeCardViewModel", "Active task found in database: $activeTask")
//                Log.d("TimeCardViewModel", "Is task still running: ${activeTask.isTaskRunning}")
//            } else {
//                Log.d("TimeCardViewModel", "No active task found in database.")
//            }
//
//            if (activeTask != null && activeTask.isTaskRunning) {
//                isTaskRunning = true
//                taskStartTime = activeTask.startTime
//
//                Log.d("TimeCardViewModel", "Task start time from database: $taskStartTime")
//                Log.d("TimeCardViewModel", "Current time: ${LocalTime.now()}")
//
//                try {
//                    val startTime = LocalTime.parse(taskStartTime, DateTimeFormatter.ofPattern("HH:mm"))
//                    val elapsed = Duration.between(startTime, LocalTime.now()).seconds
//                    taskSeconds = elapsed + activeTask.duration
//
//                    Log.d("TimeCardViewModel", "Calculated elapsed time since task start: $elapsed seconds")
//                    Log.d("TimeCardViewModel", "Total task seconds including previous duration: $taskSeconds")
//
//                    updateStateHandle()
//                    Log.d("TimeCardViewModel", "Updated state handle with taskRunning = $isTaskRunning, taskSeconds = $taskSeconds")
//                } catch (e: Exception) {
//                    Log.e("TimeCardViewModel", "Error parsing start time or calculating duration: ${e.message}", e)
//                }
//            } else {
//                Log.d("TimeCardViewModel", "No running task to restore.")
//            }
//        }
//    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun completeTask(context: Context) {
        if (!isTaskRunning) return

        val taskEndTime = getCurrentTimeFormatted()
        val taskDurationSeconds = taskSeconds
        savedStateHandle["isTaskRunning"] = false
        savedStateHandle["taskSeconds"] = 0L

        viewModelScope.launch {
            val activeTask = repository.getActiveTask()?.copy(
                endTime = taskEndTime,
                duration = taskDurationSeconds,
                isTaskRunning = false
            )
            activeTask?.let { repository.updateTimeEntry(it) }
            isTaskRunning = false
            taskStartTime = null
        }
    }

    // Helper function to format a duration in HH:MM:SS
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, secs) // Display as HH:MM:SS if hours exist
        } else {
            "%02d:%02d".format(minutes, secs) // Display as MM:SS if hours are zero
        }
    }




    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentTimeFormattedWithoutSeconds(): String {
        return LocalTime.now().format(formatterWithoutSeconds)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDateFormatted(): String = LocalDate.now().toString()



        fun deleteAllSessions() {
        viewModelScope.launch {
            repository.deleteAllSessions()
            updateTimeEntriesByDay()
        }
    }

    fun toggleSessionSubmission(session: Session) {
        viewModelScope.launch {
            Log.d("TimeCardViewModel", "Toggling session ID ${session.id}, current isSubmitted: ${session.isSubmitted}")

            // Only toggle and update if the state needs to change
            val updatedSession = session.copy(isSubmitted = session.isSubmitted)

            // Save the updated session to the repository
            repository.saveSession(updatedSession)

            Log.d("TimeCardViewModel", "Updated session ID ${updatedSession.id}, new isSubmitted: ${updatedSession.isSubmitted}")
//
//            // Refresh the sessions from the database
            updateTimeEntriesByDay()
        }
    }

    fun updateTimeEntriesByDay() {
        viewModelScope.launch {
            repository.getAllSessions().collect { sessions ->
                val groupedByDate = sessions.groupBy { it.date }
                _timeEntriesByDay.value = groupedByDate
            }
        }
    }

}

//@RequiresApi(Build.VERSION_CODES.O)
//@HiltViewModel
//class TimeCardViewModel @Inject constructor(
//    private val repository: TimeTrackingRepository,
//    private val savedStateHandle: SavedStateHandle
//) : ViewModel() {
//
//    // Observable state variables
//    var isTaskRunning by mutableStateOf(savedStateHandle["isTaskRunning"] ?: false)
//        private set
//    var taskSeconds by mutableStateOf(savedStateHandle["taskSeconds"] ?: 0L)
//        private set
//    var taskStartTime: String? = null
//    var maxTaskTime by mutableStateOf(savedStateHandle["maxTaskTime"] ?: "")
//        private set
//
//    private val formatterWithoutSeconds = DateTimeFormatter.ofPattern("hh:mm a")
//    private val formatterWithSeconds = DateTimeFormatter.ofPattern("hh:mm:ss a")
//
//
//    var totalWorkTime by mutableStateOf(0L)
//    var clockInTime: String? = null
//
//    private val _isClockedIn = MutableStateFlow(false)
//    val isClockedIn: StateFlow<Boolean> = _isClockedIn
//
//    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<TaskTimeEntry>>>(emptyMap())
//    val timeEntriesByDay: StateFlow<Map<String, List<TaskTimeEntry>>> = _timeEntriesByDay
//
//    private val _elapsedTime = MutableStateFlow(0L)
//    val elapsedTime: StateFlow<Long> = _elapsedTime
//
//    private var isServiceBound = false
//    private var timerService: TimerService? = null
//
//    private var taskTimerJob: Job? = null // Track the job for the task timer
//
//    init {
////        restoreSessionState()
////        restoreTaskState()
//        updateTimeEntriesByDay()
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getExpectedTaskDuration(): Int {
//        // Convert the selected task time (in minutes) to an integer
//        val expectedDuration = maxTaskTime.toIntOrNull() ?: 0 // Fallback to 0 if maxTaskTime is not set correctly
//        Log.d("TimeCardViewModel", "Expected task duration is: $expectedDuration minutes.")
//        return expectedDuration
//    }
//
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startTask(context: Context) {
//        if (!_isClockedIn.value) {
//            Log.d("TimeCardViewModel", "Cannot start a task when not clocked in.")
//            return
//        }
//
//        if (isTaskRunning) {
//            Log.d("TimeCardViewModel", "A task is already running, skipping start.")
//            return
//        }
//
//        taskStartTime = getCurrentTimeFormatted()
//        isTaskRunning = true
//        taskSeconds = 0L
//
//        // Update state and save the running task to the database
//        viewModelScope.launch {
//            val activeTask = TaskTimeEntry(
//                startTime = taskStartTime ?: "00:00 AM",
//                endTime = "", // End time not set yet
//                duration = 0, // Will be calculated on completion
//                date = getCurrentDateFormatted(),
//                isSubmitted = false,
//                expectedDuration = maxTaskTime.toIntOrNull() ?: 0,
//                isOverUnderAET = false,
//                minutesOverUnderAET = 0,
//                isTaskRunning = true
//            )
//
//            repository.insertTaskTimeEntry(activeTask)
//            Log.d("TimeCardViewModel", "Started new task at $taskStartTime")
//
//            saveSessionState()
//        }
//
//        // Start the task timer and foreground service
//        startForegroundService(context)
//    }
//
//
//
//    fun updateTaskSeconds(seconds: Long) {
//        taskSeconds = seconds
//        savedStateHandle["taskSeconds"] = seconds
//    }
//
//
//
//    /**
//     * Completes the current task and performs necessary updates to stop and finalize it.
//     *
//     * This function is used when a task is marked as fully completed. It updates the task state,
//     * resets relevant variables, and saves the task information to the database. It also stops the
//     * foreground service related to the task timer.
//     *
//     * @param context The context to use for stopping the foreground service.
//     */
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun completeTask(context: Context) {
//        if (!isTaskRunning) {
//            Log.d("TimeCardViewModel", "No task is running, skipping task completion.")
//            return
//        }
//
//        // Mark task as complete
//        val taskEndTime = getCurrentTimeFormatted()
//        val taskDurationMinutes = (taskSeconds / 60).toInt()
//
//        viewModelScope.launch {
//            val activeTask = repository.getActiveTask()
//            if (activeTask == null) {
//                Log.e("TimeCardViewModel", "No active task found to complete.")
//                return@launch
//            }
//
//            // Create a completed task entry
//            val completedTask = activeTask.copy(
//                endTime = taskEndTime,
//                duration = taskDurationMinutes,
//                isTaskRunning = false,
//                isOverUnderAET = calculateOverUnderAET(taskDurationMinutes, activeTask.expectedDuration) != 0
//            )
//
//            Log.d("TimeCardViewModel", "Updating task with endTime: $taskEndTime and isTaskRunning = false")
//
//            // Update the task in the database
//            repository.updateTimeEntry(completedTask)
//            Log.d("TimeCardViewModel", "Task marked as complete in the database: $completedTask")
//
//            // Clear task state and update session state after task completion
//            taskStartTime = null
//            taskSeconds = 0L
//            isTaskRunning = false
//
//            saveSessionState() // This should handle session consistency
//        }
//
//        // Stop the foreground service
//        stopForegroundService(context)
//    }
//
//
//
//
//
//
//
//
//
//    /**
//     * Stops the current task without marking it as completed.
//     *
//     * This function is used when a task needs to be stopped or paused temporarily. It cancels the
//     * running timer job and stops the foreground service but does not finalize the task in the database.
//     * It focuses on stopping ongoing processes related to the task.
//     *
//     * @param context The context to use for stopping the foreground service.
//     */
//    fun stopTask(context: Context) {
//        if (!isTaskRunning) return
//
//        isTaskRunning = false
//        taskTimerJob?.cancel() // Cancel the running timer job
//        taskTimerJob = null // Clear the job to avoid redundant tasks
//
//        savedStateHandle["isTaskRunning"] = false
//        savedStateHandle["taskSeconds"] = 0L // Reset task seconds when the task stops
//
//        Log.d("TimeCardViewModel", "Task stopped and timer reset.")
//
//        stopForegroundService(context) // Stop the foreground service
//    }
//
//    fun calculateOverUnderAET(actualDuration: Int, expectedDuration: Int): Int {
//        // Calculate the difference between the actual and expected duration
//        val overUnderAET = actualDuration - expectedDuration
//        Log.d("TimeCardViewModel", "Actual duration: $actualDuration, Expected duration: $expectedDuration, Over/Under AET: $overUnderAET minutes.")
//        return overUnderAET
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun restoreSessionState(context: Context) {
//        viewModelScope.launch {
//            val session = repository.getSession() // Fetch saved session from the database
//            session?.let {
//                Log.d("TimeCardViewModel", "Restored session data: isClockedIn = ${it.isClockedIn}, clockInTime = ${it.clockInTime}")
//
//                // Restore high-level session data
//                _isClockedIn.value = it.isClockedIn
//                clockInTime = it.clockInTime
//                totalWorkTime = it.totalWorkTime
//
//                // Update savedStateHandle with restored values
//                savedStateHandle["isClockedIn"] = it.isClockedIn
//
//                // Only retrieve active tasks if there is an indication that one might exist
//                if (_isClockedIn.value) {
//                    val activeTask = repository.getActiveTask() // Get the active task from the database
//
//                    // Add a log statement to check the task state
//                    Log.d("TimeCardViewModel", "Retrieved active task: isTaskRunning = ${activeTask?.isTaskRunning}")
//
//                    // If an active task exists and is marked as running, restore its state
//                    activeTask?.let { taskEntry ->
//                        Log.d("TimeCardViewModel", "Restored active task: TaskStartTime = ${taskEntry.startTime}")
//
//                        // Set task-specific properties directly
//                        taskStartTime = taskEntry.startTime
//                        isTaskRunning = true // Since activeTask only returns running tasks
//                        savedStateHandle["isTaskRunning"] = true
//                        savedStateHandle["taskStartTime"] = taskEntry.startTime
//
//                        Log.d("TimeCardViewModel", "Restoring task session and resuming timer.") // Log task restoration
//
//                        // Calculate elapsed time based on task start time
//                        val additionalElapsedTime = calculateElapsedTime(taskStartTime ?: "00:00 AM", taskSeconds)
//                        updateTaskSeconds(additionalElapsedTime)
//
//                        // Rebind to the service and resume the task timer
//                        bindToService(context) {
//                            timerService?.startTaskTimer() // Resume the task timer in the service
//                        }
//                    } ?: run {
//                        // No active running task found, reset task states
//                        resetTaskState()
//                    }
//                } else {
//                    // Not clocked in or no task expected, reset task states
//                    resetTaskState()
//                }
//            } ?: run {
//                // No session found, reset all states
//                Log.d("TimeCardViewModel", "No session found, resetting all states.")
//                _isClockedIn.value = false
//                clockInTime = null
//                totalWorkTime = 0L
//                resetTaskState()
//            }
//        }
//    }
//
//
//
//
//
//
//    // Utility function to reset task-related states
//    private fun resetTaskState() {
//        isTaskRunning = false
//        taskStartTime = null
//        updateTaskSeconds(0)
//        savedStateHandle["isTaskRunning"] = false
//    }
//
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun resetSessionState() {
//        _isClockedIn.value = false
//        clockInTime = null
//        totalWorkTime = 0L
//        resetTaskState() // Reset task states as well
//        Log.d("TimeCardViewModel", "All session and task states reset successfully.")
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun calculateElapsedTime(startTime: String, savedTaskSeconds: Long): Long {
//        val formatterWithSeconds = DateTimeFormatter.ofPattern("hh:mm:ss a")
//        val start = LocalTime.parse(startTime, formatterWithSeconds)
//        val current = LocalTime.now()
//
//        // Calculate duration in seconds
//        val elapsedDuration = java.time.Duration.between(start, current).seconds
//
//        // Add previously saved task seconds
//        val totalElapsedSeconds = elapsedDuration + savedTaskSeconds
//
//        Log.i("ElapsedTime", "Start time: $startTime, Current time: ${current.format(formatterWithSeconds)}, Total elapsed time in seconds: $totalElapsedSeconds")
//        return totalElapsedSeconds
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentTimeFormattedWithoutSeconds(): String {
//        return LocalTime.now().format(formatterWithoutSeconds)
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun saveSessionState() {
//        viewModelScope.launch {
//            // Save the current session state
//            val session = Session(
//                clockInTime = clockInTime ?: "",
//                isClockedIn = _isClockedIn.value,
//                totalWorkTime = totalWorkTime
//            )
//
//            Log.d("TimeCardViewModel", "Saving session state with isClockedIn = ${_isClockedIn.value}.")
//
//            repository.saveSession(session)
//            Log.d("TimeCardViewModel", "Session saved successfully.")
//
//            // Additionally, save the current active task if one is running
//            if (isTaskRunning) {
//                val activeTask = TaskTimeEntry(
//                    startTime = taskStartTime ?: getCurrentTimeFormattedWithoutSeconds(),
//                    endTime = "", // End time not set until task is completed
//                    duration = (taskSeconds / 60).toInt(),
//                    date = getCurrentDateFormatted(),
//                    isSubmitted = false,
//                    expectedDuration = maxTaskTime.toIntOrNull() ?: 0,
//                    isOverUnderAET = false, // This will be updated when the task is completed
//                    minutesOverUnderAET = 0,
//                    isTaskRunning = isTaskRunning
//                )
//
//                Log.d("TimeCardViewModel", "Saving active task entry: $activeTask")
//
//                repository.insertTaskTimeEntry(activeTask)
//                Log.d("TimeCardViewModel", "Active task entry saved successfully.")
//            }
//        }
//    }
//
//
//    // Foreground Service Management
//    fun bindToService(context: Context, onServiceConnected: (() -> Unit)? = null) {
//        val intent = Intent(context, TimerService::class.java)
//        context.bindService(intent, object : ServiceConnection {
//            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
//                val localBinder = binder as? TimerService.TimerBinder
//                timerService = localBinder?.getService()
//
//                isServiceBound = true
//                Log.d("TimeCardViewModel", "Service bound successfully")
//
//                onServiceConnected?.invoke() // Perform any action after service is bound
//            }
//
//            override fun onServiceDisconnected(name: ComponentName?) {
//                timerService = null
//                isServiceBound = false
//            }
//        }, Context.BIND_AUTO_CREATE)
//    }
//
//
//    fun unbindFromService(context: Context) {
//        if (isServiceBound) {
//            context.unbindService(serviceConnection)
//            isServiceBound = false
//        }
//    }
//
//    fun startForegroundService(context: Context) {
//        val serviceIntent = Intent(context, TimerService::class.java)
//        ContextCompat.startForegroundService(context, serviceIntent)
//        bindToService(context) // Bind to the service after starting it
//    }
//
//    fun stopForegroundService(context: Context) {
//        val serviceIntent = Intent(context, TimerService::class.java)
//        context.stopService(serviceIntent)
//        unbindFromService(context) // Unbind from the service after stopping it
//    }
//
//    private val serviceConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
//            val localBinder = binder as? TimerService.TimerBinder
//            timerService = localBinder?.getService()
//
//            if (timerService != null) {
//                isServiceBound = true
//
//                // Use callback to update elapsed and task time
//                timerService?.timerCallback = object : TimerService.TimerCallback {
//                    override fun onTimeUpdate(elapsedTime: Long, clockIn: String) {
//                        // Update the elapsed time and log the clock in time
//                        _elapsedTime.value = elapsedTime
//                        Log.d("TimeCardViewModel", "Elapsed Time: $elapsedTime, ClockIn: $clockIn")
//
//                        // If you want to update the clockInTime variable safely:
//                        if (clockInTime == null) {
//                            clockInTime = clockIn // Assign only if it's not set already
//                        }
//                    }
//
//                    override fun onTaskTimeUpdate(taskSeconds: Long) {
//                        updateTaskSeconds(taskSeconds) // Update task timer in ViewModel
//                        Log.d("TimeCardViewModel", "Task Time: $taskSeconds seconds")
//                    }
//                }
//
////                 Check if a task is running and continue the task timer
////                if (isTaskRunning) {
////                    timerService?.startTaskTimer() // Continue task timer if task is running
////                }
//            }
//        }
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            isServiceBound = false
//        }
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startWorkSession(context: Context) {
//        _isClockedIn.value = true
//        clockInTime = getCurrentTimeFormattedWithoutSeconds() // Save without seconds
//
//        // Save the work session start as a new entry
//        viewModelScope.launch {
//            // Create and insert a new work session entry
//            val session = Session(
//                clockInTime = clockInTime ?: "00:00 AM",
//                isClockedIn = true,
//                totalWorkTime = 0L // This will be calculated upon clock out
//            )
//
//            repository.saveSession(session)
//            Log.d("TimeCardViewModel", "Started work session at $clockInTime")
//
//            // Save initial session state and start the foreground service
//            saveSessionState()
//            startForegroundService(context)
//        }
//    }
//
//
//    fun endWorkSession(context: Context) {
//        // Ensure the user is currently clocked in
//        if (!_isClockedIn.value) return
//
//        _isClockedIn.value = false
//        val clockOutTime = getCurrentTimeFormattedWithoutSeconds() // Save without seconds
//
//        // Calculate total duration for the work session
//        val totalWorkMinutes = calculateDuration(clockInTime ?: "00:00 AM", clockOutTime)
//
//        viewModelScope.launch {
//            // Update the work session and mark it as ended
//            val session = repository.getSession()?.copy(
//                isClockedIn = false,
//                totalWorkTime = totalWorkMinutes.toLong()
//            )
//
//            session?.let {
//                repository.saveSession(it)
//                Log.d("TimeCardViewModel", "Ended work session at $clockOutTime with total duration $totalWorkMinutes minutes")
//
//                // Stop the foreground service and save session state
//                stopForegroundService(context)
//                saveSessionState()
//            }
//        }
//    }
//
//
//
//    // Time entry-related functions
//    fun updateTimeEntriesByDay() {
//        viewModelScope.launch {
//            repository.getAllTimeEntries().collect { entries ->
//                _timeEntriesByDay.value = entries.groupBy { it.date }
//            }
//        }
//    }
//        fun updateMaxTaskTime(time: String) {
//        maxTaskTime = time
//        savedStateHandle["maxTaskTime"] = time
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentTimeFormatted(): String {
//        return LocalTime.now().format(formatterWithSeconds)
//    }
//
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentDateFormatted(): String = LocalDate.now().toString()
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun calculateDuration(startTime: String, endTime: String): Int {
//        val start = LocalTime.parse(startTime, formatterWithoutSeconds)
//        val end = LocalTime.parse(endTime, formatterWithoutSeconds)
//
//        return java.time.Duration.between(start, end).toMinutes().toInt()
//    }
//
//
//    fun deleteAllEntries() {
//        viewModelScope.launch {
//            repository.deleteAllTimeEntries()
//            updateTimeEntriesByDay()
//        }
//    }
//
//    fun toggleTimeEntrySubmission(entry: TaskTimeEntry) {
//        viewModelScope.launch {
//            val updatedEntry = entry.copy(isSubmitted = !entry.isSubmitted)
//            repository.updateTimeEntry(updatedEntry)
//            updateTimeEntriesByDay()
//        }
//    }
//}







