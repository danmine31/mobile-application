package com.example.pathfinding.algorithms

import kotlin.math.log2

data class DecisionNode(
    val featureName: String? = null,
    var value: String? = null,
    val result: String? = null,
    val children: MutableList<DecisionNode> = mutableListOf()
)

class DecisionTree {
    private var root: DecisionNode? = null

    fun build(data: List<Map<String, String>>, features: List<String>, target: String) {
        root = buildTree(data, features, target)
    }

    fun predict(instance: Map<String, String>): String? {
        return predict(root, instance)
    }

    fun printTree(): String {
        return buildString {
            printNode(root, 0, this)
        }
    }

    private fun buildTree(data: List<Map<String, String>>, features: List<String>, target: String): DecisionNode {
        val targetValues = data.map { it[target] }.distinct()
        if (targetValues.size == 1) {
            return DecisionNode(result = targetValues.first())
        }
        if (features.isEmpty()) {
            val majority = data.groupingBy { it[target] }.eachCount().maxByOrNull { it.value }?.key
            return DecisionNode(result = majority)
        }

        val bestFeature = selectBestFeature(data, features, target)
        val featureValues = data.mapNotNull { it[bestFeature] }.distinct()

        val node = DecisionNode(featureName = bestFeature)
        for (value in featureValues) {
            val subset = data.filter { it[bestFeature] == value }
            if (subset.isEmpty()) {
                val majority = data.groupingBy { it[target] }.eachCount().maxByOrNull { it.value }?.key
                node.children.add(DecisionNode(value = value, result = majority))
            } else {
                val remainingFeatures = features.filter { it != bestFeature }
                val childNode = buildTree(subset, remainingFeatures, target)
                childNode.value = value
                node.children.add(childNode)
            }
        }
        return node
    }

    private fun selectBestFeature(data: List<Map<String, String>>, features: List<String>, target: String): String {
        val baseEntropy = calculateEntropy(data.map { it[target] }.filterNotNull())
        var bestGain = 0.0
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

    private fun predict(node: DecisionNode?, instance: Map<String, String>): String? {
        if (node == null) return null
        if (node.result != null) return node.result

        val featureValue = instance[node.featureName]
        val child = node.children.find { it.value == featureValue }
        return if (child != null) predict(child, instance) else node.children.firstOrNull()?.let { predict(it, instance) }
    }

    private fun printNode(node: DecisionNode?, indent: Int, builder: StringBuilder) {
        if (node == null) return
        if (node.result != null) {
            builder.append("  ".repeat(indent))
            builder.append("-> ${node.result}\n")
            return
        }
        for (child in node.children) {
            builder.append("  ".repeat(indent))
            builder.append("${node.featureName} = ${child.value}\n")
            printNode(child, indent + 1, builder)
        }
    }
}