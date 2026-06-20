package com.example.ringer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
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
        binding.timeoutInput.setText(viewModel.getCurrentTimeout().toString())

        binding.saveButton.setOnClickListener {
            val minutes = binding.timeoutInput.text.toString().toIntOrNull() ?: 1
            viewModel.setUnlockTimeout(minutes)
            Toast.makeText(this, "Timeout saved", Toast.LENGTH_SHORT).show()
        }

        binding.enableAccessibilityBtn.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        binding.disableBatteryBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.unlockTimeout.collect { timeout ->
                binding.timeoutInput.setText(timeout.toString())
            }
        }
    }
}