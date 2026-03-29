package com.larissa.socialcontrol

object AppConfig {
    const val LOCK_DEBOUNCE_MS = 1_500L
    const val DEFAULT_REQUIRED_SECONDS = 15
    const val DEFAULT_UNLOCK_WINDOW_MINUTES = 10
}

object RuleLimits {
    val REQUIRED_SECONDS_RANGE = 10..300
    val UNLOCK_WINDOW_MINUTES_RANGE = 1..60
}
