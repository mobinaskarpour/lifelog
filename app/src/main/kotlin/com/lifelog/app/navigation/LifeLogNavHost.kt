package com.lifelog.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lifelog.feature.apps.AppsScreen
import com.lifelog.feature.calls.CallsScreen
import com.lifelog.feature.dashboard.DashboardScreen
import com.lifelog.feature.export.GlobalSearchScreen
import com.lifelog.feature.location.LocationScreen
import com.lifelog.feature.notifications.NotificationsScreen
import com.lifelog.feature.permissions.OnboardingScreen
import com.lifelog.feature.settings.AboutScreen
import com.lifelog.feature.settings.SettingsScreen
import com.lifelog.feature.settings.StatisticsScreen
import com.lifelog.feature.sms.SmsConversationScreen
import com.lifelog.feature.sms.SmsScreen
import com.lifelog.feature.timeline.TimelineScreen
import com.lifelog.ui.navigation.LifeLogRoutes

data class MainNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun LifeLogNavHost(
    startDestination: String,
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
    pendingRoute: String? = null,
    onRouteHandled: () -> Unit = {},
    onSaveRoute: (String) -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems =
        listOf(
            MainNavItem(LifeLogRoutes.DASHBOARD, "Dashboard", com.lifelog.ui.components.bottomNavItems[0].icon),
            MainNavItem(LifeLogRoutes.TIMELINE, "Timeline", com.lifelog.ui.components.bottomNavItems[1].icon),
            MainNavItem(LifeLogRoutes.APPS, "Apps", com.lifelog.ui.components.bottomNavItems[2].icon),
            MainNavItem(LifeLogRoutes.NOTIFICATIONS, "Alerts", com.lifelog.ui.components.bottomNavItems[3].icon),
            MainNavItem(LifeLogRoutes.SMS, "Messages", com.lifelog.ui.components.bottomNavItems[4].icon),
        )

    val bottomNavRoutes = bottomNavItems.map { it.route }

    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            if (route in bottomNavRoutes) {
                onSaveRoute(route)
            }
        }
    }

    LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        if (route in bottomNavRoutes) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
        onRouteHandled()
    }

    val showBottomBar = currentRoute in bottomNavRoutes
    val showSearchFab = showBottomBar && currentRoute != LifeLogRoutes.SMS

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showSearchFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(LifeLogRoutes.SEARCH) },
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(LifeLogRoutes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        onOnboardingComplete()
                        navController.navigate(LifeLogRoutes.DASHBOARD) {
                            popUpTo(LifeLogRoutes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(LifeLogRoutes.DASHBOARD) {
                DashboardScreen(
                    onNavigateToSettings = { navController.navigate(LifeLogRoutes.SETTINGS) },
                )
            }
            composable(LifeLogRoutes.TIMELINE) { TimelineScreen() }
            composable(LifeLogRoutes.APPS) { AppsScreen() }
            composable(LifeLogRoutes.NOTIFICATIONS) { NotificationsScreen() }
            composable(LifeLogRoutes.CALLS) { CallsScreen() }
            composable(LifeLogRoutes.SMS) {
                SmsScreen(
                    onThreadClick = { threadId ->
                        navController.navigate("sms/$threadId")
                    },
                    onNavigateToSettings = { navController.navigate(LifeLogRoutes.SETTINGS) },
                )
            }
            composable(
                route = LifeLogRoutes.SMS_CONVERSATION,
                arguments = listOf(navArgument("threadId") { type = NavType.LongType }),
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(280)) { fullWidth -> fullWidth }
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = tween(280)) { fullWidth -> -fullWidth / 3 }
                },
                popEnterTransition = {
                    slideInHorizontally(animationSpec = tween(280)) { fullWidth -> -fullWidth / 3 }
                },
                popExitTransition = {
                    slideOutHorizontally(animationSpec = tween(280)) { fullWidth -> fullWidth }
                },
            ) {
                SmsConversationScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(LifeLogRoutes.SETTINGS) },
                )
            }
            composable(LifeLogRoutes.LOCATION) { LocationScreen() }
            composable(LifeLogRoutes.STATISTICS) { StatisticsScreen() }
            composable(LifeLogRoutes.SETTINGS) {
                SettingsScreen(
                    onNavigateToAbout = { navController.navigate(LifeLogRoutes.ABOUT) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(LifeLogRoutes.ABOUT) { AboutScreen() }
            composable(LifeLogRoutes.SEARCH) { GlobalSearchScreen() }
        }
    }
}
