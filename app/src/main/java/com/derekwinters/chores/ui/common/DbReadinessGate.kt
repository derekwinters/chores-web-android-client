package com.derekwinters.chores.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.derekwinters.chores.R

/**
 * Issue #11: shows a "Database initializing…" state while [isReady] is false, then reveals
 * [content] — needed for freshly-started self-hosted backends whose DB migration hasn't finished
 * yet.
 */
@Composable
fun DbReadinessGate(isReady: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (isReady) {
        content()
        return
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = stringResource(R.string.database_initializing),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
