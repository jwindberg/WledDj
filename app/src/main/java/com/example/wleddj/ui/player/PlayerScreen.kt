package com.example.wleddj.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
                                val virtualRect = canvasGeometry.screenToVirtual(dropPos, installation!!.width, installation!!.height)
                                if (virtualRect != null) {
                                    val type = dragTool ?: "Ball"
                                    viewModel.onToolDropped(type, virtualRect)
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
        val scale = minOf(scaleX, scaleY) * 0.9f
        
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

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                 globalTouchPosition = coordinates.positionInRoot()
            }
            .pointerInput(isInteractive) {
               if (isInteractive) {
                   detectDragGestures(
                       onDragStart = { offset -> 
                           val global = globalTouchPosition + offset
                           currentOnInteract.value(global.x, global.y)
                       },
                       onDrag = { change, _ ->
                           change.consume()
                           val global = globalTouchPosition + change.position
                           currentOnInteract.value(global.x, global.y)
                       },
                       onDragEnd = {}
                   )
               } else {
                   // EDIT MODE
                   class DragState {
                        var targetRegionId: String? = null
                        var mode: String = "NONE" // NONE, MOVE, RESIZE, ROTATE
                        var startRect = android.graphics.RectF()
                        var startRotation = 0f
                        var startTouch = Offset.Zero
                        
                        var accumulatedX = 0f
                        var accumulatedY = 0f
                   }
                   val dragState = DragState()
                   
                   detectDragGestures(
                        onDragStart = { offset ->
                            val regs = currentRegionsState.value
                            val width = installation.width
                            val height = installation.height
                            
                            val scaleX = size.width / width
                            val scaleY = size.height / height
                            val scale = minOf(scaleX, scaleY) * 0.9f 
                            val offsetX = (size.width - width * scale) / 2f
                            val offsetY = (size.height - height * scale) / 2f
                            
                            // Convert touch to virtual space
                            val virtX = (offset.x - offsetX) / scale
                            val virtY = (offset.y - offsetY) / scale
                            
                            var hitId: String? = null
                            var hitMode = "NONE"
                            
                            for (region in regs.reversed()) {
                                val cx = region.rect.centerX()
                                val cy = region.rect.centerY()
                                
                                // To check hits on rotated regions, rotate the touch point negatively
                                val rad = Math.toRadians(-region.rotation.toDouble())
                                val cos = Math.cos(rad)
                                val sin = Math.sin(rad)
                                val dx = virtX - cx
                                val dy = virtY - cy
                                val rotX = (dx * cos - dy * sin).toFloat() + cx
                                val rotY = (dx * sin + dy * cos).toFloat() + cy

                                // Handle radius
                                val handleR = 40f
                                
                                // Rotation Handle (Top Center, extended up by 50 units)
                                val rotHandleY = region.rect.top - 50f
                                if (rotX >= cx - handleR && rotX <= cx + handleR &&
                                    rotY >= rotHandleY - handleR && rotY <= rotHandleY + handleR) {
                                    hitId = region.id
                                    hitMode = "ROTATE"
                                    break
                                }

                                // Resize Handle (Bottom Right)
                                if (rotX >= region.rect.right - handleR && rotX <= region.rect.right + handleR &&
                                    rotY >= region.rect.bottom - handleR && rotY <= region.rect.bottom + handleR) {
                                    hitId = region.id
                                    hitMode = "RESIZE"
                                    break
                                }
                                
                                // Move (Body)
                                if (rotX >= region.rect.left && rotX <= region.rect.right &&
                                    rotY >= region.rect.top && rotY <= region.rect.bottom) {
                                    hitId = region.id
                                    hitMode = "MOVE"
                                    break
                                }
                            }
                            
                            if (hitId != null) {
                                dragState.targetRegionId = hitId
                                dragState.mode = hitMode
                                val region = regs.find { it.id == hitId }
                                if (region != null) {
                                    dragState.startRect = android.graphics.RectF(region.rect)
                                    dragState.startRotation = region.rotation
                                    dragState.startTouch = Offset(virtX, virtY)
                                    dragState.accumulatedX = 0f
                                    dragState.accumulatedY = 0f
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val tId = dragState.targetRegionId
                            if (dragState.mode != "NONE" && tId != null) {
                                val region = currentRegionsState.value.find { it.id == tId } ?: return@detectDragGestures
                                
                                val width = installation.width
                                val height = installation.height
                                val scaleX = size.width / width
                                val scaleY = size.height / height
                                val scale = minOf(scaleX, scaleY) * 0.9f 
                                
                                val dx = dragAmount.x / scale
                                val dy = dragAmount.y / scale
                                
                                dragState.accumulatedX += dx
                                dragState.accumulatedY += dy
                                
                                val newRect = android.graphics.RectF(dragState.startRect)
                                var newRotation = dragState.startRotation
                                
                                if (dragState.mode == "MOVE") {
                                    // Move in rotated space? No, move is usually absolute translation.
                                    // Complex: if we drag a rotated box, dx/dy are screen aligned.
                                    // We just add them to the rect.
                                    newRect.offset(dragState.accumulatedX, dragState.accumulatedY)
                                } else if (dragState.mode == "RESIZE") {
                                    // Simple resizing for now (unrotated axis)
                                    // For true rotated resize, we need to project drag onto axes.
                                    // Simplifying: Just track distance from center?
                                    // OR just accept screen-aligned resize which feels weird on rotated box.
                                    // Let's Rotate the drag delta into local space!
                                    
                                    val rad = Math.toRadians(-dragState.startRotation.toDouble())
                                    val cos = Math.cos(rad)
                                    val sin = Math.sin(rad)
                                    
                                    // We use total accumulated drag
                                    val adx = dragState.accumulatedX
                                    val ady = dragState.accumulatedY
                                    
                                    val rdx = (adx * cos - ady * sin).toFloat()
                                    val rdy = (adx * sin + ady * cos).toFloat()
                                    
                                    newRect.right += rdx
                                    newRect.bottom += rdy
                                     if (newRect.width() < 10f) newRect.right = newRect.left + 10f
                                     if (newRect.height() < 10f) newRect.bottom = newRect.top + 10f
                                } else if (dragState.mode == "ROTATE") {
                                    // Calculate angle from center to current touch
                                    val cx = dragState.startRect.centerX() + dragState.accumulatedX // if moving? No.
                                    // Wait, rotation handle drag purely affects rotation.
                                    
                                    // Current touch pos in virtual space
                                    // We need to re-calculate virtX/virtY from event?
                                    // We can just use accumulated delta + startTouch
                                    val currX = dragState.startTouch.x + dragState.accumulatedX
                                    val currY = dragState.startTouch.y + dragState.accumulatedY
                                    
                                    val cxCenter = dragState.startRect.centerX()
                                    val cyCenter = dragState.startRect.centerY()
                                    
                                    val angle = Math.toDegrees(Math.atan2((currY - cyCenter).toDouble(), (currX - cxCenter).toDouble())).toFloat()
                                    // Initial angle was -90 (top)
                                    // We want rotation relative to upright.
                                    // Handle is at top (-90 deg from center).
                                    // So Rotation = angle + 90
                                    newRotation = angle + 90f
                                }
                                
                                currentOnUpdateState.value(tId, newRect, newRotation)
                            }
                        }
                   )
               }
            }
    ) {
         val width = installation.width
         val height = installation.height
         val scaleX = size.width / width
         val scaleY = size.height / height
         val scale = minOf(scaleX, scaleY) * 0.9f 
         
         val offsetX = (size.width - width * scale) / 2f
         val offsetY = (size.height - height * scale) / 2f
         
         withTransform({
             translate(left = offsetX, top = offsetY)
             scale(scale, scale, pivot = Offset.Zero)
         }) {
             // 1. LED Preview (Background)
             drawImage(
                 image = bitmap.asImageBitmap(),
                 dstSize = IntSize(width.toInt(), height.toInt())
             )

             // 2. Devices Outline (Overlay)
             installation.devices.forEach { device ->
                 val cx = device.x + device.width / 2f
                 val cy = device.y + device.height / 2f
                 
                 // Use consistent styling with LayoutEditor
                 // In Player, we don't have "Selection" concept for devices, so use default Blue style
                 val bodyColor = Color(0xFF2196F3).copy(alpha = 0.5f)
                 val outlineColor = Color.White
                 
                 withTransform({
                     rotate(device.rotation, pivot = Offset(cx, cy))
                 }) {
                     // Body
                     drawRect(
                        color = bodyColor,
                        topLeft = Offset(device.x, device.y),
                        size = Size(device.width, device.height)
                     )
                     
                     // Outline
                     drawRect(
                        color = outlineColor,
                        style = Stroke(width = 2f / scale),
                        topLeft = Offset(device.x, device.y),
                        size = Size(device.width, device.height)
                     )
                     
                     // Pixel dots
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
             
             // 3. Animation Regions
             regions.forEach { region ->
                 // Consistent visual style (no dimming in Perform Mode)
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
                         // Resize Handle (Bottom Right)
                         drawCircle(
                            color = Color.Yellow,
                            radius = 20f / scale,
                            center = Offset(region.rect.right, region.rect.bottom)
                         )
                         
                         // Rotation Handle (Top Center, Stick + Ball)
                         val cx = region.rect.centerX()
                         val top = region.rect.top
                         drawLine(
                             color = Color.Blue,
                             start = Offset(cx, top),
                             end = Offset(cx, top - 50f),
                             strokeWidth = 3f / scale
                         )
                         drawCircle(
                            color = Color.Blue,
                            radius = 20f / scale,
                            center = Offset(cx, top - 50f)
                         )
                     }
                 }
             }
             
             // Boundary (Optional, making it very subtle or removing if desired)
             // drawRect(Color.DarkGray, style = Stroke(1f / scale), size = Size(width, height))
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
            .width(300.dp) // Fixed width palette
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
                        // offset is local to the item
                        // Global start = ItemGlobal + LocalOffset
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
