package com.aninova.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Watchlist : Screen("watchlist")
    object Profile : Screen("profile")
    object Login : Screen("login")
    object Register : Screen("register")
    object AnimeDetail : Screen("anime/{slug}") {
        fun createRoute(slug: String) = "anime/$slug"
    }
    object Episode : Screen("episode/{slug}") {
        fun createRoute(slug: String) = "episode/$slug"
    }
    object GenreList : Screen("genre/{slug}") {
        fun createRoute(slug: String) = "genre/$slug"
    }
}
