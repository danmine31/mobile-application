package com.example.myapplication.data

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import com.example.myapplication.data.AppConstants as Config
import org.json.JSONObject
import java.io.File

class MapGridGenerator(private val context: Context) {

    private data class TempPoint(val lat: Double, val lon: Double)

    fun generateFullGrid(): GridMap {
        val nodes = mutableMapOf<Long, TempPoint>()
        val obstacles = mutableListOf<List<TempPoint>>()
        val walkableSurfaces = mutableListOf<List<TempPoint>>()


        val inputStream = context.assets.open(Config.OSM_FILE_NAME)
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var currentWayNodes = mutableListOf<Long>()
        var isObstacle = false
        var isWalkableSurface = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "node" -> {
                            val id = parser.getAttributeValue(null, "id").toLong()
                            val lat = parser.getAttributeValue(null, "lat").toDouble()
                            val lon = parser.getAttributeValue(null, "lon").toDouble()
                            nodes[id] = TempPoint(lat, lon)
                        }
                        "way" -> {
                            currentWayNodes.clear()
                            isObstacle = false
                            isWalkableSurface = false
                        }
                        "nd" -> {
                            val ref = parser.getAttributeValue(null, "ref").toLong()
                            currentWayNodes.add(ref)
                        }
                        "tag" -> {
                            val k = parser.getAttributeValue(null, "k")
                            val v = parser.getAttributeValue(null, "v")

                            if (k == "highway") {
                                when (v) {
                                    "footway", "path", "pedestrian", "service", "steps", 
                                    "living_street", "track", "residential", "unclassified", 
                                    "sidewalk", "corridor", "platform", "cycleway", "road",
                                    "primary", "secondary", "tertiary", "trunk", "motorway",
                                    "primary_link", "secondary_link", "tertiary_link" -> {
                                        isWalkableSurface = true
                                    }
                                }
                            }

                            if (k == "building" || k == "barrier" || v == "water" || (k == "natural" && v == "water") || k == "amenity" && v == "parking") {
                                isObstacle = true
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "way") {
                        val pts = currentWayNodes.mapNotNull { nodes[it] }
                        if (pts.isNotEmpty()) {
                            if (isObstacle) {
                                obstacles.add(pts)
                            } else if (isWalkableSurface) {
                                walkableSurfaces.add(pts)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        val latDiff = Config.MAX_LAT - Config.MIN_LAT
        val lonDiff = Config.MAX_LON - Config.MIN_LON
        val avgLat = (Config.MAX_LAT + Config.MIN_LAT) / 2

        val gridHeight = (latDiff * Config.METERS_PER_LAT_DEGREE / Config.CELL_SIZE_METERS).toInt()
        val gridWidth = (lonDiff * Config.getMetersPerLonDegree(avgLat) / Config.CELL_SIZE_METERS).toInt()

        val walkable = Array(gridWidth) { BooleanArray(gridHeight) { false } }

        val latStep = latDiff / gridHeight
        val lonStep = lonDiff / gridWidth

        for (points in walkableSurfaces) {
            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val minLon = points.minOf { it.lon }
            val maxLon = points.maxOf { it.lon }

            val minI = (((minLat - Config.MIN_LAT) / latDiff) * gridHeight).toInt().coerceIn(0, gridHeight - 1)
            val maxI = (((maxLat - Config.MIN_LAT) / latDiff) * gridHeight).toInt().coerceIn(0, gridHeight - 1)
            val minJ = (((minLon - Config.MIN_LON) / lonDiff) * gridWidth).toInt().coerceIn(0, gridWidth - 1)
            val maxJ = (((maxLon - Config.MIN_LON) / lonDiff) * gridWidth).toInt().coerceIn(0, gridWidth - 1)

            for (i in minI..maxI) {
                for (j in minJ..maxJ) {
                    val cellLat = Config.MIN_LAT + i * latStep + (latStep / 2)
                    val cellLon = Config.MIN_LON + j * lonStep + (lonStep / 2)

                    if (isNearLine(points, cellLat, cellLon)) {
                        walkable[j][i] = true
                    }
                }
            }
        }

        for (points in obstacles) {
            val minLat = points.minOf { it.lat }
            val maxLat = points.maxOf { it.lat }
            val minLon = points.minOf { it.lon }
            val maxLon = points.maxOf { it.lon }

            val minI = (((minLat - Config.MIN_LAT) / latDiff) * gridHeight).toInt().coerceIn(0, gridHeight - 1)
            val maxI = (((maxLat - Config.MIN_LAT) / latDiff) * gridHeight).toInt().coerceIn(0, gridHeight - 1)
            val minJ = (((minLon - Config.MIN_LON) / lonDiff) * gridWidth).toInt().coerceIn(0, gridWidth - 1)
            val maxJ = (((maxLon - Config.MIN_LON) / lonDiff) * gridWidth).toInt().coerceIn(0, gridWidth - 1)

            for (i in minI..maxI) {
                for (j in minJ..maxJ) {

                    if (walkable[j][i]) continue

                    val cellLat = Config.MIN_LAT + i * latStep + (latStep / 2)
                    val cellLon = Config.MIN_LON + j * lonStep + (lonStep / 2)

                    if (containsPoint(points, cellLat, cellLon)) {
                        walkable[j][i] = false
                    }
                }
            }
        }

        return GridMap(gridWidth, gridHeight, walkable)
    }

    private fun isNearLine(line: List<TempPoint>, lat: Double, lon: Double): Boolean {
        val threshold = (Config.CELL_SIZE_METERS * 1.0) / Config.METERS_PER_LAT_DEGREE
        for (k in 0 until line.size - 1) {
            val p1 = line[k]
            val p2 = line[k + 1]
            if (distanceToSegment(lat, lon,
                    p1.lat, p1.lon,
                    p2.lat, p2.lon) < threshold) {
                return true
            }
        }
        return false
    }

    private fun distanceToSegment(x: Double, y: Double,
                                  x1: Double, y1: Double,
                                  x2: Double, y2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0.0 && dy == 0.0) return Math.hypot(x - x1, y - y1)
        val t = ((x - x1) * dx + (y - y1) * dy) / (dx * dx + dy * dy)
        return when {
            t < 0 -> Math.hypot(x - x1, y - y1)
            t > 1 -> Math.hypot(x - x2, y - y2)
            else -> Math.hypot(x - (x1 + t * dx), y - (y1 + t * dy))
        }
    }

    private fun containsPoint(polygon: List<TempPoint>, lat: Double, lon: Double): Boolean {
        var isInside = false
        var j = polygon.size - 1
        for (i in polygon.indices) {
            if (((polygon[i].lon > lon) != (polygon[j].lon > lon)) &&
                (lat < (polygon[j].lat - polygon[i].lat) * (lon - polygon[i].lon)
                        / (polygon[j].lon - polygon[i].lon) + polygon[i].lat)
            ) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }

    fun saveGridToJson(gridMap: GridMap, fileName: String) {
        try {
            val json = JSONObject()
            json.put("width", gridMap.width)
            json.put("height", gridMap.height)

            val sb = StringBuilder(gridMap.width * gridMap.height)
            for (i in 0 until gridMap.height) {
                for (j in 0 until gridMap.width) {
                    sb.append(if (gridMap.walkable[j][i]) '1' else '0')
                }
            }
            json.put("data_string", sb.toString())

            val file = File(context.filesDir, fileName)
            file.writeText(json.toString())
        } catch (e: Exception) {}
    }
}