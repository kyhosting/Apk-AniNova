package com.aninova.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aninova.app.ui.screens.*

private const val DURATION = 320

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = Screen.Home.route) {
    NavHost(
        navController       = navController,
        startDestination    = startDestination,
        enterTransition     = { fadeIn(tween(DURATION)) },
        exitTransition      = { fadeOut(tween(DURATION / 2)) },
        popEnterTransition  = { fadeIn(tween(DURATION)) },
        popExitTransition   = { fadeOut(tween(DURATION / 2)) },
    ) {

        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
        }
        composable(Screen.Watchlist.route) {
            WatchlistScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }

        composable(
            route = Screen.Login.route,
            enterTransition  = { slideInVertically(tween(DURATION)) { it } + fadeIn(tween(DURATION)) },
            popExitTransition = { slideOutVertically(tween(DURATION)) { it } + fadeOut(tween(DURATION)) },
        ) {
            LoginScreen(navController = navController)
        }

        composable(
            route = Screen.Register.route,
            enterTransition  = { slideInVertically(tween(DURATION)) { it } + fadeIn(tween(DURATION)) },
            popExitTransition = { slideOutVertically(tween(DURATION)) { it } + fadeOut(tween(DURATION)) },
        ) {
            RegisterScreen(navController = navController)
        }

        composable(
            route      = Screen.AnimeDetail.route,
            arguments  = listOf(navArgument("slug") { type = NavType.StringType }),
            enterTransition  = { slideInHorizontally(tween(DURATION)) { it } + fadeIn(tween(DURATION)) },
            popExitTransition = { slideOutHorizontally(tween(DURATION)) { it } + fadeOut(tween(DURATION)) },
        ) { back ->
            AnimeDetailScreen(
                navController = navController,
                slug          = back.arguments?.getString("slug") ?: "",
            )
        }

        composable(
            route      = Screen.Episode.route,
            arguments  = listOf(navArgument("slug") { type = NavType.StringType }),
            enterTransition  = { slideInVertically(tween(DURATION)) { it / 2 } + fadeIn(tween(DURATION)) },
            popExitTransition = { slideOutVertically(tween(DURATION)) { it / 2 } + fadeOut(tween(DURATION)) },
        ) { back ->
            EpisodeScreen(
                navController = navController,
                slug          = back.arguments?.getString("slug") ?: "",
            )
        }

        composable(
            route      = Screen.GenreList.route,
            arguments  = listOf(navArgument("slug") { type = NavType.StringType }),
            enterTransition  = { slideInHorizontally(tween(DURATION)) { it } + fadeIn(tween(DURATION)) },
            popExitTransition = { slideOutHorizontally(tween(DURATION)) { it } + fadeOut(tween(DURATION)) },
        ) { back ->
            GenreScreen(
                navController = navController,
                slug          = back.arguments?.getString("slug") ?: "",
            )
        }
    }
}
