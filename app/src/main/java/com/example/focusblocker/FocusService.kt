package com.example.focusblocker

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FocusService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var isSessionActive = false
    private var isPaused = false
    private var millisRemaining = 0L

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var statsHelper: UsageStatsHelper

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra("ACTION")) {
                "PAUSE" -> pauseSession()
                "RESUME" -> resumeSession()
            }
        }
    }

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (!isPaused) {
                checkForegroundAppAndEnforceLimits()
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        statsHelper = UsageStatsHelper(this)
        createNotificationChannel()
        startForeground(1, createNotification("Focus Protection Active", "Monitoring limits and sessions..."))

        val filter = IntentFilter("FOCUS_SESSION_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(controlReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(controlReceiver, filter)
        }

        handler.post(monitorRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationMinutes = intent?.getIntExtra("DURATION_MINUTES", 0) ?: 0

        if (durationMinutes > 0) {
            millisRemaining = durationMinutes * 60 * 1000L
            val targetEndTime = System.currentTimeMillis() + millisRemaining

            val prefs = getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_session_active", true)
                .putLong("target_end_time", targetEndTime)
                .apply()

            isSessionActive = true
            isPaused = false

            if (prefs.getBoolean("enable_overlay", true)) {
                startService(Intent(this, OverlayService::class.java))
            }

            startFocusTimer(millisRemaining)
        }

        return START_STICKY
    }

    private fun pauseSession() {
        isPaused = true
        countDownTimer?.cancel()
        val broadcastIntent = Intent("FOCUS_TIMER_UPDATE")
        broadcastIntent.putExtra("TIME_LEFT", "PAUSED")
        sendBroadcast(broadcastIntent)
    }

    private fun resumeSession() {
        if (isPaused && millisRemaining > 0) {
            isPaused = false
            startFocusTimer(millisRemaining)
        }
    }

    private fun checkForegroundAppAndEnforceLimits() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val now = System.currentTimeMillis()
        val usageEvents = usageStatsManager.queryEvents(now - 3000, now)
        val event = UsageEvents.Event()

        var currentForegroundApp: String? = null
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundApp = event.packageName
            }
        }

        if (currentForegroundApp != null && currentForegroundApp != packageName) {
            val prefs = getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
            val blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
            val dailyLimitMinutes = prefs.getInt("limit_$currentForegroundApp", 0)

            var shouldBlock = false

            if (isSessionActive && blockedApps.contains(currentForegroundApp)) {
                shouldBlock = true
            }

            if (!shouldBlock && dailyLimitMinutes > 0) {
                val todayMillis = statsHelper.getTodayUsageMillis(currentForegroundApp)
                val todayMinutes = (todayMillis / 1000 / 60).toInt()
                if (todayMinutes >= dailyLimitMinutes) {
                    shouldBlock = true
                }
            }

            if (shouldBlock) {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                Toast.makeText(this, "Focus Builder: Access blocked!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startFocusTimer(durationMillis: Long) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (isPaused) return
                millisRemaining = millisUntilFinished

                val minutesLeft = millisUntilFinished / 1000 / 60
                val secondsLeft = (millisUntilFinished / 1000) % 60
                val timeStr = String.format("%02d:%02d", minutesLeft, secondsLeft)

                val broadcastIntent = Intent("FOCUS_TIMER_UPDATE")
                broadcastIntent.putExtra("TIME_LEFT", timeStr)
                sendBroadcast(broadcastIntent)
            }

            override fun onFinish() {
                stopFocusSession()
            }
        }.start()
    }

    private fun stopFocusSession() {
        isSessionActive = false
        isPaused = false
        stopService(Intent(this, OverlayService::class.java))

        val prefs = getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_session_active", false)
            .putLong("target_end_time", 0L)
            .apply()
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, "focus_channel")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "focus_channel",
                "Focus Builder Protection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(controlReceiver) } catch (e: Exception) {}
        countDownTimer?.cancel()
        handler.removeCallbacks(monitorRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
