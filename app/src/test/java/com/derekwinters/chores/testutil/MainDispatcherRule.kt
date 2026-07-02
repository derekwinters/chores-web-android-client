package com.derekwinters.chores.testutil

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps [kotlinx.coroutines.Dispatchers.Main] (used by `viewModelScope`) for a test dispatcher for
 * the duration of a test, so ViewModel coroutines can run on the JVM without a real Android
 * main-thread looper.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        setMain(dispatcher)
    }

    override fun finished(description: Description) {
        resetMain()
    }
}
