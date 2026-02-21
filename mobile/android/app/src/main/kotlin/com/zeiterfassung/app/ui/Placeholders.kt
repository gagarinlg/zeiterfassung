package com.zeiterfassung.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zeiterfassung.app.R

@Composable
fun LoginScreenPlaceholder(onLoginSuccess: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onLoginSuccess, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.login))
        }
    }
}

@Composable
fun DashboardScreenPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Dashboard", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun TimeTrackingScreenPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Time Tracking", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun VacationScreenPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Vacation", style = MaterialTheme.typography.headlineMedium)
    }
}
