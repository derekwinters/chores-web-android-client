package com.derekwinters.chores.ui.theme

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.ui.UiState

/**
 * Issue #25: personal theme preference — a grid of "Default (household theme)"
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

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ThemeOptionCard(
                            name = "Default (${data.defaultInfo.name})",
                            theme = householdDefault,
                            selected = !data.current.isPersonalOverride,
                            onClick = { onSelectTheme(null) }
                        )
                    }
                    items(data.themes, key = { it.id }) { theme ->
                        ThemeOptionCard(
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
private fun ThemeOptionCard(name: String, theme: ThemeOption, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                name,
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
            // Display all 4 preview colors (matching web: primary, secondary, accent, background)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorSwatch(color = theme.primary, contentDescription = "Primary color")
                ColorSwatch(color = theme.secondary, contentDescription = "Secondary color")
                ColorSwatch(color = theme.accent, contentDescription = "Accent color")
                ColorSwatch(color = theme.background, contentDescription = "Background color")
            }
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: String, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(parseHexColor(color), RoundedCornerShape(4.dp))
    ) {
        // Empty box for color display
    }
}
