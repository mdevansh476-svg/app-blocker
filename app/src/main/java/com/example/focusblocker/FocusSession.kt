package com.example.focusblocker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val durationSeconds: Int, // Stores exact total seconds (e.g., 125 seconds = 2m 5s)
    val isCompleted: Boolean
)
