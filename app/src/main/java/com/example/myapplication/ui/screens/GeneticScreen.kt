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
import com.example.pathfinding.algorithms.GeneticAlgorithm
import com.example.pathfinding.models.Cafe
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun GeneticScreen() {
    val cafes = DataProvider.cafes
    val dishes = DataProvider.dishes

    var selectedDishes by remember { mutableStateOf(setOf<String>()) }
    var resultRoute by remember { mutableStateOf<List<Cafe>?>(null) }
    var bestTime by remember { mutableStateOf(0.0) }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runGA() {
        if (selectedDishes.isEmpty()) return
        val neededCafeIds = selectedDishes.map { dishName ->
            dishes.find { it.name == dishName }?.cafeId
        }.filterNotNull().distinct()
        val neededCafes = cafes.filter { it.id in neededCafeIds }
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        isRunning = true
        scope.launch {
            val ga = GeneticAlgorithm(cafes, neededCafes, currentHour = currentHour)
            val result = ga.run()
            resultRoute = result.route
            bestTime = result.fitness
            isRunning = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Выберите блюда для приёма пищи", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(dishes) { dish ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedDishes.contains(dish.name),
                        onCheckedChange = { isChecked ->
                            selectedDishes = if (isChecked) selectedDishes + dish.name
                            else selectedDishes - dish.name
                        }
                    )
                    Text(dish.name, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Button(
            onClick = { runGA() },
            enabled = !isRunning,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(if (isRunning) "Построение маршрута..." else "Построить маршрут")
        }

        if (resultRoute != null) {
            Text("Лучший маршрут (время: ${"%.1f".format(bestTime)} мин)", modifier = Modifier.padding(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
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

                resultRoute!!.forEach { cafe ->
                    drawCircle(
                        color = Color.Blue,
                        radius = cellWidth * 0.15f,
                        center = Offset((cafe.x * cellWidth).toFloat(), (cafe.y * cellHeight).toFloat())
                    )
                }

                for (i in 0 until resultRoute!!.size - 1) {
                    val from = resultRoute!![i]
                    val to = resultRoute!![i + 1]
                    drawLine(
                        color = Color.Red,
                        start = Offset((from.x * cellWidth).toFloat(), (from.y * cellHeight).toFloat()),
                        end = Offset((to.x * cellWidth).toFloat(), (to.y * cellHeight).toFloat()),
                        strokeWidth = 4f
                    )
                }

                drawCircle(
                    color = Color.Green,
                    radius = cellWidth * 0.2f,
                    center = Offset(0f, 0f)
                )
            }
        }
    }
}