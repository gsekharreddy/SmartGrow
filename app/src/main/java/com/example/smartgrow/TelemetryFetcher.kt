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
            // Using the endpoint you provided
            val urlString = "${BuildConfig.THINGSBOARD_BASE_URL}/api/plugins/telemetry/DEVICE/${BuildConfig.THINGSBOARD_DEVICE_ID}/values/timeseries?limit=1"
            Log.d("TelemetryFetcher", "Fetching from: $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            connection.setRequestProperty(
                "X-Authorization",
                "Bearer ${BuildConfig.THINGSBOARD_TOKEN}"
            )
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            Log.d("TelemetryFetcher", "Response Code: $responseCode")

            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                Log.d("TelemetryFetcher", "Success Response: $response")

                val json = JSONObject(response)
                val result = mutableMapOf<String, String>()

                // Safely loop through the keys
                val iter = json.keys()
                while (iter.hasNext()) {
                    val key = iter.next()
                    try {
                        val array = json.getJSONArray(key)
                        if (array.length() > 0) {
                            val value = array.getJSONObject(0).get("value").toString()
                            result[key] = value
                        }
                    } catch (e: Exception) {
                        // Just in case the value isn't an array
                        result[key] = json.getString(key)
                    }
                }

                Log.d("TelemetryFetcher", "Parsed Data: $result")
                return@withContext result
            } else {
                // If it fails (like a 401 Unauthorized), let's read the error message!
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                Log.e("TelemetryFetcher", "Error Response: $errorResponse")
            }
        } catch (e: Exception) {
            Log.e("TelemetryFetcher", "Crash during fetch: ${e.message}")
            e.printStackTrace()
        }
        return@withContext null
    }
}