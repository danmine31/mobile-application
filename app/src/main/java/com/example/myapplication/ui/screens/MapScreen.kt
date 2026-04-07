package com.example.myapplication.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import com.example.myapplication.algorithms.AStar
import com.example.myapplication.data.AppConstants
import com.example.myapplication.data.GridMap
import com.example.myapplication.data.GridNode


fun geoPointToGridCell(point: GeoPoint, gridWidth: Int, gridHeight: Int): GridNode {
    val latRange = AppConstants.MAX_LAT - AppConstants.MIN_LAT
    val lonRange = AppConstants.MAX_LON - AppConstants.MIN_LON

    val i = ((point.latitude - AppConstants.MIN_LAT) / latRange * gridHeight).toInt()
    val j = ((point.longitude - AppConstants.MIN_LON) / lonRange * gridWidth).toInt()

    return GridNode(i.coerceIn(0, gridHeight - 1), j.coerceIn(0, gridWidth - 1))
}

fun gridCellToGeoPoint(node: GridNode, width: Int, height: Int): GeoPoint {
    val lat = AppConstants.MIN_LAT + (node.y.toDouble() / height) * (AppConstants.MAX_LAT - AppConstants.MIN_LAT)
    val lon = AppConstants.MIN_LON + (node.x.toDouble() / width) * (AppConstants.MAX_LON - AppConstants.MIN_LON)
    return GeoPoint(lat, lon)
}

@Composable
fun MapScreen(gridMap: GridMap) {
    var startPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var endPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var routeToDraw by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    val aStar = remember { AStar<GridNode>() }

    fun calculatePath() {
        if (startPoint != null && endPoint != null) {
            val startNode = geoPointToGridCell(startPoint!!, gridMap.width, gridMap.height)
            val goalNode = geoPointToGridCell(endPoint!!, gridMap.width, gridMap.height)

            val result = aStar.findPath(
                start = startNode,
                goal = goalNode,
                getNeighbors = { gridMap.getNeighbors(it) },
                heuristic = { a, b -> gridMap.heuristic(a, b) }
            )

            if (result != null) {
                routeToDraw = result.path.map { gridCellToGeoPoint(it, gridMap.width, gridMap.height) }
            } else {
                routeToDraw = emptyList()
            }
        }
    }

    fun clearMap() {
        startPoint = null
        endPoint = null
        routeToDraw = emptyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { calculatePath() }, enabled = startPoint != null && endPoint != null) {
                Text("Построить маршрут")
            }
            Button(onClick = { clearMap() }) {
                Text("Очистить")
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(17.0)
                    val centerLat = (AppConstants.MAX_LAT + AppConstants.MIN_LAT) / 2
                    val centerLon = (AppConstants.MAX_LON + AppConstants.MIN_LON) / 2
                    controller.setCenter(GeoPoint(centerLat, centerLon))
                }
            }, update = { mapView ->
                mapView.overlays.removeAll { it is Marker || it is MapEventsOverlay
                        || it is Polyline || it is GridOverlay }

                val gridOverlay = GridOverlay(gridMap)
                mapView.overlays.add(gridOverlay)

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
                                routeToDraw = emptyList()
                            }
                        }
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                }
                mapView.overlays.add(MapEventsOverlay(eventsReceiver))
                mapView.invalidate()

                if (routeToDraw.isNotEmpty()) {
                    val polyline = Polyline(mapView)
                    polyline.setPoints(routeToDraw)
                    polyline.outlinePaint.color = android.graphics.Color.RED
                    polyline.outlinePaint.strokeWidth = 8f
                    mapView.overlays.add(polyline)
                }

                mapView.invalidate()
            }
        )
    }
}
