package io.foxbird.edumate.feature.common.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    HISTORY("history", "History", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    // FAB placeholder — no actual tab here (index 2 is the center FAB gap)
    LIBRARY("library", "Library", Icons.AutoMirrored.Filled.LibraryBooks, Icons.AutoMirrored.Outlined.LibraryBooks),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}

object NavRoutes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val CHAT_DETAIL = "chat/{conversationId}"
    const val LIBRARY = "library"
    const val MATERIAL_DETAIL = "material/{materialId}"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"
    const val KNOWLEDGE_GRAPH = "knowledge_graph"
    const val WORKSHEET = "worksheet"
    const val DEV_TOOLS = "dev_tools"

    fun chatDetail(conversationId: Long) = "chat/$conversationId"
    fun materialDetail(materialId: Long) = "material/$materialId"
}
