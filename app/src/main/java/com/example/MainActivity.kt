package com.example

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        try {
            com.example.data.AppContainer.initialize(applicationContext)
            com.example.data.AppContainer.requestSyncOnForeground(applicationContext)
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.data.AppContainer.initialize(applicationContext)
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            // Status/navigation bar icons follow the in-app theme (not the system one)
            DisposableEffect(isDarkTheme) {
                val style = SystemBarStyle.auto(
                    lightScrim = AndroidColor.TRANSPARENT,
                    darkScrim = AndroidColor.TRANSPARENT,
                    detectDarkMode = { isDarkTheme }
                )
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}
