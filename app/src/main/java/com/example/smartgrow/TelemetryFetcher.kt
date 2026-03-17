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
            val baseUrl = "https://thingsboard.cloud"
            // Ensure these are correctly defined in your build.gradle from local.properties
            val deviceId = BuildConfig.THINGSBOARD_DEVICE_ID
            val token = BuildConfig.THINGSBOARD_TOKEN

            if (token.isNullOrBlank()) {
                Log.e("TelemetryFetcher", "Token is MISSING! Check your local.properties 💀")
                return@withContext null
            }

            // ThingsBoard API path for latest telemetry
            val urlString = "$baseUrl/api/plugins/telemetry/DEVICE/$deviceId/values/timeseries?limit=1"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            // NOTE: Some ThingsBoard setups use "X-Authorization: $token"
            // while others use "X-Authorization: Bearer $token".
            // We'll stick to Bearer but check your TB settings if 401 persists.
            connection.setRequestProperty("X-Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val result = mutableMapOf<String, String>()

                val keysIterator = json.keys()
                while (keysIterator.hasNext()) {
                    val key = keysIterator.next()
                    val dataArray = json.getJSONArray(key)
                    if (dataArray.length() > 0) {
                        // ThingsBoard returns an array of {ts: long, value: string}
                        val latestEntry = dataArray.getJSONObject(0)
                        result[key] = latestEntry.get("value").toString()
                    }
                }
                Log.d("TelemetryFetcher", "Sync Successful: $result")
                return@withContext result
            } else {
                // Read error stream for more clues
                val errorMsg = connection.errorStream?.bufferedReader()?.readText() ?: "No error body"
                Log.e("TelemetryFetcher", "Fetch failed: $responseCode - $errorMsg")
            }
        } catch (e: Exception) {
            Log.e("TelemetryFetcher", "Network Error: ${e.message}")
        }
        return@withContext null
    }
}