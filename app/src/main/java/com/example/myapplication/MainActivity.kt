package com.example.myapplication

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.data.MapGridGenerator
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.data.GridMap
import com.example.myapplication.ui.screens.MapScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.foundation.layout.Arrangement
import com.example.pathfinding.GeneticScreen
import com.example.pathfinding.AntScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentScreen by remember { mutableStateOf("pathfinding") }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { currentScreen = "pathfinding" }) { Text("Поиск пути") }
                    Button(onClick = { currentScreen = "clustering" }) { Text("Кластеризация") }
                    Button(onClick = { currentScreen = "genetic" }) { Text("Генетический") }
                    Button(onClick = { currentScreen = "ant" }) { Text("Муравьиный") }
                }

                when (currentScreen) {
                    "pathfinding" -> AStarDemo(gridMap)
                    "clustering" -> ClusteringScreen()
                    "genetic" -> GeneticScreen()
                    "ant" -> AntScreen()
                }
            }
        }
    }
    fun loadGridFromAssets(context: Context): GridMap? {
        return try {
            val inputStream = context.assets.open("map_data.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            val w = json.getInt("width")
            val h = json.getInt("height")
            val data = json.getJSONArray("data")

            val walkable = Array(h) { i ->
                BooleanArray(w) { j ->
                    data.getInt(i * w + j) == 1
                }
            }
            GridMap(w, h, walkable)
        } catch (e: Exception) {
            null
        }
    }
}
