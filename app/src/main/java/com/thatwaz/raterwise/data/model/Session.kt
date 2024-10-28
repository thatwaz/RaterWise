package com.thatwaz.raterwise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "session")
data class Session(
    @PrimaryKey val id: Int = 1, // Only one active session
    val clockInTime: String,
    val isClockedIn: Boolean,
    val totalWorkTime: Long = 0L,
    val numberOfTasks: Int = 0, // New field to store the number of tasks
    val totalOverUnderAET: Int = 0 // New field to store the sum of over/under AET for all tasks
)






