package com.example.myapplication.data

import android.content.Context
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import com.example.myapplication.data.AppConstants as Config
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class MapGridGenerator(private val context: Context) {

    private data class TempPoint(val lat: Double, val lon: Double)
    private val uniqueTags = mutableSetOf<String>()

    fun generateFullGrid(): GridMap {
        val nodes = mutableMapOf<Long, TempPoint>()
        val obstacles = mutableListOf<List<TempPoint>>()


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
                        }
                        "nd" -> {
                            val ref = parser.getAttributeValue(null, "ref").toLong()
                            currentWayNodes.add(ref)
                        }
                        "tag" -> {
                            val k = parser.getAttributeValue(null, "k")
                            val v = parser.getAttributeValue(null, "v")

                            if (k == "highway" && (v == "footway" || v == "path" || v == "pedestrian" || v == "service" || v == "steps")) {
                                isWalkableSurface = true
                            }

                            if (k == "building" || k == "barrier" || v == "water") {
                                isObstacle = true
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "way" && isObstacle) {
                        val pts = currentWayNodes.mapNotNull { nodes[it] }
                        if (pts.isNotEmpty()) obstacles.add(pts)
                    }
                }
            }
            eventType = parser.next()
        }

        val latDiff = Config.MAX_LAT - Config.MIN_LAT
        val lonDiff = Config.MAX_LON - Config.MIN_LON

        val gridHeight = (latDiff * Config.METERS_PER_LAT_DEGREE / Config.CELL_SIZE_METERS).toInt()
        val gridWidth = (lonDiff * Config.METERS_PER_LON_DEGREE / Config.CELL_SIZE_METERS).toInt()

        val walkable = Array(gridHeight) { BooleanArray(gridWidth) { true } }

        val latStep = latDiff / gridHeight
        val lonStep = lonDiff / gridWidth

        for (points in obstacles) {
            val minI = (((points.minOf { it.lat } - Config.MIN_LAT) / latDiff) * gridHeight).toInt().coerceIn(0, gridHeight - 1)
            val maxI = (((points.maxOf { it.lat } - Config.MIN_LAT) / latDiff) * gridHeight).toInt().coerceIn(0, gridHeight - 1)
            val minJ = (((points.minOf { it.lon } - Config.MIN_LON) / lonDiff) * gridWidth).toInt().coerceIn(0, gridWidth - 1)
            val maxJ = (((points.maxOf { it.lon } - Config.MIN_LON) / lonDiff) * gridWidth).toInt().coerceIn(0, gridWidth - 1)

            for (i in minI..maxI) {
                for (j in minJ..maxJ) {
                    val cellLat = Config.MIN_LAT + i * latStep + (latStep / 2)
                    val cellLon = Config.MIN_LON + j * lonStep + (lonStep / 2)

                    if (containsPoint(points, cellLat, cellLon)) {
                        walkable[i][j] = false
                    }
                }
            }
        }

        return GridMap(gridWidth, gridHeight, walkable)
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
        val json = JSONObject()
        json.put("width", gridMap.width)
        json.put("height", gridMap.height)

        val dataArray = JSONArray()
        for (i in 0 until gridMap.height) {
            for (j in 0 until gridMap.width) {
                dataArray.put(if (gridMap.walkable[i][j]) 1 else 0)
            }
        }
        json.put("data", dataArray)


        val file = File(context.filesDir, fileName)
        file.writeText(json.toString())
    }

}