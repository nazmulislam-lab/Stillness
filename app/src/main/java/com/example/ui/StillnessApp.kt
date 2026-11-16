package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.AppLimitDialog
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WeeklyScreen

const val ROUTE_TODAY = "today"
const val ROUTE_WEEKLY = "weekly"
const val ROUTE_APPS = "apps"
const val ROUTE_GOALS = "goals"

private data class StillnessDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val destinations = listOf(
    StillnessDestination(
        route = ROUTE_TODAY,
        labelRes = R.string.nav_today,
        selectedIcon = Icons.Filled.Today,
        unselectedIcon = Icons.Outlined.Today,
        testTag = "nav_item_today",
    ),
    StillnessDestination(
        route = ROUTE_WEEKLY,
        labelRes = R.string.nav_weekly,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_item_weekly",
    ),
    StillnessDestination(
        route = ROUTE_APPS,
        labelRes = R.string.nav_apps,
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps,
        testTag = "nav_item_apps",
    ),
    StillnessDestination(
        route = ROUTE_GOALS,
        labelRes = R.string.nav_goals,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_item_goals",
    ),
)

@Composable
fun StillnessApp(
    viewModel: StillnessViewModel = viewModel(
        factory = StillnessViewModel.provideFactory(LocalContext.current),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUsageStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openUsageSettings = {
        runCatching {
            context.startActivity(viewModel.createUsageAccessIntent())
        }
        Unit
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        if (useRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    destinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        val label = stringResource(dest.labelRes)
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = label,
                                )
                            },
                            label = { Text(label) },
                            modifier = Modifier.testTag(dest.testTag),
                        )
                    }
                }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets.safeDrawing,
                ) { innerPadding ->
                    StillnessNavHost(
                        navController = navController,
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenUsageSettings = openUsageSettings,
                        onNavigateTab = navigateToTab,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        destinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(dest.testTag),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                StillnessNavHost(
                    navController = navController,
                    uiState = uiState,
                    viewModel = viewModel,
                    onOpenUsageSettings = openUsageSettings,
                    onNavigateTab = navigateToTab,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }

    uiState.editingLimitApp?.let { app ->
        AppLimitDialog(
            app = app,
            onSave = { limitMinutes ->
                viewModel.saveAppLimit(app.packageName, app.appName, limitMinutes)
            },
            onRemove = {
                viewModel.removeAppLimit(app.packageName)
            },
            onDismiss = { viewModel.openLimitEditor(null) },
        )
    }
}

@Composable
private fun StillnessNavHost(
    navController: NavHostController,
    uiState: StillnessUiState,
    viewModel: StillnessViewModel,
    onOpenUsageSettings: () -> Unit,
    onNavigateTab: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ROUTE_TODAY,
        modifier = modifier,
    ) {
        composable(ROUTE_TODAY) {
            TodayScreen(
                uiState = uiState,
                getAppIcon = viewModel::getAppIcon,
                onOpenUsageSettings = onOpenUsageSettings,
                onRefresh = viewModel::refreshUsageStats,
                onOpenAllApps = { onNavigateTab(ROUTE_APPS) },
                onOpenWeekly = { onNavigateTab(ROUTE_WEEKLY) },
                onEditAppLimit = viewModel::openLimitEditor,
            )
        }
        composable(ROUTE_WEEKLY) {
            WeeklyScreen(
                uiState = uiState,
                onSelectDay = viewModel::selectDayIndex,
                onRefreshSuggestions = viewModel::regenerateWeeklySuggestions,
                onToggleSuggestion = viewModel::toggleSuggestionCompleted,
            )
        }
        composable(ROUTE_APPS) {
            AppsScreen(
                uiState = uiState,
                getAppIcon = viewModel::getAppIcon,
                onOpenUsageSettings = onOpenUsageSettings,
                onSearchChange = viewModel::updateSearchQuery,
                onSelectCategory = viewModel::selectCategory,
                onToggleSort = viewModel::updateSortOption,
                onSelectTimeframe = viewModel::updateTimeframe,
                onEditAppLimit = viewModel::openLimitEditor,
            )
        }
        composable(ROUTE_GOALS) {
            GoalsScreen(
                uiState = uiState,
                onUpdateDailyGoal = viewModel::updateDailyGoalMinutes,
                onUpdateWindDownHour = viewModel::updateWindDownHour,
                onUpdateWeeklyReviewDay = viewModel::updateWeeklyReviewDay,
                onRemoveAppLimit = viewModel::removeAppLimit,
                onOpenUsageSettings = onOpenUsageSettings,
            )
        }
    }
}
