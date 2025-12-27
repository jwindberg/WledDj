package com.marsraver.wleddj.engine

import android.graphics.Canvas
import android.graphics.Color
import com.marsraver.wleddj.engine.color.Palette

interface Animation {
    fun draw(canvas: Canvas, width: Float, height: Float)
    fun onTouch(x: Float, y: Float): Boolean { return false }
    fun onTransform(panX: Float, panY: Float, zoom: Float, rotation: Float): Boolean { return false }
    fun destroy() {}

    // Capabilities
    fun supportsPrimaryColor(): Boolean = false
    fun supportsSecondaryColor(): Boolean = false
    fun supportsPalette(): Boolean = false
    fun supportsText(): Boolean = false

    // State Accessors
    var primaryColor: Int
        get() = Color.WHITE
        set(value) {}

    var secondaryColor: Int
        get() = Color.BLACK
        set(value) {}

    var currentPalette: Palette?
        get() = null
        set(value) {}

    fun setText(text: String) {}
    fun getText(): String = ""
}
