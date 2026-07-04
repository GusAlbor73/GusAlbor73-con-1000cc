package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.MotoConnectRepository
import com.example.ui.MotoConnectViewModel
import com.example.ui.MotoConnectViewModelFactory
import com.example.ui.screens.MotoConnectApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Database & Repository
    val database = AppDatabase.getDatabase(applicationContext)
    val repository = MotoConnectRepository(database)

    // ViewModel initialization with custom factory
    val viewModel: MotoConnectViewModel by viewModels {
      MotoConnectViewModelFactory(application, repository)
    }

    setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        MotoConnectApp(viewModel)
      }
    }
  }
}
