package com.derekwinters.chores.ui

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.data.repository.ConfigRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Issue #58 behavior: TopAppBar household/app title branding is backed by `GET /v1/config`'s
 * `title` field, exposed here for [com.derekwinters.chores.ui.ChoresAuthenticatedScaffold] to
 * render with serif styling.
 */
class AppTitleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsAppTitleFromConfig() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(configResult = ConfigDto(title = "The Winters House"))
        val viewModel = AppTitleViewModel(ConfigRepository(api))

        assertNull(viewModel.appTitle.value)
        advanceUntilIdle()

        assertEquals("The Winters House", viewModel.appTitle.value)
    }

    @Test
    fun refresh_reloadsAppTitle() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(configResult = ConfigDto(title = "Updated Title"))
        val viewModel = AppTitleViewModel(ConfigRepository(api))
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("Updated Title", viewModel.appTitle.value)
    }
}
