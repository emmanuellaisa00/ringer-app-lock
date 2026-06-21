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

        // Check if already unlocked
        if (repository.isUnlocked(targetPackage!!)) {
            finish()
            return
        }

        // Stealth: no UI visible, just monitor
        startVolumeMonitoring()
    }

    private fun startVolumeMonitoring() {
        monitoringJob = lifecycleScope.launch {
            while (isActive) {
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (currentVolume >= maxVolume) {
                    unlockApp()
                    break
                }
                delay(300) // poll every 300ms
            }
        }
    }

    private fun unlockApp() {
        targetPackage?.let { pkg ->
            lifecycleScope.launch {
                val timeoutSeconds = repository.getUnlockTimeoutSeconds()
                repository.setUnlocked(pkg, timeoutSeconds)

                // Restore volume after 1 second
                delay(1000)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousVolume, 0)

                // Finish lock activity so the app can open
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
    }
}
