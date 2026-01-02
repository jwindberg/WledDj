package com.marsraver.wleddj.wled

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class WledUdpClient {
    private val socket = DatagramSocket()
    private val addressCache = ConcurrentHashMap<String, InetAddress>()

    companion object {
        const val MAX_PIXELS_PER_PACKET = 480
    }

    suspend fun sendFrame(ip: String, data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val totalPixels = data.size / 3
            var pixelOffset = 0
            
            while (pixelOffset < totalPixels) {
                val pixelsToSend = minOf(MAX_PIXELS_PER_PACKET, totalPixels - pixelOffset)
                val chunkLen = pixelsToSend * 3
                val packetLen = 10 + chunkLen
                val isLastChunk = (pixelOffset + pixelsToSend) >= totalPixels
                
                // Header (DDP v1)
                val buffer = ByteArray(packetLen)
                // Flag: Push (0x41) only on last chunk, otherwise 0x40 to buffer (No Push)
                // This prevents partial frame updates (tearing)
                val flags = if (isLastChunk) 0x41 else 0x40
                buffer[0] = flags.toByte()

                buffer[1] = 0.toByte()    // Sequence
                buffer[2] = 1.toByte()    // Type: RGB
                buffer[3] = 1.toByte()    // ID
                
                // Offset (Big Endian 32-bit int, offset in PIXELS)
                buffer[4] = (pixelOffset shr 24).toByte()
                buffer[5] = (pixelOffset shr 16).toByte()
                buffer[6] = (pixelOffset shr 8).toByte()
                buffer[7] = (pixelOffset and 0xFF).toByte()
                
                // Length (Big Endian 16-bit int, length in BYTES)
                buffer[8] = (chunkLen shr 8).toByte()
                buffer[9] = (chunkLen and 0xFF).toByte()
                
                System.arraycopy(data, pixelOffset * 3, buffer, 10, chunkLen)
                
                val address = addressCache.getOrPut(ip) { InetAddress.getByName(ip) }
                socket.send(DatagramPacket(buffer, packetLen, address, 4048))
                
                // Flow Control: Yield on intermediate packets to let device process
                if (!isLastChunk) {
                    delay(5) 
                }
                
                pixelOffset += pixelsToSend
            }
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
