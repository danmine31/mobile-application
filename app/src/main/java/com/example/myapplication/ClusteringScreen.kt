package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.random.Random

data class Point(val x: Double, val y: Double)

class KMeansResult(val labels: List<Int>, val centroids: List<Point>)

fun kMeans(points: List<Point>, k: Int, maxIter: Int = 100, seed: Long = 42): KMeansResult {
    if (k <= 0) return KMeansResult(emptyList(), emptyList())
    if (points.isEmpty()) return KMeansResult(emptyList(), emptyList())
    val rng = Random(seed)

    val centroids = mutableListOf<Point>()
    val shuffled = points.shuffled(rng)
    for (i in 0 until min(k, shuffled.size)) {
        centroids.add(shuffled[i])
    }
    while (centroids.size < k) {
        centroids.add(Point(rng.nextDouble(0.0, 10.0), rng.nextDouble(0.0, 10.0)))
    }

    var labels = MutableList(points.size) { 0 }
    var changed: Boolean
    repeat(maxIter) {
        val newLabels = points.map { point ->
            centroids.withIndex().minByOrNull { (_, c) ->
                val dx = point.x - c.x
                val dy = point.y - c.y
                dx * dx + dy * dy
            }?.index ?: 0
        }
        changed = newLabels != labels
        labels = newLabels.toMutableList()

        for (i in 0 until k) {
            val clusterPoints = points.filterIndexed { idx, _ -> labels[idx] == i }
            if (clusterPoints.isNotEmpty()) {
                val avgX = clusterPoints.map { it.x }.average()
                val avgY = clusterPoints.map { it.y }.average()
                centroids[i] = Point(avgX, avgY)
            }
        }

        if (!changed) return@repeat
    }
    return KMeansResult(labels, centroids)
}

@Composable
fun ClusteringScreen(
    gridWidth: Int = 10,
    gridHeight: Int = 10,
    initialPoints: List<Point> = listOf(
        Point(1.0, 2.0), Point(2.0, 8.0), Point(3.0, 4.0),
        Point(5.0, 6.0), Point(6.0, 3.0), Point(7.0, 7.0),
        Point(8.0, 1.0), Point(9.0, 9.0), Point(4.0, 5.0)
    )
) {
    var points by remember { mutableStateOf(initialPoints.toMutableList()) }
    var k by remember { mutableStateOf(3) }
    var result by remember { mutableStateOf<KMeansResult?>(null) }
    var showVoronoi by remember { mutableStateOf(true) }

    val clusterColors = listOf(
        Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
        Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DB6AC),
        Color(0xFFFF8A65), Color(0xFFA1887F), Color(0xFF90A4AE)
    )

    fun runClustering() {
        if (points.size < k) {
            k = points.size
        }
        result = if (points.isNotEmpty()) kMeans(points, k, seed = 42) else null
    }

    LaunchedEffect(k, points) {
        runClustering()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Кластеров: $k   ")
                Button(onClick = { if (k > 1) k-- }) { Text("-") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { if (k < 9) k++ }) { Text("+") }
            }
            Button(onClick = { showVoronoi = !showVoronoi }) {
                Text(if (showVoronoi) "Скрыть зоны" else "Показать зоны")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            val cellWidth = size.width / gridWidth
            val cellHeight = size.height / gridHeight

            for (x in 0 until gridWidth) {
                for (y in 0 until gridHeight) {
                    val left = x * cellWidth
                    val top = y * cellHeight
                    val cellColor = if (showVoronoi && result != null) {
                        val cellCenter = Point(x + 0.5, y + 0.5)
                        val closestIdx = result!!.centroids.withIndex()
                            .minByOrNull { (_, c) ->
                                val dx = cellCenter.x - c.x
                                val dy = cellCenter.y - c.y
                                dx * dx + dy * dy
                            }?.index ?: 0
                        clusterColors[closestIdx % clusterColors.size].copy(alpha = 0.3f)
                    } else {
                        Color.LightGray
                    }
                    drawRect(
                        color = cellColor,
                        topLeft = Offset(left, top),
                        size = Size(cellWidth, cellHeight)
                    )
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(left, top),
                        size = Size(cellWidth, cellHeight),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.5f)
                    )
                }
            }

            result?.centroids?.forEachIndexed { idx, centroid ->
                val cx = (centroid.x * cellWidth).toFloat()
                val cy = (centroid.y * cellHeight).toFloat()
                drawCircle(
                    color = clusterColors[idx % clusterColors.size],
                    radius = cellWidth * 0.3f,
                    center = Offset(cx, cy)
                )
            }

            points.forEachIndexed { idx, point ->
                val px = (point.x * cellWidth).toFloat()
                val py = (point.y * cellHeight).toFloat()
                val clusterIdx = result?.labels?.getOrNull(idx) ?: 0
                val pointColor = if (result != null)
                    clusterColors[clusterIdx % clusterColors.size]
                else
                    Color.Black
                drawCircle(
                    color = pointColor,
                    radius = cellWidth * 0.2f,
                    center = Offset(px, py)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Точки заведений (${points.size} шт.)",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}