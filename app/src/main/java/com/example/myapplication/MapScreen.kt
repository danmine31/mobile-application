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
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.sqrt

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

fun loadMapData(context: Context): Pair<List<List<GeoPoint>>, List<List<GeoPoint>>> {
    val polygons = mutableListOf<List<GeoPoint>>()
    val lines = mutableListOf<List<GeoPoint>>()
    try {
        val inputStream = context.resources.openRawResource(R.raw.map_data)
        val jsonString = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
        val features = org.json.JSONObject(jsonString).getJSONArray("features")

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val geometry = feature.getJSONObject("geometry")
            val type = geometry.getString("type")
            val props = feature.optJSONObject("properties")

            val isHighway = props?.has("highway") == true

            if (type == "Polygon") {
                val coordinates = geometry.getJSONArray("coordinates").getJSONArray(0)
                polygons.add(parseGeoJsonCoordinates(coordinates))
            } else if (type == "LineString" && isHighway) {
                val coordinates = geometry.getJSONArray("coordinates")
                lines.add(parseGeoJsonCoordinates(coordinates))
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return Pair(polygons, lines)
}

private fun parseGeoJsonCoordinates(array: org.json.JSONArray): List<GeoPoint> {
    val list = mutableListOf<GeoPoint>()
    for (i in 0 until array.length()) {
        val pt = array.getJSONArray(i)
        list.add(GeoPoint(pt.getDouble(1), pt.getDouble(0)))
    }
    return list
}
@Composable
fun MapScreen(context: Context) {
    var startPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var endPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var routeToDraw by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var clusteredPoints by remember { mutableStateOf<List<List<GeoPoint>>>(emptyList()) }
    var gridData by remember { mutableStateOf<Array<IntArray>?>(null) }

    var isPathRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val mapData = loadMapData(context)
        gridData = generateRealGrid(mapData.first, mapData.second)
    }

    LaunchedEffect(isPathRequested) {
        if (isPathRequested && startPoint != null && endPoint != null && gridData != null) {
            val startCell = geoPointToGridCell(startPoint!!)
            val endCell = geoPointToGridCell(endPoint!!)

            val aStar = GridAStar(gridData!!)

            val result = aStar.findPath(
                start = startCell,
                goal = endCell,
                getNeighbors = { cell ->
                    val neighbors = mutableListOf<Pair<Int, Int>>()
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val ni = cell.first + dx
                            val nj = cell.second + dy
                            if (ni in 0 until GRID_SIZE && nj in 0 until GRID_SIZE && gridData!![ni][nj] != 0) {
                                neighbors.add(Pair(ni, nj))
                            }
                        }
                    }
                    neighbors
                },
                heuristic = { a, b ->
                    sqrt(Math.pow((a.first - b.first).toDouble(), 2.0) + Math.pow((a.second - b.second).toDouble(), 2.0))
                }
            )

            if (result != null) {
                routeToDraw = result.path.map { gridCellToGeoPoint(it) }
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

                if (showClusters && clusteredPoints.isNotEmpty()) {
                    val clusterColors = listOf(
                        android.graphics.Color.RED,
                        android.graphics.Color.GREEN,
                        android.graphics.Color.BLUE,
                        android.graphics.Color.MAGENTA,
                        android.graphics.Color.CYAN
                    )

                    clusteredPoints.forEachIndexed { clusterIdx, pointsInCluster ->
                        val color = clusterColors[clusterIdx % clusterColors.size]

                        pointsInCluster.forEach { point ->
                            val circle = Polygon(mapView).apply {
                                points = Polygon.pointsAsCircle(point, 15.0)
                                fillPaint.color = color
                                fillPaint.alpha = 150
                                outlinePaint.color = color
                                outlinePaint.strokeWidth = 2f
                                title = "Заведение (Кластер ${clusterIdx + 1})"
                            }
                            mapView.overlays.add(circle)
                        }
                    }
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
                    val foodLocations = realFoodLocations
                    val pointsForAlgo = foodLocations.map { Point(it.longitude, it.latitude) }

                    val k = 3
                    val result = kMeans(pointsForAlgo, k)

                    val clusters = List(k) { mutableListOf<GeoPoint>() }
                    result.labels.forEachIndexed { index, labelIdx ->
                        clusters[labelIdx].add(foodLocations[index])
                    }

                    clusteredPoints = clusters
                    showClusters = true
                }
            ) {
                Text("Зоны еды")
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

private fun generateRealGrid(polygons: List<List<GeoPoint>>, lines: List<List<GeoPoint>>): Array<IntArray> {
    val grid = Array(GRID_SIZE) { IntArray(GRID_SIZE) { 5 } }

    for (i in 0 until GRID_SIZE) {
        for (j in 0 until GRID_SIZE) {
            val cellPoint = gridCellToGeoPoint(Pair(i, j))
            if (polygons.any { isPointInPolygon(cellPoint, it) }) {
                grid[i][j] = 0
            }
        }
    }

    for (line in lines) {
        for (point in line) {
            val cell = geoPointToGridCell(point)
            grid[cell.first][cell.second] = 1
        }
    }
    return grid
}

val realFoodLocations = listOf(
    GeoPoint(56.469340, 84.946744),
    GeoPoint(56.469604, 84.946239),
    GeoPoint(56.469124, 84.951161),
    GeoPoint(56.472440, 84.948511),
    GeoPoint(56.471410, 84.941150),
    GeoPoint(56.467154, 84.940174),
    GeoPoint(56.472607, 84.950853)
)