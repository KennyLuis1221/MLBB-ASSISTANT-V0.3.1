package com.mlbb.assistant

internal object FrameBudget {
    const val INTERVAL_MS = 1_000L
    const val DEADLINE_MS = 1_000L
    const val OCR_MS = 500L

    fun due(now: Long, lastStarted: Long) = now - lastStarted >= INTERVAL_MS
    fun withinDeadline(now: Long, started: Long) = now - started in 0..DEADLINE_MS
}
