package com.example.focusblocker

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import kotlin.concurrent.thread

class AppsFragment : Fragment() {

    data class AppInfo(
        val name: String,
        val packageName: String,
        val icon: Drawable?,
        var isSelected: Boolean = false
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_apps, container, false)

        val listView = view.findViewById<ListView>(R.id.lv_apps)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pb_loading_apps)
        val btnSave = view.findViewById<Button>(R.id.btn_save_apps)

        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        val savedBlocked = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()

        // Background thread loading eliminates tab-switch lag
        thread {
            val installedApps = getInstalledUserApps().map {
                AppInfo(it.name, it.packageName, it.icon, savedBlocked.contains(it.packageName))
            }.toMutableList()

            activity?.runOnUiThread {
                val adapter = AppListAdapter(ctx, installedApps)
                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->
                    installedApps[position].isSelected = !installedApps[position].isSelected
                    adapter.notifyDataSetChanged()
                }

                btnSave.setOnClickListener {
                    val selectedPackages = installedApps.filter { it.isSelected }.map { it.packageName }.toSet()
                    prefs.edit().putStringSet("blocked_apps", selectedPackages).apply()
                    Toast.makeText(ctx, "Saved ${selectedPackages.size} blocked apps!", Toast.LENGTH_SHORT).show()
                }

                pbLoading.visibility = View.GONE
                listView.visibility = View.VISIBLE
            }
        }

        return view
    }

    private fun getInstalledUserApps(): List<AppInfo> {
        val pm = requireContext().packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(mainIntent, 0)

        return apps.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == requireContext().packageName) null
            else {
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = try { resolveInfo.loadIcon(pm) } catch (e: Exception) { null }
                AppInfo(label, pkg, icon)
            }
        }.sortedBy { it.name.lowercase() }
    }

    private class AppListAdapter(
        context: Context,
        private val list: List<AppInfo>
    ) : ArrayAdapter<AppInfo>(context, 0, list) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_app_select, parent, false)
            val item = list[position]

            val tvName = view.findViewById<TextView>(R.id.tv_app_name)
            val ivIcon = view.findViewById<ImageView>(R.id.iv_app_icon)
            val cb = view.findViewById<CheckBox>(R.id.cb_select)

            tvName.text = item.name
            cb.isChecked = item.isSelected

            if (item.icon != null) ivIcon.setImageDrawable(item.icon)
            else ivIcon.setImageResource(R.mipmap.ic_launcher)

            return view
        }
    }
}
