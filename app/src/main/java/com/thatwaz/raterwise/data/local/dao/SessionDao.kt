package com.thatwaz.raterwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thatwaz.raterwise.data.model.Session
import kotlinx.coroutines.flow.Flow


@Dao
interface SessionDao {

    // Get the current or active session
    @Query("SELECT * FROM session LIMIT 1")
    suspend fun getSession(): Session?

    // Save or update session information
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: Session)

    // Clear the session data
    @Query("DELETE FROM session")
    suspend fun clearSession()

    // Optionally, if you want to track multiple session entries
    @Query("SELECT * FROM session ORDER BY id DESC")
    fun getAllSessions(): Flow<List<Session>>
}


//@Dao
//interface SessionDao {
//
//    @Query("SELECT * FROM session LIMIT 1")
//    suspend fun getSession(): Session?
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun saveSession(session: Session)
//
//    @Query("DELETE FROM session")
//    suspend fun clearSession()
//}

