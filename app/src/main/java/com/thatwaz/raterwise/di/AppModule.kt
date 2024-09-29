package com.thatwaz.raterwise.di


import android.content.Context
import androidx.room.Room
import com.thatwaz.raterwise.data.local.dao.TimeTrackingDao
import com.thatwaz.raterwise.data.local.database.TimeTrackingDatabase
import com.thatwaz.raterwise.data.repository.TimeTrackingRepository
import com.thatwaz.raterwise.data.repository.TimeTrackingRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    // Bind the implementation to the interface using `@Binds`
    @Binds
    @Singleton
    abstract fun bindTimeTrackingRepository(
        impl: TimeTrackingRepositoryImpl
    ): TimeTrackingRepository

    companion object {
        // Provide the Room Database instance
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): TimeTrackingDatabase {
            return Room.databaseBuilder(
                context,
                TimeTrackingDatabase::class.java,
                "time_tracking_db"
            ).build()
        }

        // Provide the TimeTrackingDao from the database instance
        @Provides
        @Singleton
        fun provideTimeTrackingDao(database: TimeTrackingDatabase): TimeTrackingDao {
            return database.timeTrackingDao()
        }
    }
}

