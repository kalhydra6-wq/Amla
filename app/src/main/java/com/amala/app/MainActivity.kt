package com.amala.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amala.app.ui.AmlaApp
import com.amala.app.ui.theme.AmlaTheme
import com.amala.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmlaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AmlaApp(viewModel = viewModel<MainViewModel>())
                }
            }
        }
    }
}
