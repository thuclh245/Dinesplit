package com.example.dinesplit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import coil.Coil
import com.example.dinesplit.core.di.CoilConfiguration
import com.example.dinesplit.core.firebase.FirebaseBaselineCheck
import com.example.dinesplit.core.navigation.AppNavHost
import com.example.dinesplit.ui.theme.DineSplitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize Coil with memory-optimized config
        Coil.setImageLoader(CoilConfiguration.createImageLoader(this))

        try {
            runCatching {
                FirebaseBaselineCheck.verify()
            }.onFailure { throwable ->
                android.util.Log.e("MainActivity", "Firebase baseline check failed", throwable)
            }

            setContent {
                DineSplitTheme {
                    val navController = rememberNavController()
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        AppNavHost(navController = navController)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "FATAL CRASH IN ONCREATE", e)
        }
    }
}
