package com.example.ringer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ringer.data.AppInfo
import com.example.ringer.data.LockRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val repository: LockRepository) : ViewModel() {

    val lockedApps: StateFlow<List<AppInfo>> = repository.lockedAppsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    fun addApp(app: AppInfo) {
        viewModelScope.launch {
            repository.addLockedApp(app)
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            repository.removeLockedApp(packageName)
        }
    }

    fun updateAccessibilityStatus(enabled: Boolean) {
        _isAccessibilityEnabled.value = enabled
    }

    fun getUnlockTimeoutSeconds(): Int = repository.getUnlockTimeoutSeconds()
}
