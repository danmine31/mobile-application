package com.example.myapplication.algorithms

import kotlin.math.min
import kotlin.random.Random

data class Point(val x: Double, val y: Double)
data class KMeansResult(val centroids: List<Point>, val labels: List<Int>)

enum class DistanceMetric { EUCLIDEAN, MANHATTAN }

fun kMeans(
    points: List<Point>,
    k: Int,
    maxIter: Int = 100,
    metric: DistanceMetric = DistanceMetric.EUCLIDEAN
): KMeansResult {
    if (points.isEmpty()) return KMeansResult(emptyList(), emptyList())

    var centroids = points.shuffled().take(k)
    var labels = MutableList(points.size) { -1 }

    repeat(maxIter) {
        val newLabels = points.map { point ->
            centroids.indices.minByOrNull { i ->
                distance(point, centroids[i], metric)
            } ?: 0
        }

        val changed = newLabels != labels
        labels = newLabels.toMutableList()

        if (!changed) return@repeat

        centroids = centroids.indices.map { i ->
            val clusterPoints = points.filterIndexed { index, _ -> labels[index] == i }
            if (clusterPoints.isEmpty()) {
                centroids[i]
            } else {
                Point(
                    clusterPoints.map { it.x }.average(),
                    clusterPoints.map { it.y }.average()
                )
            }
        }
    }

    return KMeansResult(centroids, labels)
}

private fun distance(p1: Point, p2: Point, metric: DistanceMetric): Double {
    return when (metric) {
        DistanceMetric.EUCLIDEAN -> Math.sqrt(
            Math.pow(p1.x - p2.x, 2.0) + Math.pow(
                p1.y - p2.y,
                2.0
            )
        )

        DistanceMetric.MANHATTAN -> Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y)
    }
}
