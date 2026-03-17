package com.example.smartgrow

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object TelemetryFetcher {

    // Cache the JWT in memory. We'll refresh it only when it expires.
    private var cachedJwt: String? = null

    // Mutex ensures we don't spam the login API if multiple fragments request data at the exact same time
    private val mutex = Mutex()

    // Helper class to return both the HTTP code and the data
    private data class FetchResult(val responseCode: Int, val data: Map<String, String>?)

    suspend fun fetchTelemetry(): Map<String, String>? = withContext(Dispatchers.IO) {
        // Try fetching data with the current token
        var result = attemptFetch(cachedJwt)

        // 401 Unauthorized means token expired or is missing. Auto-login to get a fresh one!
        if (result.responseCode == 401 || cachedJwt == null) {
            Log.d("TelemetryFetcher", "Token expired (401) or missing. Fetching a fresh JWT...")
            val freshToken = loginAndGetJwt()

            if (freshToken != null) {
                cachedJwt = freshToken
                // Retry the fetch immediately with the new token
                result = attemptFetch(freshToken)
            }
        }

        if (result.responseCode == 403) {
            // 403 means the token is VALID, but ThingsBoard rejected the telemetry fetch.
            // Let's run a diagnostic check to find out exactly why!
            Log.e("TelemetryFetcher", "💀 403 Forbidden on Telemetry. Running server diagnostic...")
            cachedJwt?.let { runDiagnosticCheck(it) }
        }

        return@withContext result.data
    }

    private fun attemptFetch(jwt: String?): FetchResult {
        if (jwt.isNullOrBlank()) return FetchResult(401, null)

        try {
            // Strip any accidental spaces or quotes from local.properties
            val baseUrl = BuildConfig.THINGSBOARD_BASE_URL.trim().removeSurrounding("\"")
            val deviceId = BuildConfig.THINGSBOARD_DEVICE_ID.trim().removeSurrounding("\"")

            // =========================================================================
            // STEP 1: Fetch the available telemetry keys dynamically
            // =========================================================================
            val keysUrl = URL("$baseUrl/api/plugins/telemetry/DEVICE/$deviceId/keys/timeseries")
            val keysConn = keysUrl.openConnection() as HttpURLConnection
            keysConn.requestMethod = "GET"
            keysConn.setRequestProperty("X-Authorization", "Bearer $jwt")
            keysConn.setRequestProperty("Accept", "application/json")
            keysConn.connectTimeout = 5000
            keysConn.readTimeout = 5000

            val keysCode = keysConn.responseCode
            if (keysCode != 200) {
                Log.e("TelemetryFetcher", "Keys fetch failed: $keysCode")
                return FetchResult(keysCode, null)
            }

            val keysResponse = keysConn.inputStream.bufferedReader().readText()
            val keysArray = JSONArray(keysResponse)
            val keysList = mutableListOf<String>()
            for (i in 0 until keysArray.length()) {
                keysList.add(keysArray.getString(i))
            }

            if (keysList.isEmpty()) {
                Log.d("TelemetryFetcher", "No telemetry keys found for this device yet.")
                return FetchResult(200, emptyMap())
            }

            // =========================================================================
            // STEP 2: Fetch the actual values using the keys we just found
            // =========================================================================
            val keysString = keysList.joinToString(",")
            val valuesUrl = URL("$baseUrl/api/plugins/telemetry/DEVICE/$deviceId/values/timeseries?keys=$keysString")
            val valuesConn = valuesUrl.openConnection() as HttpURLConnection
            valuesConn.requestMethod = "GET"
            valuesConn.setRequestProperty("X-Authorization", "Bearer $jwt")
            valuesConn.setRequestProperty("Accept", "application/json")
            valuesConn.connectTimeout = 5000
            valuesConn.readTimeout = 5000

            val valCode = valuesConn.responseCode
            if (valCode == 200) {
                val valResponse = valuesConn.inputStream.bufferedReader().readText()
                val json = JSONObject(valResponse)
                val resultData = mutableMapOf<String, String>()

                val keysIterator = json.keys()
                while (keysIterator.hasNext()) {
                    val key = keysIterator.next()
                    val dataArray = json.getJSONArray(key)
                    if (dataArray.length() > 0) {
                        resultData[key] = dataArray.getJSONObject(0).get("value").toString()
                    }
                }
                Log.d("TelemetryFetcher", "Telemetry Sync Successful: $resultData")
                return FetchResult(200, resultData)
            } else {
                Log.e("TelemetryFetcher", "Values fetch failed: $valCode")
                return FetchResult(valCode, null)
            }

        } catch (e: Exception) {
            Log.e("TelemetryFetcher", "Network Error: ${e.message}")
            return FetchResult(-1, null)
        }
    }

    private suspend fun loginAndGetJwt(): String? = mutex.withLock {
        try {
            val baseUrl = BuildConfig.THINGSBOARD_BASE_URL.trim().removeSurrounding("\"")
            val username = BuildConfig.THINGSBOARD_USERNAME.trim().removeSurrounding("\"")
            val password = BuildConfig.THINGSBOARD_PASSWORD.trim().removeSurrounding("\"")

            if (username.isBlank() || password.isBlank()) {
                Log.e("TelemetryFetcher", "Missing username/password in local.properties")
                return null
            }

            val url = URL("$baseUrl/api/auth/login")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true

            val payload = JSONObject().apply {
                put("username", username)
                put("password", password)
            }

            OutputStreamWriter(connection.outputStream).use { it.write(payload.toString()) }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val token = JSONObject(response).getString("token")
                Log.d("TelemetryFetcher", "Successfully generated new JWT!")
                return token
            } else {
                Log.e("TelemetryFetcher", "Login failed: ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e("TelemetryFetcher", "Login API Error: ${e.message}")
        }
        return null
    }

//    // Diagnostics function to figure out WHY ThingsBoard is throwing a 403
//    private fun runDiagnosticCheck(jwt: String) {
//        try {
//            val baseUrl = BuildConfig.THINGSBOARD_BASE_URL.trim().removeSurrounding("\"")
//            val deviceId = BuildConfig.THINGSBOARD_DEVICE_ID.trim().removeSurrounding("\"")
//
//            val url = URL("$baseUrl/api/device/$deviceId")
//            val conn = url.openConnection() as HttpURLConnection
//            conn.requestMethod = "GET"
//            conn.setRequestProperty("X-Authorization", "Bearer $jwt")
//            conn.setRequestProperty("Accept", "application/json")
//
//            val code = conn.responseCode
//            if (code == 200) {
//                val response = conn.inputStream.bufferedReader().readText()
//                val name = JSONObject(response).optString("name", "Unknown")
//                Log.e("TelemetryFetcher", "✅ DIAGNOSTIC: Device '$name' exists and is readable! The 403 is purely on the Telemetry plugin. Check if the device has actual telemetry data uploaded yet, or if TB Cloud limits are blocking you.")
//            } else if (code == 404 || code == 400) {
//                Log.e("TelemetryFetcher", "🚨 DIAGNOSTIC ($code): The UUID $deviceId is NOT A DEVICE! You likely copied the 'Device Profile ID' or an 'Asset ID'. Go to Devices -> Click your device -> Copy the first ID, not the Profile ID.")
//            } else if (code == 403) {
//                Log.e("TelemetryFetcher", "🚨 DIAGNOSTIC (403): You don't even have permission to view the device entity itself. Ensure the Tenant ID matches.")
//            } else {
//                Log.e("TelemetryFetcher", "Diagnostic returned unexpected code: $code")
//            }
//        } catch (e: Exception) {
//            Log.e("TelemetryFetcher", "Diagnostic failed to run: ${e.message}")
//        }
//    }
// Diagnostics to decode the JWT and expose the app's true identity
private fun runDiagnosticCheck(jwt: String) {
    try {
        val parts = jwt.split(".")
        if (parts.size == 3) {
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            Log.e("TelemetryFetcher", "🕵️ ACTUAL JWT PAYLOAD: $payload")

            if (payload.contains("\"CUSTOMER_USER\"")) {
                Log.e("TelemetryFetcher", "🚨 BUSTED! The app logged in as a CUSTOMER_USER, not a TENANT_ADMIN. That's why it can't see the Tenant's devices!")
            } else if (!payload.contains("smartgrow92@gmail.com")) {
                Log.e("TelemetryFetcher", "🚨 BUSTED! The app logged into the wrong account!")
            }
        } else {
            Log.e("TelemetryFetcher", "🚨 Invalid JWT format returned by server!")
        }
    } catch (e: Exception) {
        Log.e("TelemetryFetcher", "Diagnostic crashed: ${e.message}")
    }
}
}