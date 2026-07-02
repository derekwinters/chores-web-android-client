package com.derekwinters.chores.ui

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.auth.FakeCredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.network.dto.UserInfoDto
import com.derekwinters.chores.data.repository.AuthRepository
import java.io.IOException
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #10 behavior: "Determine the current user's admin status (GET /auth/me) to drive
 * admin-only nav visibility" (area: ui, android, network).
 */
class CurrentUserViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(api: FakeChoresApi): CurrentUserViewModel {
        val credentialStore = FakeCredentialStore()
        val sessionManager = SessionManager(credentialStore)
        return CurrentUserViewModel(AuthRepository(api, credentialStore, sessionManager))
    }

    @Test
    fun init_startsInLoadingState() {
        val viewModel = buildViewModel(FakeChoresApi(currentUserResult = UserInfoDto("alice", true)))
        assertEquals(UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun refresh_success_exposesAdminFlag() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = buildViewModel(FakeChoresApi(currentUserResult = UserInfoDto("alice", true)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertTrue((state as UiState.Success).data.isAdmin)
        assertEquals("alice", state.data.username)
    }

    @Test
    fun refresh_nonAdmin_exposesFalseAdminFlag() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = buildViewModel(FakeChoresApi(currentUserResult = UserInfoDto("bob", false)))
        advanceUntilIdle()

        val state = viewModel.uiState.value as UiState.Success
        assertFalse(state.data.isAdmin)
    }

    @Test
    fun refresh_failure_updatesStateToError() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = buildViewModel(FakeChoresApi(currentUserError = IOException("boom")))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals(HttpErrorMessages.NETWORK_ERROR, (state as UiState.Error).message)
    }
}
