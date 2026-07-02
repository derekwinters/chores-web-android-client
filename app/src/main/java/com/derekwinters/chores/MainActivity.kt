package com.derekwinters.chores

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.derekwinters.chores.notification.NotificationSender
import com.derekwinters.chores.ui.ChoresRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * App entry point.
 *
 * Owns the one "impure" boundary for notifications: requesting the POST_NOTIFICATIONS runtime
 * permission and posting the test notification (issue #2, area: android). Screens themselves stay
 * plain, stateless composables (issue #2 grilling decision) with thin Hilt-wired Route wrappers
 * for the first ViewModels, introduced in issue #5 (see docs/adr/0002-network-auth-architecture.md).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationSender by lazy { NotificationSender(this) }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                notificationSender.sendTestNotification()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChoresRoot(onSendTestNotification = ::sendTestNotification)
        }
    }

    private fun sendTestNotification() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            notificationSender.sendTestNotification()
        } else {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
