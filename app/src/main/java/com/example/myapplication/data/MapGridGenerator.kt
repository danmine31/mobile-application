package com.example.myapplication.data

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import com.example.myapplication.data.AppConstants as Config

data class GeoPoint(val lat: Double, val lon: Double)

data class Obstacle(val points: List<GeoPoint>)

class MapGridGenerator(private val context: Context) {

    fun generateFullGrid(): Array<IntArray> {
        val obstacles = parseObstaclesFromOsm()

        val latDiff = Config.MAX_LAT - Config.MIN_LAT
        val lonDiff = Config.MAX_LON - Config.MIN_LON
        val heightInMeters = latDiff * Config.METERS_PER_LAT_DEGREE
        val widthInMeters = lonDiff * Config.METERS_PER_LON_DEGREE

        val gridHeight = (heightInMeters / Config.CELL_SIZE_METERS).toInt()
        val gridWidth = (widthInMeters / Config.CELL_SIZE_METERS).toInt()

        val grid = Array(gridHeight) { IntArray(gridWidth) { 0 } }

        val latStep = latDiff / gridHeight
        val lonStep = lonDiff / gridWidth

        for (obstacle in obstacles) {
            val minI = (((obstacle.points.minOf { it.lat } - Config.MIN_LAT) /
                    latDiff) * gridHeight).toInt().coerceIn(0, gridHeight - 1)
            val maxI = (((obstacle.points.maxOf { it.lat } - Config.MIN_LAT) /
                    latDiff) * gridHeight).toInt().coerceIn(0, gridHeight - 1)
            val minJ = (((obstacle.points.minOf { it.lon } - Config.MIN_LON) /
                    lonDiff) * gridWidth).toInt().coerceIn(0, gridWidth - 1)
            val maxJ = (((obstacle.points.maxOf { it.lon } - Config.MIN_LON) /
                    lonDiff) * gridWidth).toInt().coerceIn(0, gridWidth - 1)

            for (i in minI..maxI) {
                for (j in minJ..maxJ) {
                    val cellLat = Config.MIN_LAT + i * latStep
                    val cellLon = Config.MIN_LON + j * lonStep

                    if (containsPoint(obstacle.points, GeoPoint(cellLat, cellLon))) {
                        grid[i][j] = 1
                    }
                }
            }
        }
        return grid
    }

    private fun parseObstaclesFromOsm(): List<Obstacle> {
        val obstacles = mutableListOf<Obstacle>()
        val nodes = mutableMapOf<Long, GeoPoint>()
        val inputStream = context.assets.open(Config.OSM_FILE_NAME)
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        val currentWayNodes = mutableListOf<Long>()
        var isObstacle = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (name) {
                        "node" -> {
                            val id = parser.getAttributeValue(null, "id").toLong()
                            val lat = parser.getAttributeValue(null, "lat").toDouble()
                            val lon = parser.getAttributeValue(null, "lon").toDouble()
                            nodes[id] = GeoPoint(lat, lon)
                        }

                        "way" -> {
                            currentWayNodes.clear()
                            isObstacle = false
                        }

                        "nd" -> {
                            val ref = parser.getAttributeValue(null, "ref").toLong()
                            currentWayNodes.add(ref)
                        }

                        "tag" -> {
                            val k = parser.getAttributeValue(null, "k")
                            val v = parser.getAttributeValue(null, "v")

                            if (k == "building" ||
                                (k == "landuse" && (v == "construction" || v == "grass" || v == "meadow")) ||
                                (k == "natural" && (v == "water" || v == "scrub" || v == "wood")) ||
                                (k == "leisure" && (v == "park" || v == "garden"))
                            ) {
                                isObstacle = true
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (name == "way" && isObstacle) {
                        val points = currentWayNodes.mapNotNull { nodes[it] }
                        if (points.isNotEmpty()) obstacles.add(Obstacle(points))
                    }
                }
            }
            eventType = parser.next()
        }
        return obstacles
    }

    private fun containsPoint(polygon: List<GeoPoint>, point: GeoPoint): Boolean {
        var isInside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            if (((polygon[i].lon > point.lon) != (polygon[j].lon > point.lon)) &&
                (point.lat < (polygon[j].lat - polygon[i].lat) * (point.lon - polygon[i].lon)
                        / (polygon[j].lon - polygon[i].lon) + polygon[i].lat)
            ) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }
}