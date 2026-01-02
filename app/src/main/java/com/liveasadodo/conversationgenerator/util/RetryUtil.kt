package com.liveasadodo.conversationgenerator.util

import android.util.Log
import kotlinx.coroutines.delay

object RetryUtil {
    suspend fun <T> callWithRetry(
        maxRetries: Int = 3,
        initialDelay: Long = 2000L,
        maxDelay: Long = 10000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay

        repeat(maxRetries) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    return Result.failure(e)
                }

                // Log retry attempt
                Log.w("RetryUtil", "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms", e)

                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }

        return Result.failure(Exception("Max retries exceeded"))
    }
}
