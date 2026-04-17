package com.example.myapplication

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class NeuralNetwork(context: Context) {

    private val inputSize = 50 * 50
    private val h1Size = 512
    private val h2Size = 256
    private val outputSize = 10

    private val w1: List<List<Double>>
    private val b1: List<Double>
    private val w2: List<List<Double>>
    private val b2: List<Double>
    private val w3: List<List<Double>>
    private val b3: List<Double>

    init {
        w1 = loadMatrix(context, R.raw.w1)
        b1 = loadVector(context, R.raw.b1)
        w2 = loadMatrix(context, R.raw.w2)
        b2 = loadVector(context, R.raw.b2)
        w3 = loadMatrix(context, R.raw.w3)
        b3 = loadVector(context, R.raw.b3)
    }

    private fun loadVector(context: Context, resourceId: Int): List<Double> {
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(resourceId)))
        val line = reader.readLine()
        reader.close()
        return line.split(",").map { it.trim().toDouble() }
    }

    private fun loadMatrix(context: Context, resourceId: Int): List<List<Double>> {
        val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(resourceId)))
        val matrix = mutableListOf<List<Double>>()
        reader.forEachLine { line ->
            matrix.add(line.split(",").map { it.trim().toDouble() })
        }
        reader.close()
        return matrix
    }

    private fun relu(x: Double): Double = if (x > 0) x else 0.0

    private fun softmax(inputs: List<Double>): List<Double> {
        val expValues = inputs.map { Math.exp(it) }
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }
    }

    fun predict(input: List<Double>): Int {
        if (input.size != inputSize) {
            throw IllegalArgumentException("Input size must be $inputSize")
        }

        val h1_input = DoubleArray(h1Size)
        for (i in 0 until inputSize) {
            for (j in 0 until h1Size) {
                h1_input[j] += input[i] * w1[i][j]
            }
        }
        val h1_output = h1_input.mapIndexed { index, value -> relu(value + b1[index]) }

        val h2_input = DoubleArray(h2Size)
        for (i in 0 until h1Size) {
            for (j in 0 until h2Size) {
                h2_input[j] += h1_output[i] * w2[i][j]
            }
        }
        val h2_output = h2_input.mapIndexed { index, value -> relu(value + b2[index]) }

        val output_input = DoubleArray(outputSize)
        for (i in 0 until h2Size) {
            for (j in 0 until outputSize) {
                output_input[j] += h2_output[i] * w3[i][j]
            }
        }
        val final_output = softmax(output_input.mapIndexed { index, value -> value + b3[index] })

        return final_output.indexOf(final_output.maxOrNull())
    }
}