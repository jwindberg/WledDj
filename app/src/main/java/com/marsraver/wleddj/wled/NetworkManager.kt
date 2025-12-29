package com.marsraver.wleddj.wled

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.marsraver.wleddj.model.WledDevice

class NetworkManager {

    private val httpClient = WledHttpClient()
    
    private val _deviceStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val deviceStatuses = _deviceStatuses.asStateFlow()
    
    private var monitorJob: Job? = null
    
    // We need to know which devices to monitor
    private var targets: List<WledDevice> = emptyList()
    
    fun setTargets(devices: List<WledDevice>) {
        targets = devices
        // Trigger immediate update if running
        if (monitorJob?.isActive == true) {
            // Logic implies next loop will pick it up
        }
    }
    
    fun startMonitoring(scope: CoroutineScope) {
        if (monitorJob?.isActive == true) return
        
        monitorJob = scope.launch {
            while (isActive) {
                if (targets.isNotEmpty()) {
                    // Parallel pings
                    val tasks = targets.map { device ->
                        device.ip to async { httpClient.pingDevice(device.ip) }
                    }
                    
                    val results = mutableMapOf<String, Boolean>()
                    for ((ip, task) in tasks) {
                        results[ip] = task.await()
                    }
                    _deviceStatuses.value = results
                }
                delay(5000)
            }
        }
    }
    
    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    // Helper for single check
    suspend fun ping(ip: String): Boolean {
        return httpClient.pingDevice(ip)
    }
}
