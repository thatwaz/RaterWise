package com.thatwaz.raterwise.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_entries")
data class TimeEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Primary key for the TimeEntry
    val startTime: String,
    val endTime: String,
    val duration: Int, // Duration in minutes
    val date: String, // Add this date field to store the entry's date
    val isSubmitted: Boolean,
    val expectedDuration: Int, // Expected duration (AET) in minutes
    val isOverUnderAET: Boolean // Indicates whether the actual time is over or under the expected time
)






