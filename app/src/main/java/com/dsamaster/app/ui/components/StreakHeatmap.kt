package com.dsamaster.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dsamaster.app.data.entity.StreakEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.LaunchedEffect

/**
 * GitHub-style contribution heatmap. Draws a grid of weeks (columns) x days (rows),
 * color intensity based on problemsSolved for that day.
 */
@Composable
fun StreakHeatmap(
    entries: List<StreakEntry>,
    weeksToShow: Int = 26,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val entriesByDate = entries.associateBy { it.date }
    val today = LocalDate.now()

    val cellSizeDp = 14.dp
    val cellSpacingDp = 3.dp
    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSizeDp.toPx() }
    val cellSpacingPx = with(density) { cellSpacingDp.toPx() }

    val activeColor = MaterialTheme.colorScheme.tertiary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val freezeColor = MaterialTheme.colorScheme.secondary

    // Start from the most recent Sunday, going back weeksToShow weeks
    val startDate = today.minusWeeks(weeksToShow.toLong()).let { approx ->
        approx.minusDays(approx.dayOfWeek.value % 7L)
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(entries) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Box(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .width((cellSizeDp + cellSpacingDp) * (weeksToShow + 1))
                .height((cellSizeDp + cellSpacingDp) * 7)
        ) {
            for (week in 0..weeksToShow) {
                for (dayOfWeekIndex in 0..6) {
                    val date = startDate.plusDays((week * 7 + dayOfWeekIndex).toLong())
                    if (date.isAfter(today)) continue

                    val entry = entriesByDate[date.format(dateFormatter)]
                    val cellColor: Color = when {
                        entry == null -> emptyColor
                        entry.streakFreezeUsed && entry.problemsSolved == 0 -> freezeColor
                        entry.problemsSolved == 0 -> emptyColor
                        entry.problemsSolved <= 1 -> activeColor.copy(alpha = 0.4f)
                        entry.problemsSolved <= 3 -> activeColor.copy(alpha = 0.7f)
                        else -> activeColor
                    }

                    val offsetX = week * (cellSizePx + cellSpacingPx)
                    val offsetY = dayOfWeekIndex * (cellSizePx + cellSpacingPx)

                    drawRoundRect(
                        color = cellColor,
                        topLeft = Offset(offsetX, offsetY),
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }
    }
}