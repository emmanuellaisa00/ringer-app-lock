package com.example.ringer

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ringer.databinding.ActivityAppSelectionBinding

data class AppItem(
    val packageName: String,
    val appName: String,
    val appInfo: ApplicationInfo
)

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSelectionBinding
    private lateinit var adapter: AppPickerAdapter
    private lateinit var allApps: List<AppItem>
    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        // Query ALL installed apps that have a launcher intent (user-facing apps)
        allApps = loadAllApps()

        adapter = AppPickerAdapter(allApps, packageManager) { app ->
            val intent = Intent().apply {
                putExtra("selected_package", app.packageName)
                putExtra("selected_name", app.appName)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        setupSearch()
    }

    /**
     * Loads all user-facing apps (those with a launcher intent).
     * Uses QUERY_ALL_PACKAGES permission to see all installed apps,
     * then filters to only those that can actually be launched.
     */
    private fun loadAllApps(): List<AppItem> {
        val pm = packageManager

        // Get launch intent apps — these are user-facing
        val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(launchIntent, PackageManager.GET_META_DATA)

        val apps = resolveInfos.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName == this.packageName) return@mapNotNull null
            val appInfo = resolveInfo.activityInfo.applicationInfo
            val appName = pm.getApplicationLabel(appInfo).toString()
            AppItem(
                packageName = packageName,
                appName = appName,
                appInfo = appInfo
            )
        }.distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }

        return apps
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                filterApps(query)
                binding.clearSearchBtn.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.clearSearchBtn.setOnClickListener {
            binding.searchInput.text.clear()
        }
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { app ->
                app.appName.lowercase().contains(query) ||
                app.packageName.lowercase().contains(query)
            }
        }
        adapter.updateApps(filtered)
        binding.noResultsText.visibility = if (filtered.isEmpty() && query.isNotEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }
}
