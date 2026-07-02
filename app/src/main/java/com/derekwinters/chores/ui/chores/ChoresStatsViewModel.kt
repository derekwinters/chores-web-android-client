package com.derekwinters.chores.ui.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.repository.ChoreRepository
import com.derekwinters.chores.data.repository.PeopleRepository
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Issue #14: the Chores screen's collapsible stats panel numbers. */
data class ChoresStats(
    val totalEnabledChores: Int,
    val totalPoints: Int,
    val completedLast7Days: Int,
    val dueNext7DaysPoints: Int
)

/**
 * Issue #14 behavior: "Total (enabled) Chores, Total Points (sum of enabled chores' point
 * values), Completed Last 7 Days (sum from points-summary), Due Next 7 Days (sum of points for
 * enabled chores due within 7 days)". Independent from [ChoreListViewModel] so that ViewModel's
 * existing tests/behavior are untouched — this fetches its own copy of the chores list plus the
 * points summary.
 */
@HiltViewModel
class ChoresStatsViewModel @Inject constructor(
    private val choreRepository: ChoreRepository,
    private val peopleRepository: PeopleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ChoresStats>>(UiState.Loading)
    val uiState: StateFlow<UiState<ChoresStats>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val choresResult = choreRepository.getChores()
            val chores = choresResult.getOrElse { error ->
                _uiState.value = UiState.Error(errorMessage(error))
                return@launch
            }
            val completedLast7Days = peopleRepository.getPointsSummary()
                .getOrDefault(emptyList())
                .sumOf { it.points7d }

            _uiState.value = UiState.Success(computeStats(chores, completedLast7Days))
        }
    }

    private fun computeStats(chores: List<Chore>, completedLast7Days: Int): ChoresStats {
        val enabled = chores.filter { it.enabled }
        val dueNext7Days = enabled.applyFilters(ChoreFilters(dueWithin = DueWithinFilter.NEXT_7_DAYS))
        return ChoresStats(
            totalEnabledChores = enabled.size,
            totalPoints = enabled.sumOf { it.points },
            completedLast7Days = completedLast7Days,
            dueNext7DaysPoints = dueNext7Days.sumOf { it.points }
        )
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
