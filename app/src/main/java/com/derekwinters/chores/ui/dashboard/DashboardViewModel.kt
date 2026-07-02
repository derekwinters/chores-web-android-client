package com.derekwinters.chores.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.ChoreRepository
import com.derekwinters.chores.data.repository.ConfigRepository
import com.derekwinters.chores.data.repository.PeopleRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #12 behavior: "chores-web's default screen (/, pages/Dashboard.jsx) is a grid of
 * per-person cards ... Auto-refresh chores + points-summary data periodically (web refetches
 * every 60s)."
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val choreRepository: ChoreRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<DashboardCard>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<DashboardCard>>> = _uiState.asStateFlow()

    private var autoRefreshStarted = false

    init {
        refresh()
        startAutoRefresh()
    }

    fun refresh() {
        viewModelScope.launch { loadOnce(showLoading = _uiState.value !is UiState.Success) }
    }

    private fun startAutoRefresh() {
        if (autoRefreshStarted) return
        autoRefreshStarted = true
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                loadOnce(showLoading = false)
            }
        }
    }

    private suspend fun loadOnce(showLoading: Boolean) {
        if (showLoading) _uiState.value = UiState.Loading

        val peopleResult = peopleRepository.getPeople()
        val people = peopleResult.getOrElse { error ->
            _uiState.value = UiState.Error(errorMessage(error))
            return
        }

        val pointsSummaries = peopleRepository.getPointsSummary().getOrDefault(emptyList())
        val chores = choreRepository.getChores().getOrDefault(emptyList())
        val dueSoonDays = configRepository.getConfig().getOrNull()?.dueSoonDays ?: 3

        _uiState.value = UiState.Success(buildDashboardCards(people, pointsSummaries, chores, dueSoonDays))
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR

    private companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 60_000L
    }
}
