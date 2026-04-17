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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.CustomZoomButtonsController
import android.view.MotionEvent
import org.osmdroid.views.overlay.Overlay
import android.graphics.Point
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.myapplication.algorithms.AStar
import com.example.myapplication.algorithms.kMeans
import com.example.myapplication.algorithms.DistanceMetric
import com.example.myapplication.algorithms.Point as AlgoPoint
import com.example.myapplication.data.AppConstants
import com.example.myapplication.data.GridMap
import com.example.myapplication.data.GridNode
import com.example.myapplication.ui.theme.TSU_LIGHT_BLUE
import com.example.myapplication.ui.theme.TSU_LightBlue
import com.example.myapplication.ui.theme.TSU_DarkBlue
import com.example.myapplication.ui.theme.ROUTE_COLOR
import com.example.myapplication.ui.theme.GRID_WALKABLE_COLOR
import com.example.myapplication.ui.theme.GRID_OBSTACLE_COLOR
import com.example.myapplication.data.DynamicObstacle
import com.example.myapplication.data.ObstacleType
import android.widget.Toast
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.background
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import kotlinx.coroutines.delay

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

fun getNodesOnLine(start: GridNode, end: GridNode): List<GridNode> {
    val nodes = mutableListOf<GridNode>()
    var x = start.x
    var y = start.y
    val dx = Math.abs(end.x - start.x)
    val dy = Math.abs(end.y - start.y)
    val sx = if (start.x < end.x) 1 else -1
    val sy = if (start.y < end.y) 1 else -1
    var err = dx - dy

    while (true) {
        nodes.add(GridNode(x, y))
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
    return nodes
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

    var isDrawingMode by remember { mutableStateOf(false) }
    var firstLinePoint by remember { mutableStateOf<GridNode?>(null) }
    var isAnimatingAStar by remember { mutableStateOf(false) }
    var animationSpeed by remember { mutableStateOf(50f) }
    var shouldAnimateAStar by remember { mutableStateOf(true) }
    var currentAStarStep by remember { mutableStateOf<AStar.Step<GridNode>?>(null) }

    var lastAddedNode by remember { mutableStateOf<GridNode?>(null) }

    val aStar = remember { AStar<GridNode>() }

    val gridOverlay = remember(gridMap) { GridOverlay(gridMap) }
    
    var kClusters by remember { mutableStateOf(3) }
    var clusterMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) }
    var selectedMetric by remember { mutableStateOf(DistanceMetric.EUCLIDEAN) }
    
    var isCalculating by remember { mutableStateOf(false) }
    var clusteringJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    var foodPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var userPoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var useUserPoints by remember { mutableStateOf(false) }

    val clusterColors = listOf(
        ComposeColor(0xFFE57373), ComposeColor(0xFF81C784), ComposeColor(0xFF64B5F6),
        ComposeColor(0xFFFFD54F), ComposeColor(0xFFBA68C8), ComposeColor(0xFF4DB6AC),
        ComposeColor(0xFFFF8A65), ComposeColor(0xFFA1887F), ComposeColor(0xFF90A4AE)
    )
    

    LaunchedEffect(gridMap) {
        foodPoints = gridMap.foodPoints.map { GeoPoint(it.lat, it.lon) }
    }

    var clusterCentroidsForOverlay by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    val clusterColorsInt = remember { clusterColors.map { 
        android.graphics.Color.argb(
            (it.alpha * 255).toInt(),
            (it.red * 255).toInt(),
            (it.green * 255).toInt(),
            (it.blue * 255).toInt()
        )
    } }

    fun calculateClusters() {
        val activePointsCopy = if (useUserPoints) userPoints.toList() else foodPoints.toList()
        if (activePointsCopy.isEmpty()) return
        
        clusteringJob?.cancel()
        clusteringJob = scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            isCalculating = true
            
            val latRange = AppConstants.MAX_LAT - AppConstants.MIN_LAT
            val lonRange = AppConstants.MAX_LON - AppConstants.MIN_LON
            
            val foodPointsSnapshot = gridMap.foodPoints.toList()
            
            val points = activePointsCopy.map { gp ->
                AlgoPoint(
                    x = (gp.longitude - AppConstants.MIN_LON) / lonRange,
                    y = (gp.latitude - AppConstants.MIN_LAT) / latRange
                )
            }
            
            val res = kMeans(points, kClusters, metric = selectedMetric)
            
            val centroids = res.centroids.map { p ->
                GeoPoint(
                    AppConstants.MIN_LAT + p.y * latRange,
                    AppConstants.MIN_LON + p.x * lonRange
                )
            }
        
            val markerData = activePointsCopy.mapIndexed { index, gp ->
                val clusterIdx = res.labels.getOrNull(index) ?: 0
                
                val name = if (!useUserPoints) {
                    foodPointsSnapshot.find { it.lat == gp.latitude && it.lon == gp.longitude }?.name ?: "Заведение"
                } else {
                    "Твоя точка ${index + 1}"
                }
                
                val title = "$name (Кластер ${clusterIdx + 1})"
                 val color = clusterColors[clusterIdx % clusterColors.size]
                 
                 ClusterMarkerData(gp, color, title)
             }

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                clusterCentroidsForOverlay = centroids
                
                clusterMarkers = markerData.map { data ->
                    Marker(MapView(context)).apply {
                        position = data.gp
                        icon = createColoredDotIcon(context, AppConstants.MARKER_SIZE_PX, data.color)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        this.title = data.title
                        
                        if (useUserPoints) {
                            infoWindow = null
                        }
                    }
                }
                isCalculating = false
            }
        }
    }

    fun calculatePath() {
        if (startPoint != null && endPoint != null) {

            routeToDraw = emptyList()
            currentAStarStep = null

            val startNode = geoPointToGridCell(startPoint!!, gridMap.width, gridMap.height)
            val goalNode = geoPointToGridCell(endPoint!!, gridMap.width, gridMap.height)
            
            val snappedStart = gridMap.getNearestWalkable(startNode.x, startNode.y)
            val snappedGoal = gridMap.getNearestWalkable(goalNode.x, goalNode.y)
            
            if (snappedStart == null || snappedGoal == null) {
                routeToDraw = emptyList()
                Toast.makeText(context, "Нельзя построить путь (точка в препятствии)", Toast.LENGTH_SHORT).show()
                return
            }

            scope.launch {
                isAnimatingAStar = true
                currentAStarStep = null
                
                val result = withContext(Dispatchers.Default) {
                    aStar.findPath(
                        start = snappedStart,
                        goal = snappedGoal,
                        getNeighbors = { gridMap.getNeighbors(it) },
                        heuristic = { a, b -> gridMap.heuristic(a, b) },
                        costBetween = { a, b -> gridMap.costBetween(a, b) },
                        onStep = { step ->
                            if (shouldAnimateAStar) {
                                withContext(Dispatchers.Main) {
                                    currentAStarStep = step
                                }
                                delay(animationSpeed.toLong())
                            }
                        }
                    )
                }

                if (result != null) {
                    val smoothedPath = smoothPath(result.path, gridMap)
                    routeToDraw = smoothedPath.map { gridCellToGeoPoint(it, gridMap.width, gridMap.height) }
                    currentAStarStep = null
                } else {
                    routeToDraw = emptyList()
                    Toast.makeText(context, "Путь не найден!", Toast.LENGTH_LONG).show()
                }
                isAnimatingAStar = false
            }
        }
    }

    fun clearMap() {
        startPoint = null
        endPoint = null
        routeToDraw = emptyList()
        currentAStarStep = null
        firstLinePoint = null
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
                    
                    controller.setZoom(AppConstants.DEFAULT_ZOOM)
                    val centerLat = (AppConstants.MAX_LAT + AppConstants.MIN_LAT) / 2
                    val centerLon = (AppConstants.MAX_LON + AppConstants.MIN_LON) / 2
                    controller.setCenter(GeoPoint(centerLat, centerLon))
                    controller.setZoom(AppConstants.DEFAULT_ZOOM)
                }
            }, update = { mapView ->

                gridOverlay.gridEnabled = isGridEnabled
                gridOverlay.showOnlyWalkable = showOnlyWalkable
                gridOverlay.astarStep = currentAStarStep

                if (currentSection == AppSection.CLUSTERING) {
                    gridOverlay.clusterCentroids = clusterCentroidsForOverlay
                    gridOverlay.clusterColors = clusterColorsInt
                } else {
                    gridOverlay.clusterCentroids = emptyList()
                }

                mapView.overlays.clear()
                
                val rotationOverlay = RotationGestureOverlay(mapView)
                rotationOverlay.isEnabled = !isDrawingMode
                mapView.overlays.add(rotationOverlay)
                mapView.overlays.add(gridOverlay)


                val drawingOverlay = object : Overlay() {
                    override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
                        if (!isDrawingMode || currentSection != AppSection.NAVIGATION) return false
                        
                        val gp = mapView.projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                        val node = geoPointToGridCell(gp, gridMap.width, gridMap.height)

                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                firstLinePoint = node
                                gridMap.dynamicObstacles.add(node)
                                lastAddedNode = node
                                mapView.invalidate()
                                return true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (node != lastAddedNode) {

                                    if (lastAddedNode != null) {
                                        val lineNodes = getNodesOnLine(lastAddedNode!!, node)
                                        gridMap.dynamicObstacles.addAll(lineNodes)
                                    } else {
                                        gridMap.dynamicObstacles.add(node)
                                    }
                                    lastAddedNode = node
                                    mapView.invalidate()
                                }
                                return true
                            }
                            MotionEvent.ACTION_UP -> {
                                firstLinePoint = null
                                lastAddedNode = null
                                return true
                            }
                        }
                        return false
                    }
                }
                mapView.overlays.add(drawingOverlay)

                if (currentSection == AppSection.NAVIGATION) {
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

                    if (routeToDraw.isNotEmpty()) {
                        val polyline = Polyline(mapView)
                        polyline.setPoints(routeToDraw)
                        polyline.outlinePaint.color = ROUTE_COLOR
                        polyline.outlinePaint.strokeWidth = AppConstants.ROUTE_WIDTH_PX
                        polyline.infoWindow = null
                        mapView.overlays.add(polyline)
                    }
                }

                val eventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        p?.let { gp ->
                            if (currentSection == AppSection.NAVIGATION) {
                                if (!isDrawingMode) {

                                    if (startPoint == null) {
                                        startPoint = gp
                                    } else if (endPoint == null) {
                                        endPoint = gp
                                    } else {
                                        startPoint = gp
                                        endPoint = null
                                        routeToDraw = emptyList()
                                    }
                                }
                            } else if (currentSection == AppSection.CLUSTERING) {
                                if (useUserPoints) {
                                    userPoints = userPoints + gp
                                }
                            }
                        }
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                }
                mapView.overlays.add(MapEventsOverlay(eventsReceiver))

                if (currentSection == AppSection.CLUSTERING) {
                    if (useUserPoints) {
                        userPoints.toList().forEachIndexed { index, gp ->
                            val tempMarker = Marker(mapView).apply {
                                position = gp
                                icon = createColoredDotIcon(context, AppConstants.MARKER_SIZE_PX, ComposeColor.Gray)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = "Твоя точка ${index + 1}"
                                infoWindow = null
                            }
                            mapView.overlays.add(tempMarker)
                        }
                    }

                     clusterMarkers.forEach { marker ->
                         val m = Marker(mapView).apply {
                             position = marker.position
                             icon = marker.icon
                             setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                             title = marker.title

                             if (useUserPoints) {
                                 infoWindow = null
                             }
                         }
                         mapView.overlays.add(m)
                     }
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
            if (currentSection == AppSection.NAVIGATION) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = shouldAnimateAStar, onCheckedChange = { shouldAnimateAStar = it })
                    Text("Анимация поиска", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (currentSection == AppSection.NAVIGATION) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(ComposeColor.White.copy(alpha = 0.9f), ComposeColor.White)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Навигация A*", style = MaterialTheme.typography.titleLarge, color = TSU_DarkBlue, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = {
                        gridMap.dynamicObstacles.clear()
                        clearMap()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = TSU_LightBlue)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Surface(
                        onClick = { isDrawingMode = !isDrawingMode; firstLinePoint = null },
                        color = if (isDrawingMode) TSU_LightBlue else ComposeColor.LightGray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isDrawingMode) Icons.Default.Edit else Icons.Default.Create,
                                contentDescription = null,
                                tint = if (isDrawingMode) ComposeColor.White else ComposeColor.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isDrawingMode) "Рисовашка ВКЛ" else "Рисовашка",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isDrawingMode) ComposeColor.White else ComposeColor.Gray
                            )
                        }
                    }
                }

                if (shouldAnimateAStar) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Скорость анимации", style = MaterialTheme.typography.labelMedium, color = ComposeColor.Gray)
                            Text("${animationSpeed.toInt()} мс", style = MaterialTheme.typography.labelMedium, color = TSU_LightBlue)
                        }
                        Slider(
                            value = animationSpeed,
                            onValueChange = { animationSpeed = it },
                            valueRange = 1f..200f,
                            colors = SliderDefaults.colors(
                                thumbColor = TSU_LightBlue,
                                activeTrackColor = TSU_LightBlue,
                                inactiveTrackColor = TSU_LightBlue.copy(alpha = 0.2f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { calculatePath() }, 
                    enabled = startPoint != null && endPoint != null && !isAnimatingAStar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TSU_LightBlue)
                ) {
                    if (isAnimatingAStar) {
                        CircularProgressIndicator(color = ComposeColor.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("ПОСТРОИТЬ МАРШРУТ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (currentSection == AppSection.CLUSTERING) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(ComposeColor.White.copy(alpha = 0.9f), ComposeColor.White)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Кластеризация", style = MaterialTheme.typography.titleLarge, color = TSU_DarkBlue, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Метрика:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = selectedMetric == DistanceMetric.EUCLIDEAN,
                        onClick = { selectedMetric = DistanceMetric.EUCLIDEAN; calculateClusters() },
                        label = { Text("Евклид") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = selectedMetric == DistanceMetric.MANHATTAN,
                        onClick = { selectedMetric = DistanceMetric.MANHATTAN; calculateClusters() },
                        label = { Text("Манхэттен") }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Точки:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !useUserPoints,
                        onClick = { useUserPoints = false; clusterMarkers = emptyList() },
                        label = { Text("Карта") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = useUserPoints,
                        onClick = { useUserPoints = true; clusterMarkers = emptyList() },
                        label = { Text("Свои") }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Кластеров: $kClusters", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = kClusters.toFloat(),
                        onValueChange = { kClusters = it.toInt() },
                        valueRange = 2f..9f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = TSU_LightBlue, activeTrackColor = TSU_LightBlue)
                    )
                }

                Button(
                    onClick = { calculateClusters() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TSU_LightBlue)
                ) {
                    if (isCalculating) {
                        CircularProgressIndicator(color = ComposeColor.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("РАССЧИТАТЬ ЗОНЫ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class ClusterMarkerData(
    val gp: GeoPoint,
    val color: ComposeColor,
    val title: String
)

enum class AppSection(val title: String) {
    NAVIGATION("A*"),
    CLUSTERING("Кластеры"),
    GENETIC("Обед"),
    ANTS("Муравьи"),
    TREE("Выбор"),
    NEURAL("Оценка")
}

enum class ObstacleMode { NONE, LINE, CIRCLE }

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

fun createColoredDotIcon(
    context: Context, 
    sizePx: Int, 
    composeColor: androidx.compose.ui.graphics.Color
): Drawable {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.argb(
            (composeColor.alpha * 255).toInt(),
            (composeColor.red * 255).toInt(),
            (composeColor.green * 255).toInt(),
            (composeColor.blue * 255).toInt()
        )
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2f, paint)
    
    paint.apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 1f, paint)
    
    return BitmapDrawable(context.resources, bitmap)
}