package com.example.ringer

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ringer.data.AppInfo
import com.example.ringer.databinding.ActivityMainBinding
import com.example.ringer.service.RingerForegroundService
import com.example.ringer.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: LockedAppsAdapter

    private val packagePicker = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val pkg = result.data?.getStringExtra("selected_package")
            val name = result.data?.getStringExtra("selected_name")
            if (pkg != null && name != null) {
                viewModel.addApp(AppInfo(pkg, name))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make the toolbar transparent for glass effect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val repository = (application as RingerApplication).repository
        viewModel = MainViewModel(repository)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        checkPermissionsAndStartService()
    }

    private fun setupRecyclerView() {
        adapter = LockedAppsAdapter { packageName ->
            viewModel.removeApp(packageName)
        }
        binding.lockedAppsRecycler.layoutManager = LinearLayoutManager(this)
        binding.lockedAppsRecycler.adapter = adapter

        // Show empty state initially
        binding.emptyState.visibility = android.view.View.VISIBLE
        binding.lockedAppsRecycler.visibility = android.view.View.GONE
    }

    private fun setupListeners() {
        binding.addButton.setOnClickListener {
            // iOS-like bounce animation
            binding.addButton.animate()
                .scaleX(0.75f).scaleY(0.75f)
                .setDuration(80)
                .withEndAction {
                    binding.addButton.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(160)
                        .start()
                }
                .start()
            launchAppPicker()
        }

        binding.settingsButton.setOnClickListener {
            // Smooth press animation
            binding.settingsButton.animate()
                .scaleX(0.85f).scaleY(0.85f)
                .setDuration(70)
                .withEndAction {
                    binding.settingsButton.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(140)
                        .start()
                }
                .start()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.lockedApps.collect { apps ->
                adapter.submitList(apps)
                if (apps.isEmpty()) {
                    binding.emptyState.visibility = android.view.View.VISIBLE
                    binding.lockedAppsRecycler.visibility = android.view.View.GONE
                } else {
                    binding.emptyState.visibility = android.view.View.GONE
                    binding.lockedAppsRecycler.visibility = android.view.View.VISIBLE
                    binding.statusText.text = "${apps.size} apps locked"
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isAccessibilityEnabled.collect { enabled ->
                binding.statusText.text = if (enabled) {
                    getString(R.string.accessibility_enabled)
                } else {
                    getString(R.string.accessibility_disabled)
                }
            }
        }
    }

    private fun launchAppPicker() {
        val intent = Intent(this, AppSelectionActivity::class.java)
        packagePicker.launch(intent)
    }

    private fun checkPermissionsAndStartService() {
        // Check accessibility
        val enabled = isAccessibilityServiceEnabled()
        viewModel.updateAccessibilityStatus(enabled)

        if (!enabled) {
            Toast.makeText(this, "Please enable Accessibility Service", Toast.LENGTH_LONG).show()
        }

        // Battery optimization
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        // Start foreground service
        val serviceIntent = Intent(this, RingerForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.id.contains(packageName) }
    }

    override fun onResume() {
        super.onResume()
        val enabled = isAccessibilityServiceEnabled()
        viewModel.updateAccessibilityStatus(enabled)
    }
}