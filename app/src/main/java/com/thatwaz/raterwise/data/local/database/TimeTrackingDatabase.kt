package com.thatwaz.raterwise.data.local.database


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.thatwaz.raterwise.data.local.Converters
import com.thatwaz.raterwise.data.local.dao.TimeTrackingDao
import com.thatwaz.raterwise.data.model.DailyWorkSummary
import com.thatwaz.raterwise.data.model.TimeEntry
import com.thatwaz.raterwise.data.model.WorkPeriod

@Database(
    entities = [TimeEntry::class, DailyWorkSummary::class, WorkPeriod::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class) // Use the Converters class for handling complex types
abstract class TimeTrackingDatabase : RoomDatabase() {
    abstract fun timeTrackingDao(): TimeTrackingDao
}

