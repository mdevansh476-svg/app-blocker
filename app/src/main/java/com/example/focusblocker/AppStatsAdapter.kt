package com.example.focusblocker

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.ArrayList

class AppStatsAdapter(
    private val context: Context,
    private val appList: List<AppUsageData>,
    private val onLimitChanged: () -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = appList.size
    override fun getItem(position: Int): Any = appList[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_app_stat, parent, false)
        val data = appList[position]

        val tvName = view.findViewById<TextView>(R.id.tv_app_name)
        val tvLimitStatus = view.findViewById<TextView>(R.id.tv_limit_status)
        val pbGraph = view.findViewById<ProgressBar>(R.id.pb_usage_graph)
        val btnSetLimit = view.findViewById<Button>(R.id.btn_set_limit)

        val tvToday = view.findViewById<TextView>(R.id.tv_today_usage)
        val tvYesterday = view.findViewById<TextView>(R.id.tv_yesterday_usage)
        val tvDailyAvg = view.findViewById<TextView>(R.id.tv_daily_avg)
        val tvWeeklyTotal = view.findViewById<TextView>(R.id.tv_weekly_total)

        tvName.text = data.appName
        tvToday.text = UsageStatsHelper.formatMillis(data.todayMillis)
        tvYesterday.text = UsageStatsHelper.formatMillis(data.yesterdayMillis)
        tvDailyAvg.text = UsageStatsHelper.formatMillis(data.dailyAvgMillis)
        tvWeeklyTotal.text = UsageStatsHelper.formatMillis(data.weeklyTotalMillis)

        // Set up Graph Progress Bar
        val todayMins = (data.todayMillis / 1000 / 60).toInt()
        if (data.dailyLimitMinutes > 0) {
            tvLimitStatus.text = "Daily Limit: $todayMins / ${data.dailyLimitMinutes} mins"
            val progressPercent = ((todayMins.toFloat() / data.dailyLimitMinutes) * 100).toInt()
            pbGraph.progress = progressPercent.coerceAtMost(100)
        } else {
            tvLimitStatus.text = "Daily Limit: Off"
            pbGraph.progress = (todayMins / 3).coerceAtMost(100) // Default visual relative scale
        }

        btnSetLimit.setOnClickListener {
            showSetLimitDialog(data)
        }

        return view
    }

    private fun showSetLimitDialog(data: AppUsageData) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Set Daily Limit for ${data.appName}")

        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Limit in minutes (e.g. 30, enter 0 to turn off)"
            if (data.dailyLimitMinutes > 0) setText(data.dailyLimitMinutes.toString())
        }
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val limitMinutes = input.text.toString().toIntOrNull() ?: 0
            val prefs = context.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("limit_${data.packageName}", limitMinutes).apply()

            data.dailyLimitMinutes = limitMinutes
            notifyDataSetChanged()
            onLimitChanged()
            Toast.makeText(context, "Saved limit for ${data.appName}", Toast.LENGTH_SHORT).show()
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}
