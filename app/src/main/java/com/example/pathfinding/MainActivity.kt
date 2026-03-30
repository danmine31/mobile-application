package com.example.pathfinding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pathfinding.algorithms.AStar
import com.example.pathfinding.algorithms.AStarResult
import com.example.pathfinding.data.GridMap
import com.example.pathfinding.data.GridNode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.pathfinding.ClusteringScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val width = 10
        val height = 10
        val walkable = Array(width) { BooleanArray(height) { true } }
        walkable[5][5] = false

        val gridMap = GridMap(width, height, walkable)

        setContent {
            var currentScreen by remember { mutableStateOf("pathfinding") }
            Column {
                Row {
                    Button(onClick = { currentScreen = "pathfinding" }) {
                        Text("Поиск пути")
                    }
                    Button(onClick = { currentScreen = "clustering" }) {
                        Text("Кластеризация")
                    }
                }
                when (currentScreen) {
                    "pathfinding" -> AStarDemo(gridMap)
                    "clustering" -> ClusteringScreen()
                }
            }
        }
    }
}

@Composable
fun AStarDemo(gridMap: GridMap) {
    val aStar = remember { AStar<GridNode>() }
    var result by remember { mutableStateOf<AStarResult<GridNode>?>(null) }
    var currentStepIndex by remember { mutableStateOf(0) }
    var isAnimating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val start = GridNode(0, 0)
    val goal = GridNode(9, 9)

    fun runNormal() {
        val res = aStar.findPath(
            start = start,
            goal = goal,
            getNeighbors = { gridMap.getNeighbors(it) },
            heuristic = { a, b -> gridMap.heuristic(a, b) },
            collectSteps = false
        )
        result = res
        isAnimating = false
        currentStepIndex = 0
    }

    fun runAnimated() {
        val res = aStar.findPath(
            start = start,
            goal = goal,
            getNeighbors = { gridMap.getNeighbors(it) },
            heuristic = { a, b -> gridMap.heuristic(a, b) },
            collectSteps = true
        )
        result = res
        if (res != null && res.steps != null) {
            isAnimating = true
            currentStepIndex = 0
            scope.launch {
                while (currentStepIndex < res.steps.size) {
                    delay(50)
                    currentStepIndex++
                }
                isAnimating = false
            }
        } else {
            isAnimating = false
        }
    }

    Column {
        Row {
            Button(onClick = { runNormal() }) {
                Text("Найти путь")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { runAnimated() }) {
                Text("Анимация")
            }
        }

        Canvas(modifier = Modifier.size(400.dp)) {
            val cellWidth = size.width / gridMap.width
            val cellHeight = size.height / gridMap.height

            for (x in 0 until gridMap.width) {
                for (y in 0 until gridMap.height) {
                    val isWalkable = gridMap.walkable[x][y]
                    val color = if (isWalkable) Color.LightGray else Color.DarkGray
                    drawRect(
                        color = color,
                        topLeft = Offset(x * cellWidth, y * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                }
            }

            if (isAnimating && result?.steps != null && currentStepIndex > 0) {
                val step = result!!.steps!![currentStepIndex - 1]
                for (node in step.openSet) {
                    drawRect(
                        color = Color.Yellow.copy(alpha = 0.5f),
                        topLeft = Offset(node.x * cellWidth, node.y * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                }
                for (node in step.closedSet) {
                    drawRect(
                        color = Color.Green.copy(alpha = 0.3f),
                        topLeft = Offset(node.x * cellWidth, node.y * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                }
                step.current?.let { node ->
                    drawRect(
                        color = Color.Red.copy(alpha = 0.6f),
                        topLeft = Offset(node.x * cellWidth, node.y * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                }
            }

            result?.path?.let { path ->
                for (i in 0 until path.size - 1) {
                    val from = path[i]
                    val to = path[i + 1]
                    drawLine(
                        color = Color.Red,
                        start = Offset(from.x * cellWidth + cellWidth / 2, from.y * cellHeight + cellHeight / 2),
                        end = Offset(to.x * cellWidth + cellWidth / 2, to.y * cellHeight + cellHeight / 2),
                        strokeWidth = 4f
                    )
                }
            }

            drawCircle(
                color = Color.Green,
                radius = cellWidth / 3,
                center = Offset(start.x * cellWidth + cellWidth / 2, start.y * cellHeight + cellHeight / 2)
            )
            drawCircle(
                color = Color.Red,
                radius = cellWidth / 3,
                center = Offset(goal.x * cellWidth + cellWidth / 2, goal.y * cellHeight + cellHeight / 2)
            )
        }
    }
}