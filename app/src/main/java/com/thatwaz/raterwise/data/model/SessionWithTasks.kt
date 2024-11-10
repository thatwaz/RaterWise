package com.thatwaz.raterwise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_with_tasks")
data class SessionWithTasks(
    @PrimaryKey(autoGenerate = true) val sessionId: Int = 0,
    val date: String,
    val clockInTime: String,
    val clockOutTime: String? = null,
    val isClockedIn: Boolean = false,
    val totalWorkTime: Long = 0L, // Total work time in seconds
    val numberOfTasks: Int = 0, // Number of tasks worked on during the session
    val totalOverUnderAET: Long = 0L, // Sum of over/under AET for all tasks in seconds
    val isSubmitted: Boolean = false, // Indicates if the session has been submitted
    val isTaskRunning: Boolean = false, // Indicates if a task is currently active in the session
    val taskSeconds: Long = 0L, // Seconds elapsed for the current task
    val taskStartTime: String? = null // Start time of the current task
)


@Entity(tableName = "task_entries")
data class TaskEntry(
    @PrimaryKey(autoGenerate = true) val taskId: Int = 0,
    val sessionId: Int, // Foreign key reference to SessionWithTasks
    val startTime: String,
    val endTime: String? = null,
    val duration: Long = 0L, // Duration in seconds
    val expectedDuration: Int = 0, // Expected duration in minutes
    val secondsOverUnderAET: Long = 0L, // Track the over/under AET duration in seconds
    val date: String, // Add date field
    val isTaskRunning: Boolean = false // Indicates if a task is currently active
)



