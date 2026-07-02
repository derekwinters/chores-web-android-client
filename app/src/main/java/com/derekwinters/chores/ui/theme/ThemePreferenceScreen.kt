package com.derekwinters.chores.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.ui.UiState

/**
 * Issue #25: personal theme preference — a grid (here, a list) of "Default (household theme)"
 * plus every available theme; tapping applies immediately (no separate save step).
 *
 * Thin Hilt-wired wrapper around [ThemePreferenceContent].
 */
@Composable
fun ThemePreferenceScreen(modifier: Modifier = Modifier, viewModel: ThemePreferenceViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    ThemePreferenceContent(modifier = modifier, uiState = uiState, onSelectTheme = viewModel::selectTheme)
}

@Composable
fun ThemePreferenceContent(
    uiState: UiState<ThemePreferenceData>,
    onSelectTheme: (String?) -> Unit,
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
                val data = uiState.data
                // `defaultInfo` (from the non-admin-gated /v1/theme/default-info) is the household
                // default's true id/name regardless of override state; `current.theme` alone can't
                // be used for this label since it reflects the *resolved* (possibly overridden)
                // theme, not necessarily the default. Its colors are looked up from the full
                // catalog just to draw the swatch.
                val householdDefault = data.themes.firstOrNull { it.id == data.defaultInfo.id } ?: data.current.theme
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    item {
                        ThemeOptionRow(
                            name = "Default (${data.defaultInfo.name})",
                            theme = householdDefault,
                            selected = !data.current.isPersonalOverride,
                            onClick = { onSelectTheme(null) }
                        )
                    }
                    items(data.themes, key = { it.id }) { theme ->
                        ThemeOptionRow(
                            name = theme.name,
                            theme = theme,
                            selected = data.current.isPersonalOverride && data.current.theme.id == theme.id,
                            onClick = { onSelectTheme(theme.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(name: String, theme: ThemeOption, selected: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(24.dp).background(parseHexColor(theme.primary), CircleShape)
            )
            Text(name, modifier = Modifier.padding(start = 12.dp).weight(1f))
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = "Selected")
            }
        }
    }
}
