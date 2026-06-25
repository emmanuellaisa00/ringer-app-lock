package com.example.ringer

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
                Toast.makeText(this, getString(R.string.app_locked_toast, name), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = (application as RingerApplication).repository
        viewModel = MainViewModel(repository)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
        checkPermissionsAndStartService()
        entranceAnimation()
    }

    private fun entranceAnimation() {
        // Header slides down
        binding.header.alpha = 0f
        binding.header.translationY = -20f
        binding.header.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(100)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        // Status card fades in
        binding.statusCard.alpha = 0f
        binding.statusCard.translationY = 10f
        binding.statusCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setStartDelay(200)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // FAB slides up from bottom
        binding.addButton.translationY = 100f
        binding.addButton.alpha = 0f
        binding.addButton.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(300)
            .setInterpolator(OvershootInterpolator(0.8f))
            .start()
    }

    private fun setupRecyclerView() {
        adapter = LockedAppsAdapter { app: AppInfo ->
            showRemoveConfirmDialog(app)
        }
        binding.lockedAppsRecycler.layoutManager = LinearLayoutManager(this)
        binding.lockedAppsRecycler.adapter = adapter
    }

    private fun showRemoveConfirmDialog(app: AppInfo) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_remove_title))
            .setMessage(getString(R.string.confirm_remove_message))
            .setPositiveButton(getString(R.string.remove)) { _, _ ->
                viewModel.removeApp(app.packageName)
                Toast.makeText(this, getString(R.string.app_removed_toast, app.appName), Toast.LENGTH_SHORT).show()
            }
            .setCancelable(true)
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun setupListeners() {
        binding.addButton.setOnClickListener {
            launchAppPicker()
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.lockedApps.collect { apps ->
                adapter.submitList(apps)
                if (apps.isEmpty()) {
                    binding.lockedAppsRecycler.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                    binding.lockedAppsHeader.visibility = View.GONE
                    binding.lockedCountText.visibility = View.GONE
                } else {
                    binding.lockedAppsRecycler.visibility = View.VISIBLE
                    binding.emptyState.visibility = View.GONE
                    binding.lockedAppsHeader.visibility = View.VISIBLE
                    binding.lockedCountText.visibility = View.VISIBLE
                    binding.lockedCountText.text = getString(R.string.locked_count, apps.size)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isAccessibilityEnabled.collect { enabled ->
                updateProtectionStatus(enabled)
            }
        }
    }

    private fun updateProtectionStatus(enabled: Boolean) {
        if (enabled) {
            binding.statusText.text = getString(R.string.accessibility_enabled)
            binding.statusText.setTextColor(getColor(R.color.status_active))
            binding.statusIcon.imageTintList = getColorStateList(R.color.status_active)
        } else {
            binding.statusText.text = getString(R.string.accessibility_disabled)
            binding.statusText.setTextColor(getColor(R.color.status_inactive))
            binding.statusIcon.imageTintList = getColorStateList(R.color.status_inactive)
        }
    }

    private fun launchAppPicker() {
        val intent = Intent(this, AppSelectionActivity::class.java)
        packagePicker.launch(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun checkPermissionsAndStartService() {
        val enabled = isAccessibilityServiceEnabled()
        viewModel.updateAccessibilityStatus(enabled)

        if (!enabled) {
            Toast.makeText(this, getString(R.string.enable_accessibility_prompt), Toast.LENGTH_LONG).show()
        }

        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        } catch (_: Exception) { }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        startProtectionService()
    }

    private fun startProtectionService() {
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
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
