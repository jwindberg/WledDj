package com.marsraver.wleddj.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsraver.wleddj.data.model.WledDevice
import com.marsraver.wleddj.data.repository.FileInstallationRepository
import com.marsraver.wleddj.engine.network.DiscoveredDevice
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutEditorScreen(
    installationId: String?,
    onBack: () -> Unit,
    onPlay: () -> Unit
) {
    if (installationId == null) {
        onBack()
        return
    }

    val context = LocalContext.current
    val repository = com.marsraver.wleddj.data.repository.RepositoryProvider.getRepository(context)
    val viewModel: EditorViewModel = viewModel(
        factory = EditorViewModel.Factory(installationId, repository, context)
    )

    val installation by viewModel.installation.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var selectedDeviceIp: String? by remember { mutableStateOf(null) }

    // Derive the selected device object from the current installation
    val selectedDevice = remember(installation, selectedDeviceIp) {
        installation?.devices?.find { it.ip == selectedDeviceIp }
    }

    // Intercept System Back to Save
    androidx.activity.compose.BackHandler {
        viewModel.saveProject()
        onBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Device Layout")
                        if (selectedDevice != null) {
                            Text(
                                text = selectedDevice.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveProject()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (selectedDevice != null) {
                        IconButton(onClick = {
                            selectedDevice?.let { viewModel.removeDevice(it) }
                            selectedDeviceIp = null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete, 
                                contentDescription = "Delete Device",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(onClick = {
                        viewModel.saveProject()
                        onPlay() // Navigates to subsequent screen (Animation Layout)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward, 
                            contentDescription = "Animation Layout",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { showRebootDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Refresh, "Reboot All")
                }

                FloatingActionButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Default.Add, "Add Device")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            installation?.let { install ->
                LayoutCanvas(
                    installation = install,
                    selectedDevice = selectedDevice,
                    onSelectDevice = { selectedDeviceIp = it?.ip },
                    onMoveDevice = { device, newX, newY, w, h, rot ->
                        viewModel.updateDevice(device.ip, newX, newY, w, h, rot)
                    },
                    onUpdateViewport = { z, offset ->
                        viewModel.updateCamera(offset.x, offset.y, z)
                    },
                    onInteractionEnd = { viewModel.saveProject() }
                )
            }
        }

        if (showAddSheet) {
            ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Device", style = MaterialTheme.typography.titleLarge)
                    if (discoveredDevices.isEmpty()) {
                        Text("Scanning for WLED devices...", modifier = Modifier.padding(vertical = 16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LazyColumn {
                            items(discoveredDevices) { device ->
                                // Filter out already added devices visually
                                val isAdded = installation?.devices?.any { it.ip == device.ip } == true
                                if (!isAdded) {
                                    DiscoveredDeviceItem(device) {
                                        viewModel.addDevice(device)
                                        showAddSheet = false
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text("Reboot Devices") },
            text = { Text("Reboot all WLED devices?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rebootAllDevices()
                    showRebootDialog = false
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LayoutCanvas(
    installation: com.marsraver.wleddj.data.model.Installation,
    selectedDevice: WledDevice?,
    onSelectDevice: (WledDevice?) -> Unit,
    onMoveDevice: (WledDevice, Float, Float, Float, Float, Float) -> Unit,
    onUpdateViewport: (Float, Offset) -> Unit,
    onInteractionEnd: () -> Unit
) {
    val currentInstallationState = rememberUpdatedState(installation)
    val currentSelectedDeviceState = rememberUpdatedState(selectedDevice)
    val currentOnSelectDeviceState = rememberUpdatedState(onSelectDevice)
    val currentOnMoveDeviceState = rememberUpdatedState(onMoveDevice)
    val currentOnUpdateViewportState = rememberUpdatedState(onUpdateViewport)
    val currentOnInteractionEndState = rememberUpdatedState(onInteractionEnd)

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

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                     awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        
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
                        val installDevices = currentInstallationState.value.devices
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
                                     
                                     // SNAP TO GRID (10px)
                                     val snapX = (dragX / 10f).roundToInt() * 10f
                                     val snapY = (dragY / 10f).roundToInt() * 10f
                                     
                                     currentOnMoveDeviceState.value(hitDevice, snapX, snapY, hitDevice.width, hitDevice.height, hitDevice.rotation)
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
            // Transform:
            // 1. Translate ScreenCenter
            // 2. Scale
            // 3. Translate -Cam
            
            withTransform({
                translate(left = screenCenter.x, top = screenCenter.y)
                scale(totalScale, totalScale, pivot = Offset.Zero)
                translate(left = -cx, top = -cy)
            }) {
                // Draw Devices
                installation.devices.forEach { device ->
                    val isSelected = device == selectedDevice
                    drawRect(
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
        }
    }
}

@Composable
fun DiscoveredDeviceItem(device: DiscoveredDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium)
            Text(device.ip, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
