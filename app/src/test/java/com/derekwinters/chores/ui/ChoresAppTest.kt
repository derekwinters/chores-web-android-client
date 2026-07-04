package com.derekwinters.chores.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.CurrentUser
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behaviors: drawer nav wiring the 7 destinations, admin-only Users/Settings visibility, the
 * Logout action (issue #10, area: ui), and the TopAppBar's household/app title branding
 * (issue #58, area: ui). Uses [ChoresAppContent]'s injectable slots so this doesn't require a
 * Hilt test component — see LoginContentTest and ChoreListContentTest for the real screens' own
 * behavior coverage.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChoresAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        isAuthenticated: Boolean = true,
        isAdmin: Boolean = false,
        onLogout: () -> Unit = {},
        currentTitleProvider: @Composable () -> String? = { "Test Household" }
    ) {
        composeTestRule.setContent {
            ChoresAppContent(
                isAuthenticated = isAuthenticated,
                onSendTestNotification = {},
                onLogout = onLogout,
                loginContent = { Text("Fake Login") },
                dashboardContent = { Text("Fake Dashboard") },
                choresContent = { _, _, _ -> Text("Fake Chores") },
                userDetailContent = { Text("Fake User Detail") },
                logContent = { Text("Fake Log") },
                usersContent = { Text("Fake Users") },
                settingsContent = { Text("Fake Settings") },
                preferencesContent = { Text("Fake Preferences") },
                currentUserProvider = { UiState.Success(CurrentUser("alice", isAdmin)) },
                isDatabaseReadyProvider = { true },
                currentThemeProvider = { null },
                currentTitleProvider = currentTitleProvider
            )
        }
    }

    @Test
    fun choresApp_signedOut_showsLoginContent() {
        composeTestRule.setContent {
            ChoresAppContent(
                isAuthenticated = false,
                onSendTestNotification = {},
                loginContent = { Text("Fake Login") }
            )
        }

        composeTestRule.onNodeWithText("Fake Login").assertExists()
    }

    @Test
    fun choresApp_signedIn_startsOnDashboard() {
        setContent()

        composeTestRule.onNodeWithText("Fake Dashboard").assertExists()
    }

    @Test
    fun choresApp_drawer_navigatesToChoresAndBackToDashboard() {
        setContent()

        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        composeTestRule.onNodeWithText("Chores").performClick()
        composeTestRule.onNodeWithText("Fake Chores").assertExists()

        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        composeTestRule.onNodeWithText("Dashboard").performClick()
        composeTestRule.onNodeWithText("Fake Dashboard").assertExists()
    }

    @Test
    fun choresApp_nonAdmin_hidesAdminOnlyDestinations() {
        setContent(isAdmin = false)

        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        composeTestRule.onNodeWithText("Users").assertDoesNotExist()
        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()
    }

    @Test
    fun choresApp_admin_showsAdminOnlyDestinations() {
        setContent(isAdmin = true)

        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        composeTestRule.onNodeWithText("Users").assertExists()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun choresApp_logout_invokesCallback() {
        var loggedOut = false
        setContent(onLogout = { loggedOut = true })

        composeTestRule.onNodeWithContentDescription("User menu").performClick()
        composeTestRule.onNodeWithText("Logout").performClick()

        assert(loggedOut)
    }

    @Test
    fun choresApp_topBar_showsHouseholdTitleBranding() {
        setContent(currentTitleProvider = { "The Winters House" })

        composeTestRule.onNodeWithTag("appTitleBranding").assertTextEquals("The Winters House")
    }

    @Test
    fun choresApp_topBar_fallsBackToAppNameWhenTitleNotLoaded() {
        setContent(currentTitleProvider = { null })

        composeTestRule.onNodeWithTag("appTitleBranding").assertTextEquals("Chores")
    }
}
