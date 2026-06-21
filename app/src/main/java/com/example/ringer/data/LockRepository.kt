package com.example.ringer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

class LockRepository(
    private val appDao: AppDao,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ringer_lock", Context.MODE_PRIVATE)
    private val UNLOCK_TIMEOUT_KEY = "unlock_timeout_seconds"
    private val DEFAULT_TIMEOUT_SECONDS = 0 // 0 = immediately

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

    /**
     * @param timeoutSeconds How long the app stays unlocked. 0 = immediately re-locks.
     */
    suspend fun setUnlocked(packageName: String, timeoutSeconds: Int = getUnlockTimeoutSeconds()) {
        val unlockTime = if (timeoutSeconds == 0) {
            // Immediately: set to current time so it's already expired
            System.currentTimeMillis() - 1
        } else {
            System.currentTimeMillis() + (timeoutSeconds * 1000L)
        }
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

    fun getUnlockTimeoutSeconds(): Int {
        return prefs.getInt(UNLOCK_TIMEOUT_KEY, DEFAULT_TIMEOUT_SECONDS)
    }

    suspend fun setUnlockTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt(UNLOCK_TIMEOUT_KEY, seconds).apply()
    }

    // For StateFlow in ViewModel
    fun getUnlockTimeFlow(packageName: String): Flow<Long> {
        return kotlinx.coroutines.flow.flow {
            while (true) {
                emit(prefs.getLong("unlock_${packageName}", 0L))
                kotlinx.coroutines.delay(1000)
            }
        }
    }
}
