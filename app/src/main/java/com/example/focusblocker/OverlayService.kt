package com.example.focusblocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var isRunning = false

    private val CHANNEL_ID = "FocusBlockerChannel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            createNotificationChannel()
            
            // Using a built-in Android system icon to prevent XML resource crashes
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Focus Mode Active")
                .setContentText("App blocker is running.")
                .setSmallIcon(android.R.drawable.ic_menu_manage) 
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            startForeground(1, notification)

            if (!isRunning) {
                isRunning = true
                startAppMonitoring()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Focus Blocker Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun startAppMonitoring() {
        runnable = object : Runnable {
            override fun run() {
                try {
                    if (isBlockedAppInForeground()) {
                        showBlockOverlay()
                    } else {
                        hideBlockOverlay()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                if (isRunning) {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(runnable)
    }

    private fun isBlockedAppInForeground(): Boolean {
        val context = applicationContext
        val prefs = context.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        val blockedApps = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()

        if (blockedApps.isEmpty()) return false

        val currentApp = getForegroundPackageName(context)
        
        if (currentApp == packageName || currentApp.isEmpty()) return false

        return blockedApps.contains(currentApp)
    }

    private fun getForegroundPackageName(context: Context): String {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager 
            ?: return ""
        
        try {
            val time = System.currentTimeMillis()
            val usageEvents = usageStatsManager.queryEvents(time - 5000, time)
            val event = android.app.usage.UsageEvents.Event()
            
            var latestPackage = ""
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == 1) {
                    if (!event.packageName.isNullOrEmpty()) {
                        latestPackage = event.packageName
                    }
                }
            }
            return latestPackage
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private fun showBlockOverlay() {
        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_block_screen, null).apply {
            findViewById<Button>(R.id.btn_go_back)?.setOnClickListener {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
            }
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideBlockOverlay() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(runnable)
        hideBlockOverlay()
    }
}
