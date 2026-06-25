package com.aninova.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aninova.app.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController, startDestination: String = Screen.Home.route) {
    NavHost(navController = navController, startDestination = startDestination) {

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

        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        composable(
            route = Screen.AnimeDetail.route,
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
        ) { backStackEntry ->
            AnimeDetailScreen(
                navController = navController,
                slug = backStackEntry.arguments?.getString("slug") ?: "",
            )
        }

        composable(
            route = Screen.Episode.route,
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
        ) { backStackEntry ->
            EpisodeScreen(
                navController = navController,
                slug = backStackEntry.arguments?.getString("slug") ?: "",
            )
        }

        composable(
            route = Screen.GenreList.route,
            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
        ) { backStackEntry ->
            GenreScreen(
                navController = navController,
                slug = backStackEntry.arguments?.getString("slug") ?: "",
            )
        }
    }
}
