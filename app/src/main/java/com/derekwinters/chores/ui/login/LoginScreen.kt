package com.derekwinters.chores.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.derekwinters.chores.R
import com.derekwinters.chores.common.UiState

/**
 * Login screen: server URL + username/password fields, calling [onLogin] on submit.
 *
 * Stateless (form field state aside) so it can be unit tested without Hilt — the real ViewModel
 * wiring lives in [LoginRoute].
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    uiState: UiState<Unit>?,
    onLogin: (serverUrl: String, username: String, password: String) -> Unit
) {
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val isLoading = uiState is UiState.Loading

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            value = serverUrl,
            onValueChange = { serverUrl = it },
            singleLine = true,
            label = { Text(stringResource(R.string.login_server_url_label)) }
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            value = username,
            onValueChange = { username = it },
            singleLine = true,
            label = { Text(stringResource(R.string.login_username_label)) }
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            value = password,
            onValueChange = { password = it },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            label = { Text(stringResource(R.string.login_password_label)) }
        )

        if (uiState is UiState.Error) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            modifier = Modifier.padding(top = 16.dp),
            enabled = !isLoading,
            onClick = { onLogin(serverUrl, username, password) }
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text(stringResource(R.string.login_button))
            }
        }
    }
}
