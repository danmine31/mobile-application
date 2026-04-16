package com.example.myapplication.data

class GridMap(
    val width: Int,
    val height: Int,
    val walkable: Array<BooleanArray>
) {

    fun getNearestWalkable(x: Int, y: Int): GridNode? {
        if (walkable[x][y]) return GridNode(x, y)

        val maxDist = maxOf(width, height)
        for (dist in 1..maxDist) {
            for (dx in -dist..dist) {
                for (dy in -dist..dist) {
                    if (Math.abs(dx) != dist && Math.abs(dy) != dist) continue
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height && walkable[nx][ny]) {
                        return GridNode(nx, ny)
                    }
                }
            }
        }
        return null
    }

    fun getNeighbors(node: GridNode): List<GridNode> {
        val neighbors = mutableListOf<GridNode>()
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = node.x + dx
                val ny = node.y + dy
                if (nx in 0 until width && ny in 0 until height && walkable[nx][ny]) {
                    neighbors.add(GridNode(nx, ny))
                }
            }
        }
        return neighbors
    }

    fun heuristic(a: GridNode, b: GridNode): Double {
        return Math.sqrt(Math.pow((a.x-b.x).toDouble(), 2.0) + Math.pow((a.y-b.y).toDouble(), 2.0))
    }

    fun costBetween(a: GridNode, b: GridNode): Double {
        return if (a.x != b.x && a.y != b.y) 1.41421356 else 1.0
    }

    fun costBetween(a: GridNode, b: GridNode): Double = 1.0
}