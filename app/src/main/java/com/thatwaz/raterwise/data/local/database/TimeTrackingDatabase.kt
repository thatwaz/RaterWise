package com.thatwaz.raterwise.data.local.database


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.thatwaz.raterwise.data.local.Converters
import com.thatwaz.raterwise.data.local.dao.DailyWorkSummaryDao
import com.thatwaz.raterwise.data.local.dao.SessionDao
import com.thatwaz.raterwise.data.local.dao.TaskTimeTrackingDao
import com.thatwaz.raterwise.data.local.dao.WorkPeriodDao
import com.thatwaz.raterwise.data.model.DailyWorkSummary
import com.thatwaz.raterwise.data.model.Session
import com.thatwaz.raterwise.data.model.TaskTimeEntry
import com.thatwaz.raterwise.data.model.WorkPeriod


@Database(
    entities = [TaskTimeEntry::class, DailyWorkSummary::class, WorkPeriod::class, Session::class],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class) // Use the Converters class for handling complex types
abstract class TimeTrackingDatabase : RoomDatabase() {

    abstract fun taskTimeTrackingDao(): TaskTimeTrackingDao
    abstract fun sessionDao(): SessionDao
    abstract fun workPeriodDao(): WorkPeriodDao
    abstract fun dailyWorkSummaryDao(): DailyWorkSummaryDao
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

