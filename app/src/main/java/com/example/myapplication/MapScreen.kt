package com.example.myapplication

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.compose.runtime.*
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Polygon
import android.graphics.Color
import org.osmdroid.library.R
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

const val MIN_LAT = 56.4637
const val MAX_LAT = 56.4733
const val MIN_LON = 84.9329
const val MAX_LON = 84.9532
const val GRID_SIZE = 250

fun geoPointToGridCell(point: GeoPoint): Pair<Int, Int> {
    val latRange = MAX_LAT - MIN_LAT
    val lonRange = MAX_LON - MIN_LON

    val i = ((point.latitude - MIN_LAT) / latRange * GRID_SIZE).toInt()
    val j = ((point.longitude - MIN_LON) / lonRange * GRID_SIZE).toInt()

    return Pair(i.coerceIn(0, GRID_SIZE - 1), j.coerceIn(0, GRID_SIZE - 1))
}

fun gridCellToGeoPoint(cell: Pair<Int, Int>): GeoPoint {
    val latStep = (MAX_LAT - MIN_LAT) / GRID_SIZE
    val lonStep = (MAX_LON - MIN_LON) / GRID_SIZE

    val lat = MIN_LAT + cell.first * latStep + latStep / 2.0
    val lon = MIN_LON + cell.second * lonStep + lonStep / 2.0

    return GeoPoint(lat, lon)
}

fun loadObstaclesFromGeoJson(context: Context): List<List<GeoPoint>> {
    val obstacles = mutableListOf<List<GeoPoint>>()

    try {
        val inputStream = context.resources.openRawResource(com.example.myapplication.R.raw.map_data)
        val jsonString = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }

        val jsonObject = JSONObject(jsonString)
        val features = jsonObject.getJSONArray("features")

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val geometry = feature.getJSONObject("geometry")
            val type = geometry.getString("type")

            if (type == "Polygon") {
                val coordinates = geometry.getJSONArray("coordinates").getJSONArray(0)
                val polygon = mutableListOf<GeoPoint>()

                for (j in 0 until coordinates.length()) {
                    val point = coordinates.getJSONArray(j)
                    val lon = point.getDouble(0)
                    val lat = point.getDouble(1)
                    polygon.add(GeoPoint(lat, lon))
                }
                obstacles.add(polygon)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return obstacles
}
@Composable
fun MapScreen(context: Context) {
    var startPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var endPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var routeToDraw by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var clusteredPoints by remember { mutableStateOf<List<List<GeoPoint>>>(emptyList()) }
    var gridData by remember { mutableStateOf<Array<IntArray>?>(null) }

    var isPathRequested by remember { mutableStateOf(false) }
    var obstacles by remember { mutableStateOf<List<List<GeoPoint>>>(emptyList()) }

    LaunchedEffect(Unit) {
        obstacles = loadObstaclesFromGeoJson(context)
        gridData = generateRealGrid(obstacles)
    }

    LaunchedEffect(isPathRequested) {
        if (isPathRequested && startPoint != null && endPoint != null && gridData != null) {
            val startCell = geoPointToGridCell(startPoint!!)
            val endCell = geoPointToGridCell(endPoint!!)

            val heuristic = { a: Pair<Int, Int>, b: Pair<Int, Int> ->
                val dx = (a.first - b.first).toDouble()
                val dy = (a.second - b.second).toDouble()
                Math.sqrt(dx * dx + dy * dy)
            }

            val getNeighbors = { cell: Pair<Int, Int> ->
                val neighbors = mutableListOf<Pair<Int, Int>>()
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val ni = cell.first + dx
                        val nj = cell.second + dy
                        if (ni in 0 until GRID_SIZE && nj in 0 until GRID_SIZE && gridData!![ni][nj] == 1) {
                            neighbors.add(Pair(ni, nj))
                        }
                    }
                }
                neighbors
            }

            val aStar = AStar<Pair<Int, Int>>()
            val result = aStar.findPath(
                start = startCell,
                goal = endCell,
                getNeighbors = getNeighbors,
                heuristic = heuristic
            )

            if (result != null) {
                routeToDraw = result.path.map { gridCellToGeoPoint(it) }
            } else {
                routeToDraw = emptyList()
            }
            isPathRequested = false
        }
    }

    var showClusters by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { currentContext ->
                MapView(currentContext).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)

                    controller.setZoom(17.0)
                    controller.setCenter(GeoPoint(56.4635, 84.9480))
                }
            }, update = { mapView ->
                mapView.overlays.clear()
                val eventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        p?.let {
                            if (startPoint == null) startPoint = it
                            else if (endPoint == null) endPoint = it
                            else { startPoint = it; endPoint = null }
                        }
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                }
                mapView.overlays.add(MapEventsOverlay(eventsReceiver))
                startPoint?.let {
                    val marker = Marker(mapView)
                    marker.position = it
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = "Старт"
                    mapView.overlays.add(marker)
                }

                endPoint?.let {
                    val marker = Marker(mapView)
                    marker.position = it
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = "Финиш"
                    mapView.overlays.add(marker)
                }

                if (routeToDraw.isNotEmpty()) {
                    val polyline = Polyline(mapView)
                    polyline.setPoints(routeToDraw)
                    polyline.color = android.graphics.Color.BLUE
                    polyline.width = 10f
                    mapView.overlays.add(polyline)
                }

                mapView.invalidate()
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = { isPathRequested = true },
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                enabled = startPoint != null && endPoint != null && !isPathRequested
            ) {
                Text("Построить маршрут")
            }

            Button(
                onClick = {
                    if (gridData != null) {
                        val foodLocations = generateDummyFoodLocations(gridData!!)
                        val clusters = runCatching {
                            generateDummyClusters(foodLocations, numClusters = 3)
                        }.getOrDefault(emptyList())

                        clusteredPoints = clusters
                        showClusters = true
                        routeToDraw = emptyList()
                    }
                },
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Text("Кластеризация")
            }
        }
    }
}

fun isPointInPolygon(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
    var intersectCount = 0
    for (j in polygon.indices) {
        val i = if (j == 0) polygon.size - 1 else j - 1
        val vi = polygon[i]
        val vj = polygon[j]
        if ((vj.latitude > point.latitude) != (vi.latitude > point.latitude) &&
            (point.longitude < (vi.longitude - vj.longitude) * (point.latitude - vj.latitude) / (vi.latitude - vj.latitude) + vj.longitude)
        ) {
            intersectCount++
        }
    }
    return intersectCount % 2 != 0
}

private fun getGridCellCorners(i: Int, j: Int, latStep: Double, lonStep: Double): List<GeoPoint> {
    val northWestLat = MIN_LAT + i * latStep
    val northWestLon = MIN_LON + j * lonStep

    val southWestLat = MIN_LAT + (i + 1) * latStep
    val southWestLon = MIN_LON + j * lonStep

    val southEastLat = MIN_LAT + (i + 1) * latStep
    val southEastLon = MIN_LON + (j + 1) * lonStep

    val northEastLat = MIN_LAT + i * latStep
    val northEastLon = MIN_LON + (j + 1) * lonStep

    return listOf(
        GeoPoint(northWestLat, northWestLon),
        GeoPoint(southWestLat, southWestLon),
        GeoPoint(southEastLat, southEastLon),
        GeoPoint(northEastLat, northEastLon),
        GeoPoint(northWestLat, northWestLon)
    )
}
private fun generateDummyPath(start: Pair<Int, Int>, end: Pair<Int, Int>): List<Pair<Int, Int>> {
    val path = mutableListOf<Pair<Int, Int>>()
    var current = start
    path.add(current)
    while (current != end) {
        val nextI = if (current.first < end.first) current.first + 1 else if (current.first > end.first) current.first - 1 else current.first
        val nextJ = if (current.second < end.second) current.second + 1 else if (current.second > end.second) current.second - 1 else current.second
        current = Pair(nextI, nextJ)
        path.add(current)
        if (path.size > GRID_SIZE * GRID_SIZE) break
    }
    return path
}

private fun generateRealGrid(obstaclesList: List<List<GeoPoint>>): Array<IntArray> {
    val grid = Array(GRID_SIZE) { IntArray(GRID_SIZE) { 1 } }
    for (i in 0 until GRID_SIZE) {
        for (j in 0 until GRID_SIZE) {
            val cellPoint = gridCellToGeoPoint(Pair(i, j))

            if (obstaclesList.any { isPointInPolygon(cellPoint, it) }) {
                grid[i][j] = 0
            }
        }
    }
    return grid
}


private fun generateDummyFoodLocations(grid: Array<IntArray>): List<GeoPoint> {
    val locations = mutableListOf<GeoPoint>()
    for (i in 0 until 10) {
        val latOffset = (0..100).random() / 10000.0
        val lonOffset = (0..100).random() / 10000.0
        locations.add(GeoPoint(MIN_LAT + latOffset, MIN_LON + lonOffset))
    }
    return locations
}

private fun generateDummyClusters(foodLocations: List<GeoPoint>, numClusters: Int): List<List<GeoPoint>> {
    val clusters = List(numClusters) { mutableListOf<GeoPoint>() }
    foodLocations.forEachIndexed { index, geoPoint ->
        clusters[index % numClusters].add(geoPoint)
    }
    return clusters
}