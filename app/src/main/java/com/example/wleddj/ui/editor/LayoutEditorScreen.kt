package com.example.wleddj.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.wleddj.data.model.WledDevice
import com.example.wleddj.data.repository.FileInstallationRepository
import com.example.wleddj.engine.network.DiscoveredDevice
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
    val repository = remember { FileInstallationRepository(context) } // Should be injected
    val viewModel: EditorViewModel = viewModel(
        factory = EditorViewModel.Factory(installationId, repository, context)
    )

    val installation by viewModel.installation.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedDeviceIp: String? by remember { mutableStateOf(null) }

    // Derive the selected device object from the current installation
    val selectedDevice = remember(installation, selectedDeviceIp) {
        installation?.devices?.find { it.ip == selectedDeviceIp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val title = if (selectedDevice != null) {
                        "Selected: ${selectedDevice.name}"
                    } else {
                        installation?.name ?: "Layout Editor"
                    }
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (selectedDevice != null) {
                        IconButton(onClick = {
                            selectedDevice?.let { viewModel.removeDevice(it) }
                            selectedDeviceIp = null
                        }) {
                            Icon(Icons.Default.Delete, "Delete Device")
                        }
                    }
                    IconButton(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, "Play Mode")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Default.Add, "Add Device")
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
                    }
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
}

@Composable
fun LayoutCanvas(
    installation: com.example.wleddj.data.model.Installation,
    selectedDevice: WledDevice?,
    onSelectDevice: (WledDevice?) -> Unit,
    onMoveDevice: (WledDevice, Float, Float, Float, Float, Float) -> Unit // device, x, y, w, h, rot
) {
    // Current state references for the gesture detector (use State object directly)
    val currentInstallationState = rememberUpdatedState(installation)
    val currentSelectedDeviceState = rememberUpdatedState(selectedDevice)
    val currentOnSelectDeviceState = rememberUpdatedState(onSelectDevice)
    val currentOnMoveDeviceState = rememberUpdatedState(onMoveDevice)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Wrapper to hold local drag state that persists across the gesture event stream
                class DragState {
                    var draggedDevice: WledDevice? = null
                    var startX = 0f
                    var startY = 0f
                    var accumulatedDragX = 0f
                    var accumulatedDragY = 0f
                }
                val dragState = DragState()
                
                detectDragGestures(
                    onDragStart = { offset ->
                        val install = currentInstallationState.value
                        val canvasWidth = size.width.toFloat()
                        val canvasHeight = size.height.toFloat()
                        
                        // Calculate scale
                        val scaleX = canvasWidth / install.width
                        val scaleY = canvasHeight / install.height
                        val scale = minOf(scaleX, scaleY) 
                        
                        val offsetX = (canvasWidth - install.width * scale) / 2f
                        val offsetY = 0f // Align Top (Match Player)
                        
                        val virtualX = (offset.x - offsetX) / scale
                        val virtualY = (offset.y - offsetY) / scale

                        val hit = install.devices.find { device ->
                             virtualX >= device.x && virtualX <= device.x + device.width &&
                             virtualY >= device.y && virtualY <= device.y + device.height
                        }
                        
                        currentOnSelectDeviceState.value(hit)
                        
                        // Initialize local drag state
                        dragState.draggedDevice = hit
                        if (hit != null) {
                            dragState.startX = hit.x
                            dragState.startY = hit.y
                            dragState.accumulatedDragX = 0f
                            dragState.accumulatedDragY = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val device = dragState.draggedDevice
                        val install = currentInstallationState.value

                        if (device != null) {
                            val canvasWidth = size.width.toFloat()
                            val canvasHeight = size.height.toFloat()
                            val scaleX = canvasWidth / install.width
                            val scaleY = canvasHeight / install.height
                            val scale = minOf(scaleX, scaleY) 

                            // Scale the drag amount
                            val dbX = dragAmount.x / scale
                            val dbY = dragAmount.y / scale
                            
                            // Accumulate locally - this is the source of truth for the drag session
                            dragState.accumulatedDragX += dbX
                            dragState.accumulatedDragY += dbY

                            // Calculate target position based on LOCAL start + LOCAL accumulator
                            // This completely ignores the VM's current 'device.x' which might be lagging
                            var newX = dragState.startX + dragState.accumulatedDragX
                            var newY = dragState.startY + dragState.accumulatedDragY
                            
                            // Snapping Logic
                            val snapThreshold = 10f
                            val otherDevices = install.devices.filter { it.ip != device.ip }
                            
                            // Snap X
                            for (other in otherDevices) {
                                if (kotlin.math.abs(newX - other.x) < snapThreshold) newX = other.x
                                if (kotlin.math.abs(newX + device.width - (other.x + other.width)) < snapThreshold) newX = other.x + other.width - device.width
                                if (kotlin.math.abs(newX - (other.x + other.width)) < snapThreshold) newX = other.x + other.width
                                if (kotlin.math.abs(newX + device.width - other.x) < snapThreshold) newX = other.x - device.width
                            }
                            if (kotlin.math.abs(newX) < snapThreshold) newX = 0f
                            
                            // Snap Y
                            for (other in otherDevices) {
                                if (kotlin.math.abs(newY - other.y) < snapThreshold) newY = other.y
                                if (kotlin.math.abs(newY + device.height - (other.y + other.height)) < snapThreshold) newY = other.y + other.height - device.height
                                if (kotlin.math.abs(newY - (other.y + other.height)) < snapThreshold) newY = other.y + other.height
                                if (kotlin.math.abs(newY + device.height - other.y) < snapThreshold) newY = other.y - device.height
                            }
                            if (kotlin.math.abs(newY) < snapThreshold) newY = 0f
                            
                            // Calculate offsets to determine screen boundaries in virtual space
                            val offsetX = (canvasWidth - install.width * scale) / 2f
                            val offsetY = 0f // Align Top (Match Player)
                            
                            // Visual Boundaries (Virtual Coordinates at Screen Edges)
                            // Top Edge of Screen (y=0) -> Virtual Y = (0 - offsetY) / scale
                            // Bottom Edge of Screen (y=H) -> Virtual Y = (H - offsetY) / scale
                            
                            val minVisX = -offsetX / scale
                            val minVisY = -offsetY / scale
                            val maxVisX = (canvasWidth - offsetX) / scale
                            val maxVisY = (canvasHeight - offsetY) / scale
                            
                            // Constraint Logic: Keep inside PHYSICAL SCREEN bounds
                            // This allows dragging into the "black bars" (negative coords or > 1000)
                            // which solves the "Top Fourth" dead zone issue.
                            
                            newX = newX.coerceIn(minVisX, maxVisX - device.width)
                            newY = newY.coerceIn(minVisY, maxVisY - device.height)

                            // Send absolute position
                            currentOnMoveDeviceState.value(
                                device,
                                newX,
                                newY,
                                device.width,
                                device.height,
                                device.rotation
                            )
                        }
                    }
                )
            }
    ) {
        val install = installation
        val width = install.width
        val height = install.height
        
        // Calculate scale (Fit Center)
        val scaleX = size.width / width
        val scaleY = size.height / height
        val scale = minOf(scaleX, scaleY) // 100% fit
        
        val offsetX = (size.width - width * scale) / 2f
        val offsetY = 0f // Align Top (Match Player)
        
        // Apply Transform
        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            // Draw Devices
            install.devices.forEach { device ->
                val isSelected = device == selectedDevice
                
                // Body
                drawRect(
                    color = if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.7f) else Color(0xFF2196F3).copy(alpha = 0.5f),
                    topLeft = Offset(device.x, device.y),
                    size = Size(device.width, device.height)
                )
                
                // Outline
                drawRect(
                    color = if (isSelected) Color.Yellow else Color.White,
                    style = Stroke(width = (if (isSelected) 4f else 2f) / scale),
                    topLeft = Offset(device.x, device.y),
                    size = Size(device.width, device.height)
                )

                // Handles
                if (isSelected) {
                    val handleRadius = 8f / scale
                    val handleColor = Color.Yellow
                    drawCircle(handleColor, handleRadius, Offset(device.x, device.y))
                    drawCircle(handleColor, handleRadius, Offset(device.x + device.width, device.y))
                    drawCircle(handleColor, handleRadius, Offset(device.x, device.y + device.height))
                    drawCircle(handleColor, handleRadius, Offset(device.x + device.width, device.y + device.height))
                }
                
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
    }
  } // End BoxWithConstraints
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
