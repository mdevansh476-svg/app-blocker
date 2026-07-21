package com.example.focusblocker

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat

class FocusService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationMinutes = intent?.getIntExtra("DURATION_MINUTES", 25) ?: 25
        val durationMillis = durationMinutes * 60 * 1000L

        createNotificationChannel()
        val notification = createNotification("Focus Session Active", "Time remaining: $durationMinutes min")
        startForeground(1, notification)

        startFocusTimer(durationMillis)
        return START_STICKY
    }

    private fun startFocusTimer(durationMillis: Long) {
        countDownTimer?.cancel()
        isRunning = true

        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutesLeft = millisUntilFinished / 1000 / 60
                val secondsLeft = (millisUntilFinished / 1000) % 60
                val timeStr = String.format("%02d:%02d", minutesLeft, secondsLeft)

                // Update Notification
                val updateNotif = createNotification("Focus Session Active", "Remaining: $timeStr")
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, updateNotif)

                // Broadcast time to UI
                val broadcastIntent = Intent("FOCUS_TIMER_UPDATE")
                broadcastIntent.putExtra("TIME_LEFT", timeStr)
                sendBroadcast(broadcastIntent)
            }

            override fun onFinish() {
                isRunning = false
                stopForeground(true)
                stopSelf()
            }
        }.start()
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
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
