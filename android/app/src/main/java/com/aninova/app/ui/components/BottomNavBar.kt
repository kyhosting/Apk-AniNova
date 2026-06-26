package com.aninova.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aninova.app.ui.navigation.Screen
import com.aninova.app.ui.theme.*

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val route: String,
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Outlined.Home, Icons.Filled.Home, Screen.Home.route),
    BottomNavItem("Search", Icons.Outlined.Search, Icons.Filled.Search, Screen.Search.route),
    BottomNavItem("Watchlist", Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark, Screen.Watchlist.route),
    BottomNavItem("Profil", Icons.Outlined.PersonOutline, Icons.Filled.Person, Screen.Profile.route),
)

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Surface,
        contentColor = OnSurface,
        tonalElevation = 0.dp,
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        if (selected) item.iconSelected else item.icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Primary,
                    selectedTextColor = Primary,
                    indicatorColor = Primary.copy(alpha = 0.12f),
                    unselectedIconColor = OnSurfaceVariant,
                    unselectedTextColor = OnSurfaceVariant,
                ),
            )
        }
    }
}
