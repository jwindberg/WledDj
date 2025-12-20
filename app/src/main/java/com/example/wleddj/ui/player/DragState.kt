package com.example.wleddj.ui.player

import androidx.compose.ui.geometry.Offset
import android.graphics.RectF

class DragState {
    var targetRegionId: String? = null
    var mode: String = "NONE" // NONE, MOVE, RESIZE, ROTATE
    var startRect = RectF()
    var startRotation = 0f
    var startTouch = Offset.Zero
    
    var accumulatedX = 0f
    var accumulatedY = 0f
}
