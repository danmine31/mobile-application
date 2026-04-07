package com.example.myapplication.ui.screens

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import com.example.myapplication.data.GridMap
import com.example.myapplication.data.GridNode
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class GridOverlay(private val gridMap: GridMap) : Overlay() {

    private val paintLine = Paint().apply {
        color = Color.DKGRAY
        alpha = 160
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val projection = mapView.projection
        val p1 = Point()
        val p2 = Point()

        //---------------
        val paintObstacle = Paint().apply { color = Color.RED; alpha = 50 }

        for (i in 0 until gridMap.height) {
            for (j in 0 until gridMap.width) {
                if (!gridMap.walkable[i][j]) {
                    val gp = gridCellToGeoPoint(GridNode(j, i), gridMap.width, gridMap.height)
                    projection.toPixels(gp, p1)
                    canvas.drawRect(p1.x - 5f, p1.y - 5f, p1.x + 5f, p1.y + 5f, paintObstacle)
                }
            }
        }
        //--------------

        for (i in 0..gridMap.height) {
            val startGP = gridCellToGeoPoint(GridNode(0, i), gridMap.width, gridMap.height)
            val endGP = gridCellToGeoPoint(GridNode(gridMap.width, i), gridMap.width, gridMap.height)

            projection.toPixels(startGP, p1)
            projection.toPixels(endGP, p2)
            canvas.drawLine(p1.x.toFloat(), p1.y.toFloat(), p2.x.toFloat(), p2.y.toFloat(), paintLine)
        }

        for (j in 0..gridMap.width) {
            val startGP = gridCellToGeoPoint(GridNode(j, 0), gridMap.width, gridMap.height)
            val endGP = gridCellToGeoPoint(GridNode(j, gridMap.height), gridMap.width, gridMap.height)

            projection.toPixels(startGP, p1)
            projection.toPixels(endGP, p2)
            canvas.drawLine(p1.x.toFloat(), p1.y.toFloat(), p2.x.toFloat(), p2.y.toFloat(), paintLine)
        }
    }
}