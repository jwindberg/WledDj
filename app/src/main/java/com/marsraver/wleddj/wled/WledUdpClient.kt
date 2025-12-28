package com.marsraver.wleddj.wled

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class WledUdpClient {
    private val socket = DatagramSocket()
    private val addressCache = ConcurrentHashMap<String, InetAddress>()

    suspend fun sendFrame(ip: String, data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val dataLen = data.size
            val packetLen = 10 + dataLen
            
            // WARLS / DRGB header
            val buffer = ByteArray(packetLen)
            buffer[0] = 0x41.toByte() // Protocol ID
            buffer[1] = 0.toByte()    // Timeout (0 = default)
            buffer[2] = 1.toByte()    // Sequence number (can be static for simpler implementations)
            buffer[3] = 1.toByte()    // Options
            
            // Little-endian length? Original code did:
            // buffer[8] = (dataLen shr 8).toByte() -> High byte
            // buffer[9] = (dataLen and 0xFF).toByte() -> Low byte
            // That's big-endian (network byte order), usually.
            buffer[8] = (dataLen shr 8).toByte()
            buffer[9] = (dataLen and 0xFF).toByte()
            
            System.arraycopy(data, 0, buffer, 10, dataLen)
            
            val address = addressCache.getOrPut(ip) { InetAddress.getByName(ip) }
            socket.send(DatagramPacket(buffer, packetLen, address, 4048))
        } catch (e: Exception) {
            // Log or ignore
        }
    }

    fun close() {
        try {
            socket.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
