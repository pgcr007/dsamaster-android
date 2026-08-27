package com.dsamaster.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

@Composable
fun TopicDiagram(diagramType: String, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface

    when (diagramType) {
        "array" -> Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
            drawArrayDiagram(primaryColor, surfaceColor)
        }
        "linkedlist" -> Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
            drawLinkedListDiagram(primaryColor, surfaceColor)
        }
        "binarysearch" -> Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
            drawBinarySearchDiagram(primaryColor, surfaceColor)
        }
        "tree" -> Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
            drawTreeDiagram(primaryColor, surfaceColor)
        }
        "stack" -> Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
            drawStackDiagram(primaryColor, surfaceColor)
        }
        "queue" -> Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
            drawQueueDiagram(primaryColor, surfaceColor)
        }
        "heap" -> Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
            drawHeapDiagram(primaryColor, surfaceColor)
        }
        "graph" -> Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
            drawGraphDiagram(primaryColor, surfaceColor)
        }
        "trie" -> Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
            drawTrieDiagram(primaryColor, surfaceColor)
        }
        // "none" or any unrecognized type: render nothing
        // "none" or any unrecognized type: render nothing
    }
}

private fun DrawScope.drawArrayDiagram(primary: androidx.compose.ui.graphics.Color, surface: androidx.compose.ui.graphics.Color) {
    val boxCount = 6
    val spacing = 8f
    val boxSize = (size.width - spacing * (boxCount - 1)) / boxCount
    for (i in 0 until boxCount) {
        val x = i * (boxSize + spacing)
        drawRoundRect(
            color = primary.copy(alpha = 0.25f + (i % 3) * 0.15f),
            topLeft = Offset(x, 0f),
            size = androidx.compose.ui.geometry.Size(boxSize, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
    }
}

private fun DrawScope.drawLinkedListDiagram(primary: androidx.compose.ui.graphics.Color, surface: androidx.compose.ui.graphics.Color) {
    val nodeCount = 4
    val nodeSize = 50f
    val gap = (size.width - nodeSize * nodeCount) / (nodeCount - 1)
    val centerY = size.height / 2

    for (i in 0 until nodeCount) {
        val x = i * (nodeSize + gap)
        drawRoundRect(
            color = primary.copy(alpha = 0.6f),
            topLeft = Offset(x, centerY - nodeSize / 2),
            size = androidx.compose.ui.geometry.Size(nodeSize, nodeSize),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )
        if (i < nodeCount - 1) {
            drawLine(
                color = primary,
                start = Offset(x + nodeSize, centerY),
                end = Offset(x + nodeSize + gap, centerY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawBinarySearchDiagram(primary: androidx.compose.ui.graphics.Color, surface: androidx.compose.ui.graphics.Color) {
    val boxCount = 7
    val spacing = 6f
    val boxSize = (size.width - spacing * (boxCount - 1)) / boxCount
    val midIndex = boxCount / 2

    for (i in 0 until boxCount) {
        val x = i * (boxSize + spacing)
        val alpha = if (i == midIndex) 1f else 0.25f
        drawRoundRect(
            color = primary.copy(alpha = alpha),
            topLeft = Offset(x, 0f),
            size = androidx.compose.ui.geometry.Size(boxSize, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
    }
}

private fun DrawScope.drawTreeDiagram(primary: androidx.compose.ui.graphics.Color, surface: androidx.compose.ui.graphics.Color) {
    val nodeRadius = 20f
    val rootCenter = Offset(size.width / 2, nodeRadius + 10f)
    val leftCenter = Offset(size.width / 4, size.height - nodeRadius - 10f)
    val rightCenter = Offset(size.width * 3 / 4, size.height - nodeRadius - 10f)

    drawLine(color = primary, start = rootCenter, end = leftCenter, strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(color = primary, start = rootCenter, end = rightCenter, strokeWidth = 4f, cap = StrokeCap.Round)

    drawCircle(color = primary, radius = nodeRadius, center = rootCenter)
    drawCircle(color = primary.copy(alpha = 0.7f), radius = nodeRadius, center = leftCenter)
    drawCircle(color = primary.copy(alpha = 0.7f), radius = nodeRadius, center = rightCenter)
}

private fun DrawScope.drawStackDiagram(primary: androidx.compose.ui.graphics.Color, surface: androidx.compose.ui.graphics.Color) {
    val blockCount = 4
    val blockHeight = size.height / blockCount
    val blockWidth = size.width * 0.5f
    val startX = (size.width - blockWidth) / 2

    for (i in 0 until blockCount) {
        val y = i * blockHeight
        drawRoundRect(
            color = primary.copy(alpha = 0.3f + (blockCount - i) * 0.15f),
            topLeft = Offset(startX, y),
            size = androidx.compose.ui.geometry.Size(blockWidth, blockHeight - 6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
    }
}

private fun DrawScope.drawQueueDiagram(primary: androidx.compose.ui.graphics.Color, surface: androidx.compose.ui.graphics.Color) {
    val boxCount = 5
    val spacing = 8f
    val boxSize = (size.width - spacing * (boxCount - 1)) / boxCount
    for (i in 0 until boxCount) {
        val x = i * (boxSize + spacing)
        val alpha = 0.3f + (boxCount - i) * 0.12f
        drawRoundRect(
            color = primary.copy(alpha = alpha.coerceAtMost(1f)),
            topLeft = Offset(x, 0f),
            size = androidx.compose.ui.geometry.Size(boxSize, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
    }
}

private fun DrawScope.drawHeapDiagram(primary: androidx.compose.ui.graphics.Color, surface: androidx.compose.ui.graphics.Color) {
    val nodeRadius = 18f
    val rootCenter = Offset(size.width / 2, nodeRadius + 10f)
    val level2Y = size.height / 2
    val level3Y = size.height - nodeRadius - 10f

    val l2Left = Offset(size.width / 4, level2Y)
    val l2Right = Offset(size.width * 3 / 4, level2Y)
    val l3a = Offset(size.width / 8, level3Y)
    val l3b = Offset(size.width * 3 / 8, level3Y)

    drawLine(color = primary, start = rootCenter, end = l2Left, strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(color = primary, start = rootCenter, end = l2Right, strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(color = primary, start = l2Left, end = l3a, strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(color = primary, start = l2Left, end = l3b, strokeWidth = 3f, cap = StrokeCap.Round)

    drawCircle(color = primary, radius = nodeRadius, center = rootCenter)
    drawCircle(color = primary.copy(alpha = 0.75f), radius = nodeRadius, center = l2Left)
    drawCircle(color = primary.copy(alpha = 0.75f), radius = nodeRadius, center = l2Right)
    drawCircle(color = primary.copy(alpha = 0.5f), radius = nodeRadius * 0.8f, center = l3a)
    drawCircle(color = primary.copy(alpha = 0.5f), radius = nodeRadius * 0.8f, center = l3b)
}

private fun DrawScope.drawGraphDiagram(primary: androidx.compose.ui.graphics.Color, surface: androidx.compose.ui.graphics.Color) {
    val nodeRadius = 18f
    val a = Offset(size.width * 0.2f, size.height * 0.2f)
    val b = Offset(size.width * 0.8f, size.height * 0.25f)
    val c = Offset(size.width * 0.15f, size.height * 0.8f)
    val d = Offset(size.width * 0.55f, size.height * 0.7f)
    val e = Offset(size.width * 0.85f, size.height * 0.85f)

    val edges = listOf(a to b, a to c, b to d, c to d, d to e, b to e)
    edges.forEach { (from, to) ->
        drawLine(color = primary.copy(alpha = 0.6f), start = from, end = to, strokeWidth = 3f, cap = StrokeCap.Round)
    }

    listOf(a, b, c, d, e).forEach { node ->
        drawCircle(color = primary, radius = nodeRadius, center = node)
    }
}

private fun DrawScope.drawTrieDiagram(primary: androidx.compose.ui.graphics.Color, surface: androidx.compose.ui.graphics.Color) {
    val nodeRadius = 16f
    val root = Offset(size.width / 2, nodeRadius + 10f)
    val mid = Offset(size.width / 2, size.height / 2)
    val leftLeaf = Offset(size.width * 0.25f, size.height - nodeRadius - 10f)
    val rightLeaf = Offset(size.width * 0.75f, size.height - nodeRadius - 10f)

    drawLine(color = primary, start = root, end = mid, strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(color = primary, start = mid, end = leftLeaf, strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(color = primary, start = mid, end = rightLeaf, strokeWidth = 3f, cap = StrokeCap.Round)

    drawCircle(color = primary, radius = nodeRadius, center = root)
    drawCircle(color = primary.copy(alpha = 0.75f), radius = nodeRadius, center = mid)
    drawCircle(color = primary.copy(alpha = 0.5f), radius = nodeRadius, center = leftLeaf)
    drawCircle(color = primary.copy(alpha = 0.5f), radius = nodeRadius, center = rightLeaf)
}