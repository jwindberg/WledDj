package com.marsraver.wleddj.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class WledInfoResponse(
    val leds: WledLedsInfo,
    val name: String
)

@Serializable
data class WledLedsInfo(
    val count: Int,
    val w: Int = 0,
    val h: Int = 0
)

object WledApiHelper {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getDeviceInfo(ip: String): WledInfoResponse? = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("WledApiHelper", "Fetching info for $ip")
            val url = URL("http://$ip/json/info")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                android.util.Log.d("WledApiHelper", "Response for $ip: $text")
                json.decodeFromString<WledInfoResponse>(text)
            } else {
                android.util.Log.e("WledApiHelper", "Error $ip: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("WledApiHelper", "Exception fetching $ip", e)
            null
        }
    }


    suspend fun rebootDevice(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip/json/state")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            val jsonBody = "{\"rb\":true}"
            connection.outputStream.use { it.write(jsonBody.toByteArray()) }
            
            val code = connection.responseCode
            code == 200
        } catch (e: Exception) {
            android.util.Log.e("WledApiHelper", "Error rebooting $ip", e)
            false
        }
    }


    suspend fun pingDevice(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip/json/state")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 1000 // 1s Timeout
            connection.readTimeout = 1000
            connection.requestMethod = "GET"
            
            val code = connection.responseCode
            if (code == 200) {
                try { connection.inputStream.close() } catch (ignored: Exception) {}
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
