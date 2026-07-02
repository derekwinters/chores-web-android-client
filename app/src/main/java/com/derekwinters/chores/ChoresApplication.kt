package com.derekwinters.chores

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for Hilt's dependency graph.
 *
 * Hilt is introduced in issue #5 alongside the first ViewModels/repositories — see
 * docs/adr/0002-network-auth-architecture.md.
 */
@HiltAndroidApp
class ChoresApplication : Application()
