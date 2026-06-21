package com.example.ringer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ringer.data.LockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: LockRepository) : ViewModel() {

    /** Timeout in seconds. 0 = immediately. */
    private val _unlockTimeoutSeconds = MutableStateFlow(repository.getUnlockTimeoutSeconds())
    val unlockTimeoutSeconds: StateFlow<Int> = _unlockTimeoutSeconds.asStateFlow()

    /** Available timeout options in seconds */
    val timeoutOptions: List<Int> = listOf(0, 30, 60, 120, 300)

    fun setTimeoutSeconds(seconds: Int) {
        viewModelScope.launch {
            repository.setUnlockTimeoutSeconds(seconds)
            _unlockTimeoutSeconds.value = seconds
        }
    }

    fun getCurrentTimeoutSeconds(): Int = repository.getUnlockTimeoutSeconds()

    fun formatTimeoutLabel(seconds: Int): String {
        return when (seconds) {
            0 -> "Immediately"
            30 -> "30 sec"
            60 -> "1 min"
            120 -> "2 min"
            300 -> "5 min"
            else -> if (seconds < 60) "$seconds sec" else "${seconds / 60} min"
        }
    }
}
