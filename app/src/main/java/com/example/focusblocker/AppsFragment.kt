package com.example.focusblocker

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

    private val socialKeywords = listOf("instagram", "facebook", "whatsapp", "snapchat", "tiktok", "twitter", "x", "reddit", "discord", "telegram")
    private val gameKeywords = listOf("game", "pubg", "roblox", "freefire", "clash", "candy", "subway", "minecraft")
    private val streamingKeywords = listOf("youtube", "netflix", "prime", "hulu", "twitch", "hotstar", "disney")

    private var masterAppList = mutableListOf<AppInfo>()
    private var displayedAppList = mutableListOf<AppInfo>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_apps, container, false)

        val listView = view.findViewById<ListView>(R.id.lv_apps)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pb_loading_apps)
        val btnSave = view.findViewById<Button>(R.id.btn_save_apps)
        val etSearch = view.findViewById<EditText>(R.id.et_search_apps)

        val btnSocial = view.findViewById<Button>(R.id.btn_group_social)
        val btnGames = view.findViewById<Button>(R.id.btn_group_games)
        val btnStreaming = view.findViewById<Button>(R.id.btn_group_streaming)

        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("FocusPrefs", Context.MODE_PRIVATE)
        val savedBlocked = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()

        thread {
            masterAppList = getInstalledUserApps().map {
                AppInfo(it.name, it.packageName, it.icon, savedBlocked.contains(it.packageName))
            }.toMutableList()

            sortAppsWithSelectedOnTop(masterAppList)
            displayedAppList.clear()
            displayedAppList.addAll(masterAppList)

            activity?.runOnUiThread {
                val adapter = AppListAdapter(ctx, displayedAppList)
                listView.adapter = adapter

                listView.setOnItemClickListener { _, _, position, _ ->
                    val clickedApp = displayedAppList[position]
                    clickedApp.isSelected = !clickedApp.isSelected

                    sortAppsWithSelectedOnTop(masterAppList)
                    filterList(etSearch.text.toString(), adapter)
                }

                etSearch.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        filterList(s.toString(), adapter)
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })

                btnSocial.setOnClickListener { toggleCategorySelection(socialKeywords, adapter) }
                btnGames.setOnClickListener { toggleCategorySelection(gameKeywords, adapter) }
                btnStreaming.setOnClickListener { toggleCategorySelection(streamingKeywords, adapter) }

                btnSave.setOnClickListener {
                    val selectedPackages = masterAppList.filter { it.isSelected }.map { it.packageName }.toSet()
                    prefs.edit().putStringSet("blocked_apps", selectedPackages).apply()
                    Toast.makeText(ctx, "Saved ${selectedPackages.size} blocked apps!", Toast.LENGTH_SHORT).show()
                }

                pbLoading.visibility = View.GONE
                listView.visibility = View.VISIBLE
            }
        }

        return view
    }

    private fun sortAppsWithSelectedOnTop(list: MutableList<AppInfo>) {
        list.sortWith(compareByDescending<AppInfo> { it.isSelected }.thenBy { it.name.lowercase() })
    }

    private fun filterList(query: String, adapter: AppListAdapter) {
        displayedAppList.clear()
        if (query.isEmpty()) {
            displayedAppList.addAll(masterAppList)
        } else {
            val q = query.lowercase()
            displayedAppList.addAll(masterAppList.filter {
                it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            })
        }
        adapter.notifyDataSetChanged()
    }

    private fun toggleCategorySelection(keywords: List<String>, adapter: AppListAdapter) {
        masterAppList.forEach { app ->
            val matches = keywords.any { kw ->
                app.packageName.lowercase().contains(kw) || app.name.lowercase().contains(kw)
            }
            if (matches) {
                app.isSelected = true
            }
        }
        sortAppsWithSelectedOnTop(masterAppList)
        displayedAppList.clear()
        displayedAppList.addAll(masterAppList)
        adapter.notifyDataSetChanged()
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
        }
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
