package com.example.focusblocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocusService : Service() {

    private val CHANNEL_ID = "FocusTimerChannel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationMinutes = intent?.getIntExtra("DURATION_MINUTES", 25) ?: 25
        val totalSeconds = durationMinutes * 60

        createNotificationChannel()
        
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Session Running")
            .setContentText("Stay focused! Timer is active.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(2, notification)

        // Example: If session completes or stops, log it to the database with totalSeconds
        // logFocusSession(totalSeconds, isCompleted = true)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Focus Timer Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun logFocusSession(durationSecs: Int, isCompleted: Boolean) {
        val session = FocusSession(
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSecs, // Fixed to match the new database schema
            isCompleted = isCompleted
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getDatabase(applicationContext).sessionDao().insert(session)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
