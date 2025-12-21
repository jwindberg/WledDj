package com.marsraver.wleddj.engine.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Loudness meter for overall volume level.
 * Provides a smoothed, normalized loudness value (0-1024) and a peak-hold value.
 * Maintains history for normalization and smooths values automatically.
 */
class LoudnessMeter {
    companion object {
        private const val HISTORY_SIZE = 600
        private const val SMOOTHING_FACTOR = 0.50
        private const val PEAK_DECAY_RATE = 0.05
    }

    @Volatile
    private var currentLoudness: Int = 0

    @Volatile
    private var peakLoudness: Int = 0

    private val audioLock = Any()
    private val historyLock = Any()
    private val history = ArrayDeque<Int>(HISTORY_SIZE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isStarted = false

    init {
        start()
    }

    private fun start() {
        if (isStarted) return
        isStarted = true

        scope.launch {
            AudioPipeline.rmsFlow().collectLatest { rms ->
                synchronized(audioLock) {
                    val rawLoudness = (rms.rms * 1024.0).toInt().coerceIn(0, 1024)

                    // Smooth the value (exponential moving average)
                    val smoothing = if (rawLoudness > currentLoudness) {
                        0.3
                    } else {
                        SMOOTHING_FACTOR
                    }
                    currentLoudness = (currentLoudness * smoothing + rawLoudness * (1.0 - smoothing)).toInt().coerceIn(0, 1024)

                    // Update peak
                    if (currentLoudness > peakLoudness) {
                        peakLoudness = currentLoudness
                    } else {
                        peakLoudness = max(currentLoudness, (peakLoudness * (1.0 - PEAK_DECAY_RATE)).toInt())
                    }

                    // Add to history for normalization
                    synchronized(historyLock) {
                        history.addLast(currentLoudness)
                        if (history.size > HISTORY_SIZE) {
                            history.removeFirst()
                        }
                    }
                }
            }
        }
    }

    fun getCurrentLoudness(): Int {
        return synchronized(audioLock) {
            currentLoudness
        }
    }

    fun getNormalizedLoudness(): Int {
        val current = synchronized(audioLock) {
            currentLoudness
        }

        return synchronized(historyLock) {
            if (history.size < 2) {
                current
            } else {
                val min = history.minOrNull() ?: 0
                val max = history.maxOrNull() ?: 1024

                if (max > min) {
                    val position = ((current - min).toDouble() / (max - min)).coerceIn(0.0, 1.0)
                    (position * 1024.0).toInt().coerceIn(0, 1024)
                } else {
                    512
                }
            }
        }
    }

    fun getPeakLoudness(): Int {
        return synchronized(audioLock) {
            peakLoudness
        }
    }

    fun stop() {
        scope.cancel()
        isStarted = false
    }
}
