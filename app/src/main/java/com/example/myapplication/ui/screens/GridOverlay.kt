package com.example.myapplication.ui.screens

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import com.example.myapplication.data.AppConstants
import com.example.myapplication.data.GridMap
import com.example.myapplication.data.GridNode
import com.example.myapplication.ui.theme.GRID_WALKABLE_COLOR
import com.example.myapplication.ui.theme.GRID_OBSTACLE_COLOR
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class GridOverlay(
    private val gridMap: GridMap,
    var gridEnabled: Boolean = true,
    var showOnlyWalkable: Boolean = true
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

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !gridEnabled) return

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

        for (x in startX..endX) {
            for (y in startY..endY) {
                val isWalkable = gridMap.walkable[x][y]

                if (showOnlyWalkable && !isWalkable) continue

                val gp = gridCellToGeoPoint(GridNode(x, y), gridMap.width, gridMap.height)
                projection.toPixels(gp, pDraw)

                val currentPaint = if (isWalkable) paintWalkable else paintObstacle

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
}