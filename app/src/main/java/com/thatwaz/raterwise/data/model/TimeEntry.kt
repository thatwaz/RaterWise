package com.thatwaz.raterwise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_entries")
data class TimeEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: String,
    val endTime: String,
    val duration: Int, // Duration in minutes
    val date: String, // Date for the time entry
    val isSubmitted: Boolean,
    val expectedDuration: Int, // Expected duration (AET)
    val isOverUnderAET: Boolean, // Over or under AET
    val minutesOverUnderAET: Int // New field to track the over/under AET
)







