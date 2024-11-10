package com.thatwaz.raterwise.data.repository

import android.util.Log
import com.thatwaz.raterwise.data.local.dao.TimeTrackingDao
import com.thatwaz.raterwise.data.model.SessionWithTasks
import com.thatwaz.raterwise.data.model.SessionWithTasksAndEntries
import com.thatwaz.raterwise.data.model.TaskEntry
import javax.inject.Inject


class TimeTrackingRepositoryImpl @Inject constructor(
    private val timeTrackingDao: TimeTrackingDao
) : TimeTrackingRepository {

    override suspend fun insertSessionWithTasks(session: SessionWithTasks): Long {
        return timeTrackingDao.insertSessionWithTasks(session)
    }
    override suspend fun getMostRecentSession(): SessionWithTasks? {
        val mostRecentSession = timeTrackingDao.getMostRecentSession()

        // Log details of the most recent session
        mostRecentSession?.let {
            Log.d("TimeTrackingRepository", "Most recent session retrieved: sessionId=${it.sessionId}, date=${it.date}, clockInTime=${it.clockInTime}, clockOutTime=${it.clockOutTime}, isClockedIn=${it.isClockedIn}, isTaskRunning=${it.isTaskRunning}, taskSeconds=${it.taskSeconds}, taskStartTime=${it.taskStartTime}")
        } ?: Log.d("TimeTrackingRepository", "No recent session found.")

        return mostRecentSession
    }

    override suspend fun getSessionById(sessionId: Long): SessionWithTasks? {
        return timeTrackingDao.getSessionById(sessionId)
    }

    override suspend fun getSessionWithTasksById(sessionId: Long): SessionWithTasks? {
        return timeTrackingDao.getSessionWithTasksById(sessionId)
    }


    override suspend fun updateSessionWithTasks(session: SessionWithTasks) {
        Log.d("TimeTrackingRepository", "Updating session: sessionId=${session.sessionId}, date=${session.date}, clockInTime=${session.clockInTime}, clockOutTime=${session.clockOutTime}, isClockedIn=${session.isClockedIn}, isTaskRunning=${session.isTaskRunning}, taskSeconds=${session.taskSeconds}, taskStartTime=${session.taskStartTime}, numberOfTasks=${session.numberOfTasks}, totalOverUnderAET=${session.totalOverUnderAET}, isSubmitted=${session.isSubmitted}")

        timeTrackingDao.updateSessionWithTasks(session)
    }


    override suspend fun getAllSessions(): List<SessionWithTasks> {
        return timeTrackingDao.getAllSessions()
    }

    override suspend fun insertTaskEntry(taskEntry: TaskEntry) {
        timeTrackingDao.insertTaskEntry(taskEntry)
    }

    override suspend fun updateTaskEntry(taskEntry: TaskEntry) {
        timeTrackingDao.updateTaskEntry(taskEntry) // Ensure your DAO has an equivalent update method
    }
    override suspend fun getSessionsWithTasksInDateRange(startDate: String, endDate: String): List<SessionWithTasksAndEntries> {
        Log.d("TimeTrackingRepository", "Fetching sessions from $startDate to $endDate")

        val sessions = timeTrackingDao.getSessionsWithTasksInDateRange(startDate, endDate)

        Log.d("TimeTrackingRepository", "Retrieved ${sessions.size} sessions between $startDate and $endDate")


        return sessions
    }

    override suspend fun getSessionsGroupedByDate(): Map<String, List<SessionWithTasks>> {
        val sessions = timeTrackingDao.getAllSessions()
        return sessions.groupBy { it.date }
    }

    override suspend fun getTaskEntriesBySessionId(sessionId: Int): List<TaskEntry> {
        return timeTrackingDao.getTaskEntriesBySessionId(sessionId)
    }

    override fun calculateOverUnderAET(duration: Long, expectedDuration: Int): Long {
        return duration - expectedDuration * 60L // Convert minutes to seconds
    }

    override suspend fun deleteAllSessions() {
        timeTrackingDao.deleteAllSessions()
    }
}


//class TimeTrackingRepositoryImpl @Inject constructor(
//    private val timeTrackingDao: TimeTrackingDao
//) : TimeTrackingRepository {
//
//    override suspend fun insertSessionWithTasks(session: SessionWithTasks): Long {
//        return timeTrackingDao.insertSessionWithTasks(session)
//    }
//
//    override suspend fun getSessionWithTasksById(sessionId: Int): SessionWithTasks? {
//        return timeTrackingDao.getSessionWithTasksById(sessionId)
//    }
//    override suspend fun insertTaskEntry(taskEntry: TaskEntry) {
//        timeTrackingDao.insertTaskEntry(taskEntry)
//    }
////    override suspend fun getSessionsWithTasksInDateRange(startDate: String, endDate: String): List<SessionWithTasksAndEntries> {
////        return timeTrackingDao.getSessionsWithTasksInDateRange(startDate, endDate)
////    }
//
//    override suspend fun getSessionsWithTasksInDateRange(startDate: String, endDate: String): List<SessionWithTasksAndEntries> {
//        val sessionsWithTasks = timeTrackingDao.getSessionsWithTasksInDateRange(startDate, endDate)
//
//        // Log the input parameters and the result
//        Log.d("TimeTrackingRepository", "Fetching sessions with tasks from $startDate to $endDate. Found ${sessionsWithTasks.size} sessions.")
//        sessionsWithTasks.forEach { sessionWithTasks ->
//            Log.d("TimeTrackingRepository", "Session: ${sessionWithTasks.session}, Tasks: ${sessionWithTasks.taskEntries}")
//        }
//
//        return sessionsWithTasks
//    }
//
//    override suspend fun updateSessionWithTasks(session: SessionWithTasks) {
//        timeTrackingDao.updateSessionWithTasks(session)
//    }
//
//    override suspend fun getAllSessions(): List<SessionWithTasks> {
//        return timeTrackingDao.getAllSessions()
//    }
//
//    override suspend fun getSessionsGroupedByDate(): Map<String, List<SessionWithTasks>> {
//        val sessions = timeTrackingDao.getAllSessions()
//        return sessions.groupBy { it.date }
//    }
//
//
//    override suspend fun getTaskEntriesBySessionId(sessionId: Int): List<TaskEntry> {
//        return timeTrackingDao.getTaskEntriesBySessionId(sessionId)
//    }
//
//    override fun calculateOverUnderAET(duration: Long, expectedDuration: Int): Long {
//        val expectedDurationInSeconds = expectedDuration * 60L // Convert expected duration from minutes to seconds
//        return duration - expectedDurationInSeconds
//    }
//
//    override suspend fun deleteAllSessions() {
//        // Delete all task entries first to avoid foreign key constraint issues
//        timeTrackingDao.deleteAllTaskEntries()
//
//        // Then delete all sessions
//        timeTrackingDao.deleteAllSessions()
//    }
//}



//class TimeTrackingRepositoryImpl @Inject constructor(
//    private val taskTimeTrackingDao: TaskTimeTrackingDao, // Handles task-related operations
//    private val sessionDao: SessionDao, // Handles session-related operations
//    private val dailyWorkSummaryDao: DailyWorkSummaryDao, // Handles daily summaries
//    private val workPeriodDao: WorkPeriodDao // Handles work periods
//) : TimeTrackingRepository {
//
//    // TaskTimeEntry Operations
//    override fun getTimeEntriesByDate(date: String): Flow<List<TaskTimeEntry>> {
//        Log.d("TimeTrackingRepo", "Fetching task time entries for date: $date")
//        return taskTimeTrackingDao.getTaskTimeEntriesByDate(date)
//    }
//
//    override fun getAllTimeEntries(): Flow<List<TaskTimeEntry>> =
//        taskTimeTrackingDao.getAllTaskTimeEntries()
//
//    override suspend fun insertTaskTimeEntry(taskTimeEntry: TaskTimeEntry) {
//        Log.d("TimeTrackingRepo", "Inserting TaskTimeEntry: $taskTimeEntry")
//        val insertedId = taskTimeTrackingDao.insertTaskTimeEntry(taskTimeEntry)
//        Log.d("TimeTrackingRepo", "Inserted TaskTimeEntry with ID: $insertedId")
//    }
//
//    override suspend fun updateTimeEntry(taskTimeEntry: TaskTimeEntry) {
//        Log.d("TimeTrackingRepo", "Attempting to update task entry in database: $taskTimeEntry")
//        taskTimeTrackingDao.updateTaskTimeEntry(taskTimeEntry)
//        Log.d("TimeTrackingRepo", "Task entry updated successfully.")
//    }
//
//    override suspend fun deleteEntriesForDate(date: String) {
//        taskTimeTrackingDao.deleteEntriesForDate(date)
//    }
//
////    override suspend fun getTimeEntriesByDay(): Map<String, List<TaskTimeEntry>> {
////        return taskTimeTrackingDao.getTimeEntriesByDay()
////    }
//
//    override suspend fun getSessionsGroupedByDate(): Map<String, List<Session>> {
//        // Retrieve sessions grouped by date from DAO
//        return taskTimeTrackingDao.getSessions().groupBy { it.date }
//    }
//
//    override suspend fun getTaskEntriesBySessionId(sessionId: Int): List<TaskTimeEntry> {
//        // Retrieve task entries for a specific session ID
//        return taskTimeTrackingDao.getTasksForSession(sessionId)
//    }
//
//
//    override suspend fun deleteAllTimeEntries() {
//        Log.d("TimeTrackingRepo", "Deleting all TaskTimeEntries")
//        taskTimeTrackingDao.deleteAllTaskTimeEntries()
//    }
//
//    override suspend fun deleteAllSessions() {
//        Log.d("TimeTrackingRepo", "Deleting all sessions")
//        sessionDao.deleteAllSessions()
//    }
//
//    // DailyWorkSummary Operations
//    override fun getDailySummary(date: String): Flow<DailyWorkSummary> {
//        Log.d("TimeTrackingRepo", "Fetching daily summary for date: $date")
//        return dailyWorkSummaryDao.getDailyWorkSummary(date)
//    }
//
//    override suspend fun insertDailyWorkSummary(summary: DailyWorkSummary) {
//        Log.d("TimeTrackingRepo", "Inserting DailyWorkSummary for date: ${summary.date}")
//        dailyWorkSummaryDao.insertDailyWorkSummary(summary)
//    }
//
//    // WorkPeriod operations
//    override fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod> {
//        Log.d("TimeTrackingRepo", "Fetching WorkPeriod from $startDate to $endDate")
//        return workPeriodDao.getWorkPeriod(startDate, endDate)
//    }
//
//    override suspend fun insertWorkPeriod(workPeriod: WorkPeriod) {
//        Log.d("TimeTrackingRepo", "Inserting WorkPeriod: $workPeriod")
//        workPeriodDao.insertWorkPeriod(workPeriod)
//    }
//
//    override fun getAllSessions(): Flow<List<Session>> {
//        return sessionDao.getAllSessions()
//    }
//
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod> = flow {
//        val today = LocalDate.now()
//        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1) // Monday
//        val endOfWeek = startOfWeek.plusDays(6) // Sunday
//
//        val currentWorkPeriod = workPeriodDao
//            .getWorkPeriod(startOfWeek.toString(), endOfWeek.toString())
//            .first()
//        emit(currentWorkPeriod)
//    }
//
//    // Session Operations
//    override suspend fun getActiveSession(): Session? {
//        val activeSession = sessionDao.getActiveSession()
//        Log.d("TimeTrackingRepo", "Fetched active session: $activeSession")
//        return activeSession
//    }
//
//
//    override suspend fun saveSession(session: Session) {
//        Log.d("TimeTrackingRepo", "Saving session: $session")
//        sessionDao.saveSession(session)
//    }
//
//    override suspend fun clearSession() {
//        Log.d("TimeTrackingRepo", "Clearing session")
//        sessionDao.clearSession()
//    }
//
//    override fun calculateOverUnderAET(duration: Long, expectedDuration: Int): Long {
//        val expectedDurationInSeconds = expectedDuration * 60L // Convert expected duration from minutes to seconds
//        return duration - expectedDurationInSeconds
//    }
//
//    override suspend fun getActiveTask(): TaskTimeEntry? {
//        // Directly return the active task from the DAO
//        return taskTimeTrackingDao.getActiveTask()
//    }
//}


//class TimeTrackingRepositoryImpl @Inject constructor(
//    private val taskTimeTrackingDao: TaskTimeTrackingDao, // Handles task-related operations
//    private val sessionDao: SessionDao, // Handles session-related operations
//    private val dailyWorkSummaryDao: DailyWorkSummaryDao, // Handles daily summaries
//    private val workPeriodDao: WorkPeriodDao // Handles work periods
//) : TimeTrackingRepository {
//
//    // TaskTimeEntry Operations
//    override fun getTimeEntriesByDate(date: String): Flow<List<TaskTimeEntry>> {
//        Log.d("TimeTrackingRepo", "Fetching task time entries for date: $date")
//        return taskTimeTrackingDao.getTaskTimeEntriesByDate(date)
//    }
//
//    override fun getAllTimeEntries(): Flow<List<TaskTimeEntry>> =
//        taskTimeTrackingDao.getAllTaskTimeEntries()
//
//    override suspend fun insertTaskTimeEntry(taskTimeEntry: TaskTimeEntry) {
//        Log.d("TimeTrackingRepo", "Inserting TaskTimeEntry: $taskTimeEntry")
//        val insertedId = taskTimeTrackingDao.insertTaskTimeEntry(taskTimeEntry)
//        Log.d("TimeTrackingRepo", "Inserted TaskTimeEntry with ID: $insertedId")
//    }
//
//    override suspend fun submitTimeEntry(date: String, entry: TaskTimeEntry) {
//        val updatedEntry = entry.copy(isSubmitted = true)
//        Log.d("TimeTrackingRepo", "Submitting TaskTimeEntry for date: $date, ID: ${entry.id}")
//        taskTimeTrackingDao.updateTaskTimeEntry(updatedEntry)
//    }
//
//    override suspend fun updateTimeEntry(taskTimeEntry: TaskTimeEntry) {
//        Log.d("TimeTrackingRepo", "Attempting to update task entry in database: $taskTimeEntry")
//        taskTimeTrackingDao.updateTaskTimeEntry(taskTimeEntry)
//        Log.d("TimeTrackingRepo", "Task entry updated successfully.")
//    }
//
//
////    override suspend fun updateTimeEntry(taskTimeEntry: TaskTimeEntry) {
////        Log.d("TimeTrackingRepo", "Updating TaskTimeEntry: ID = ${taskTimeEntry.id}")
////        taskTimeTrackingDao.updateTaskTimeEntry(taskTimeEntry)
////    }
//
//    override suspend fun deleteAllTimeEntries() {
//        Log.d("TimeTrackingRepo", "Deleting all TaskTimeEntries")
//        taskTimeTrackingDao.deleteAllTaskTimeEntries()
//    }
//
//    // DailyWorkSummary Operations
//    // DailyWorkSummary operations
//    override fun getDailySummary(date: String): Flow<DailyWorkSummary> {
//        Log.d("TimeTrackingRepo", "Fetching daily summary for date: $date")
//        return dailyWorkSummaryDao.getDailyWorkSummary(date)
//    }
//
//    override suspend fun insertDailyWorkSummary(summary: DailyWorkSummary) {
//        Log.d("TimeTrackingRepo", "Inserting DailyWorkSummary for date: ${summary.date}")
//        dailyWorkSummaryDao.insertDailyWorkSummary(summary)
//    }
//
//    // WorkPeriod operations
//    override fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod> {
//        Log.d("TimeTrackingRepo", "Fetching WorkPeriod from $startDate to $endDate")
//        return workPeriodDao.getWorkPeriod(startDate, endDate)
//    }
//
//    override suspend fun insertWorkPeriod(workPeriod: WorkPeriod) {
//        Log.d("TimeTrackingRepo", "Inserting WorkPeriod: $workPeriod")
//        workPeriodDao.insertWorkPeriod(workPeriod)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod> = flow {
//        val today = LocalDate.now()
//        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1) // Monday
//        val endOfWeek = startOfWeek.plusDays(6) // Sunday
//
//        val currentWorkPeriod = workPeriodDao
//            .getWorkPeriod(startOfWeek.toString(), endOfWeek.toString())
//            .first()
//        emit(currentWorkPeriod)
//    }
//
//    // Session Operations
//    override suspend fun getSession(): Session? {
//        val session = sessionDao.getSession()
//        Log.d("TimeTrackingRepo", "Fetched session: $session")
//        return session
//    }
//
//    override suspend fun saveSession(session: Session) {
//        Log.d("TimeTrackingRepo", "Saving session: $session")
//        sessionDao.saveSession(session)
//    }
//
//    override suspend fun clearSession() {
//        Log.d("TimeTrackingRepo", "Clearing session")
//        sessionDao.clearSession()
//    }
//
//    override fun calculateOverUnderAET(duration: Long, expectedDuration: Int): Long {
//        val expectedDurationInSeconds = expectedDuration * 60L // Convert expected duration from minutes to seconds
//        return duration - expectedDurationInSeconds
//    }
//
//
//    override suspend fun getActiveTask(): TaskTimeEntry? {
//        // Directly return the active task from the DAO
//        return taskTimeTrackingDao.getActiveTask()
//    }
//
//
//}


//class TimeTrackingRepositoryImpl @Inject constructor(
//    private val taskTimeTrackingDao: TaskTimeTrackingDao,  // For time entries and summaries
//    private val sessionDao: SessionDao  // For session management
//) : TimeTrackingRepository {
//
//    // TimeEntry operations
//    override fun getTimeEntriesByDate(date: String): Flow<List<TaskTimeEntry>> {
//        Log.d("TimeTrackingRepo", "Fetching time entries for date: $date")
//        return taskTimeTrackingDao.getTimeEntriesByDate(date)
//    }
//
//    override fun getAllTimeEntries(): Flow<List<TaskTimeEntry>> =
//        taskTimeTrackingDao.getAllTimeEntries()
//    override suspend fun insertTimeEntry(taskTimeEntry: TaskTimeEntry) {
//        Log.d(
//            "TimeTrackingRepo",
//            "Inserting TimeEntry: Start: ${taskTimeEntry.startTime}, End: ${taskTimeEntry.endTime}, Date: ${taskTimeEntry.date}"
//        )
//        val insertedId = taskTimeTrackingDao.insertTimeEntry(taskTimeEntry) // Returns Long
//        Log.d("TimeTrackingRepo", "Inserted TimeEntry with ID: $insertedId")
//    }
//
//    override suspend fun submitTimeEntry(date: String, entry: TaskTimeEntry) {
//        val updatedEntry = entry.copy(isSubmitted = true)
//        Log.d("TimeTrackingRepo", "Submitting TimeEntry for date: $date, ID: ${entry.id}")
//        taskTimeTrackingDao.updateTimeEntry(updatedEntry)
//    }
//
//    override suspend fun updateTimeEntry(taskTimeEntry: TaskTimeEntry) {
//        taskTimeTrackingDao.updateTimeEntry(taskTimeEntry)
//    }
//
//    override suspend fun deleteAllTimeEntries() {
//        taskTimeTrackingDao.deleteAllTimeEntries()
//    }
//
//    // DailyWorkSummary operations
//    override fun getDailySummary(date: String): Flow<DailyWorkSummary> {
//        Log.d("TimeTrackingRepo", "Fetching daily summary for date: $date")
//        return taskTimeTrackingDao.getDailyWorkSummary(date)
//    }
//
//    override suspend fun insertDailyWorkSummary(summary: DailyWorkSummary) {
//        Log.d("TimeTrackingRepo", "Inserting DailyWorkSummary for date: ${summary.date}")
//        taskTimeTrackingDao.insertDailyWorkSummary(summary)
//    }
//
//    // WorkPeriod operations
//    override fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod> {
//        return taskTimeTrackingDao.getWorkPeriod(startDate, endDate)
//    }
//
//    override suspend fun insertWorkPeriod(workPeriod: WorkPeriod) {
//        taskTimeTrackingDao.insertWorkPeriod(workPeriod)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod> = flow {
//        val today = LocalDate.now()
//        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)  // Monday
//        val endOfWeek = startOfWeek.plusDays(6)  // Sunday
//
//        val currentWorkPeriod = taskTimeTrackingDao
//            .getWorkPeriod(startOfWeek.toString(), endOfWeek.toString())
//            .first()
//        emit(currentWorkPeriod)
//    }
//
//    // Session operations
//    override suspend fun getSession(): Session? {
//        val session = sessionDao.getSession()
//        Log.d("TimeTrackingRepo", "Fetched session: $session")
//        return session
//    }
//
//    override suspend fun saveSession(session: Session) {
//        Log.d("TimeTrackingRepo", "Saving session: $session")
//        sessionDao.saveSession(session)
//    }
//
//    override suspend fun clearSession() {
//        Log.d("TimeTrackingRepo", "Clearing session")
//        sessionDao.clearSession()
//    }
//
//    override fun calculateOverUnderAET(duration: Int, expectedDuration: Int): Int {
//        return duration - expectedDuration
//    }
//}


//class TimeTrackingRepositoryImpl @Inject constructor(
//    private val timeTrackingDao: TimeTrackingDao, // For TimeEntry and related operations
//    private val sessionDao: SessionDao // For Session operations
//) : TimeTrackingRepository {
//
//    // TimeEntry operations
//    override fun getTimeEntriesByDate(date: String): Flow<List<TimeEntry>> {
//        Log.d("TimeTrackingRepo", "Fetching time entries for date: $date")
//        return timeTrackingDao.getTimeEntriesByDate(date)
//    }
//
//    override fun getAllTimeEntries(): Flow<List<TimeEntry>> = timeTrackingDao.getAllTimeEntries()
//
//    override suspend fun insertTimeEntry(timeEntry: TimeEntry) {
//        Log.d(
//            "TimeTrackingRepo",
//            "Inserting TimeEntry: Start: ${timeEntry.startTime}, End: ${timeEntry.endTime}, Date: ${timeEntry.date}"
//        )
//        timeTrackingDao.insertTimeEntry(timeEntry)
//        Log.d("TimeTrackingRepo", "Inserted TimeEntry with ID: ${timeEntry.id}")
//    }
//
//    override suspend fun updateTimeEntry(timeEntry: TimeEntry) {
//        timeTrackingDao.updateTimeEntry(timeEntry)
//    }
//
//    override suspend fun deleteAllTimeEntries() {
//        timeTrackingDao.deleteAllTimeEntries()
//    }
//
//    // DailyWorkSummary operations
//    override fun getDailySummary(date: String): Flow<DailyWorkSummary> {
//        Log.d("TimeTrackingRepo", "Fetching daily summary for date: $date")
//        return timeTrackingDao.getDailyWorkSummary(date)
//    }
//
//    override suspend fun insertDailyWorkSummary(summary: DailyWorkSummary) {
//        Log.d("TimeTrackingRepo", "Inserting DailyWorkSummary for date: ${summary.date}")
//        timeTrackingDao.insertDailyWorkSummary(summary)
//        Log.d("TimeTrackingRepo", "Inserted DailyWorkSummary with ID: ${summary.id}")
//    }
//
//    // WorkPeriod operations
//    override fun getWorkPeriod(startDate: String, endDate: String): Flow<WorkPeriod> {
//        Log.d("TimeTrackingRepo", "Fetching work period from $startDate to $endDate")
//        return timeTrackingDao.getWorkPeriod(startDate, endDate)
//    }
//
//    override suspend fun insertWorkPeriod(workPeriod: WorkPeriod) {
//        Log.d("TimeTrackingRepo", "Inserting WorkPeriod: Start: ${workPeriod.startDate}, End: ${workPeriod.endDate}")
//        timeTrackingDao.insertWorkPeriod(workPeriod)
//        Log.d("TimeTrackingRepo", "Inserted WorkPeriod with ID: ${workPeriod.id}")
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override suspend fun getCurrentWorkPeriod(): Flow<WorkPeriod> {
//        val today = LocalDate.now()
//        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1) // Monday
//        val endOfWeek = startOfWeek.plusDays(6) // Sunday
//
//        Log.d("TimeTrackingRepo", "Calculating current work period: $startOfWeek to $endOfWeek")
//
//        return flow {
//            val currentWorkPeriod = timeTrackingDao.getWorkPeriod(
//                startOfWeek.toString(), endOfWeek.toString()
//            ).first()
//            Log.d("TimeTrackingRepo", "Current work period retrieved: $currentWorkPeriod")
//            emit(currentWorkPeriod)
//        }
//    }
//
//    override suspend fun submitTimeEntry(date: String, entry: TimeEntry) {
//        val updatedEntry = entry.copy(isSubmitted = true)
//        Log.d("TimeTrackingRepo", "Submitting TimeEntry for date: $date, ID: ${entry.id}")
//        timeTrackingDao.updateTimeEntry(updatedEntry)
//        Log.d(
//            "TimeTrackingRepo",
//            "TimeEntry submitted: ID: ${updatedEntry.id}, Start: ${updatedEntry.startTime}, End: ${updatedEntry.endTime}"
//        )
//    }
//
//    // Session operations
//    override suspend fun getSession(): Session? {
//        val session = sessionDao.getSession()
//        Log.d("TimeTrackingRepo", "Fetched session: $session")
//        return session
//    }
//
//    override suspend fun saveSession(session: Session) {
//        Log.d("TimeTrackingRepo", "Saving session: $session")
//        sessionDao.saveSession(session)
//    }
//
//    override suspend fun clearSession() {
//        Log.d("TimeTrackingRepo", "Clearing session")
//        sessionDao.clearSession()
//    }
//}






