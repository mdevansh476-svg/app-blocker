package com.example.focusblocker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnGrant: Button
    private lateinit var statusTitle: TextView
    private lateinit var statusDesc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGrant = findViewById(R.id.btn_grant_permissions)
        statusTitle = findViewById(R.id.status_title)
        statusDesc = findViewById(R.id.status_desc)

        btnGrant.setOnClickListener {
            handleButtonClick()
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions every time you return to the app!
        if (hasAllPermissions()) {
            showReadyState()
        }
    }

    private fun hasAllPermissions(): Boolean {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
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
            // 1. Request Overlay Permission if missing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please enable 'Display over other apps'", Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return
            }

            // 2. Request Usage Access Permission if missing
            if (!checkUsageStatsPermission()) {
                Toast.makeText(this, "Please allow 'Usage Access' for Focus Builder", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "Focus Session Started!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showReadyState() {
        statusTitle.text = "Focus Builder Active 🚀"
        statusDesc.text = "All necessary permissions are granted. Ready to lock down distractions!"
        btnGrant.text = "Start Focus Session"
    }
}
