package com.derekwinters.chores.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.PointsLogEntry
import com.derekwinters.chores.data.repository.PointsLogPage
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Reads the resolved text color directly off the node's text layout result — same
 * `GetTextLayoutResult`-based approach as `ActivityLogContentTest.textColor()`, since
 * Robolectric's headless rendering doesn't support pixel-sampling (`captureToImage`) reliably.
 */
private fun SemanticsNodeInteraction.textColor(): Color {
    val results = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
    return results.first().layoutInput.style.color
}

/** Issue #23 behaviors: inline edit + delete-confirmation warning (area: ui, android). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class PointsLogContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val entry = PointsLogEntry(id = 1, person = "alice", points = 5, choreId = 4, completedAt = "2026-07-01")

    @Test
    fun pointsLogContent_rendersPageHeading() {
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 1, offset = 0, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Points Log").assertExists()
    }

    @Test
    fun pointsLogContent_editEntry_savesUpdatedValues() {
        var updated: Triple<Int, String, Int>? = null
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 1, offset = 0, limit = 20)),
                onUpdate = { id, person, points -> updated = Triple(id, person, points) },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("alice").performClick()
        composeTestRule.onNodeWithText("Points").performTextClearance()
        composeTestRule.onNodeWithText("Points").performTextInput("8")
        composeTestRule.onNodeWithText("Save").performClick()

        assert(updated == Triple(1, "alice", 8))
    }

    @Test
    fun pointsLogContent_deleteEntry_requiresConfirmation() {
        var deletedId: Int? = null
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 1, offset = 0, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = { deletedId = it },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("alice").performClick()
        composeTestRule.onNodeWithText("Delete Entry").performClick()
        composeTestRule.onNodeWithText("This will reverse the points on the person, floored at 0, and cannot be undone.").assertExists()

        composeTestRule.onNodeWithText("Delete").performClick()

        assert(deletedId == 1)
    }

    /** Issue #121: delete-confirmation dialog displays the entry's ID (area: ui). */
    @Test
    fun pointsLogContent_deleteConfirmation_showsEntryId() {
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 1, offset = 0, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("alice").performClick()
        composeTestRule.onNodeWithText("Delete Entry").performClick()

        composeTestRule.onNodeWithText("Delete entry #1?").assertExists()
    }

    /**
     * Issue #122: Delete actions are styled red/error-colored, consistent with User Management
     * (area: ui). Asserted by red-channel dominance rather than an exact value, since no
     * `ChoresTheme` wrapper is present here so the M3 default light error color resolves.
     */
    @Test
    fun pointsLogContent_deleteActions_styledRed() {
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 1, offset = 0, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("alice").performClick()
        val deleteEntryColor = composeTestRule.onNodeWithText("Delete Entry", useUnmergedTree = true).textColor()
        assert(deleteEntryColor.red > deleteEntryColor.green && deleteEntryColor.red > deleteEntryColor.blue) {
            "expected a reddish Delete Entry action, got r=${deleteEntryColor.red} g=${deleteEntryColor.green} b=${deleteEntryColor.blue}"
        }

        composeTestRule.onNodeWithText("Delete Entry").performClick()
        val confirmColor = composeTestRule.onNodeWithText("Delete", useUnmergedTree = true).textColor()
        assert(confirmColor.red > confirmColor.green && confirmColor.red > confirmColor.blue) {
            "expected a reddish Delete confirm action, got r=${confirmColor.red} g=${confirmColor.green} b=${confirmColor.blue}"
        }
    }

    /** Issue #121: each Points Log row displays the entry's ID (area: ui). */
    @Test
    fun pointsLogContent_rowDisplaysEntryId() {
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 1, offset = 0, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("ID 1 · Chore #4 · 2026-07-01").assertExists()
    }

    /** Issue #124: pagination text displays the current range and total, e.g. "Showing X–Y of Z" (area: ui). */
    @Test
    fun pointsLogContent_paginationText_showsCurrentRange() {
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 45, offset = 20, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Showing 21–40 of 45").assertExists()
    }

    /** Issue #124: the range end is clamped to the total on a short final page (area: ui). */
    @Test
    fun pointsLogContent_paginationText_clampsRangeEndToTotal() {
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 45, offset = 40, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Showing 41–45 of 45").assertExists()
    }

    @Test
    fun pointsLogContent_rendersFormattedTimestamp() {
        val timestampedEntry = entry.copy(completedAt = "2026-07-02T22:40:54.326377Z")
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(timestampedEntry), total = 1, offset = 0, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("ID 1 · Chore #4 · ${formatDateTime("2026-07-02T22:40:54.326377Z")}").assertExists()
    }

    @Test
    fun pointsLogContent_malformedTimestamp_fallsBackToRawString() {
        // entry.completedAt = "2026-07-01" is date-only, so Instant.parse throws and this
        // exercises the raw-string fallback path.
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 1, offset = 0, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("ID 1 · Chore #4 · 2026-07-01").assertExists()
    }
}
