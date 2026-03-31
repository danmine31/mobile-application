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

const val MIN_LAT = 56.4587
const val MAX_LAT = 56.4658
const val MIN_LON = 84.9401
const val MAX_LON = 84.9532
const val GRID_SIZE = 200

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
@Composable
fun MapScreen(context: Context) {
    var startPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var endPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var routeToDraw by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var clusteredPoints by remember { mutableStateOf<List<List<GeoPoint>>>(emptyList()) }
    var gridData by remember { mutableStateOf<Array<IntArray>?>(null) }

    LaunchedEffect(Unit) {
        gridData = generateDummyGrid()
    }

    LaunchedEffect(startPoint, endPoint, gridData) {
        if (startPoint != null && endPoint != null && gridData != null) {
            val startCell = geoPointToGridCell(startPoint!!)
            val endCell = geoPointToGridCell(endPoint!!)
            val pathCells = runCatching {
                generateDummyPath(startCell, endCell)
            }.getOrDefault(emptyList())
            routeToDraw = pathCells.map { gridCellToGeoPoint(it) }
        } else {
            routeToDraw = emptyList()
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
                mapView.overlays.clear()

                val eventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        p?.let {
                            if (startPoint == null) {
                                startPoint = it
                            } else if (endPoint == null) {
                                endPoint = it
                            } else {
                                startPoint = it
                                endPoint = null
                            }
                        }
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                }
                mapView.overlays.add(MapEventsOverlay(eventsReceiver))

                val linePaint = Color.LTGRAY
                val gridStrokeWidth = 1f

                val lonStep = (MAX_LON - MIN_LON) / GRID_SIZE
                val latStep = (MAX_LAT - MIN_LAT) / GRID_SIZE

                for (i in 0..GRID_SIZE) {
                    val lat = MIN_LAT + i * latStep
                    val line = Polyline(mapView)
                    line.setPoints(listOf(GeoPoint(lat, MIN_LON), GeoPoint(lat, MAX_LON)))
                    line.color = linePaint
                    line.width = gridStrokeWidth
                    mapView.overlays.add(line)
                }

                for (j in 0..GRID_SIZE) {
                    val lon = MIN_LON + j * lonStep
                    val line = Polyline(mapView)
                    line.setPoints(listOf(GeoPoint(MIN_LAT, lon), GeoPoint(MAX_LAT, lon)))
                    line.color = linePaint
                    line.width = gridStrokeWidth
                    mapView.overlays.add(line)
                }

                gridData?.let { grid ->
                    for (i in 0 until GRID_SIZE) {
                        for (j in 0 until GRID_SIZE) {
                            val box = Polygon(mapView)
                            box.points = getGridCellCorners(i, j, latStep, lonStep)

                            if (grid[i][j] == 0) {
                                box.fillColor = Color.argb(60, 0, 0, 0)
                            } else {
                                box.fillColor = Color.TRANSPARENT
                            }

                            box.strokeColor = Color.BLACK
                            box.strokeWidth = 0.5f

                            mapView.overlays.add(box)
                        }
                    }
                }

                startPoint?.let {
                    val marker = Marker(mapView)
                    marker.position = it
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = "Старт"
                    marker.icon = context.resources.getDrawable(R.drawable.marker_default, null)
                    mapView.overlays.add(marker)
                }
                endPoint?.let {
                    val marker = Marker(mapView)
                    marker.position = it
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = "Финиш"
                    marker.icon = context.resources.getDrawable(R.drawable.marker_default, null)
                    mapView.overlays.add(marker)
                }

                if (showClusters && clusteredPoints.isNotEmpty()) {
                    clusteredPoints.forEachIndexed { clusterIndex, cluster ->
                        val color = when (clusterIndex % 3) {
                            0 -> 0xFFFF0000.toInt()
                            1 -> 0xFF00FF00.toInt()
                            else -> 0xFFFFFF00.toInt()
                        }
                        cluster.forEach { geoPoint ->
                            val clusterMarker = Marker(mapView)
                            clusterMarker.position = geoPoint
                            clusterMarker.icon = context.resources.getDrawable(
                                R.drawable.marker_default, null
                            )
                            clusterMarker.title = "Кластер ${clusterIndex + 1}"
                            clusterMarker.alpha = 0.7f
                            mapView.overlays.add(clusterMarker)
                        }
                    }
                }

                if (routeToDraw.isNotEmpty()) {
                    val polyline = Polyline(mapView)
                    polyline.setPoints(routeToDraw)
                    polyline.setColor(0xFF0000FF.toInt())
                    polyline.width = 7f
                    mapView.overlays.add(0, polyline)
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
                onClick = {
                    showClusters = false
                    clusteredPoints = emptyList()
                },
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                enabled = startPoint != null && endPoint != null
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

private fun generateDummyGrid(): Array<IntArray> {
    val grid = Array(GRID_SIZE) { IntArray(GRID_SIZE) { 1 } }

    for (x in 50..70) {
        for (y in 30..60) {
            grid[x][y] = 0
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
