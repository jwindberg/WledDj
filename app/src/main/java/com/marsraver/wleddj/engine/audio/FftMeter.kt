package com.marsraver.wleddj.engine.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * FFT meter for normalized frequency spectrum data.
 */
class FftMeter(
    private val bands: Int = 16,
    private val sampleRate: Int = 44100
) {
    companion object {
        private const val HISTORY_SIZE = 600
        private const val SMOOTHING_FACTOR = 0.5
    }
    
    @Volatile
    private var currentBands: IntArray = IntArray(bands)
    
    @Volatile
    private var majorPeakFrequency: Float = 0.0f
    
    private val audioLock = Any()
    private val historyLock = Any()
    private val bandHistory = Array(bands) { ArrayDeque<Int>(HISTORY_SIZE) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isStarted = false

    init {
        start()
    }

    private fun start() {
        if (isStarted) return
        isStarted = true

        scope.launch {
            AudioPipeline.spectrumFlow(bands = bands).collectLatest { spectrum ->
                synchronized(audioLock) {
                    for (i in 0 until bands) {
                        val rawValue = spectrum.bands.getOrNull(i) ?: 0
                        val normalized = rawValue.coerceIn(0, 255)
                        
                        val current = currentBands.getOrNull(i) ?: 0
                        val smoothing = if (normalized > current) {
                            0.3
                        } else {
                            SMOOTHING_FACTOR
                        }
                        val smoothed = (current * smoothing + normalized * (1.0 - smoothing)).toInt()
                        currentBands[i] = smoothed.coerceIn(0, 255)
                        
                        synchronized(historyLock) {
                            val history = bandHistory[i]
                            history.addLast(smoothed)
                            if (history.size > HISTORY_SIZE) {
                                history.removeFirst()
                            }
                        }
                    }
                    
                    var maxMag = 0
                    var maxBandIndex = 0
                    for (i in 0 until bands) {
                        if (currentBands[i] > maxMag) {
                            maxMag = currentBands[i]
                            maxBandIndex = i
                        }
                    }
                    majorPeakFrequency = if (maxMag > 0) {
                        (maxBandIndex * sampleRate / (bands * 2.0f)).coerceAtLeast(1.0f)
                    } else {
                        0.0f
                    }
                }
            }
        }
    }

    fun getBands(): IntArray {
        return synchronized(audioLock) {
            currentBands.copyOf()
        }
    }

    fun getNormalizedBands(): IntArray {
        val bandsSnapshot = synchronized(audioLock) {
            currentBands.copyOf()
        }
        
        return synchronized(historyLock) {
            IntArray(bands) { bandIndex ->
                val history = bandHistory[bandIndex]
                val currentValue = bandsSnapshot[bandIndex]
                
                if (history.size < 2) {
                    currentValue
                } else {
                    val min = history.minOrNull() ?: 0
                    val max = history.maxOrNull() ?: 255
                    
                    if (max > min) {
                        val position = ((currentValue - min).toDouble() / (max - min)).coerceIn(0.0, 1.0)
                        (position * 255.0).toInt().coerceIn(0, 255)
                    } else {
                        128
                    }
                }
            }
        }
    }

    fun getMajorPeakFrequency(): Float {
        return synchronized(audioLock) {
            majorPeakFrequency
        }
    }

    fun stop() {
        scope.cancel()
        isStarted = false
    }
}
