package com.mechanicar.gastos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mechanicar.gastos.ui.GastosNavHost
import com.mechanicar.gastos.ui.theme.GastosCorsaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GastosCorsaApp
        setContent {
            GastosCorsaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GastosNavHost(repository = app.repository)
                }
            }
        }
    }
}
