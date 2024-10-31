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
    var isTaskRunning by mutableStateOf(false)
        private set
    var taskSeconds by mutableStateOf(0L)
        private set
    var taskStartTime: String? = null
    var maxTaskTime by mutableStateOf("")

    val taskDurationSeconds = taskSeconds // Already in seconds

    private val formatterWithoutSeconds = DateTimeFormatter.ofPattern("hh:mm a")

    var totalWorkTime by mutableStateOf(0L)
    var clockInTime: String? = null

    private val _isClockedIn = MutableStateFlow(false)
    val isClockedIn: StateFlow<Boolean> = _isClockedIn

    // StateFlow to manage the mapping of date to session lists
    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<Session>>>(emptyMap())
    val timeEntriesByDay: StateFlow<Map<String, List<Session>>> = _timeEntriesByDay
    init {
        updateTimeEntriesByDay()
    }


    // Function to start the work session (clock-in)
    @RequiresApi(Build.VERSION_CODES.O)
    fun startWorkSession(context: Context) {
        _isClockedIn.value = true
        clockInTime = getCurrentTimeFormattedWithoutSeconds()

        val currentDate = LocalDate.now().toString() // Set the current date

        // Create and save session in the database
        viewModelScope.launch {
            val session = Session(
                date = currentDate, // <-- Set the date here
                clockInTime = clockInTime ?: "00:00 AM",
                isClockedIn = true,
                totalWorkTime = 0L, // Initially zero, will be calculated on clock-out
                isSubmitted = false
            )

            repository.saveSession(session)
            Log.d("TimeCardViewModel", "Started work session at $clockInTime on date $currentDate")
        }
    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun endWorkSession(context: Context) {
//        if (!_isClockedIn.value) return
//
//        _isClockedIn.value = false
//        val clockOutTime = getCurrentTimeFormattedWithoutSeconds()
//
//        // Calculate total session duration
//        val sessionDurationSeconds = calculateSessionDuration(clockInTime ?: "00:00 AM", clockOutTime)
//        totalWorkTime = sessionDurationSeconds
//
//        viewModelScope.launch {
//            val activeSession = repository.getActiveSession()?.copy(
//                isClockedIn = false,
//                clockOutTime = clockOutTime, // Set the specific clockOutTime for this session
//                totalWorkTime = sessionDurationSeconds,
//                isSubmitted = false
//            )
//
//            activeSession?.let {
//                repository.saveSession(it)
//                Log.d("TimeCardViewModel", "Ended work session at $clockOutTime with total duration ${formatDuration(sessionDurationSeconds)}")
//            } ?: run {
//                Log.e("TimeCardViewModel", "No active session found to end.")
//            }
//        }
//    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun endWorkSession(context: Context) {
        if (!_isClockedIn.value) return

        _isClockedIn.value = false
        val clockOutTime = getCurrentTimeFormattedWithoutSeconds()

        // Calculate total session duration
        val sessionDurationSeconds = calculateSessionDuration(clockInTime ?: "00:00 AM", clockOutTime)
        totalWorkTime = sessionDurationSeconds

        viewModelScope.launch {
            // Fetch the actual active (clocked in) session to ensure correct update
            val activeSession = repository.getActiveSession()?.copy(
                isClockedIn = false,
                clockOutTime = clockOutTime,
                totalWorkTime = sessionDurationSeconds,
                isSubmitted = false
            )

            activeSession?.let {
                repository.saveSession(it) // Save by unique ID to avoid overwriting
                Log.d("TimeCardViewModel", "Ended work session at $clockOutTime with total duration ${formatDuration(sessionDurationSeconds)}")
            } ?: run {
                Log.e("TimeCardViewModel", "No active session found to end.")
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

        fun updateTaskSeconds(seconds: Long) {
        taskSeconds = seconds
        savedStateHandle["taskSeconds"] = seconds
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startTask(context: Context) {
        if (!_isClockedIn.value) {
            Log.d("TimeCardViewModel", "Cannot start a task when not clocked in.")
            return
        }

        if (isTaskRunning) {
            Log.d("TimeCardViewModel", "A task is already running, skipping start.")
            return
        }

        taskStartTime = getCurrentTimeFormatted()
        isTaskRunning = true
        taskSeconds = 0L

        viewModelScope.launch {
            // Retrieve the active session
            val activeSession = repository.getActiveSession()
            if (activeSession == null) {
                Log.e("TimeCardViewModel", "No active session found. Cannot start a task.")
                return@launch
            }

            // Save the task in the database, linking it to the active session
            val activeTask = TaskTimeEntry(
                sessionId = activeSession.id, // Link to the current session
                startTime = taskStartTime ?: "00:00 AM",
                endTime = "", // Not yet set
                duration = 0,
                date = getCurrentDateFormatted(),
                expectedDuration = maxTaskTime.toIntOrNull() ?: 0,
                secondsOverUnderAET = 0,
                isTaskRunning = true
            )

            repository.insertTaskTimeEntry(activeTask)

            // Update the number of tasks in the session
            val updatedSession = activeSession.copy(
                numberOfTasks = activeSession.numberOfTasks + 1
            )
            repository.saveSession(updatedSession)

            Log.d("TimeCardViewModel", "Started new task at $taskStartTime")
        }
    }




    // Function to complete a task
    @RequiresApi(Build.VERSION_CODES.O)
    fun completeTask(context: Context) {
//        if (!isTaskRunning) {
//            Log.d("TimeCardViewModel", "No task is running, skipping task completion.")
//            return
//        }

        // Calculate end time and lock the duration
        val taskEndTime = getCurrentTimeFormatted()
        val taskDurationSeconds = taskSeconds

        viewModelScope.launch {
            // Get the active task from the repository
            val activeTask = repository.getActiveTask()
            if (activeTask == null) {
                Log.e("TimeCardViewModel", "No active task found to complete.")
                return@launch
            }

            // Calculate over/under AET in seconds
            val expectedDurationSeconds = activeTask.expectedDuration * 60L
            val overUnderAET = taskDurationSeconds - expectedDurationSeconds

            // Update the task entry with the calculated values
            val completedTask = activeTask.copy(
                endTime = taskEndTime,
                duration = taskDurationSeconds,
                secondsOverUnderAET = overUnderAET,
                isTaskRunning = false
            )

            // Update the entry in the database
            repository.updateTimeEntry(completedTask)
            Log.d("TimeCardViewModel", "Task completed at $taskEndTime with duration $taskDurationSeconds seconds and over/under AET: $overUnderAET seconds.")

            // Update the associated session's over/under AET sum and number of tasks
            val activeSession = repository.getActiveSession()
            activeSession?.let { session ->
                val updatedSession = session.copy(
                    totalOverUnderAET = session.totalOverUnderAET + overUnderAET,
                    numberOfTasks = session.numberOfTasks // This remains the same unless new tasks were added
                )
                repository.saveSession(updatedSession)
                Log.d("TimeCardViewModel", "Updated session with new over/under AET total: ${updatedSession.totalOverUnderAET} seconds.")
            }

            // Clear the task state
            isTaskRunning = false
            taskStartTime = null
            taskSeconds = 0L // Clear the counter
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

    // Function to fetch the active session
    @RequiresApi(Build.VERSION_CODES.O)
    fun getActiveSession(): Session? {
        var activeSession: Session? = null
        viewModelScope.launch {
            activeSession = repository.getActiveSession()
            Log.d("TimeCardViewModel", "Active session retrieved: $activeSession")
        }
        return activeSession
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






    fun submitSession(session: Session) {
        viewModelScope.launch {
            // Update the session's isSubmitted field to true
            val updatedSession = session.copy(isSubmitted = true)
            repository.saveSession(updatedSession)

            // Log the submission action for debugging
            Log.d("TimeCardViewModel", "Session submitted with ID: ${updatedSession.id}")

            // Trigger UI updates if necessary, like refreshing the list of sessions
            updateTimeEntriesByDay() // Or other appropriate method to update the session list
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







