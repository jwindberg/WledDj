package com.marsraver.wleddj.engine.animations

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.marsraver.wleddj.R
import com.marsraver.wleddj.engine.Animation
import kotlinx.coroutines.*
import kotlin.math.roundToInt

class DeathStarAnimation(private val context: Context) : Animation {

    private val frames = java.util.Collections.synchronizedList(mutableListOf<Bitmap>())
    private var isLoading = true
    private var startTime = System.currentTimeMillis()
    private val paint = Paint().apply { isFilterBitmap = true } // Smooth scaling
    private val loadJob: Job
    
    // FPS target for extraction (reduce memory/cpu load)
    private val TARGET_FPS = 15
    private val TARGET_SIZE = 200 // Max dimension for LED scaling

    init {
        loadJob = CoroutineScope(Dispatchers.IO).launch {
            loadVideo()
        }
    }

    private fun loadVideo() {
        val extractor = android.media.MediaExtractor()
        var decoder: android.media.MediaCodec? = null
        var imageReader: android.media.ImageReader? = null
        
        try {
            val afd = context.resources.openRawResourceFd(R.raw.death_star_run) // Direct FD access
            extractor.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            
            // Find Video Track
            var trackIndex = -1
            var mimeType = ""
            var format: android.media.MediaFormat? = null
            
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    trackIndex = i
                    mimeType = mime
                    format = f
                    break
                }
            }
            
            if (trackIndex == -1 || format == null) {
                isLoading = false
                return
            }
            
            extractor.selectTrack(trackIndex)
            
            // Get Dims
            val w = format.getInteger(android.media.MediaFormat.KEY_WIDTH)
            val h = format.getInteger(android.media.MediaFormat.KEY_HEIGHT)
            // Scale Target
            val scale = TARGET_SIZE.toFloat() / maxOf(w, h)
            val dstW = (w * scale).roundToInt()
            val dstH = (h * scale).roundToInt()
            
            // Initialize ImageReader (RGBA)
            // Note: Scaling isn't done by decoder usually, we get full size. 
            // We can resize lazily or during copy. 
            // For max perf, we read full size then scale.
            imageReader = android.media.ImageReader.newInstance(w, h, android.graphics.PixelFormat.RGBA_8888, 2)
            
            // Configure Decoder
            decoder = android.media.MediaCodec.createDecoderByType(mimeType)
            decoder.configure(format, imageReader.surface, null, 0)
            decoder.start()
            
            val info = android.media.MediaCodec.BufferInfo()
            var isEOS = false
            var outputDone = false
            
            val timeoutUs = 10000L
            val intervalUs = (1000000 / TARGET_FPS).toLong()
            var lastSavedTimeUs = -intervalUs // Start ready to save first frame
            
            while (!outputDone) {
                // 1. Feed Input
                if (!isEOS) {
                    val inIndex = decoder.dequeueInputBuffer(timeoutUs)
                    // ... (no change to input logic)
                    if (inIndex >= 0) {
                        val buffer = decoder.getInputBuffer(inIndex)
                        if (buffer != null) {
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inIndex, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                isEOS = true
                            } else {
                                val time = extractor.sampleTime
                                decoder.queueInputBuffer(inIndex, 0, sampleSize, time, 0)
                                extractor.advance()
                            }
                        }
                    }
                }
                
                // 2. Poll Output
                val outIndex = decoder.dequeueOutputBuffer(info, timeoutUs)
                if (outIndex >= 0) {
                    if ((info.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                    
                    // Logic: Should we render this frame?
                    // If EOS, play safe.
                    // If presentationTimeUs >= lastSavedTime + interval, Render.
                    // Else, Drop.
                    // (Allow slight jitter handling? strict > is fine for this)
                    
                    val timeUs = info.presentationTimeUs
                    val shouldRender = (info.size > 0) && (timeUs >= lastSavedTimeUs + intervalUs)
                    
                    decoder.releaseOutputBuffer(outIndex, shouldRender)
                    
                    if (shouldRender) {
                        lastSavedTimeUs = timeUs
                        // Acquire Image
                        val image = imageReader.acquireLatestImage()
                        // ...
                        if (image != null) {
                             // Convert Image to Bitmap
                             val planes = image.planes
                             val buffer = planes[0].buffer
                             val pixelStride = planes[0].pixelStride
                             val rowStride = planes[0].rowStride
                             val rowPadding = rowStride - pixelStride * w
                             
                             // Safety check
                             // Create Bitmap directly?
                             // Bitmap.createBitmap(w, h, Config.ARGB_8888) expects packed...
                             // If stride != width * 4, we have padding.
                             
                             // Fast Copy:
                             val bitmap = Bitmap.createBitmap(w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888)
                             bitmap.copyPixelsFromBuffer(buffer)
                             
                             // Cleanup dimensions (Crop padding)
                             val cleanBitmap = if (rowPadding == 0) bitmap else Bitmap.createBitmap(bitmap, 0, 0, w, h)
                             if (cleanBitmap != bitmap) bitmap.recycle()
                             
                             // Scale now
                             val scaled = Bitmap.createScaledBitmap(cleanBitmap, dstW, dstH, true)
                             if (scaled != cleanBitmap) cleanBitmap.recycle()
                             
                             synchronized(frames) {
                                  frames.add(scaled)
                             }
                             image.close()
                        }
                    }
                } else if (outIndex == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                     // Handle format change if needed (stride/slice height)
                }
            }
            
        } catch (e: Exception) {
            Log.e("DeathStarAnim", "MediaCodec Error", e)
        } finally {
            try {
                decoder?.stop()
                decoder?.release()
                extractor.release()
                imageReader?.close()
            } catch (e: Exception) {}
            isLoading = false
            startTime = System.currentTimeMillis()
        }
    }

    // Playback Logic Helper
    
    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Just sync access.
        
        var frameToDraw: Bitmap? = null
        var loadedCount = 0
        
        synchronized(frames) {
            loadedCount = frames.size
            if (loadedCount > 0) {
                 // Playback Logic
                val frameDurationMs = (1000 / TARGET_FPS)
                val elapsed = System.currentTimeMillis() - startTime
                
                var frameIndex = (elapsed / frameDurationMs).toInt()
                
                if (isLoading) {
                    // While loading, if we catch up to end, wait.
                    if (frameIndex >= loadedCount) {
                        frameIndex = loadedCount - 1
                        // Optional: Show "Buffering" if stalled?
                    }
                } else {
                    // Loop when done
                    frameIndex %= loadedCount
                }
                
                frameToDraw = frames.getOrNull(frameIndex) ?: frames.last()
            }
        }

        if (frameToDraw == null) {
            // Still 0 frames
            paint.color = Color.GRAY
            paint.textSize = 30f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Loading Video...", width/2, height/2, paint)
            return
        }

        // Draw Frame
        // scaling logic: Fit Center
        val frame = frameToDraw!!
        val src = Rect(0, 0, frame.width, frame.height)
        
        val videoRatio = frame.width.toFloat() / frame.height
        val canvasRatio = width / height
        
        var dstW = width
        var dstH = height
        
        if (videoRatio > canvasRatio) {
            dstH = width / videoRatio
        } else {
            dstW = height * videoRatio
        }
        
        val cx = width / 2
        val cy = height / 2
        
        val dst = android.graphics.RectF(
            cx - dstW / 2,
            cy - dstH / 2,
            cx + dstW / 2,
            cy + dstH / 2
        )
        
        canvas.drawBitmap(frame, null, dst, paint)
        
        if (isLoading) {
             // Small loading indicator
             paint.color = Color.YELLOW
             canvas.drawCircle(20f, 20f, 5f, paint)
        }
    }
}
