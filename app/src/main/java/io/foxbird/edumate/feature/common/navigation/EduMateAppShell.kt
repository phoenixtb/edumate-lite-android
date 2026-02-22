package io.foxbird.edumate.feature.common.navigation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.foxbird.edumate.R
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
import np.com.susanthapa.curved_bottom_navigation.CbnMenuItem
import np.com.susanthapa.curved_bottom_navigation.CurvedBottomNavigationView
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

    val menuItems = arrayOf(
        CbnMenuItem(R.drawable.ic_nav_home, R.drawable.avd_nav_home, title = "Home"),
        CbnMenuItem(R.drawable.ic_nav_history, R.drawable.avd_nav_history, title = "History"),
        CbnMenuItem(R.drawable.ic_nav_add, R.drawable.avd_nav_add, title = ""),
        CbnMenuItem(R.drawable.ic_nav_library, R.drawable.avd_nav_library, title = "Library"),
        CbnMenuItem(R.drawable.ic_nav_settings, R.drawable.avd_nav_settings, title = "Settings"),
    )

    Scaffold(
        bottomBar = {
            if (isTopLevel) {
                val selectedIndex = when {
                    currentDestination?.hierarchy?.any { it.route == NavRoutes.HOME } == true -> 0
                    currentDestination?.hierarchy?.any { it.route == NavRoutes.HISTORY } == true -> 1
                    currentDestination?.hierarchy?.any { it.route == NavRoutes.LIBRARY } == true -> 3
                    currentDestination?.hierarchy?.any { it.route == NavRoutes.SETTINGS } == true -> 4
                    else -> 0
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E2E))
                        .navigationBarsPadding()
                ) {
                    AndroidView(
                        factory = { context ->
                            (LayoutInflater.from(context)
                                .inflate(R.layout.curved_bottom_nav, null) as CurvedBottomNavigationView)
                                .apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                    setMenuItems(menuItems, selectedIndex)
                                    setOnMenuItemClickListener { _, index ->
                                        when (index) {
                                            0 -> navController.navigate(NavRoutes.HOME) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true; restoreState = true
                                            }
                                            1 -> navController.navigate(NavRoutes.HISTORY) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true; restoreState = true
                                            }
                                            2 -> navController.navigate(NavRoutes.chatDetail(-1L))
                                            3 -> navController.navigate(NavRoutes.LIBRARY) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true; restoreState = true
                                            }
                                            4 -> navController.navigate(NavRoutes.SETTINGS) {
                                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                launchSingleTop = true; restoreState = true
                                            }
                                        }
                                    }
                                }
                        },
                        update = { view ->
                            view.onMenuItemClick(selectedIndex)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
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
