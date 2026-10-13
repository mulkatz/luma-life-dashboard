package com.mulkatz.lumacare

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mulkatz.lumacare.ui.LumaApp
import com.mulkatz.lumacare.ui.theme.LumaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // ShiftHeader ist in beiden Themes Aubergine → immer helle Statusleisten-Icons.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            LumaTheme {
                LumaApp()
            }
        }
    }
}
