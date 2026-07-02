package com.derekwinters.chores.ui

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.auth.FakeCredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.DbStatusDto
import com.derekwinters.chores.data.repository.AuthRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #11 behavior: "DB-readiness gate: poll GET /status/db-status before rendering the
 * authenticated app shell" (area: android, ui, network).
 */
class DbReadinessViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(dbStatusResult: DbStatusDto): DbReadinessViewModel {
        val credentialStore = FakeCredentialStore()
        val sessionManager = SessionManager(credentialStore)
        val api = FakeChoresApi(dbStatusResult = dbStatusResult)
        return DbReadinessViewModel(AuthRepository(api, credentialStore, sessionManager))
    }

    @Test
    fun init_startsNotReady() {
        assertTrue(!buildViewModel(DbStatusDto(ready = true)).isReady.value)
    }

    @Test
    fun init_dbAlreadyReady_becomesReadyAfterFirstPoll() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = buildViewModel(DbStatusDto(ready = true))
        advanceUntilIdle()

        assertTrue(viewModel.isReady.value)
    }
}
