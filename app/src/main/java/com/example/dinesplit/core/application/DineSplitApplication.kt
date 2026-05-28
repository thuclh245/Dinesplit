package com.example.dinesplit.core.application

import android.app.Application
import android.graphics.ImageDecoder
import android.os.Build
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.dinesplit.core.di.CoilConfiguration
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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

