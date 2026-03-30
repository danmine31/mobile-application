package com.example.myapplication.data

data class GeoPoint(val lat: Double, val lon: Double)

data class Obstacle(val points: List<GeoPoint>)


class MapGridGenerator {
}