package com.example.ringer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ringer.data.LockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: LockRepository) : ViewModel() {

    private val _unlockTimeout = MutableStateFlow(repository.getUnlockTimeout())
    val unlockTimeout: StateFlow<Int> = _unlockTimeout.asStateFlow()

    fun setUnlockTimeout(minutes: Int) {
        viewModelScope.launch {
            repository.setUnlockTimeout(minutes)
            _unlockTimeout.value = minutes
        }
    }

    fun getCurrentTimeout(): Int = repository.getUnlockTimeout()
}