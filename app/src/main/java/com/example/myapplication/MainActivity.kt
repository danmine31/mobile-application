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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current

                var gridMap by remember { mutableStateOf<GridMap?>(null) }

                LaunchedEffect(Unit) {
                    gridMap = withContext(Dispatchers.Default) {
                        var loadedGrid = loadGridFromAssets(context)

                        if (loadedGrid == null) {
                            val generator = MapGridGenerator(context)
                            loadedGrid = generator.generateFullGrid()
                            generator.saveGridToJson(loadedGrid, "map_data.json")
                        }

                        loadedGrid
                    }
                }

                if (gridMap == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Text("Генерируем разметку, подождите pls...",
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.Center))
                    }
                } else {
                    MapScreen(gridMap = gridMap!!)
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
