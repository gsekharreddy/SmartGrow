package com.example.smartgrow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

class AnalyticsFragment : Fragment(R.layout.activity_analytics) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.makeEverythingInteractive(requireContext())
    }
}