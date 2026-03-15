package com.example.smartgrow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Load Dashboard by default on launch
        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment())
            bottomNav?.selectedItemId = R.id.nav_dashboard
        }

        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> replaceFragment(DashboardFragment())
                R.id.nav_analytics -> replaceFragment(AnalyticsFragment())
                R.id.nav_insights -> replaceFragment(AiRecommendationsFragment())
                R.id.nav_alerts -> replaceFragment(AlertsFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}