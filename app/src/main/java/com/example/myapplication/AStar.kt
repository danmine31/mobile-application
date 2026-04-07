package com.example.myapplication

import java.util.PriorityQueue
import kotlin.math.sqrt

data class AStarResult<T>(
    val path: List<T>,
    val cost: Double
)

open class AStar<T> {
    open fun costBetween(a: T, b: T): Double = 1.0

    fun findPath(
        start: T,
        goal: T,
        getNeighbors: (T) -> List<T>,
        heuristic: (T, T) -> Double
    ): AStarResult<T>? {
        val openSet = PriorityQueue<Pair<T, Double>>(compareBy { it.second })
        val openMap = mutableMapOf<T, Double>()
        val closedSet = mutableSetOf<T>()

        val gScore = mutableMapOf<T, Double>().withDefault { Double.POSITIVE_INFINITY }
        val cameFrom = mutableMapOf<T, T>()

        gScore[start] = 0.0
        val startF = heuristic(start, goal)
        openSet.add(start to startF)
        openMap[start] = startF

        while (openSet.isNotEmpty()) {
            val (current, _) = openSet.poll()!!
            openMap.remove(current)

            if (current == goal) {
                return AStarResult(reconstructPath(cameFrom, current), gScore[current] ?: 0.0)
            }

            closedSet.add(current)

            for (neighbor in getNeighbors(current)) {
                if (neighbor in closedSet) continue

                val tentativeG = (gScore[current] ?: Double.POSITIVE_INFINITY) + costBetween(current, neighbor)

                if (tentativeG < (gScore[neighbor] ?: Double.POSITIVE_INFINITY)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeG
                    val fScore = tentativeG + heuristic(neighbor, goal)

                    if (neighbor !in openMap) {
                        openSet.add(neighbor to fScore)
                        openMap[neighbor] = fScore
                    }
                }
            }
        }
        return null
    }

    private fun reconstructPath(cameFrom: Map<T, T>, current: T): List<T> {
        val path = mutableListOf(current)
        var node = current
        while (cameFrom.containsKey(node)) {
            node = cameFrom[node]!!
            path.add(0, node)
        }
        return path
    }
}

class GridAStar(private val grid: Array<IntArray>) : AStar<Pair<Int, Int>>() {
    override fun costBetween(a: Pair<Int, Int>, b: Pair<Int, Int>): Double {
        val weight = grid[b.first][b.second].toDouble()

        val isDiagonal = a.first != b.first && a.second != b.second
        val distance = if (isDiagonal) sqrt(2.0) else 1.0

        return weight * distance
    }
}
