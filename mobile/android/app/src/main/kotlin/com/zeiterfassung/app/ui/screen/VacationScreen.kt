package com.zeiterfassung.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zeiterfassung.app.R
import com.zeiterfassung.app.data.model.VacationRequest
import com.zeiterfassung.app.ui.viewmodel.VacationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationScreen(
    viewModel: VacationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.vacation)) }) },
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
                    Text(
                        text = stringResource(R.string.error_network),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        state.balance?.let { balance ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(R.string.vacation_remaining),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        BalanceItem(label = stringResource(R.string.balance_total), value = "%.1f d".format(balance.totalDays))
                                        BalanceItem(label = stringResource(R.string.balance_used), value = "%.1f d".format(balance.usedDays))
                                        BalanceItem(
                                            label = stringResource(R.string.balance_remaining),
                                            value = "%.1f d".format(balance.remainingDays),
                                            color = if (balance.remainingDays < 5) MaterialTheme.colorScheme.error else Color.Unspecified,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.requests.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.vacation_requests),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(state.requests) { request ->
                            VacationRequestCard(request = request)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceItem(
    label: String,
    value: String,
    color: Color = Color.Unspecified,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun VacationRequestCard(request: VacationRequest) {
    val statusColor = when (request.status) {
        "APPROVED" -> Color(0xFF2E7D32)
        "REJECTED" -> Color(0xFFC62828)
        "PENDING" -> Color(0xFFF57F17)
        else -> Color.Gray
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${request.startDate} – ${request.endDate}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = request.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
            }
            Text(
                text = "%.1f %s".format(request.totalDays, stringResource(R.string.days)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!request.rejectionReason.isNullOrBlank()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = request.rejectionReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
