package com.mulkatz.lumacare

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mulkatz.lumacare.ui.components.EmptyState
import com.mulkatz.lumacare.ui.components.ShiftHeader
import com.mulkatz.lumacare.ui.theme.LumaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // ShiftHeader ist in beiden Themes Aubergine → immer helle Statusleisten-Icons.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        setContent {
            LumaTheme {
                Scaffold(
                    topBar = {
                        // TODO: aus ShiftClock/DB
                        ShiftHeader(area = "Ahorn", shift = "Frühdienst", time = "07:15", remaining = "6:45 h")
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState()
                    }
                }
            }
        }
    }
}
