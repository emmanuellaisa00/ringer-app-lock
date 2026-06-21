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

    /** Apps the user is currently actively using (in foreground). */
    private val foregroundUnlocked = mutableSetOf<String>()

    /** Apps just unlocked via volume trigger — one-time pass to enter. */
    private val justUnlocked = mutableSetOf<String>()

    val lockedAppsFlow: Flow<List<AppInfo>> = appDao.getAllLockedApps()

    suspend fun addLockedApp(app: AppInfo) {
        appDao.insertApp(app)
    }

    suspend fun removeLockedApp(packageName: String) {
        appDao.deleteByPackage(packageName)
        // Clean up any unlock state
        clearAllUnlockState(packageName)
    }

    suspend fun getLockedApp(packageName: String): AppInfo? {
        return appDao.getAppByPackage(packageName)
    }

    /**
     * Mark an app as unlocked after volume trigger.
     * @param timeoutSeconds 0 = immediately re-lock once user exits.
     */
    suspend fun setUnlocked(packageName: String, timeoutSeconds: Int = getUnlockTimeoutSeconds()) {
        val unlockTime = if (timeoutSeconds == 0) {
            // Immediately: expired timestamp; foreground set handles accessibility
            System.currentTimeMillis() - 1
        } else {
            System.currentTimeMillis() + (timeoutSeconds * 1000L)
        }
        prefs.edit()
            .putLong("unlock_$packageName", unlockTime)
            .apply()

        justUnlocked.add(packageName)
    }

    /**
     * Can the user access this locked app right now?
     * True if: foreground-unlocked OR just-unlocked OR timestamp still valid.
     */
    fun isAccessible(packageName: String): Boolean {
        if (foregroundUnlocked.contains(packageName)) return true
        if (justUnlocked.contains(packageName)) return true
        return isTimestampUnlocked(packageName)
    }

    /** Is this app currently in the foreground-unlocked state? */
    fun isForegroundUnlocked(packageName: String): Boolean {
        return foregroundUnlocked.contains(packageName)
    }

    /** Timestamp-based unlock check. */
    private fun isTimestampUnlocked(packageName: String): Boolean {
        val unlockTime = prefs.getLong("unlock_$packageName", 0L)
        return System.currentTimeMillis() < unlockTime
    }

    /**
     * Mark an app as currently in the foreground.
     * The user will NOT be kicked out while this is set.
     */
    fun setForeground(packageName: String) {
        foregroundUnlocked.add(packageName)
        justUnlocked.remove(packageName) // no longer needs one-time pass
    }

    /**
     * Clear foreground when the user intentionally leaves the app.
     * After this, timestamp-based timeout determines re-entry.
     */
    fun clearForeground(packageName: String) {
        foregroundUnlocked.remove(packageName)
    }

    /** Clear all unlock state for a package. */
    private fun clearAllUnlockState(packageName: String) {
        prefs.edit().remove("unlock_$packageName").apply()
        foregroundUnlocked.remove(packageName)
        justUnlocked.remove(packageName)
    }

    /** Clear the just-unlocked flag (after it's been consumed). */
    fun consumeJustUnlocked(packageName: String) {
        justUnlocked.remove(packageName)
    }

    fun getUnlockTimeoutSeconds(): Int {
        return prefs.getInt(UNLOCK_TIMEOUT_KEY, DEFAULT_TIMEOUT_SECONDS)
    }

    suspend fun setUnlockTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt(UNLOCK_TIMEOUT_KEY, seconds).apply()
    }

    fun getUnlockTimeFlow(packageName: String): Flow<Long> {
        return kotlinx.coroutines.flow.flow {
            while (true) {
                emit(prefs.getLong("unlock_$packageName", 0L))
                kotlinx.coroutines.delay(1000)
            }
        }
    }
}
