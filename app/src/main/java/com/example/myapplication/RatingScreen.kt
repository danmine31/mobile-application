package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.TSU_DarkBlue
import com.example.myapplication.ui.theme.TSU_LightBlue
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@Composable
fun RatingScreen(establishmentName: String, establishmentPoint: GeoPoint, onBack: () -> Unit) {
    val gridSize = 50
    val totalPixels = gridSize * gridSize
    
    var gridData by remember { mutableStateOf(BooleanArray(totalPixels) { false }) }
    var drawTrigger by remember { mutableStateOf(0) }

    var recognizedDigit by remember { mutableStateOf<Int?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isNNLoading by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val neuralNetwork = remember { NeuralNetwork(context) }

    LaunchedEffect(Unit) {
        neuralNetwork.loadWeights()
        isNNLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (isNNLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = TSU_LightBlue)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Загрузка нейросети...", color = TSU_DarkBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Оценка заведения",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TSU_DarkBlue,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = establishmentName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TSU_LightBlue
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(2.dp, TSU_LightBlue, RoundedCornerShape(16.dp))
                        .pointerInput(gridSize) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                if (updateGridArray(change.position, size.width, gridSize, gridData)) {
                                    drawTrigger++
                                }
                            }
                        }
                        .pointerInput(gridSize) {
                            detectTapGestures { touchPos ->
                                if (updateGridArray(touchPos, size.width, gridSize, gridData)) {
                                    drawTrigger++
                                }
                            }
                        }
                ) {
                    key(drawTrigger) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val pixelSize = size.width / gridSize
                            for (i in 0 until gridSize) {
                                for (j in 0 until gridSize) {
                                    if (gridData[i * gridSize + j]) {
                                        drawRect(
                                            color = Color.White,
                                            topLeft = Offset(j * pixelSize, i * pixelSize),
                                            size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isProcessing) {
                    CircularProgressIndicator(color = TSU_LightBlue)
                } else if (recognizedDigit != null) {
                    Surface(
                        color = TSU_LightBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Ваша оценка: $recognizedDigit",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TSU_DarkBlue
                        )
                    }
                } else {
                    Text("Нарисуйте цифру от 0 до 9", color = Color.Gray)
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            gridData = BooleanArray(totalPixels) { false }
                            drawTrigger++
                            recognizedDigit = null
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TSU_LightBlue)
                    ) {
                        Text("Очистить")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                val input = withContext(Dispatchers.Default) {
                                    processGridArrayForNN(gridData, gridSize)
                                }
                                if (input != null) {
                                    recognizedDigit = withContext(Dispatchers.Default) {
                                        neuralNetwork.predict(input.toList())
                                    }
                                }
                                isProcessing = false
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TSU_LightBlue)
                    ) {
                        Text("Распознать")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onBack() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TSU_DarkBlue)
                ) {
                    Text(if (recognizedDigit != null) "Сохранить и выйти" else "Назад")
                }
            }
        }
    }
}

private fun updateGridArray(touchPos: Offset, canvasWidth: Int, gridSize: Int, gridData: BooleanArray): Boolean {
    val pixelSize = canvasWidth.toFloat() / gridSize
    val col = (touchPos.x / pixelSize).toInt()
    val row = (touchPos.y / pixelSize).toInt()
    var changed = false

    if (col in 0 until gridSize && row in 0 until gridSize) {
        val offsets = listOf(Pair(0,0), Pair(1,0), Pair(-1,0), Pair(0,1), Pair(0,-1))
        for (off in offsets) {
            val r = row + off.second
            val c = col + off.first
            if (r in 0 until gridSize && c in 0 until gridSize) {
                if (!gridData[r * gridSize + c]) {
                    gridData[r * gridSize + c] = true
                    changed = true
                }
            }
        }
    }
    return changed
}

private fun processGridArrayForNN(gridData: BooleanArray, gridSize: Int): DoubleArray? {
    var minX = gridSize; var maxX = 0; var minY = gridSize; var maxY = 0
    var hasPixels = false
    for (y in 0 until gridSize) {
        for (x in 0 until gridSize) {
            if (gridData[y * gridSize + x]) {
                if (x < minX) minX = x; if (x > maxX) maxX = x
                if (y < minY) minY = y; if (y > maxY) maxY = y
                hasPixels = true
            }
        }
    }
    if (!hasPixels) return null

    val normalizedInput = DoubleArray(gridSize * gridSize) { 0.0 }
    val width = maxX - minX + 1
    val height = maxY - minY + 1
    val offsetX = (gridSize - width) / 2
    val offsetY = (gridSize - height) / 2

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (gridData[(minY + y) * gridSize + (minX + x)]) {
                val targetIdx = (offsetY + y) * gridSize + (offsetX + x)
                if (targetIdx in 0 until gridSize * gridSize) normalizedInput[targetIdx] = 1.0
            }
        }
    }
    return normalizedInput
}
