package com.derekwinters.chores.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #11 behavior: "DB-readiness gate: poll GET /status/db-status before rendering the
 * authenticated app shell ... web polls up to 60x/500ms". Scoped to the authenticated part of
 * the composable tree (see ChoresApp), so polling restarts fresh on every login.
 */
@HiltViewModel
class DbReadinessViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        pollUntilReady()
    }

    private fun pollUntilReady() {
        viewModelScope.launch {
            repeat(MAX_ATTEMPTS) {
                val ready = authRepository.isDatabaseReady().getOrDefault(false)
                if (ready) {
                    _isReady.value = true
                    return@launch
                }
                delay(POLL_INTERVAL_MS)
            }
            // Give up waiting after MAX_ATTEMPTS; reveal the app shell anyway so a genuinely
            // stuck backend surfaces its own errors through the normal screens rather than an
            // indefinite spinner.
            _isReady.value = true
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 60
        const val POLL_INTERVAL_MS = 500L
    }
}
