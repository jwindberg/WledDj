package com.example.wleddj.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wleddj.data.repository.FileInstallationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.foundation.Canvas
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    installationId: String?,
    onBack: () -> Unit
) {
    if (installationId == null) {
        onBack()
        return
    }

    val context = LocalContext.current
    val repository = remember { FileInstallationRepository(context) }
    val viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(installationId, repository)
    )

    val engine by viewModel.engine.collectAsState()
    val previewBitmap by (engine?.previewBitmap ?: MutableStateFlow(null)).collectAsState()
    val installation by viewModel.installation.collectAsState()
    val regions by viewModel.regions.collectAsState()
    
    val dragTool by viewModel.draggedTool.collectAsState()
    val dragPosition by viewModel.dragPosition.collectAsState()
    
    val isInteractive by viewModel.isInteractiveMode.collectAsState()

    val canvasGeometry = remember { CanvasGeometry() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isInteractive) "Perform Mode" else "Edit Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                     // Toggle Interactive Mode
                     IconButton(onClick = { viewModel.toggleInteractiveMode() }) {
                        Icon(
                            if (isInteractive) Icons.Filled.Lock else Icons.Filled.Edit, 
                            "Toggle Mode"
                        )
                    }
                     IconButton(onClick = { viewModel.clearAnimations() }) {
                        Icon(Icons.Default.Delete, "Clear All")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // ROOT BOX
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. CANVAS AREA (Full Screen)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                             canvasGeometry.rootOffset = coordinates.positionInRoot()
                             canvasGeometry.viewWidth = coordinates.size.width.toFloat()
                             canvasGeometry.viewHeight = coordinates.size.height.toFloat()
                             viewModel.onViewportSizeChanged(canvasGeometry.viewWidth, canvasGeometry.viewHeight)
                        }
                ) {
                    if (engine != null && installation != null) {
                         InteractivePlayerCanvas(
                             bitmap = previewBitmap,
                             regions = regions,
                             installation = installation!!,
                             canvasGeometry = canvasGeometry,
                             isInteractive = isInteractive,
                             onUpdateRegion = { id, rect, rot -> viewModel.updateRegion(id, rect, rot) },
                             onRemoveRegion = { viewModel.removeRegion(it) },
                             onInteract = { x, y -> 
                                 // Convert Screen -> Virtual
                                 val virtualPoint = canvasGeometry.screenToVirtualPoint(x, y, installation!!.width, installation!!.height)
                                 if (virtualPoint != null) {
                                     viewModel.handleCanvasTouch(virtualPoint.x, virtualPoint.y)
                                 }
                             }
                         )
                    } else {
                        Text("Initializing...", color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                }
                
                // 2. TOOLBOX OVERLAY (Floating at Bottom)
                if (!isInteractive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp) // Float slightly up
                    ) {
                        AnimationToolbox(
                            onDragStart = { type, offset -> 
                                 viewModel.startToolDrag(type, offset)
                            },
                            onDrag = { delta -> 
                                 viewModel.updateToolDrag(delta)
                            },
                            onDragEnd = {
                                val dropPos = dragPosition
                                val virtualPoint = canvasGeometry.screenToVirtualPoint(dropPos.x, dropPos.y, installation!!.width, installation!!.height)
                                if (virtualPoint != null) {
                                    val type = dragTool ?: "Ball"
                                    viewModel.onToolDropped(type, virtualPoint.x, virtualPoint.y, installation!!.width, installation!!.height)
                                }
                                viewModel.endToolDrag() 
                            }
                        )
                    }
                }
            }
            
            // DRAG OVERLAY
            if (dragTool != null) {
                Box(
                     modifier = Modifier
                         .offset { IntOffset(dragPosition.x.roundToInt() - 40.dp.roundToPx(), dragPosition.y.roundToInt() - 40.dp.roundToPx()) }
                         .size(80.dp)
                         .background(Color.White.copy(alpha = 0.5f), CircleShape),
                     contentAlignment = Alignment.Center
                ) {
                     Icon(Icons.Default.PlayArrow, null)
                }
            }
        }
    }
}

class CanvasGeometry {
    var rootOffset: Offset = Offset.Zero
    var viewWidth: Float = 1f
    var viewHeight: Float = 1f
    
    fun screenToVirtual(screenPos: Offset, installW: Float, installH: Float): android.graphics.RectF? {
        val pt = screenToVirtualPoint(screenPos.x, screenPos.y, installW, installH) ?: return null
        val size = 300f
        return android.graphics.RectF(pt.x - size/2, pt.y - size/2, pt.x + size/2, pt.y + size/2)
    }

    fun screenToVirtualPoint(screenX: Float, screenY: Float, installW: Float, installH: Float): Offset? {
        val localX = screenX - rootOffset.x
        val localY = screenY - rootOffset.y
        
        if (localX < 0 || localX > viewWidth || localY < 0 || localY > viewHeight) {
            return null
        }
        
        val scaleX = viewWidth / installW
        val scaleY = viewHeight / installH
        val scale = minOf(scaleX, scaleY)
        
        val offsetX = (viewWidth - installW * scale) / 2f
        val offsetY = (viewHeight - installH * scale) / 2f
        
        val virtualX = (localX - offsetX) / scale
        val virtualY = (localY - offsetY) / scale
        
        return Offset(virtualX, virtualY)
    }
}

@Composable
fun InteractivePlayerCanvas(
    bitmap: android.graphics.Bitmap?,
    regions: List<com.example.wleddj.data.model.AnimationRegion>,
    installation: com.example.wleddj.data.model.Installation,
    canvasGeometry: CanvasGeometry,
    isInteractive: Boolean,
    onUpdateRegion: (String, android.graphics.RectF, Float) -> Unit,
    onRemoveRegion: (String) -> Unit,
    onInteract: (Float, Float) -> Unit
) {
    if (bitmap == null) return
    
    val currentRegionsState = rememberUpdatedState(regions)
    val currentOnUpdateState = rememberUpdatedState(onUpdateRegion)
    val currentOnInteract = rememberUpdatedState(onInteract)
    
    var globalTouchPosition by remember { mutableStateOf(Offset.Zero) }
    
    // Shared Drag State
    val dragState = remember { DragState() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                 globalTouchPosition = coordinates.positionInRoot()
            }
            // Unified Gesture Detector (Pan, Zoom, Select)
            .pointerInput(isInteractive) {
                if (!isInteractive) {
                    val getScaleInfo = {
                        val width = installation.width
                        val height = installation.height
                        val viewW = canvasGeometry.viewWidth
                        val viewH = canvasGeometry.viewHeight
                        val scaleX = viewW / width
                        val scaleY = viewH / height
                        val scale = minOf(scaleX, scaleY)
                        val offsetX = (viewW - width * scale) / 2f
                        val offsetY = (viewH - height * scale) / 2f 
                        Triple(scale, offsetX, offsetY)
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        
                        // 1. HIT TEST (Selection)
                        val regs = currentRegionsState.value
                        val (scale, offsetX, offsetY) = getScaleInfo()
                        val touchX = down.position.x
                        val touchY = down.position.y
                        
                        // Convert touch to virtual space
                        val virtX = (touchX - offsetX) / scale
                        val virtY = (touchY - offsetY) / scale
                        
                        var hitId: String? = null
                        
                        // Check handles and body
                        for (region in regs.reversed()) {
                            val cx = region.rect.centerX()
                            val cy = region.rect.centerY()
                            val rad = Math.toRadians(-region.rotation.toDouble())
                            val cos = Math.cos(rad)
                            val sin = Math.sin(rad)
                            val dx = virtX - cx
                            val dy = virtY - cy
                            val rotX = (dx * cos - dy * sin).toFloat() + cx
                            val rotY = (dx * sin + dy * cos).toFloat() + cy

                            // Check Handle (though with Pinch we don't strictly need handle, but good to prioritize)
                            val handleR = 40f
                            if (rotX >= region.rect.right - handleR && rotX <= region.rect.right + handleR &&
                                rotY >= region.rect.bottom - handleR && rotY <= region.rect.bottom + handleR) {
                                hitId = region.id
                                break
                            }
                            // Check Body
                            if (rotX >= region.rect.left && rotX <= region.rect.right &&
                                rotY >= region.rect.top && rotY <= region.rect.bottom) {
                                hitId = region.id
                                break
                            }
                        }
                        
                        dragState.targetRegionId = hitId
                        
                        // 2. TRANSFORM LOOP
                        if (hitId != null) {
                            var zoom = 1f
                            var pan = Offset.Zero
                            var pastTouchValue: Offset? = null
                            
                            do {
                                val event = awaitPointerEvent()
                                val canceled = event.changes.any { it.isConsumed }
                                if (canceled) break
                                
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val rotationChange = event.calculateRotation()
                                
                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                     val region = currentRegionsState.value.find { it.id == hitId }
                                     if (region != null) {
                                         val newRect = android.graphics.RectF(region.rect)
                                         
                                         // Apply Zoom (Pinch)
                                         if (zoomChange != 1f) {
                                             val curCx = newRect.centerX()
                                             val curCy = newRect.centerY()
                                             val w = newRect.width() * zoomChange
                                             val h = newRect.height() * zoomChange
                                             newRect.left = curCx - w/2
                                             newRect.top = curCy - h/2
                                             newRect.right = curCx + w/2
                                             newRect.bottom = curCy + h/2
                                         }
                                         
                                         // Apply Pan
                                         if (panChange != Offset.Zero) {
                                             val dx = panChange.x / scale
                                             val dy = panChange.y / scale
                                             newRect.offset(dx, dy)
                                         }
                                         
                                         // Rotation (Disabled)
                                         val newRotation = region.rotation
                                         
                                         currentOnUpdateState.value(hitId, newRect, newRotation)
                                     }
                                     
                                     event.changes.forEach { 
                                         if (it.position != it.previousPosition) {
                                             it.consume() 
                                         }
                                     }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                }
            }
    ) {
         val width = installation.width
         val height = installation.height
         val scaleX = size.width / width
         val scaleY = size.height / height
         val scale = minOf(scaleX, scaleY) 
         
         val offsetX = (size.width - width * scale) / 2f
         val offsetY = (size.height - height * scale) / 2f
         
         withTransform({
             translate(left = offsetX, top = offsetY)
             scale(scale, scale, pivot = Offset.Zero)
         }) {
             drawImage(
                 image = bitmap.asImageBitmap(),
                 dstSize = IntSize(width.toInt(), height.toInt())
             )

             installation.devices.forEach { device ->
                 val cx = device.x + device.width / 2f
                 val cy = device.y + device.height / 2f
                 
                 val bodyColor = Color(0xFF2196F3).copy(alpha = 0.5f)
                 val outlineColor = Color.White
                 
                 withTransform({
                     rotate(device.rotation, pivot = Offset(cx, cy))
                 }) {
                     drawRect(
                        color = bodyColor,
                        topLeft = Offset(device.x, device.y),
                        size = Size(device.width, device.height)
                     )
                     drawRect(
                        color = outlineColor,
                        style = Stroke(width = 2f / scale),
                        topLeft = Offset(device.x, device.y),
                        size = Size(device.width, device.height)
                     )
                     
                     val dotsX = 10 
                     val dotsY = (dotsX * (device.height / device.width)).roundToInt().coerceAtLeast(1)
                     val stepX = device.width / dotsX
                     val stepY = device.height / dotsY
                    
                     for(i in 0 until dotsX) {
                        for(j in 0 until dotsY) {
                             drawCircle(
                                 color = Color.White.copy(alpha = 0.3f),
                                 radius = 2f / scale,
                                 center = Offset(device.x + stepX * i + stepX/2, device.y + stepY * j + stepY/2)
                             )
                        }
                     }
                 }
             }
             
             regions.forEach { region ->
                 val borderColor = Color.White 
                 
                 withTransform({
                     rotate(region.rotation, pivot = Offset(region.rect.centerX(), region.rect.centerY()))
                 }) {
                     drawRect(
                        color = borderColor,
                        style = Stroke(2f / scale),
                        topLeft = Offset(region.rect.left, region.rect.top),
                        size = Size(region.rect.width(), region.rect.height())
                     )
                     
                     if (!isInteractive) {
                         drawCircle(
                            color = Color.Yellow,
                            radius = 20f / scale,
                            center = Offset(region.rect.right, region.rect.bottom)
                         )
                         
                     }
                 }
             }
         }
    }
}

@Composable
fun AnimationToolbox(
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(300.dp)
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), MaterialTheme.shapes.medium)
            .border(1.dp, Color.White.copy(alpha = 0.3f), MaterialTheme.shapes.medium),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolItem("Ball", onDragStart, onDrag, onDragEnd)
        ToolItem("Static", onDragStart, onDrag, onDragEnd)
        ToolItem("Rects", onDragStart, onDrag, onDragEnd)
    }
}

@Composable
fun ToolItem(
    type: String,
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var globalPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .size(80.dp)
            .background(Color.DarkGray, MaterialTheme.shapes.small)
            .onGloballyPositioned { coordinates ->
                 globalPosition = coordinates.positionInRoot()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> 
                        val start = globalPosition + offset
                        onDragStart(type, start) 
                    }, 
                    onDrag = { change, dragAmount -> 
                        change.consume()
                        onDrag(dragAmount) 
                    },
                    onDragEnd = { onDragEnd() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White)
            Text(type, color = Color.White)
        }
    }
}
