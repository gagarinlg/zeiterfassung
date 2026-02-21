package com.zeiterfassung.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
            LoginScreenPlaceholder(onLoginSuccess = {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Dashboard.route) {
            DashboardScreenPlaceholder()
        }
        composable(Screen.TimeTracking.route) {
            TimeTrackingScreenPlaceholder()
        }
        composable(Screen.Vacation.route) {
            VacationScreenPlaceholder()
        }
    }
}
