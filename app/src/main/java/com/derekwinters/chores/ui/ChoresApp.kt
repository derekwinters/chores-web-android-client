package com.derekwinters.chores.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.derekwinters.chores.R
import com.derekwinters.chores.data.model.CurrentUser
import com.derekwinters.chores.ui.auth.AuthGateScreen
import com.derekwinters.chores.ui.chores.ChoreListScreen
import com.derekwinters.chores.ui.common.DbReadinessGate
import com.derekwinters.chores.ui.common.PlaceholderScreen
import com.derekwinters.chores.ui.dashboard.DashboardScreen
import kotlinx.coroutines.launch

/**
 * Top-level nav destinations (issue #10: "Add navigation destinations for: Dashboard, Chores,
 * Activity Log, Users, Settings, Preferences"). Chores-web's default route (`/`) is Board/
 * Dashboard, so [Dashboard] — not [Chores] — is the app's start destination now; the chore list
 * screen from issue #5 becomes the "Chores" destination unchanged.
 *
 * [adminOnly] mirrors web's `App.jsx` `adminOnly: true` nav-item gating (Users, Settings).
 */
sealed class ChoresDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val adminOnly: Boolean = false
) {
    data object Dashboard : ChoresDestination("dashboard", R.string.nav_dashboard, Icons.Filled.Dashboard)
    data object Chores : ChoresDestination("chores", R.string.nav_chores, Icons.Filled.CheckCircle)
    data object ActivityLog : ChoresDestination("log", R.string.nav_log, Icons.Filled.History)
    data object Users : ChoresDestination("users", R.string.nav_users, Icons.Filled.People, adminOnly = true)
    data object Settings : ChoresDestination("settings", R.string.nav_settings, Icons.Filled.Settings, adminOnly = true)
    data object Preferences : ChoresDestination("preferences", R.string.nav_preferences, Icons.Filled.Palette)
    data object Notification : ChoresDestination("notification", R.string.nav_notification, Icons.Filled.Notifications)
}

/** Issue #12: builds the Chores route + query args for a Dashboard Due Now/Due Soon deep link. */
private fun choresRouteWithArgs(assignee: String?, dueWithin: String?): String {
    val params = listOfNotNull(
        assignee?.let { "assignee=${android.net.Uri.encode(it)}" },
        dueWithin?.let { "dueWithin=${android.net.Uri.encode(it)}" }
    )
    return if (params.isEmpty()) ChoresDestination.Chores.route else "${ChoresDestination.Chores.route}?${params.joinToString("&")}"
}

private val drawerDestinations = listOf(
    ChoresDestination.Dashboard,
    ChoresDestination.Chores,
    ChoresDestination.ActivityLog,
    ChoresDestination.Users,
    ChoresDestination.Settings,
    ChoresDestination.Preferences,
    ChoresDestination.Notification
)

/**
 * Root app composable: Login screen gates a drawer-nav Scaffold (issue #10 replaces the 2-item
 * bottom nav bar with a sidebar-style drawer, matching chores-web's sidebar, now that there are
 * 7 destinations). [CurrentUserViewModel] drives admin-only item visibility.
 *
 * Thin Hilt-wired wrapper around [ChoresAppContent]; see ChoresAppContentTest for gating/nav
 * behavior coverage that doesn't require a Hilt test component.
 */
@Composable
fun ChoresApp(
    onSendTestNotification: () -> Unit,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val isAuthenticated by sessionViewModel.isAuthenticated.collectAsState()
    ChoresAppContent(
        isAuthenticated = isAuthenticated,
        onSendTestNotification = onSendTestNotification,
        onLogout = sessionViewModel::logout
    )
}

/**
 * @param loginContent slot rendered while signed out; defaults to the real (Hilt-wired)
 *   [AuthGateScreen] but is overridable in tests to avoid needing a Hilt test component.
 * @param currentUserProvider supplies the signed-in user's admin status for nav gating;
 *   overridable in tests. Defaults to the real (Hilt-wired) [CurrentUserViewModel].
 */
@Composable
fun ChoresAppContent(
    isAuthenticated: Boolean,
    onSendTestNotification: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    loginContent: @Composable () -> Unit = { AuthGateScreen() },
    dashboardContent: @Composable ((assignee: String?, dueWithin: String?) -> Unit) -> Unit = { onNavigateToChores ->
        DashboardScreen(onNavigateToChores = onNavigateToChores)
    },
    choresContent: @Composable (assignee: String?, dueWithin: String?) -> Unit = { assignee, dueWithin ->
        ChoreListScreen(initialAssignee = assignee, initialDueWithin = dueWithin)
    },
    logContent: @Composable () -> Unit = { PlaceholderScreen(stringResource(R.string.coming_soon)) },
    usersContent: @Composable () -> Unit = { PlaceholderScreen(stringResource(R.string.coming_soon)) },
    settingsContent: @Composable () -> Unit = { PlaceholderScreen(stringResource(R.string.coming_soon)) },
    preferencesContent: @Composable () -> Unit = { PlaceholderScreen(stringResource(R.string.coming_soon)) },
    currentUserProvider: @Composable () -> UiState<CurrentUser> = {
        val viewModel: CurrentUserViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsState()
        state
    },
    isDatabaseReadyProvider: @Composable () -> Boolean = {
        val viewModel: DbReadinessViewModel = hiltViewModel()
        val ready by viewModel.isReady.collectAsState()
        ready
    }
) {
    if (!isAuthenticated) {
        loginContent()
        return
    }

    val isDatabaseReady = isDatabaseReadyProvider()

    DbReadinessGate(isReady = isDatabaseReady, modifier = modifier) {
        val currentUserState = currentUserProvider()
        val isAdmin = (currentUserState as? UiState.Success)?.data?.isAdmin == true

        ChoresAuthenticatedScaffold(
            isAdmin = isAdmin,
            onLogout = onLogout,
            dashboardContent = dashboardContent,
            choresContent = choresContent,
            logContent = logContent,
            usersContent = usersContent,
            settingsContent = settingsContent,
            preferencesContent = preferencesContent,
            notificationContent = { NotificationScreen(onSendTestNotification = onSendTestNotification) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoresAuthenticatedScaffold(
    isAdmin: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    dashboardContent: @Composable ((assignee: String?, dueWithin: String?) -> Unit) -> Unit,
    choresContent: @Composable (assignee: String?, dueWithin: String?) -> Unit,
    logContent: @Composable () -> Unit,
    usersContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    preferencesContent: @Composable () -> Unit,
    notificationContent: @Composable () -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var userMenuExpanded by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val visibleDestinations = drawerDestinations.filter { !it.adminOnly || isAdmin }
    // Chores' actual registered route carries query-arg placeholders ("chores?assignee=...");
    // matching by prefix keeps drawer highlighting/title working whether or not args are present.
    fun isCurrent(dest: ChoresDestination) =
        currentDestination?.hierarchy?.any { it.route?.startsWith(dest.route) == true } == true
    val currentLabel = visibleDestinations
        .firstOrNull(::isCurrent)
        ?.labelRes
        ?.let { stringResource(it) }
        ?: stringResource(R.string.app_name)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                visibleDestinations.forEach { destination ->
                    val selected = isCurrent(destination)
                    NavigationDrawerItem(
                        label = { Text(stringResource(destination.labelRes)) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        selected = selected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(destination.route) {
                                popUpTo(ChoresDestination.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(currentLabel) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.open_navigation_menu))
                        }
                    },
                    actions = {
                        IconButton(onClick = { userMenuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.user_menu))
                        }
                        DropdownMenu(expanded = userMenuExpanded, onDismissRequest = { userMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.logout)) },
                                leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null) },
                                onClick = {
                                    userMenuExpanded = false
                                    onLogout()
                                }
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ChoresDestination.Dashboard.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(ChoresDestination.Dashboard.route) {
                    dashboardContent { assignee, dueWithin ->
                        navController.navigate(choresRouteWithArgs(assignee, dueWithin))
                    }
                }
                composable(
                    route = "${ChoresDestination.Chores.route}?assignee={assignee}&dueWithin={dueWithin}",
                    arguments = listOf(
                        navArgument("assignee") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                        navArgument("dueWithin") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    choresContent(
                        backStackEntry.arguments?.getString("assignee"),
                        backStackEntry.arguments?.getString("dueWithin")
                    )
                }
                composable(ChoresDestination.ActivityLog.route) { logContent() }
                composable(ChoresDestination.Users.route) { usersContent() }
                composable(ChoresDestination.Settings.route) { settingsContent() }
                composable(ChoresDestination.Preferences.route) { preferencesContent() }
                composable(ChoresDestination.Notification.route) { notificationContent() }
            }
        }
    }
}
