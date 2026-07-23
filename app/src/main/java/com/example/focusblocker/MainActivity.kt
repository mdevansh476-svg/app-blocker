package com.example.focusblocker

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var navTabFocus: LinearLayout
    private lateinit var navTabApps: LinearLayout
    private lateinit var navTabStats: LinearLayout
    private lateinit var navTabHistory: LinearLayout
    private lateinit var navTabAbout: LinearLayout

    private lateinit var tvNavFocus: TextView
    private lateinit var tvNavApps: TextView
    private lateinit var tvNavStats: TextView
    private lateinit var tvNavHistory: TextView
    private lateinit var tvNavAbout: TextView

    // Cache fragments to prevent recreation crashes and memory overhead
    private val focusFragment = FocusFragment()
    private val appsFragment = AppsFragment()
    private val statsFragment = StatsFragment()
    private val historyFragment = HistoryFragment()
    private val moreFragment = MoreFragment()

    private var activeFragment: Fragment = focusFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start the background overlay & blocker service immediately
        startOverlayService()

        navTabFocus = findViewById(R.id.nav_tab_focus)
        navTabApps = findViewById(R.id.nav_tab_apps)
        navTabStats = findViewById(R.id.nav_tab_stats)
        navTabHistory = findViewById(R.id.nav_tab_history)
        navTabAbout = findViewById(R.id.nav_tab_about)

        tvNavFocus = findViewById(R.id.tv_nav_focus)
        tvNavApps = findViewById(R.id.tv_nav_apps)
        tvNavStats = findViewById(R.id.tv_nav_stats)
        tvNavHistory = findViewById(R.id.tv_nav_history)
        tvNavAbout = findViewById(R.id.tv_nav_about)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, focusFragment, "FOCUS")
                .add(R.id.fragment_container, appsFragment, "APPS").hide(appsFragment)
                .add(R.id.fragment_container, statsFragment, "STATS").hide(statsFragment)
                .add(R.id.fragment_container, historyFragment, "HISTORY").hide(historyFragment)
                .add(R.id.fragment_container, moreFragment, "MORE").hide(moreFragment)
                .commitAllowingStateLoss()
        }

        navTabFocus.setOnClickListener {
            switchFragment(focusFragment)
            highlightTab(navTabFocus, tvNavFocus)
        }

        navTabApps.setOnClickListener {
            switchFragment(appsFragment)
            highlightTab(navTabApps, tvNavApps)
        }

        navTabStats.setOnClickListener {
            switchFragment(statsFragment)
            highlightTab(navTabStats, tvNavStats)
        }

        navTabHistory.setOnClickListener {
            switchFragment(historyFragment)
            highlightTab(navTabHistory, tvNavHistory)
        }

        navTabAbout.setOnClickListener {
            switchFragment(moreFragment)
            highlightTab(navTabAbout, tvNavAbout)
        }
    }

    private fun startOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun switchFragment(target: Fragment) {
        if (activeFragment == target) return
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commitAllowingStateLoss()
        activeFragment = target
    }

    private fun highlightTab(activeLayout: LinearLayout, activeTextView: TextView) {
        val tabs = listOf(navTabFocus, navTabApps, navTabStats, navTabHistory, navTabAbout)
        val texts = listOf(tvNavFocus, tvNavApps, tvNavStats, tvNavHistory, tvNavAbout)

        tabs.forEach { it.setBackgroundColor(Color.TRANSPARENT) }
        texts.forEach { it.setTextColor(Color.parseColor("#94A3B8")) }

        activeLayout.setBackgroundResource(R.drawable.bg_nav_tab_active)
        activeTextView.setTextColor(Color.parseColor("#8B5CF6"))
    }
}
