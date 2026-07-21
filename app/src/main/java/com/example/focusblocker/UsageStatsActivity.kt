package com.example.focusblocker

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class UsageStatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_stats)

        loadStats()
    }

    private fun loadStats() {
        val listView = findViewById<ListView>(R.id.lv_stats)
        val tvTotalToday = findViewById<TextView>(R.id.tv_total_screen_time)

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val installedApps = packageManager.queryIntentActivities(mainIntent, 0)
        val helper = UsageStatsHelper(this)

        val statsList = installedApps.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == packageName) null
            else helper.getAppUsageStats(pkg)
        }.filter { it.todayMillis > 0 || it.weeklyTotalMillis > 0 }
         .sortedByDescending { it.todayMillis }

        val grandTotalToday = statsList.sumOf { it.todayMillis }
        tvTotalToday.text = UsageStatsHelper.formatMillis(grandTotalToday)

        val adapter = AppStatsAdapter(this, statsList) {
            loadStats()
        }
        listView.adapter = adapter
    }
}
