package com.example.smartgrow

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object TelemetryFetcher {

    suspend fun fetchTelemetry(): Map<String, String>? = withContext(Dispatchers.IO) {
        try {
            // Using BuildConfig to keep secrets out of GitHub
            val baseUrl = "https://thingsboard.cloud"
            val deviceId = BuildConfig.THINGSBOARD_DEVICE_ID
            val token = BuildConfig.THINGSBOARD_TOKEN

            val urlString = "$baseUrl/api/plugins/telemetry/DEVICE/$deviceId/values/timeseries?limit=1"
            Log.d("TelemetryFetcher", "Syncing with Cloud...")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("X-Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val result = mutableMapOf<String, String>()

                val keysIterator = json.keys()
                while (keysIterator.hasNext()) {
                    val key = keysIterator.next()
                    val dataArray = json.getJSONArray(key)
                    if (dataArray.length() > 0) {
                        result[key] = dataArray.getJSONObject(0).get("value").toString()
                    }
                }
                return@withContext result
            } else {
                Log.e("TelemetryFetcher", "Fetch failed: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("TelemetryFetcher", "Safe Mode Error: ${e.message}")
        }
        return@withContext null
    }
}