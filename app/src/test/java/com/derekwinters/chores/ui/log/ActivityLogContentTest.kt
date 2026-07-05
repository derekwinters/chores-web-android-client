package com.derekwinters.chores.ui.log

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDateTime
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Reads the resolved text color directly off the node's text layout result -- same
 * `GetTextLayoutResult`-based approach as `ChoresStatsPanelContentTest.textFontSizeSp()`, since
 * Robolectric's headless rendering doesn't support pixel-sampling (`captureToImage`) reliably.
 */
private fun SemanticsNodeInteraction.textColor(): Color {
    val results = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
    return results.first().layoutInput.style.color
}

/**
 * Issue #19 behaviors: row expand shows amendment diffs, pagination controls (area: ui, android).
 * Exercises [ActivityLogContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ActivityLogContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val amendment = LogEntry(
        id = 1,
        choreId = 1,
        choreName = "Dishes",
        person = "alice",
        action = "updated",
        timestamp = "2026-07-01",
        reassignedTo = null,
        assignee = null,
        fieldName = "points",
        oldValue = "5",
        newValue = "8"
    )

    private val personEntry = LogEntry(
        id = 2,
        choreId = 0,
        choreName = "Person: bob",
        person = "alice",
        action = "password_changed",
        timestamp = "2026-07-01",
        reassignedTo = null,
        assignee = null,
        fieldName = null,
        oldValue = null,
        newValue = null
    )

    @Test
    fun activityLogContent_expandAmendment_showsFieldDiff() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()

        composeTestRule.onNodeWithText("points: 5 -> 8").assertExists()
    }

    @Test
    fun activityLogContent_choreEntry_showsChoreChipAndSeparatedActionTarget() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        // Not onNodeWithText("Chore") -- the filters row also has a "Chore" text-field label.
        // useUnmergedTree is required: LogRow's Card is clickable (mergeDescendants = true), and
        // TestTag isn't propagated upward through a merge boundary -- it stays on the exact node
        // (PillBadge's inner Text) it was set on. Same pattern as ChoreListContentTest.
        composeTestRule.onNodeWithTag("targetTypeChip", useUnmergedTree = true).assertTextEquals("Chore")
        composeTestRule.onNodeWithText("Updated").assertExists()
        composeTestRule.onNodeWithText("Dishes").assertExists()
    }

    @Test
    fun activityLogContent_personEntry_showsUserChipAndStripsPrefix() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(personEntry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("targetTypeChip", useUnmergedTree = true).assertTextEquals("User")
        composeTestRule.onNodeWithText("Password Changed").assertExists()
        composeTestRule.onNodeWithText("bob").assertExists()
    }

    /**
     * Issue #73: raw action values are mapped to humanized labels, e.g. "completed" ->
     * "Completed", mirroring chores-web and the Auth Log's (#91) humanization.
     */
    @Test
    fun activityLogContent_humanizesCompletedAction() {
        val entry = amendment.copy(action = "completed", fieldName = null, oldValue = null, newValue = null)
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(entry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Completed").assertExists()
    }

    /** Issue #73: unmapped/unknown action values fall back to a title-cased transform. */
    @Test
    fun activityLogContent_unmappedAction_fallsBackToTitleCasedTransform() {
        val entry = amendment.copy(action = "some_new_event", fieldName = null, oldValue = null, newValue = null)
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(entry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Some New Event").assertExists()
    }

    /**
     * Issue #71: the action value renders inside a dedicated `actionBadge`-tagged pill (not just
     * loose row text), so it can be styled/colored distinctly from the rest of the row.
     */
    @Test
    fun activityLogContent_actionRendersAsBadge() {
        val entry = amendment.copy(action = "completed", fieldName = null, oldValue = null, newValue = null)
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(entry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("actionBadge", useUnmergedTree = true).assertTextEquals("Completed")
    }

    /**
     * Issue #71: the target value (chore name / person name) renders inside a dedicated
     * `targetBadge`-tagged pill, distinct from the existing `targetTypeChip` (User/Chore)
     * indicator.
     */
    @Test
    fun activityLogContent_targetRendersAsBadge() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("targetBadge", useUnmergedTree = true).assertTextEquals("Dishes")
    }

    /** Issue #71: person-target rows' target badge strips the "Person: " prefix, same as before. */
    @Test
    fun activityLogContent_personTarget_targetBadgeStripsPrefix() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(personEntry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("targetBadge", useUnmergedTree = true).assertTextEquals("bob")
    }

    @Test
    fun activityLogContent_expandEntry_showsFullTimestamp() {
        val timestampedEntry = amendment.copy(timestamp = "2026-07-02T22:40:54.326377Z")
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(timestampedEntry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()

        composeTestRule.onNodeWithText("Timestamp: ${formatDateTime("2026-07-02T22:40:54.326377Z")}").assertExists()
    }

    @Test
    fun activityLogContent_relativeTimestamp_justNow() {
        val justNowEntry = amendment.copy(timestamp = java.time.Instant.now().toString())
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(justNowEntry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("alice · just now").assertExists()
    }

    /**
     * Issue #81: entries 24h+ old get a muted-red timestamp (mirroring chores-web's stale-entry
     * treatment), while entries under 24h keep the default color. Asserted by comparing the two
     * resolved colors rather than pinning to an exact color value/alpha, since the "muted red"
     * comes from `MaterialTheme.colorScheme.error` (no `ChoresTheme` wrapper is present in this
     * test, so the M3 default light error color resolves) -- the color's red channel should
     * dominate green/blue and its alpha should be reduced below fully opaque.
     */
    @Test
    fun activityLogContent_agedTimestamp_rendersInMutedRed() {
        val agedEntry = amendment.copy(timestamp = java.time.Instant.now().minus(java.time.Duration.ofHours(48)).toString())
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(agedEntry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        val color = composeTestRule.onNodeWithTag("logRowTimestamp", useUnmergedTree = true).textColor()

        assert(color.alpha < 1f) { "expected a muted (reduced-alpha) color, got alpha=${color.alpha}" }
        assert(color.red > color.green && color.red > color.blue) {
            "expected a reddish color, got r=${color.red} g=${color.green} b=${color.blue}"
        }
    }

    /** Issue #81: entries under 24h old keep the default (non-error) timestamp color. */
    @Test
    fun activityLogContent_recentTimestamp_keepsDefaultColor() {
        val recentEntry = amendment.copy(timestamp = java.time.Instant.now().minus(java.time.Duration.ofHours(1)).toString())
        val agedEntry = amendment.copy(id = 2, timestamp = java.time.Instant.now().minus(java.time.Duration.ofHours(48)).toString())
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(recentEntry, agedEntry), total = 2, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        val timestampNodes = composeTestRule.onAllNodesWithTag("logRowTimestamp", useUnmergedTree = true)
        val recentColor = timestampNodes[0].textColor()
        val agedColor = timestampNodes[1].textColor()

        assert(recentColor != agedColor) {
            "expected recent and aged timestamps to render in different colors, both were $recentColor"
        }
    }

    /**
     * Issue #77: a chevron icon signals the row is expandable (Android otherwise gave no visual
     * cue, unlike web's chevron affordance) and it flips between ExpandMore/ExpandLess as
     * `expanded` toggles. useUnmergedTree is required here too, same reason as
     * targetTypeChip/actionBadge/targetBadge above: the Icon lives inside LogRow's clickable
     * Card, and TestTag doesn't propagate upward through that merge boundary.
     */
    @Test
    fun activityLogContent_chevron_reflectsExpandedState() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("expandChevron", useUnmergedTree = true)
            .assert(hasContentDescription("Expand entry"))

        composeTestRule.onNodeWithText("Dishes").performClick()

        composeTestRule.onNodeWithTag("expandChevron", useUnmergedTree = true)
            .assert(hasContentDescription("Collapse entry"))
    }

    @Test
    fun activityLogContent_previousPageDisabledOnFirstPage() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(emptyList(), total = 0, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Previous").assertIsNotEnabled()
    }
}
