package com.example.myapplication.data

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun GridVisualizer(grid: Array<IntArray>) {
    val rows = grid.size
    val cols = grid[0].size

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellSizePx = minOf(size.width / cols.toFloat(), size.height / rows.toFloat())

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val color = if (grid[i][j] == 1) Color.Black else Color.LightGray
                val topLeft = Offset(j * cellSizePx, i * cellSizePx)
                drawRect(
                    color = color,
                    topLeft = topLeft,
                    size = Size(cellSizePx, cellSizePx)
                )
            }
        }
    }
}
