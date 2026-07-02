package com.derekwinters.chores.ui.theme

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.CurrentThemeDto
import com.derekwinters.chores.data.network.dto.ThemeDto
import com.derekwinters.chores.data.repository.ThemeRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private fun themeDto(id: Int, name: String) = ThemeDto(
    id = id, name = name, bg = "#000000", surface = "#111111", surface2 = "#222222",
    accent = "#333333", primary = "#444444", secondary = "#555555", success = "#0F0",
    warning = "#FF0", error = "#F00"
)

/** Issue #25 behaviors: resolves current theme (personal override vs. household default). */
class ThemePreferenceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_exposesThemesAndCurrentSelection() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            themesResult = listOf(themeDto(1, "Dark"), themeDto(2, "Light")),
            currentThemeResult = CurrentThemeDto(theme = themeDto(2, "Light"), is_personal_override = true)
        )
        val viewModel = ThemePreferenceViewModel(ThemeRepository(api))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(2, data.themes.size)
        assertTrue(data.current.isPersonalOverride)
        assertEquals("Light", data.current.theme.name)
    }

    @Test
    fun selectTheme_clearOverride_sendsSentinelId() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(currentThemeResult = CurrentThemeDto(theme = themeDto(1, "Dark")))
        val viewModel = ThemePreferenceViewModel(ThemeRepository(api))
        advanceUntilIdle()

        viewModel.selectTheme(null)
        advanceUntilIdle()

        assertEquals(ThemeRepository.CLEAR_PERSONAL_THEME_ID, api.lastSetPersonalThemeId)
    }

    @Test
    fun selectTheme_specificTheme_sendsThemeId() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(currentThemeResult = CurrentThemeDto(theme = themeDto(1, "Dark")))
        val viewModel = ThemePreferenceViewModel(ThemeRepository(api))
        advanceUntilIdle()

        viewModel.selectTheme(3)
        advanceUntilIdle()

        assertEquals(3, api.lastSetPersonalThemeId)
    }
}
