package com.aninova.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.aninova.app.ui.navigation.NavGraph
import com.aninova.app.ui.navigation.Screen
import com.aninova.app.ui.theme.AniNovaTheme
import com.aninova.app.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AniNovaTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                val startDest = if (isLoggedIn) Screen.Home.route else Screen.Login.route

                NavGraph(
                    navController = navController,
                    startDestination = startDest,
                )
            }
        }
    }
}
