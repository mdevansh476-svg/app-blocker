package com.example.focusblocker

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment

class HistoryFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        val listView = view.findViewById<ListView>(R.id.lv_history)

        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        val historySet = prefs.getStringSet("session_database_history", emptySet()) ?: emptySet()

        val historyList = if (historySet.isEmpty()) {
            listOf("No recorded focus sessions yet. Complete a session to populate logs!")
        } else {
            historySet.toList().sortedDescending()
        }

        val adapter = ArrayAdapter(ctx, android.R.layout.simple_list_item_1, historyList)
        listView.adapter = adapter

        return view
    }
}
