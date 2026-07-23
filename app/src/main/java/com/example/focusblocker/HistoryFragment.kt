package com.example.focusblocker

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private lateinit var listView: ListView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        listView = view.findViewById(R.id.lv_history)
        return view
    }

    override fun onResume() {
        super.onResume()
        loadSessionsFromDatabase()
    }

    private fun loadSessionsFromDatabase() {
        val context = context ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sessionList = AppDatabase.getDatabase(context).sessionDao().getAllSessions()

                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        val adapter = HistoryAdapter(sessionList)
                        listView.adapter = adapter
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private class HistoryAdapter(private val sessions: List<FocusSession>) : BaseAdapter() {
        override fun getCount(): Int = if (sessions.isEmpty()) 1 else sessions.size

        override fun getItem(position: Int): Any = if (sessions.isEmpty()) "Empty" else sessions[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val context = parent.context
            
            if (sessions.isEmpty()) {
                return TextView(context).apply {
                    text = "No recorded focus sessions yet. Complete a session to populate logs!"
                    setTextColor(Color.parseColor("#94A3B8"))
                    setPadding(16, 32, 16, 32)
                    textSize = 14f
                }
            }

            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_history_session, parent, false)
            val session = sessions[position]

            val tvDate = view.findViewById<TextView>(R.id.tv_date)
            val tvStatus = view.findViewById<TextView>(R.id.tv_status)
            val tvDuration = view.findViewById<TextView>(R.id.tv_duration)

            val dateStr = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(session.timestamp))
            tvDate.text = dateStr

            if (session.isCompleted) {
                tvStatus.text = "Completed ✅"
                tvStatus.setTextColor(Color.parseColor("#10B981"))
            } else {
                tvStatus.text = "Stopped Early 🛑"
                tvStatus.setTextColor(Color.parseColor("#EF4444"))
            }

            // Convert total seconds into Minutes and Seconds format
            val minutes = session.durationSeconds / 60
            val seconds = session.durationSeconds % 60
            tvDuration.text = "Duration: ${minutes}m ${seconds}s"

            return view
        }
    }
}
