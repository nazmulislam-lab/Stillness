package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AppCategory
import com.example.model.AppUsageEntry
import com.example.model.DailyUsageSummary
import com.example.ui.theme.StillnessThemeTokens
import com.example.ui.util.formatDurationMillis
import com.example.ui.util.formatDurationMinutes
import kotlin.math.roundToInt

@Composable
fun categoryColor(category: AppCategory): Color {
    val ext = StillnessThemeTokens.extendedColors
    return when (category) {
        AppCategory.SOCIAL -> ext.categorySocial
        AppCategory.VIDEO -> ext.categoryVideo
        AppCategory.GAMES -> ext.categoryGames
        AppCategory.COMMUNICATION -> ext.categoryCommunication
        AppCategory.PRODUCTIVITY -> ext.categoryProductivity
        AppCategory.OTHER -> ext.categoryOther
    }
}

@Composable
fun UsageAccessOnboardingCard(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Image(
                painter = painterResource(id = R.drawable.img_usage_access_1794810589756),
                contentDescription = "Illustration of calm mobile device usage",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.usage_access_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.usage_access_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("grant_usage_access_button"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.usage_access_cta))
            }
        }
    }
}

@Composable
fun ScreenTimeRingCard(
    todaySummary: DailyUsageSummary,
    dailyGoalMinutes: Int,
    hasUsageAccess: Boolean,
    modifier: Modifier = Modifier,
) {
    val totalMinutes = (todaySummary.totalTimeMillis / 60_000L).toInt()
    val progress = (totalMinutes.toFloat() / dailyGoalMinutes.coerceAtLeast(30).toFloat()).coerceIn(0f, 1.25f)
    val animatedSweep by animateFloatAsState(
        targetValue = (progress.coerceIn(0f, 1f)) * 240f,
        animationSpec = tween(durationMillis = 650),
        label = "screenTimeArc",
    )

    val ext = StillnessThemeTokens.extendedColors
    val arcColor = when {
        !hasUsageAccess -> MaterialTheme.colorScheme.outlineVariant
        progress <= 0.8f -> ext.good
        progress <= 1.0f -> ext.caution
        else -> ext.alert
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val statusLabel = when {
        !hasUsageAccess -> "Awaiting access"
        progress <= 0.8f -> "Calm pace"
        progress <= 1.0f -> "Near daily budget"
        else -> "Over budget"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Today's Screen Time",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = CircleShape,
                    color = arcColor.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = arcColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(210.dp),
            ) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    val strokeWidth = 18.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = trackColor,
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    if (animatedSweep > 0.5f) {
                        drawArc(
                            color = arcColor,
                            startAngle = 150f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (hasUsageAccess) formatDurationMillis(todaySummary.totalTimeMillis) else "--",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "of ${formatDurationMinutes(dailyGoalMinutes)} goal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricMiniPill(
                    icon = Icons.Outlined.TouchApp,
                    label = "App opens",
                    value = if (hasUsageAccess) "${todaySummary.totalLaunches}" else "--",
                    modifier = Modifier.weight(1f),
                )
                MetricMiniPill(
                    icon = Icons.Outlined.AccessTime,
                    label = "Longest",
                    value = if (hasUsageAccess) formatDurationMillis(todaySummary.longestSessionMillis) else "--",
                    modifier = Modifier.weight(1f),
                )
                MetricMiniPill(
                    icon = Icons.Outlined.Bedtime,
                    label = "Late night",
                    value = if (hasUsageAccess) formatDurationMillis(todaySummary.lateNightMillis) else "--",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MetricMiniPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun HourlyRhythmCard(
    hourlyMinutes: List<Int>,
    windDownStartHour: Int,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val lateNightColor = MaterialTheme.colorScheme.secondary
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant
    val maxMinute = (hourlyMinutes.maxOrNull() ?: 15).coerceAtLeast(15)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "24-Hour Rhythm",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendDot(color = activeColor, label = "Day")
                    LegendDot(color = lateNightColor, label = "Wind-down")
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
            ) {
                val barCount = 24
                val gap = 4.dp.toPx()
                val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)
                val minHeight = 4.dp.toPx()

                for (hour in 0 until barCount) {
                    val mins = hourlyMinutes.getOrElse(hour) { 0 }
                    val fraction = (mins.toFloat() / maxMinute.toFloat()).coerceIn(0f, 1f)
                    val barHeight = if (mins == 0) minHeight else (minHeight + fraction * (size.height - minHeight))
                    val isWindDown = hour >= windDownStartHour || hour < 5
                    val barColor = when {
                        mins == 0 -> emptyBarColor
                        isWindDown -> lateNightColor
                        else -> activeColor
                    }
                    val left = hour * (barWidth + gap)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("12a", "6a", "12p", "6p", "11p").forEach { tick ->
                    Text(
                        text = tick,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CategoryBalanceCard(
    categoryMillis: Map<AppCategory, Long>,
    modifier: Modifier = Modifier,
) {
    val total = categoryMillis.values.sum().coerceAtLeast(1L)
    val sorted = categoryMillis.entries
        .filter { it.value > 0L }
        .sortedByDescending { it.value }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Category Balance",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (sorted.isEmpty()) {
                Text(
                    text = "No category usage recorded yet for this window.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(CircleShape),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    sorted.forEach { (category, millis) ->
                        val weight = (millis.toFloat() / total.toFloat()).coerceAtLeast(0.03f)
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(14.dp)
                                .background(categoryColor(category)),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                sorted.take(4).forEach { (category, millis) ->
                    val pct = ((millis.toDouble() / total.toDouble()) * 100.0).roundToInt()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor(category)),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            text = "${formatDurationMillis(millis)} ($pct%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyTrendChartCard(
    dailySummaries: List<DailyUsageSummary>,
    selectedIndex: Int,
    dailyGoalMinutes: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val overBudgetColor = MaterialTheme.colorScheme.secondary
    val selectedRingColor = MaterialTheme.colorScheme.onSurface
    val goalLineColor = MaterialTheme.colorScheme.outline

    val maxMinutes = maxOf(
        dailyGoalMinutes + 30,
        dailySummaries.maxOfOrNull { (it.totalTimeMillis / 60_000L).toInt() } ?: 180,
    ).coerceAtLeast(60)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "7-Day Screen Time",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Goal: ${formatDurationMinutes(dailyGoalMinutes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
            ) {
                val goalY = size.height * (1f - (dailyGoalMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f))
                drawLine(
                    color = goalLineColor.copy(alpha = 0.55f),
                    start = Offset(0f, goalY),
                    end = Offset(size.width, goalY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                )

                val count = dailySummaries.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.46f).coerceIn(14.dp.toPx(), 32.dp.toPx())

                dailySummaries.forEachIndexed { idx, summary ->
                    val mins = (summary.totalTimeMillis / 60_000L).toInt()
                    val ratio = (mins.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
                    val barHeight = maxOf(6.dp.toPx(), ratio * size.height)
                    val centerX = slotWidth * idx + slotWidth / 2f
                    val left = centerX - barWidth / 2f
                    val isOverGoal = mins > dailyGoalMinutes
                    val isSelected = idx == selectedIndex
                    val barColor = when {
                        mins == 0 -> goalLineColor.copy(alpha = 0.25f)
                        isOverGoal -> overBudgetColor
                        else -> primaryColor
                    }.copy(alpha = if (isSelected) 1f else 0.7f)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                    )
                    if (isSelected) {
                        drawRoundRect(
                            color = selectedRingColor.copy(alpha = 0.35f),
                            topLeft = Offset(left - 2.dp.toPx(), size.height - barHeight - 2.dp.toPx()),
                            size = Size(barWidth + 4.dp.toPx(), barHeight + 4.dp.toPx()),
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                dailySummaries.forEachIndexed { index, summary ->
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                            )
                            .clickable { onSelectDay(index) }
                            .testTag("weekly_day_selector_$index"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = summary.dayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageRowCard(
    app: AppUsageEntry,
    iconDrawable: Drawable?,
    maxReferenceMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val catColor = categoryColor(app.category)
    val iconBitmap = remember(app.packageName, iconDrawable) {
        iconDrawable?.toSafeImageBitmap()
    }
    val limitMinutes = app.dailyLimitMinutes
    val appMinutes = app.totalMinutes
    val isOverLimit = limitMinutes != null && appMinutes > limitMinutes
    val progress = if (limitMinutes != null && limitMinutes > 0) {
        (appMinutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1f)
    } else {
        (app.totalTimeMillis.toFloat() / maxReferenceMillis.coerceAtLeast(1L).toFloat()).coerceIn(0.04f, 1f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("app_row_${app.packageName}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = "${app.appName} icon",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(catColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = catColor,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDurationMillis(app.totalTimeMillis),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isOverLimit) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${app.category.label} · ${app.launchCount} opens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (limitMinutes != null) {
                        Text(
                            text = if (isOverLimit) "Over ${formatDurationMinutes(limitMinutes)} cap"
                            else "Cap ${formatDurationMinutes(limitMinutes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOverLimit) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape),
                    color = if (isOverLimit) MaterialTheme.colorScheme.secondary else catColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SampleAiNoticeBanner(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.sample_ai_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
fun AppLimitDialog(
    app: AppUsageEntry,
    onSave: (Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var sliderMinutes by remember(app.packageName) {
        mutableFloatStateOf((app.dailyLimitMinutes ?: 45).toFloat())
    }
    val roundedMinutes = ((sliderMinutes / 5f).roundToInt() * 5).coerceIn(10, 240)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Daily Budget · ${app.appName}",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Text(
                    text = "Set a mindful daily ceiling for ${app.appName}. Stillness flags when you cross this threshold.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = formatDurationMinutes(roundedMinutes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = sliderMinutes,
                    onValueChange = { sliderMinutes = it },
                    valueRange = 10f..240f,
                    modifier = Modifier.testTag("app_limit_slider"),
                )
                if (app.dailyLimitMinutes != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("remove_app_limit_button"),
                    ) {
                        Text("Remove Daily Budget")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(roundedMinutes) },
                modifier = Modifier.testTag("save_app_limit_button"),
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_app_limit_button"),
            ) {
                Text("Cancel")
            }
        },
    )
}

private fun Drawable.toSafeImageBitmap(): ImageBitmap? {
    return try {
        if (this is BitmapDrawable && bitmap != null) {
            bitmap.asImageBitmap()
        } else {
            val width = intrinsicWidth.takeIf { it > 0 } ?: 96
            val height = intrinsicHeight.takeIf { it > 0 } ?: 96
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            bmp.asImageBitmap()
        }
    } catch (_: Exception) {
        null
    }
}
