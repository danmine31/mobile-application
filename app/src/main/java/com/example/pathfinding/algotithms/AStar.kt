package com.example.pathfinding.algorithms

import java.util.PriorityQueue

data class AStarResult<T>(
    val path: List<T>,
    val cost: Double,
    val steps: List<AStar.Step<T>>? = null
)

class AStar<T> {

    data class Step<T>(
        val current: T?,
        val openSet: Set<T>,
        val closedSet: Set<T>,
        val cameFrom: Map<T, T>,
        val gScore: Map<T, Double>,
        val fScore: Map<T, Double>
    )

    fun findPath(
        start: T,
        goal: T,
        getNeighbors: (T) -> List<T>,
        heuristic: (T, T) -> Double,
        collectSteps: Boolean = false
    ): AStarResult<T>? {
        val steps = if (collectSteps) mutableListOf<Step<T>>() else null

        val openSet = PriorityQueue<Pair<T, Double>>(compareBy { it.second })
        val openMap = mutableMapOf<T, Double>()
        val closedSet = mutableSetOf<T>()

        val gScore = mutableMapOf<T, Double>().withDefault { Double.POSITIVE_INFINITY }
        val fScore = mutableMapOf<T, Double>().withDefault { Double.POSITIVE_INFINITY }
        val cameFrom = mutableMapOf<T, T>()

        gScore[start] = 0.0
        val startF = heuristic(start, goal)
        fScore[start] = startF
        openSet.add(start to startF)
        openMap[start] = startF

        while (openSet.isNotEmpty()) {
            val (current, _) = openSet.poll()
            openMap.remove(current)

            if (collectSteps) {
                steps?.add(
                    Step(
                        current = current,
                        openSet = openMap.keys.toSet(),
                        closedSet = closedSet.toSet(),
                        cameFrom = cameFrom.toMap(),
                        gScore = gScore.toMap(),
                        fScore = fScore.toMap()
                    )
                )
            }

            if (current == goal) {
                val path = reconstructPath(cameFrom, current)
                val totalCost = gScore[current] ?: 0.0
                return AStarResult(path, totalCost, steps)
            }

            closedSet.add(current)

            for (neighbor in getNeighbors(current)) {
                if (neighbor in closedSet) continue

                val stepCost = costBetween(current, neighbor)
                val tentativeG = (gScore[current] ?: Double.POSITIVE_INFINITY) + stepCost

                if (tentativeG < (gScore[neighbor] ?: Double.POSITIVE_INFINITY)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeG
                    val neighborF = tentativeG + heuristic(neighbor, goal)
                    fScore[neighbor] = neighborF

                    if (neighbor !in openMap) {
                        openSet.add(neighbor to neighborF)
                        openMap[neighbor] = neighborF
                    }
                }
            }
        }

        return null
    }

    protected open fun costBetween(a: T, b: T): Double = 1.0

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