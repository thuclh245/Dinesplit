package com.example.dinesplit.core.application

import android.app.Application
import android.util.Log
import coil.Coil
import com.example.dinesplit.core.di.CoilConfiguration
import com.example.dinesplit.core.notification.NotificationHelper
import com.example.dinesplit.core.notification.FcmManager

class DineSplitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FcmManager.initialize(this)
        initializeCoil()
        initializeNotificationChannel()
    }

    private fun initializeNotificationChannel() {
        try {
            NotificationHelper.createNotificationChannel(this)
            Log.d("DineSplitApp", "Notification channel created successfully")
        } catch (e: Exception) {
            Log.e("DineSplitApp", "Failed to create notification channel", e)
        }
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
