package com.example.focusblocker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SelectAppsActivity : AppCompatActivity() {

    private data class AppInfo(val name: String, val packageName: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_apps)

        val listView = findViewById<ListView>(R.id.lv_apps)
        val btnSave = findViewById<Button>(R.id.btn_save_apps)

        val installedApps = getInstalledLaunchableApps()
        val appNames = installedApps.map { it.name }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, appNames)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        val prefs = getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        val savedBlocked = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()

        // Pre-check currently blocked apps
        installedApps.forEachIndexed { index, appInfo ->
            if (savedBlocked.contains(appInfo.packageName)) {
                listView.setItemChecked(index, true)
            }
        }

        btnSave.setOnClickListener {
            val selectedPackages = mutableSetOf<String>()
            val checkedPositions = listView.checkedItemPositions

            for (i in 0 until installedApps.size) {
                if (checkedPositions.get(i)) {
                    selectedPackages.add(installedApps[i].packageName)
                }
            }

            prefs.edit().putStringSet("blocked_apps", selectedPackages).apply()
            Toast.makeText(this, "Saved ${selectedPackages.size} blocked apps!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getInstalledLaunchableApps(): List<AppInfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = packageManager.queryIntentActivities(mainIntent, 0)
        
        return apps.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == packageName) null // Skip Focus Builder itself
            else {
                val label = resolveInfo.loadLabel(packageManager).toString()
                AppInfo(label, pkg)
            }
        }.sortedBy { it.name.lowercase() }
    }
}
