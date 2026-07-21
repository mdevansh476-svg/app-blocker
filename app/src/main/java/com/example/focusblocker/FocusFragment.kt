package com.example.focusblocker

import android.app.AppOpsManager
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class FocusFragment : Fragment() {

    private lateinit var btnCircleStart: RelativeLayout
    private lateinit var tvCircleText: TextView
    private lateinit var tvCustomTime: TextView
    private lateinit var statusDesc: TextView
    private lateinit var btnMinus5: Button
    private lateinit var btnPlus5: Button

    private var customMinutes = 25

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val timeRemaining = intent?.getStringExtra("TIME_LEFT") ?: return
            tvCustomTime.text = timeRemaining
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_focus, container, false)

        btnCircleStart = view.findViewById(R.id.btn_circle_start)
        tvCircleText = view.findViewById(R.id.tv_circle_button_text)
        tvCustomTime = view.findViewById(R.id.tv_custom_time)
        statusDesc = view.findViewById(R.id.status_desc)

        btnMinus5 = view.findViewById(R.id.btn_minus_5)
        btnPlus5 = view.findViewById(R.id.btn_plus_5)

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

        btnCircleStart.setOnClickListener { handleCircleButtonClick() }

        return view
    }

    override fun onResume() {
        super.onResume()
        val ctx = requireContext()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(timerReceiver, IntentFilter("FOCUS_TIMER_UPDATE"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            ctx.registerReceiver(timerReceiver, IntentFilter("FOCUS_TIMER_UPDATE"))
        }

        syncSessionUI()
    }

    private fun syncSessionUI() {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("is_session_active", false)
        val targetEndTime = prefs.getLong("target_end_time", 0L)
        val now = System.currentTimeMillis()

        if (isActive && targetEndTime > now) {
            val remainingMillis = targetEndTime - now
            val minutesLeft = remainingMillis / 1000 / 60
            val secondsLeft = (remainingMillis / 1000) % 60
            tvCustomTime.text = String.format("%02d:%02d", minutesLeft, secondsLeft)

            val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
            statusDesc.text = "Session active • ${blockedApps.size} apps locked"
            tvCircleText.text = "STOP\nSESSION"
            btnCircleStart.setBackgroundResource(R.drawable.bg_circle_button_active)

            btnMinus5.visibility = View.INVISIBLE
            btnPlus5.visibility = View.INVISIBLE
        } else {
            statusDesc.text = "Ready to lock down distractions"
            tvCircleText.text = "START\nFOCUS"
            btnCircleStart.setBackgroundResource(R.drawable.bg_circle_button)

            btnMinus5.visibility = View.VISIBLE
            btnPlus5.visibility = View.VISIBLE
            updateTimerDisplay()
        }
    }

    override fun onPause() {
        try {
            requireContext().unregisterReceiver(timerReceiver)
        } catch (e: Exception) { }
        super.onPause()
    }

    private fun updateTimerDisplay() {
        tvCustomTime.text = String.format("%02d:00", customMinutes)
    }

    private fun hasAllPermissions(): Boolean {
        val ctx = requireContext()
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(ctx)
        } else true

        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val mode = appOps?.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            ctx.packageName
        )
        val hasUsageStats = mode == AppOpsManager.MODE_ALLOWED

        return hasOverlay && hasUsageStats
    }

    private fun handleCircleButtonClick() {
        val ctx = requireContext()
        if (!hasAllPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(ctx)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                return
            }
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            val prefs = ctx.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
            val isActive = prefs.getBoolean("is_session_active", false)
            if (!isActive) startFocusSession() else stopFocusSession()
        }
    }

    private fun startFocusSession() {
        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()

        if (blockedApps.isEmpty()) {
            Toast.makeText(ctx, "Select at least 1 app in '📱 Apps' tab first!", Toast.LENGTH_LONG).show()
            return
        }

        val serviceIntent = Intent(ctx, FocusService::class.java).apply {
            putExtra("DURATION_MINUTES", customMinutes)
        }
        ContextCompat.startForegroundService(ctx, serviceIntent)
        syncSessionUI()
    }

    private fun stopFocusSession() {
        val ctx = requireContext()
        ctx.stopService(Intent(ctx, FocusService::class.java))

        val prefs = ctx.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_session_active", false)
            .putLong("target_end_time", 0L)
            .apply()

        syncSessionUI()
    }
}
