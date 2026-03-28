package com.larissa.socialcontrol

object AppConfig {
    const val BLOCKED_PACKAGE = "com.instagram.android"
    const val CONTROL_PACKAGE = "com.duolingo"
    const val REQUIRED_SECONDS = 15
    const val LOCK_DEBOUNCE_MS = 1_500L
    const val UNLOCK_WINDOW_MS = 10 * 60 * 1_000L
}
