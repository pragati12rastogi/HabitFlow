package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.HabitDurationFormatter
import com.example.ui.theme.IndigoContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LavenderSecondary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSuccess
import com.example.ui.viewmodel.StopwatchSession
import kotlinx.coroutines.delay

@Composable
fun rememberLiveElapsedSeconds(session: StopwatchSession): Long {
    var elapsed by remember(session.habitId, session.date, session.accumulatedSeconds, session.runningSinceEpochMs) {
        mutableLongStateOf(session.currentElapsedSeconds())
    }
    LaunchedEffect(session.habitId, session.date, session.accumulatedSeconds, session.runningSinceEpochMs) {
        elapsed = session.currentElapsedSeconds()
        while (session.isRunning) {
            delay(250L)
            elapsed = session.currentElapsedSeconds()
        }
    }
    return elapsed
}

@Composable
fun ActiveStopwatchBar(
    session: StopwatchSession,
    onExpand: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onQuickFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val elapsedSeconds = rememberLiveElapsedSeconds(session)
    val accent = parseHexColor(session.colorHex)
    val indicatorColor by animateColorAsState(
        targetValue = if (session.isRunning) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "stopwatchBarIndicator"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onExpand() }
            .testTag("active_stopwatch_bar"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = "Active habit stopwatch",
                    tint = indicatorColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.habitName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (session.isRunning) "Focusing · Tap to open timer" else "Paused · Tap to resume",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (session.isRunning) IndigoContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = HabitDurationFormatter.formatStopwatch(elapsedSeconds),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (session.isRunning) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            IconButton(
                onClick = { if (session.isRunning) onPause() else onResume() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("stopwatch_bar_toggle")
            ) {
                Icon(
                    imageVector = if (session.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (session.isRunning) "Pause stopwatch" else "Resume stopwatch",
                    tint = IndigoPrimary
                )
            }

            FilledTonalIconButton(
                onClick = onQuickFinish,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MintContainer,
                    contentColor = MintSuccess
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("stopwatch_bar_finish")
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Finish and complete habit"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchFocusSheet(
    session: StopwatchSession,
    previouslyLoggedSecondsToday: Long,
    onMinimize: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onDiscard: () -> Unit,
    onFinishAndComplete: (notes: String) -> Unit,
    onLogTimeOnly: (notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val elapsedSeconds = rememberLiveElapsedSeconds(session)
    var sessionNotes by remember(session.habitId, session.date) { mutableStateOf("") }
    val accent = parseHexColor(session.colorHex)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    ModalBottomSheet(
        onDismissRequest = onMinimize,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    Text(
                        text = session.habitName,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDiscard,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("stopwatch_discard_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Discard stopwatch session"
                    )
                }
            }

            if (previouslyLoggedSecondsToday > 0L) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MintContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "Already logged today: ${HabitDurationFormatter.formatCompact(previouslyLoggedSecondsToday)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Stopwatch Dial
            Box(
                modifier = Modifier.size(216.dp),
                contentAlignment = Alignment.Center
            ) {
                val secondsInMinute = (elapsedSeconds % 60L).toFloat()
                val minuteSweep = if (elapsedSeconds == 0L) 0f else ((secondsInMinute / 60f).takeIf { it > 0f } ?: 1f) * 360f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 14.dp.toPx()
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
                    if (minuteSweep > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(accent, IndigoPrimary, LavenderSecondary, accent)
                            ),
                            startAngle = -90f,
                            sweepAngle = minuteSweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = HabitDurationFormatter.formatStopwatch(elapsedSeconds),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("stopwatch_elapsed_display")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (session.isRunning) "STOPWATCH RUNNING" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (session.isRunning) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Transport Controls: Reset / Pause-Resume
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onReset,
                    enabled = elapsedSeconds > 0L,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("stopwatch_reset_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset")
                }

                Button(
                    onClick = { if (session.isRunning) onPause() else onResume() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (session.isRunning) IndigoContainer else IndigoPrimary,
                        contentColor = if (session.isRunning) IndigoPrimary else Color.White
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("stopwatch_toggle_button")
                ) {
                    Icon(
                        imageVector = if (session.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (session.isRunning) "Pause" else "Resume")
                }
            }

            OutlinedTextField(
                value = sessionNotes,
                onValueChange = { if (it.length <= 300) sessionNotes = it },
                label = { Text("Session note (optional)") },
                placeholder = { Text("What did you focus on?") },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Save Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onFinishAndComplete(sessionNotes.trim()) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintSuccess,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("stopwatch_finish_complete_button")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Finish & Mark Habit Done")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onLogTimeOnly(sessionNotes.trim()) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("stopwatch_log_time_only_button")
                    ) {
                        Text("Log Time Only")
                    }
                    TextButton(
                        onClick = onMinimize,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("stopwatch_minimize_button")
                    ) {
                        Text("Minimize")
                    }
                }
            }
        }
    }
}
