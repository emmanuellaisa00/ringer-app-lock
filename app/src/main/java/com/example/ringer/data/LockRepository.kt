package com.example.ringer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "ringer_prefs")

class LockRepository(
    private val appDao: AppDao,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ringer_lock", Context.MODE_PRIVATE)
    private val UNLOCK_TIMEOUT_KEY = "unlock_timeout_minutes"
    private val DEFAULT_TIMEOUT = 1 // minutes as per user preference

    val lockedAppsFlow: Flow<List<AppInfo>> = appDao.getAllLockedApps()

    suspend fun addLockedApp(app: AppInfo) {
        appDao.insertApp(app)
    }

    suspend fun removeLockedApp(packageName: String) {
        appDao.deleteByPackage(packageName)
    }

    suspend fun getLockedApp(packageName: String): AppInfo? {
        return appDao.getAppByPackage(packageName)
    }

    suspend fun setUnlocked(packageName: String, timeoutMinutes: Int = getUnlockTimeout()) {
        val unlockTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeoutMinutes.toLong())
        prefs.edit()
            .putLong("unlock_${packageName}", unlockTime)
            .apply()
    }

    fun isUnlocked(packageName: String): Boolean {
        val unlockTime = prefs.getLong("unlock_${packageName}", 0L)
        return System.currentTimeMillis() < unlockTime
    }

    suspend fun clearUnlock(packageName: String) {
        prefs.edit().remove("unlock_${packageName}").apply()
    }

    fun getUnlockTimeout(): Int {
        return prefs.getInt(UNLOCK_TIMEOUT_KEY, DEFAULT_TIMEOUT)
    }

    suspend fun setUnlockTimeout(minutes: Int) {
        prefs.edit().putInt(UNLOCK_TIMEOUT_KEY, minutes).apply()
    }

    // For StateFlow in ViewModel
    fun getUnlockTimeFlow(packageName: String): Flow<Long> {
        // Simple polling approach using SharedPreferences listener could be used but this works
        return kotlinx.coroutines.flow.flow {
            while (true) {
                emit(prefs.getLong("unlock_${packageName}", 0L))
                kotlinx.coroutines.delay(1000)
            }
        }
    }
}