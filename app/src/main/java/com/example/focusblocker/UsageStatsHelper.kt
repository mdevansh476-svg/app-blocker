package com.example.focusblocker

import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.drawable.Drawable
import java.util.Calendar

data class AppUsageData(
    val appName: String,
    val packageName: String,
    val appIcon: Drawable?,
    val todayMillis: Long,
    val yesterdayMillis: Long,
    val weeklyTotalMillis: Long,
    val dailyAvgMillis: Long,
    var dailyLimitMinutes: Int
)

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getAppUsageStats(packageName: String): AppUsageData {
        val today = getTodayUsageMillis(packageName)
        val yesterday = getYesterdayUsageMillis(packageName)
        val weeklyTotal = getWeeklyUsageMillis(packageName)
        val dailyAvg = weeklyTotal / 7

        val prefs = context.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        val limit = prefs.getInt("limit_$packageName", 0)

        var appName = packageName
        var icon: Drawable? = null

        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            appName = pm.getApplicationLabel(info).toString()
            icon = pm.getApplicationIcon(info)
        } catch (e: Exception) {
            // Ignored
        }

        return AppUsageData(appName, packageName, icon, today, yesterday, weeklyTotal, dailyAvg, limit)
    }

    fun getTodayUsageMillis(packageName: String): Long {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return queryUsageForRange(packageName, startOfToday, System.currentTimeMillis())
    }

    private fun getYesterdayUsageMillis(packageName: String): Long {
        val startOfYesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfYesterday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return queryUsageForRange(packageName, startOfYesterday, endOfYesterday)
    }

    private fun getWeeklyUsageMillis(packageName: String): Long {
        val startOf7DaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return queryUsageForRange(packageName, startOf7DaysAgo, System.currentTimeMillis())
    }

    private fun queryUsageForRange(packageName: String, startTime: Long, endTime: Long): Long {
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return 0L

        return stats.filter { it.packageName == packageName }
            .sumOf { it.totalTimeInForeground }
    }

    companion object {
        fun formatMillis(millis: Long): String {
            val totalMinutes = millis / 1000 / 60
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }
    }
}
