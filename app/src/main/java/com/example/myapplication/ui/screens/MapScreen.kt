package com.example.myapplication.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.CustomZoomButtonsController
import com.example.myapplication.algorithms.AStar
import com.example.myapplication.data.AppConstants
import com.example.myapplication.data.GridMap
import com.example.myapplication.data.GridNode
import com.example.myapplication.ui.theme.*
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background


fun smoothPath(path: List<GridNode>, gridMap: GridMap): List<GridNode> {
    if (path.size <= 2) return path
    val smoothed = mutableListOf<GridNode>()
    smoothed.add(path[0])

    var currentIdx = 0
    while (currentIdx < path.size - 1) {
        var furthestVisible = currentIdx + 1

        for (checkIdx in (currentIdx + 2) until path.size) {
            if (hasLineOfSight(path[currentIdx], path[checkIdx], gridMap)) {
                furthestVisible = checkIdx
            } else {
                break
            }
        }
        smoothed.add(path[furthestVisible])
        currentIdx = furthestVisible
    }
    return smoothed
}

fun hasLineOfSight(start: GridNode, end: GridNode, gridMap: GridMap): Boolean {
    var x = start.x
    var y = start.y
    val dx = Math.abs(end.x - start.x)
    val dy = Math.abs(end.y - start.y)
    val sx = if (start.x < end.x) 1 else -1
    val sy = if (start.y < end.y) 1 else -1
    var err = dx - dy

    while (true) {

        if (!gridMap.walkable[x][y]) return false
        if (x == end.x && y == end.y) break
        val e2 = 2 * err
        if (e2 > -dy) {
            err -= dy
            x += sx
        }
        if (e2 < dx) {
            err += dx
            y += sy
        }
    }
    return true
}

fun geoPointToGridCell(point: GeoPoint, gridWidth: Int, gridHeight: Int): GridNode {
    val latRange = AppConstants.MAX_LAT - AppConstants.MIN_LAT
    val lonRange = AppConstants.MAX_LON - AppConstants.MIN_LON

    val y = ((point.latitude - AppConstants.MIN_LAT) / latRange * gridHeight).toInt()
    val x = ((point.longitude - AppConstants.MIN_LON) / lonRange * gridWidth).toInt()

    return GridNode(
        x.coerceIn(0, gridWidth - 1),
        y.coerceIn(0, gridHeight - 1)
    )
}

fun gridCellToGeoPoint(node: GridNode, width: Int, height: Int): GeoPoint {
    val latRange = AppConstants.MAX_LAT - AppConstants.MIN_LAT
    val lonRange = AppConstants.MAX_LON - AppConstants.MIN_LON

    val lat = AppConstants.MIN_LAT + ((node.y.toDouble() + 0.5) / height) * latRange
    val lon = AppConstants.MIN_LON + ((node.x.toDouble() + 0.5) / width) * lonRange

    return GeoPoint(lat, lon)
}

@Composable
fun MapScreen(gridMap: GridMap) {

    val context = LocalContext.current

    var startPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var endPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var routeToDraw by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }

    var currentSection by remember { mutableStateOf(AppSection.NAVIGATION) }

    var isGridEnabled by remember { mutableStateOf(false) }
    var showOnlyWalkable by remember { mutableStateOf(true) }

    val aStar = remember { AStar<GridNode>() }

    val gridOverlay = remember(gridMap) { GridOverlay(gridMap) }

    fun calculatePath() {
        if (startPoint != null && endPoint != null) {
            val startNode = geoPointToGridCell(startPoint!!, gridMap.width, gridMap.height)
            val goalNode = geoPointToGridCell(endPoint!!, gridMap.width, gridMap.height)
            
            val snappedStart = gridMap.getNearestWalkable(startNode.x, startNode.y)
            val snappedGoal = gridMap.getNearestWalkable(goalNode.x, goalNode.y)
            
            if (snappedStart == null || snappedGoal == null) {
                routeToDraw = emptyList()
                return
            }

            startPoint = if (snappedStart != startNode) {
                gridCellToGeoPoint(snappedStart, gridMap.width, gridMap.height)
            } else startPoint
            
            endPoint = if (snappedGoal != goalNode) {
                gridCellToGeoPoint(snappedGoal, gridMap.width, gridMap.height)
            } else endPoint

            val result = aStar.findPath(
                start = snappedStart,
                goal = snappedGoal,
                getNeighbors = { gridMap.getNeighbors(it) },
                heuristic = { a, b -> gridMap.heuristic(a, b) },
                costBetween = { a, b -> gridMap.costBetween(a, b) }
            )

            if (result != null && result.path.isNotEmpty()) {
                val smoothedPath = smoothPath(result.path, gridMap)
                routeToDraw = smoothedPath.map { gridCellToGeoPoint(it, gridMap.width, gridMap.height) }
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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = true
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setCenter(GeoPoint((AppConstants.MAX_LAT + AppConstants.MIN_LAT) / 2, (AppConstants.MAX_LON + AppConstants.MIN_LON) / 2))
                    controller.setZoom(AppConstants.DEFAULT_ZOOM)
                }
            }, update = { mapView ->
                gridOverlay.gridEnabled = isGridEnabled
                gridOverlay.showOnlyWalkable = showOnlyWalkable
                mapView.overlays.clear()
                val rotationOverlay = RotationGestureOverlay(mapView)
                rotationOverlay.isEnabled = true
                mapView.overlays.add(rotationOverlay)
                mapView.overlays.add(gridOverlay)

                startPoint?.let {
                    val marker = Marker(mapView)
                    marker.position = it
                    marker.icon = createBlueDotIcon(context, AppConstants.MARKER_SIZE_PX)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    marker.title = "Старт"
                    marker.infoWindow = null
                    mapView.overlays.add(marker)
                }

                endPoint?.let {
                    val marker = Marker(mapView)
                    marker.position = it
                    marker.icon = createBlueDotIcon(context, AppConstants.MARKER_SIZE_PX)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = "Финиш"
                    marker.infoWindow = null
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

                if (routeToDraw.isNotEmpty()) {
                    val polyline = Polyline(mapView)
                    polyline.setPoints(routeToDraw)
                    polyline.outlinePaint.color = ROUTE_COLOR
                    polyline.outlinePaint.strokeWidth = AppConstants.ROUTE_WIDTH_PX
                    polyline.infoWindow = null
                    mapView.overlays.add(polyline)
                }
                mapView.invalidate()
            }
        )

        NavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            containerColor = ComposeColor.White, 
            contentColor = TSU_LightBlue
        ) {
            AppSection.values().forEach { section ->
                NavigationBarItem(
                    selected = currentSection == section,
                    onClick = { currentSection = section },
                    icon = { },
                    label = { 
                        Text(
                            section.title, 
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (currentSection == section) TSU_LightBlue else ComposeColor.Gray
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TSU_LightBlue,
                        selectedTextColor = TSU_LightBlue,
                        indicatorColor = TSU_LightBlue.copy(alpha = 0.1f),
                        unselectedTextColor = ComposeColor.Gray,
                        unselectedIconColor = ComposeColor.Gray
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .padding(top = 48.dp)
                .background(ComposeColor.White.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.medium)
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isGridEnabled, onCheckedChange = { isGridEnabled = it })
                Text("Сетка", style = MaterialTheme.typography.bodySmall)
            }
            if (isGridEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showOnlyWalkable, onCheckedChange = { showOnlyWalkable = it })
                    Text("Только дороги", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (currentSection == AppSection.NAVIGATION) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { calculatePath() }, 
                    enabled = startPoint != null && endPoint != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TSU_LightBlue,
                        contentColor = ComposeColor.White,
                        disabledContainerColor = TSU_LightBlue.copy(alpha = 0.5f),
                        disabledContentColor = ComposeColor.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text("Построить маршрут",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                }
                Button(
                    onClick = { clearMap() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TSU_LightBlue,
                        contentColor = ComposeColor.White
                    )
                ) {
                    Text("Очистить",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }
    }
}

enum class AppSection(val title: String) {
    NAVIGATION("A*"),
    CLUSTERING("Кластеры"),
    GENETIC("Обед"),
    ANTS("Муравьи"),
    TREE("Выбор"),
    NEURAL("Оценка")
}

fun createBlueDotIcon(context: Context, sizePx: Int): Drawable {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = TSU_LIGHT_BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
    return BitmapDrawable(context.resources, bitmap)
}