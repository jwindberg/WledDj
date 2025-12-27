package com.marsraver.wleddj.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
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
        factory = PlayerViewModel.Factory(context.applicationContext as android.app.Application, installationId, repository)
    )
    val deviceStatuses by viewModel.deviceStatuses.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.resumeEngine()
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.pauseEngine()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
    
    val isInteractive by viewModel.isInteractiveMode.collectAsState()

    val canvasGeometry = remember { CanvasGeometry() }

    var showSheet by remember { mutableStateOf(false) }

    // Wake Lock Logic - Default to ON per user request
    var isScreenLocked by remember { mutableStateOf(true) }
    
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
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isInteractive) "Performance Mode" else "Animation Layout")
                        if (!isInteractive && selectedRegionId != null) {
                             val animType = installation?.animations?.find { it.id == selectedRegionId }?.type ?: "Unknown"
                             Text(
                                 text = animType,
                                 style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                        }
                    }
                },
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
                    // Always allow toggling Wake Lock
                    val icon = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen
                    val desc = if (isScreenLocked) "Unlock Screen" else "Lock Screen"
                    
                    IconButton(onClick = { isScreenLocked = !isScreenLocked }) {
                       Icon(icon, desc, tint = MaterialTheme.colorScheme.onSurface)
                    }

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
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { showSheet = false })
                        }
                ) {
                    if (engine != null && installation != null && previewFrame != null) {
                         InteractivePlayerCanvas(
                             frame = previewFrame!!,
                             regions = regions,
                             installation = installation!!,
                             deviceStatuses = deviceStatuses,
                             canvasGeometry = canvasGeometry,
                             isInteractive = isInteractive,
                             selectedRegionId = selectedRegionId, // Pass state
                             onSelectRegion = { viewModel.selectRegion(it) }, // Pass callback
                             onUpdateRegion = { id, rect, rot -> viewModel.updateRegion(id, rect, rot) },
                             onRemoveRegion = { viewModel.removeRegion(it) },
                             onInteract = { x, y -> 
                                 val virtualPoint = canvasGeometry.screenToVirtualPoint(
                                     x, y, 
                                     installation!!.width, 
                                     installation!!.height,
                                     installation!!.cameraZoom,
                                     installation!!.cameraX,
                                     installation!!.cameraY
                                 )
                                 if (virtualPoint != null) {
                                     viewModel.handleCanvasTouch(virtualPoint.x, virtualPoint.y)
                                 }
                             },
                             // onSideControl Removed
                             onTransform = { tx, ty, px, py, z, r ->
                                 val virtualPoint = canvasGeometry.screenToVirtualPoint(
                                     tx, ty, 
                                     installation!!.width, 
                                     installation!!.height,
                                     installation!!.cameraZoom,
                                     installation!!.cameraX,
                                     installation!!.cameraY
                                 )
                                 if (virtualPoint != null) {
                                     val scale = 1f / installation!!.cameraZoom
                                     viewModel.handleCanvasTransform(
                                         virtualPoint.x, virtualPoint.y,
                                         px * scale, py * scale, 
                                         z, r
                                     )
                                 }
                             },
                             onInteractionEnd = { viewModel.saveAnimations() }
                         )
                    }
                }
                
                // 2. CONTROLS BAR (Overlay at Bottom)
                if (!isInteractive) {
                    val controlsState by viewModel.animationControlsState.collectAsState()
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 16.dp, start = 16.dp, end = 100.dp) // Avoid FAB
                    ) {
                        AnimationControlsBar(
                            state = controlsState,
                            onPrimaryColorChange = { viewModel.setPrimaryColor(it) },
                            onSecondaryColorChange = { viewModel.setSecondaryColor(it) },
                            onPaletteChange = { viewModel.setPalette(it) },
                            onTextChange = { viewModel.updateText(it) }
                        )
                    }
                }

                // 3. BOTTOM SHEET (Custom Implementation)
                if (showSheet && !isInteractive) {
                     AnimationSelectionSheet(
                         modifier = Modifier.align(Alignment.BottomCenter),
                         onSelect = { type ->
                                val width = installation!!.width
                                val height = installation!!.height
                                
                                // Drop at Center of Viewport (Camera Position)
                                val cx = installation!!.cameraX ?: (width / 2f)
                                val cy = installation!!.cameraY ?: (height / 2f)
                                
                                viewModel.onToolDropped(type, cx, cy, width, height)
                                showSheet = false
                         }
                     )
                }
            }
        }
    }
}

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
    frame: com.marsraver.wleddj.engine.RenderEngine.PreviewFrame,
    regions: List<com.marsraver.wleddj.data.model.AnimationRegion>,
    installation: com.marsraver.wleddj.data.model.Installation,
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
                if (!isInteractive) {
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

@Composable
fun AnimationSelectionSheet(
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.4f), 
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = MaterialTheme.shapes.large.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp), bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Add Animation",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val animations = listOf(
                    "Akemi",
                    "Aquarium",
                    "Fire 2012 2D",
                    "FireNoise2D",
                    "Noise2D",
                    "PlasmaBall2D",
                    "Matrix",
                    "MetaBalls",
                    "Game Of Life",
                    "Julia",
                    "Swirl",
                    "Pacifica",
                    "Blobs",
                    "DistortionWaves",
                    "Plasmoid",
                    "PolarLights",
                    "Space Ships",
                    "SquareSwirl",
                    "Puddles",
                    "Lissajous",
                    "Tartan",
                    "Waverly",
                    "CrazyBees",
                    "GhostRider",
                    "SunRadiation",
                    "WashingMachine",
                    "RotoZoomer",
                    "Tetrix",
                    "Hiphotic",
                    "BlackHole",
                    "FunkyPlank",
                    "DriftRose",
                    "Matripix",
                    "WavingCell",
                    "Frizzles",
                    "PixelWave",
                    "FreqMatrix",
                    "Lake",
                    "DnaSpiral",
                    "Globe",
                    "Ball", 
                    "Spectrogram",
                    "InfiniteTunnel",
                    "Sonar",
                    "ScrollingText",
                    "Fireworks",

                    "Aurora Borealis",
                    "Blurz",
                    "GEQ",
                    "MusicBall",
                    "DeathStarRun",
                    "Flashlight",
                    "Fireflies",
                    "TronRecognizer",
                    "SpectrumTree",
                    "Soap",
                    "Fireworks"
                ).sorted()
                
                val audioReactiveConfigs = setOf(
                    "GEQ",
                    "MusicBall",
                    "SpectrumTree",
                    "Fireworks",
                    "InfiniteTunnel",
                    "Sonar"
                )
                
                items(animations.size) { index ->
                    val name = animations[index]
                    AnimationListItem(
                        type = name,
                        isAudioReactive = audioReactiveConfigs.contains(name),
                        onClick = { onSelect(name) }
                    )
                }
            }
        }
    }
}

@Composable
fun AnimationListItem(
    type: String,
    isAudioReactive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick) 
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = type, 
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            if (isAudioReactive) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.MusicNote,
                    contentDescription = "Audio Reactive",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
