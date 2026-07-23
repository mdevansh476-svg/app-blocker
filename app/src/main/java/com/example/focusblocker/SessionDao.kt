package com.example.focusblocker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: FocusSession)

    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessions(): List<FocusSession>
}
