package com.derekwinters.chores.ui.theme

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.ThemeDto
import com.derekwinters.chores.data.repository.ThemeRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private fun themeDto(id: Int, name: String, builtin: Boolean = false) = ThemeDto(
    id = id,
    name = name,
    is_builtin = builtin,
    bg = "#000000",
    surface = "#111111",
    surface2 = "#222222",
    accent = "#333333",
    primary = "#444444",
    secondary = "#555555",
    success = "#00FF00",
    warning = "#FFFF00",
    error = "#FF0000"
)

/** Issue #24 behaviors: theme list, create-via-copy, update, delete, set-default. */
class ThemeAdminViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_populatesThemes() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(themesResult = listOf(themeDto(1, "Dark", builtin = true)))
        val viewModel = ThemeAdminViewModel(ThemeRepository(api))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(1, (state as UiState.Success).data.size)
    }

    @Test
    fun createTheme_success_reloadsList() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(createThemeResult = themeDto(2, "My Theme"))
        val viewModel = ThemeAdminViewModel(ThemeRepository(api))
        advanceUntilIdle()

        viewModel.createTheme("My Theme", sourceThemeId = 1)
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.actionState.value)
        assertEquals(1, api.lastCreateThemeRequest?.source_theme_id)
    }

    @Test
    fun setDefaultTheme_callsRepository() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(setDefaultThemeResult = themeDto(1, "Dark", builtin = true))
        val viewModel = ThemeAdminViewModel(ThemeRepository(api))
        advanceUntilIdle()

        viewModel.setDefaultTheme(1)
        advanceUntilIdle()

        assertEquals(1, api.lastSetDefaultThemeId)
    }

    @Test
    fun deleteTheme_success_reloadsList() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi()
        val viewModel = ThemeAdminViewModel(ThemeRepository(api))
        advanceUntilIdle()

        viewModel.deleteTheme(2)
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.actionState.value)
        assertEquals(2, api.lastDeleteThemeId)
    }
}
