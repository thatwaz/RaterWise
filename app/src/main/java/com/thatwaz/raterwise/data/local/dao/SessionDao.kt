package com.thatwaz.raterwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import kotlinx.coroutines.flow.Flow


//@Dao
//interface SessionDao {
//
////    // Get the current or active session (if not yet submitted)
////    @Query("SELECT * FROM session WHERE isSubmitted = 0 LIMIT 1")
////    suspend fun getActiveSession(): Session?
//
//    @Query("SELECT * FROM session WHERE isClockedIn = 1 ORDER BY id DESC LIMIT 1")
//    suspend fun getActiveSession(): Session?
//
//
//    // Save or update session information
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun saveSession(session: Session)
//
//    // Clear all session data (this can be used to delete all previous sessions)
//    @Query("DELETE FROM session")
//    suspend fun clearSession()
//
//    // Get all submitted sessions
//    @Query("SELECT * FROM session WHERE isSubmitted = 1 ORDER BY id DESC")
//    fun getAllSubmittedSessions(): Flow<List<Session>>
//
//    // Get all active and previous sessions (if needed for history)
//    @Query("SELECT * FROM session ORDER BY id DESC")
//    fun getAllSessions(): Flow<List<Session>>
//
//    // Delete all session data
//    @Query("DELETE FROM session")
//    suspend fun deleteAllSessions()
//}


//@Dao
//interface SessionDao {
//
//    // Get the current or active session
//    @Query("SELECT * FROM session LIMIT 1")
//    suspend fun getSession(): Session?
//
//    // Save or update session information
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun saveSession(session: Session)
//
//    // Clear the session data
//    @Query("DELETE FROM session")
//    suspend fun clearSession()
//
//    // Optionally, if you want to track multiple session entries
//    @Query("SELECT * FROM session ORDER BY id DESC")
//    fun getAllSessions(): Flow<List<Session>>
//}


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

