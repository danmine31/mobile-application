package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.osmdroid.util.GeoPoint
import java.util.Locale
import androidx.compose.ui.platform.LocalContext

@Composable
fun RatingScreen(establishmentPoint: GeoPoint, onBack: () -> Unit) {
    val gridSize = 50
    val totalPixels = gridSize * gridSize

    val gridState = remember {
        mutableStateListOf<Boolean>().apply { repeat(totalPixels) { add(false) } }
    }

    var recognizedDigit by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val neuralNetwork = remember { NeuralNetwork(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Оценка заведения", style = MaterialTheme.typography.titleMedium)
        Text(
            text = String.format(Locale.US, "Lat: %.4f, Lon: %.4f", establishmentPoint.latitude, establishmentPoint.longitude),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Нарисуйте цифру (50x50)", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        updateGrid(change.position, size.width, gridSize, gridState)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { touchPos ->
                        updateGrid(touchPos, size.width, gridSize, gridState)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val pixelSize = canvasWidth / gridSize

                for (i in 0 until gridSize) {
                    for (j in 0 until gridSize) {
                        if (gridState[i * gridSize + j]) {
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

        Spacer(modifier = Modifier.height(20.dp))

        if (recognizedDigit != null) {
            Text(
                text = "Распознано: $recognizedDigit",
                fontSize = 28.sp,
                color = Color.Blue,
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                var minX = gridSize; var maxX = 0; var minY = gridSize; var maxY = 0
                var hasPixels = false

                for (y in 0 until gridSize) {
                    for (x in 0 until gridSize) {
                        if (gridState[y * gridSize + x]) {
                            if (x < minX) minX = x; if (x > maxX) maxX = x
                            if (y < minY) minY = y; if (y > maxY) maxY = y
                            hasPixels = true
                        }
                    }
                }

                if (hasPixels) {
                    val normalizedInput = DoubleArray(totalPixels) { 0.0 }
                    val width = maxX - minX + 1
                    val height = maxY - minY + 1

                    val offsetX = (gridSize - width) / 2
                    val offsetY = (gridSize - height) / 2

                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            if (gridState[(minY + y) * gridSize + (minX + x)]) {
                                val targetIdx = (offsetY + y) * gridSize + (offsetX + x)
                                if (targetIdx in 0 until totalPixels) normalizedInput[targetIdx] = 1.0
                            }
                        }
                    }
                    recognizedDigit = neuralNetwork.predict(normalizedInput.toList())
                }
            }) {
                Text("Распознать")
            }

            Button(onClick = {
                for (i in 0 until totalPixels) {
                    gridState[i] = false
                }
                recognizedDigit = null
            }) {
                Text("Очистить")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { onBack() }) {
            Text(if (recognizedDigit != null) "Сохранить и выйти" else "Назад к карте")
        }
    }
}

private fun updateGrid(touchPos: Offset, canvasWidth: Int, gridSize: Int, gridState: SnapshotStateList<Boolean>) {
    val pixelSize = canvasWidth.toFloat() / gridSize
    val col = (touchPos.x / pixelSize).toInt()
    val row = (touchPos.y / pixelSize).toInt()

    if (col in 0 until gridSize && row in 0 until gridSize) {
        val offsets = listOf(Pair(0,0), Pair(1,0), Pair(-1,0), Pair(0,1), Pair(0,-1))
        for (off in offsets) {
            val r = row + off.second
            val c = col + off.first
            if (r in 0 until gridSize && c in 0 until gridSize) {
                gridState[r * gridSize + c] = true
            }
        }
    }
}
