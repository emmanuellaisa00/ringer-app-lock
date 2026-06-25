package com.example.ringer

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.DecelerateInterpolator
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        allApps = loadAllApps()

        adapter = AppPickerAdapter(allApps, packageManager) { app ->
            val intent = Intent().apply {
                putExtra("selected_package", app.packageName)
                putExtra("selected_name", app.appName)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        setupSearch()
        entranceAnimation()
    }

    private fun entranceAnimation() {
        binding.header.alpha = 0f
        binding.header.translationX = -30f
        binding.header.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        binding.searchCard.alpha = 0f
        binding.searchCard.translationY = 10f
        binding.searchCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setStartDelay(100)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.recyclerView.alpha = 0f
        binding.recyclerView.translationY = 15f
        binding.recyclerView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(200)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun loadAllApps(): List<AppItem> {
        val pm = packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(launchIntent, PackageManager.GET_META_DATA)

        return resolveInfos.mapNotNull { resolveInfo ->
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
        binding.noResultsLayout.visibility = if (filtered.isEmpty() && query.isNotEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
