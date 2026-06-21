package com.example.ringer.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.ringer.LockActivity
import com.example.ringer.RingerApplication
import com.example.ringer.data.LockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RingerAccessibilityService : AccessibilityService() {

    private lateinit var repository: LockRepository
    private var lastPackage: String? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** System packages that are overlays/dialogs — switching to these does NOT mean the user left the app. */
    private val systemOverlayPrefixes = listOf(
        "com.android.systemui",
        "com.android.internal",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.android.packageinstaller"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = RingerApplication.instance.repository

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            packageNames = null // monitor all
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore our own app's activities (LockActivity, MainActivity, etc.)
        if (packageName == this.packageName) {
            lastPackage = packageName
            return
        }

        val previousPackage = lastPackage
        lastPackage = packageName

        // Same package as before (e.g. navigating within the same app) — no action needed
        if (packageName == previousPackage) return

        // When user switches away from a foreground-unlocked app to a REAL app
        // (not a system overlay like notification shade or dialog), clear the foreground status.
        // This means: the user intentionally left the locked app, so the timeout now matters.
        if (previousPackage != null && !isSystemOverlay(packageName)) {
            repository.clearForeground(previousPackage)
        }

        // Check if the new package is a locked app
        serviceScope.launch {
            val lockedApp = repository.getLockedApp(packageName) ?: return@launch

            // Guard: if user already moved on by the time coroutine runs, skip
            if (lastPackage != packageName) return@launch

            if (repository.isAccessible(packageName)) {
                // App is accessible — user is allowed in.
                // Set foreground so they stay in until they explicitly leave.
                repository.setForeground(packageName)
            } else {
                // App is locked and not accessible — close it immediately.
                performGlobalAction(GLOBAL_ACTION_HOME)

                // Launch LockActivity to monitor for volume trigger
                val intent = Intent(this@RingerAccessibilityService, LockActivity::class.java).apply {
                    putExtra("target_package", packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            }
        }
    }

    /**
     * Returns true if the package is a system overlay (notification shade, permission dialog, etc.)
     * Switching to a system overlay does NOT mean the user left the previous app.
     */
    private fun isSystemOverlay(pkg: String): Boolean {
        return systemOverlayPrefixes.any { pkg.startsWith(it) }
    }

    override fun onInterrupt() {
        // No-op
    }
}
