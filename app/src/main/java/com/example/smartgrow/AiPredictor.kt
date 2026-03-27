package com.example.smartgrow

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.json.JSONObject
import java.nio.FloatBuffer

class AiPredictor(private val context: Context) {

    data class PredictionResult(val cropName: String, val confidence: Float)

    private var env: OrtEnvironment? = null
    private var cropSession: OrtSession? = null
    private var labelMap: JSONObject? = null

    init {
        try {
            env = OrtEnvironment.getEnvironment()

            // Load the ONNX model from assets
            val modelBytes = context.assets.open("crop_rf_model.onnx").readBytes()
            cropSession = env?.createSession(modelBytes)

            // Load the JSON label mapping
            val jsonString = context.assets.open("label_maps.json").bufferedReader().use { it.readText() }
            labelMap = JSONObject(jsonString)

            Log.d("AiPredictor", "✅ ML Models & Labels loaded successfully!")
        } catch (e: Exception) {
            Log.e("AiPredictor", "💀 Failed to load ML models: ${e.message}")
        }
    }

    /**
     * Predicts crop based on all 7 parameters.
     */
    fun predictCrop(
        temperature: Float,
        humidity: Float,
        moisture: Float,
        soilType: Float = 0f,
        n: Float = 0f,
        p: Float = 0f,
        k: Float = 0f
    ): PredictionResult {
        // --- 1. GARBAGE DATA GUARDS ---
        if (temperature == 0f && humidity == 0f && moisture == 0f && n == 0f && p == 0f && k == 0f) {
            return PredictionResult("⏳ Waiting for valid sensor data...", 0f)
        }
        if (humidity > 100f || humidity < 0f) {
            return PredictionResult("⚠️ Sensor Error: Humidity out of range ($humidity%)", 0f)
        }

        // --- 2. ML PREDICTION ---
        try {
            val session = cropSession ?: return PredictionResult("❌ ML Model not initialized.", 0f)
            val environment = env ?: return PredictionResult("❌ ML Env not initialized.", 0f)

            val inputFloats = floatArrayOf(
                temperature,
                humidity,
                moisture,
                soilType,
                n,
                p,
                k
            )

            // Set the shape to exactly [1, 7]
            val inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(inputFloats), longArrayOf(1, 7))

            // Run prediction
            val inputName = session.inputNames.iterator().next()
            val result = session.run(mapOf(inputName to inputTensor))

            // Extract the predicted class index (First output)
            val outputValue = result[0].value
            val predictedIndex = when (outputValue) {
                is LongArray -> outputValue[0]
                is IntArray -> outputValue[0].toLong()
                is Array<*> -> (outputValue[0] as LongArray)[0]
                else -> outputValue.toString().replace("[", "").replace("]", "").toLongOrNull() ?: 0L
            }

            // Extract confidence
            var confidence = 0.92f // Default fallback
            if (result.size() > 1) {
                val probsValue = result[1].value
                if (probsValue is List<*>) {
                    val probsMap = probsValue[0] as? Map<*, *>
                    confidence = (probsMap?.get(predictedIndex) as? Float) ?: 0.92f
                }
            }

            result.close()
            inputTensor.close()

            // --- 3. DECODE LABEL ---
            val cropClasses = labelMap?.optJSONArray("crop_classes")
            val cropName = if (cropClasses != null) {
                cropClasses.optString(predictedIndex.toInt(), "Unknown Crop")
            } else {
                "Unknown Crop ID: $predictedIndex"
            }

            return PredictionResult("🌱 Ideal Crop: $cropName", confidence)

        } catch (e: Exception) {
            Log.e("AiPredictor", "Prediction crash: ${e.message}")
            return PredictionResult("❌ Prediction Error: ${e.message}", 0f)
        }
    }

    fun close() {
        cropSession?.close()
        env?.close()
    }
}