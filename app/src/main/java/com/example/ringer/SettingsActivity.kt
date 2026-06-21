package com.example.ringer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ringer.databinding.ActivitySettingsBinding
import com.example.ringer.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

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
        observeViewModel()
    }

    private fun setupUI() {
        binding.backButton.setOnClickListener {
            finish()
        }

        // Build timeout chips
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
                chipStrokeWidth = 1f
                setChipStrokeColorResource(R.color.border_subtle)
                chipCornerRadius = 36f

                setOnCheckedChangeListener { view, isChecked ->
                    if (isChecked) {
                        setChipBackgroundColorResource(R.color.chip_selected_bg)
                        setTextColor(resources.getColor(R.color.chip_selected_text, theme))
                        setChipStrokeColorResource(R.color.primary)
                        viewModel.setTimeoutSeconds(seconds)
                    } else {
                        setChipBackgroundColorResource(R.color.chip_unselected_bg)
                        setTextColor(resources.getColor(R.color.chip_unselected_text, theme))
                        setChipStrokeColorResource(R.color.border_subtle)
                    }
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.unlockTimeoutSeconds.collect { timeout ->
                val label = viewModel.formatTimeoutLabel(timeout)
                Toast.makeText(this@SettingsActivity, "Re-lock: $label", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
