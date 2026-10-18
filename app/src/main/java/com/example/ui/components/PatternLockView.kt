package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

import androidx.compose.material3.MaterialTheme

@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    dotRadius: Float = 14f,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    lineColor: Color = MaterialTheme.colorScheme.secondary,
    dotColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    onPatternComplete: (String) -> Unit
) {
    var selectedDots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var dotCenters by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selectedDots = emptyList()
                        currentTouchPoint = offset
                        val hit = findHitDot(offset, dotCenters, dotRadius * 3f)
                        if (hit != null) {
                            selectedDots = listOf(hit)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentTouchPoint = change.position
                        val hit = findHitDot(change.position, dotCenters, dotRadius * 3f)
                        if (hit != null && !selectedDots.contains(hit)) {
                            val last = selectedDots.lastOrNull()
                            if (last != null) {
                                val mid = getIntermediateDot(last, hit)
                                if (mid != null && !selectedDots.contains(mid)) {
                                    selectedDots = selectedDots + mid
                                }
                            }
                            selectedDots = selectedDots + hit
                        }
                    },
                    onDragEnd = {
                        currentTouchPoint = null
                        if (selectedDots.isNotEmpty()) {
                            val patternStr = selectedDots.joinToString("-")
                            onPatternComplete(patternStr)
                        }
                    },
                    onDragCancel = {
                        currentTouchPoint = null
                        selectedDots = emptyList()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = this.size.width
            val height = this.size.height
            val cellWidth = width / 3f
            val cellHeight = height / 3f

            // Calculate 3x3 centers
            val centers = mutableListOf<Offset>()
            for (row in 0 until 3) {
                for (col in 0 until 3) {
                    val cx = col * cellWidth + cellWidth / 2f
                    val cy = row * cellHeight + cellHeight / 2f
                    centers.add(Offset(cx, cy))
                }
            }
            dotCenters = centers

            // Draw connecting lines between selected dots
            if (selectedDots.size > 1) {
                for (i in 0 until selectedDots.size - 1) {
                    val startDot = selectedDots[i]
                    val endDot = selectedDots[i + 1]
                    drawLine(
                        color = lineColor,
                        start = centers[startDot],
                        end = centers[endDot],
                        strokeWidth = 10f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw line to current touch point
            if (selectedDots.isNotEmpty() && currentTouchPoint != null) {
                val lastDot = selectedDots.last()
                drawLine(
                    color = lineColor.copy(alpha = 0.7f),
                    start = centers[lastDot],
                    end = currentTouchPoint!!,
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
            }

            // Draw 9 dots
            for (i in centers.indices) {
                val center = centers[i]
                val isSelected = selectedDots.contains(i)

                if (isSelected) {
                    // Outer glow ring
                    drawCircle(
                        color = activeColor.copy(alpha = 0.35f),
                        radius = dotRadius * 2.2f,
                        center = center
                    )
                    // Inner filled circle
                    drawCircle(
                        color = activeColor,
                        radius = dotRadius * 1.3f,
                        center = center
                    )
                } else {
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = center
                    )
                }
            }
        }
    }
}

private fun findHitDot(point: Offset, centers: List<Offset>, threshold: Float): Int? {
    for (i in centers.indices) {
        val center = centers[i]
        val dist = hypot(point.x - center.x, point.y - center.y)
        if (dist <= threshold) {
            return i
        }
    }
    return null
}

private fun getIntermediateDot(from: Int, to: Int): Int? {
    return when {
        // Horizontal middle skips
        (from == 0 && to == 2) || (from == 2 && to == 0) -> 1
        (from == 3 && to == 5) || (from == 5 && to == 3) -> 4
        (from == 6 && to == 8) || (from == 8 && to == 6) -> 7
        // Vertical middle skips
        (from == 0 && to == 6) || (from == 6 && to == 0) -> 3
        (from == 1 && to == 7) || (from == 7 && to == 1) -> 4
        (from == 2 && to == 8) || (from == 8 && to == 2) -> 5
        // Diagonal middle skips
        (from == 0 && to == 8) || (from == 8 && to == 0) -> 4
        (from == 2 && to == 6) || (from == 6 && to == 2) -> 4
        else -> null
    }
}
