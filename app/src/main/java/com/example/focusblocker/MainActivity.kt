package com.example.focusblocker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 80, 60, 60)
        }

        val title = TextView(this).apply {
            text = "🛡️ Focus & App Blocker"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 40)
        }
        layout.addView(title)

        val status = TextView(this).apply {
            text = "Status: Tracking Active\nDaily Limit Left: 2 Hours 15 Mins\nActive Overlay: Enabled"
            textSize = 16f
            setPadding(0, 0, 0, 60)
        }
        layout.addView(status)

        val permButton = Button(this).apply {
            text = "Grant System Permissions"
            setOnClickListener {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }
        layout.addView(permButton)

        setContentView(layout)
    }
}
