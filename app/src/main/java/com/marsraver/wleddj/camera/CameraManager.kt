package com.marsraver.wleddj.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object CameraManager {
    private const val TAG = "CameraManager"
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var executor: ExecutorService? = null
    
    @Volatile
    private var latestFrame: Bitmap? = null
    private val lock = Any()
    
    var isFrontCamera: Boolean = false
        private set

    fun start(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        front: Boolean
    ) {
        synchronized(lock) {
            if (executor == null) {
                executor = Executors.newSingleThreadExecutor()
            }
            isFrontCamera = front
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                synchronized(lock) {
                    cameraProvider = provider
                    provider.unbindAll()

                    val cameraSelector = if (front) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor!!) { imageProxy ->
                        processImageProxy(imageProxy)
                    }

                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        imageAnalysis
                    )
                    Log.d(TAG, "Camera bound successfully. front=$front")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera use cases", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        synchronized(lock) {
            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unbind camera provider", e)
            }
            cameraProvider = null
            
            executor?.shutdown()
            executor = null
            
            latestFrame = null
            Log.d(TAG, "Camera stopped and cleaned up successfully.")
        }
    }

    fun getLatestFrame(): Bitmap? {
        synchronized(lock) {
            return latestFrame
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            val originalBitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            
            val targetSize = 128
            val scale = targetSize.toFloat() / Math.max(originalBitmap.width, originalBitmap.height)
            
            val matrix = Matrix().apply {
                if (scale < 1f) {
                    postScale(scale, scale)
                }
                if (rotationDegrees != 0) {
                    postRotate(rotationDegrees.toFloat())
                }
                if (isFrontCamera) {
                    postScale(-1f, 1f)
                }
            }

            val processedBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )
            
            if (originalBitmap != processedBitmap) {
                originalBitmap.recycle()
            }

            synchronized(lock) {
                latestFrame = processedBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image frame", e)
        } finally {
            imageProxy.close()
        }
    }
}
