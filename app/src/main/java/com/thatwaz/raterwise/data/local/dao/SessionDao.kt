package com.thatwaz.raterwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thatwaz.raterwise.data.model.Session

@Dao
interface SessionDao {

    @Query("SELECT * FROM session LIMIT 1")
    suspend fun getSession(): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: Session)

    @Query("DELETE FROM session")
    suspend fun clearSession()
}

