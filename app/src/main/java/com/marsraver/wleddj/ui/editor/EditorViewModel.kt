package com.marsraver.wleddj.ui.editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.marsraver.wleddj.data.model.Installation
import com.marsraver.wleddj.data.model.WledDevice
import com.marsraver.wleddj.data.repository.InstallationRepository
import com.marsraver.wleddj.wled.WledDiscoveryClient
import com.marsraver.wleddj.wled.WledHttpClient
import com.marsraver.wleddj.wled.model.DiscoveredDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class EditorViewModel(
    private val installationId: String,
    private val repository: InstallationRepository,
    context: Context
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
            
            // Determine dimensions
            val wledW = info?.leds?.w ?: 0
            val wledH = info?.leds?.h ?: 0
            
            val (width, height) = if (wledW > 0 && wledH > 0) {
                 // Use WLED provided dimensions (scaled to some reasonable virtual size)
                 val ratio = wledH.toFloat() / wledW.toFloat()
                 200f to (200f * ratio)
            } else {
                // Heuristic: Check if square
                val sqrt = kotlin.math.sqrt(pixelCount.toFloat())
                if (sqrt % 1.0 == 0.0) {
                    // It's a square!
                    200f to 200f
                } else {
                    // Default strip
                    200f to 50f
                }
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
            val isMatrix = (wledW > 0) || (kotlin.math.sqrt(pixelCount.toFloat()) % 1.0 == 0.0)

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
                segmentWidth = if (wledW > 0) wledW else if (isMatrix) kotlin.math.sqrt(pixelCount.toFloat()).toInt() else 0
            )

            val updated = current.copy(devices = current.devices + newDevice)
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
                it.copy(
                    segmentWidth = segmentWidth
                )
            } else it
        }
        updateInstallation(current.copy(devices = updatedDevices))
    }
    
    fun forceRefreshDeviceConfig(device: WledDevice) {
        viewModelScope.launch {
            val config = httpClient.getDeviceConfig(device.ip)
            // Need to parse config similar to previous logic
            // WledConfigResponse -> WledDevice
            // BUT wait, WledHttpClient returns WledConfigResponse which has `hw.led.matrix`
            
            val matrix = config?.hw?.led?.matrix
            val panel = matrix?.panels?.firstOrNull()
            
            if (panel != null) {
                 val updated = device.copy(
                     segmentWidth = panel.w
                 )
                 
                 // Update device in installation
                 val current = _installation.value ?: return@launch
                 val newDevices = current.devices.map { if (it.ip == device.ip) updated else it }
                 updateInstallation(current.copy(devices = newDevices))
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
