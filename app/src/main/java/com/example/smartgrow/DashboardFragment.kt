package com.example.smartgrow

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardFragment : Fragment(R.layout.activity_dashboard) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.makeEverythingInteractive(requireContext())

        // Specific override for the orange irrigation button
        view.findViewById<MaterialButton>(R.id.btnIrrigation)?.setOnClickListener {
            Toast.makeText(requireContext(), "Irrigation System Activated! Watering Section A...", Toast.LENGTH_LONG).show()
        }

        // 1. Cache the specific text views based on their initial XML placeholder text
        val tvTemp = view.findTextViewByExactText("24°C")
        val tvHumidity = view.findTextViewByExactText("65%")
        val tvSoil = view.findTextViewByExactText("40%")

        // Wire up the manual refresh button
        view.findViewById<ImageView>(R.id.btnRefresh)?.setOnClickListener {
            fetchLiveTelemetry(tvTemp, tvHumidity, tvSoil, isManual = true)
        }

        // 2. Start Live Polling Loop
        viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) { // Keeps running as long as the fragment is visible
                fetchLiveTelemetry(tvTemp, tvHumidity, tvSoil, isManual = false)

                // Wait 5 seconds before fetching again
                delay(5000)
            }
        }
    }

    private fun fetchLiveTelemetry(tvTemp: TextView?, tvHumidity: TextView?, tvSoil: TextView?, isManual: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            val data = TelemetryFetcher.fetchTelemetry()

            if (data != null) {
                // Update the cached views with live data
                data["temperature"]?.let { tvTemp?.text = "$it°C" }
                data["humidity"]?.let { tvHumidity?.text = "$it%" }
                data["soil_moisture"]?.let { tvSoil?.text = "$it%" }

                if (isManual) {
                    Toast.makeText(requireContext(), "Live data refreshed!", Toast.LENGTH_SHORT).show()
                }
            } else if (isManual) {
                Toast.makeText(requireContext(), "Failed to sync telemetry", Toast.LENGTH_SHORT).show()
            }
        }
    }
}