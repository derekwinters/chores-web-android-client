package com.derekwinters.chores.ui.auth

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.auth.FakeCredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.SetupStatusDto
import com.derekwinters.chores.data.repository.AuthRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #11 behavior: "First-run setup: if the backend reports setup_needed, show a
 * 'Create Admin Account' flow instead of Login" (area: android, ui, network).
 */
class AuthGateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(setupNeeded: Boolean): AuthGateViewModel {
        val credentialStore = FakeCredentialStore()
        val sessionManager = SessionManager(credentialStore)
        val api = FakeChoresApi(setupStatusResult = SetupStatusDto(setup_needed = setupNeeded))
        return AuthGateViewModel(AuthRepository(api, credentialStore, sessionManager))
    }

    @Test
    fun initialState_isEnterServerUrl() {
        assertEquals(AuthGateState.EnterServerUrl, buildViewModel(setupNeeded = false).state.value)
    }

    @Test
    fun checkServer_setupNeeded_showsSetup() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = buildViewModel(setupNeeded = true)

        viewModel.checkServer("http://chores.example.com")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is AuthGateState.ShowSetup)
        assertEquals("http://chores.example.com", (state as AuthGateState.ShowSetup).serverUrl)
    }

    @Test
    fun checkServer_setupNotNeeded_showsLogin() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = buildViewModel(setupNeeded = false)

        viewModel.checkServer("http://chores.example.com")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is AuthGateState.ShowLogin)
        assertEquals("http://chores.example.com", (state as AuthGateState.ShowLogin).serverUrl)
    }
}
