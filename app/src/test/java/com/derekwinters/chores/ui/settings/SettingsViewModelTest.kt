package com.derekwinters.chores.ui.settings

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.data.repository.ConfigRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Issue #20 behaviors: load/save the shared config across the four Settings forms. */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_populatesConfig() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(configResult = ConfigDto(title = "My Chores"))
        val viewModel = SettingsViewModel(ConfigRepository(api))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("My Chores", (state as UiState.Success).data.appTitle)
    }

    @Test
    fun save_success_updatesStateAndUiState() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(configResult = ConfigDto())
        val viewModel = SettingsViewModel(ConfigRepository(api))
        advanceUntilIdle()

        viewModel.save(ConfigDto(title = "New Title").toDomain())
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.saveState.value)
        assertEquals("New Title", (viewModel.uiState.value as UiState.Success).data.appTitle)
    }
}
