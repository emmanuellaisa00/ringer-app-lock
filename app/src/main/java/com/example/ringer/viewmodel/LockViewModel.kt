package com.example.ringer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ringer.data.LockRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LockViewModel(private val repository: LockRepository) : ViewModel() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var targetPackage: String? = null

    fun setTargetPackage(packageName: String) {
        targetPackage = packageName
        _isUnlocked.value = repository.isUnlocked(packageName)
    }

    fun unlockWithVolume(packageName: String, previousVolume: Int, maxVolume: Int) {
        viewModelScope.launch {
            val timeout = repository.getUnlockTimeout()
            repository.setUnlocked(packageName, timeout)

            // Simulate restore previous volume after 1 second
            delay(1000)
            // Note: Volume restore logic is handled in service

            _isUnlocked.value = true
        }
    }

    fun checkUnlockStatus(packageName: String): Boolean {
        return repository.isUnlocked(packageName)
    }
}