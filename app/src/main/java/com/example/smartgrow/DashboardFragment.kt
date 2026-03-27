package com.example.smartgrow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class DashboardFragment : Fragment() {

    private val viewModel: AnalyticsViewModel by activityViewModels {
        AnalyticsViewModelFactory(requireContext().applicationContext)
    }
    
    private var tvTemperature: TextView? = null
    private var tvHumidity: TextView? = null
    private var tvSoilMoisture: TextView? = null
    private var tvPhLevel: TextView? = null
    private var tvRecommendedCrop: TextView? = null
    private var tvConfidenceScore: TextView? = null
    private var tvWateringStatus: TextView? = null
    private var tvQuickInsight: TextView? = null
    private var tvLastUpdated: TextView? = null

    // Dev Mode UI
    private var switchDevMode: SwitchCompat? = null
    private var cardDevMode: View? = null
    private var etDevTemp: EditText? = null
    private var etDevHum: EditText? = null
    private var etDevMoist: EditText? = null
    private var etDevN: EditText? = null
    private var etDevP: EditText? = null
    private var etDevK: EditText? = null
    private var btnDevSubmit: Button? = null
    private var btnDevRandom: Button? = null

    // Sync Interval UI
    private var toggleSyncInterval: MaterialButtonToggleGroup? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_dashboard, container, false)

        tvTemperature = view.findViewById(R.id.tvTemperature)
        tvHumidity = view.findViewById(R.id.tvHumidity)
        tvSoilMoisture = view.findViewById(R.id.tvSoilMoisture)
        tvPhLevel = view.findViewById(R.id.tvPhLevel)
        tvRecommendedCrop = view.findViewById(R.id.tvRecommendedCrop)
        tvConfidenceScore = view.findViewById(R.id.tvConfidenceScore)
        tvWateringStatus = view.findViewById(R.id.tvWateringStatus)
        tvQuickInsight = view.findViewById(R.id.tvQuickInsight)
        tvLastUpdated = view.findViewById(R.id.tvLastUpdated)

        // Dev Mode IDs
        switchDevMode = view.findViewById(R.id.switchDevMode)
        cardDevMode = view.findViewById(R.id.cardDevMode)
        etDevTemp = view.findViewById(R.id.etDevTemp)
        etDevHum = view.findViewById(R.id.etDevHum)
        etDevMoist = view.findViewById(R.id.etDevMoist)
        etDevN = view.findViewById(R.id.etDevN)
        etDevP = view.findViewById(R.id.etDevP)
        etDevK = view.findViewById(R.id.etDevK)
        btnDevSubmit = view.findViewById(R.id.btnDevSubmit)
        btnDevRandom = view.findViewById(R.id.btnDevRandom)

        // Sync Interval ID
        toggleSyncInterval = view.findViewById(R.id.toggleSyncInterval)

        setupDevMode()
        setupSyncInterval()

        return view
    }

    private fun setupDevMode() {
        switchDevMode?.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDevMode(isChecked)
            cardDevMode?.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        btnDevRandom?.setOnClickListener {
            randomizeDevInputs()
        }

        btnDevSubmit?.setOnClickListener {
            val t = etDevTemp?.text?.toString()?.toFloatOrNull() ?: 0f
            val h = etDevHum?.text?.toString()?.toFloatOrNull() ?: 0f
            val m = etDevMoist?.text?.toString()?.toFloatOrNull() ?: 0f
            val n = etDevN?.text?.toString()?.toFloatOrNull() ?: 0f
            val p = etDevP?.text?.toString()?.toFloatOrNull() ?: 0f
            val k = etDevK?.text?.toString()?.toFloatOrNull() ?: 0f

            viewModel.runManualPrediction(t, h, m, n, p, k)
        }
    }

    private fun setupSyncInterval() {
        toggleSyncInterval?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val seconds = when (checkedId) {
                    R.id.btnSync1s -> 1
                    R.id.btnSync3s -> 3
                    R.id.btnSync5s -> 5
                    else -> 1
                }
                viewModel.setPollingInterval(seconds)
            }
        }
    }

    private fun randomizeDevInputs() {
        // Ranges: Temp (10-40), Hum (20-95), Moist (10-90), N/P/K (0-140)
        val rTemp = Random.nextInt(10, 41).toString()
        val rHum = Random.nextInt(20, 96).toString()
        val rMoist = Random.nextInt(10, 91).toString()
        val rN = Random.nextInt(0, 141).toString()
        val rP = Random.nextInt(0, 141).toString()
        val rK = Random.nextInt(0, 141).toString()

        etDevTemp?.setText(rTemp)
        etDevHum?.setText(rHum)
        etDevMoist?.setText(rMoist)
        etDevN?.setText(rN)
        etDevP?.setText(rP)
        etDevK?.setText(rK)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentTemp.collectLatest { tvTemperature?.text = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentHumidity.collectLatest { tvHumidity?.text = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentSoil.collectLatest { tvSoilMoisture?.text = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentPh.collectLatest { tvPhLevel?.text = it }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.aiPrediction.collectLatest { 
                tvRecommendedCrop?.text = it
                tvQuickInsight?.text = "🤖 AI Insight: $it"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.aiConfidence.collectLatest { tvConfidenceScore?.text = it }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentSoil.collectLatest { soilStr ->
                val moisture = soilStr.replace("%", "").toFloatOrNull() ?: 0f
                val needsWater = if (moisture < 30) "Yes" else "No"
                tvWateringStatus?.text = "Water Needed: $needsWater"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentTemp.collectLatest {
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                tvLastUpdated?.text = "Last updated: ${sdf.format(Date())}"
            }
        }

        // Restore Switch state if fragment is recreated
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isDevMode.collectLatest { 
                switchDevMode?.isChecked = it
                cardDevMode?.visibility = if (it) View.VISIBLE else View.GONE
            }
        }

        // Sync Toggle state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pollingIntervalMs.collectLatest { ms ->
                val id = when (ms) {
                    1000L -> R.id.btnSync1s
                    3000L -> R.id.btnSync3s
                    5000L -> R.id.btnSync5s
                    else -> R.id.btnSync1s
                }
                toggleSyncInterval?.check(id)
            }
        }
    }
}