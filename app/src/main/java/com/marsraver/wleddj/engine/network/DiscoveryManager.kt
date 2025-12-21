package com.marsraver.wleddj.engine.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.InetAddress

data class DiscoveredDevice(
    val name: String,
    val ip: String,
    val port: Int
)

class DiscoveryManager(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_wled._tcp." // Or _http._tcp.
    private val tag = "DiscoveryManager"

    fun discoverDevices(): Flow<List<DiscoveredDevice>> = callbackFlow {
        val foundDevices = mutableListOf<DiscoveredDevice>()
        // Initial emit
        trySend(foundDevices.toList())

        // Queue for sequential resolution
        val resolveQueue = java.util.concurrent.ConcurrentLinkedQueue<NsdServiceInfo>()
        var isResolving = false

        fun processQueue() {
            synchronized(resolveQueue) {
                if (isResolving) return
                val service = resolveQueue.poll() ?: return
                isResolving = true

                Log.d(tag, "Resolving: ${service.serviceName}")

                val resolveListener = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e(tag, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                        synchronized(resolveQueue) {
                            isResolving = false
                        }
                        processQueue()
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        Log.d(tag, "Resolve Succeeded: ${serviceInfo.serviceName}")
                        val host = serviceInfo.host
                        if (host != null) {
                            val device = DiscoveredDevice(
                                name = serviceInfo.serviceName,
                                ip = host.hostAddress ?: "",
                                port = serviceInfo.port
                            )
                            // Avoid duplicates
                            synchronized(foundDevices) {
                                if (foundDevices.none { it.ip == device.ip }) {
                                    foundDevices.add(device)
                                    trySend(foundDevices.toList())
                                }
                            }
                        }
                        synchronized(resolveQueue) {
                            isResolving = false
                        }
                        processQueue()
                    }
                }
                
                try {
                    nsdManager.resolveService(service, resolveListener)
                } catch (e: Exception) {
                    Log.e(tag, "Error starting resolve for ${service.serviceName}", e)
                     synchronized(resolveQueue) {
                        isResolving = false
                    }
                    processQueue()
                }
            }
        }

        val discoveryListener = object : NsdManager.DiscoveryListener {
             override fun onDiscoveryStarted(regType: String) {
                Log.d(tag, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(tag, "Service discovery success $service")
                if (service.serviceType.contains("_wled") || 
                   (service.serviceType.contains("_http") && service.serviceName.contains("wled", ignoreCase = true))) {
                     resolveQueue.offer(service)
                     processQueue()
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e(tag, "service lost: $service")
                synchronized(foundDevices) {
                    foundDevices.removeAll { it.name == service.serviceName }
                    trySend(foundDevices.toList())
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(tag, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }

        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(tag, "Error starting discovery", e)
            close(e)
        }

        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                // Ignore if already stopped
            }
        }
    }
}
