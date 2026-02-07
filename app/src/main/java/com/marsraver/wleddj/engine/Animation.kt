package com.marsraver.wleddj.engine

import android.graphics.Canvas
import android.graphics.Color
import com.marsraver.wleddj.engine.color.Palette

interface Animation {
    fun draw(canvas: Canvas, width: Float, height: Float)
    fun onTouch(x: Float, y: Float): Boolean { return false }
    fun onTransform(panX: Float, panY: Float, zoom: Float, rotation: Float): Boolean { return false }
    fun onInteractionEnd() {}
    fun onCommand(cmd: String) {}
    fun destroy() {}

    // Hit Testing
    fun ignoresBounds(): Boolean = false

    // Capabilities
    fun supportsPrimaryColor(): Boolean = false
    fun supportsSecondaryColor(): Boolean = false
    fun supportsPalette(): Boolean = false
    fun supportsText(): Boolean = false
    fun supportsSpeed(): Boolean = false
    fun setSpeed(speed: Float) {}
    fun getSpeed(): Float = 0.5f

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
