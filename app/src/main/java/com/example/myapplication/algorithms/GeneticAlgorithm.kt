package com.example.myapplication.algorithms

import kotlin.random.Random

class GeneticAlgorithm(
    private val numNodes: Int,
    private val distanceMatrix: Array<DoubleArray>,
    private val openHours: IntArray,
    private val closeHours: IntArray,
    private val currentHour: Int,
    private val populationSize: Int = 100,
    private val generations: Int = 200,
    private val mutationRate: Double = 0.05
) {
    data class Individual(val route: List<Int>, val fitness: Double)

    fun run(): Individual {
        val cafeIndices = (1 until numNodes).toList()
        if (cafeIndices.isEmpty()) return Individual(emptyList(), 0.0)

        var population = List(populationSize) {
            val route = cafeIndices.shuffled()
            Individual(route, evaluate(route))
        }.toMutableList()

        repeat(generations) {
            val newPopulation = mutableListOf<Individual>()
            val best = population.minByOrNull { it.fitness }!!
            newPopulation.add(best)

            while (newPopulation.size < populationSize) {
                val parent1 = selection(population)
                val parent2 = selection(population)
                var child = crossover(parent1, parent2)
                child = mutate(child)
                newPopulation.add(Individual(child, evaluate(child)))
            }
            population = newPopulation
        }

        return population.minByOrNull { it.fitness }!!
    }

    private fun selection(population: List<Individual>): List<Int> {
        return population.shuffled().take(5).minByOrNull { it.fitness }!!.route
    }

    private fun crossover(parent1: List<Int>, parent2: List<Int>): List<Int> {
        val size = parent1.size
        if (size < 2) return parent1
        val start = Random.nextInt(size)
        val end = Random.nextInt(start, size)
        val child = MutableList<Int?>(size) { null }

        for (i in start..end) child[i] = parent1[i]

        val remaining = parent2.filter { it !in child }
        var pos = 0
        for (i in 0 until size) {
            if (child[i] == null) child[i] = remaining[pos++]
        }
        return child.requireNoNulls()
    }

    private fun mutate(route: List<Int>): List<Int> {
        if (route.size < 2) return route
        return if (Random.nextDouble() < mutationRate) {
            val idx1 = Random.nextInt(route.size)
            val idx2 = Random.nextInt(route.size)
            val newRoute = route.toMutableList()
            newRoute[idx1] = newRoute[idx2].also { newRoute[idx2] = newRoute[idx1] }
            newRoute
        } else route
    }

    private fun evaluate(route: List<Int>): Double {
        var totalTimeMinutes = 0.0
        var currentPos = 0

        for (nextPos in route) {
            val distMeters = distanceMatrix[currentPos][nextPos]
            if (distMeters == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY

            val travelTime = distMeters / 83.33
            totalTimeMinutes += travelTime

            val arrivalHour = ((currentHour * 60 + totalTimeMinutes) / 60).toInt() % 24
            if (arrivalHour < openHours[nextPos] || arrivalHour >= closeHours[nextPos]) {
                totalTimeMinutes += 1000.0
            }

            currentPos = nextPos
        }
        return totalTimeMinutes
    }
}
