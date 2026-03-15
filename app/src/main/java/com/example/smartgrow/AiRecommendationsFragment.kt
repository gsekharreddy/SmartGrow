package com.example.smartgrow

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

class AiRecommendationsFragment : Fragment(R.layout.activity_ai_recommendations) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Everything becomes clickable!
        view.makeEverythingInteractive(requireContext())
    }
}