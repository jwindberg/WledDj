package com.marsraver.wleddj.ui.player

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
// import androidx.compose.material.icons.filled.Edit // Removed unused edit
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
import com.marsraver.wleddj.data.repository.FileInstallationRepository
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
    val repository = remember { com.marsraver.wleddj.data.repository.RepositoryProvider.getRepository(context) }
    val viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(installationId, repository)
    )

    // Permission Request
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, audio will work
        } else {
            // Permission denied, maybe show snackbar?
        }
    }
    
    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    val engine by viewModel.engine.collectAsState()
    val previewFrame by (engine?.previewFrame ?: MutableStateFlow(null)).collectAsState()
    
    val installation by viewModel.installation.collectAsState()
    val selectedRegionId by viewModel.selectedRegionId.collectAsState()
    val regions by viewModel.regions.collectAsState()
    
    val dragTool by viewModel.draggedTool.collectAsState()
    val dragPosition by viewModel.dragPosition.collectAsState()
    
    val isInteractive by viewModel.isInteractiveMode.collectAsState()

    val canvasGeometry = remember { CanvasGeometry() }

    var showSheet by remember { mutableStateOf(false) }

    // Track the offset of the content container (due to Scaffold padding, etc.)
    var containerOffset by remember { mutableStateOf(Offset.Zero) }

    // Wake Lock Logic
    var isScreenLocked by remember { mutableStateOf(false) }
    
    // Auto-unlock if leaving performance mode? Not strictly required but good UX.
    LaunchedEffect(isInteractive) {
        if (!isInteractive) isScreenLocked = false
    }

    DisposableEffect(isScreenLocked) {
        val window = (context as? android.app.Activity)?.window
        if (isScreenLocked) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    // Back Handler: In Performance Mode -> Go to Edit Mode
    androidx.activity.compose.BackHandler(enabled = isInteractive) {
        viewModel.toggleInteractiveMode()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isInteractive) "Performance Mode" else "Animation Layout") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isInteractive) viewModel.toggleInteractiveMode() else onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (!isInteractive) {
                        if (selectedRegionId != null) {
                             IconButton(onClick = { viewModel.deleteSelection() }) {
                                Icon(Icons.Default.Delete, "Delete Selection", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                         // Arrow to Performance Mode
                         IconButton(onClick = { viewModel.toggleInteractiveMode() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward, 
                                contentDescription = "Enter Performance Mode",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                         // Interactive (Performance) Mode - Lock Logic
                         val icon = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen
                         val desc = if (isScreenLocked) "Unlock Screen" else "Lock Screen"
                         
                         IconButton(onClick = { isScreenLocked = !isScreenLocked }) {
                            Icon(icon, desc, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isInteractive && !showSheet) {
                FloatingActionButton(onClick = { showSheet = true }) {
                     Icon(Icons.Default.Add, "Add Animation")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()) // Ignore Top Padding to draw behind status bar
                .onGloballyPositioned { coordinates ->
                    containerOffset = coordinates.positionInRoot()
                }
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
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { showSheet = false })
                        }
                ) {
                    if (engine != null && installation != null && previewFrame != null) {
                         InteractivePlayerCanvas(
                             frame = previewFrame!!,
                             regions = regions,
                             installation = installation!!,
                             canvasGeometry = canvasGeometry,
                             isInteractive = isInteractive,
                             selectedRegionId = selectedRegionId, // Pass state
                             onSelectRegion = { viewModel.selectRegion(it) }, // Pass callback
                             onUpdateRegion = { id, rect, rot -> viewModel.updateRegion(id, rect, rot) },
                             onRemoveRegion = { viewModel.removeRegion(it) },
                             onInteract = { x, y -> 
                                 val virtualPoint = canvasGeometry.screenToVirtualPoint(x, y, installation!!.width, installation!!.height)
                                 if (virtualPoint != null) {
                                     viewModel.handleCanvasTouch(virtualPoint.x, virtualPoint.y)
                                 }
                             },
                             onInteractionEnd = { viewModel.saveAnimations() }
                         )
                    }
                }
                
                // 2. BOTTOM SHEET (Custom Implementation)
                if (showSheet && !isInteractive) {
                     AnimationSelectionSheet(
                         modifier = Modifier.align(Alignment.BottomCenter),
                         onDragStart = { type, globalOffset -> 
                             // Correct the offset: Global (Root) -> Local (Container)
                             val localStart = globalOffset - containerOffset
                             viewModel.startToolDrag(type, localStart)
                         },
                         onDrag = { delta -> viewModel.updateToolDrag(delta) }, // Delta is vector, no correction needed
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
                                
                                val vx = (dropPos.x - screenCenter.x) / currentScale + cx
                                val vy = (dropPos.y - screenCenter.y) / currentScale + cy
                                
                                if (vx >= -10000 && vx <= 10000) { 
                                    val type = dragTool ?: "Ball"
                                    viewModel.onToolDropped(type, vx, vy, width, height)
                                }
                                viewModel.endToolDrag() 
                         }
                     )
                }
            }
            
            // DRAG OVERLAY (Always on Top)
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
    frame: com.marsraver.wleddj.engine.RenderEngine.PreviewFrame,
    regions: List<com.marsraver.wleddj.data.model.AnimationRegion>,
    installation: com.marsraver.wleddj.data.model.Installation,
    canvasGeometry: CanvasGeometry,
    isInteractive: Boolean,
    selectedRegionId: String?,
    onSelectRegion: (String?) -> Unit,
    onUpdateRegion: (String, android.graphics.RectF, Float) -> Unit,
    onRemoveRegion: (String) -> Unit,
    onInteract: (Float, Float) -> Unit,
    onInteractionEnd: () -> Unit
) {
    // No null check needed really, caller handles it

    val currentRegionsState = rememberUpdatedState(regions)
    val currentSelectedIdState = rememberUpdatedState(selectedRegionId)
    val currentOnUpdateState = rememberUpdatedState(onUpdateRegion)
    val currentOnSelectState = rememberUpdatedState(onSelectRegion)
    val currentOnInteractionEnd = rememberUpdatedState(onInteractionEnd)
    
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
                        val selectedId = currentSelectedIdState.value
                        
                        val virtualPoint = screenToVirtual(down.position.x, down.position.y)
                        val virtX = virtualPoint.x
                        val virtY = virtualPoint.y
                        
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
                            
                            // Loop Ended (Finger Up)
                            currentOnInteractionEnd.value()
                        }
                     }
                } else {
                    // Performance Mode: Pass raw touch to engine (Flashlight, etc)
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        onInteract(down.position.x, down.position.y)
                        
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { 
                                if (it.pressed) {
                                    onInteract(it.position.x, it.position.y)
                                    it.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
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
// ... ToolItem and AnimationToolbox are below, unchanged or need to be preserved if I'm replacing till end of file.
// The replace range EndLine:588 covers the whole file end.
// I need to include ToolItem and AnimationToolbox in replacement content or use replacement chunks.
// MultiReplace is better if I can target just the function.
// But InteractivePlayerCanvas is large.
// I will include the rest of the file content in strict replacement.

@Composable
fun AnimationSelectionSheet(
    modifier: Modifier = Modifier,
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.4f), // Take up 40% of screen
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = MaterialTheme.shapes.large.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp), bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Animations",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Grid of Animations
            // Using LazyVerticalGrid equivalent (or just flow row for now if lazy grid dep not added)
            // Assuming LazyGrid is available or standard Row/Column for MVP
            
            // Note: Standard LazyVerticalGrid requires foundation dependency. Assuming available.
            // If not, using LazyRow/Column.
            
            // Simple Scrolling List
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Full WLED FX List
                val animations = listOf(
                    "Ball", 
                    "Static", 
                    "Rects", 
                    "Fireworks", 
                    "Aurora Borealis",
                    "Blurz",
                    "GEQ",
                    "Flashlight"
                ).sorted()
                
                items(animations.size) { index ->
                    AnimationListItem(
                        type = animations[index],
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDragEnd = onDragEnd
                    )
                }
            }
        }
    }
}

@Composable
fun AnimationListItem(
    type: String,
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var globalPosition by remember { mutableStateOf(Offset.Zero) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp) // Compact Row Height
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = type, 
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // DRAG HANDLE
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp) // Large Touch Target
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
                Icon(
                    Icons.Default.Menu, 
                    contentDescription = "Drag Handle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
