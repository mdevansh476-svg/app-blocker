package com.example.focusblocker

import android.app.Service
import android.content.*
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.TextView
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var layoutParams: WindowManager.LayoutParams

    private lateinit var tvBubbleTimer: TextView
    private lateinit var tvExpandedTimer: TextView
    private lateinit var layoutBubble: View
    private lateinit var layoutExpanded: View
    private lateinit var btnPause: Button
    private lateinit var btnMinimize: Button
    private lateinit var btnClose: Button

    private var isPaused = false

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val time = intent?.getStringExtra("TIME_LEFT") ?: return
            if (time == "PAUSED") {
                isPaused = true
                btnPause.text = "Resume"
                tvBubbleTimer.text = "PAUSED"
                tvExpandedTimer.text = "PAUSED"
            } else {
                if (isPaused && time != "PAUSED") {
                    isPaused = false
                    btnPause.text = "Pause"
                }
                tvBubbleTimer.text = time
                tvExpandedTimer.text = time
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        tvBubbleTimer = floatingView.findViewById(R.id.tv_overlay_timer)
        tvExpandedTimer = floatingView.findViewById(R.id.tv_expanded_timer)
        layoutBubble = floatingView.findViewById(R.id.layout_bubble)
        layoutExpanded = floatingView.findViewById(R.id.layout_expanded)
        btnPause = floatingView.findViewById(R.id.btn_overlay_pause)
        btnMinimize = floatingView.findViewById(R.id.btn_overlay_minimize)
        btnClose = floatingView.findViewById(R.id.btn_overlay_close)

        setupDragAndTouch()

        btnPause.setOnClickListener {
            val action = if (isPaused) "RESUME" else "PAUSE"
            sendBroadcast(Intent("FOCUS_SESSION_ACTION").putExtra("ACTION", action))
        }

        btnMinimize.setOnClickListener {
            layoutExpanded.visibility = View.GONE
            layoutBubble.visibility = View.VISIBLE
        }

        btnClose.setOnClickListener {
            stopSelf()
        }

        windowManager.addView(floatingView, layoutParams)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerReceiver, IntentFilter("FOCUS_TIMER_UPDATE"), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(timerReceiver, IntentFilter("FOCUS_TIMER_UPDATE"))
        }
    }

    private fun setupDragAndTouch() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        layoutBubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = abs(event.rawX - initialTouchX)
                    val diffY = abs(event.rawY - initialTouchY)
                    if (diffX < 10 && diffY < 10) {
                        layoutBubble.visibility = View.GONE
                        layoutExpanded.visibility = View.VISIBLE
                    } else {
                        val screenWidth = resources.displayMetrics.widthPixels
                        layoutParams.x = if (layoutParams.x < screenWidth / 2) 10 else screenWidth - floatingView.width - 10
                        windowManager.updateViewLayout(floatingView, layoutParams)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(timerReceiver) } catch (e: Exception) {}
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
