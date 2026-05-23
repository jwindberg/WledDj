package com.marsraver.wleddj.animations

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.marsraver.wleddj.engine.Animation
import java.io.File

class ImageAnimation : Animation {
    private var imagePath: String = ""
    private var bitmap: Bitmap? = null
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    private var lastLoadedPath: String? = null

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        val path = imagePath
        if (path.isNotEmpty() && path != lastLoadedPath) {
            loadBitmap(path)
        }

        val bmp = bitmap
        if (bmp != null) {
            val destRect = RectF(0f, 0f, width, height)
            canvas.drawBitmap(bmp, null, destRect, paint)
        } else {
            // Retro-futuristic dark placeholder with diagonal lines
            val placeholderPaint = Paint().apply {
                color = android.graphics.Color.DKGRAY
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(0f, 0f, width, height, placeholderPaint)
            canvas.drawLine(0f, 0f, width, height, placeholderPaint)
            canvas.drawLine(width, 0f, 0f, height, placeholderPaint)
        }
    }

    private fun loadBitmap(path: String) {
        try {
            val file = File(path)
            if (file.exists()) {
                val loaded = BitmapFactory.decodeFile(file.absolutePath)
                if (loaded != null) {
                    bitmap?.recycle()
                    bitmap = loaded
                    lastLoadedPath = path
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageAnimation", "Failed to load bitmap from: $path", e)
        }
    }

    override fun supportsText(): Boolean = true

    override fun setText(text: String) {
        imagePath = text
    }

    override fun getText(): String {
        return imagePath
    }

    override fun destroy() {
        bitmap?.recycle()
        bitmap = null
        lastLoadedPath = null
    }
}
