package com.example.ringer

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ringer.data.LockRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LockActivity : AppCompatActivity() {

    private lateinit var repository: LockRepository
    private lateinit var audioManager: AudioManager
    private var targetPackage: String? = null
    private var previousVolume: Int = 0
    private var maxVolume: Int = 15
    private var monitoringJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        repository = RingerApplication.instance.repository
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        targetPackage = intent.getStringExtra("target_package")

        if (targetPackage == null) {
            finish()
            return
        }

        // If already accessible, just open it and finish
        if (repository.isAccessible(targetPackage!!)) {
            launchTargetApp()
            finish()
            return
        }

        // Monitor volume for the unlock trigger
        startVolumeMonitoring()
    }

    private fun startVolumeMonitoring() {
        monitoringJob = lifecycleScope.launch {
            while (isActive) {
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVolume >= maxVolume) {
                    onVolumeTriggered()
                    break
                }
                delay(250)
            }
        }
    }

    private fun onVolumeTriggered() {
        targetPackage?.let { pkg ->
            lifecycleScope.launch {
                // Mark the app as unlocked
                val timeoutSeconds = repository.getUnlockTimeoutSeconds()
                repository.setUnlocked(pkg, timeoutSeconds)

                // Set it as foreground so the accessibility service doesn't re-lock it
                repository.setForeground(pkg)

                // Restore volume after brief delay
                delay(600)
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0)
                } catch (_: Exception) {
                    // Volume restore may fail on some devices, non-critical
                }

                // Launch the target app
                launchTargetApp()

                // Finish the lock activity
                finish()
            }
        }
    }

    /**
     * Launch the target locked app using its launch intent.
     * Falls back to opening the app's settings page if no launch intent exists.
     */
    private fun launchTargetApp() {
        targetPackage?.let { pkg ->
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                startActivity(launchIntent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
    }
}
