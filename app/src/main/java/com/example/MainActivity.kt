package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.firebase.configureAppCheck
import com.example.ui.SmartSoilApp
import com.example.ui.theme.SmartSoilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge and render Smart Soil Compose UI
        enableEdgeToEdge()
        setContent {
            SmartSoilTheme {
                SmartSoilApp()
            }
        }
    }
}
