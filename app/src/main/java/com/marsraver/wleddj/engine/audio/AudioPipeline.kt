package com.marsraver.wleddj.engine.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility for exposing microphone data as coroutine-friendly flows.
 * Falls back to simulated audio when no microphone is available or permission denied.
 */
object AudioPipeline {

    private const val DEFAULT_SAMPLE_RATE = 44100
    private const val DEFAULT_BUFFER_SIZE = 4096
    private const val TAG = "AudioPipeline"

    private val _pcmSharedFlow by lazy {
        pcmFlow(DEFAULT_SAMPLE_RATE, DEFAULT_BUFFER_SIZE, true)
            .shareIn(
                scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Default),
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                replay = 0
            )
    }

    fun rmsFlow(): Flow<AudioLevel> =
        _pcmSharedFlow
            .map { frame ->
                val rms = computeRms(frame.samples)
                AudioLevel(rms, levelFromRms(rms))
            }
            .flowOn(Dispatchers.Default)
            .conflate()

    fun spectrumFlow(bands: Int = 16): Flow<AudioSpectrum> =
        _pcmSharedFlow
            .map { frame ->
                AudioSpectrum(computeBands(frame.samples, bands))
            }
            .flowOn(Dispatchers.Default)
            .conflate()

    @SuppressLint("MissingPermission")
    private fun pcmFlow(
        sampleRate: Int,
        bufferSize: Int,
        simulateWhenUnavailable: Boolean,
    ): Flow<PcmFrame> = callbackFlow {
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val actualBufferSize = kotlin.math.max(bufferSize, minBufferSize)
        
        var recorder: AudioRecord? = null

        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                actualBufferSize
            )
            
            if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                recorder.startRecording()
                Log.d(TAG, "Audio Recording Started")
                
                val buffer = ShortArray(bufferSize) // Reading shorts directly is cleaner on Android

                val reader = launch(Dispatchers.IO) {
                    while (isActive) {
                        val readCount = recorder.read(buffer, 0, buffer.size)
                        if (readCount > 0) {
                            val frameSamples = buffer.copyOf(readCount)
                            trySend(PcmFrame(frameSamples, System.nanoTime()))
                        } else {
                            // Error or stop
                            if (readCount < 0) {
                                Log.e(TAG, "Audio Record Read Error: $readCount")
                                break
                            }
                        }
                    }
                }

                awaitClose {
                    Log.d(TAG, "Stopping Audio Recording")
                    reader.cancel()
                    recorder.stop()
                    recorder.release()
                }
            } else {
                Log.e(TAG, "Audio Record Init Failed")
                recorder.release()
                if (simulateWhenUnavailable) {
                   launchSimulator(sampleRate, bufferSize)
                } else {
                    close()
                }
            }
        } catch (e: java.util.concurrent.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio Setup Failed: ${e.message}")
            if (simulateWhenUnavailable) {
               launchSimulator(sampleRate, bufferSize)
            } else {
                close()
            }
        }
    }.buffer(Channel.CONFLATED)

    private suspend fun kotlinx.coroutines.channels.ProducerScope<PcmFrame>.launchSimulator(sampleRate: Int, bufferSize: Int) {
        val simulator = launch {
            Log.d(TAG, "Starting Audio Simulator")
            val sampleCount = bufferSize
            var time = 0.0
            val increment1 = 2 * PI * 110 / sampleRate
            val increment2 = 2 * PI * 220 / sampleRate
            val increment3 = 2 * PI * 440 / sampleRate
            
            while (isActive) {
                val samples = ShortArray(sampleCount)
                for (i in 0 until sampleCount) {
                    val value = sin(time) * 0.6 +
                        sin(time * 0.5) * 0.3 +
                        sin(time * 1.5) * 0.2
                    samples[i] = (value * Short.MAX_VALUE * 0.7).roundToInt().toShort()
                    time += increment1
                }
                trySend(PcmFrame(samples, System.nanoTime()))
                delay(16)
            }
        }
        awaitClose { simulator.cancel() }
    }

    private fun computeRms(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var sum = 0.0
        for (sample in samples) {
            val normalized = sample / Short.MAX_VALUE.toDouble()
            sum += normalized * normalized
        }
        return sqrt(sum / samples.size)
    }

    private fun levelFromRms(rms: Double): Int =
        (rms * 255.0 * 10.0).roundToInt().coerceIn(0, 255)

    private fun computeBands(samples: ShortArray, bands: Int): IntArray {
        // Use a window size of 512 for FFT (good balance for mobile)
        val fftSize = 512
        if (samples.size < fftSize) return IntArray(bands)

        // 1. Prepare input (Real part only, Imaginary 0)
        // Apply Hanning Window to reduce leakage
        val real = FloatArray(fftSize)
        val imag = FloatArray(fftSize)
        
        for (i in 0 until fftSize) {
            val window = 0.5 * (1.0 - kotlin.math.cos(2.0 * Math.PI * i / (fftSize - 1)))
            real[i] = (samples[i] * window).toFloat()
            imag[i] = 0f
        }

        // 2. Perform FFT
        fft(real, imag)

        // 3. Compute Magnitudes needed for the requested number of bands
        // Max frequency is SampleRate / 2.
        // We want to map linear bins to the requested 'bands'.
        // FFT bins: 0 to fftSize/2.
        
        val magnitudes =  FloatArray(fftSize / 2)
        for (i in 0 until fftSize / 2) {
            magnitudes[i] = kotlin.math.sqrt(real[i] * real[i] + imag[i] * imag[i])
        }

        val outBands = IntArray(bands)
        val binsPerBand = (fftSize / 2) / bands
        
        for (i in 0 until bands) {
            var sum = 0f
            // Simple linear averaging. For better audio vis, logarithmic is better but this suffices.
            // Ensure we don't go out of bounds
            val startBin = i * binsPerBand
            val endBin = min(startBin + binsPerBand, magnitudes.size)
            
            for (j in startBin until endBin) {
                sum += magnitudes[j]
            }
            // Average and scale
            // Scaling factor tuned for 16-bit PCM inputs
            val avg = if (endBin > startBin) sum / (endBin - startBin) else 0f
            val db = 20 * kotlin.math.log10(avg + 1) // Log scale usually looks better
            val scaled = (db * 3).toInt().coerceIn(0, 255) 
            
            outBands[i] = scaled
        }
        
        return outBands
    }

    /**
     * In-place radix-2 FFT.
     * Arrays must be power of 2 size.
     */
    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        // Bit reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }

        // Butterfly updates
        var l = 2
        while (l <= n) {
            val m = l / 2
            val ang = -2.0 * Math.PI / l
            val wReBase = kotlin.math.cos(ang)
            val wImBase = kotlin.math.sin(ang)
            
            var wRe = 1.0
            var wIm = 0.0
            
            for (k in 0 until m) {
                var i = k
                while (i < n) {
                    val j = i + m
                    val tr = (wRe * real[j] - wIm * imag[j]).toFloat()
                    val ti = (wRe * imag[j] + wIm * real[j]).toFloat()
                    
                    real[j] = real[i] - tr
                    imag[j] = imag[i] - ti
                    real[i] = real[i] + tr
                    imag[i] = imag[i] + ti
                    i += l
                }
                
                // Rotate w
                val nextWRe = wRe * wReBase - wIm * wImBase
                val nextWIm = wRe * wImBase + wIm * wReBase
                wRe = nextWRe
                wIm = nextWIm
            }
            l *= 2
        }
    }
}

data class PcmFrame(
    val samples: ShortArray,
    val timestampNanos: Long,
)

data class AudioLevel(
    val rms: Double,
    val level: Int,
)

data class AudioSpectrum(
    val bands: IntArray,
)
