package com.derekwinters.chores.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Issue #116: Styled banner component for error/success feedback in settings.
 * Renders as a bordered, tinted box instead of plain text.
 */
@Composable
fun SettingsBanner(
    message: String,
    isError: Boolean = true,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val textColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }

    Box(
        modifier = modifier
            .border(1.dp, borderColor, MaterialTheme.shapes.small)
            .background(backgroundColor, MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Text(
            text = message,
            color = textColor,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
