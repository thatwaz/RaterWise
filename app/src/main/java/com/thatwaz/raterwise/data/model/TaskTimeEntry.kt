package com.thatwaz.raterwise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "task_time_entries")
data class TaskTimeEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: String,
    val endTime: String,
    val duration: Int, // Duration in minutes
    val date: String, // Date for the task entry
    val isSubmitted: Boolean,
    val expectedDuration: Int, // Expected duration (AET)
    val isOverUnderAET: Boolean, // Over or under AET indicator
    val minutesOverUnderAET: Int, // Track the over/under AET duration
    val isTaskRunning: Boolean = false // Indicates if a task is running
)








