package com.example.myapplication.ui.screens

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import com.example.myapplication.data.AppConstants
import com.example.myapplication.data.GridMap
import com.example.myapplication.data.GridNode
import com.example.myapplication.algorithms.AStar
import com.example.myapplication.data.DynamicObstacle
import com.example.myapplication.data.ObstacleType
import com.example.myapplication.ui.theme.GRID_WALKABLE_COLOR
import com.example.myapplication.ui.theme.GRID_OBSTACLE_COLOR
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class GridOverlay(
    private val gridMap: GridMap,
    var gridEnabled: Boolean = true,
    var showOnlyWalkable: Boolean = true,
    var clusterCentroids: List<org.osmdroid.util.GeoPoint> = emptyList(),
    var clusterColors: List<Int> = emptyList(),
    var astarStep: AStar.Step<GridNode>? = null
) : Overlay() {

    private val pDraw = Point()
    private val paintWalkable = Paint().apply {
        color = GRID_WALKABLE_COLOR
        style = Paint.Style.FILL
    }

    private val paintObstacle = Paint().apply {
        color = GRID_OBSTACLE_COLOR
        style = Paint.Style.FILL
    }

    private val paintDynamicObstacle = Paint().apply {
        color = 0xAAFF8C00.toInt()
        style = Paint.Style.FILL
    }

    private val paintAStarOpen = Paint().apply {
        color = 0x8000FF00.toInt()
        style = Paint.Style.FILL
    }

    private val paintAStarClosed = Paint().apply {
        color = 0x80FFFF00.toInt()
        style = Paint.Style.FILL
    }

    private val paintAStarCurrent = Paint().apply {
        color = 0xFFFF00FF.toInt()
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return

        if (mapView.zoomLevelDouble < AppConstants.MIN_ZOOM_FOR_GRID) return

        val projection = mapView.projection
        val viewPort = projection.boundingBox

        val latRange = AppConstants.MAX_LAT - AppConstants.MIN_LAT
        val lonRange = AppConstants.MAX_LON - AppConstants.MIN_LON

        val startX = (((viewPort.lonWest - AppConstants.MIN_LON) / lonRange) * gridMap.width).toInt().coerceIn(0, gridMap.width - 1)
        val endX = (((viewPort.lonEast - AppConstants.MIN_LON) / lonRange) * gridMap.width).toInt().coerceIn(0, gridMap.width - 1)
        val startY = (((viewPort.latSouth - AppConstants.MIN_LAT) / latRange) * gridMap.height).toInt().coerceIn(0, gridMap.height - 1)
        val endY = (((viewPort.latNorth - AppConstants.MIN_LAT) / latRange) * gridMap.height).toInt().coerceIn(0, gridMap.height - 1)

        val gp0 = gridCellToGeoPoint(GridNode(startX, startY), gridMap.width, gridMap.height)
        val gp1 = gridCellToGeoPoint(GridNode(startX + 1, startY + 1), gridMap.width, gridMap.height)
        
        projection.toPixels(gp0, pDraw)
        val px0 = pDraw.x
        val py0 = pDraw.y
        
        projection.toPixels(gp1, pDraw)
        val px1 = pDraw.x
        val py1 = pDraw.y

        val cellWidthPx = Math.abs(px1 - px0).toFloat()
        val cellHeightPx = Math.abs(py1 - py0).toFloat()

        if (gridEnabled && mapView.zoomLevelDouble >= AppConstants.MIN_ZOOM_FOR_GRID) {
            for (x in startX..endX) {
                for (y in startY..endY) {
                    val isBaseWalkable = gridMap.walkable[x][y]
                    if (showOnlyWalkable && !isBaseWalkable) continue
                    
                    val gp = gridCellToGeoPoint(GridNode(x, y), gridMap.width, gridMap.height)
                    projection.toPixels(gp, pDraw)
                    
                    val currentPaint = if (isBaseWalkable) paintWalkable else paintObstacle
                    canvas.drawRect(
                        pDraw.x.toFloat(),
                        pDraw.y.toFloat() - cellHeightPx,
                        pDraw.x.toFloat() + cellWidthPx,
                        pDraw.y.toFloat(),
                        currentPaint
                    )
                }
            }
        }

        for (node in gridMap.dynamicObstacles) {
            if (node.x in startX..endX && node.y in startY..endY) {
                val gp = gridCellToGeoPoint(node, gridMap.width, gridMap.height)
                projection.toPixels(gp, pDraw)
                canvas.drawRect(
                    pDraw.x.toFloat(),
                    pDraw.y.toFloat() - cellHeightPx,
                    pDraw.x.toFloat() + cellWidthPx,
                    pDraw.y.toFloat(),
                    paintDynamicObstacle
                )
            }
        }

        astarStep?.let { step ->
            val visibleOpen = step.openSet.filter { it.x in startX..endX && it.y in startY..endY }
            val visibleClosed = step.closedSet.filter { it.x in startX..endX && it.y in startY..endY }
            
            visibleOpen.forEach { node ->
                val gp = gridCellToGeoPoint(node, gridMap.width, gridMap.height)
                projection.toPixels(gp, pDraw)
                canvas.drawRect(pDraw.x.toFloat(), pDraw.y.toFloat() - cellHeightPx, pDraw.x.toFloat() + cellWidthPx, pDraw.y.toFloat(), paintAStarOpen)
            }
            visibleClosed.forEach { node ->
                val gp = gridCellToGeoPoint(node, gridMap.width, gridMap.height)
                projection.toPixels(gp, pDraw)
                canvas.drawRect(pDraw.x.toFloat(), pDraw.y.toFloat() - cellHeightPx, pDraw.x.toFloat() + cellWidthPx, pDraw.y.toFloat(), paintAStarClosed)
            }
            if (step.current.x in startX..endX && step.current.y in startY..endY) {
                val gp = gridCellToGeoPoint(step.current, gridMap.width, gridMap.height)
                projection.toPixels(gp, pDraw)
                canvas.drawRect(pDraw.x.toFloat(), pDraw.y.toFloat() - cellHeightPx, pDraw.x.toFloat() + cellWidthPx, pDraw.y.toFloat(), paintAStarCurrent)
            }
        }

        if (gridEnabled && clusterCentroids.isNotEmpty() && clusterColors.isNotEmpty()) {
            val zoom = mapView.zoomLevelDouble
            val step = if (zoom < 14.0) 8 else if (zoom < 16.0) 4 else if (zoom < 17.0) 2 else 1
            for (x in startX..endX step step) {
                for (y in startY..endY step step) {
                    val gp = gridCellToGeoPoint(GridNode(x, y), gridMap.width, gridMap.height)
                    var closestIdx = -1
                    var minDist = Double.MAX_VALUE
                    
                    for (i in clusterCentroids.indices) {
                        val d = gp.distanceToAsDouble(clusterCentroids[i])
                        if (d < minDist) {
                            minDist = d
                            closestIdx = i
                        }
                    }
                    
                    if (closestIdx != -1) {
                        projection.toPixels(gp, pDraw)
                        val color = clusterColors[closestIdx % clusterColors.size]
                        val paint = Paint().apply { 
                            this.color = color
                            alpha = 60 
                        }
                        canvas.drawRect(
                            pDraw.x.toFloat(),
                            pDraw.y.toFloat() - cellHeightPx * step,
                            pDraw.x.toFloat() + cellWidthPx * step,
                            pDraw.y.toFloat(),
                            paint
                        )
                    }
                }
            }
        }
    }
}