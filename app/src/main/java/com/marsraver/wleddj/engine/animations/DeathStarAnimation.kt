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
        val retriever = MediaMetadataRetriever()
        try {
            val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.death_star_run}")
            retriever.setDataSource(context, uri)
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            
            if (durationMs == 0L) {
                isLoading = false
                return
            }

            // Extract frames
            val intervalUs = (1000000 / TARGET_FPS).toLong()
            var timeUs = 0L
            
            // Loop until end
            while (timeUs < durationMs * 1000) {
                // Get Frame
                // Option: getScaledFrameAtTime (API 27+) is better but let's stick to getFrameAtTime for compat/stability
                // Actually, resizing afterwards is safer if API level is unknown (though Project is likely 24+)
                val rawFrame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                
                if (rawFrame != null) {
                    // Resize to save memory
                    val scale = TARGET_SIZE.toFloat() / maxOf(rawFrame.width, rawFrame.height)
                    val w = (rawFrame.width * scale).roundToInt()
                    val h = (rawFrame.height * scale).roundToInt()
                    val scaled = Bitmap.createScaledBitmap(rawFrame, w, h, true)
                    
                    frames.add(scaled)
                    if (scaled != rawFrame) rawFrame.recycle()
                } else {
                     break // End of stream or error
                }
                
                timeUs += intervalUs
            }

        } catch (e: Exception) {
            Log.e("DeathStarAnim", "Error loading video", e)
        } finally {
            retriever.release()
            isLoading = false
            startTime = System.currentTimeMillis() // Reset start on ready
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
