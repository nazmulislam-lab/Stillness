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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AppCategory
import com.example.model.AppUsageEntry
import com.example.ui.AppSortOption
import com.example.ui.AppsTimeframe
import com.example.ui.StillnessUiState
import com.example.ui.components.AppUsageRowCard
import com.example.ui.components.UsageAccessOnboardingCard

@Composable
fun AppsScreen(
    uiState: StillnessUiState,
    getAppIcon: (String) -> Drawable?,
    onOpenUsageSettings: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (AppCategory?) -> Unit,
    onToggleSort: (AppSortOption) -> Unit,
    onSelectTimeframe: (AppsTimeframe) -> Unit,
    onEditAppLimit: (AppUsageEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseApps = remember(uiState.snapshot, uiState.appsTimeframe) {
        when (uiState.appsTimeframe) {
            AppsTimeframe.TODAY -> uiState.todaySummary?.apps.orEmpty()
            AppsTimeframe.WEEK -> uiState.snapshot?.topApps.orEmpty()
        }
    }

    val filteredApps = remember(
        baseApps,
        uiState.appsSearchQuery,
        uiState.selectedCategory,
        uiState.appsSortOption,
    ) {
        val q = uiState.appsSearchQuery.trim()
        baseApps
            .filter { app ->
                (uiState.selectedCategory == null || app.category == uiState.selectedCategory) &&
                    (q.isEmpty() || app.appName.contains(q, ignoreCase = true))
            }
            .let { list ->
                when (uiState.appsSortOption) {
                    AppSortOption.MOST_TIME -> list.sortedByDescending { it.totalTimeMillis }
                    AppSortOption.MOST_LAUNCHES -> list.sortedByDescending { it.launchCount }
                }
            }
    }

    val maxRefMillis = filteredApps.maxOfOrNull { it.totalTimeMillis } ?: 1L

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
            ) {
                Text(
                    text = "APP ACTIVITY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Apps & Budgets",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap any app to set a mindful daily time budget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!uiState.hasUsageAccess) {
            item {
                UsageAccessOnboardingCard(
                    onOpenSettings = onOpenUsageSettings,
                    modifier = Modifier.widthIn(max = 680.dp),
                )
            }
        } else {
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                ) {
                    AppsTimeframe.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = uiState.appsTimeframe == option,
                            onClick = { onSelectTimeframe(option) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = AppsTimeframe.entries.size,
                            ),
                            modifier = Modifier.testTag("apps_timeframe_${option.name.lowercase()}"),
                        ) {
                            Text(option.label)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.appsSearchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search apps") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search apps",
                        )
                    },
                    trailingIcon = {
                        if (uiState.appsSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .testTag("apps_search_input"),
                )
            }

            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategory == null,
                            onClick = { onSelectCategory(null) },
                            label = { Text("All") },
                            modifier = Modifier.testTag("category_chip_all"),
                        )
                    }
                    items(AppCategory.entries) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = {
                                onSelectCategory(
                                    if (uiState.selectedCategory == category) null else category,
                                )
                            },
                            label = { Text(category.label) },
                            modifier = Modifier.testTag("category_chip_${category.name.lowercase()}"),
                        )
                    }
                }
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
                        text = "${filteredApps.size} tracked apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val nextSort = if (uiState.appsSortOption == AppSortOption.MOST_TIME) {
                        AppSortOption.MOST_LAUNCHES
                    } else {
                        AppSortOption.MOST_TIME
                    }
                    TextButton(
                        onClick = { onToggleSort(nextSort) },
                        modifier = Modifier.testTag("apps_sort_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapVert,
                            contentDescription = null,
                        )
                        Text(" ${uiState.appsSortOption.label}")
                    }
                }
            }

            if (filteredApps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Text(
                            text = "No apps match your current filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            } else {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppUsageRowCard(
                        app = app,
                        iconDrawable = getAppIcon(app.packageName),
                        maxReferenceMillis = maxRefMillis,
                        onClick = { onEditAppLimit(app) },
                        modifier = Modifier.widthIn(max = 680.dp),
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
