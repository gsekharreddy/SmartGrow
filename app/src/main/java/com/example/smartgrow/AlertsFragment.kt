package com.example.smartgrow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AlertsFragment : Fragment(R.layout.activity_alerts) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This works because of ViewUtils.kt!
        view.makeEverythingInteractive(requireContext())

        // Fetch live API data and update Alerts
        viewLifecycleOwner.lifecycleScope.launch {
            val data = TelemetryFetcher.fetchTelemetry()
            if (data != null) {
                // Default to 100.0 if it can't parse the moisture
                val moisture = data["soil_moisture"]?.toDoubleOrNull() ?: 100.0

                // Dynamic UI updates based on real sensor data
                if (moisture < 20.0) {
                    view.replaceText("Critical: Low soil moisture", "CRITICAL: Moisture at ${data["soil_moisture"]}%!")
                    view.replaceText("Section A is below 15% threshold.", "Section A moisture dropped to ${data["soil_moisture"]}%. Pump required.")
                } else {
                    view.replaceText("Critical: Low soil moisture", "Resolved: Soil Moisture Optimal")
                    view.replaceText("Section A is below 15% threshold.", "Current moisture is healthy at ${data["soil_moisture"]}%.")
                }
            }
        }
    }
}