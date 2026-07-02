package com.derekwinters.chores.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.chores.DueWithinFilter

/**
 * Issue #12: chores-web's default screen (Board) — a grid (here, a column on phone-width
 * screens) of per-person cards with 7d/30d progress and Due Now/Due Soon deep links into Chores.
 *
 * Thin Hilt-wired wrapper around [DashboardContent].
 *
 * @param onNavigateToChores invoked with (assignee, dueWithin) when a Due Now/Due Soon count is
 *   tapped; `dueWithin` is null for Due Now (today incl. overdue) and
 *   [DueWithinFilter.NEXT_3_DAYS]'s name for Due Soon (issue #12's default `due_soon_days`
 *   window, matched at the ViewModel level to the actual config value for counting, and
 *   approximated here for the deep-link filter since Chores' filter only has fixed buckets).
 */
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToChores: (assignee: String?, dueWithin: String?) -> Unit = { _, _ -> },
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardContent(modifier = modifier, uiState = uiState, onNavigateToChores = onNavigateToChores)
}

@Composable
fun DashboardContent(
    uiState: UiState<List<DashboardCard>>,
    onNavigateToChores: (assignee: String?, dueWithin: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                if (uiState.data.isEmpty()) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(R.string.dashboard_empty)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.data, key = { it.personId }) { card ->
                            DashboardUserCard(card = card, onNavigateToChores = onNavigateToChores)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardUserCard(
    card: DashboardCard,
    onNavigateToChores: (assignee: String?, dueWithin: String?) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = card.initial, color = MaterialTheme.colorScheme.onPrimary)
                }
                Text(
                    text = card.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            ProgressRow(
                label = stringResource(R.string.dashboard_7_day_progress, card.points7d, card.goal7d),
                progress = progressFraction(card.points7d, card.goal7d),
                trend = card.trend7d
            )
            ProgressRow(
                label = stringResource(R.string.dashboard_30_day_progress, card.points30d, card.goal30d),
                progress = progressFraction(card.points30d, card.goal30d),
                trend = card.trend30d
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { onNavigateToChores(card.username, null) }) {
                    Text(stringResource(R.string.dashboard_due_now_format, card.dueNowCount))
                }
                TextButton(onClick = { onNavigateToChores(card.username, DueWithinFilter.NEXT_3_DAYS.name) }) {
                    Text(stringResource(R.string.dashboard_due_soon_format, card.dueSoonCount))
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(label: String, progress: Float, trend: TrendStatus) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = trendColor(trend)
        )
    }
}

@Composable
private fun trendColor(trend: TrendStatus): Color = when (trend) {
    TrendStatus.SUCCESS -> MaterialTheme.colorScheme.primary
    TrendStatus.WARNING -> Color(0xFFF9A825)
    TrendStatus.ERROR -> MaterialTheme.colorScheme.error
}

private fun progressFraction(current: Int, goal: Int): Float =
    if (goal <= 0) 0f else (current.toFloat() / goal).coerceIn(0f, 1f)
