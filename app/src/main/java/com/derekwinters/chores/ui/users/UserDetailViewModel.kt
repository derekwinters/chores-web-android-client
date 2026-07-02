package com.derekwinters.chores.ui.users

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.data.model.PersonStats
import com.derekwinters.chores.data.model.Redemption
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.LogRepository
import com.derekwinters.chores.data.repository.PeopleRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Issue #17: all the data a User Detail screen needs, fetched together. */
data class UserDetailData(
    val stats: PersonStats,
    val redemptions: List<Redemption>,
    val activity: List<LogEntry>
)

/**
 * Issue #17 behaviors: stats panel, redeem-points flow, redemption history, and a chore-activity
 * feed filtered to `completed`/`skipped`/`reassigned` actions for this person.
 */
@HiltViewModel
class UserDetailViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val logRepository: LogRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val personId: Int = checkNotNull(savedStateHandle.get<Int>("personId")) { "UserDetailScreen requires a personId nav arg" }

    /**
     * chores-web's points-stats/history endpoints (`GET /v1/points/stats/{person}`, `GET
     * /v1/points/{person}`) are keyed by username, not the numeric person id used everywhere
     * else on this screen (redeem, redemption history, person CRUD). Every caller that navigates
     * here (Dashboard card tap) already supplies both nav args, so this is required rather than
     * optional like [personId]'s sibling.
     */
    val username: String = checkNotNull(savedStateHandle.get<String>("username")) { "UserDetailScreen requires a username nav arg" }

    private val _uiState = MutableStateFlow<UiState<UserDetailData>>(UiState.Loading)
    val uiState: StateFlow<UiState<UserDetailData>> = _uiState.asStateFlow()

    private val _redeemState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val redeemState: StateFlow<UiState<Unit>> = _redeemState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val statsResult = peopleRepository.getPersonStats(username)
            val stats = statsResult.getOrElse { error ->
                _uiState.value = UiState.Error(errorMessage(error))
                return@launch
            }
            val redemptions = peopleRepository.getRedemptions(personId).getOrDefault(emptyList())
            val activity = logRepository.getLog(person = username).getOrNull()?.entries?.filter {
                it.action in ACTIVITY_ACTIONS
            } ?: emptyList()

            _uiState.value = UiState.Success(UserDetailData(stats, redemptions, activity))
        }
    }

    /** Issue #17: "enter amount (validated: numeric, >0, ≤ available)". */
    fun validateRedeemAmount(amountText: String): String? {
        val amount = amountText.toIntOrNull() ?: return "Enter a valid number"
        if (amount <= 0) return "Amount must be greater than 0"
        val available = (uiState.value as? UiState.Success)?.data?.stats?.availablePoints ?: 0
        if (amount > available) return "Amount exceeds available points"
        return null
    }

    fun redeem(amount: Int) {
        _redeemState.value = UiState.Loading
        viewModelScope.launch {
            peopleRepository.redeemPoints(personId, amount)
                .onSuccess {
                    _redeemState.value = UiState.Success(Unit)
                    load()
                }
                .onFailure { error -> _redeemState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun clearRedeemState() {
        _redeemState.value = UiState.Idle
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR

    private companion object {
        val ACTIVITY_ACTIONS = setOf("completed", "skipped", "reassigned")
    }
}
