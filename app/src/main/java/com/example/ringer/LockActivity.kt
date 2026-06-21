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
                delay(200) // fast polling for responsive unlock
            }
        }
    }

    private fun onVolumeTriggered() {
        targetPackage?.let { pkg ->
            isUnlocking = true
            lifecycleScope.launch {
                // Set the app as unlocked
                val timeoutSeconds = repository.getUnlockTimeoutSeconds()
                repository.setUnlocked(pkg, timeoutSeconds)

                // Immediately mark as foreground so the accessibility service
                // won't re-lock it when the target app opens
                repository.setForeground(pkg)

                // Wait a moment for the unlock state to propagate
                delay(400)

                // Restore volume
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0)
                } catch (_: Exception) { }

                // Launch the target app
                launchTargetApp()

                // Finish LockActivity
                finish()
            }
        }
    }

    /**
     * Launch the target locked app.
     */
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
            } catch (_: Exception) {
                // If we can't launch the app, the user can open it manually
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
    }

    override fun onBackPressed() {
        // Don't allow back — user must trigger volume to unlock
        // or go home via the system navigation
    }
}
