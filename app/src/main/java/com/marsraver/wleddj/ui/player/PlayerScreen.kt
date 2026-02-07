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
import androidx.compose.material.icons.filled.Remove
// import androidx.compose.material.icons.filled.Edit // Removed unused edit
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.res.stringResource
import com.marsraver.wleddj.R
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
                        Text(if (isInteractive) stringResource(R.string.performance_mode) else stringResource(R.string.animation_layout_title))
                        if (!isInteractive && selectedRegionId != null) {
                             val animType = installation?.animations?.find { it.id == selectedRegionId }?.type?.displayName ?: stringResource(R.string.unknown)
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
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Always allow toggling Wake Lock
                    val icon = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen
                    val desc = if (isScreenLocked) stringResource(R.string.unlock_screen) else stringResource(R.string.lock_screen)
                    
                    IconButton(onClick = { isScreenLocked = !isScreenLocked }) {
                       Icon(icon, desc, tint = MaterialTheme.colorScheme.onSurface)
                    }

                    if (!isInteractive) {
                         // Arrow to Performance Mode
                         IconButton(onClick = { viewModel.toggleInteractiveMode() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward, 
                                contentDescription = stringResource(R.string.enter_performance_mode),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isInteractive && !showSheet) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.navigationBarsPadding() // Prevent nav bar overlap
                ) {
                    if (selectedRegionId != null) {
                         FloatingActionButton(
                             onClick = { viewModel.deleteSelection() },
                             containerColor = MaterialTheme.colorScheme.secondaryContainer,
                             contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                         ) {
                             Icon(Icons.Default.Remove, stringResource(R.string.delete_selection))
                         }
                    }

                    FloatingActionButton(
                        onClick = { showSheet = true }
                    ) {
                         Icon(Icons.Default.Add, stringResource(R.string.add_animation))
                    }
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
                             onInteractionEnd = { viewModel.onGestureEnded() }
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
                            onTextChange = { viewModel.updateText(it) },
                            onSpeedChange = { viewModel.setSpeed(it) }
                        )
                    }
                }

                // 3. BOTTOM SHEET (Standard ModalBottomSheet)
                if (showSheet && !isInteractive) {
                     ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                         AnimationSelectionSheet(
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
                         // Spacer for nav bar if needed, though ModalBottomSheet handles insets usually
                         Spacer(modifier = Modifier.height(16.dp))
                     }
                }
                
                // 4. PERFORMANCE MODE CONTROLS
                if (isInteractive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        val OctagonShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
                            val width = size.width
                            val height = size.height
                            val cornerRatio = 0.29f // 1 / (2 + sqrt(2)) for regular octagon
                            val cutX = width * cornerRatio
                            val cutY = height * cornerRatio
                            
                            moveTo(cutX, 0f)
                            lineTo(width - cutX, 0f)
                            lineTo(width, cutY)
                            lineTo(width, height - cutY)
                            lineTo(width - cutX, height)
                            lineTo(cutX, height)
                            lineTo(0f, height - cutY)
                            lineTo(0f, cutY)
                            close()
                        }

                        FloatingActionButton(
                            onClick = { viewModel.broadcastCommand("STOP") },
                            shape = OctagonShape,
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "STOP", 
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Canvas and Geometry moved to PlayerCanvas.kt



