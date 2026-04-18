package com.example.myapplication

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.data.MapGridGenerator
import com.example.myapplication.ui.theme.*
import com.example.myapplication.data.GridMap
import com.example.myapplication.ui.screens.MapScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                val context = LocalContext.current
                var gridMap by remember { mutableStateOf<GridMap?>(null) }
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {

                    val loaded = withContext(Dispatchers.Default) {
                        var g = loadGridFromFile(context, "map_data_v2.json")
                        if (g == null) g = loadGridFromAssets(context)
                        if (g == null) {
                            val generator = MapGridGenerator(context)
                            g = generator.generateFullGrid()
                            generator.saveGridToJson(g, "map_data_v2.json")
                        }
                        g
                    }
                    gridMap = loaded
                    delay(2000)
                    showSplash = false
                }

                Crossfade(targetState = showSplash, label = "splash_fade") { isSplash ->
                    if (isSplash) {
                        SplashScreen()
                    } else if (gridMap == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        MapScreen(gridMap = gridMap!!)
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val scale = infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    ).value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(350.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = TSU_LightBlue.copy(alpha = 0.05f)
            ) {}

            val painter = painterResource(id = R.drawable.tsu_logo_basic_sign)
            Image(
                painter = painter,
                contentDescription = "TSU Logo",
                modifier = Modifier.size(400.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TSU.AlgoMap",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = TSU_LightBlue,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Томский Государственный Университет",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = ComposeColor.Gray
            )
        )
    }
}

private fun loadGridFromFile(context: Context, fileName: String): GridMap? {
    return try {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return null

        val jsonString = file.readText()
        parseGridJson(jsonString)
    } catch (e: Exception) {
        null
    }
}

private fun loadGridFromAssets(context: Context): GridMap? {
    return try {
        val inputStream = context.assets.open("map_data.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        parseGridJson(jsonString)
    } catch (e: Exception) {
        null
    }
}

private fun parseGridJson(jsonString: String): GridMap {
    val json = JSONObject(jsonString)
    val w = json.getInt("width")
    val h = json.getInt("height")

    val dataString = json.getString("data_string")

    val walkable = Array(w) { j ->
        BooleanArray(h) { i ->
            dataString[i * w + j] == '1'
        }
    }

    val foodPoints = mutableListOf<com.example.myapplication.data.FoodPoint>()
    if (json.has("food_points")) {
        val foodArray = json.getJSONArray("food_points")
        for (i in 0 until foodArray.length()) {
            val f = foodArray.getJSONObject(i)
            foodPoints.add(
                com.example.myapplication.data.FoodPoint(
                    name = f.getString("name"),
                    lat = f.getDouble("lat"),
                    lon = f.getDouble("lon"),
                    type = f.getString("type")
                )
            )
        }
    }

    return GridMap(w, h, walkable, foodPoints)
}
