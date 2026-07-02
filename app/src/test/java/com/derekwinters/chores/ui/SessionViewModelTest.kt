package com.derekwinters.chores.ui

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.auth.FakeCredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.repository.AuthRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Issue #10 behavior: "user menu ... with a Logout action" (area: ui, android, network). */
class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun logout_clearsSessionAndToken() = runTest(mainDispatcherRule.testDispatcher) {
        val credentialStore = FakeCredentialStore(token = "tok123", tokenType = "Bearer")
        val sessionManager = SessionManager(credentialStore)
        assertTrue(sessionManager.isAuthenticated.value)

        val viewModel = SessionViewModel(sessionManager, AuthRepository(FakeChoresApi(), credentialStore, sessionManager))

        viewModel.logout()
        advanceUntilIdle()

        assertFalse(sessionManager.isAuthenticated.value)
        assertNull(credentialStore.getToken())
    }
}
