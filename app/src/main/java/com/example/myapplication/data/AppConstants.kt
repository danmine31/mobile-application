package com.example.myapplication.data

object AppConstants {
    const val MIN_LAT = 56.46382
    const val MAX_LAT = 56.47398
    const val MIN_LON = 84.93393
    const val MAX_LON = 84.95891

    const val CELL_SIZE_METERS = 3.0

    const val METERS_PER_LAT_DEGREE = 111132.0
    
    fun getMetersPerLonDegree(lat: Double): Double {
        return 111320.0 * Math.cos(Math.toRadians(lat))
    }

    const val OSM_FILE_NAME = "map.osm"

    const val DEFAULT_ZOOM = 17.0
    const val MIN_ZOOM_FOR_GRID = 16.0
    const val MARKER_SIZE_PX = 40
    const val ROUTE_WIDTH_PX = 10f
}