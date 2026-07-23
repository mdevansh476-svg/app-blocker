package com.example.focusblocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment

class MoreFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_more, container, false)
        val swOverlay = view.findViewById<Switch>(R.id.sw_overlay)

        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)

        swOverlay.isChecked = prefs.getBoolean("enable_overlay", true)

        swOverlay.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_overlay", isChecked).apply()
            if (!isChecked) {
                ctx.stopService(Intent(ctx, OverlayService::class.java))
            }
        }

        return view
    }
}
