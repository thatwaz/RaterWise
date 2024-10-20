package com.thatwaz.raterwise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "session")
data class Session(
    @PrimaryKey val id: Int = 1, // Only one active session
    val clockInTime: String,
    val isClockedIn: Boolean,
    val taskStartTime: String? = null, // Track task start time
    val totalWorkTime: Long = 0L, // Total time worked in current session
    val isTaskRunning: Boolean = false // Indicates if a task is running
)




