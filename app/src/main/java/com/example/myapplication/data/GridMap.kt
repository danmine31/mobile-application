package com.example.myapplication.data

data class FoodPoint(
    val name: String,
    val lat: Double,
    val lon: Double,
    val type: String
)

data class DynamicObstacle(
    val type: ObstacleType,
    val points: List<GridNode>,
    val radiusCells: Int = 2
)

enum class ObstacleType { LINE, CIRCLE }

class GridMap(
    val width: Int,
    val height: Int,
    val walkable: Array<BooleanArray>,
    val foodPoints: List<FoodPoint> = emptyList(),
    val dynamicObstacles: MutableSet<GridNode> = mutableSetOf()
) {

    fun isWalkableAt(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        if (!walkable[x][y]) return false

        if (dynamicObstacles.contains(GridNode(x, y))) return false
        
        return true
    }

    fun getNearestWalkable(x: Int, y: Int): GridNode? {
        if (isWalkableAt(x, y)) return GridNode(x, y)

        val maxDist = maxOf(width, height)
        for (dist in 1..maxDist) {
            for (dx in -dist..dist) {
                for (dy in -dist..dist) {
                    if (Math.abs(dx) != dist && Math.abs(dy) != dist) continue
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height && isWalkableAt(nx, ny)) {
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
                
                if (nx in 0 until width && ny in 0 until height && isWalkableAt(nx, ny)) {
                    if (Math.abs(dx) == 1 && Math.abs(dy) == 1) {
                        if (isWalkableAt(node.x + dx, node.y) && isWalkableAt(node.x, node.y + dy)) {
                            neighbors.add(GridNode(nx, ny))
                        }
                    } else {
                        neighbors.add(GridNode(nx, ny))
                    }
                }
            }
        }
        return neighbors
    }

    fun heuristic(a: GridNode, b: GridNode): Double {
        return Math.sqrt(Math.pow((a.x-b.x).toDouble(), 2.0)+Math.pow((a.y-b.y).toDouble(), 2.0))
    }

    fun costBetween(a: GridNode, b: GridNode): Double {
        return if (a.x != b.x && a.y != b.y) 1.41421356 else 1.0
    }
}