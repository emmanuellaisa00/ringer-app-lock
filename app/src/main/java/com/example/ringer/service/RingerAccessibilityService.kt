package com.example.ringer.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.ringer.LockActivity
import com.example.ringer.RingerApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RingerAccessibilityService : AccessibilityService() {

    private lateinit var app: RingerApplication
    private var currentPackage: String? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Packages that are transient overlays — switching to these does NOT mean
     * the user left the previous app.
     */
    private val overlayPrefixes = listOf(
        "com.android.systemui",
        "com.android.internal",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.android.dialog",
        "android"
    )

    /** Keywords in launcher package names — going here = user went home. */
    private val homeKeywords = listOf("launcher", "home", "trebuchet", "pixel")

    override fun onServiceConnected() {
        super.onServiceConnected()
        app = RingerApplication.instance

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 50
            packageNames = null
            flags = AccessibilityServiceInfo.DEFAULT
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Always ignore our own app
        if (packageName == this.packageName) return

        // Skip null/empty packages (rare edge case)
        if (packageName.isBlank()) return

        val previousPackage = currentPackage
        currentPackage = packageName

        // Same package — user is navigating within the same app, ignore
        if (packageName == previousPackage) return

        val repository = app.repository

        // ---- Step 1: Detect leaving a foreground-unlocked app ----
        if (previousPackage != null && repository.isForegroundUnlocked(previousPackage)) {
            if (isRealAppSwitch(previousPackage, packageName)) {
                // User intentionally left the locked app for a different real app or home
                repository.clearForeground(previousPackage)
            }
            // else: switching to an overlay (dialog, notification shade, etc.) — don't clear foreground
        }

        // ---- Step 2: Check if the new package is locked ----
        serviceScope.launch {
            // Re-check currentPackage in case it changed while coroutine was pending
            if (currentPackage != packageName) return@launch

            val lockedApp = repository.getLockedApp(packageName) ?: return@launch

            if (repository.isAccessible(packageName)) {
                // App is accessible — user is allowed in.
                // Set foreground so they stay in until they intentionally leave.
                repository.setForeground(packageName)
            } else {
                // App is LOCKED — close it immediately and monitor for unlock
                lockAndMonitor(packageName)
            }
        }
    }

    /**
     * Close the locked app and launch LockActivity to monitor volume.
     */
    private fun lockAndMonitor(packageName: String) {
        // Go home to close the locked app
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Small delay so home action takes effect before we start our activity
        serviceScope.launch {
            delay(300)

            // Re-check: maybe user already moved on
            if (currentPackage != packageName && !isHome(currentPackage)) {
                return@launch
            }

            val intent = Intent(this@RingerAccessibilityService, LockActivity::class.java).apply {
                putExtra("target_package", packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }
    }

    /**
     * Returns true if this is a "real" app switch — i.e., the user intentionally
     * navigated away from the previous app to a different real app or home screen.
     * Switching to overlays/dialogs does NOT count.
     */
    private fun isRealAppSwitch(fromPackage: String, toPackage: String): Boolean {
        // Going to an overlay is NOT a real switch
        if (isOverlay(toPackage)) return false

        // Going home IS a real switch
        if (isHome(toPackage)) return true

        // Going to a different real app IS a real switch
        return fromPackage != toPackage
    }

    private fun isOverlay(pkg: String): Boolean {
        return overlayPrefixes.any { pkg.startsWith(it) }
    }

    private fun isHome(pkg: String?): Boolean {
        if (pkg == null) return true
        val lower = pkg.lowercase()
        return homeKeywords.any { lower.contains(it) }
    }

    override fun onInterrupt() {}
}
