package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.HabitPriority
import com.example.domain.DayProgressPoint
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberStreak
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralError
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.LavenderSecondary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSuccess

fun parseHexColor(hex: String, fallback: Color = IndigoPrimary): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        if (clean.length == 6) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else if (clean.length == 8) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

fun categoryIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "health", "favorite" -> Icons.Outlined.FavoriteBorder
        "fitness" -> Icons.Outlined.FitnessCenter
        "learning", "school" -> Icons.Outlined.School
        "productivity", "bolt" -> Icons.Outlined.Bolt
        "personal development", "psychology" -> Icons.Outlined.Psychology
        "finance", "savings" -> Icons.Outlined.Savings
        "mindfulness", "spa" -> Icons.Outlined.Spa
        "lifestyle", "wb_sunny" -> Icons.Outlined.WbSunny
        "bedtime", "sleep" -> Icons.Outlined.Bedtime
        else -> Icons.Outlined.StarOutline
    }
}

@Composable
fun AnimatedCompletionRing(
    percentage: Int,
    modifier: Modifier = Modifier,
    ringSize: Dp = 112.dp,
    strokeWidth: Dp = 11.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (percentage.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 700),
        label = "completionRing"
    )
    val trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val accentColor = MintSuccess

    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryColor, LavenderSecondary, accentColor, primaryColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Today",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StreakPill(streakDays: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = AmberContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "Streak",
                tint = AmberStreak,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$streakDays d",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF92400E)
            )
        }
    }
}

@Composable
fun PriorityBadge(priorityRaw: String) {
    val priority = HabitPriority.fromRaw(priorityRaw)
    val (bg, fg) = when (priority) {
        HabitPriority.HIGH -> CoralContainer to CoralError
        HabitPriority.MEDIUM -> IndigoContainer to IndigoPrimary
        HabitPriority.LOW -> MintContainer to Color(0xFF15803D)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = priority.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun WeeklyBarChart(
    points: List<DayProgressPoint>,
    modifier: Modifier = Modifier
) {
    val barBrush = Brush.verticalGradient(listOf(IndigoPrimary, LavenderSecondary))
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val successColor = MintSuccess

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val barSlotWidth = size.width / points.size
            val barWidth = (barSlotWidth * 0.48f).coerceAtMost(34.dp.toPx())
            val maxHeight = size.height - 8.dp.toPx()

            points.forEachIndexed { idx, point ->
                val left = idx * barSlotWidth + (barSlotWidth - barWidth) / 2f
                // Background track bar
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 4.dp.toPx()),
                    size = Size(barWidth, maxHeight),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                val fillFraction = point.rate.coerceIn(0f, 1f)
                if (fillFraction > 0f) {
                    val fillHeight = (maxHeight * fillFraction).coerceAtLeast(10.dp.toPx())
                    val top = 4.dp.toPx() + (maxHeight - fillHeight)
                    if (point.rate >= 0.99f) {
                        drawRoundRect(
                            color = successColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, fillHeight),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    } else {
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, fillHeight),
                            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEach { point ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${point.completed}/${point.scheduled}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
fun TrendAreaChart(
    points: List<DayProgressPoint>,
    modifier: Modifier = Modifier
) {
    val lineColor = IndigoPrimary
    val fillStart = IndigoPrimary.copy(alpha = 0.28f)
    val fillEnd = IndigoPrimary.copy(alpha = 0.02f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val h = size.height
            val w = size.width
            // Horizontal guide lines
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                val y = h * (1f - frac)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            if (points.size < 2) return@Canvas
            val stepX = w / (points.size - 1).coerceAtLeast(1)
            val coords = points.mapIndexed { idx, p ->
                val x = idx * stepX
                val y = h - (p.rate.coerceIn(0f, 1f) * (h - 16.dp.toPx())) - 8.dp.toPx()
                Offset(x, y)
            }
            val areaPath = Path().apply {
                moveTo(coords.first().x, h)
                coords.forEach { lineTo(it.x, it.y) }
                lineTo(coords.last().x, h)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(listOf(fillStart, fillEnd))
            )
            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    val prev = coords[i - 1]
                    val curr = coords[i]
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            coords.forEach { pt ->
                drawCircle(color = Color.White, radius = 4.5.dp.toPx(), center = pt)
                drawCircle(color = lineColor, radius = 3.dp.toPx(), center = pt)
            }
        }
        if (points.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(points.first().date.takeLast(5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(points[points.size / 2].date.takeLast(5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(points.last().date.takeLast(5), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CalendarHeatmap(
    points: List<DayProgressPoint>,
    modifier: Modifier = Modifier
) {
    val rows = points.chunked(7)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEach { day ->
                    val cellColor = when {
                        day.scheduled == 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        day.rate >= 0.99f -> MintSuccess
                        day.rate >= 0.6f -> IndigoPrimary
                        day.rate > 0f -> LavenderSecondary.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(cellColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.date.takeLast(2),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (day.rate > 0.3f) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SponsoredBannerSlot(
    settings: AppSettings,
    slotLabel: String,
    slotId: String,
    modifier: Modifier = Modifier
) {
    if (!settings.adsEnabled) return
    val hasRealConfig = settings.adPublisherId.isNotBlank() && slotId.isNotBlank()
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(IndigoContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasRealConfig) Icons.Filled.Campaign else Icons.Outlined.AutoAwesome,
                    contentDescription = "Sponsored placement",
                    tint = IndigoPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasRealConfig) "Sponsored · $slotLabel" else "Ad Placement Ready · $slotLabel",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (hasRealConfig) {
                        "Publisher ${settings.adPublisherId} · Slot $slotId"
                    } else {
                        "Configure Publisher & Slot IDs in Admin Settings to activate live display ads."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyHabitsCard(
    onCreateHabit: () -> Unit,
    onLoadDemoData: () -> Unit,
    modifier: Modifier = Modifier,
    heroImage: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            heroImage()
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Ready to start your flow?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Create your first habit or load sample habits & 10-day history to explore streaks, routines, and analytics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCreateHabit() },
                    color = IndigoPrimary
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "New Habit",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onLoadDemoData() },
                    color = IndigoContainer
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Load Demo",
                            style = MaterialTheme.typography.labelLarge,
                            color = IndigoPrimary
                        )
                    }
                }
            }
        }
    }
}
