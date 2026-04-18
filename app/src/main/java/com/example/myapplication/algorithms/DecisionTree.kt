package com.example.myapplication.algorithms

import kotlin.math.log2

data class DecisionNode(
    val featureName: String? = null,
    var value: String? = null,
    val result: String? = null,
    val children: MutableList<DecisionNode> = mutableListOf(),
    var sampleCount: Int = 0
)

data class PredictionResult(
    val result: String,
    val path: List<String>
)

class DecisionTree {
    var root: DecisionNode? = null
        private set

    fun build(data: List<Map<String, String>>, features: List<String>, target: String) {
        root = buildTree(data, features, target)
    }

    fun predictWithPath(instance: Map<String, String>): PredictionResult? {
        val path = mutableListOf<String>()
        val result = predict(root, instance, path)
        return if (result != null) PredictionResult(result, path) else null
    }

    fun predict(instance: Map<String, String>): String? {
        return predict(root, instance, mutableListOf())
    }

    fun prune() {
        root = pruneTree(root)
    }

    private fun pruneTree(node: DecisionNode?): DecisionNode? {
        if (node == null) return null
        if (node.result != null) return node

        for (i in node.children.indices) {
            node.children[i] = pruneTree(node.children[i])!!
        }

        if (node.children.all { it.result != null }) {
            val results = node.children.map { it.result }.distinct()
            if (results.size == 1) {
                return DecisionNode(result = results.first(), sampleCount = node.sampleCount)
            }
        }

        return node
    }

    private fun buildTree(
        data: List<Map<String, String>>,
        features: List<String>,
        target: String
    ): DecisionNode {
        val targetValues = data.map { it[target] }.distinct()
        val sampleCount = data.size

        if (targetValues.size == 1) {
            return DecisionNode(result = targetValues.first(), sampleCount = sampleCount)
        }
        if (features.isEmpty()) {
            val majority = data.groupingBy { it[target] }.eachCount().maxByOrNull { it.value }?.key
            return DecisionNode(result = majority, sampleCount = sampleCount)
        }

        val bestFeature = selectBestFeature(data, features, target)
        val featureValues = data.mapNotNull { it[bestFeature] }.distinct()

        val node = DecisionNode(featureName = bestFeature, sampleCount = sampleCount)
        for (value in featureValues) {
            val subset = data.filter { it[bestFeature] == value }
            if (subset.isEmpty()) {
                val majority =
                    data.groupingBy { it[target] }.eachCount().maxByOrNull { it.value }?.key
                node.children.add(DecisionNode(value = value, result = majority, sampleCount = 0))
            } else {
                val remainingFeatures = features.filter { it != bestFeature }
                val childNode = buildTree(subset, remainingFeatures, target)
                childNode.value = value
                node.children.add(childNode)
            }
        }
        return node
    }

    private fun selectBestFeature(
        data: List<Map<String, String>>,
        features: List<String>,
        target: String
    ): String {
        val baseEntropy = calculateEntropy(data.map { it[target] }.filterNotNull())
        var bestGain = -1.0
        var bestFeature = features.first()

        for (feature in features) {
            val values = data.mapNotNull { it[feature] }.distinct()
            var conditionalEntropy = 0.0
            for (value in values) {
                val subset = data.filter { it[feature] == value }
                val subsetTargets = subset.mapNotNull { it[target] }
                val weight = subset.size.toDouble() / data.size
                conditionalEntropy += weight * calculateEntropy(subsetTargets)
            }
            val gain = baseEntropy - conditionalEntropy
            if (gain > bestGain) {
                bestGain = gain
                bestFeature = feature
            }
        }
        return bestFeature
    }

    private fun calculateEntropy(values: List<String>): Double {
        if (values.isEmpty()) return 0.0
        val counts = values.groupingBy { it }.eachCount()
        var entropy = 0.0
        for (count in counts.values) {
            val probability = count.toDouble() / values.size
            entropy -= probability * log2(probability)
        }
        return entropy
    }

    private fun predict(
        node: DecisionNode?,
        instance: Map<String, String>,
        path: MutableList<String>
    ): String? {
        if (node == null) return null
        if (node.result != null) {
            path.add("-> Рекомендация: ${node.result}")
            return node.result
        }

        val featureValue = instance[node.featureName]
        path.add("Проверка ${node.featureName} = $featureValue")

        val child = node.children.find { it.value == featureValue }
        return if (child != null) {
            predict(child, instance, path)
        } else {
            val fallback =
                node.children.maxByOrNull { it.sampleCount } ?: node.children.firstOrNull()
            if (fallback != null) {
                path.add("(Значение не найдено в обучении, пойдём по наиболее вероятному пути: ${fallback.value})")
                predict(fallback, instance, path)
            } else null
        }
    }

    fun printTree(): String {
        return buildString {
            printNode(root, 0, this)
        }
    }

    private fun printNode(node: DecisionNode?, indent: Int, builder: StringBuilder) {
        if (node == null) return
        if (node.result != null) {
            builder.append("  ".repeat(indent))
            builder.append("|__ 🏁 РЕЗУЛЬТАТИК: ${node.result}\n")
            return
        }

        builder.append("  ".repeat(indent))
        builder.append("❓ ${node.featureName}?\n")

        for (child in node.children) {
            builder.append("  ".repeat(indent + 1))
            builder.append("|-- ${child.value}\n")
            printNode(child, indent + 2, builder)
        }
    }
}