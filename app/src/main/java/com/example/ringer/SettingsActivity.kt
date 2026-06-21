package com.example.ringer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.ringer.databinding.ActivitySettingsBinding
import com.example.ringer.viewmodel.SettingsViewModel

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = (application as RingerApplication).repository
        viewModel = SettingsViewModel(repository)

        setupUI()
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener { finish() }

        setupTimeoutChips()

        binding.enableAccessibilityBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.disableBatteryBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    private fun setupTimeoutChips() {
        val chipGroup = binding.timeoutChipGroup
        val currentTimeout = viewModel.getCurrentTimeoutSeconds()

        for (seconds in viewModel.timeoutOptions) {
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = viewModel.formatTimeoutLabel(seconds)
                isCheckable = true
                isChecked = seconds == currentTimeout
                setChipBackgroundColorResource(R.color.chip_unselected_bg)
                setTextColor(resources.getColor(R.color.chip_unselected_text, theme))
                chipStrokeWidth = 0f
                chipCornerRadius = 32f

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        setChipBackgroundColorResource(R.color.chip_selected_bg)
                        setTextColor(resources.getColor(R.color.chip_selected_text, theme))
                        viewModel.setTimeoutSeconds(seconds)
                    } else {
                        setChipBackgroundColorResource(R.color.chip_unselected_bg)
                        setTextColor(resources.getColor(R.color.chip_unselected_text, theme))
                    }
                }
            }
            chipGroup.addView(chip)
        }
    }

    override fun onResume() {
        super.onResume()
        // Update accessibility status dot
        val enabled = isAccessibilityServiceEnabled()
        binding.accessibilityStatus.text = if (enabled) getString(R.string.accessibility_enabled) else getString(R.string.accessibility_disabled)
        val colorRes = if (enabled) R.color.status_active else R.color.status_inactive
        binding.accessibilityStatus.setTextColor(getColor(colorRes))
        binding.accessibilityDot.setBackgroundColor(getColor(colorRes))
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any { it.id.contains(packageName) }
    }
}
