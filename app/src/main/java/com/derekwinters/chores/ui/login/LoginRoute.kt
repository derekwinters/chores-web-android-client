package com.derekwinters.chores.ui.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Hilt-wired entry point for [LoginScreen]. Successful login updates
 * [com.derekwinters.chores.auth.SessionManager]'s auth state, which the root composable observes
 * to swap away from Login — no explicit navigation call needed here.
 */
@Composable
fun LoginRoute(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreen(
        modifier = modifier,
        uiState = uiState,
        onLogin = viewModel::login
    )
}
