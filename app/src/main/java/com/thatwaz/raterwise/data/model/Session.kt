package com.thatwaz.raterwise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "session")
data class Session(
    @PrimaryKey val id: Int = 1, // Ensure only one active session
    val clockInTime: String,
    val isClockedIn: Boolean,
    val taskStartTime: String? = null,
    val totalWorkTime: Long = 0
)



