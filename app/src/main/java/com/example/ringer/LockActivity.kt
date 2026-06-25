package com.example.ringer

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LockActivity : AppCompatActivity() {

    private lateinit var repository: com.example.ringer.data.LockRepository
    private lateinit var audioManager: AudioManager
    private var targetPackage: String? = null
    private var previousVolume: Int = 0
    private var maxVolume: Int = 15
    private var monitoringJob: Job? = null
    private var isUnlocking: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)
        // Fade in the stealth overlay
        overridePendingTransition(R.anim.fade_in, 0)

        repository = RingerApplication.instance.repository
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        targetPackage = intent.getStringExtra("target_package")

        if (targetPackage == null) {
            finish()
            return
        }

        // If already accessible, just launch the target and finish
        if (repository.isAccessible(targetPackage!!)) {
            repository.setForeground(targetPackage!!)
            launchTargetApp()
            finish()
            return
        }

        // Start monitoring volume for unlock trigger
        startVolumeMonitoring()
    }

    private fun startVolumeMonitoring() {
        monitoringJob = lifecycleScope.launch {
            while (isActive) {
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVolume >= maxVolume && !isUnlocking) {
                    onVolumeTriggered()
                    break
                }
                delay(200)
            }
        }
    }

    private fun onVolumeTriggered() {
        targetPackage?.let { pkg ->
            isUnlocking = true
            lifecycleScope.launch {
                val timeoutSeconds = repository.getUnlockTimeoutSeconds()
                repository.setUnlocked(pkg, timeoutSeconds)
                repository.setForeground(pkg)
                delay(400)
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0)
                } catch (_: Exception) { }
                launchTargetApp()
                finish()
            }
        }
    }

    private fun launchTargetApp() {
        targetPackage?.let { pkg ->
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                    startActivity(launchIntent)
                }
            } catch (_: Exception) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        overridePendingTransition(0, R.anim.fade_out)
    }

    override fun onBackPressed() {
        // Don't allow back — user must trigger volume to unlock
        // or go home via the system navigation
    }
}
