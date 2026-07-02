package com.derekwinters.chores.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Thin Compose-facing wrapper around the app-scoped [SessionManager] singleton (issue #5
 * grilling: "Login screen gates the existing bottom-nav Scaffold"). [SessionManager] itself
 * isn't a ViewModel — it's also used from [com.derekwinters.chores.data.network.UnauthorizedInterceptor]
 * on an OkHttp thread — so this just exposes its StateFlow for `collectAsStateWithLifecycle()`.
 *
 * Also owns the manual Logout action (issue #10: "user menu ... with a Logout action") since
 * that's the other session-lifecycle operation alongside the 401-triggered one.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionManager: SessionManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    val isAuthenticated: StateFlow<Boolean> = sessionManager.isAuthenticated

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
