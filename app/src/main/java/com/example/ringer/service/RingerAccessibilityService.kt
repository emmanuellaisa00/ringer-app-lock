package com.example.ringer.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.example.ringer.LockActivity
import com.example.ringer.RingerApplication
import com.example.ringer.data.LockRepository

class RingerAccessibilityService : AccessibilityService() {

    private lateinit var repository: LockRepository
    private var lastPackage: String? = null

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
        if (packageName == lastPackage || packageName == this.packageName) {
            lastPackage = packageName
            return
        }
        lastPackage = packageName

        // Check if package is locked
        val isLocked = repository.getLockedApp(packageName) != null &&
                !repository.isUnlocked(packageName)

        if (isLocked) {
            // Immediately minimize
            performGlobalAction(GLOBAL_ACTION_HOME)

            // Launch lock screen (invisible to intruder)
            val intent = Intent(this, LockActivity::class.java).apply {
                putExtra("target_package", packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // No-op
    }
}