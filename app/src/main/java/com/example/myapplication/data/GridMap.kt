package com.example.myapplication.data

class GridMap(
    val width: Int,
    val height: Int,
    val walkable: Array<BooleanArray>
) {
    fun getNeighbors(node: GridNode): List<GridNode> {
        val neighbors = mutableListOf<GridNode>()
        val directions = listOf(
            GridNode(0, -1),
            GridNode(0, 1),
            GridNode(-1, 0),
            GridNode(1, 0)
        )
        for (dir in directions) {
            val nx = node.x + dir.x
            val ny = node.y + dir.y
            if (nx in 0 until width && ny in 0 until height && walkable[nx][ny]) {
                neighbors.add(GridNode(nx, ny))
            }
        }
        return neighbors
    }

    fun heuristic(a: GridNode, b: GridNode): Double {
        return (Math.abs(a.x - b.x) + Math.abs(a.y - b.y)).toDouble()
    }

    fun costBetween(a: GridNode, b: GridNode): Double = 1.0
}