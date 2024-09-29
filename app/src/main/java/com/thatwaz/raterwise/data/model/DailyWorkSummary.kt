package com.thatwaz.raterwise.data.model




import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.thatwaz.raterwise.data.local.Converters

@Entity(tableName = "daily_work_summaries")
@TypeConverters(Converters::class)
data class DailyWorkSummary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Unique ID for each daily summary
    val date: String,
    val timeWorked: Long, // Time worked in minutes
    val isSubmitted: Boolean,
    val entries: List<TimeEntry> = emptyList(), // Entries for that day
    val totalEarnings: Double = 0.0 // Earnings calculated based on hourly wage
)


