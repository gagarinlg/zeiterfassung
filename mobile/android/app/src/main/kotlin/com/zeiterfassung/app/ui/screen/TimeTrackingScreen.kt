package com.zeiterfassung.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeiterfassung.app.R
import com.zeiterfassung.app.ui.viewmodel.TimeTrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTrackingScreen(
    viewModel: TimeTrackingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.actionError) {
        if (state.actionError != null) {
            snackbarHostState.showSnackbar("Action failed. Please try again.")
            viewModel.clearActionError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.time_tracking)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.error_network),
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadStatus() }) {
                            Text(stringResource(R.string.loading))
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    val status = state.status
                    val trackingStatus = status?.status ?: "CLOCKED_OUT"
                    val statusLabel = when (trackingStatus) {
                        "CLOCKED_IN" -> stringResource(R.string.status_clocked_in)
                        "ON_BREAK" -> stringResource(R.string.status_on_break)
                        else -> stringResource(R.string.status_clocked_out)
                    }

                    StatusCard(
                        statusLabel = statusLabel,
                        status = trackingStatus,
                        elapsedWorkMinutes = status?.elapsedWorkMinutes ?: 0,
                        elapsedBreakMinutes = status?.elapsedBreakMinutes ?: 0,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    when (trackingStatus) {
                        "CLOCKED_OUT" -> {
                            Button(
                                onClick = { viewModel.clockIn() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isActionLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            ) {
                                if (state.isActionLoading) {
                                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Text(stringResource(R.string.clock_in))
                                }
                            }
                        }

                        "CLOCKED_IN" -> {
                            Button(
                                onClick = { viewModel.startBreak() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isActionLoading,
                            ) {
                                Text(stringResource(R.string.break_start))
                            }
                            OutlinedButton(
                                onClick = { viewModel.clockOut() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isActionLoading,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Text(stringResource(R.string.clock_out))
                            }
                        }

                        "ON_BREAK" -> {
                            Button(
                                onClick = { viewModel.endBreak() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isActionLoading,
                            ) {
                                Text(stringResource(R.string.break_end))
                            }
                            OutlinedButton(
                                onClick = { viewModel.clockOut() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isActionLoading,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Text(stringResource(R.string.clock_out))
                            }
                        }
                    }

                    status?.let {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.today_hours),
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("${stringResource(R.string.today_work)}: ${formatMinutes(it.todayWorkMinutes)}")
                                    Text("${stringResource(R.string.today_break)}: ${formatMinutes(it.todayBreakMinutes)}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    statusLabel: String,
    status: String,
    elapsedWorkMinutes: Int,
    elapsedBreakMinutes: Int,
) {
    val statusColor = when (status) {
        "CLOCKED_IN" -> Color(0xFF2E7D32)
        "ON_BREAK" -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = statusColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (status != "CLOCKED_OUT") {
                Text(
                    text = if (status == "ON_BREAK") formatMinutes(elapsedBreakMinutes) else formatMinutes(elapsedWorkMinutes),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "%dh %02dm".format(h, m)
}
