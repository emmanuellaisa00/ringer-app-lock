package com.example.ringer

import android.content.Context
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

        // Check if already accessible (foreground or timeout unlock)
        if (repository.isAccessible(targetPackage!!)) {
            launchTargetApp()
            finish()
            return
        }

        // Stealth: no UI visible, just monitor volume
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
                delay(300) // poll every 300ms
            }
        }
    }

    private fun onVolumeTriggered() {
        targetPackage?.let { pkg ->
            lifecycleScope.launch {
                // Mark app as unlocked (sets timestamp + justUnlocked flag)
                val timeoutSeconds = repository.getUnlockTimeoutSeconds()
                repository.setUnlocked(pkg, timeoutSeconds)

                // Restore volume after a brief delay
                delay(800)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0)

                // Automatically launch the locked app so the user can use it
                launchTargetApp()

                finish()
            }
        }
    }

    /**
     * Launch the target (locked) app so the user can use it after unlocking.
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
