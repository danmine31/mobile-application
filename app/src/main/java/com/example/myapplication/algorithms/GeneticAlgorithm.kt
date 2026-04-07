package com.example.pathfinding.algorithms

import com.example.pathfinding.models.Cafe
import kotlin.random.Random

class GeneticAlgorithm(
    private val cafes: List<Cafe>,
    private val neededCafes: List<Cafe>,
    private val populationSize: Int = 100,
    private val generations: Int = 200,
    private val mutationRate: Double = 0.05,
    private val currentHour: Int
) {
    data class Individual(val route: List<Cafe>, val fitness: Double)

    fun run(): Individual {
        var population = List(populationSize) {
            val route = neededCafes.shuffled()
            Individual(route, evaluate(route))
        }.toMutableList()

        repeat(generations) { gen ->
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

    private fun selection(population: List<Individual>): List<Cafe> {
        val tournament = population.shuffled().take(5)
        return tournament.minByOrNull { it.fitness }!!.route
    }

    private fun crossover(parent1: List<Cafe>, parent2: List<Cafe>): List<Cafe> {
        val size = parent1.size
        if (size < 2) return parent1
        val start = Random.nextInt(size)
        val end = Random.nextInt(start, size)
        val child = MutableList(size) { parent1[it] }
        val remaining = parent2.filter { it !in child.slice(start..end) }
        var pos = 0
        for (i in 0 until size) {
            if (i < start || i > end) {
                child[i] = remaining[pos++]
            }
        }
        return child
    }

    private fun mutate(route: List<Cafe>): List<Cafe> {
        if (route.size < 2) return route
        return if (Random.nextDouble() < mutationRate) {
            val idx1 = Random.nextInt(route.size)
            val idx2 = Random.nextInt(route.size)
            val newRoute = route.toMutableList()
            newRoute[idx1] = newRoute[idx2].also { newRoute[idx2] = newRoute[idx1] }
            newRoute
        } else {
            route
        }
    }

    private fun evaluate(route: List<Cafe>): Double {
        if (route.isEmpty()) return Double.POSITIVE_INFINITY
        var totalTime = 0.0
        var currentTimeMin = currentHour * 60.0

        fun travelTime(c1: Cafe, c2: Cafe): Double {
            val dx = c1.x - c2.x
            val dy = c1.y - c2.y
            val distKm = Math.sqrt(dx * dx + dy * dy) * 0.1
            return distKm / 5.0 * 60.0
        }

        val startCafe = Cafe("start", "Start", 0.0, 0.0, 0, 0, emptyList())
        totalTime += travelTime(startCafe, route[0])
        currentTimeMin += totalTime

        for (i in 0 until route.size - 1) {
            val current = route[i]
            val next = route[i + 1]

            val arrivalHour = (currentTimeMin / 60).toInt() % 24
            if (arrivalHour < current.openHour || arrivalHour >= current.closeHour) {
                totalTime += 1000.0
            } else if (current.closeHour - arrivalHour < 1) {
                totalTime += 60.0
            }

            val travel = travelTime(current, next)
            totalTime += travel
            currentTimeMin += travel
        }

        val last = route.last()
        val lastArrivalHour = (currentTimeMin / 60).toInt() % 24
        if (lastArrivalHour < last.openHour || lastArrivalHour >= last.closeHour) {
            totalTime += 1000.0
        } else if (last.closeHour - lastArrivalHour < 1) {
            totalTime += 60.0
        }

        return totalTime
    }
}