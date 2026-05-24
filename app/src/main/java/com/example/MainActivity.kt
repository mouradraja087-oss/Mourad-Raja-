package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MontageViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: MontageViewModel = viewModel()
        val currentScreen by viewModel.currentScreen.collectAsState()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          when (currentScreen) {
            "DASHBOARD" -> {
              DashboardScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
              )
            }
            "EDITOR" -> {
              EditorScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
              )
            }
            else -> {
              DashboardScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
              )
            }
          }
        }
      }
    }
  }
}
