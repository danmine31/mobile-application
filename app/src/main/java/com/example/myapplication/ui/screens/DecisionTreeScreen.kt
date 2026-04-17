package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.algorithms.DecisionTree

@Composable
fun DecisionTreeScreen() {
    val defaultCsv = """location,budget,time_available,food_type,queue_tolerance,weather,recommended_place
main_building,low,medium,full_meal,medium,good,Main_Cafeteria
main_building,low,short,snack,low,good,Yarche
main_building,medium,short,coffee,low,good,Bus_Stop_Coffee
main_building,high,medium,coffee,medium,good,Starbooks
second_building,low,very_short,snack,low,good,Vending_Machine
second_building,medium,short,coffee,medium,good,Second_Building_Cafe
second_building,medium,medium,full_meal,medium,good,Main_Cafeteria
second_building,low,short,snack,low,bad,Vending_Machine
campus_center,medium,short,pancakes,medium,good,Siberian_Pancakes"""

    var csvText by remember { mutableStateOf(defaultCsv) }
    var tree by remember { mutableStateOf<DecisionTree?>(null) }
    var predictionResult by remember { mutableStateOf<String?>(null) }

    var location by remember { mutableStateOf("main_building") }
    var budget by remember { mutableStateOf("low") }
    var timeAvailable by remember { mutableStateOf("medium") }
    var foodType by remember { mutableStateOf("full_meal") }
    var queueTolerance by remember { mutableStateOf("medium") }
    var weather by remember { mutableStateOf("good") }

    fun buildTree() {
        val (headers, data) = parseCsv(csvText)
        if (data.isEmpty()) return
        val features = headers.filter { it != "recommended_place" }
        val target = "recommended_place"
        val decisionTree = DecisionTree()
        decisionTree.build(data, features, target)
        tree = decisionTree
        predictionResult = null
    }

    fun predict() {
        val instance = mapOf(
            "location" to location,
            "budget" to budget,
            "time_available" to timeAvailable,
            "food_type" to foodType,
            "queue_tolerance" to queueTolerance,
            "weather" to weather
        )
        predictionResult = tree?.predict(instance)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Дерево решений", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(8.dp))

        Text("CSV данные:", style = MaterialTheme.typography.titleMedium)
        BasicTextField(
            value = csvText,
            onValueChange = { csvText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        )

        Button(onClick = { buildTree() }, modifier = Modifier.padding(8.dp)) {
            Text("Построить дерево")
        }

        if (tree != null) {
            Text("Дерево решений:", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.height(200.dp)) {
                item {
                    Text(
                        text = tree!!.printTree(),
                        style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Введите параметры для предсказания:", style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column {
                    Text("location")
                    listOf("main_building", "second_building", "campus_center").forEach { value ->
                        Row {
                            RadioButton(selected = location == value, onClick = { location = value })
                            Text(value)
                        }
                    }
                }
                Column {
                    Text("budget")
                    listOf("low", "medium", "high").forEach { value ->
                        Row {
                            RadioButton(selected = budget == value, onClick = { budget = value })
                            Text(value)
                        }
                    }
                }
                Column {
                    Text("time_available")
                    listOf("very_short", "short", "medium").forEach { value ->
                        Row {
                            RadioButton(selected = timeAvailable == value, onClick = { timeAvailable = value })
                            Text(value)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column {
                    Text("food_type")
                    listOf("coffee", "pancakes", "full_meal", "snack").forEach { value ->
                        Row {
                            RadioButton(selected = foodType == value, onClick = { foodType = value })
                            Text(value)
                        }
                    }
                }
                Column {
                    Text("queue_tolerance")
                    listOf("low", "medium", "high").forEach { value ->
                        Row {
                            RadioButton(selected = queueTolerance == value, onClick = { queueTolerance = value })
                            Text(value)
                        }
                    }
                }
                Column {
                    Text("weather")
                    listOf("good", "bad").forEach { value ->
                        Row {
                            RadioButton(selected = weather == value, onClick = { weather = value })
                            Text(value)
                        }
                    }
                }
            }

            Button(onClick = { predict() }, modifier = Modifier.padding(8.dp)) {
                Text("Предсказать")
            }

            if (predictionResult != null) {
                Text("Рекомендуемое заведение: $predictionResult", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}