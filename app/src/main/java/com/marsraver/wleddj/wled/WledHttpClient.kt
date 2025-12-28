package com.marsraver.wleddj.wled

import com.marsraver.wleddj.wled.model.WledInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class WledHttpClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getDeviceInfo(ip: String): WledInfoResponse? = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip/json/info")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<WledInfoResponse>(text)
            } else {
                null
            }
        } catch (e: Exception) {
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
            false
        }
    }

    suspend fun pingDevice(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip/json/state")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 1000
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
    suspend fun getDeviceConfig(ip: String): com.marsraver.wleddj.wled.model.WledConfigResponse? = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$ip/json/cfg")
            android.util.Log.d("WledHttpClient", "Fetching config from $url")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.requestMethod = "GET"
            
            if (connection.responseCode == 200) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                android.util.Log.d("WledHttpClient", "Config JSON: $text")
                json.decodeFromString<com.marsraver.wleddj.wled.model.WledConfigResponse>(text)
            } else {
                android.util.Log.e("WledHttpClient", "Error fetching config: ${connection.responseCode}")
                null
            }
        } catch (e: Exception) {
            // Fallback or Log
            android.util.Log.e("WledHttpClient", "Failed to fetch config for $ip: ${e.message}")
            null
        }
    }
}
