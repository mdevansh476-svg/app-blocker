package com.example.focusblocker

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

class StreakHelper(private val context: Context) {

    private val prefs = context.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun recordCompletedSession() {
        val todayStr = dateFormat.format(Date())
        val lastActiveDateStr = prefs.getString("last_active_date", "")
        var currentStreak = prefs.getInt("current_streak", 0)

        if (lastActiveDateStr == todayStr) {
            return
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = dateFormat.format(calendar.time)

        if (lastActiveDateStr == yesterdayStr) {
            currentStreak += 1
        } else if (lastActiveDateStr.isNullOrEmpty()) {
            currentStreak = 1
        } else {
            currentStreak = 1
        }

        val bestStreak = prefs.getInt("best_streak", 0)
        val newBest = if (currentStreak > bestStreak) currentStreak else bestStreak

        prefs.edit()
            .putString("last_active_date", todayStr)
            .putInt("current_streak", currentStreak)
            .putInt("best_streak", newBest)
            .apply()
    }

    fun getCurrentStreak(): Int {
        val lastActiveDateStr = prefs.getString("last_active_date", "")
        val currentStreak = prefs.getInt("current_streak", 0)

        if (lastActiveDateStr.isNullOrEmpty()) return 0

        val todayStr = dateFormat.format(Date())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = dateFormat.format(calendar.time)

        if (lastActiveDateStr != todayStr && lastActiveDateStr != yesterdayStr) {
            prefs.edit().putInt("current_streak", 0).apply()
            return 0
        }

        return currentStreak
    }

    fun getBestStreak(): Int {
        return prefs.getInt("best_streak", 0)
    }
}
