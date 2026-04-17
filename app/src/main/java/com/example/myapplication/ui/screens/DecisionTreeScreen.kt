package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
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
import com.example.myapplication.algorithms.PredictionResult
import com.example.myapplication.parseCsv
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import com.example.myapplication.algorithms.DecisionNode

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
    var predictionResult by remember { mutableStateOf<PredictionResult?>(null) }

    var location by remember { mutableStateOf("main_building") }
    var budget by remember { mutableStateOf("low") }
    var timeAvailable by remember { mutableStateOf("medium") }
    var foodType by remember { mutableStateOf("full_meal") }
    var queueTolerance by remember { mutableStateOf("medium") }
    var weather by remember { mutableStateOf("good") }

    val scrollState = rememberScrollState()
    var showVisualTree by remember { mutableStateOf(true) }

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

    fun pruneTree() {
        tree?.prune()
        val currentTree = tree
        tree = null
        tree = currentTree
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
        predictionResult = tree?.predictWithPath(instance)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌲 Дерево решений: Где обедаем?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📊 Обучающие CSV данные:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { buildTree() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Построить")
                    }
                    if (tree != null) {
                        Button(onClick = { pruneTree() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Сжать")
                        }
                    }
                }
            }
        }

        if (tree != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🌳 Визуализация:", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Вид:", style = MaterialTheme.typography.bodySmall)
                            Switch(
                                checked = showVisualTree,
                                onCheckedChange = { showVisualTree = it },
                                modifier = Modifier.scale(0.7f)
                            )
                            Text(if (showVisualTree) "График" else "Текст", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .background(Color.DarkGray.copy(alpha = 0.05f))
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        if (showVisualTree) {
                            VisualTreeCanvas(tree!!.root)
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                item {
                                    Text(
                                        text = tree!!.printTree(),
                                        style = androidx.compose.ui.text.TextStyle(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = MaterialTheme.typography.bodySmall.fontSize
                                        )
                                    )
                                }
                            }
                        }
                    }
                    if (showVisualTree) {
                        Text(
                            "💡 Используй два пальца для зума и перемещения", 
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎯 Параметры для выбора:", style = MaterialTheme.typography.titleMedium)
                    
                    ParameterSelector("Местоположение", location, listOf("main_building", "second_building", "campus_center")) { location = it }
                    ParameterSelector("Бюджет", budget, listOf("low", "medium", "high")) { budget = it }
                    ParameterSelector("Времени есть", timeAvailable, listOf("very_short", "short", "medium")) { timeAvailable = it }
                    ParameterSelector("Тип еды", foodType, listOf("coffee", "pancakes", "full_meal", "snack")) { foodType = it }
                    ParameterSelector("Очередь", queueTolerance, listOf("low", "medium", "high")) { queueTolerance = it }
                    ParameterSelector("Погода", weather, listOf("good", "bad")) { weather = it }

                    Button(
                        onClick = { predict() }, 
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Где мне поесть?")
                    }
                }
            }

            if (predictionResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🎉 РЕЗУЛЬТАТ:", style = MaterialTheme.typography.titleSmall)
                        Text(
                            predictionResult!!.result, 
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("📍 Путь по дереву:", style = MaterialTheme.typography.titleSmall)
                        predictionResult!!.path.forEach { step ->
                            Text(
                                step, 
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun VisualTreeCanvas(root: DecisionNode?) {
    if (root == null) return

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }

    val textMeasurer = rememberTextMeasurer()
    val nodeWidth = 140f
    val nodeHeight = 60f
    val verticalSpacing = 120f
    val horizontalSpacing = 20f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .transformable(state = state)
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
        ) {
            val treeWidth = calculateTreeWidth(root) * (nodeWidth + horizontalSpacing)
            drawNodeRecursive(
                node = root,
                x = size.width / 2,
                y = 50f,
                width = treeWidth,
                textMeasurer = textMeasurer,
                nodeWidth = nodeWidth,
                nodeHeight = nodeHeight,
                verticalSpacing = verticalSpacing,
                horizontalSpacing = horizontalSpacing
            )
        }
    }
}

private fun calculateTreeWidth(node: DecisionNode): Int {
    if (node.children.isEmpty()) return 1
    return node.children.sumOf { calculateTreeWidth(it) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNodeRecursive(
    node: DecisionNode,
    x: Float,
    y: Float,
    width: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    nodeWidth: Float,
    nodeHeight: Float,
    verticalSpacing: Float,
    horizontalSpacing: Float
) {
    val isLeaf = node.result != null
    val boxColor = if (isLeaf) Color(0xFFC8E6C9) else Color(0xFFBBDEFB)
    val borderColor = if (isLeaf) Color(0xFF4CAF50) else Color(0xFF2196F3)

    drawRoundRect(
        color = boxColor,
        topLeft = Offset(x - nodeWidth / 2, y),
        size = androidx.compose.ui.geometry.Size(nodeWidth, nodeHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(x - nodeWidth / 2, y),
        size = androidx.compose.ui.geometry.Size(nodeWidth, nodeHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
        style = Stroke(width = 2f)
    )

    val label = if (isLeaf) node.result!! else node.featureName!!
    val textLayoutResult = textMeasurer.measure(
        text = label,
        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
    )
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(x - textLayoutResult.size.width / 2, y + nodeHeight / 2 - textLayoutResult.size.height / 2)
    )

    if (node.children.isNotEmpty()) {
        var currentX = x - width / 2
        for (child in node.children) {
            val childWidth = calculateTreeWidth(child) * (nodeWidth + horizontalSpacing)
            val childX = currentX + childWidth / 2
            val childY = y + verticalSpacing

            drawLine(
                color = Color.Gray,
                start = Offset(x, y + nodeHeight),
                end = Offset(childX, childY),
                strokeWidth = 2f
            )

            val valueLabel = child.value ?: ""
            val valueLayout = textMeasurer.measure(
                text = valueLabel,
                style = TextStyle(fontSize = 8.sp, color = Color.DarkGray)
            )
            drawText(
                textLayoutResult = valueLayout,
                topLeft = Offset((x + childX) / 2 - valueLayout.size.width / 2, (y + nodeHeight + childY) / 2 - valueLayout.size.height / 2)
            )

            drawNodeRecursive(child, childX, childY, childWidth, textMeasurer, nodeWidth, nodeHeight, verticalSpacing, horizontalSpacing)
            currentX += childWidth
        }
    }
}

@Composable
fun ParameterSelector(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option, fontSize = MaterialTheme.typography.bodySmall.fontSize) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}