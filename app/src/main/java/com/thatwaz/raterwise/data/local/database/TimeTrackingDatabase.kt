package com.thatwaz.raterwise.data.local.database


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.thatwaz.raterwise.data.local.Converters
import com.thatwaz.raterwise.data.local.dao.TimeTrackingDao
import com.thatwaz.raterwise.data.model.SessionWithTasks
import com.thatwaz.raterwise.data.model.TaskEntry

@Database(
    entities = [SessionWithTasks::class, TaskEntry::class],
    version = 14,
    exportSchema = false
)
@TypeConverters(Converters::class) // Attach converters here
abstract class TimeTrackingDatabase : RoomDatabase() {

    abstract fun timeTrackingDao(): TimeTrackingDao
}



//@Database(
//    entities = [TaskTimeEntry::class, DailyWorkSummary::class, WorkPeriod::class, Session::class],
//    version = 4,
//    exportSchema = false
//)
//@TypeConverters(Converters::class) // Use the Converters class for handling complex types
//abstract class TimeTrackingDatabase : RoomDatabase() {
//    abstract fun timeTrackingDao(): TaskTimeTrackingDao
//    abstract fun sessionDao(): SessionDao
//}

