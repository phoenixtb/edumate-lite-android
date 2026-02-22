package io.foxbird.edumate.feature.common.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.foxbird.edumate.data.preferences.AppPreferences
import io.foxbird.edumate.data.preferences.UserPreferencesManager
import io.foxbird.edumate.feature.chat.ChatHistoryScreen
import io.foxbird.edumate.feature.chat.ChatScreen
import io.foxbird.edumate.feature.home.HomeScreen
import io.foxbird.edumate.feature.knowledge.KnowledgeGraphScreen
import io.foxbird.edumate.feature.materials.MaterialDetailScreen
import io.foxbird.edumate.feature.materials.MaterialsScreen
import io.foxbird.edumate.feature.onboarding.OnboardingScreen
import io.foxbird.edumate.feature.settings.DevToolsScreen
import io.foxbird.edumate.feature.settings.SettingsScreen
import io.foxbird.edumate.feature.worksheet.WorksheetScreen
import org.koin.compose.koinInject

@Composable
fun EduMateAppShell() {
    val prefsManager: UserPreferencesManager = koinInject()
    val prefs by prefsManager.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = AppPreferences()
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val topLevelDestinations = TopLevelDestination.entries

    val isTopLevel = topLevelDestinations.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    val startDestination = if (prefs.onboardingComplete) NavRoutes.HOME else NavRoutes.ONBOARDING

    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    val items = topLevelDestinations
                    val fabInsertIndex = 2 // After HISTORY, before LIBRARY

                    items.forEachIndexed { index, destination ->
                        if (index == fabInsertIndex) {
                            // Spacer for center FAB
                            Spacer(modifier = Modifier.width(56.dp))
                        }

                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTopLevel) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.offset(y = 48.dp)
                ) {
                    FloatingActionButton(
                        onClick = { navController.navigate(NavRoutes.chatDetail(-1L)) },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 12.dp
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavRoutes.HOME) {
                HomeScreen(
                    onNavigateToChat = { navController.navigate(NavRoutes.chatDetail(-1L)) },
                    onNavigateToLibrary = { navController.navigate(NavRoutes.LIBRARY) },
                    onNavigateToWorksheet = { navController.navigate(NavRoutes.WORKSHEET) },
                    onNavigateToKnowledgeGraph = { navController.navigate(NavRoutes.KNOWLEDGE_GRAPH) },
                    onNavigateToMaterialDetail = { id -> navController.navigate(NavRoutes.materialDetail(id)) }
                )
            }
            composable(NavRoutes.HISTORY) {
                ChatHistoryScreen(
                    onChatClick = { id -> navController.navigate(NavRoutes.chatDetail(id)) },
                    onNewChat = { navController.navigate(NavRoutes.chatDetail(-1L)) }
                )
            }
            composable(NavRoutes.LIBRARY) {
                MaterialsScreen(
                    onMaterialClick = { id -> navController.navigate(NavRoutes.materialDetail(id)) }
                )
            }
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                    onNavigateToDevTools = { navController.navigate(NavRoutes.DEV_TOOLS) }
                )
            }
            composable(
                NavRoutes.CHAT_DETAIL,
                arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
            ) { backStackEntry ->
                val convId = backStackEntry.arguments?.getLong("conversationId") ?: -1L
                ChatScreen(
                    conversationId = convId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                NavRoutes.MATERIAL_DETAIL,
                arguments = listOf(navArgument("materialId") { type = NavType.LongType })
            ) { backStackEntry ->
                val matId = backStackEntry.arguments?.getLong("materialId") ?: return@composable
                MaterialDetailScreen(
                    materialId = matId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.KNOWLEDGE_GRAPH) {
                KnowledgeGraphScreen()
            }
            composable(NavRoutes.WORKSHEET) {
                WorksheetScreen(onBack = { navController.popBackStack() })
            }
            composable(NavRoutes.DEV_TOOLS) {
                DevToolsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
