package com.thatwaz.raterwise.data.model



import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "work_periods")
data class WorkPeriod(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Unique ID for each period
    val startDate: String,
    val endDate: String,
    val totalHoursWorked: Long, // Total hours worked in the period
    val overtimeHours: Long = 0L,
    val isSubmittedForPayroll: Boolean = false,
    val dailySummaries: List<DailyWorkSummary> = emptyList(),
    val totalEarnings: Double = 0.0 // Total earnings for the period
)


