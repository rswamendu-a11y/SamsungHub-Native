package com.samsunghub.app.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.samsunghub.app.R
import com.samsunghub.app.ui.fragments.AnalyticsFragment
import com.samsunghub.app.ui.fragments.ReportsFragment
import com.samsunghub.app.ui.fragments.ProfileFragment
import com.samsunghub.app.ui.fragments.TrackerFragment

class SalesTrackerActivity : AppCompatActivity() {

    private val viewModel: SalesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales_tracker)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Default Fragment
        if (savedInstanceState == null) {
            loadFragment(TrackerFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tracker -> loadFragment(TrackerFragment())
                R.id.nav_analytics -> loadFragment(AnalyticsFragment())
                R.id.nav_reports -> loadFragment(ReportsFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
                else -> false
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
