package com.example.focusblocker

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FocusService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private var blockedApps = setOf<String>()
    private lateinit var statsHelper: UsageStatsHelper

    private val appCheckRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                checkForegroundAppAndBlock()
                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationMinutes = intent?.getIntExtra("DURATION_MINUTES", 25) ?: 25
        val durationMillis = durationMinutes * 60 * 1000L
        val targetEndTime = System.currentTimeMillis() + durationMillis

        val prefs = getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_session_active", true)
            .putLong("target_end_time", targetEndTime)
            .apply()

        statsHelper = UsageStatsHelper(this)
        blockedApps = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()

        createNotificationChannel()
        val notification = createNotification("Focus Active & Enforcing Limits", "Monitoring app usage...")
        startForeground(1, notification)

        startFocusTimer(durationMillis)
        isRunning = true
        handler.post(appCheckRunnable)

        return START_STICKY
    }

    private fun checkForegroundAppAndBlock() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val time = System.currentTimeMillis()
        val usageEvents = usageStatsManager.queryEvents(time - 2000, time)
        val event = UsageEvents.Event()

        var currentForegroundApp: String? = null
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundApp = event.packageName
            }
        }

        if (currentForegroundApp != null) {
            val prefs = getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
            val dailyLimitMinutes = prefs.getInt("limit_$currentForegroundApp", 0)

            var shouldBlock = blockedApps.contains(currentForegroundApp)

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
                Toast.makeText(this, "Focus Builder: App limit reached or blocked!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startFocusTimer(durationMillis: Long) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutesLeft = millisUntilFinished / 1000 / 60
                val secondsLeft = (millisUntilFinished / 1000) % 60
                val timeStr = String.format("%02d:%02d", minutesLeft, secondsLeft)

                val updateNotif = createNotification("Focus Active", "Remaining: $timeStr")
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, updateNotif)

                val broadcastIntent = Intent("FOCUS_TIMER_UPDATE")
                broadcastIntent.putExtra("TIME_LEFT", timeStr)
                sendBroadcast(broadcastIntent)
            }

            override fun onFinish() {
                cleanUpSessionState()
                stopForeground(true)
                stopSelf()
            }
        }.start()
    }

    private fun cleanUpSessionState() {
        isRunning = false
        handler.removeCallbacks(appCheckRunnable)
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
                "Focus Builder Sessions",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        cleanUpSessionState()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
