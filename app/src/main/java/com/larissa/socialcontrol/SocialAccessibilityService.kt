package com.larissa.socialcontrol

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class SocialAccessibilityService : AccessibilityService() {
    private var lastInterceptedPackage: String? = null
    private var lastInterceptedAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName == packageNameForSelf()) return

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        Log.d(TAG, "Accessibility event from package=$packageName type=${event.eventType}")

        if (packageName != AppConfig.BLOCKED_PACKAGE) return
        if (UnlockGrantStore(this).isUnlocked(packageName)) {
            Log.d(TAG, "Skipping intercept for unlocked package=$packageName")
            return
        }
        if (isDebounced(packageName)) return

        lastInterceptedPackage = packageName
        lastInterceptedAt = SystemClock.elapsedRealtime()

        val lockIntent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        startActivity(lockIntent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
    }

    private fun isDebounced(packageName: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        return lastInterceptedPackage == packageName &&
            now - lastInterceptedAt < AppConfig.LOCK_DEBOUNCE_MS
    }

    private fun packageNameForSelf(): String = applicationContext.packageName

    private companion object {
        const val TAG = "SocialAccessService"
    }
}
