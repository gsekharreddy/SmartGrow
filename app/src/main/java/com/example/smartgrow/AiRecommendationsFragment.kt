package com.example.smartgrow

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AiRecommendationsFragment : Fragment(R.layout.activity_ai_recommendations) {

    private val viewModel: AnalyticsViewModel by activityViewModels {
        AnalyticsViewModelFactory(requireContext().applicationContext)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvTelemetryData = view.findViewById<TextView>(R.id.tvTelemetryData)
        val tvAiResult = view.findViewById<TextView>(R.id.tvAiResult)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentTemp.collectLatest { temp ->
                viewModel.currentHumidity.collectLatest { hum ->
                    viewModel.currentSoil.collectLatest { soil ->
                        tvTelemetryData.text = "Temp: $temp | Hum: $hum | Soil: $soil"
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.aiPrediction.collectLatest { prediction ->
                viewModel.aiConfidence.collectLatest { confidence ->
                    tvAiResult.text = "Recommendation: $prediction ($confidence)"
                }
            }
        }
    }
}