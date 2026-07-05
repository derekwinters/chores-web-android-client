package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.AuthLogEntry
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Issue #21 behaviors: filters and row rendering, plus #84/#91/#95 filter/label/pagination behaviors (area: ui, android). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class AuthLogContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun authLogContent_filterByUsername_invokesCallback() {
        var filters: AuthLogFilters? = null
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(emptyList(), total = 0, page = 1)),
                filters = AuthLogFilters(),
                onFiltersChange = { filters = it },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Username").performTextInput("alice")

        assert(filters?.username == "alice")
    }

    @Test
    fun authLogContent_rendersEntryWithChangedBy() {
        val entry = AuthLogEntry(
            id = 1,
            timestamp = "2026-07-02T22:40:54.326377Z",
            username = "bob",
            action = "password_reset",
            changedBy = "admin"
        )
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(listOf(entry), total = 1, page = 1)),
                filters = AuthLogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Password Reset").assertExists()
        composeTestRule.onNodeWithText("bob").assertExists()
        composeTestRule.onNodeWithText(formatDateTime("2026-07-02T22:40:54.326377Z")).assertExists()
        composeTestRule.onNodeWithText("Changed by: admin").assertExists()
    }

    /** Issue #91: raw action values are mapped to humanized labels. */
    @Test
    fun authLogContent_humanizesKnownActionValues() {
        val entries = listOf(
            AuthLogEntry(id = 1, timestamp = "2026-07-02T22:40:54.326377Z", username = "a", action = "login_succeeded", changedBy = null),
            AuthLogEntry(id = 2, timestamp = "2026-07-02T22:40:54.326377Z", username = "b", action = "login_failed", changedBy = null),
            AuthLogEntry(id = 3, timestamp = "2026-07-02T22:40:54.326377Z", username = "c", action = "password_changed", changedBy = null),
            AuthLogEntry(id = 4, timestamp = "2026-07-02T22:40:54.326377Z", username = "d", action = "user_created", changedBy = null)
        )
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(entries, total = entries.size, page = 1)),
                filters = AuthLogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Login Succeeded").assertExists()
        composeTestRule.onNodeWithText("Login Failed").assertExists()
        composeTestRule.onNodeWithText("Password Changed").assertExists()
        composeTestRule.onNodeWithText("User Created").assertExists()
    }

    /** Issue #91: unmapped action values fall back to a title-cased transform. */
    @Test
    fun authLogContent_unmappedAction_fallsBackToTitleCasedTransform() {
        val entry = AuthLogEntry(id = 5, timestamp = "2026-07-02T22:40:54.326377Z", username = "e", action = "some_new_event", changedBy = null)
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(listOf(entry), total = 1, page = 1)),
                filters = AuthLogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Some New Event").assertExists()
    }

    @Test
    fun authLogContent_malformedTimestamp_fallsBackToRawString() {
        val entry = AuthLogEntry(id = 2, timestamp = "not-a-timestamp", username = "carol", action = "login_failed", changedBy = null)
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(listOf(entry), total = 1, page = 1)),
                filters = AuthLogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("not-a-timestamp").assertExists()
    }

    /** Issue #84: action filter field invokes the callback with the entered value. */
    @Test
    fun authLogContent_filterByAction_invokesCallback() {
        var filters: AuthLogFilters? = null
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(emptyList(), total = 0, page = 1)),
                filters = AuthLogFilters(),
                onFiltersChange = { filters = it },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Action").performTextInput("login_failed")

        assert(filters?.action == "login_failed")
    }

    /** Issue #84: start/end date filter fields invoke the callback with the entered value. */
    @Test
    fun authLogContent_filterByDateRange_invokesCallback() {
        var filters: AuthLogFilters? = null
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(emptyList(), total = 0, page = 1)),
                filters = AuthLogFilters(),
                onFiltersChange = { filters = it },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Start Date").performTextInput("2026-07-01")
        assert(filters?.start == "2026-07-01")

        composeTestRule.onNodeWithText("End Date").performTextInput("2026-07-31")
        assert(filters?.end == "2026-07-31")
    }

    /** Issue #84: Clear Filters button resets filters to their default (empty) state. */
    @Test
    fun authLogContent_clearFilters_resetsToDefault() {
        var filters = AuthLogFilters(username = "alice", action = "login_failed", start = "2026-07-01", end = "2026-07-31")
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(emptyList(), total = 0, page = 1)),
                filters = filters,
                onFiltersChange = { filters = it },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Clear Filters").performClick()

        assert(filters == AuthLogFilters())
    }

    /** Issue #95: pagination text reads "Page X of Y", aligned with the Activity Log format. */
    @Test
    fun authLogContent_paginationText_showsPageOfTotalPages() {
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(emptyList(), total = 0, page = 2, totalPages = 5)),
                filters = AuthLogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Page 2 of 5").assertExists()
    }
}
