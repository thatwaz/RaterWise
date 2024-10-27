package com.thatwaz.raterwise.di

import android.content.Context
import androidx.room.Room
import com.thatwaz.raterwise.data.local.dao.DailyWorkSummaryDao
import com.thatwaz.raterwise.data.local.dao.SessionDao
import com.thatwaz.raterwise.data.local.dao.TaskTimeTrackingDao
import com.thatwaz.raterwise.data.local.dao.WorkPeriodDao
import com.thatwaz.raterwise.data.local.database.TimeTrackingDatabase
import com.thatwaz.raterwise.data.repository.TimeTrackingRepository
import com.thatwaz.raterwise.data.repository.TimeTrackingRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTimeTrackingRepository(
        taskTimeTrackingDao: TaskTimeTrackingDao,
        sessionDao: SessionDao,
        workPeriodDao: WorkPeriodDao,
        dailyWorkSummaryDao: DailyWorkSummaryDao
    ): TimeTrackingRepository {
        return TimeTrackingRepositoryImpl(
            taskTimeTrackingDao = taskTimeTrackingDao,
            sessionDao = sessionDao,
            workPeriodDao = workPeriodDao,
            dailyWorkSummaryDao = dailyWorkSummaryDao
        )
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TimeTrackingDatabase {
        return Room.databaseBuilder(
            context,
            TimeTrackingDatabase::class.java,
            "time_tracking_db"
        )
            .fallbackToDestructiveMigration() // Enable destructive migration
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskTimeTrackingDao(database: TimeTrackingDatabase): TaskTimeTrackingDao {
        return database.taskTimeTrackingDao()
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: TimeTrackingDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun provideWorkPeriodDao(database: TimeTrackingDatabase): WorkPeriodDao {
        return database.workPeriodDao()
    }

    @Provides
    @Singleton
    fun provideDailyWorkSummaryDao(database: TimeTrackingDatabase): DailyWorkSummaryDao {
        return database.dailyWorkSummaryDao()
    }
}

//@Module
//@InstallIn(SingletonComponent::class)
//object AppModule {
//
//    @Provides
//    @Singleton
//    fun provideTimeTrackingRepository(
//        taskTimeTrackingDao: TaskTimeTrackingDao,
//        sessionDao: SessionDao
//    ): TimeTrackingRepository {
//        return TimeTrackingRepositoryImpl(taskTimeTrackingDao, sessionDao)
//    }
//
//    @Provides
//    @Singleton
//    fun provideDatabase(@ApplicationContext context: Context): TimeTrackingDatabase {
//        return Room.databaseBuilder(
//            context,
//            TimeTrackingDatabase::class.java,
//            "time_tracking_db"
//        )
//            .fallbackToDestructiveMigration() // Enable destructive migration
//            .build()
//    }
//
//    @Provides
//    @Singleton
//    fun provideTimeTrackingDao(database: TimeTrackingDatabase): TaskTimeTrackingDao {
//        return database.timeTrackingDao()
//    }
//
//    @Provides
//    @Singleton
//    fun provideSessionDao(database: TimeTrackingDatabase): SessionDao {
//        return database.sessionDao()
//    }
//}


//@Module
//@InstallIn(SingletonComponent::class)
//object AppModule {
//
//    @Provides
//    @Singleton
//    fun provideTimeTrackingRepository(
//        timeTrackingDao: TimeTrackingDao,
//        sessionDao: SessionDao
//    ): TimeTrackingRepository = TimeTrackingRepositoryImpl(
//        timeTrackingDao = timeTrackingDao,
//        sessionDao = sessionDao
//    )
//
//    @Provides
//    @Singleton
//    fun provideDatabase(@ApplicationContext context: Context): TimeTrackingDatabase {
//        return Room.databaseBuilder(
//            context,
//            TimeTrackingDatabase::class.java,
//            "time_tracking_db"
//        ).build()
//    }
//
//    @Provides
//    @Singleton
//    fun provideTimeTrackingDao(database: TimeTrackingDatabase): TimeTrackingDao {
//        return database.timeTrackingDao()
//    }
//
//    @Provides
//    @Singleton
//    fun provideSessionDao(database: TimeTrackingDatabase): SessionDao {
//        return database.sessionDao()
//    }
//}
