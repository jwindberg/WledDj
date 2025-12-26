package com.marsraver.wleddj.engine.animations

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import com.marsraver.wleddj.engine.color.RgbColor
import kotlin.math.roundToInt

/**
 * Base class for pixel-based animations migrated from WledFx.
 * Provides a virtual grid (width/height) and helper methods to manipulate pixels.
 */
abstract class BasePixelAnimation : Animation {

    protected var width: Int = 0
    protected var height: Int = 0
    protected var pixelCount: Int = 0
    
    // Internal buffer
    protected var pixels: IntArray = IntArray(0)
    private var bitmap: Bitmap? = null
    private val paint = Paint()
    private val destRect = Rect()
    
    private var isInitialized = false

    // Params
    protected var paramSpeed: Int = 128
    protected var paramIntensity: Int = 128
    
    // Backing fields
    // Backing fields
    private var _primaryColor: Int = getDefaultPrimaryColor()
    private var _secondaryColor: Int = Color.BLACK
    private var _palette: Palette = Palettes.get(getDefaultPaletteName()) ?: Palettes.getDefault()

    /**
     * Override to specify a different default palette for this animation.
     */
    open fun getDefaultPaletteName(): String = Palettes.DEFAULT_PALETTE_NAME

    /**
     * Override to specify a different default primary color.
     */
    open fun getDefaultPrimaryColor(): Int = Color.WHITE

    // ParamColor matches PrimaryColor for legacy/WledFx compat
    protected var paramColor: Int
        get() = _primaryColor
        set(value) { _primaryColor = value }

    // Animation Interface Implementation
    override var primaryColor: Int
        get() = _primaryColor
        set(value) { _primaryColor = value }

    override var secondaryColor: Int
        get() = _secondaryColor
        set(value) { _secondaryColor = value }

    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    // Capability Defaults (Subclasses override)
    override fun supportsPrimaryColor(): Boolean = false
    override fun supportsSecondaryColor(): Boolean = false
    override fun supportsPalette(): Boolean = false
    
    // Abstract Init
    abstract fun onInit()
    
    // Abstract Update (returns true if something changed)
    abstract fun update(now: Long): Boolean

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        // Init if needed or resized (though WledDj uses fixed regions per instance usually)
        // For simplicity, we assume fixed virtual resolution or 1:1 if desired.
        // Let's use a standard low-res grid for defaults if not specified? 
        // No, let's use a reasonable default like 64x64 or 32x32 unless set.
        // Or adapt to the region size?
        // WledFx animations often expect 1D arrays or specific 2D logic.
        
        if (!isInitialized) {
            // Default 64x64 unless subclass sets different
            init(64, 64) 
        }
        
        // Run update logic
        update(System.nanoTime())
        
        // Render
        bitmap?.let { bmp ->
            bmp.setPixels(pixels, 0, this.width, 0, 0, this.width, this.height)
            destRect.set(0, 0, width.toInt(), height.toInt())
            canvas.drawBitmap(bmp, null, destRect, paint)
        }
    }
    
    protected fun init(w: Int, h: Int) {
        this.width = w
        this.height = h
        this.pixelCount = w * h
        this.pixels = IntArray(pixelCount)
        this.bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        
        onInit()
        isInitialized = true
    }

    // --- Pixel Manipulation Helpers ---

    open fun setPixelColor(x: Int, y: Int, color: Int) {
        if (x in 0 until width && y in 0 until height) {
            pixels[y * width + x] = color
        }
    }

    open fun getPixelColor(x: Int, y: Int): Int {
        if (x in 0 until width && y in 0 until height) {
            return pixels[y * width + x]
        }
        return Color.BLACK
    }
    
    // Index based
    open fun setPixelColor(index: Int, color: Int) {
        if (index in 0 until pixelCount) {
            pixels[index] = color
        }
    }
    
    fun getPixelColor(index: Int): Int {
        if (index in 0 until pixelCount) return pixels[index]
        return Color.BLACK
    }

    // --- Effects Helpers ---
    
    fun fadeToBlackBy(amount: Int) {
        for (i in pixels.indices) {
            pixels[i] = fadeColor(pixels[i], amount)
        }
    }
    
    // Scale 8-bit channel down by (255-scale)
    protected open fun fadeColor(color: Int, amount: Int): Int {
        // amount 255 = fade completely?
        // FastLED: nscale8 behavior. 
        // WledFx usually implies "amount to subtract" or "scale down"?
        // Typically fadeToBlackBy(x) means scale by (255-x).
        
        val scale = (255 - amount).coerceAtLeast(0)
        
        val a = Color.alpha(color)
        val r = (Color.red(color) * scale) shr 8
        val g = (Color.green(color) * scale) shr 8
        val b = (Color.blue(color) * scale) shr 8
        
        return Color.argb(a, r, g, b)
    }
    
    fun blur2d(amount: Int) {
         val blurAmount = amount.coerceIn(0, 255)
         if (blurAmount == 0) return

         val w = width
         val h = height
         val src = pixels.clone() // Scratch copy

         for (y in 0 until h) {
             for (x in 0 until w) {
                 var rSum = 0; var gSum = 0; var bSum = 0; var count = 0
                 
                 // 3x3 Kernel
                 for (ky in -1..1) {
                     for (kx in -1..1) {
                         val nx = x + kx
                         val ny = y + ky
                         if (nx in 0 until w && ny in 0 until h) {
                             val col = src[ny * w + nx]
                             rSum += (col shr 16) and 0xFF // Color.red
                             gSum += (col shr 8) and 0xFF  // Color.green
                             bSum += col and 0xFF          // Color.blue
                             count++
                         }
                     }
                 }
                 
                 val avgR = rSum / count
                 val avgG = gSum / count
                 val avgB = bSum / count
                 
                 val cur = src[y * w + x]
                 val curR = (cur shr 16) and 0xFF
                 val curG = (cur shr 8) and 0xFF
                 val curB = cur and 0xFF
                 
                 // Lerp
                 val newR = (curR * (255 - blurAmount) + avgR * blurAmount) shr 8
                 val newG = (curG * (255 - blurAmount) + avgG * blurAmount) shr 8
                 val newB = (curB * (255 - blurAmount) + avgB * blurAmount) shr 8
                 
                 pixels[y * w + x] = Color.rgb(newR, newG, newB)
             }
         }
    }
    
    // HSV Helper
    fun hsvToRgb(h: Int, s: Int, v: Int): Int {
        val hf = (h % 256) / 255.0f * 360.0f
        val sf = s / 255.0f
        val vf = v / 255.0f
        return Color.HSVToColor(floatArrayOf(hf, sf, vf))
    }
    
    fun colorWheel(pos: Int): Int {
        var p = pos and 0xFF
        return if (p < 85) {
            Color.rgb(p * 3, 255 - p * 3, 0)
        } else if (p < 170) {
            p -= 85
            Color.rgb(255 - p * 3, 0, p * 3)
        } else {
            p -= 170
            Color.rgb(0, p * 3, 255 - p * 3)
        }
    }

    // Palette Helper
    fun getColorFromPalette(index: Int): Int {
        // Use interpolated lookup for smooth blending
        return _palette.getInterpolatedInt(index)
    }
}
