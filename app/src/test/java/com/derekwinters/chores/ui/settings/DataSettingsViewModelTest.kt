package com.derekwinters.chores.ui.settings

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.ImportResultDto
import com.derekwinters.chores.data.repository.ConfigRepository
import com.derekwinters.chores.data.repository.DataRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Issue #22 behaviors: import confirmation summary counts, then submit reporting counts. */
class DataSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun previewImport_countsTopLevelArrays() {
        val viewModel = DataSettingsViewModel(DataRepository(FakeChoresApi()), ConfigRepository(FakeChoresApi()))
        val json = """{"people":[{},{}],"chores":[{}],"settings":[]}"""

        viewModel.previewImport(json)

        val preview = viewModel.importPreview.value
        assertEquals(2, preview?.peopleCount)
        assertEquals(1, preview?.choresCount)
        assertEquals(0, preview?.settingsCount)
    }

    @Test
    fun previewImport_malformedJson_countsAsZeroWithoutCrashing() {
        val viewModel = DataSettingsViewModel(DataRepository(FakeChoresApi()), ConfigRepository(FakeChoresApi()))

        viewModel.previewImport("not json")

        val preview = viewModel.importPreview.value
        assertEquals(0, preview?.peopleCount)
    }

    @Test
    fun confirmImport_success_reportsCountsAndClearsPreview() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(importConfigResult = ImportResultDto(people_count = 2, chores_count = 5, settings_count = 3))
        val viewModel = DataSettingsViewModel(DataRepository(api), ConfigRepository(api))
        viewModel.previewImport("""{"people":[],"chores":[],"settings":[]}""")

        viewModel.confirmImport()
        advanceUntilIdle()

        val state = viewModel.importState.value
        assertEquals(UiState.Success(com.derekwinters.chores.data.repository.ImportSummary(2, 5, 3)), state)
        assertNull(viewModel.importPreview.value)
    }

    @Test
    fun cancelImport_clearsPreviewWithoutSubmitting() {
        val viewModel = DataSettingsViewModel(DataRepository(FakeChoresApi()), ConfigRepository(FakeChoresApi()))
        viewModel.previewImport("""{"people":[]}""")

        viewModel.cancelImport()

        assertNull(viewModel.importPreview.value)
    }
}
