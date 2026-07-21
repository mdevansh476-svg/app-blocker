package com.example.focusblocker

import android.app.AppOpsManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnCircleStart: RelativeLayout
    private lateinit var tvCircleText: TextView
    private lateinit var tvCustomTime: TextView
    private lateinit var statusDesc: TextView

    private lateinit var btnMinus5: Button
    private lateinit var btnPlus5: Button

    private lateinit var navTabFocus: LinearLayout
    private lateinit var navTabApps: LinearLayout
    private lateinit var navTabStats: LinearLayout

    private var customMinutes = 25
    private var isSessionActive = false

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val timeRemaining = intent?.getStringExtra("TIME_LEFT") ?: return
            tvCustomTime.text = timeRemaining
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCircleStart = findViewById(R.id.btn_circle_start)
        tvCircleText = findViewById(R.id.tv_circle_button_text)
        tvCustomTime = findViewById(R.id.tv_custom_time)
        statusDesc = findViewById(R.id.status_desc)

        btnMinus5 = findViewById(R.id.btn_minus_5)
        btnPlus5 = findViewById(R.id.btn_plus_5)

        navTabFocus = findViewById(R.id.nav_tab_focus)
        navTabApps = findViewById(R.id.nav_tab_apps)
        navTabStats = findViewById(R.id.nav_tab_stats)

        btnMinus5.setOnClickListener {
            if (customMinutes > 5) {
                customMinutes -= 5
                updateTimerDisplay()
            }
        }

        btnPlus5.setOnClickListener {
            if (customMinutes < 300) {
                customMinutes += 5
                updateTimerDisplay()
            }
        }

        // Bottom Navigation Tab Actions
        navTabApps.setOnClickListener {
            startActivity(Intent(this, SelectAppsActivity::class.java))
        }

        navTabStats.setOnClickListener {
            startActivity(Intent(this, UsageStatsActivity::class.java))
        }

        navTabFocus.setOnClickListener {
            Toast.makeText(this, "You're already on the Focus timer screen", Toast.LENGTH_SHORT).show()
        }

        btnCircleStart.setOnClickListener { handleCircleButtonClick() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerReceiver, IntentFilter("FOCUS_TIMER_UPDATE"), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(timerReceiver, IntentFilter("FOCUS_TIMER_UPDATE"))
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasAllPermissions() && !isSessionActive) {
            showReadyState()
        }
    }

    private fun updateTimerDisplay() {
        tvCustomTime.text = String.format("%02d:00", customMinutes)
    }

    private fun hasAllPermissions(): Boolean {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        val hasUsageStats = checkUsageStatsPermission()
        return hasOverlay && hasUsageStats
    }

    private fun checkUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun handleCircleButtonClick() {
        if (!hasAllPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                return
            }
            if (!checkUsageStatsPermission()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        } else {
            if (!isSessionActive) {
                startFocusSession()
            } else {
                stopFocusSession()
            }
        }
    }

    private fun startFocusSession() {
        val prefs = getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()

        if (blockedApps.isEmpty()) {
            Toast.makeText(this, "Select at least 1 app in '📱 Apps' tab first!", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SelectAppsActivity::class.java))
            return
        }

        isSessionActive = true
        statusDesc.text = "Session active • ${blockedApps.size} apps locked"
        tvCircleText.text = "STOP\nSESSION"
        btnCircleStart.setBackgroundResource(R.drawable.bg_circle_button_active)

        btnMinus5.visibility = View.INVISIBLE
        btnPlus5.visibility = View.INVISIBLE

        val serviceIntent = Intent(this, FocusService::class.java)
        serviceIntent.putExtra("DURATION_MINUTES", customMinutes)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopFocusSession() {
        isSessionActive = false
        stopService(Intent(this, FocusService::class.java))
        showReadyState()
    }

    private fun showReadyState() {
        statusDesc.text = "Ready to lock down distractions"
        tvCircleText.text = "START\nFOCUS"
        btnCircleStart.setBackgroundResource(R.drawable.bg_circle_button)

        btnMinus5.visibility = View.VISIBLE
        btnPlus5.visibility = View.VISIBLE
        updateTimerDisplay()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(timerReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        super.onDestroy()
    }
}
