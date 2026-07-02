package com.derekwinters.chores.ui

import androidx.lifecycle.ViewModel
import com.derekwinters.chores.auth.AuthState
import com.derekwinters.chores.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Exposes [SessionManager]'s [AuthState] to [ChoresRoot], which gates the bottom-nav [ChoresApp]
 * Scaffold behind the Login screen. When [com.derekwinters.chores.network.UnauthorizedInterceptor]
 * clears the token on a global 401, this flow flips to [AuthState.LOGGED_OUT] and the root
 * composable reactively swaps back to Login.
 */
@HiltViewModel
class AuthGateViewModel @Inject constructor(
    sessionManager: SessionManager
) : ViewModel() {
    val authState: StateFlow<AuthState> = sessionManager.authState
}
