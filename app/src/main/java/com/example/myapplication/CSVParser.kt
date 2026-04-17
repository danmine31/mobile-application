package com.example.myapplication

fun parseCsv(csvText: String): Pair<List<String>, List<Map<String, String>>> {
    val lines = csvText.trim().split("\n")
    if (lines.isEmpty()) return Pair(emptyList(), emptyList())
    val headers = lines[0].split(",").map { it.trim() }
    val data = lines.drop(1).map { line ->
        val values = line.split(",").map { it.trim() }
        headers.zip(values).toMap()
    }
    return Pair(headers, data)
}