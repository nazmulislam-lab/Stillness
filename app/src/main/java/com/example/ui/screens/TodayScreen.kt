package com.example.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppUsageEntry
import com.example.ui.StillnessUiState
import com.example.ui.components.AppUsageRowCard
import com.example.ui.components.CategoryBalanceCard
import com.example.ui.components.HourlyRhythmCard
import com.example.ui.components.ScreenTimeRingCard
import com.example.ui.components.UsageAccessOnboardingCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(
    uiState: StillnessUiState,
    getAppIcon: (String) -> Drawable?,
    onOpenUsageSettings: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenWeekly: () -> Unit,
    onEditAppLimit: (AppUsageEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = uiState.todaySummary
    val dateHeader = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = dateHeader.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Daily Balance",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("refresh_today_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh usage statistics",
                    )
                }
            }
        }

        if (!uiState.hasUsageAccess) {
            item {
                UsageAccessOnboardingCard(
                    onOpenSettings = onOpenUsageSettings,
                    modifier = Modifier.widthIn(max = 680.dp),
                )
            }
        }

        if (today != null) {
            item {
                ScreenTimeRingCard(
                    todaySummary = today,
                    dailyGoalMinutes = uiState.settings.dailyGoalMinutes,
                    hasUsageAccess = uiState.hasUsageAccess,
                    modifier = Modifier.widthIn(max = 680.dp),
                )
            }

            if (uiState.hasUsageAccess) {
                item {
                    HourlyRhythmCard(
                        hourlyMinutes = today.hourlyMinutes,
                        windDownStartHour = uiState.settings.windDownStartHour,
                        modifier = Modifier.widthIn(max = 680.dp),
                    )
                }

                item {
                    CategoryBalanceCard(
                        categoryMillis = today.categoryMinutes,
                        modifier = Modifier.widthIn(max = 680.dp),
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Most Used Today",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        TextButton(
                            onClick = onOpenAllApps,
                            modifier = Modifier.testTag("view_all_apps_button"),
                        ) {
                            Text("See all")
                        }
                    }
                }

                if (today.apps.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 680.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Spa,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "Quiet day so far — no app sessions over 15 seconds logged yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    val maxMillis = today.apps.firstOrNull()?.totalTimeMillis ?: 1L
                    items(today.apps.take(5), key = { it.packageName }) { app ->
                        AppUsageRowCard(
                            app = app,
                            iconDrawable = getAppIcon(app.packageName),
                            maxReferenceMillis = maxMillis,
                            onClick = { onEditAppLimit(app) },
                            modifier = Modifier.widthIn(max = 680.dp),
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weekly Well-Being Digest",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "Review 7-day trends and mindful suggestions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        )
                    }
                    TextButton(
                        onClick = onOpenWeekly,
                        modifier = Modifier.testTag("today_to_weekly_button"),
                    ) {
                        Text("Open")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
