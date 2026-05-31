package com.example.dinesplit.core.application

import android.app.Application
import android.util.Log
import coil.Coil
import com.example.dinesplit.core.di.CoilConfiguration

class DineSplitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeCoil()
    }

    private fun initializeCoil() {
        try {
            val imageLoader = CoilConfiguration.createImageLoader(this)
            Coil.setImageLoader(imageLoader)
            Log.d("DineSplitApp", "Coil initialized with optimized config")
        } catch (e: Exception) {
            Log.e("DineSplitApp", "Failed to initialize Coil", e)
        }
    }
}
