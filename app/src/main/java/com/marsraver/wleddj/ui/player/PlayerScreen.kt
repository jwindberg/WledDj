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
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.marsraver.wleddj.repository.FileInstallationRepository
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
    val repository = remember { com.marsraver.wleddj.repository.RepositoryProvider.getRepository(context) }
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
                                val zoom = installation!!.cameraZoom
                                
                                viewModel.onToolDropped(type, cx, cy, width, height, zoom)
                                showSheet = false
                         }
                     )
                }
            }
        }
    }
}

// Canvas and Geometry moved to PlayerCanvas.kt


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
