package com.derekwinters.chores.ui.setup

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.auth.FakeCredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.LoginResponseDto
import com.derekwinters.chores.data.network.dto.UserInfoDto
import com.derekwinters.chores.data.repository.AuthRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #11 behavior: "Creates the first user as admin, then sets auth_enabled via config"
 * (area: android, ui, network).
 */
class SetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun createAdminAccount_success_persistsSessionAndSucceeds() = runTest(mainDispatcherRule.testDispatcher) {
        val credentialStore = FakeCredentialStore()
        val sessionManager = SessionManager(credentialStore)
        val api = FakeChoresApi(loginResult = LoginResponseDto("tok", "bearer", UserInfoDto("admin", true)))
        val viewModel = SetupViewModel(AuthRepository(api, credentialStore, sessionManager))

        viewModel.createAdminAccount("http://chores.example.com", "admin", "secret123", requireAuth = true)
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.uiState.value)
        assertEquals("tok", credentialStore.getToken())
        assertTrue(sessionManager.isAuthenticated.value)
    }
}
