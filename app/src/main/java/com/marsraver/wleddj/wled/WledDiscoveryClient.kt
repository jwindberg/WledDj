package com.marsraver.wleddj.wled

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.marsraver.wleddj.wled.model.DiscoveredDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ConcurrentLinkedQueue

class WledDiscoveryClient(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_wled._tcp."
    private val tag = "WledDiscoveryClient"

    fun discoverDevices(): Flow<List<DiscoveredDevice>> = callbackFlow {
        val foundDevices = mutableListOf<DiscoveredDevice>()
        trySend(foundDevices.toList()) // Initial empty list

        val resolveQueue = ConcurrentLinkedQueue<NsdServiceInfo>()
        var isResolving = false

        fun processQueue() {
            synchronized(resolveQueue) {
                if (isResolving) return
                val service = resolveQueue.poll() ?: return
                isResolving = true

                val resolveListener = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        synchronized(resolveQueue) { isResolving = false }
                        processQueue()
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host
                        if (host != null) {
                            val device = DiscoveredDevice(
                                name = serviceInfo.serviceName,
                                ip = host.hostAddress ?: "",
                                port = serviceInfo.port
                            )
                            synchronized(foundDevices) {
                                if (foundDevices.none { it.ip == device.ip }) {
                                    foundDevices.add(device)
                                    trySend(foundDevices.toList())
                                }
                            }
                        }
                        synchronized(resolveQueue) { isResolving = false }
                        processQueue()
                    }
                }

                try {
                    nsdManager.resolveService(service, resolveListener)
                } catch (e: Exception) {
                    synchronized(resolveQueue) { isResolving = false }
                    processQueue()
                }
            }
        }

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("_wled") || 
                   (service.serviceType.contains("_http") && service.serviceName.contains("wled", ignoreCase = true))) {
                     resolveQueue.offer(service)
                     processQueue()
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                synchronized(foundDevices) {
                    foundDevices.removeAll { it.name == service.serviceName }
                    trySend(foundDevices.toList())
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
        }

        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {}
        }
    }
}
