package com.derekwinters.chores.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.derekwinters.chores.auth.AuthState
import com.derekwinters.chores.ui.login.LoginRoute

/**
 * App root: gates the bottom-nav [ChoresApp] Scaffold behind the Login screen, per issue #5's
 * grilling decision. Swaps reactively (no explicit navigation call) whenever
 * [AuthGateViewModel.authState] changes — including the global-401 case, where
 * [com.derekwinters.chores.network.UnauthorizedInterceptor] clears the token from a background
 * thread and this composable observes the resulting [AuthState.LOGGED_OUT].
 */
@Composable
fun ChoresRoot(
    onSendTestNotification: () -> Unit,
    authGateViewModel: AuthGateViewModel = hiltViewModel()
) {
    val authState by authGateViewModel.authState.collectAsStateWithLifecycle()

    when (authState) {
        AuthState.LOGGED_IN -> ChoresApp(onSendTestNotification = onSendTestNotification)
        AuthState.LOGGED_OUT -> LoginRoute()
    }
}
