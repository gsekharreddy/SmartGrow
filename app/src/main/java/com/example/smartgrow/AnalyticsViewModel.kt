package com.example.smartgrow

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AnalyticsViewModel(context: Context) : ViewModel() {

    private val aiPredictor = AiPredictor(context)

    private val _temperatureData = MutableStateFlow<List<Float>>(emptyList())
    val temperatureData = _temperatureData.asStateFlow()

    private val _soilMoistureData = MutableStateFlow<List<Float>>(emptyList())
    val soilMoistureData = _soilMoistureData.asStateFlow()

    private val _humidityData = MutableStateFlow<List<Float>>(emptyList())
    val humidityData = _humidityData.asStateFlow()

    private val _currentTemp = MutableStateFlow("--°C")
    val currentTemp = _currentTemp.asStateFlow()

    private val _currentSoil = MutableStateFlow("--%")
    val currentSoil = _currentSoil.asStateFlow()

    private val _currentHumidity = MutableStateFlow("--%")
    val currentHumidity = _currentHumidity.asStateFlow()
    
    private val _currentPh = MutableStateFlow("--")
    val currentPh = _currentPh.asStateFlow()

    private val _aiPrediction = MutableStateFlow("Waiting...")
    val aiPrediction = _aiPrediction.asStateFlow()

    private val _aiConfidence = MutableStateFlow("0% Confidence")
    val aiConfidence = _aiConfidence.asStateFlow()

    private val _isDevMode = MutableStateFlow(false)
    val isDevMode = _isDevMode.asStateFlow()

    private val _pollingIntervalMs = MutableStateFlow(1000L) // Default 1 second
    val pollingIntervalMs = _pollingIntervalMs.asStateFlow()

    private val maxPoints = 20

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                if (!_isDevMode.value) {
                    val data = TelemetryFetcher.fetchTelemetry()
                    if (data != null) {
                        val tempVal = data["temperature"]?.toFloatOrNull() ?: 0f
                        val humVal = data["humidity"]?.toFloatOrNull() ?: 0f
                        val moistVal = data["soil_moisture"]?.toFloatOrNull() ?: 0f
                        val phVal = data["ph"] ?: "--"

                        _currentTemp.value = "${data["temperature"]}°C"
                        _currentSoil.value = "${data["soil_moisture"]}%"
                        _currentHumidity.value = "${data["humidity"]}%"
                        _currentPh.value = phVal

                        updateDataList(_temperatureData, tempVal)
                        updateDataList(_soilMoistureData, moistVal)
                        updateDataList(_humidityData, humVal)

                        // Real AI Confidence
                        val result = aiPredictor.predictCrop(tempVal, humVal, moistVal)
                        _aiPrediction.value = result.cropName
                        _aiConfidence.value = "${(result.confidence * 100).toInt()}% Confidence"
                    }
                }
                delay(_pollingIntervalMs.value)
            }
        }
    }

    fun setPollingInterval(seconds: Int) {
        _pollingIntervalMs.value = seconds * 1000L
    }

    fun setDevMode(enabled: Boolean) {
        _isDevMode.value = enabled
    }

    fun runManualPrediction(
        temp: Float,
        hum: Float,
        moist: Float,
        n: Float,
        p: Float,
        k: Float
    ) {
        _currentTemp.value = "$temp°C"
        _currentHumidity.value = "$hum%"
        _currentSoil.value = "$moist%"
        
        updateDataList(_temperatureData, temp)
        updateDataList(_humidityData, hum)
        updateDataList(_soilMoistureData, moist)

        val result = aiPredictor.predictCrop(
            temperature = temp,
            humidity = hum,
            moisture = moist,
            n = n,
            p = p,
            k = k
        )
        _aiPrediction.value = result.cropName
        _aiConfidence.value = "${(result.confidence * 100).toInt()}% Confidence"
    }

    private fun updateDataList(flow: MutableStateFlow<List<Float>>, newValue: Float) {
        val currentList = flow.value.toMutableList()
        currentList.add(newValue)
        if (currentList.size > maxPoints) {
            currentList.removeAt(0)
        }
        flow.value = currentList
    }

    override fun onCleared() {
        super.onCleared()
        aiPredictor.close()
    }
}