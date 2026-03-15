package com.example.smartgrow

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AnalyticsFragment : Fragment(R.layout.activity_analytics) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.makeEverythingInteractive(requireContext())

        // 1. Find Text Views
        val tvTemp = view.findViewById<TextView>(R.id.tvAnalyticsTemp)
        val tvSoil = view.findViewById<TextView>(R.id.tvAnalyticsSoil)
        val tvHumidity = view.findViewById<TextView>(R.id.tvAnalyticsHumidity)

        // 2. Find and configure Charts
        val chartTemp = view.findViewById<LiveLineChartView>(R.id.chartTemp)
        chartTemp.lineColor = Color.parseColor("#2F7F34") // Green
        chartTemp.fillColor = Color.parseColor("#202F7F34")
        chartTemp.minY = 10f
        chartTemp.maxY = 40f // Temp usually between 10C and 40C

        val chartSoil = view.findViewById<LiveLineChartView>(R.id.chartSoil)
        chartSoil.lineColor = Color.parseColor("#1E90FF") // User secondary blue
        chartSoil.fillColor = Color.parseColor("#201E90FF")
        chartSoil.minY = 0f
        chartSoil.maxY = 100f // Percentage

        val chartHumidity = view.findViewById<LiveLineChartView>(R.id.chartHumidity)
        chartHumidity.lineColor = Color.parseColor("#FFA500") // User primary orange
        chartHumidity.fillColor = Color.parseColor("#20FFA500")
        chartHumidity.minY = 0f
        chartHumidity.maxY = 100f // Percentage

        // 3. Start Live Polling
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                val data = TelemetryFetcher.fetchTelemetry()

                if (data != null) {
                    // Parse values safely to Float and feed them to the charts!
                    data["temperature"]?.let {
                        tvTemp.text = "$it°C"
                        it.toFloatOrNull()?.let { v -> chartTemp.addDataPoint(v) }
                    }
                    data["soil_moisture"]?.let {
                        tvSoil.text = "$it%"
                        it.toFloatOrNull()?.let { v -> chartSoil.addDataPoint(v) }
                    }
                    data["humidity"]?.let {
                        tvHumidity.text = "$it%"
                        it.toFloatOrNull()?.let { v -> chartHumidity.addDataPoint(v) }
                    }
                }

                // Fetch new data every 3 seconds to keep graphs moving smoothly
                delay(3000)
            }
        }
    }
}