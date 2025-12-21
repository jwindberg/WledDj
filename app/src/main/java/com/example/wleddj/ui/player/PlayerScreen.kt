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
    val repository = remember { com.example.wleddj.data.repository.RepositoryProvider.getRepository(context) }
    val viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(installationId, repository)
    )

    val engine by viewModel.engine.collectAsState()
    // Collect the Frame, not just the Bitmap
    val previewFrame by (engine?.previewFrame ?: MutableStateFlow(null)).collectAsState()
    
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
                    if (engine != null && installation != null && previewFrame != null) {
                         InteractivePlayerCanvas(
                             frame = previewFrame!!, // Pass the Frame
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
                                
                                val width = installation!!.width
                                val height = installation!!.height
                                val viewW = canvasGeometry.viewWidth
                                val viewH = canvasGeometry.viewHeight
                                val screenCenter = Offset(viewW / 2f, viewH / 2f)
                                
                                val sx = viewW / width
                                val sy = viewH / height
                                val baseScale = minOf(sx, sy)
                                
                                val zoom = installation!!.cameraZoom
                                val cx = installation!!.cameraX ?: (width / 2f)
                                val cy = installation!!.cameraY ?: (height / 2f)
                                val currentScale = baseScale * zoom
                                
                                // Proper Inverse Transform:
                                // Screen = (Virtual - Camera) * Scale + ScreenCenter
                                // Virtual = (Screen - ScreenCenter) / Scale + Camera
                                val vx = (dropPos.x - screenCenter.x) / currentScale + cx
                                val vy = (dropPos.y - screenCenter.y) / currentScale + cy
                                
                                if (vx >= -10000 && vx <= 10000) { // Sanity check, virtual space is large
                                    val type = dragTool ?: "Ball"
                                    viewModel.onToolDropped(type, vx, vy, width, height)
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
    frame: com.example.wleddj.engine.RenderEngine.PreviewFrame,
    regions: List<com.example.wleddj.data.model.AnimationRegion>,
    installation: com.example.wleddj.data.model.Installation,
    canvasGeometry: CanvasGeometry,
    isInteractive: Boolean,
    onUpdateRegion: (String, android.graphics.RectF, Float) -> Unit,
    onRemoveRegion: (String) -> Unit,
    onInteract: (Float, Float) -> Unit
) {
    // No null check needed really, caller handles it

    val currentRegionsState = rememberUpdatedState(regions)
    val currentOnUpdateState = rememberUpdatedState(onUpdateRegion)
    
    // Shared Drag State
    val dragState = remember { DragState() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isInteractive) {
                if (!isInteractive) {
                     awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        
                        // Geometry (Matches Editor Camera Model)
                        val viewW = canvasGeometry.viewWidth
                        val viewH = canvasGeometry.viewHeight
                        val screenCenter = Offset(viewW / 2f, viewH / 2f)
                        
                        val installW = installation.width
                        val installH = installation.height
                        
                        val sx = viewW / installW
                        val sy = viewH / installH
                        val baseScale = minOf(sx, sy)

                        // Camera
                        val zoom = installation.cameraZoom
                        val cx = installation.cameraX ?: (installW / 2f)
                        val cy = installation.cameraY ?: (installH / 2f)
                        val currentScale = baseScale * zoom

                        // Helper
                        fun screenToVirtual(touchX: Float, touchY: Float): Offset {
                            val vx = (touchX - screenCenter.x) / currentScale + cx
                            val vy = (touchY - screenCenter.y) / currentScale + cy
                            return Offset(vx, vy)
                        }

                        // 1. HIT TEST
                        val regs = currentRegionsState.value
                        val virtualPoint = screenToVirtual(down.position.x, down.position.y)
                        val virtX = virtualPoint.x
                        val virtY = virtualPoint.y
                        
                        var hitId: String? = null
                        var mode = "MOVE"
                        
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
                             
                             val handleR = 40f
                             if (rotX >= rect.right - handleR && rotX <= rect.right + handleR &&
                                 rotY >= rect.bottom - handleR && rotY <= rect.bottom + handleR) {
                                 hitId = region.id
                                 mode = "RESIZE"
                                 break
                             }
                             if (rotX >= rect.left && rotX <= rect.right &&
                                 rotY >= rect.top && rotY <= rect.bottom) {
                                 hitId = region.id
                                 mode = "MOVE"
                                 break
                             }
                        }

                        if (hitId != null) {
                            do {
                                val event = awaitPointerEvent()
                                val canceled = event.changes.any { it.isConsumed }
                                if (canceled) break
                                
                                val panChange = event.calculatePan()
                                // val zoomChange = event.calculateZoom() 
                                
                                if (panChange != Offset.Zero) {
                                     val region = currentRegionsState.value.find { it.id == hitId }
                                     if (region != null) {
                                         val newRect = android.graphics.RectF(region.rect)
                                         // Convert Pan Screen -> Virtual
                                         val dx = panChange.x / currentScale
                                         val dy = panChange.y / currentScale
                                         
                                         if (mode == "RESIZE") {
                                             newRect.right += dx
                                             newRect.bottom += dy
                                         } else {
                                             newRect.offset(dx, dy)
                                         }
                                         currentOnUpdateState.value(hitId, newRect, region.rotation)
                                     }
                                     event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
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
             // Draw Bitmap with Origin OFFSET and NATURAL SIZE
             drawImage(
                 image = frame.bitmap.asImageBitmap(),
                 topLeft = Offset(frame.originX, frame.originY)
             )

             installation.devices.forEach { device ->
                 val cxD = device.x + device.width / 2f
                 val cyD = device.y + device.height / 2f
                 
                 withTransform({
                     rotate(device.rotation, pivot = Offset(cxD, cyD))
                 }) {
                     drawRect(
                        color = Color(0xFF2196F3).copy(alpha = 0.5f),
                        topLeft = Offset(device.x, device.y),
                        size = Size(device.width, device.height)
                     )
                     drawRect(
                        color = Color.White,
                        style = Stroke(width = 2f / totalScale),
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
                 withTransform({
                     rotate(region.rotation, pivot = Offset(region.rect.centerX(), region.rect.centerY()))
                 }) {
                     drawRect(
                        color = Color.White,
                        style = Stroke(2f / totalScale),
                        topLeft = Offset(region.rect.left, region.rect.top),
                        size = Size(region.rect.width(), region.rect.height())
                     )
                     
                     // Knob
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
// ... ToolItem and AnimationToolbox are below, unchanged or need to be preserved if I'm replacing till end of file.
// The replace range EndLine:588 covers the whole file end.
// I need to include ToolItem and AnimationToolbox in replacement content or use replacement chunks.
// MultiReplace is better if I can target just the function.
// But InteractivePlayerCanvas is large.
// I will include the rest of the file content in strict replacement.

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
