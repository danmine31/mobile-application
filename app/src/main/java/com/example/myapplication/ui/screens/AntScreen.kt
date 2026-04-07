package com.example.pathfinding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pathfinding.algorithms.AntColony
import com.example.pathfinding.models.PointOfInterest
import kotlinx.coroutines.launch

@Composable
fun AntScreen() {
    val allPoints = DataProvider.pointsOfInterest
    var selectedPoints by remember { mutableStateOf(setOf<PointOfInterest>()) }
    var result by remember { mutableStateOf<AntColony.AntResult?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val startPoint = PointOfInterest("start", "Моё местоположение", 0.0, 0.0)

    fun runAnt() {
        if (selectedPoints.size < 2) return
        val pointsToVisit = selectedPoints.toList()
        val antColony = AntColony(
            points = pointsToVisit,
            startPoint = startPoint,
            antCount = 50,
            iterations = 100
        )
        isRunning = true
        scope.launch {
            val res = antColony.run()
            result = res
            isRunning = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Выберите достопримечательности", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(allPoints) { point ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedPoints.contains(point),
                        onCheckedChange = { isChecked ->
                            selectedPoints = if (isChecked) selectedPoints + point
                            else selectedPoints - point
                        }
                    )
                    Text(point.name, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Button(
            onClick = { runAnt() },
            enabled = !isRunning,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(if (isRunning) "Поиск маршрута..." else "Построить маршрут")
        }

        if (result != null && !isRunning) {
            Text("Длина маршрута: ${"%.2f".format(result!!.distance)}", modifier = Modifier.padding(8.dp))

            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                val cellWidth = size.width / 10f
                val cellHeight = size.height / 10f

                for (x in 0..10) {
                    for (y in 0..10) {
                        drawRect(
                            color = Color.LightGray,
                            topLeft = Offset(x * cellWidth, y * cellHeight),
                            size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f)
                        )
                    }
                }

                val pointsList = selectedPoints.toList()
                for (idx in result!!.route) {
                    val p = pointsList[idx]
                    drawCircle(
                        color = Color.Blue,
                        radius = cellWidth * 0.15f,
                        center = Offset((p.x * cellWidth).toFloat(), (p.y * cellHeight).toFloat())
                    )
                }

                for (i in 0 until result!!.route.size - 1) {
                    val from = pointsList[result!!.route[i]]
                    val to = pointsList[result!!.route[i + 1]]
                    drawLine(
                        color = Color.Red,
                        start = Offset((from.x * cellWidth).toFloat(), (from.y * cellHeight).toFloat()),
                        end = Offset((to.x * cellWidth).toFloat(), (to.y * cellHeight).toFloat()),
                        strokeWidth = 4f
                    )
                }
                val last = pointsList[result!!.route.last()]
                val first = pointsList[result!!.route.first()]
                drawLine(
                    color = Color.Red,
                    start = Offset((last.x * cellWidth).toFloat(), (last.y * cellHeight).toFloat()),
                    end = Offset((first.x * cellWidth).toFloat(), (first.y * cellHeight).toFloat()),
                    strokeWidth = 4f
                )

                drawCircle(
                    color = Color.Green,
                    radius = cellWidth * 0.2f,
                    center = Offset((startPoint.x * cellWidth).toFloat(), (startPoint.y * cellHeight).toFloat())
                )
            }
        }
    }
}