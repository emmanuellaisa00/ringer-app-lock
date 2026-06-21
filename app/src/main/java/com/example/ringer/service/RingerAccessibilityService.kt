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
import kotlinx.coroutines.launch

class RingerAccessibilityService : AccessibilityService() {

    private lateinit var app: RingerApplication
    private var lastPackage: String? = null
    private var lastWasLocked: Boolean = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Packages that are system-level overlays. Switching to these does NOT mean
     * the user left the previous app — they just opened a dialog/shade/etc.
     */
    private val overlayPrefixes = listOf(
        "com.android.systemui",
        "com.android.internal",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.android.settings",        // settings quick tiles
        "com.android.dialog",          // system dialogs
        "com.google.android.apps.messaging", // RCS dialog overlays
        "android"
    )

    /** The launcher packages — going here means user went home. */
    private val launcherKeywords = listOf("launcher", "home", "trebuchet")

    override fun onServiceConnected() {
        super.onServiceConnected()
        app = RingerApplication.instance

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 50
            packageNames = null // monitor all packages
            flags = AccessibilityServiceInfo.DEFAULT
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore our own app's activities
        if (packageName == this.packageName) {
            lastPackage = packageName
            return
        }

        val previousPackage = lastPackage
        lastPackage = packageName

        // Same package, no action
        if (packageName == previousPackage) return

        val repository = app.repository

        // ---- Detect leaving a foreground-unlocked locked app ----
        if (previousPackage != null && !isOverlay(packageName)) {
            // User left previousPackage for a real different app
            repository.clearForeground(previousPackage)
        }

        // ---- Check if the new package is locked ----
        serviceScope.launch {
            val lockedApp = repository.getLockedApp(packageName) ?: return@launch

            // Guard: if user already moved on, skip
            if (lastPackage != packageName) return@launch

            if (repository.isAccessible(packageName)) {
                // Allowed in — mark as foreground so user stays protected while using
                repository.setForeground(packageName)
                lastWasLocked = true
            } else {
                // LOCKED: Close the app and show LockActivity for volume monitoring
                lastWasLocked = true
                performGlobalAction(GLOBAL_ACTION_HOME)

                // Brief delay for home action to take effect
                kotlinx.coroutines.delay(200)

                val intent = Intent(this@RingerAccessibilityService, LockActivity::class.java).apply {
                    putExtra("target_package", packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            }
        }
    }

    /**
     * Check if a package is a system overlay or launcher.
     * Switching to these does NOT mean the user intentionally left the previous app.
     */
    private fun isOverlay(pkg: String): Boolean {
        if (overlayPrefixes.any { pkg.startsWith(it) }) return true
        if (launcherKeywords.any { pkg.contains(it) }) return true
        return false
    }

    override fun onInterrupt() {
        // No-op
    }
}
