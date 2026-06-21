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

    /** Apps the user is currently actively using (in foreground). Stays until they leave the app. */
    private val foregroundUnlockedPackages = mutableSetOf<String>()

    /** Apps that were just unlocked via volume trigger. One-time pass to enter the app. */
    private val justUnlockedPackages = mutableSetOf<String>()

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
     * Mark an app as unlocked after the volume condition is met.
     * @param timeoutSeconds How long the app stays unlocked after the user LEAVES it.
     *                       0 = immediately re-locks once user exits the app.
     */
    suspend fun setUnlocked(packageName: String, timeoutSeconds: Int = getUnlockTimeoutSeconds()) {
        val unlockTime = if (timeoutSeconds == 0) {
            // For "Immediately": timestamp is already expired.
            // The justUnlocked + foreground sets handle the actual accessibility.
            System.currentTimeMillis() - 1
        } else {
            System.currentTimeMillis() + (timeoutSeconds * 1000L)
        }
        prefs.edit()
            .putLong("unlock_${packageName}", unlockTime)
            .apply()

        // Mark as just-unlocked so the accessibility service grants one-time entry
        justUnlockedPackages.add(packageName)
    }

    /**
     * Check if the app is currently accessible (user can use it freely).
     * Returns true if:
     * 1. The user is currently in the app (foreground unlocked), OR
     * 2. The app was just unlocked via volume (one-time entry), OR
     * 3. The timestamp-based unlock is still valid (within timeout)
     */
    fun isAccessible(packageName: String): Boolean {
        if (foregroundUnlockedPackages.contains(packageName)) return true
        if (justUnlockedPackages.contains(packageName)) return true
        return isUnlocked(packageName)
    }

    /**
     * Pure timestamp-based unlock check.
     */
    fun isUnlocked(packageName: String): Boolean {
        val unlockTime = prefs.getLong("unlock_${packageName}", 0L)
        return System.currentTimeMillis() < unlockTime
    }

    /**
     * Mark an app as currently in the foreground and being used by the user.
     * Called by the accessibility service when it detects the user entering an accessible locked app.
     */
    fun setForeground(packageName: String) {
        foregroundUnlockedPackages.add(packageName)
        // Clear the just-unlocked flag since the user has entered the app
        justUnlockedPackages.remove(packageName)
    }

    /**
     * Clear the foreground status when the user leaves the locked app.
     * After this, the timestamp-based timeout determines if they can re-enter.
     */
    fun clearForeground(packageName: String) {
        foregroundUnlockedPackages.remove(packageName)
    }

    suspend fun clearUnlock(packageName: String) {
        prefs.edit().remove("unlock_${packageName}").apply()
        foregroundUnlockedPackages.remove(packageName)
        justUnlockedPackages.remove(packageName)
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
