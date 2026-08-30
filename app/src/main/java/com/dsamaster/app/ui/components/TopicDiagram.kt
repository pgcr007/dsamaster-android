package com.dsamaster.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dsamaster.app.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlin.math.round

@Composable
fun TopicDiagram(diagramType: String, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.tertiary
    val highlightColor = MaterialTheme.colorScheme.secondary

    when (diagramType) {
        "array" -> {
            val boxCount = 6
            val transition = rememberInfiniteTransition(label = "arrayScan")
            val scanPos by transition.animateFloat(
                initialValue = 0f,
                targetValue = (boxCount - 1).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scanPos"
            )
            Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
                drawArrayDiagram(primaryColor, highlightColor, boxCount, scanPos)
            }
        }

        "linkedlist" -> {
            val nodeCount = 4
            val transition = rememberInfiniteTransition(label = "listTraverse")
            val travelPos by transition.animateFloat(
                initialValue = 0f,
                targetValue = (nodeCount - 1).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "travelPos"
            )
            Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
                drawLinkedListDiagram(primaryColor, highlightColor, nodeCount, travelPos)
            }
        }

        "binarysearch" -> {
            // Simulated binary search over 15 elements, searching for the value at index 2.
            val boxCount = 15
            val target = 2
            val steps = remember { simulateBinarySearchSteps(boxCount, target) }
            val step by rememberLoopingStep(steps = steps.size + 2, holdMillis = 1000L)
            val clampedStep = step.coerceAtMost(steps.size - 1)
            val current = steps[clampedStep]
            val isFoundStep = clampedStep == steps.lastIndex

            val animatedLow by animateFloatAsState(current.low.toFloat(), tween(450), label = "low")
            val animatedHigh by animateFloatAsState(current.high.toFloat(), tween(450), label = "high")
            val animatedMid by animateFloatAsState(current.mid.toFloat(), tween(450), label = "mid")

            Canvas(modifier = modifier.fillMaxWidth().height(96.dp)) {
                drawBinarySearchDiagram(
                    primary = primaryColor,
                    highlight = highlightColor,
                    foundColor = SuccessGreen,
                    boxCount = boxCount,
                    low = animatedLow,
                    high = animatedHigh,
                    mid = animatedMid,
                    isFound = isFoundStep
                )
            }
        }

        "tree" -> {
            val step by rememberLoopingStep(steps = 4, holdMillis = 700L)
            val rootAlpha by animateFloatAsState(if (step >= 1) 1f else 0f, tween(400), label = "rootAlpha")
            val levelAlpha by animateFloatAsState(if (step >= 2) 1f else 0f, tween(400), label = "levelAlpha")
            Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
                drawTreeDiagram(primaryColor, rootAlpha, levelAlpha)
            }
        }

        "stack" -> {
            val blockCount = 4
            val totalSteps = blockCount + 3
            val step by rememberLoopingStep(steps = totalSteps, holdMillis = 550L)
            val visibleBlocks = when {
                step <= blockCount - 1 -> step + 1
                step == blockCount -> blockCount
                step == blockCount + 1 -> blockCount - 1
                else -> blockCount - 1
            }
            val animatedVisible by animateFloatAsState(visibleBlocks.toFloat(), tween(350), label = "stackVisible")
            Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
                drawStackDiagram(primaryColor, blockCount, animatedVisible)
            }
        }

        "queue" -> {
            val boxCount = 5
            val step by rememberLoopingStep(steps = 4, holdMillis = 650L)
            val frontAlpha by animateFloatAsState(if (step == 1) 0.15f else 1f, tween(400), label = "frontAlpha")
            val backAlpha by animateFloatAsState(if (step == 3) 1f else if (step == 2) 0.15f else 1f, tween(400), label = "backAlpha")
            Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
                drawQueueDiagram(primaryColor, boxCount, frontAlpha, backAlpha)
            }
        }

        "heap" -> {
            val step by rememberLoopingStep(steps = 5, holdMillis = 650L)
            val rootAlpha by animateFloatAsState(if (step >= 1) 1f else 0f, tween(400), label = "heapRoot")
            val level2Alpha by animateFloatAsState(if (step >= 2) 1f else 0f, tween(400), label = "heapL2")
            val level3Alpha by animateFloatAsState(if (step >= 3) 1f else 0f, tween(400), label = "heapL3")
            Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
                drawHeapDiagram(primaryColor, rootAlpha, level2Alpha, level3Alpha)
            }
        }

        "graph" -> {
            val edgeCount = 6
            val step by rememberLoopingStep(steps = edgeCount + 2, holdMillis = 550L)
            val revealedCount = step.coerceAtMost(edgeCount)
            Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
                drawGraphDiagram(primaryColor, highlightColor, revealedCount)
            }
        }

        "trie" -> {
            val step by rememberLoopingStep(steps = 5, holdMillis = 650L)
            val rootAlpha by animateFloatAsState(if (step >= 1) 1f else 0f, tween(400), label = "trieRoot")
            val midAlpha by animateFloatAsState(if (step >= 2) 1f else 0f, tween(400), label = "trieMid")
            val leafAlpha by animateFloatAsState(if (step >= 3) 1f else 0f, tween(400), label = "trieLeaf")
            Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
                drawTrieDiagram(primaryColor, rootAlpha, midAlpha, leafAlpha)
            }
        }
        // "none" or any unrecognized type: render nothing
    }
}

/** Drives a looping integer step counter (0 until [steps]) that advances every [holdMillis]. */
@Composable
private fun rememberLoopingStep(steps: Int, holdMillis: Long = 900L): State<Int> {
    val state = remember { mutableStateOf(0) }
    LaunchedEffect(steps, holdMillis) {
        if (steps <= 1) return@LaunchedEffect
        while (true) {
            delay(holdMillis)
            state.value = (state.value + 1) % steps
        }
    }
    return state
}

private data class SearchStep(val low: Int, val high: Int, val mid: Int)

private fun simulateBinarySearchSteps(size: Int, target: Int): List<SearchStep> {
    val steps = mutableListOf<SearchStep>()
    var low = 0
    var high = size - 1
    while (low <= high) {
        val mid = (low + high) / 2
        steps.add(SearchStep(low, high, mid))
        if (mid == target) break
        if (mid < target) low = mid + 1 else high = mid - 1
    }
    return steps
}

private fun DrawScope.drawArrayDiagram(
    primary: Color,
    highlight: Color,
    boxCount: Int,
    scanPos: Float
) {
    val spacing = 8f
    val boxSize = (size.width - spacing * (boxCount - 1)) / boxCount
    for (i in 0 until boxCount) {
        val x = i * (boxSize + spacing)
        drawRoundRect(
            color = primary.copy(alpha = 0.25f + (i % 3) * 0.15f),
            topLeft = Offset(x, 0f),
            size = Size(boxSize, size.height),
            cornerRadius = CornerRadius(6f, 6f)
        )
    }
    // Animated scanner outline sliding across the array
    val scanX = scanPos * (boxSize + spacing)
    drawRoundRect(
        color = highlight,
        topLeft = Offset(scanX, 0f),
        size = Size(boxSize, size.height),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 5f)
    )
}

private fun DrawScope.drawLinkedListDiagram(
    primary: Color,
    highlight: Color,
    nodeCount: Int,
    travelPos: Float
) {
    val nodeSize = 50f
    val gap = (size.width - nodeSize * nodeCount) / (nodeCount - 1)
    val centerY = size.height / 2

    for (i in 0 until nodeCount) {
        val x = i * (nodeSize + gap)
        drawRoundRect(
            color = primary.copy(alpha = 0.6f),
            topLeft = Offset(x, centerY - nodeSize / 2),
            size = Size(nodeSize, nodeSize),
            cornerRadius = CornerRadius(8f, 8f)
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

    // Traveling pointer walking the chain, back and forth
    val step = nodeSize + gap
    val travelX = travelPos * step + nodeSize / 2
    drawCircle(color = highlight, radius = 10f, center = Offset(travelX, centerY))
}

private fun DrawScope.drawBinarySearchDiagram(
    primary: Color,
    highlight: Color,
    foundColor: Color,
    boxCount: Int,
    low: Float,
    high: Float,
    mid: Float,
    isFound: Boolean
) {
    val spacing = 4f
    val boxSize = (size.width - spacing * (boxCount - 1)) / boxCount
    val topY = 26f
    val boxHeight = size.height - topY
    val roundedLow = low.toInt()
    val roundedHigh = high.toInt()
    val roundedMid = round(mid).toInt()

    for (i in 0 until boxCount) {
        val x = i * (boxSize + spacing)
        val inRange = i in roundedLow..roundedHigh
        val isMid = i == roundedMid
        val color = when {
            isMid && isFound -> foundColor
            isMid -> highlight
            inRange -> primary.copy(alpha = 0.55f)
            else -> primary.copy(alpha = 0.12f)
        }
        drawRoundRect(
            color = color,
            topLeft = Offset(x, topY),
            size = Size(boxSize, boxHeight),
            cornerRadius = CornerRadius(5f, 5f)
        )
    }

    // Range bracket showing [low, high] narrowing over time
    val bracketY = topY - 12f
    val leftX = low * (boxSize + spacing)
    val rightX = high * (boxSize + spacing) + boxSize
    drawLine(color = primary, start = Offset(leftX, bracketY), end = Offset(rightX, bracketY), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(color = primary, start = Offset(leftX, bracketY - 5f), end = Offset(leftX, bracketY + 5f), strokeWidth = 3f)
    drawLine(color = primary, start = Offset(rightX, bracketY - 5f), end = Offset(rightX, bracketY + 5f), strokeWidth = 3f)

    // Mid marker, animated smoothly between steps
    val midX = mid * (boxSize + spacing) + boxSize / 2
    val markerColor = if (isFound) foundColor else highlight
    drawCircle(color = markerColor, radius = 6f, center = Offset(midX, bracketY - 14f))
}

private fun DrawScope.drawTreeDiagram(primary: Color, rootAlpha: Float, levelAlpha: Float) {
    val nodeRadius = 20f
    val rootCenter = Offset(size.width / 2, nodeRadius + 10f)
    val leftCenter = Offset(size.width / 4, size.height - nodeRadius - 10f)
    val rightCenter = Offset(size.width * 3 / 4, size.height - nodeRadius - 10f)

    if (levelAlpha > 0f) {
        drawLine(color = primary.copy(alpha = levelAlpha), start = rootCenter, end = leftCenter, strokeWidth = 4f, cap = StrokeCap.Round)
        drawLine(color = primary.copy(alpha = levelAlpha), start = rootCenter, end = rightCenter, strokeWidth = 4f, cap = StrokeCap.Round)
    }
    if (rootAlpha > 0f) {
        drawCircle(color = primary.copy(alpha = rootAlpha), radius = nodeRadius, center = rootCenter)
    }
    if (levelAlpha > 0f) {
        drawCircle(color = primary.copy(alpha = 0.7f * levelAlpha), radius = nodeRadius, center = leftCenter)
        drawCircle(color = primary.copy(alpha = 0.7f * levelAlpha), radius = nodeRadius, center = rightCenter)
    }
}

private fun DrawScope.drawStackDiagram(primary: Color, blockCount: Int, animatedVisible: Float) {
    val blockHeight = size.height / blockCount
    val blockWidth = size.width * 0.5f
    val startX = (size.width - blockWidth) / 2

    for (i in 0 until blockCount) {
        val y = i * blockHeight
        val slotFromBottom = blockCount - i // 1 = bottom-most slot, blockCount = top-most
        val revealAmount = (animatedVisible - (slotFromBottom - 1)).coerceIn(0f, 1f)
        if (revealAmount <= 0.01f) continue
        val baseAlpha = 0.3f + (blockCount - i) * 0.15f
        drawRoundRect(
            color = primary.copy(alpha = (baseAlpha * revealAmount).coerceIn(0f, 1f)),
            topLeft = Offset(startX, y),
            size = Size(blockWidth, blockHeight - 6f),
            cornerRadius = CornerRadius(6f, 6f)
        )
    }
}

private fun DrawScope.drawQueueDiagram(primary: Color, boxCount: Int, frontAlpha: Float, backAlpha: Float) {
    val spacing = 8f
    val boxSize = (size.width - spacing * (boxCount - 1)) / boxCount
    for (i in 0 until boxCount) {
        val x = i * (boxSize + spacing)
        var alpha = (0.3f + (boxCount - i) * 0.12f).coerceAtMost(1f)
        if (i == 0) alpha *= frontAlpha
        if (i == boxCount - 1) alpha *= backAlpha
        drawRoundRect(
            color = primary.copy(alpha = alpha),
            topLeft = Offset(x, 0f),
            size = Size(boxSize, size.height),
            cornerRadius = CornerRadius(6f, 6f)
        )
    }
}

private fun DrawScope.drawHeapDiagram(primary: Color, rootAlpha: Float, level2Alpha: Float, level3Alpha: Float) {
    val nodeRadius = 18f
    val rootCenter = Offset(size.width / 2, nodeRadius + 10f)
    val level2Y = size.height / 2
    val level3Y = size.height - nodeRadius - 10f

    val l2Left = Offset(size.width / 4, level2Y)
    val l2Right = Offset(size.width * 3 / 4, level2Y)
    val l3a = Offset(size.width / 8, level3Y)
    val l3b = Offset(size.width * 3 / 8, level3Y)

    if (level2Alpha > 0f) {
        drawLine(color = primary.copy(alpha = level2Alpha), start = rootCenter, end = l2Left, strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(color = primary.copy(alpha = level2Alpha), start = rootCenter, end = l2Right, strokeWidth = 3f, cap = StrokeCap.Round)
    }
    if (level3Alpha > 0f) {
        drawLine(color = primary.copy(alpha = level3Alpha), start = l2Left, end = l3a, strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(color = primary.copy(alpha = level3Alpha), start = l2Left, end = l3b, strokeWidth = 3f, cap = StrokeCap.Round)
    }

    if (rootAlpha > 0f) drawCircle(color = primary.copy(alpha = rootAlpha), radius = nodeRadius, center = rootCenter)
    if (level2Alpha > 0f) {
        drawCircle(color = primary.copy(alpha = 0.75f * level2Alpha), radius = nodeRadius, center = l2Left)
        drawCircle(color = primary.copy(alpha = 0.75f * level2Alpha), radius = nodeRadius, center = l2Right)
    }
    if (level3Alpha > 0f) {
        drawCircle(color = primary.copy(alpha = 0.5f * level3Alpha), radius = nodeRadius * 0.8f, center = l3a)
        drawCircle(color = primary.copy(alpha = 0.5f * level3Alpha), radius = nodeRadius * 0.8f, center = l3b)
    }
}

private fun DrawScope.drawGraphDiagram(primary: Color, highlight: Color, revealedCount: Int) {
    val nodeRadius = 18f
    val a = Offset(size.width * 0.2f, size.height * 0.2f)
    val b = Offset(size.width * 0.8f, size.height * 0.25f)
    val c = Offset(size.width * 0.15f, size.height * 0.8f)
    val d = Offset(size.width * 0.55f, size.height * 0.7f)
    val e = Offset(size.width * 0.85f, size.height * 0.85f)

    val edges = listOf(a to b, a to c, b to d, c to d, d to e, b to e)
    edges.forEachIndexed { index, (from, to) ->
        val visited = index < revealedCount
        drawLine(
            color = if (visited) highlight else primary.copy(alpha = 0.25f),
            start = from,
            end = to,
            strokeWidth = if (visited) 4f else 3f,
            cap = StrokeCap.Round
        )
    }

    listOf(a, b, c, d, e).forEach { node ->
        drawCircle(color = primary, radius = nodeRadius, center = node)
    }
}

private fun DrawScope.drawTrieDiagram(primary: Color, rootAlpha: Float, midAlpha: Float, leafAlpha: Float) {
    val nodeRadius = 16f
    val root = Offset(size.width / 2, nodeRadius + 10f)
    val mid = Offset(size.width / 2, size.height / 2)
    val leftLeaf = Offset(size.width * 0.25f, size.height - nodeRadius - 10f)
    val rightLeaf = Offset(size.width * 0.75f, size.height - nodeRadius - 10f)

    if (midAlpha > 0f) drawLine(color = primary.copy(alpha = midAlpha), start = root, end = mid, strokeWidth = 3f, cap = StrokeCap.Round)
    if (leafAlpha > 0f) {
        drawLine(color = primary.copy(alpha = leafAlpha), start = mid, end = leftLeaf, strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(color = primary.copy(alpha = leafAlpha), start = mid, end = rightLeaf, strokeWidth = 3f, cap = StrokeCap.Round)
    }

    if (rootAlpha > 0f) drawCircle(color = primary.copy(alpha = rootAlpha), radius = nodeRadius, center = root)
    if (midAlpha > 0f) drawCircle(color = primary.copy(alpha = 0.75f * midAlpha), radius = nodeRadius, center = mid)
    if (leafAlpha > 0f) {
        drawCircle(color = primary.copy(alpha = 0.5f * leafAlpha), radius = nodeRadius, center = leftLeaf)
        drawCircle(color = primary.copy(alpha = 0.5f * leafAlpha), radius = nodeRadius, center = rightLeaf)
    }
}