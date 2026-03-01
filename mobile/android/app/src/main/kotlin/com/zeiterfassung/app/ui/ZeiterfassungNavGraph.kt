package com.zeiterfassung.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zeiterfassung.app.ui.screen.DashboardScreen
import com.zeiterfassung.app.ui.screen.LoginScreen
import com.zeiterfassung.app.ui.screen.TimeTrackingScreen
import com.zeiterfassung.app.ui.screen.VacationScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object TimeTracking : Screen("time_tracking")
    object Vacation : Screen("vacation")
}

@Composable
fun ZeiterfassungNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
        composable(Screen.TimeTracking.route) {
            TimeTrackingScreen()
        }
        composable(Screen.Vacation.route) {
            VacationScreen()
        }
    }
}

