package com.example.hackernews.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hackernews.ui.classics.ClassicsScreen
import com.example.hackernews.ui.components.BottomNavItem
import com.example.hackernews.ui.components.TerminalBottomBar
import com.example.hackernews.ui.feed.FeedScreen
import com.example.hackernews.ui.profile.AboutScreen
import com.example.hackernews.ui.profile.ProfileScreen
import com.example.hackernews.ui.profile.bookmarks.BookmarksScreen
import com.example.hackernews.ui.profile.reading.ReadingModeScreen
import com.example.hackernews.ui.profile.topics.TopicSettingsScreen

internal const val FEED_ROUTE = "feed"
internal const val PROFILE_ROUTE = "profile"

private object Routes {
    const val FEED = FEED_ROUTE
    const val CLASSICS = "classics"
    const val PROFILE = PROFILE_ROUTE
    const val BOOKMARKS = "bookmarks"
    const val TOPICS = "topics"
    const val READING = "reading"
    const val ABOUT = "about"
}

private fun NavController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppNav(startDestination: String = FEED_ROUTE) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomRoutes = setOf(Routes.FEED, Routes.CLASSICS, Routes.PROFILE)

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomRoutes) {
                TerminalBottomBar(
                    items = listOf(
                        BottomNavItem(
                            label = "FEED",
                            selected = currentRoute == Routes.FEED,
                            onClick = { navController.switchTab(Routes.FEED) },
                        ),
                        BottomNavItem(
                            label = "CLASSICS",
                            selected = currentRoute == Routes.CLASSICS,
                            onClick = { navController.switchTab(Routes.CLASSICS) },
                        ),
                        BottomNavItem(
                            label = "PROFILE",
                            selected = currentRoute == Routes.PROFILE,
                            onClick = { navController.switchTab(Routes.PROFILE) },
                        ),
                    ),
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.FEED) { FeedScreen() }
            composable(Routes.CLASSICS) { ClassicsScreen() }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) },
                    onOpenTopics = { navController.navigate(Routes.TOPICS) },
                    onOpenReading = { navController.navigate(Routes.READING) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.BOOKMARKS) {
                BookmarksScreen(onBack = navController::popBackStack)
            }
            composable(Routes.TOPICS) {
                TopicSettingsScreen(onBack = navController::popBackStack)
            }
            composable(Routes.READING) {
                ReadingModeScreen(onBack = navController::popBackStack)
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = navController::popBackStack)
            }
        }
    }
}
