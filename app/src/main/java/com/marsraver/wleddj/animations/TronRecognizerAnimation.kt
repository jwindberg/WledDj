package com.marsraver.wleddj.animations

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.R
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.*

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Tron Recognizer animation - a 3D recognizer swooping in and out of frame.
 * Ported from LedFx (Java AWT) to Android Canvas.
 */
class TronRecognizerAnimation(private val context: Context) : Animation, CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    // Recognizer 3D model data
    private companion object {
        const val RECOGNIZER_COLOR_HEX = 0xFF00FFFF.toInt() // Cyan
        const val OUTLINE_COLOR_VAL = android.graphics.Color.WHITE
        const val STROKE_WIDTH = 3f
        const val TEXT_SIZE = 60f
        const val CAMERA_DISTANCE = 400f
        const val MODEL_SIZE = 200f
        const val FRICTION = 1.0f
        const val MOMENTUM_SCALE = 0.15f
    }

    private data class Vertex3D(val x: Float, val y: Float, val z: Float)
    private data class Edge(val v1: Int, val v2: Int)
    private data class Face(val vertexIndices: List<Int>)


    // Animation state
    private var startTime: Long = 0


    // Tron colors
    // Cyan: 0, 255, 255
    private val recognizerColor = RECOGNIZER_COLOR_HEX
    // White outline
    private val outlineColor = OUTLINE_COLOR_VAL

    private val fillPaint = Paint().apply { 
        style = Paint.Style.FILL 
        isAntiAlias = true
    }
    
    private val strokePaint = Paint().apply { 
        style = Paint.Style.STROKE 
        color = outlineColor
        strokeWidth = STROKE_WIDTH
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }



    // Thread-safe state
    @Volatile private var loadedVertices: List<Vertex3D> = emptyList()
    @Volatile private var loadedFaces: List<Face> = emptyList()
    @Volatile private var isLoading = false
    @Volatile private var loadingError: String? = null

    // Helper for loading paint
    private val textPaint = Paint().apply {
        color = Color.CYAN
        textSize = TEXT_SIZE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // Physics State
    private var posX = 0f
    private var posY = 0f
    private var posZ = -250f 
    
    // Euler Angles (Radians)
    // Reset to 0 for calibration
    private var rotX = 0f
    private var rotY = 0f
    private var rotZ = 0f
    
    // Initial spin
    private var velRotX = 0f 
    private var velRotY = 0f 
    
    // Touch State (Thread Safe)
    @Volatile private var lastTouchTime = 0L
    @Volatile private var isTouching = false
    @Volatile private var lastTouchX = 0f
    @Volatile private var lastTouchY = 0f
    
    // Smooth averages for fling
    @Volatile private var deltaX = 0f
    @Volatile private var deltaY = 0f

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Trigger Async Load once
        if (loadedVertices.isEmpty() && !isLoading) {
            isLoading = true
            launch(Dispatchers.IO) {
                loadModelAsync()
            }
        }

        // Initialize Position logic once we know width/height
        if (startTime == 0L) {
             startTime = System.currentTimeMillis()
             posX = width / 2f
             posY = height / 2f
        }

        // Clear background
        canvas.drawColor(Color.BLACK)

        // Show Loading/Error state if empty
        if (loadedVertices.isEmpty()) {
            if (loadingError != null) {
                textPaint.color = Color.RED
                canvas.drawText("Error: $loadingError", width / 2f, height / 2f, textPaint)
            } else {
                textPaint.color = Color.CYAN
                // Pulse loading text
                val alpha = (sin(System.currentTimeMillis() / 200.0) * 127 + 128).toInt()
                textPaint.alpha = alpha
                canvas.drawText("LOADING...", width / 2f, height / 2f, textPaint)
            }
            return
        }

        try {
            val currentTime = System.currentTimeMillis()
            val dt = 1.0f / 30.0f // Approx delta time (or calculate real)

            // Touch Timeout Logic (Detect Release)
            // Reduced to 60ms for snappier response
            if (isTouching && (currentTime - lastTouchTime > 60)) {
                // User stopped touching > 60ms ago -> Release
                isTouching = false
                
                // Apply Fling Momentum
                // Use smoothed cumulative delta for more consistent fling
                // Inverted for consistency
                velRotY = -deltaX * MOMENTUM_SCALE 
                velRotX = -deltaY * MOMENTUM_SCALE 
            }

            if (!isTouching) {
                // Free Spin Physics
                rotX += velRotX * dt
                rotY += velRotY * dt
                
                // Friction
                velRotX *= FRICTION
                velRotY *= FRICTION
            } else {
                // Decay delta if holding still
                if (currentTime - lastTouchTime > 50) {
                     deltaX *= 0.5f
                     deltaY *= 0.5f
                }
            }
            
            // Pass local references to ensure consistency during this frame
            drawRecognizer(
                canvas, 
                loadedVertices, 
                loadedFaces, 
                width / 2f, height / 2f, posZ, // Always centered 
                rotX, rotY, rotZ, 
                width, height
            )

        } catch (e: Exception) {
            e.printStackTrace()
            // Don't crash visuals on transient errors
        }
    }
    
    override fun onTouch(x: Float, y: Float): Boolean {
        // We still need onTouch for basic "Is touching" state detection 
        // which drives the physics loop (friction/timeout).
        // But the actual rotation update now happens mainly in onTransform for interactive mode.
        // However, PlayerScreen passes BOTH onTouch and onTransform events.
        // We should use onTouch ONLY for state management (Up/Down) and coordinate tracking for fling start?
        
        val now = System.currentTimeMillis()
        if (!isTouching) {
            isTouching = true
            // Reset state
            deltaX = 0f; deltaY = 0f
            velRotX = 0f; velRotY = 0f
            lastTouchX = x; lastTouchY = y
        } else {
            // If we receive pure touch move (no transform yet or single finger),
            // calculate delta here as fallback?
            // PlayerScreen logic handles it:
            // "onInteract" (onTouch) is called for all pointer events.
            // "onTransform" is called for detected gestures.
            
            // If we rely on onTransform for rotation, we should ignore movement here?
            // Actually, for single finger, onTransform probably won't trigger "rotation".
            // But it DOES trigger "pan".
            
            // Let's rely on onTransform for ALL movement if it's reliable.
            // But `awaitEachGesture` in PlayerScreen sends absolute positions.
            // `detectTransformGestures` sends deltas.
            
            // If we rely on absolute diffs here, it's robust for single finger.
            // If we use onTransform, it's robust for multi-touch.
            
            // Strategy: Use onTouch ONLY for "Down" (Start) and "Up" (Timeout) detection.
            // Use onTransform for ALL rotation/pan application.
            // But wait, `detectTransformGestures` doesn't strictly guarantee to fire on single tiny moves as robustly as raw touch?
            // It does.
            
            lastTouchX = x
            lastTouchY = y
        }
        lastTouchTime = now
        return true
    }

    override fun onTransform(panX: Float, panY: Float, zoom: Float, rotation: Float): Boolean {
        // Rotation (Degrees to Radians)
        // 2-Finger Rotation -> Roll (Z)
        if (abs(rotation) > 0.1f) {
           rotZ += Math.toRadians(rotation.toDouble()).toFloat()
        }
        
        // Zoom (Scale) -> Move Z (Dolly)
        if (zoom != 1.0f) {
             // Zoom is a multiplier (e.g. 1.01 or 0.99)
             // We can map this to posZ movement
             // Current posZ starts at -250. 
             // Zooming IN (>1) should increase Z (closer to 0).
             // Zooming OUT (<1) should decrease Z.
             
             // posZ += (zoom - 1f) * 500f 
             // Or safer: scale relative depth
             posZ += (zoom - 1f) * 1000f
             
             // Clamp range to prevent disappearing
             posZ = posZ.coerceIn(-2000f, -50f)
        }
        
        // Pan (Pixels) -> Pitch/Yaw (X/Y)
        // Inverted inputs per user feedback
        if (abs(panX) > 0.1f || abs(panY) > 0.1f) {
            val dX = panX
            val dY = panY
            
            rotY -= dX * 0.01f
            rotX -= dY * 0.01f
            
            // Update smoothed delta for Fling
            // Note: onTransform gives delta directly! No need to diff lastTouchX.
            deltaX = deltaX * 0.5f + dX * 0.5f
            deltaY = deltaY * 0.5f + dY * 0.5f
        }
        
        // Keep touching state alive
        lastTouchTime = System.currentTimeMillis()
        if (!isTouching) isTouching = true
        
        return true
    }

    // Load Model (Already background safe usage, but called via launch now)
    private fun loadModelAsync() {
        try {
            val inputStream = context.resources.openRawResource(R.raw.tron_recognizer)
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            val tempVertices = mutableListOf<Vertex3D>()
            val tempFaces = mutableListOf<Face>()
            
            // Temporary lists for parsing
            val rawVertices = mutableListOf<Vertex3D>()

            reader.useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("v ") -> {
                            val parts = trimmed.split("\\s+".toRegex())
                            if (parts.size >= 4) {
                                val x = parts[1].toFloatOrNull() ?: 0f
                                val y = parts[2].toFloatOrNull() ?: 0f
                                val z = parts[3].toFloatOrNull() ?: 0f
                                rawVertices.add(Vertex3D(x, y, z))
                            }
                        }
                        trimmed.startsWith("f ") -> {
                            val parts = trimmed.split("\\s+".toRegex())
                            if (parts.size >= 4) {
                                val faceIndices = mutableListOf<Int>()
                                for (i in 1 until parts.size) {
                                    val vertexPart = parts[i].split("/")[0]
                                    val index = vertexPart.toIntOrNull()?.minus(1)
                                    if (index != null && index >= 0) {
                                        faceIndices.add(index)
                                    }
                                }
                                if (faceIndices.size >= 3) {
                                    tempFaces.add(Face(faceIndices))
                                }
                            }
                        }
                    }
                }
            }

            if (rawVertices.isEmpty()) {
                loadingError = "Empty OBJ File"
                return
            }

            // Normalize and Scale (in background)
            var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
            var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
            var minZ = Float.MAX_VALUE; var maxZ = Float.MIN_VALUE

            rawVertices.forEach { v ->
                minX = min(minX, v.x); maxX = max(maxX, v.x)
                minY = min(minY, v.y); maxY = max(maxY, v.y)
                minZ = min(minZ, v.z); maxZ = max(maxZ, v.z)
            }

            val maxDim = max(maxX - minX, max(maxY - minY, maxZ - minZ))
            val targetSize = MODEL_SIZE
            val scale = if (maxDim > 0.001f) targetSize / maxDim else 1.0f

            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f
            val centerZ = (minZ + maxZ) / 2f

            // Transform vertices once
            tempVertices.addAll(rawVertices.map { 
                Vertex3D(
                    (it.x - centerX) * scale,
                    // Flip 180 from previous (Head Down -> Head Up)
                    // Previous: Y=Z, Z=-Y (Rot -90 X)
                    // New: Y=-Z, Z=Y (Rot +90 X)
                    -(it.z - centerZ) * scale, 
                    (it.y - centerY) * scale
                ) 
            })

            // Atomic Swap
            loadedVertices = tempVertices
            loadedFaces = tempFaces
            
            android.util.Log.d("TronAnim", "Model Loaded: ${tempVertices.size} verts")

        } catch (e: Exception) {
            e.printStackTrace()
            loadingError = e.message ?: "Unknown Load Error"
        } finally {
            isLoading = false
        }
    }

    private fun drawRecognizer(
        canvas: Canvas,
        vertices: List<Vertex3D>, // Pass data explicitly
        faces: List<Face>,
        centerX: Float, centerY: Float, centerZ: Float,
        rotX: Float, rotY: Float, rotZ: Float,
        width: Float, height: Float
    ) {
        // 1. Project Vertices
        val projectedVertices = vertices.map { vertex ->
            var x = vertex.x
            var y = vertex.y
            var z = vertex.z

            // 1. Project Vertices
            // Order: Rotate X (Pitch) -> Rotate Y (Global Yaw) -> Rotate Z (Roll)
            // This order prevents "Roll" behavior when the object is pitched up.

            // Rotate X
            val cosX = cos(rotX.toDouble()).toFloat()
            val sinX = sin(rotX.toDouble()).toFloat()
            val y1 = y * cosX - z * sinX
            val z1 = y * sinX + z * cosX

            // Rotate Y
            val cosY = cos(rotY.toDouble()).toFloat()
            val sinY = sin(rotY.toDouble()).toFloat()
            val x1 = x * cosY - z1 * sinY
            val z2 = x * sinY + z1 * cosY

            // Rotate Z
            val cosZ = cos(rotZ.toDouble()).toFloat()
            val sinZ = sin(rotZ.toDouble()).toFloat()
            val x2 = x1 * cosZ - y1 * sinZ
            val y2 = x1 * sinZ + y1 * cosZ

            // Translate
            val worldX = x2 + centerX
            val worldY = y2 + centerY
            val worldZ = z2 + centerZ

            // Perspective
            val cameraDistance = CAMERA_DISTANCE
            val depth = -worldZ 
            val perspective = cameraDistance / (cameraDistance + depth)

            val screenX = (worldX - centerX) * perspective + centerX
            val screenY = (worldY - centerY) * perspective + centerY

            Triple(screenX, screenY, worldZ)
        }

        // 2. Sort Faces (Painter's Algorithm)
        val sortedFaces = faces.map { face ->
            var totalDepth = 0f
            var validCount = 0
            for (index in face.vertexIndices) {
                 if (index >= 0 && index < projectedVertices.size) {
                     totalDepth += projectedVertices[index].third
                     validCount++
                 }
            }
            // Avoid division by zero
            val avgDepth = if (validCount > 0) totalDepth / validCount else Float.MAX_VALUE
            Pair(face, avgDepth)
        }.sortedByDescending { it.second } 

        // 3. Draw Faces
        val path = Path() 
        
        for ((face, depth) in sortedFaces) {
            if (depth > 50f) continue 

            val facePoints = mutableListOf<Pair<Float, Float>>()
            var allOnScreen = false

            for (index in face.vertexIndices) {
                if (index >= 0 && index < projectedVertices.size) {
                    val proj = projectedVertices[index]
                    val x = proj.first
                    val y = proj.second
                    facePoints.add(x to y)

                    if (x >= 0 && x < width && y >= 0 && y < height) {
                        allOnScreen = true
                    }
                }
            }

            if (facePoints.size < 3 || !allOnScreen) continue

            // Build Path
            path.reset() 
            path.moveTo(facePoints[0].first, facePoints[0].second)
            for (i in 1 until facePoints.size) {
                path.lineTo(facePoints[i].first, facePoints[i].second)
            }
            path.close()

            // Calculate Brightness
            val depthFactor = if (depth < 0) {
                 ((-depth) / 400f).coerceIn(0.5f, 1.5f)
            } else {
                 (1f - depth / 400f).coerceIn(0.3f, 1.0f)
            }
            val brightness = (depthFactor * 0.8f + 0.2f).coerceIn(0f, 1f)
            
            val r = (Color.red(recognizerColor) * brightness).toInt().coerceIn(0, 255)
            val g = (Color.green(recognizerColor) * brightness).toInt().coerceIn(0, 255)
            val b = (Color.blue(recognizerColor) * brightness).toInt().coerceIn(0, 255)
            
            fillPaint.color = Color.rgb(r, g, b)
            canvas.drawPath(path, fillPaint)
            
            strokePaint.color = outlineColor 
            canvas.drawPath(path, strokePaint)
        }
    }

    override fun destroy() {
       job.cancel()
    }
}
