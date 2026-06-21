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

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSelectionBinding
    private lateinit var adapter: AppPickerAdapter
    private lateinit var allApps: List<ApplicationInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        val pm = packageManager
        allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != packageName }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        adapter = AppPickerAdapter(allApps, pm) { appInfo ->
            val intent = Intent().apply {
                putExtra("selected_package", appInfo.packageName)
                putExtra("selected_name", pm.getApplicationLabel(appInfo).toString())
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        setupSearch()
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                filterApps(query)

                // Show/hide clear button
                binding.clearSearchBtn.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.clearSearchBtn.setOnClickListener {
            binding.searchInput.text.clear()
        }
    }

    private fun filterApps(query: String) {
        val pm = packageManager
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { app ->
                val name = pm.getApplicationLabel(app).toString().lowercase()
                val pkg = app.packageName.lowercase()
                name.contains(query) || pkg.contains(query)
            }
        }
        adapter.updateApps(filtered)
        binding.noResultsText.visibility = if (filtered.isEmpty() && query.isNotEmpty()) View.VISIBLE else View.GONE
    }
}
