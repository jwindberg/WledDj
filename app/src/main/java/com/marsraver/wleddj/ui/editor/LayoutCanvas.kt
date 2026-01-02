package com.marsraver.wleddj.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.marsraver.wleddj.model.Installation
import com.marsraver.wleddj.model.WledDevice
import kotlin.math.roundToInt

@Composable
fun LayoutCanvas(
    installation: Installation,
    selectedDevice: WledDevice?,
    onSelectDevice: (WledDevice?) -> Unit,
    onMoveDevice: (WledDevice, Float, Float, Float, Float, Float) -> Unit,
    onUpdateViewport: (Float, Offset) -> Unit,
    onInteractionEnd: () -> Unit
) {
    val currentOnSelectDeviceState = rememberUpdatedState(onSelectDevice)
    val currentOnMoveDeviceState = rememberUpdatedState(onMoveDevice)
    val currentOnUpdateViewportState = rememberUpdatedState(onUpdateViewport)
    val currentOnInteractionEndState = rememberUpdatedState(onInteractionEnd)
    val updatedInstallation = rememberUpdatedState(installation)

    // Camera State (Virtual Coordinates)
    // Default to Center if null (1000x1000 -> 500,500)
    val defaultCx = installation.width / 2f
    val defaultCy = installation.height / 2f
    
    var zoom by remember(installation.id) { mutableStateOf(installation.cameraZoom) }
    var cx by remember(installation.id) { mutableStateOf(installation.cameraX ?: defaultCx) }
    var cy by remember(installation.id) { mutableStateOf(installation.cameraY ?: defaultCy) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val screenCenter = Offset(screenWidth / 2f, screenHeight / 2f)
        
        // Base Scale (100% Fit based on Width/Height)
        val install = installation
        val baseScale = remember(install.width, install.height, screenWidth, screenHeight) {
            val sx = screenWidth / install.width
            val sy = screenHeight / install.height
            minOf(sx, sy)
        }
        val updatedBaseScale = rememberUpdatedState(baseScale)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(screenWidth, screenHeight) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        
                        // Capture fresh state for this gesture
                        val installation = updatedInstallation.value
                        val baseScale = updatedBaseScale.value
                        
                        // Transform Helper
                        // Screen = (Virtual - Cam) * Scale + ScreenCenter
                        val currentScale = baseScale * zoom
                        
                        fun screenToVirtual(sx: Float, sy: Float): Offset {
                            val vx = (sx - screenCenter.x) / currentScale + cx
                            val vy = (sy - screenCenter.y) / currentScale + cy
                            return Offset(vx, vy)
                        }
                        
                        // 1. HIT TEST
                        val virtualPoint = screenToVirtual(down.position.x, down.position.y)
                        val installDevices: List<WledDevice> = installation.devices
                        val hitDevice = installDevices.find { device ->
                             virtualPoint.x >= device.x && virtualPoint.x <= device.x + device.width &&
                             virtualPoint.y >= device.y && virtualPoint.y <= device.y + device.height
                        }
                        
                        currentOnSelectDeviceState.value(hitDevice)
                        
                        if (hitDevice != null) {
                            // DEVICE DRAG
                            var dragX = hitDevice.x
                            var dragY = hitDevice.y
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.isConsumed }) break
                                val panChange = event.calculatePan()
                                if (panChange != Offset.Zero) {
                                     val dx = panChange.x / currentScale
                                     val dy = panChange.y / currentScale
                                     dragX += dx
                                     dragY += dy
                                     
                                     // --- PIXEL GRID SNAPPING ---
                                     // Snap to the extended pixel grid of other devices
                                     val snapDistScreen = 30f // Snap threshold in screen pixels
                                     val snapDist = snapDistScreen / currentScale
                                     
                                     val otherDevices = installation.devices.filter { it != hitDevice }
                                     
                                     var bestX = dragX
                                     var minDX = Float.MAX_VALUE
                                     
                                     // X AXIS SNAPPING
                                     otherDevices.forEach { target ->
                                         // Determine Target Step X
                                         val tCols = if (target.segmentWidth > 0) target.segmentWidth else target.pixelCount
                                         val tStepX = target.width / tCols.toFloat()
                                         
                                         if (tStepX > 0) {
                                             // Find nearest grid line (extended indefinitely) relative to target.x
                                             // We want (dragX - target.x) to be a multiple of tStepX
                                             val k = ((dragX - target.x) / tStepX).roundToInt()
                                             val snapTargetX = target.x + k * tStepX
                                             
                                             val diff = kotlin.math.abs(dragX - snapTargetX)
                                             if (diff < snapDist && diff < minDX) {
                                                 minDX = diff
                                                 bestX = snapTargetX
                                             }
                                         }
                                     }
                                     
                                     var bestY = dragY
                                     var minDY = Float.MAX_VALUE
                                     
                                     // Y AXIS SNAPPING
                                     otherDevices.forEach { target ->
                                         // Determine Target Step Y
                                         var tRows = 1
                                         if (target.segmentWidth > 0) {
                                             tRows = (target.pixelCount + target.segmentWidth - 1) / target.segmentWidth
                                         }
                                         val tStepY = target.height / tRows.toFloat()
                                         
                                         if (tStepY > 0) {
                                             // Find nearest grid line relative to target.y
                                             val k = ((dragY - target.y) / tStepY).roundToInt()
                                             val snapTargetY = target.y + k * tStepY
                                             
                                             val diff = kotlin.math.abs(dragY - snapTargetY)
                                             if (diff < snapDist && diff < minDY) {
                                                 minDY = diff
                                                 bestY = snapTargetY
                                             }
                                         }
                                     }
                                     
                                     // If magnet didn't catch, OPTIONAL: Keep weak grid snap? 
                                     // For now, let's allow free movement if no magnet, to allow fine tuning.
                                     // Or fallback to 1px/5px grid if desired. 
                                     // Let's stick to PURE MAGNET + FREE for maximum precision.

                                     currentOnMoveDeviceState.value(hitDevice, bestX, bestY, hitDevice.width, hitDevice.height, hitDevice.rotation)
                                     event.changes.forEach { it.consume() }
                                }
                                if (!event.changes.any { it.pressed }) {
                                    currentOnInteractionEndState.value()
                                    break
                                }
                            }
                        } else {
                            // CAMERA PAN/ZOOM
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.isConsumed }) break
                                
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = true)
                                
                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                     // 1. Update Zoom
                                     val oldZoom = zoom
                                     zoom *= zoomChange
                                     zoom = zoom.coerceIn(0.1f, 20f)
                                     
                                     // 2. Update Camera Position (Pan)
                                     // Standard: Dragging screen px moves camera inverse.
                                     // cx -= dx / scale
                                     val effectiveScale = baseScale * zoom
                                     cx -= panChange.x / effectiveScale
                                     cy -= panChange.y / effectiveScale
                                     
                                     // 3. Zoom Around Centroid
                                     if (zoomChange != 1f) {
                                         val offX = centroid.x - screenCenter.x
                                         val offY = centroid.y - screenCenter.y
                                         val oldS = baseScale * oldZoom
                                         val newS = baseScale * zoom
                                         // Shift camera to keep point under centroid stable
                                         val shiftX = offX * (1/oldS - 1/newS)
                                         val shiftY = offY * (1/oldS - 1/newS)
                                         cx -= shiftX
                                         cy -= shiftY
                                     }
                                     
                                     currentOnUpdateViewportState.value(zoom, Offset(cx, cy))
                                     event.changes.forEach { it.consume() }
                                }
                                
                                if (!event.changes.any { it.pressed }) {
                                    currentOnInteractionEndState.value()
                                    break
                                }
                            }
                        }
                     }
                }
        ) {
            // DRAW
            val totalScale = baseScale * zoom
            
            withTransform({
                translate(left = screenCenter.x, top = screenCenter.y)
                scale(totalScale, totalScale, pivot = Offset.Zero)
                translate(left = -cx, top = -cy)
            }) {
                // Draw Devices
                installation.devices.forEach { device ->
                    val isSelected = device == selectedDevice
                    drawRect(
                        // Replaced hardcoded properties with semi-constants or Theme-ish values
                        color = if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.7f) else Color(0xFF2196F3).copy(alpha = 0.5f),
                        topLeft = Offset(device.x, device.y),
                        size = Size(device.width, device.height)
                    )
                     drawRect(
                        color = if (isSelected) Color.Yellow else Color.White,
                        style = Stroke(width = (if (isSelected) 4f else 2f) / totalScale),
                        topLeft = Offset(device.x, device.y),
                        size = Size(device.width, device.height)
                     )
                     // Determine grid dimensions
                     var cols = device.segmentWidth
                     var rows = 1
                     if (cols <= 0) {
                        // Linear Strip
                        cols = device.pixelCount
                        rows = 1
                     } else {
                        // Matrix
                        rows = (device.pixelCount + cols - 1) / cols
                     }
                     
                     val stepX = device.width / cols
                     val stepY = device.height / rows
                     
                     for(i in 0 until cols) {
                        for(j in 0 until rows) {
                             if (j * cols + i < device.pixelCount) {
                                 drawCircle(
                                     color = Color.White.copy(alpha = 0.3f),
                                     radius = 2f / totalScale,
                                     center = Offset(device.x + stepX * i + stepX/2, device.y + stepY * j + stepY/2)
                                 )
                             }
                        }
                     }
                }
            }
        }
    }
}
