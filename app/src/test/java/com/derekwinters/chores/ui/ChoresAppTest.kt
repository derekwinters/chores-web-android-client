package com.derekwinters.chores.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behavior: Bottom nav bar wiring Home <-> Notification screens (area: ui)
 *
 * The real Home tab content ([com.derekwinters.chores.ui.chores.ChoreListRoute]) requires Hilt
 * DI, so these tests exercise the bottom-nav wiring itself via [ChoresApp]'s injectable
 * [ChoresApp]'s `homeContent` slot rather than the Hilt-wired route.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChoresAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeHomeContentMarker = "Fake Home Content"

    private fun setContent() {
        composeTestRule.setContent {
            ChoresApp(
                onSendTestNotification = {},
                homeContent = { Text(fakeHomeContentMarker) }
            )
        }
    }

    @Test
    fun choresApp_startsOnHomeTab() {
        setContent()

        composeTestRule.onNodeWithText(fakeHomeContentMarker).assertExists()
    }

    @Test
    fun choresApp_bottomNav_navigatesToNotificationScreen() {
        setContent()

        composeTestRule.onNodeWithText("Notification").performClick()

        composeTestRule.onNodeWithText("Send Test Notification").assertExists()
    }

    @Test
    fun choresApp_bottomNav_navigatesBackToHomeTab() {
        setContent()

        composeTestRule.onNodeWithText("Notification").performClick()
        composeTestRule.onNodeWithText("Home").performClick()

        composeTestRule.onNodeWithText(fakeHomeContentMarker).assertExists()
    }
}
