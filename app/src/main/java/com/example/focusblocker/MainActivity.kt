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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnGrant: Button
    private lateinit var btnSelectApps: Button
    private lateinit var btnUsageAnalytics: Button
    private lateinit var statusTitle: TextView
    private lateinit var statusDesc: TextView
    private lateinit var timerControls: LinearLayout
    private lateinit var tvCountdown: TextView

    private lateinit var btn15m: Button
    private lateinit var btn25m: Button
    private lateinit var btn45m: Button

    private var selectedMinutes = 25
    private var isSessionActive = false

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val timeRemaining = intent?.getStringExtra("TIME_LEFT") ?: return
            tvCountdown.text = timeRemaining
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGrant = findViewById(R.id.btn_grant_permissions)
        btnSelectApps = findViewById(R.id.btn_select_apps)
        btnUsageAnalytics = findViewById(R.id.btn_usage_analytics)
        statusTitle = findViewById(R.id.status_title)
        statusDesc = findViewById(R.id.status_desc)
        timerControls = findViewById(R.id.timer_controls)
        tvCountdown = findViewById(R.id.tv_countdown)

        btn15m = findViewById(R.id.btn_15m)
        btn25m = findViewById(R.id.btn_25m)
        btn45m = findViewById(R.id.btn_45m)

        btn15m.setOnClickListener { selectDuration(15) }
        btn25m.setOnClickListener { selectDuration(25) }
        btn45m.setOnClickListener { selectDuration(45) }

        btnSelectApps.setOnClickListener {
            startActivity(Intent(this, SelectAppsActivity::class.java))
        }

        btnUsageAnalytics.setOnClickListener {
            startActivity(Intent(this, UsageStatsActivity::class.java))
        }

        btnGrant.setOnClickListener { handleButtonClick() }

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

    private fun selectDuration(mins: Int) {
        selectedMinutes = mins
        tvCountdown.text = String.format("%02d:00", mins)
        Toast.makeText(this, "Set to $mins minutes", Toast.LENGTH_SHORT).show()
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

    private fun handleButtonClick() {
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
            Toast.makeText(this, "Please select at least 1 app to block first!", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SelectAppsActivity::class.java))
            return
        }

        isSessionActive = true
        statusTitle.text = "Focus Session Running 🎯"
        statusDesc.text = "Stay focused! ${blockedApps.size} apps are locked."
        btnGrant.text = "End Focus Session"
        btnSelectApps.visibility = View.GONE
        btnUsageAnalytics.visibility = View.GONE
        timerControls.visibility = View.GONE
        tvCountdown.visibility = View.VISIBLE

        val serviceIntent = Intent(this, FocusService::class.java)
        serviceIntent.putExtra("DURATION_MINUTES", selectedMinutes)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopFocusSession() {
        isSessionActive = false
        stopService(Intent(this, FocusService::class.java))
        showReadyState()
    }

    private fun showReadyState() {
        statusTitle.text = "Focus Builder Active 🚀"
        statusDesc.text = "Select apps and duration to start focusing:"
        btnGrant.text = "Start Focus Session"
        btnSelectApps.visibility = View.VISIBLE
        btnUsageAnalytics.visibility = View.VISIBLE
        timerControls.visibility = View.VISIBLE
        tvCountdown.visibility = View.VISIBLE
        tvCountdown.text = String.format("%02d:00", selectedMinutes)
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
