package com.derekwinters.chores.ui.dashboard

/**
 * Cross-screen navigation callbacks the Dashboard destination needs (issues #12/#17), bundled so
 * ChoresApp's composable slot signatures don't keep growing a new positional lambda parameter
 * per issue.
 */
data class DashboardNavActions(
    val onNavigateToChores: (assignee: String?, dueWithin: String?) -> Unit = { _, _ -> },
    val onNavigateToUserDetail: (personId: Int, username: String) -> Unit = { _, _ -> }
)
