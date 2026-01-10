package com.marsraver.wleddj.ui.editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.marsraver.wleddj.model.Installation
import com.marsraver.wleddj.model.WledDevice
import com.marsraver.wleddj.repository.InstallationRepository
import com.marsraver.wleddj.wled.WledDiscoveryClient
import com.marsraver.wleddj.wled.WledHttpClient
import com.marsraver.wleddj.wled.model.DiscoveredDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditorViewModel(
    private val installationId: String,
    private val repository: InstallationRepository,
    private val context: Context
) : ViewModel() {

    private val _installation = MutableStateFlow<Installation?>(null)
    val installation = _installation.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private val discoveryClient = WledDiscoveryClient(context)
    private val httpClient = WledHttpClient()

    init {
        loadInstallation()
        startDiscovery()
    }

    private fun loadInstallation() {
        viewModelScope.launch {
            // Observe the repository flow to keep data fresh (e.g. if Player modified animations)
            repository.installations.collect { list ->
                val updated = list.find { it.id == installationId }
                if (updated != null) {
                    _installation.value = updated
                }
            }
        }
    }

    private val resolvedNames = mutableMapOf<String, String>()

    private fun startDiscovery() {
        viewModelScope.launch {
            discoveryClient.discoverDevices().collect { devices ->
                // Update basic list first, merging with known names
                val mergedList = devices.map { device ->
                    device.copy(name = resolvedNames[device.ip] ?: device.name)
                }
                _discoveredDevices.value = mergedList
                
                // Enhance with names for those not yet resolved or default
                devices.forEach { device ->
                    if (!resolvedNames.containsKey(device.ip)) {
                        launch {
                            val info = httpClient.getDeviceInfo(device.ip)
                            if (info != null && info.name != device.name) {
                                updateDiscoveredDeviceName(device.ip, info.name)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateDiscoveredDeviceName(ip: String, newName: String) {
        resolvedNames[ip] = newName
        val current = _discoveredDevices.value
        val updated = current.map {
            if (it.ip == ip) it.copy(name = newName) else it
        }
        _discoveredDevices.value = updated
    }

    fun addDevice(discovered: DiscoveredDevice) {
        viewModelScope.launch {
            // Check for duplicates
            val current = _installation.value ?: return@launch
            if (current.devices.any { it.ip == discovered.ip }) return@launch

            // Fetch info
            val info = httpClient.getDeviceInfo(discovered.ip)
            val name = info?.name ?: discovered.name
            val pixelCount = info?.leds?.count ?: 100 // Default
            
            // Limit Check: 480 LEDs
            if (pixelCount > 480) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: Devices with more than 480 LEDs are not supported.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }
            
            // Determine dimensions
            val wledW = info?.leds?.w ?: 0
            val rawH = info?.leds?.h ?: 0
            // If H is 0 but W is > 0, infer H
            val wledH = if (rawH > 0) rawH else if (wledW > 0 && pixelCount > 0) pixelCount / wledW else 0
            
            val (width, height) = if (wledW > 0 && wledH > 0) {
                 // Matrix: Use matrix dimensions * spacing
                 (wledW * 5f) to (wledH * 5f)
            } else {
                // Default strip: Length * spacing
                (pixelCount * 5f) to 5f
            }

            // Find free position
            var startX = 50f
            var startY = 50f
            while (current.devices.any { 
                it.x < startX + width && it.x + it.width > startX &&
                it.y < startY + height && it.y + it.height > startY
            }) {
                startX += 20f
                startY += 20f
            }
            
            // Auto-detect matrix fields
            val isMatrix = (wledW > 0)

            val newDevice = WledDevice(
                ip = discovered.ip,
                macAddress = UUID.randomUUID().toString(),
                name = name,
                pixelCount = pixelCount,
                x = startX,
                y = startY,
                width = width,
                height = height,
                rotation = 0f,
                segmentWidth = if (wledW > 0) wledW else 0,
                is2D = false,
                matrixWidth = wledW,
                matrixHeight = wledH,
                serpentine = false,
                firstLed = "",
                orientation = "",
                panelDescription = ""
            )
            
            // Immediate config fetch to populate details
            val config = httpClient.getDeviceConfig(discovered.ip)
            val matrix = config?.hw?.led?.matrix
            val panel = matrix?.panels?.firstOrNull()
            
            val finalizedDevice = if (panel != null) {
                val vertText = if (panel.v) "Vertical" else "Horizontal"
                val startV = if (panel.b) "Bottom" else "Top"
                val startH = if (panel.r) "Right" else "Left"
                
                // Recalculate visual dimensions from panel config (definitive source)
                val pW = panel.w.toFloat()
                val pH = panel.h.toFloat()
                val visualRatio = if (pW > 0) pH / pW else 1f
                val visualW = 200f
                val visualH = 200f * visualRatio
                
                newDevice.copy(
                    is2D = true,
                    // Update visuals to match reality
                    // Update visuals to match reality with spacing
                    width = panel.w * newDevice.horizontalLedSpacing,
                    height = panel.h * newDevice.verticalLedSpacing,
                    matrixWidth = panel.w,
                    matrixHeight = panel.h,
                    serpentine = panel.s,
                    // vertical = panel.v, (Removed from model)
                    segmentWidth = panel.w, // Authority
                    firstLed = "$startV-$startH",
                    orientation = vertText,
                    panelDescription = "Panel 0 (of ${matrix.panels?.size ?: 1})"
                )
            } else newDevice

            val updated = current.copy(devices = current.devices + finalizedDevice)
            repository.updateInstallation(updated)
            _installation.value = updated
        }
    }

    fun updateDevice(ip: String, x: Float, y: Float, w: Float, h: Float, rotation: Float) {
        val current = _installation.value ?: return
        val updatedDevices = current.devices.map {
            if (it.ip == ip) it.copy(x = x, y = y, width = w, height = h, rotation = rotation) else it
        }
        val updated = current.copy(devices = updatedDevices)
        _installation.value = updated
        // Do NOT save to repository here. Use saveProject() explicitly on drag end.
    }

    fun updateDeviceMatrixConfig(ip: String, segmentWidth: Int) {
        val current = _installation.value ?: return
        val updatedDevices = current.devices.map {
            if (it.ip == ip) {
                // Recalculate height based on new width (assuming matrix behavior change for strip?)
                // Actually if changing segmentWidth on a strip to make it a matrix, we should resize.
                val rows = (it.pixelCount + segmentWidth - 1) / segmentWidth
                val newW = segmentWidth * it.horizontalLedSpacing
                val newH = rows * it.verticalLedSpacing
                
                it.copy(
                    segmentWidth = segmentWidth,
                    width = newW,
                    height = newH
                )
            } else it
        }
        updateInstallation(current.copy(devices = updatedDevices))
    }

    fun updateDeviceSpacing(ip: String, hSpacing: Float, vSpacing: Float) {
        val current = _installation.value ?: return
        val updatedDevices = current.devices.map { device ->
            if (device.ip == ip) {
                val newW: Float
                val newH: Float
                
                // Recalculate dimensions
                if (device.is2D || device.segmentWidth > 0) {
                    val cols = if (device.segmentWidth > 0) device.segmentWidth else device.matrixWidth
                     // Fallback if matrixWidth is 0 but segmentWidth > 0
                    val wToCheck = if (cols > 0) cols else 1
                    
                    val rows = if (device.matrixHeight > 0) device.matrixHeight else (device.pixelCount + wToCheck - 1) / wToCheck
                    
                    newW = wToCheck * hSpacing
                    newH = rows * vSpacing
                } else {
                    // Linear Strip
                    newW = device.pixelCount * hSpacing
                    newH = vSpacing // 1 row height
                }
                
                device.copy(
                    horizontalLedSpacing = hSpacing,
                    verticalLedSpacing = vSpacing,
                    width = newW,
                    height = newH
                )
            } else device
        }
        updateInstallation(current.copy(devices = updatedDevices))
    }
    
    fun forceRefreshDeviceConfig(device: WledDevice) {
        viewModelScope.launch {
            // 1. Fetch Info for count check
            val info = httpClient.getDeviceInfo(device.ip)
            val count = info?.leds?.count ?: 0
            
            if (count > 480) {
                 withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: Devices with more than 480 LEDs are not supported.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            val config = httpClient.getDeviceConfig(device.ip)
            
            val matrix = config?.hw?.led?.matrix
            val panel = matrix?.panels?.firstOrNull()
            
            if (panel != null) {
                 val vertText = if (panel.v) "Vertical" else "Horizontal"
                 val startV = if (panel.b) "Bottom" else "Top"
                 val startH = if (panel.r) "Right" else "Left"

                 // Recalculate Dimensions to match addDevice logic (Visual Reset)
                 // Recalculate Dimensions to match addDevice logic (Visual Reset)
                 val newW = panel.w * device.horizontalLedSpacing
                 val newH = panel.h * device.verticalLedSpacing

                 val updated = device.copy(
                     width = newW,
                     height = newH,
                     segmentWidth = panel.w,
                     is2D = true,
                     matrixWidth = panel.w,
                     matrixHeight = panel.h,
                     serpentine = panel.s, // s = serpentine
                     firstLed = "$startV-$startH",
                     orientation = vertText,
                     panelDescription = "Panel 0 (of ${matrix.panels?.size ?: 1})",
                     pixelCount = count // Also update pixel count!
                 )
                 
                 // Update device in installation
                 val current = _installation.value ?: return@launch
                 val newDevices = current.devices.map { if (it.ip == device.ip) updated else it }
                 updateInstallation(current.copy(devices = newDevices))
                 
                 withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Device Updated", Toast.LENGTH_SHORT).show()
                 }
            }
        }
    }

    fun updateViewport(zoom: Float, panX: Float, panY: Float) {
        // Deprecated
    }

    fun updateCamera(cx: Float, cy: Float, zoom: Float) {
         val current = _installation.value ?: return
         val updated = current.copy(cameraX = cx, cameraY = cy, cameraZoom = zoom)
         _installation.value = updated
    }

    fun saveProject() {
        val current = _installation.value ?: return
        android.util.Log.d("EditorViewModel", "Saving Project: Cam=${current.cameraX},${current.cameraY} Z=${current.cameraZoom}")
        viewModelScope.launch {
            repository.updateInstallation(current)
            android.util.Log.d("EditorViewModel", "Save Complete")
        }
    }

    private fun saveInstallation(installation: Installation) {
        viewModelScope.launch {
            repository.updateInstallation(installation)
        }
    }
    
    private fun updateInstallation(installation: Installation) {
         _installation.value = installation
         saveInstallation(installation)
    }

    fun removeDevice(device: WledDevice) {
        val current = _installation.value ?: return
        val updatedDevices = current.devices.filter { it.ip != device.ip }
        val updated = current.copy(devices = updatedDevices)
        _installation.value = updated
        viewModelScope.launch {
            repository.updateInstallation(updated)
        }
    }

    fun rebootAllDevices() {
        val current = _installation.value ?: return
        viewModelScope.launch {
             current.devices.forEach { device ->
                 launch {
                     httpClient.rebootDevice(device.ip)
                 }
             }
        }
    }

    // Factory
    class Factory(
        private val installationId: String,
        private val repository: InstallationRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(installationId, repository, context) as T
        }
    }
}
