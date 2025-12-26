package com.marsraver.wleddj.engine.animations

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.marsraver.wleddj.engine.Animation
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.engine.color.Palettes
import kotlin.random.Random

/**
 * Matrix Animation - Real Text
 * Renders falling trails of characters using Canvas.
 */
class MatrixAnimation : Animation {

    private var _palette: Palette = Palettes.get("Forest") ?: Palettes.getDefault()
    override var currentPalette: Palette?
        get() = _palette
        set(value) { if (value != null) _palette = value }

    override fun supportsPalette(): Boolean = true

    // Params
    private var paramSpeed: Int = 128
    
    // Grid State
    private class cell {
        var char: String = ""
        var alpha: Int = 0 // 0-255 brightness
        var isLink: Boolean = false // is this the "head"?
        var colorIdx: Int = 0
    }
    
    private var grid: Array<Array<cell>>? = null
    private var cols = 0
    private var rows = 0
    private var drops: IntArray = IntArray(0) // y-position of drop head for each column
    
    private val paint = Paint().apply {
        color = Color.GREEN
        textSize = 40f 
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
        // Maybe make it bold?
        isFakeBoldText = true
    }
    
    // Glyphs: standard + some katakana-ish symbols if available, or just math
    private val glyphs = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ@#$%&<>?=+*^"

    private var lastDrawTime = 0L

    override fun draw(canvas: Canvas, width: Float, height: Float) {
        canvas.drawColor(Color.BLACK)
        
        // Font size based on width/height? Fixed size is safer for readability.
        // Let's target ~20-30 columns
        val charSize = (width / 25f).coerceIn(20f, 60f)
        paint.textSize = charSize
        
        val newCols = (width / charSize).toInt() + 1
        val newRows = (height / charSize).toInt() + 1
        
        // Init Grid if size changes
        if (grid == null || cols != newCols || rows != newRows) {
            initGrid(newCols, newRows)
        }
        
        // Update Logic (throttled slightly? No, smooth is better)
        updateMatrix(newCols, newRows)
        
        // Draw
        val g = grid ?: return
        
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val cell = g[c][r]
                if (cell.alpha > 10) {
                    val x = c * charSize + charSize/2
                    val y = r * charSize + charSize // Baseline
                    
                    // Color
                    // Head is White. Tail is Palette/Green.
                    if (cell.isLink) {
                        paint.color = Color.WHITE
                        paint.alpha = 255
                        // Glow?
                        // paint.setShadowLayer(10f, 0f, 0f, Color.WHITE)
                    } else {
                        // paint.setShadowLayer(0f, 0f, 0f, 0)
                        val palColor = _palette.getInterpolatedInt(cell.colorIdx)
                        // manually apply alpha to palColor
                        // int color = (alpha << 24) | (rgb)
                        paint.color = (cell.alpha shl 24) or (palColor and 0x00FFFFFF)
                    }
                    
                    canvas.drawText(cell.char, x, y, paint)
                }
            }
        }
        // paint.setShadowLayer(0f, 0f, 0f, 0)
    }
    
    private fun initGrid(c: Int, r: Int) {
        cols = c
        rows = r
        grid = Array(cols) { 
            Array(rows) { 
                cell().apply { char = randomGlyph() } 
            } 
        }
        drops = IntArray(cols) { -Random.nextInt(rows) } // Start drops above screen randomly
    }
    
    private fun updateMatrix(c: Int, r: Int) {
        val g = grid ?: return
        
        // Speed: 1 frame every X?
        // Or float position?
        // For grid text, integer steps are usually fine, but let's do probabilistic update
        // based on paramSpeed.
        
        // Chance to update frame: 
        // fast = 1.0 (every frame), slow = 0.1
        // Actually, Matrix usually updates every few frames.
        
        // Let's just update every frame but move drops with a delay counter?
        // Simpler: Move drops every frame? Too fast.
        
        // We need separate tracking or just probability per column.
        
        val updateChance = 0.1f + (paramSpeed / 255f) * 0.4f
        
        for (col in 0 until c) {
            // Random flipping
            if (Random.nextFloat() < 0.05f) { // 5% of columns fli a char somewhere?
                val randRow = Random.nextInt(rows)
                if (g[col][randRow].alpha > 0) {
                     g[col][randRow].char = randomGlyph()
                }
            }
        
            // Move Drop
            // Each column has a drop head at drops[col]
            // We only move it if random < chance, to vary speeds per column
            if (Random.nextFloat() < updateChance) {
                val headPos = drops[col]
                
                // Move head down
                drops[col]++ 
                val newHead = drops[col]
                
                // Activate new head
                if (newHead in 0 until rows) {
                    g[col][newHead].isLink = true // It's the bright head
                    g[col][newHead].alpha = 255
                    g[col][newHead].char = randomGlyph()
                    g[col][newHead].colorIdx = Random.nextInt(255) // assign random color from palette? Or fixed?
                    // Usually matrix is uniform color. Let's use palette index based on column or consistent.
                    g[col][newHead].colorIdx = (col * 10) % 255 // Stripes?
                }
                
                // Previous head becomes tail
                val prevPos = newHead - 1
                if (prevPos in 0 until rows) {
                    g[col][prevPos].isLink = false
                }
                
                // Reset drop if far off bottom
                // Trail length?
                if (newHead > rows + 10) { 
                    drops[col] = -Random.nextInt(20) // Reset to top
                }
            }
            
            // Decay Alpha for entire column
            for (row in 0 until rows) {
                val cell = g[col][row]
                if (!cell.isLink && cell.alpha > 0) {
                    // Fade speed
                    val fade = 5 + (paramSpeed / 20)
                    cell.alpha = (cell.alpha - fade).coerceAtLeast(0)
                }
            }
        }
    }
    
    private fun randomGlyph(): String {
        return glyphs[Random.nextInt(glyphs.length)].toString()
    }
}
