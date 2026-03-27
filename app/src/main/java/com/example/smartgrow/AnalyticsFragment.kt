package com.example.smartgrow

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnalyticsFragment : Fragment(R.layout.activity_analytics) {

    // Using activityViewModels() ensures we use the same instance held by MainActivity
    private val viewModel: AnalyticsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.makeEverythingInteractive(requireContext())

        // 1. Find Text Views
        val tvTemp = view.findViewById<TextView>(R.id.tvAnalyticsTemp)
        val tvSoil = view.findViewById<TextView>(R.id.tvAnalyticsSoil)
        val tvHumidity = view.findViewById<TextView>(R.id.tvAnalyticsHumidity)

        // 2. Find and configure Charts
        val chartTemp = view.findViewById<LiveLineChartView>(R.id.chartTemp)
        chartTemp.lineColor = Color.parseColor("#2F7F34")
        chartTemp.fillColor = Color.parseColor("#202F7F34")
        chartTemp.minY = 10f
        chartTemp.maxY = 40f

        val chartSoil = view.findViewById<LiveLineChartView>(R.id.chartSoil)
        chartSoil.lineColor = Color.parseColor("#1E90FF")
        chartSoil.fillColor = Color.parseColor("#201E90FF")
        chartSoil.minY = 0f
        chartSoil.maxY = 100f

        val chartHumidity = view.findViewById<LiveLineChartView>(R.id.chartHumidity)
        chartHumidity.lineColor = Color.parseColor("#FFA500")
        chartHumidity.fillColor = Color.parseColor("#20FFA500")
        chartHumidity.minY = 0f
        chartHumidity.maxY = 100f

        // 3. Observe ViewModel State
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentTemp.collectLatest { tvTemp.text = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentSoil.collectLatest { tvSoil.text = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentHumidity.collectLatest { tvHumidity.text = it }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.temperatureData.collectLatest { chartTemp.setData(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.soilMoistureData.collectLatest { chartSoil.setData(it) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.humidityData.collectLatest { chartHumidity.setData(it) }
        }
    }
}