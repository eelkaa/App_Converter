package com.example.directcurrencyconverter.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.directcurrencyconverter.R
import com.example.directcurrencyconverter.ui.fragments.ChartFragment
import com.example.directcurrencyconverter.ui.fragments.ConverterFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jakewharton.threetenabp.AndroidThreeTen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this) // Инициализация ThreeTenABP
        setContentView(R.layout.activity_main)

        setupNavigation(savedInstanceState)
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_converter -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, ConverterFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                R.id.navigation_charts -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, ChartFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }

                else -> false
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, ConverterFragment())
                .commit()
        }
    }
}
