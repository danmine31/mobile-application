package com.example.myapplication.algorithms

import java.util.PriorityQueue

class AStar<T> {

    data class Step<T>(val current: T, val openSet: Set<T>, val closedSet: Set<T>)
    data class Result<T>(val path: List<T>, val steps: List<Step<T>>? = null)

    suspend fun findPath(
        start: T,
        goal: T,
        getNeighbors: (T) -> List<T>,
        heuristic: (T, T) -> Double,
        costBetween: (T, T) -> Double,
        onStep: (suspend (Step<T>) -> Unit)? = null
    ): Result<T>? {
        val openSet = mutableSetOf(start)
        val closedSet = mutableSetOf<T>()
        val cameFrom = mutableMapOf<T, T>()
        val gScore = mutableMapOf(start to 0.0)
        val fScore = mutableMapOf(start to heuristic(start, goal))

        while (openSet.isNotEmpty()) {
            val current = openSet.minByOrNull { fScore[it] ?: Double.MAX_VALUE } ?: break

            onStep?.invoke(Step(current, openSet.toSet(), closedSet.toSet()))

            if (current == goal) {
                return Result(reconstructPath(cameFrom, current))
            }

            openSet.remove(current)
            closedSet.add(current)

            for (neighbor in getNeighbors(current)) {
                if (neighbor in closedSet) continue

                val tentativeGScore = (gScore[current] ?: Double.MAX_VALUE) + costBetween(current, neighbor)

                if (tentativeGScore < (gScore[neighbor] ?: Double.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    fScore[neighbor] = tentativeGScore + heuristic(neighbor, goal)
                    openSet.add(neighbor)
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