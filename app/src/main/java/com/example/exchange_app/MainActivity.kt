package com.example.exchange_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.exchange_app.data.ExchangeDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exchange_app.ui.ExchangeScreen
import com.example.exchange_app.ui.ExchangeViewModel
import com.example.exchange_app.ui.InitialSetupScreen
import com.example.exchange_app.ui.SettingsScreen
import com.example.exchange_app.ui.theme.ExchangeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val dataStore = ExchangeDataStore(applicationContext)
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExchangeViewModel(dataStore) as T
            }
        }

        setContent {
            ExchangeAppTheme {
                var currentScreen by remember { mutableStateOf("main") }
                val viewModel: ExchangeViewModel = viewModel(factory = viewModelFactory)
                val isInitialSetupComplete by viewModel.isInitialSetupComplete.collectAsStateWithLifecycle()

                if (!isInitialSetupComplete) {
                    InitialSetupScreen(viewModel = viewModel)
                } else if (currentScreen == "main") {
                    ExchangeScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { currentScreen = "settings" }
                    )
                } else {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "main" }
                    )
                }
            }
        }
    }
}
