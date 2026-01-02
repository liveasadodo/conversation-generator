package com.liveasadodo.conversationgenerator.util

import kotlinx.coroutines.delay

object RateLimiter {
    private var lastRequestTime = 0L
    private const val MIN_REQUEST_INTERVAL = 4000L // 4 seconds = ~15 RPM

    suspend fun waitIfNeeded() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime

        if (elapsed < MIN_REQUEST_INTERVAL) {
            delay(MIN_REQUEST_INTERVAL - elapsed)
        }

        lastRequestTime = System.currentTimeMillis()
    }
}
