package com.example.pathfinding.algorithms

import com.example.pathfinding.models.PointOfInterest
import kotlin.math.pow
import kotlin.random.Random

class AntColony(
    private val points: List<PointOfInterest>,
    private val startPoint: PointOfInterest,
    private val antCount: Int = 50,
    private val iterations: Int = 100,
    private val alpha: Double = 1.0,
    private val beta: Double = 2.0,
    private val evaporation: Double = 0.5,
    private val q: Double = 100.0
) {
    private val n = points.size
    private val distances = Array(n) { i ->
        DoubleArray(n) { j ->
            if (i == j) 0.0 else {
                val a = points[i]
                val b = points[j]
                val dx = a.x - b.x
                val dy = a.y - b.y
                Math.sqrt(dx * dx + dy * dy)
            }
        }
    }

    private var pheromones = Array(n) { DoubleArray(n) { 1.0 } }

    data class AntResult(val route: List<Int>, val distance: Double)

    fun run(): AntResult {
        var bestRoute: List<Int> = emptyList()
        var bestDistance = Double.POSITIVE_INFINITY

        repeat(iterations) {
            val ants = List(antCount) { Ant() }
            val results = ants.map { it.findRoute() }

            for (i in 0 until n) {
                for (j in 0 until n) {
                    pheromones[i][j] *= (1 - evaporation)
                }
            }

            for (ant in results) {
                val route = ant.route
                val distance = ant.distance
                val contribution = q / distance
                for (k in 0 until route.size - 1) {
                    val from = route[k]
                    val to = route[k + 1]
                    pheromones[from][to] += contribution
                    pheromones[to][from] += contribution
                }
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestRoute = route
                }
            }
        }

        return AntResult(bestRoute, bestDistance)
    }

    private inner class Ant {
        fun findRoute(): AntResult {
            val visited = BooleanArray(n)
            val route = mutableListOf<Int>()
            val startIdx = points.indexOf(startPoint)
            if (startIdx == -1) throw IllegalArgumentException("Стартовая точка не найдена")
            route.add(startIdx)
            visited[startIdx] = true

            var current = startIdx
            var totalDistance = 0.0

            while (route.size < n) {
                val next = selectNext(current, visited)
                if (next == -1) break
                totalDistance += distances[current][next]
                route.add(next)
                visited[next] = true
                current = next
            }
            totalDistance += distances[current][startIdx]
            return AntResult(route, totalDistance)
        }

        private fun selectNext(current: Int, visited: BooleanArray): Int {
            val probabilities = mutableListOf<Pair<Int, Double>>()
            var sum = 0.0
            for (i in 0 until n) {
                if (!visited[i]) {
                    val pheromone = pheromones[current][i].pow(alpha)
                    val heuristic = (1.0 / distances[current][i]).pow(beta)
                    val prob = pheromone * heuristic
                    probabilities.add(i to prob)
                    sum += prob
                }
            }
            if (sum == 0.0) return -1
            val rand = Random.nextDouble() * sum
            var acc = 0.0
            for ((idx, prob) in probabilities) {
                acc += prob
                if (acc >= rand) return idx
            }
            return probabilities.lastOrNull()?.first ?: -1
        }
    }
}