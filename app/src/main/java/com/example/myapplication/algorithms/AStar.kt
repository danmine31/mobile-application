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
        val fScore = mutableMapOf(start to heuristic(start, goal))
        val openQueue = PriorityQueue<T> { a, b ->
            (fScore[a] ?: Double.MAX_VALUE).compareTo(fScore[b] ?: Double.MAX_VALUE)
        }
        val openSet = mutableSetOf(start)
        openQueue.add(start)

        val closedSet = mutableSetOf<T>()
        val cameFrom = mutableMapOf<T, T>()
        val gScore = mutableMapOf(start to 0.0)

        while (openQueue.isNotEmpty()) {
            val current = openQueue.poll()!!

            if (current in closedSet) continue

            openSet.remove(current)

            onStep?.invoke(Step(current, openSet.toSet(), closedSet.toSet()))

            if (current == goal) {
                return Result(reconstructPath(cameFrom, current))
            }

            closedSet.add(current)

            for (neighbor in getNeighbors(current)) {
                if (neighbor in closedSet) continue

                val tentativeGScore =
                    (gScore[current] ?: Double.MAX_VALUE) + costBetween(current, neighbor)

                if (tentativeGScore < (gScore[neighbor] ?: Double.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    val newFScore = tentativeGScore + heuristic(neighbor, goal)
                    fScore[neighbor] = newFScore

                    openSet.add(neighbor)
                    openQueue.add(neighbor)
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