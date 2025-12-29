package com.marsraver.wleddj.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import com.marsraver.wleddj.model.AnimationRegion
import com.marsraver.wleddj.model.Installation
import com.marsraver.wleddj.engine.RenderEngine

class CanvasGeometry {
    var rootOffset: Offset = Offset.Zero
    var viewWidth: Float = 1f
    var viewHeight: Float = 1f
    
    // Legacy / Convenience for RectF
    fun screenToVirtual(screenPos: Offset, installW: Float, installH: Float, zoom: Float = 1f, cx: Float? = null, cy: Float? = null): android.graphics.RectF? {
        val pt = screenToVirtualPoint(screenPos.x, screenPos.y, installW, installH, zoom, cx, cy) ?: return null
        val size = 300f
        return android.graphics.RectF(pt.x - size/2, pt.y - size/2, pt.x + size/2, pt.y + size/2)
    }

    fun screenToVirtualPoint(screenX: Float, screenY: Float, installW: Float, installH: Float, zoom: Float = 1f, cx: Float? = null, cy: Float? = null): Offset? {
        val localX = screenX - rootOffset.x
        val localY = screenY - rootOffset.y
        
        // Check Bounds of Viewport
        if (localX < 0 || localX > viewWidth || localY < 0 || localY > viewHeight) {
            return null
        }
        
        val scaleX = viewWidth / installW
        val scaleY = viewHeight / installH
        val baseScale = minOf(scaleX, scaleY)
        val totalScale = baseScale * zoom
        
        val centerX = cx ?: (installW / 2f)
        val centerY = cy ?: (installH / 2f)
        
        val screenCenterX = viewWidth / 2f
        val screenCenterY = viewHeight / 2f
        
        val virtualX = (localX - screenCenterX) / totalScale + centerX
        val virtualY = (localY - screenCenterY) / totalScale + centerY
        
        return Offset(virtualX, virtualY)
    }
}

@Composable
fun InteractivePlayerCanvas(
    frame: RenderEngine.PreviewFrame,
    regions: List<AnimationRegion>,
    installation: Installation,
    deviceStatuses: Map<String, Boolean>,
    canvasGeometry: CanvasGeometry,
    isInteractive: Boolean,
    selectedRegionId: String?,
    onSelectRegion: (String?) -> Unit,
    onUpdateRegion: (String, android.graphics.RectF, Float) -> Unit,
    onRemoveRegion: (String) -> Unit,
    onInteract: (Float, Float) -> Unit,
    onTransform: (Float, Float, Float, Float, Float, Float) -> Unit,
    onInteractionEnd: () -> Unit
) {
    // No null check needed really, caller handles it

    val currentRegionsState = rememberUpdatedState(regions)
    val currentSelectedIdState = rememberUpdatedState(selectedRegionId)
    val currentOnUpdateState = rememberUpdatedState(onUpdateRegion)
    val currentOnSelectState = rememberUpdatedState(onSelectRegion)
    val currentOnInteractionEnd = rememberUpdatedState(onInteractionEnd)
    
    val currentInstallationState = rememberUpdatedState(installation)
    val currentOnInteractState = rememberUpdatedState(onInteract)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isInteractive, canvasGeometry.viewWidth, canvasGeometry.viewHeight) {
                 awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val localX = down.position.x
                        val localY = down.position.y
                        
                        val inst = currentInstallationState.value
                        
                        // Geometry
                        val viewW = canvasGeometry.viewWidth
                        val viewH = canvasGeometry.viewHeight
                        
                        // Calculations using FRESH installation state
                        val sx = viewW / inst.width
                        val sy = viewH / inst.height
                        val baseScale = minOf(sx, sy)
                        
                        val zoom = inst.cameraZoom
                        val cx = inst.cameraX ?: (inst.width / 2f)
                        val cy = inst.cameraY ?: (inst.height / 2f)
                        val currentScale = baseScale * zoom
                        
                        val screenCenter = Offset(viewW / 2f, viewH / 2f)
                        
                        // Helper
                        fun screenToVirtual(touchX: Float, touchY: Float): Offset {
                            val vx = (touchX - screenCenter.x) / currentScale + cx
                            val vy = (touchY - screenCenter.y) / currentScale + cy
                            return Offset(vx, vy)
                        }

                        // Normal Touch Logic
                        if (isInteractive) {
                               // In Interactive Mode (Performance), we trigger onInteract for "Touch" pos.
                               val rootOff = canvasGeometry.rootOffset
                               currentOnInteractState.value(localX + rootOff.x, localY + rootOff.y)
                               
                               // Loop to track dragging
                               do {
                                   val event = awaitPointerEvent()
                                   val change = event.changes.firstOrNull()
                                   if (change != null) {
                                       val curX = change.position.x
                                       val curY = change.position.y
                                       currentOnInteractState.value(curX + rootOff.x, curY + rootOff.y)
                                   }
                               } while (event.changes.any { it.pressed })
                               
                        } else {
                               // EDIT MODE LOGIC (Selection / Dragging Regions)
                               val virtualPoint = screenToVirtual(down.position.x, down.position.y)
                               val virtX = virtualPoint.x
                               val virtY = virtualPoint.y
                               
                               val regs = currentRegionsState.value
                               val selectedId = currentSelectedIdState.value
                               
                               var hitId: String? = null
                               var mode = "MOVE"
                               
                               // Check Hit
                               for (region in regs.reversed()) {
                                     val rect = region.rect
                                     val cxR = rect.centerX()
                                     val cyR = rect.centerY()
                                     val rad = Math.toRadians(-region.rotation.toDouble())
                                     val cos = Math.cos(rad)
                                     val sin = Math.sin(rad)
                                     val dx = virtX - cxR
                                     val dy = virtY - cyR
                                     val rotX = (dx * cos - dy * sin).toFloat() + cxR
                                     val rotY = (dx * sin + dy * cos).toFloat() + cyR
                                     
                                     // 1. Check Knob (Only if Selected)
                                     val handleR = 40f
                                     if (region.id == selectedId) {
                                          if (rotX >= rect.right - handleR && rotX <= rect.right + handleR &&
                                              rotY >= rect.bottom - handleR && rotY <= rect.bottom + handleR) {
                                              hitId = region.id
                                              mode = "RESIZE"
                                              break
                                          }
                                     }
                                     
                                     // 2. Check Body
                                     if (rotX >= rect.left && rotX <= rect.right &&
                                         rotY >= rect.top && rotY <= rect.bottom) {
                                         hitId = region.id
                                         mode = "MOVE"
                                         break
                                     }
                               }
        
                               // Selection Logic
                               if (hitId != null) {
                                   if (hitId != selectedId) {
                                       currentOnSelectState.value(hitId)
                                   }
                               } else {
                                   currentOnSelectState.value(null) // Deselect on BG click
                               }
        
                               if (hitId != null) {
                                   do {
                                       val event = awaitPointerEvent()
                                       val canceled = event.changes.any { it.isConsumed }
                                       if (canceled) break
                                       
                                       val panChange = event.calculatePan()
                                       
                                       if (panChange != Offset.Zero) {
                                            val region = currentRegionsState.value.find { it.id == hitId }
                                            if (region != null) {
                                                val newRect = android.graphics.RectF(region.rect)
                                                // Convert Pan Screen -> Virtual
                                                val pdx = panChange.x / currentScale
                                                val pdy = panChange.y / currentScale
                                                
                                                if (mode == "RESIZE") {
                                                    newRect.right += pdx
                                                    newRect.bottom += pdy
                                                } else {
                                                    newRect.offset(pdx, pdy)
                                                }
                                                currentOnUpdateState.value(hitId, newRect, region.rotation)
                                            }
                                            event.changes.forEach { it.consume() }
                                       }
                                   } while (event.changes.any { it.pressed })
                                   
                                   // Loop Ended (Finger Up)
                                   currentOnInteractionEnd.value()
                               }
                           }
                     }
                }
            // Use a separate pointerInput for Transform (Rotation)
            .pointerInput(isInteractive) {
                 if (isInteractive) {
                     val rootOff = canvasGeometry.rootOffset
                     // We need to track the centroid for targetX/Y
                     detectTransformGestures { centroid, pan, zoom, rotation ->
                         val targetX = centroid.x + rootOff.x
                         val targetY = centroid.y + rootOff.y
                         onTransform(targetX, targetY, pan.x, pan.y, zoom, rotation)
                     }
                 }
            }
    ) {
         val width = installation.width
         val height = installation.height
         val viewW = size.width
         val viewH = size.height
         val screenCenter = Offset(viewW / 2f, viewH / 2f)
         
         val sx = viewW / width
         val sy = viewH / height
         val baseScale = minOf(sx, sy)
         
         val zoom = installation.cameraZoom
         val cx = installation.cameraX ?: (width / 2f)
         val cy = installation.cameraY ?: (height / 2f)
         
         val totalScale = baseScale * zoom
         
         withTransform({
             translate(left = screenCenter.x, top = screenCenter.y)
             scale(totalScale, totalScale, pivot = Offset.Zero)
             translate(left = -cx, top = -cy)
         }) {
             drawImage(
                 image = frame.bitmap.asImageBitmap(),
                 topLeft = Offset(frame.originX, frame.originY)
             )

             installation.devices.forEach { device ->
                 val cxD = device.x + device.width / 2f
                 val cyD = device.y + device.height / 2f
                 
                 val isOnline = deviceStatuses[device.ip] ?: true

                 withTransform({
                     rotate(device.rotation, pivot = Offset(cxD, cyD))
                 }) {
                     drawRect(
                        color = if (isOnline) Color(0xFF2196F3).copy(alpha = 0.5f) else Color.Red.copy(alpha = 0.2f),
                        topLeft = Offset(device.x, device.y),
                        size = Size(device.width, device.height)
                     )
                     drawRect(
                        color = if (isOnline) Color.White else Color.Red,
                        style = Stroke(width = if (isOnline) 2f / totalScale else 6f / totalScale),
                        topLeft = Offset(device.x, device.y),
                        size = Size(device.width, device.height)
                     )
                     
                     // Dots...
                     val dotsX = 10 
                     val dotsY = (dotsX * (device.height / device.width)).roundToInt().coerceAtLeast(1)
                     val stepX = device.width / dotsX
                     val stepY = device.height / dotsY
                    
                     for(i in 0 until dotsX) {
                        for(j in 0 until dotsY) {
                             drawCircle(
                                 color = Color.White.copy(alpha = 0.3f),
                                 radius = 2f / totalScale,
                                 center = Offset(device.x + stepX * i + stepX/2, device.y + stepY * j + stepY/2)
                             )
                        }
                     }
                 }
             }
             
             regions.forEach { region ->
                 // In Performance Mode (isInteractive), nothing is "selected" visually
                 val isSelected = !isInteractive && (region.id == selectedRegionId)
                 
                 withTransform({
                     rotate(region.rotation, pivot = Offset(region.rect.centerX(), region.rect.centerY()))
                 }) {
                     drawRect(
                        color = if (isSelected) Color.Yellow else Color.White, // Highlight Selection
                        style = Stroke(if (isSelected) 4f / totalScale else 2f / totalScale),
                        topLeft = Offset(region.rect.left, region.rect.top),
                        size = Size(region.rect.width(), region.rect.height())
                     )
                     
                     // Knob - Only if Selected
                     if (isSelected) {
                         drawCircle(
                             color = Color.Yellow,
                             radius = 20f / totalScale,
                             center = Offset(region.rect.right, region.rect.bottom)
                         )
                     }
                 }
             }
         }
    }
}
