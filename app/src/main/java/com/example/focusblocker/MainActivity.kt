package com.example.focusblocker

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var navTabFocus: LinearLayout
    private lateinit var navTabApps: LinearLayout
    private lateinit var navTabStats: LinearLayout
    private lateinit var navTabAbout: LinearLayout

    private lateinit var tvNavFocus: TextView
    private lateinit var tvNavApps: TextView
    private lateinit var tvNavStats: TextView
    private lateinit var tvNavAbout: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navTabFocus = findViewById(R.id.nav_tab_focus)
        navTabApps = findViewById(R.id.nav_tab_apps)
        navTabStats = findViewById(R.id.nav_tab_stats)
        navTabAbout = findViewById(R.id.nav_tab_about)

        tvNavFocus = findViewById(R.id.tv_nav_focus)
        tvNavApps = findViewById(R.id.tv_nav_apps)
        tvNavStats = findViewById(R.id.tv_nav_stats)
        tvNavAbout = findViewById(R.id.tv_nav_about)

        // Load default Focus fragment
        if (savedInstanceState == null) {
            replaceFragment(FocusFragment())
        }

        navTabFocus.setOnClickListener {
            replaceFragment(FocusFragment())
            highlightTab(navTabFocus, tvNavFocus)
        }

        navTabApps.setOnClickListener {
            replaceFragment(AppsFragment())
            highlightTab(navTabApps, tvNavApps)
        }

        navTabStats.setOnClickListener {
            replaceFragment(StatsFragment())
            highlightTab(navTabStats, tvNavStats)
        }

        navTabAbout.setOnClickListener {
            replaceFragment(AboutFragment())
            highlightTab(navTabAbout, tvNavAbout)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun highlightTab(activeLayout: LinearLayout, activeTextView: TextView) {
        val tabs = listOf(navTabFocus, navTabApps, navTabStats, navTabAbout)
        val texts = listOf(tvNavFocus, tvNavApps, tvNavStats, tvNavAbout)

        tabs.forEach { it.setBackgroundColor(Color.TRANSPARENT) }
        texts.forEach { it.setTextColor(Color.parseColor("#94A3B8")) }

        activeLayout.setBackgroundResource(R.drawable.bg_nav_tab_active)
        activeTextView.setTextColor(Color.parseColor("#8B5CF6"))
    }
}
