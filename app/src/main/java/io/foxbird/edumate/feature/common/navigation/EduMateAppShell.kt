package io.foxbird.edumate.feature.common.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import io.foxbird.edumate.feature.settings.ModelManagerScreen
import io.foxbird.edumate.feature.settings.SettingsScreen
import io.foxbird.edumate.feature.worksheet.WorksheetScreen
import io.foxbird.edumate.ui.theme.StarfieldBackground
import io.foxbird.edumate.ui.theme.appColors
import org.koin.compose.koinInject

private val topLevelRoutes = setOf(NavRoutes.HOME, NavRoutes.HISTORY, NavRoutes.LIBRARY, NavRoutes.SETTINGS)

@Composable
fun EduMateAppShell() {
    val prefsManager: UserPreferencesManager = koinInject()
    val prefs by prefsManager.preferencesFlow.collectAsStateWithLifecycle(initialValue = AppPreferences())

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTopLevel = currentRoute in topLevelRoutes

    val startDestination = if (prefs.onboardingComplete) NavRoutes.HOME else NavRoutes.ONBOARDING

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (isTopLevel) {
                CurvedBottomBar(
                    selectedRoute = currentRoute,
                    onNavigate = { route ->
                        if (route == "new_chat") {
                            navController.navigate(NavRoutes.chatDetail(-1L))
                        } else {
                            navController.navigate(route) {
                                popUpTo(NavRoutes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    ) { innerPadding ->
        val bottomPad = innerPadding.calculateBottomPadding()
        val bgColor = MaterialTheme.colorScheme.background
        val starAlpha = MaterialTheme.appColors.starfieldAlpha
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Explicit background on the Box — guarantees stars have an opaque layer
                // to render against regardless of how inner Scaffolds handle transparency.
                .background(bgColor)
        ) {
            StarfieldBackground(
                alpha    = starAlpha,
                modifier = Modifier.fillMaxSize(),
            )
            NavHost(
                navController    = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPad)
                    .consumeWindowInsets(PaddingValues(bottom = bottomPad)),
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
                        onNavigateToChat          = { navController.navigate(NavRoutes.chatDetail(-1L)) },
                        onNavigateToLibrary       = {
                            navController.navigate(NavRoutes.LIBRARY) {
                                popUpTo(NavRoutes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToWorksheet     = { navController.navigate(NavRoutes.WORKSHEET) },
                        onNavigateToKnowledgeGraph = { navController.navigate(NavRoutes.KNOWLEDGE_GRAPH) },
                        onNavigateToMaterialDetail = { id -> navController.navigate(NavRoutes.materialDetail(id)) },
                        onNavigateToModelManager  = { navController.navigate(NavRoutes.MODEL_MANAGER) }
                    )
                }
                composable(NavRoutes.HISTORY) {
                    ChatHistoryScreen(
                        onChatClick = { id -> navController.navigate(NavRoutes.chatDetail(id)) },
                        onNewChat   = { navController.navigate(NavRoutes.chatDetail(-1L)) }
                    )
                }
                composable(NavRoutes.LIBRARY) {
                    MaterialsScreen(
                        onMaterialClick = { id -> navController.navigate(NavRoutes.materialDetail(id)) },
                        onBack = if (navController.previousBackStackEntry != null) {
                            { navController.popBackStack() }
                        } else null
                    )
                }
                composable(NavRoutes.SETTINGS) {
                    SettingsScreen(
                        onNavigateToDevTools    = { navController.navigate(NavRoutes.DEV_TOOLS) },
                        onNavigateToModelManager = { navController.navigate(NavRoutes.MODEL_MANAGER) }
                    )
                }
                composable(
                    NavRoutes.CHAT_DETAIL,
                    arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val convId = backStackEntry.arguments?.getLong("conversationId") ?: -1L
                    ChatScreen(conversationId = convId, onBack = { navController.popBackStack() })
                }
                composable(
                    NavRoutes.MATERIAL_DETAIL,
                    arguments = listOf(navArgument("materialId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val matId = backStackEntry.arguments?.getLong("materialId") ?: return@composable
                    MaterialDetailScreen(materialId = matId, onBack = { navController.popBackStack() })
                }
                composable(NavRoutes.KNOWLEDGE_GRAPH) { KnowledgeGraphScreen() }
                composable(NavRoutes.WORKSHEET) { WorksheetScreen(onBack = { navController.popBackStack() }) }
                composable(NavRoutes.DEV_TOOLS) { DevToolsScreen(onBack = { navController.popBackStack() }) }
                composable(NavRoutes.MODEL_MANAGER) { ModelManagerScreen(onBack = { navController.popBackStack() }) }
            }
        }
    }
}
