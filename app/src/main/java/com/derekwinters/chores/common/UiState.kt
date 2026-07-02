package com.derekwinters.chores.common

/**
 * Generic UI state exposed by ViewModels via [kotlinx.coroutines.flow.StateFlow].
 *
 * First ViewModel state pattern introduced in this codebase (issue #5) — see
 * docs/adr/0002-network-auth-architecture.md. All future ViewModels should reuse this
 * sealed type rather than inventing per-screen state shapes.
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
