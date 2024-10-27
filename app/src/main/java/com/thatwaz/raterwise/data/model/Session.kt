package com.thatwaz.raterwise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "session")
data class Session(
    @PrimaryKey val id: Int = 1, // Only one active session
    val clockInTime: String,
    val isClockedIn: Boolean,
    val totalWorkTime: Long = 0L, // Total time worked in current session
    val taskStartTime: String? = null // Track task start time, if applicable
)





