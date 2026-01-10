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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.res.stringResource
import com.marsraver.wleddj.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.marsraver.wleddj.model.Installation
import com.marsraver.wleddj.model.WledDevice
import com.marsraver.wleddj.repository.FileInstallationRepository
import com.marsraver.wleddj.wled.model.DiscoveredDevice
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
    val repository = com.marsraver.wleddj.repository.RepositoryProvider.getRepository(context)
    val viewModel: EditorViewModel = viewModel(
        factory = EditorViewModel.Factory(installationId, repository, context)
    )

    val installationState by viewModel.installation.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    
    val installation: com.marsraver.wleddj.model.Installation = installationState ?: return

    var showAddSheet by remember { mutableStateOf(false) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedDeviceIp: String? by remember { mutableStateOf(null) }

    // Derive the selected device object from the current installation
    val selectedDevice = remember(installation, selectedDeviceIp) {
        installation.devices.find { it.ip == selectedDeviceIp }
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
                        Text(stringResource(R.string.device_layout_title))
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
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showRebootDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reboot_all),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (selectedDevice != null) {
                        IconButton(onClick = { showDetailsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.device_settings),
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
                            contentDescription = stringResource(R.string.animation_layout_title),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.navigationBarsPadding() // Prevent nav bar overlap in landscape
            ) {
                if (selectedDevice != null) {
                    FloatingActionButton(
                        onClick = { 
                            selectedDevice?.let { viewModel.removeDevice(it) }
                            selectedDeviceIp = null
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                         Icon(Icons.Default.Remove, stringResource(R.string.delete_device))
                    }
                }

                FloatingActionButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Default.Add, stringResource(R.string.add_device))
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
                    Text(stringResource(R.string.add_device), style = MaterialTheme.typography.titleLarge)
                    if (discoveredDevices.isEmpty()) {
                        Text(stringResource(R.string.scanning_devices), modifier = Modifier.padding(vertical = 16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LazyColumn {
                            items(discoveredDevices) { device ->
                                // Filter out already added devices visually
                                val isAdded = installation.devices.any { it.ip == device.ip } == true
                                if (!isAdded) {
                                    DiscoveredDeviceItem(device) {
                                        viewModel.addDevice(device)
                                        selectedDeviceIp = device.ip // Auto-select the added device
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
            title = { Text(stringResource(R.string.reboot_dialog_title)) },
            text = { Text(stringResource(R.string.reboot_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rebootAllDevices()
                    showRebootDialog = false
                }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDetailsDialog && selectedDevice != null) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.device_details_title))
                    IconButton(onClick = { viewModel.forceRefreshDeviceConfig(selectedDevice) }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.refresh))
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = selectedDevice.name, style = MaterialTheme.typography.titleLarge)
                    HorizontalDivider()
                    DetailRow(stringResource(R.string.ip_address), selectedDevice.ip)
                    // DetailRow(stringResource(R.string.mac_address), selectedDevice.macAddress) // User explicitly removed
                    HorizontalDivider()
                    DetailRow(stringResource(R.string.pixel_count), selectedDevice.pixelCount.toString())
                    
                    if (selectedDevice.is2D) {
                        Text(stringResource(R.string.panel_info), style = MaterialTheme.typography.labelLarge)
                        DetailRow(stringResource(R.string.matrix_dimensions), "${selectedDevice.matrixWidth} x ${selectedDevice.matrixHeight}")
                        
                        val serpentineStr = stringResource(if (selectedDevice.serpentine) R.string.yes else R.string.no)
                        DetailRow(stringResource(R.string.serpentine), serpentineStr)
                        
                        DetailRow(stringResource(R.string.first_led), selectedDevice.firstLed)
                        DetailRow(stringResource(R.string.orientation), selectedDevice.orientation)
                        
                        if (selectedDevice.panelDescription.isNotEmpty()) {
                             Text(selectedDevice.panelDescription, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        DetailRow(stringResource(R.string.segment_width), selectedDevice.segmentWidth.toString())
                    }
                    
                    HorizontalDivider()
                    Text("LED Spacing (units/LED, default 5.0)", style = MaterialTheme.typography.labelLarge)
                    
                    var hSpacingText by remember { mutableStateOf(selectedDevice.horizontalLedSpacing.toString()) }
                    var vSpacingText by remember { mutableStateOf(selectedDevice.verticalLedSpacing.toString()) }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hSpacingText,
                            onValueChange = { 
                                hSpacingText = it
                                val f = it.toFloatOrNull()
                                if (f != null && f > 0) {
                                    viewModel.updateDeviceSpacing(selectedDevice.ip, f, selectedDevice.verticalLedSpacing)
                                }
                            },
                            label = { Text("Horiz (Width)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = vSpacingText,
                            onValueChange = { 
                                vSpacingText = it
                                val f = it.toFloatOrNull()
                                if (f != null && f > 0) {
                                    viewModel.updateDeviceSpacing(selectedDevice.ip, selectedDevice.horizontalLedSpacing, f)
                                }
                            },
                            label = { Text("Vert (Height)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}



@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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
