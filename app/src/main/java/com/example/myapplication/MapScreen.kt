package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.compose.runtime.*
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

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
fun MapScreen() {
    var startPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var endPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var routeToDraw by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    LaunchedEffect(endPoint) {
        if (startPoint != null && endPoint != null) {
            val startCell = geoPointToGridCell(startPoint!!)
            val endCell = geoPointToGridCell(endPoint!!)
        } else {
            routeToDraw = emptyList()
        }
    }
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                controller.setZoom(17.0)
                controller.setCenter(GeoPoint(56.4635, 84.9480))
            }
        }, update = { mapView ->
            mapView.overlays.removeAll { it is Marker || it is MapEventsOverlay }

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
                        }
                    }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }

            if (routeToDraw.isNotEmpty()) {
                val polyline = Polyline(mapView)
                polyline.setPoints(routeToDraw)
                mapView.overlays.add(0, polyline)
            }

            val eventsOverlay = MapEventsOverlay(eventsReceiver)
            mapView.overlays.add(eventsOverlay)

            mapView.invalidate()
        }
    )
}
