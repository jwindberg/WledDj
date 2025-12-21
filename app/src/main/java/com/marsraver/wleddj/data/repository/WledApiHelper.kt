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
}
