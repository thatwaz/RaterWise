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
import com.thatwaz.raterwise.data.model.SessionWithTasks
import com.thatwaz.raterwise.data.model.SessionWithTasksAndEntries
import com.thatwaz.raterwise.data.repository.TimeTrackingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject


@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class TimeCardViewModel @Inject constructor(
    private val repository: TimeTrackingRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _currentWeekEntries = MutableStateFlow<Map<String, List<SessionWithTasksAndEntries>>>(emptyMap())
    val currentWeekEntries: StateFlow<Map<String, List<SessionWithTasksAndEntries>>> = _currentWeekEntries


    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<SessionWithTasks>>>(emptyMap())
    val timeEntriesByDay: StateFlow<Map<String, List<SessionWithTasks>>> = _timeEntriesByDay

    var isTaskRunning by mutableStateOf(savedStateHandle["isTaskRunning"] ?: false)
        private set
    var taskSeconds by mutableStateOf(savedStateHandle["taskSeconds"] ?: 0L)
        private set

    private val _clockInTime = MutableStateFlow(savedStateHandle["clockInTime"] ?: "Not Clocked In")
    val clockInTime: StateFlow<String> = _clockInTime

    var taskStartTime: String? = savedStateHandle["taskStartTime"]

    var expectedTaskDuration by mutableStateOf(savedStateHandle["expectedTaskDuration"] ?: 0L) // Default to 0L
        private set


    // Max allowed time per task (added for session restore)
//    var maxTaskTime by mutableStateOf(savedStateHandle["maxTaskTime"] ?: 0L)
//        private set

    // Over/Under AET calculation (added to record total over/under time)
    var totalOverUnderAET by mutableStateOf(savedStateHandle["totalOverUnderAET"] ?: 0L)
        private set

    var currentSessionId: Long by mutableStateOf(savedStateHandle["currentSessionId"] ?: 0L)
        private set



    // Flag to check if the user is clocked in
    private val _isClockedIn = MutableStateFlow(savedStateHandle["isClockedIn"] ?: false)
    val isClockedIn: StateFlow<Boolean> = _isClockedIn

    init {

        viewModelScope.launch {
            restoreSessionState() // Ensure this is called at init to restore saved data
            loadCurrentWeekEntries()
            loadTimeEntriesByDay()
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    fun startWorkSession() {
        if (_isClockedIn.value) {
            Log.e("TimeCardViewModel", "Cannot start a new work session. Already clocked in.")
            return
        }

        val currentClockInTime = getCurrentTimeFormatted()
        _clockInTime.value = currentClockInTime
        _isClockedIn.value = true

        savedStateHandle["clockInTime"] = currentClockInTime
        savedStateHandle["isClockedIn"] = true

        Log.d("TimeCardViewModel", "Clock-in time set to: $currentClockInTime")

        viewModelScope.launch {
            val newSession = SessionWithTasks(
                date = getCurrentDateFormatted(),
                clockInTime = currentClockInTime,
                isClockedIn = true
            )
            val sessionId: Long = repository.insertSessionWithTasks(newSession)
            currentSessionId = sessionId
            savedStateHandle["currentSessionId"] = currentSessionId
            Log.d("TimeCardViewModel", "Started new work session with ID: $currentSessionId")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun endWorkSession() {
        if (!_isClockedIn.value) {
            Log.e("TimeCardViewModel", "Cannot end work session. Not currently clocked in.")
            return
        }

        val clockOutTime = getCurrentTimeFormatted()
        val clockInTime: String = savedStateHandle["clockInTime"] ?: run {
            Log.e("TimeCardViewModel", "Clock-in time not found.")
            return
        }

        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        val clockInDateTime = LocalTime.parse(clockInTime, formatter)
        val clockOutDateTime = LocalTime.parse(clockOutTime, formatter)
        val totalWorkTime = Duration.between(clockInDateTime, clockOutDateTime).seconds.coerceAtLeast(0)

        viewModelScope.launch {
            val sessionId = savedStateHandle.get<Long>("currentSessionId")
                ?: savedStateHandle.get<Int>("currentSessionId")?.toLong()

            if (sessionId == null) {
                Log.e("TimeCardViewModel", "Session ID not found.")
                return@launch
            }

            val currentSession = repository.getSessionWithTasksById(sessionId.toLong())?.copy(
                clockOutTime = clockOutTime,
                totalWorkTime = totalWorkTime,
                isClockedIn = false
            )

            if (currentSession != null) {
                repository.updateSessionWithTasks(currentSession)
                Log.d("TimeCardViewModel", "Ended work session at $clockOutTime with duration ${totalWorkTime / 60} minutes.")
            } else {
                Log.e("TimeCardViewModel", "Failed to retrieve the session with ID: $sessionId")
            }

            _isClockedIn.value = false
            savedStateHandle["isClockedIn"] = false
            savedStateHandle["clockInTime"] = null
        }
    }



    fun getCurrentDateFormatted(): String {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd") // Adjust pattern if you need a different format
        return currentDate.format(formatter)
    }

    // might need to alter name for expected task duration
    fun updateMaxTaskTime(maxTaskTime: String) {
        val taskDurationAsLong = maxTaskTime.toLongOrNull() ?: 0L // Convert to Long with default
        expectedTaskDuration = taskDurationAsLong // Set the updated duration
        savedStateHandle["expectedTaskDuration"] = taskDurationAsLong // Use correct key in savedStateHandle

        Log.d("TimeCardViewModel", "Updated maxTaskTime to $taskDurationAsLong minutes")
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun startTask(context: Context, duration: Int) { // Pass expected duration as Int in minutes
        if (!isClockedIn.value || isTaskRunning) {
            Log.e("TimeCardViewModel", "Cannot start a new task.")
            return
        }

        // Set task start parameters
        isTaskRunning = true
        taskStartTime = getCurrentTimeFormattedWithSeconds()
        val taskDurationInSeconds = duration * 60L // Convert minutes to seconds
        expectedTaskDuration = taskDurationInSeconds // Set expected duration in seconds
        savedStateHandle["expectedTaskDuration"] = taskDurationInSeconds // Save to savedStateHandle

        savedStateHandle["isTaskRunning"] = isTaskRunning
        savedStateHandle["taskStartTime"] = taskStartTime

        viewModelScope.launch {
            val sessionId = currentSessionId ?: run {
                Log.e("TimeCardViewModel", "No active session found to start task.")
                return@launch
            }
            val updatedSession = repository.getSessionWithTasksById(sessionId)?.copy(
                isTaskRunning = isTaskRunning,
                taskStartTime = taskStartTime,
                expectedTaskDuration = taskDurationInSeconds // Store in session in seconds
            )
            updatedSession?.let { repository.updateSessionWithTasks(it) }
            Log.d("TimeCardViewModel", "Started task at $taskStartTime with expected duration $taskDurationInSeconds seconds, session updated with isTaskRunning: $isTaskRunning")
        }
    }






    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentTimeFormattedWithSeconds(): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss") // 24-hour format with seconds
        return LocalTime.now().format(formatter)
    }

    fun completeTask(context: Context) {
        if (!isTaskRunning) return

        // Calculate actual duration
        val actualDuration = taskSeconds
        val overUnderAET = actualDuration - expectedTaskDuration

        // Update the task and session in the database
        viewModelScope.launch {
            val sessionId = currentSessionId
            val session = repository.getSessionWithTasksById(sessionId)
            session?.let {
                val updatedSession = it.copy(
                    isTaskRunning = false,
                    taskSeconds = 0L,
                    numberOfTasks = it.numberOfTasks + 1,  // Increment task count
                    totalOverUnderAET = it.totalOverUnderAET + overUnderAET // Update AET
                )
                repository.updateSessionWithTasks(updatedSession)
                Log.d("TimeCardViewModel", "Completed task. Task count: ${updatedSession.numberOfTasks}, Over/Under AET: ${updatedSession.totalOverUnderAET}")
            }
        }

        isTaskRunning = false
        taskSeconds = 0L
    }


//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun completeTask(context: Context) {
//        if (!isTaskRunning) return
//
//        // Capture the actual time of task completion
//        val taskEndTime = LocalTime.now()
//        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
//        val startTime = LocalTime.parse(taskStartTime, formatter)
//        val actualDuration = Duration.between(startTime, taskEndTime).seconds
//
//        // Calculate over/under AET in seconds
//        val overUnderAET = actualDuration - expectedTaskDuration
//
//        // Reset task status
//        isTaskRunning = false
//        taskSeconds = 0L
//        taskStartTime = null
//        savedStateHandle["isTaskRunning"] = isTaskRunning
//        savedStateHandle["taskSeconds"] = taskSeconds
//
//        viewModelScope.launch {
//            val sessionId = currentSessionId ?: run {
//                Log.e("TimeCardViewModel", "No active session found to complete task.")
//                return@launch
//            }
//
//            val updatedSession = repository.getSessionWithTasksById(sessionId)?.let {
//                it.copy(
//                    isTaskRunning = false,
//                    taskSeconds = 0L,
//                    taskStartTime = null,
//                    totalOverUnderAET = it.totalOverUnderAET + overUnderAET // Accumulate over/under AET
//                )
//            }
//
//            updatedSession?.let { repository.updateSessionWithTasks(it) }
//            Log.d(
//                "TimeCardViewModel",
//                "Completed task with actual duration $actualDuration seconds, " +
//                        "expected duration $expectedTaskDuration seconds, " +
//                        "overUnderAET $overUnderAET seconds. " +
//                        "Total over/under AET for session: ${updatedSession?.totalOverUnderAET} seconds."
//            )
//        }
//    }





    @RequiresApi(Build.VERSION_CODES.O)
    fun loadCurrentWeekEntries() {
        viewModelScope.launch {
            val startDate = getCurrentWeekStartDate()
            val endDate = getCurrentWeekEndDate()

            val weeklyEntries = repository.getSessionsWithTasksInDateRange(startDate, endDate)
            val groupedEntries = weeklyEntries.groupBy { it.session.date }

            _currentWeekEntries.value = groupedEntries

            Log.d("TimeCardViewModel", "Loaded current week entries: $groupedEntries")
            Log.d("TimeCardViewModel", "Loading sessions from $startDate to $endDate")
            Log.d("TimeCardViewModel", "Calculated week range: $startDate to $endDate")

            weeklyEntries.forEach { entry ->
                Log.d("TimeCardViewModel", "Session on ${entry.session.date} - Tasks: ${entry.session.numberOfTasks}, Total Over/Under AET: ${entry.session.totalOverUnderAET}")
            }

        }
    }





    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentTimeFormatted(): String {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a") // 12-hour format with AM/PM
        return LocalTime.now().format(formatter)
    }

    fun updateTaskSeconds(elapsedSeconds: Long) {
        if (!isTaskRunning) {
            Log.e("TimeCardViewModel", "Cannot update task time. No active task.")
            return
        }

        taskSeconds = elapsedSeconds
        savedStateHandle["taskSeconds"] = elapsedSeconds

        viewModelScope.launch {
            val sessionId = currentSessionId

            // Ensure sessionId is not null before proceeding
            if (sessionId == null) {
                Log.e("TimeCardViewModel", "Cannot update task seconds. Session ID is null.")
                return@launch
            }

            val updatedSession = repository.getSessionById(sessionId)?.copy(
                taskSeconds = elapsedSeconds
            )

            updatedSession?.let {
                repository.updateSessionWithTasks(it)
                Log.d("TimeCardViewModel", "Updated task seconds to $elapsedSeconds for session ID $sessionId")
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentWeekStartDate(): String {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        return startOfWeek.toString()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentWeekEndDate(): String {
        val today = LocalDate.now()
        val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        return endOfWeek.toString()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun restoreSessionState() {
        viewModelScope.launch {
            val latestSession = repository.getMostRecentSession()

            latestSession?.let {
                currentSessionId = it.sessionId.toLong()
                isTaskRunning = it.isTaskRunning
                _isClockedIn.value = it.isClockedIn
                taskStartTime = it.taskStartTime
                taskSeconds = it.taskSeconds
                expectedTaskDuration = it.expectedTaskDuration // Restore the expected task duration

                val restoredClockInTime = it.clockInTime ?: "Not Clocked In"
                _clockInTime.value = restoredClockInTime
                savedStateHandle["clockInTime"] = restoredClockInTime

                // Restore task duration state
                savedStateHandle["expectedTaskDuration"] = expectedTaskDuration

                if (isTaskRunning && taskStartTime != null) {
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    val startTime = LocalTime.parse(taskStartTime, formatter)
                    val currentTime = LocalTime.now()
                    taskSeconds = Duration.between(startTime, currentTime).seconds.coerceAtLeast(0)
                    savedStateHandle["taskSeconds"] = taskSeconds
                }

                savedStateHandle["isClockedIn"] = it.isClockedIn
                savedStateHandle["currentSessionId"] = currentSessionId
                savedStateHandle["isTaskRunning"] = isTaskRunning
                savedStateHandle["taskStartTime"] = taskStartTime

                Log.d(
                    "TimeCardViewModel",
                    "Restored session state - currentSessionId: $currentSessionId, isTaskRunning: $isTaskRunning, taskSeconds: $taskSeconds, taskStartTime: $taskStartTime, clockInTime: $restoredClockInTime, expectedTaskDuration: $expectedTaskDuration"
                )
            } ?: Log.d("TimeCardViewModel", "No session to restore.")
        }
    }






    private suspend fun loadTimeEntriesByDay() {
        val allSessions = repository.getAllSessions()
        val groupedEntries = allSessions.groupBy { it.date }
        _timeEntriesByDay.value = groupedEntries
        Log.d("TimeCardViewModel", "Loaded time entries by day: $groupedEntries")
    }

    fun deleteAllSessions() {
        viewModelScope.launch {
            repository.deleteAllSessions()
            updateTimeEntriesByDay()
        }
    }


    fun toggleSessionSubmission(sessionWithTasks: SessionWithTasks) {
        viewModelScope.launch {
            Log.d("TimeCardViewModel", "Toggling session ID ${sessionWithTasks.sessionId}, current isSubmitted: ${sessionWithTasks.isSubmitted}")

            // Toggle the `isSubmitted` property
            val updatedSession = sessionWithTasks.copy(isSubmitted = !sessionWithTasks.isSubmitted)

            // Save the updated session to the repository
            repository.updateSessionWithTasks(updatedSession)

            Log.d("TimeCardViewModel", "Updated session ID ${updatedSession.sessionId}, new isSubmitted: ${updatedSession.isSubmitted}")

            // Refresh the sessions from the database
            updateTimeEntriesByDay()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun updateTimeEntriesByDay() {
        viewModelScope.launch {
            val sessions = repository.getSessionsGroupedByDate()

            // Group sessions by date and store in timeEntriesByDay
            _timeEntriesByDay.value = sessions
            Log.d("TimeCardViewModel", "Updated timeEntriesByDay with grouped sessions: $sessions")
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
//    private val formatterWithoutSeconds = DateTimeFormatter.ofPattern("hh:mm a")
//
//    // Observable state variables
//    var isTaskRunning by mutableStateOf(savedStateHandle["isTaskRunning"] ?: false)
//        private set
//    var taskSeconds by mutableStateOf(savedStateHandle["taskSeconds"] ?: 0L)
//        private set
//
//    var taskStartTime: String? = savedStateHandle["taskStartTime"]
//    var maxTaskTime by mutableStateOf(savedStateHandle["maxTaskTime"] ?: "")
//
//    var totalWorkTime by mutableStateOf(savedStateHandle["totalWorkTime"] ?: 0L)
//    var clockInTime: String? = savedStateHandle["clockInTime"]
//
//    private val formatterWithSeconds = DateTimeFormatter.ofPattern("HH:mm:ss")
//
//    private val _isClockedIn = MutableStateFlow(savedStateHandle["isClockedIn"] ?: false)
//    val isClockedIn: StateFlow<Boolean> = _isClockedIn
//
//
//    private val _timeEntriesByDay = MutableStateFlow<Map<String, List<Session>>>(emptyMap())
//    val timeEntriesByDay: StateFlow<Map<String, List<Session>>> = _timeEntriesByDay
//
//    private val _currentWeekEntries = MutableStateFlow<Map<String, List<Pair<Session, List<TaskTimeEntry>>>>>(emptyMap())
//    val currentWeekEntries: StateFlow<Map<String, List<Pair<Session, List<TaskTimeEntry>>>>> = _currentWeekEntries
//
//
//
//    init {
//        viewModelScope.launch {
//            restoreSessionState()
//            updateTimeEntriesByDay()
//            restoreTaskTimerState()
//            taskStartTime = savedStateHandle["taskStartTime"]
////            refreshTimeEntries()
////            loadCurrentWeekEntries()
//
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun loadCurrentWeekEntries() {
//        viewModelScope.launch {
//            val allEntries = repository.getSessionsGroupedByDate()
//            Log.d("TimeCardViewModel", "All entries before filtering: $allEntries")
//
//            val currentWeekEntries = getCurrentWeekEntries(allEntries)
//            if (currentWeekEntries.isEmpty()) {
//                Log.w("TimeCardViewModel", "Warning: No entries found for the current week.")
//            }
//
//            _currentWeekEntries.value = currentWeekEntries.mapValues { (_, sessions) ->
//                sessions.map { session ->
//                    val taskEntries = repository.getTaskEntriesBySessionId(session.id)
//                    session to taskEntries
//                }
//            }
//            Log.d("TimeCardViewModel", "Filtered weekly entries after mapping: ${_currentWeekEntries.value}")
//        }
//    }
//
//
////    @RequiresApi(Build.VERSION_CODES.O)
////    fun loadCurrentWeekEntries() {
////        viewModelScope.launch {
////            val allEntries = repository.getSessionsGroupedByDate()
////            val currentWeekEntries = getCurrentWeekEntries(allEntries)
////
////            // Update _currentWeekEntries instead of _timeEntriesByDay
////            _currentWeekEntries.value = currentWeekEntries.mapValues { (_, sessions) ->
////                sessions.map { session ->
////                    val taskEntries = repository.getTaskEntriesBySessionId(session.id)
////                    session to taskEntries
////                }
////            }
////            Log.d("TimeCardViewModel", "Filtered weekly entries: $currentWeekEntries")
////        }
////    }
//
//
//
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun getCurrentWeekEntries(entries: Map<String, List<Session>>): Map<String, List<Session>> {
//        val today = LocalDate.now()
//
//        // Set the start of the week to the most recent Sunday before or on today's date
//        val startOfWeek = today.with(DayOfWeek.SUNDAY).minusWeeks(1)
//        val endOfWeek = startOfWeek.plusDays(6)
//
//        Log.d("getCurrentWeekEntries", "Filtering entries from $startOfWeek to $endOfWeek")
//
//        val filteredEntries = entries.filter { (date, _) ->
//            val entryDate = LocalDate.parse(date)
//            entryDate in startOfWeek..endOfWeek
//        }
//
//        Log.d("getCurrentWeekEntries", "Filtered entries: $filteredEntries")
//        return filteredEntries
//    }
//
//
//
//
//    private fun updateStateHandle() {
//        savedStateHandle["isTaskRunning"] = isTaskRunning
//        savedStateHandle["taskSeconds"] = taskSeconds
//        savedStateHandle["taskStartTime"] = taskStartTime
//        savedStateHandle["clockInTime"] = clockInTime
//        savedStateHandle["totalWorkTime"] = totalWorkTime
//        savedStateHandle["isClockedIn"] = _isClockedIn.value
//
//        Log.d("TimeCardViewModel", "Updated SavedStateHandle - isTaskRunning: $isTaskRunning, taskSeconds: $taskSeconds, taskStartTime: $taskStartTime, clockInTime: $clockInTime, totalWorkTime: $totalWorkTime, isClockedIn: ${_isClockedIn.value}")
//    }
//
//
//private fun restoreSessionState() {
//    // Log initial restore attempt
//    Log.d("TimeCardViewModel", "Restoring session state from SavedStateHandle...")
//
//    // Retrieve initial values from SavedStateHandle
//    isTaskRunning = savedStateHandle["isTaskRunning"] ?: false
//    taskSeconds = savedStateHandle["taskSeconds"] ?: 0L
//    taskStartTime = savedStateHandle["taskStartTime"]
//    clockInTime = savedStateHandle["clockInTime"]
//    totalWorkTime = savedStateHandle["totalWorkTime"] ?: 0L
//    _isClockedIn.value = savedStateHandle["isClockedIn"] ?: false
//
//    // Log the values retrieved from SavedStateHandle
//    Log.d("TimeCardViewModel", "SavedStateHandle - isTaskRunning: $isTaskRunning, taskSeconds: $taskSeconds, taskStartTime: $taskStartTime, clockInTime: $clockInTime, totalWorkTime: $totalWorkTime, isClockedIn: ${_isClockedIn.value}")
//
//    viewModelScope.launch {
//        if (clockInTime == null || !_isClockedIn.value) {
//            // Retrieve active session from database if not stored in SavedStateHandle
//            val activeSession = repository.getActiveSession()
//            activeSession?.let {
//                _isClockedIn.value = it.isClockedIn
//                clockInTime = it.clockInTime
//                totalWorkTime = it.totalWorkTime
//                updateStateHandle() // Save restored session to SavedStateHandle
//
//                // Log that we restored the session from the database
//                Log.d("TimeCardViewModel", "Restored active session from database - isClockedIn: ${it.isClockedIn}, clockInTime: ${it.clockInTime}, totalWorkTime: ${it.totalWorkTime}")
//            }
//        }
//
//        // Check for an active task as well
//        val activeTask = repository.getActiveTask()
//        if (activeTask != null) {
//            isTaskRunning = true
//            taskStartTime = activeTask.startTime
//
//            // Parse taskStartTime and calculate the elapsed time
//            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
//            val startTime = LocalTime.parse(taskStartTime, formatter)
//            val now = LocalTime.now()
//            val elapsed = Duration.between(startTime, now).seconds
//
//            // Log how many seconds have passed since the task was started
//            Log.d("TimeCardViewModel", "Seconds since task started: $elapsed seconds (from $taskStartTime to $now)")
//
//            // Update taskSeconds based on elapsed time
//            taskSeconds = elapsed + activeTask.duration
//            updateStateHandle() // Save restored task state to SavedStateHandle
//
//            // Log restored task details with calculated elapsed time
//            Log.d("TimeCardViewModel", "Restored active task from database - isTaskRunning: $isTaskRunning, taskStartTime: $taskStartTime, calculated taskSeconds: $taskSeconds")
//        } else {
//            Log.d("TimeCardViewModel", "No active task found in database.")
//        }
//    }
//}
//
//
//    // Function to start the work session (clock-in)
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startWorkSession(context: Context) {
//        _isClockedIn.value = true
//        clockInTime = getCurrentTimeFormattedWithoutSeconds()
//        updateStateHandle()
//        savedStateHandle["clockInTime"] = clockInTime
//        savedStateHandle["isClockedIn"] = true
//
//        viewModelScope.launch {
//            val session = Session(
//                date = LocalDate.now().toString(),
//                clockInTime = clockInTime ?: "00:00 AM",
//                isClockedIn = true,
//                totalWorkTime = 0L,
//                isSubmitted = false
//            )
//            repository.saveSession(session)
//            Log.d("TimeCardViewModel", "Started work session at $clockInTime")
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun endWorkSession(context: Context) {
//        if (!_isClockedIn.value) return
//
//        _isClockedIn.value = false
//        savedStateHandle["isClockedIn"] = false
//        val clockOutTime = getCurrentTimeFormattedWithoutSeconds()
//        val sessionDurationSeconds = calculateSessionDuration(clockInTime ?: "00:00 AM", clockOutTime)
//        totalWorkTime = sessionDurationSeconds
//        updateStateHandle()
//
//        viewModelScope.launch {
//            val activeSession = repository.getActiveSession()?.copy(
//                isClockedIn = false,
//                clockOutTime = clockOutTime,
//                totalWorkTime = sessionDurationSeconds
//            )
//
//            activeSession?.let {
//                repository.saveSession(it)
//                Log.d("TimeCardViewModel", "Ended work session at $clockOutTime with duration ${formatDuration(sessionDurationSeconds)}")
//            }
//        }
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun calculateSessionDuration(startTime: String, endTime: String): Long {
//        val start = LocalTime.parse(startTime, formatterWithoutSeconds)
//        val end = LocalTime.parse(endTime, formatterWithoutSeconds)
//        return java.time.Duration.between(start, end).seconds
//    }
//
//
//    fun updateMaxTaskTime(time: String) {
//        maxTaskTime = time
//        savedStateHandle["maxTaskTime"] = time
//    }
//
//    // Update taskSeconds and save it to savedStateHandle for persistence
//    fun updateTaskSeconds(newSeconds: Long) {
//        taskSeconds = newSeconds
//        savedStateHandle["taskSeconds"] = newSeconds
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun startTask(context: Context) {
//        if (!_isClockedIn.value) {
//            Log.d("TimeCardViewModel", "Cannot start a task when not clocked in.")
//            return
//        }
//
//        viewModelScope.launch {
//            val activeSession = repository.getActiveSession()
//            if (activeSession == null) {
//                Log.e("TimeCardViewModel", "No active session found. Cannot start a task.")
//                return@launch
//            }
//
//            taskStartTime = LocalTime.now().format(formatterWithSeconds) // Format with seconds
//            isTaskRunning = true
//            taskSeconds = 0L
//            updateStateHandle()
//
//            // Create new task entry with seconds included in startTime
//            val newTask = TaskTimeEntry(
//                sessionId = activeSession.id,
//                startTime = taskStartTime ?: "00:00:00",
//                duration = 0,
//                date = getCurrentDateFormatted(),
//                expectedDuration = maxTaskTime.toIntOrNull() ?: 0,
//                isTaskRunning = true
//            )
//            repository.insertTaskTimeEntry(newTask)
//
//            val updatedSession = activeSession.copy(
//                numberOfTasks = activeSession.numberOfTasks + 1
//            )
//            repository.saveSession(updatedSession)
//
//            Log.d("TimeCardViewModel", "Started new task at $taskStartTime linked to session ID ${activeSession.id}")
//        }
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun restoreTaskTimerState() {
//        viewModelScope.launch {
//            val activeTask = repository.getActiveTask()
//            if (activeTask != null && activeTask.isTaskRunning) {
//                isTaskRunning = true
//                taskStartTime = activeTask.startTime
//
//                // Ensure taskStartTime is in the correct format with seconds
//                val timeString = if (taskStartTime?.length == 5) "$taskStartTime:00" else taskStartTime
//                val startTime = LocalTime.parse(timeString, formatterWithSeconds)
//
//                val elapsed = Duration.between(startTime, LocalTime.now()).seconds
//                taskSeconds = elapsed + activeTask.duration
//                updateStateHandle()
//
//                Log.d("TimeCardViewModel", "Restored active task with start time $taskStartTime and total elapsed seconds $taskSeconds.")
//            }
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun completeTask(context: Context) {
//        if (!isTaskRunning) return
//
//        val taskEndTime = getCurrentTimeFormatted()
//        val taskDurationSeconds = taskSeconds
//        savedStateHandle["isTaskRunning"] = false
//        savedStateHandle["taskSeconds"] = 0L
//
//        viewModelScope.launch {
//            val activeTask = repository.getActiveTask()?.let { task ->
//                // Retrieve the expected duration (in minutes)
//                val expectedDuration = task.expectedDuration
//                // Calculate the over/under AET using the repository function
//                val overUnderAET = repository.calculateOverUnderAET(taskDurationSeconds, expectedDuration)
//
//                // Copy the task with updated fields
//                task.copy(
//                    endTime = taskEndTime,
//                    duration = taskDurationSeconds,
//                    secondsOverUnderAET = overUnderAET, // Save the over/under result
//                    isTaskRunning = false
//                )
//            }
//
//            // Update the task in the database if it exists
//            activeTask?.let {
//                repository.updateTimeEntry(it)
//            }
//
//            isTaskRunning = false
//            taskStartTime = null
//        }
//    }
//
////    @RequiresApi(Build.VERSION_CODES.O)
////    fun completeTask(context: Context) {
////        if (!isTaskRunning) return
////
////        val taskEndTime = getCurrentTimeFormatted()
////        val taskDurationSeconds = taskSeconds
////        savedStateHandle["isTaskRunning"] = false
////        savedStateHandle["taskSeconds"] = 0L
////
////        viewModelScope.launch {
////            val activeTask = repository.getActiveTask()?.copy(
////                endTime = taskEndTime,
////                duration = taskDurationSeconds,
////                isTaskRunning = false
////            )
////            activeTask?.let { repository.updateTimeEntry(it) }
////            isTaskRunning = false
////            taskStartTime = null
////        }
////    }
//
//    // Helper function to format a duration in HH:MM:SS
//    fun formatDuration(seconds: Long): String {
//        val hours = seconds / 3600
//        val minutes = (seconds % 3600) / 60
//        val secs = seconds % 60
//
//        return if (hours > 0) {
//            "%02d:%02d:%02d".format(hours, minutes, secs) // Display as HH:MM:SS if hours exist
//        } else {
//            "%02d:%02d".format(minutes, secs) // Display as MM:SS if hours are zero
//        }
//    }
//
//
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun getCurrentTimeFormattedWithoutSeconds(): String {
//        return LocalTime.now().format(formatterWithoutSeconds)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun getCurrentDateFormatted(): String = LocalDate.now().toString()
//
//
//
//        fun deleteAllSessions() {
//        viewModelScope.launch {
//            repository.deleteAllSessions()
//            updateTimeEntriesByDay()
//        }
//    }
//
//
//
//    fun deleteEntriesForDate(date: String) {
////        viewModelScope.launch {
////            repository.deleteEntriesForDate(date)
////            refreshTimeEntries() // Refresh after deletion
////        }
//    }
//
//    private fun refreshTimeEntries() {
////        viewModelScope.launch {
////            val entries = repository.getTimeEntriesByDay()
////            _timeEntriesByDay.value = entries // Update the mutable StateFlow
////        }
//    }
//
//
//    fun toggleSessionSubmission(session: Session) {
//        viewModelScope.launch {
//            Log.d("TimeCardViewModel", "Toggling session ID ${session.id}, current isSubmitted: ${session.isSubmitted}")
//
//            // Only toggle and update if the state needs to change
//            val updatedSession = session.copy(isSubmitted = session.isSubmitted)
//
//            // Save the updated session to the repository
//            repository.saveSession(updatedSession)
//
//            Log.d("TimeCardViewModel", "Updated session ID ${updatedSession.id}, new isSubmitted: ${updatedSession.isSubmitted}")
////
////            // Refresh the sessions from the database
//            updateTimeEntriesByDay()
//        }
//    }
//
//    fun updateTimeEntriesByDay() {
//        viewModelScope.launch {
//            repository.getAllSessions().collect { sessions ->
//                val groupedByDate = sessions.groupBy { it.date }
//                _timeEntriesByDay.value = groupedByDate
//            }
//        }
//    }
//
//}

