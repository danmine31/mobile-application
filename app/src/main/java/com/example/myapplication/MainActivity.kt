package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.osmdroid.util.GeoPoint

sealed class Screen {
    object Map : Screen()
    data class Rating(val establishmentPoint: GeoPoint) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Map) }
                    when (val screen = currentScreen) {
                        is Screen.Map -> {
                            MapScreen(
                                context = applicationContext,
                                onNavigateToRating = { geoPoint ->
                                    currentScreen = Screen.Rating(geoPoint)
                                }
                            )
                        }
                        is Screen.Rating -> {
                            RatingScreen(
                                establishmentPoint = screen.establishmentPoint,
                                onBack = {
                                    currentScreen = Screen.Map
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
