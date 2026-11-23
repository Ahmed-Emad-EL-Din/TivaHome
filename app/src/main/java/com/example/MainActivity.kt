package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.TivaHomeApp
import com.example.ui.theme.TivaHomeTheme
import com.example.ui.viewmodel.HomeControlViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: HomeControlViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val settings by viewModel.settings.collectAsStateWithLifecycle()
      TivaHomeTheme(themeMode = settings.themeMode) { TivaHomeApp(viewModel = viewModel) }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshBluetoothState()
  }
}
