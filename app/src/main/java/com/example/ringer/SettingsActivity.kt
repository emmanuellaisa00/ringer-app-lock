package com.example.ringer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.animation.DecelerateInterpolator
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
        entranceAnimation()
    }

    private fun entranceAnimation() {
        // Header slides in
        binding.header.alpha = 0f
        binding.header.translationX = -30f
        binding.header.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        // Scroll content fades in
        val scrollView = binding.root.getChildAt(1)
        scrollView?.let {
            it.alpha = 0f
            it.translationY = 15f
            it.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(150)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

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
        val enabled = isAccessibilityServiceEnabled()
        binding.accessibilityStatus.text = if (enabled) getString(R.string.accessibility_enabled) else getString(R.string.accessibility_disabled)
        val colorRes = if (enabled) R.color.status_active else R.color.status_inactive
        binding.accessibilityStatus.setTextColor(getColor(colorRes))
        binding.accessibilityDot.setBackgroundColor(getColor(colorRes))
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any { it.id.contains(packageName) }
    }
}
