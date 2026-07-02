package com.derekwinters.chores.ui.chores

/**
 * Cross-screen navigation callbacks the Chores destination needs (issues #15/#16), bundled so
 * ChoresApp's composable slot signatures don't keep growing a new positional lambda parameter
 * per issue.
 */
data class ChoresNavActions(
    val onNavigateToHistory: (choreName: String) -> Unit = {},
    val onNavigateToCreateChore: () -> Unit = {},
    val onNavigateToEditChore: (choreId: Int) -> Unit = {}
)
