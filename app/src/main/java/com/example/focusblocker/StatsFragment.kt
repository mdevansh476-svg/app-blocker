package com.example.focusblocker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class StatsFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvTotalToday: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)
        listView = view.findViewById(R.id.lv_stats)
        pbLoading = view.findViewById(R.id.pb_loading_stats)
        tvTotalToday = view.findViewById(R.id.tv_total_screen_time)

        loadStats()
        return view
    }

    private fun loadStats() {
        val ctx = requireContext()

        thread {
            val pm = ctx.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val installedApps = pm.queryIntentActivities(mainIntent, 0)
            val helper = UsageStatsHelper(ctx)

            val statsList = installedApps.mapNotNull { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == ctx.packageName) null else helper.getAppUsageStats(pkg)
            }.filter { it.todayMillis > 0 || it.weeklyTotalMillis > 0 }
             .sortedByDescending { it.todayMillis }

            val grandTotalToday = statsList.sumOf { it.todayMillis }

            activity?.runOnUiThread {
                tvTotalToday.text = UsageStatsHelper.formatMillis(grandTotalToday)
                val adapter = AppStatsAdapter(ctx, statsList) { loadStats() }
                listView.adapter = adapter

                pbLoading.visibility = View.GONE
                listView.visibility = View.VISIBLE
            }
        }
    }
}
