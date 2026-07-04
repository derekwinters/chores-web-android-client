package com.derekwinters.chores.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.derekwinters.chores.R
import com.derekwinters.chores.data.model.CurrentUser
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.ui.auth.AuthGateScreen
import com.derekwinters.chores.ui.chores.ChoreFormScreen
import com.derekwinters.chores.ui.chores.ChoreListScreen
import com.derekwinters.chores.ui.chores.ChoresNavActions
import com.derekwinters.chores.ui.common.DbReadinessGate
import com.derekwinters.chores.ui.dashboard.DashboardNavActions
import com.derekwinters.chores.ui.dashboard.DashboardScreen
import com.derekwinters.chores.ui.log.ActivityLogScreen
import com.derekwinters.chores.ui.settings.AuthLogScreen
import com.derekwinters.chores.ui.settings.DataSettingsNavActions
import com.derekwinters.chores.ui.settings.DataSettingsScreen
import com.derekwinters.chores.ui.settings.PointsLogScreen
import com.derekwinters.chores.ui.settings.SettingsAboutScreen
import com.derekwinters.chores.ui.settings.SettingsAuthScreen
import com.derekwinters.chores.ui.settings.SettingsChoresScreen
import com.derekwinters.chores.ui.settings.SettingsGeneralScreen
import com.derekwinters.chores.ui.settings.SettingsMenuContent
import com.derekwinters.chores.ui.theme.AppThemeViewModel
import com.derekwinters.chores.ui.theme.ChoresTheme
import com.derekwinters.chores.ui.theme.ThemeAdminScreen
import com.derekwinters.chores.ui.theme.ThemePreferenceScreen
import com.derekwinters.chores.ui.settings.SettingsNavActions
import com.derekwinters.chores.ui.settings.SettingsScreen
import com.derekwinters.chores.ui.users.UserDetailScreen
import com.derekwinters.chores.ui.users.UserManagementScreen
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

/**
 * Issue #15/#17: builds the Activity Log route + query args for a "History" deep link (Chore
 * card History action, User Detail activity feed). The Log screen itself ships in issue #19.
 */
private fun logRouteWithArgs(chore: String?, person: String?): String {
    val params = listOfNotNull(
        chore?.let { "chore=${android.net.Uri.encode(it)}" },
        person?.let { "person=${android.net.Uri.encode(it)}" }
    )
    return if (params.isEmpty()) {
        ChoresDestination.ActivityLog.route
    } else {
        "${ChoresDestination.ActivityLog.route}?${params.joinToString("&")}"
    }
}

/** Issue #17: builds the "users/{personId}" route for a Dashboard card tap / User list row tap. */
private fun userDetailRoute(personId: Int, username: String): String =
    "users/$personId?username=${android.net.Uri.encode(username)}"

/**
 * Issue #59/#60: matches web's `PAGES` list (`App.jsx` lines 30-35) — order Board → Chores →
 * Users(admin) → Log, with Settings and Preferences deliberately excluded from primary nav (they
 * live only in the avatar dropdown, see [ChoresAuthenticatedScaffold]'s `TopAppBar` actions).
 * [ChoresDestination.Notification] has no web equivalent to match order against, so it's kept
 * last.
 */
private val drawerDestinations = listOf(
    ChoresDestination.Dashboard,
    ChoresDestination.Chores,
    ChoresDestination.Users,
    ChoresDestination.ActivityLog,
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
    dashboardContent: @Composable (DashboardNavActions) -> Unit = { navActions ->
        DashboardScreen(navActions = navActions)
    },
    choresContent: @Composable (assignee: String?, dueWithin: String?, navActions: ChoresNavActions) -> Unit = { assignee, dueWithin, navActions ->
        ChoreListScreen(initialAssignee = assignee, initialDueWithin = dueWithin, navActions = navActions)
    },
    choreFormContent: @Composable (onDone: () -> Unit) -> Unit = { onDone ->
        ChoreFormScreen(onSaved = onDone, onCancel = onDone)
    },
    userDetailContent: @Composable (onNavigateToHistory: () -> Unit) -> Unit = { onNavigateToHistory ->
        UserDetailScreen(onNavigateToHistory = onNavigateToHistory)
    },
    logContent: @Composable () -> Unit = { ActivityLogScreen() },
    usersContent: @Composable (onHistoryClick: (String) -> Unit) -> Unit = { onHistoryClick ->
        UserManagementScreen(onHistoryClick = onHistoryClick)
    },
    settingsContent: @Composable (SettingsNavActions) -> Unit = { navActions -> SettingsScreen(navActions = navActions) },
    authLogContent: @Composable () -> Unit = { AuthLogScreen() },
    dataSettingsContent: @Composable (DataSettingsNavActions) -> Unit = { navActions ->
        DataSettingsScreen(navActions = navActions)
    },
    pointsLogContent: @Composable () -> Unit = { PointsLogScreen() },
    preferencesContent: @Composable () -> Unit = { ThemePreferenceScreen() },
    themeAdminContent: @Composable () -> Unit = { ThemeAdminScreen() },
    currentUserProvider: @Composable () -> UiState<CurrentUser> = {
        val viewModel: CurrentUserViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsState()
        state
    },
    isDatabaseReadyProvider: @Composable () -> Boolean = {
        val viewModel: DbReadinessViewModel = hiltViewModel()
        val ready by viewModel.isReady.collectAsState()
        ready
    },
    currentThemeProvider: @Composable () -> ThemeOption? = {
        val viewModel: AppThemeViewModel = hiltViewModel()
        val theme by viewModel.currentTheme.collectAsState()
        theme
    },
    /**
     * Issue #58: the household/app title (`config.title`) shown as serif-branded nav-shell
     * chrome, matching web's `.app-title`/`.topnav-title`. Null means "not yet loaded / fetch
     * failed" — [ChoresAuthenticatedScaffold] falls back to `R.string.app_name` in that case.
     */
    currentTitleProvider: @Composable () -> String? = {
        val viewModel: AppTitleViewModel = hiltViewModel()
        val title by viewModel.appTitle.collectAsState()
        title
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
        val username = (currentUserState as? UiState.Success)?.data?.username
        val currentTheme = currentThemeProvider()
        val appTitle = currentTitleProvider()

        ChoresTheme(themeOption = currentTheme) {
            ChoresAuthenticatedScaffold(
                isAdmin = isAdmin,
                username = username,
                onLogout = onLogout,
                appTitle = appTitle,
                dashboardContent = dashboardContent,
                choresContent = choresContent,
                choreFormContent = choreFormContent,
                userDetailContent = userDetailContent,
                logContent = logContent,
                usersContent = usersContent,
                settingsContent = settingsContent,
                authLogContent = authLogContent,
                dataSettingsContent = dataSettingsContent,
                pointsLogContent = pointsLogContent,
                preferencesContent = preferencesContent,
                themeAdminContent = themeAdminContent,
                notificationContent = { NotificationScreen(onSendTestNotification = onSendTestNotification) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoresAuthenticatedScaffold(
    isAdmin: Boolean,
    /** Issue #59: shown as the avatar initial + name in the top bar's user menu. */
    username: String?,
    onLogout: () -> Unit,
    /** Issue #58: household/app title branding; null falls back to `R.string.app_name`. */
    appTitle: String?,
    modifier: Modifier = Modifier,
    dashboardContent: @Composable (DashboardNavActions) -> Unit,
    choresContent: @Composable (assignee: String?, dueWithin: String?, navActions: ChoresNavActions) -> Unit,
    choreFormContent: @Composable (onDone: () -> Unit) -> Unit,
    userDetailContent: @Composable (onNavigateToHistory: () -> Unit) -> Unit,
    logContent: @Composable () -> Unit,
    usersContent: @Composable (onHistoryClick: (String) -> Unit) -> Unit,
    settingsContent: @Composable (SettingsNavActions) -> Unit,
    authLogContent: @Composable () -> Unit,
    dataSettingsContent: @Composable (DataSettingsNavActions) -> Unit,
    pointsLogContent: @Composable () -> Unit,
    preferencesContent: @Composable () -> Unit,
    themeAdminContent: @Composable () -> Unit,
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
    // Issue #59: Settings/Preferences label lookup uses every destination (not just the drawer's
    // visibleDestinations, which they were removed from) so the TopAppBar subtitle still reads
    // "Settings"/"Preferences" rather than falling back to the app title when navigated there via
    // the avatar dropdown.
    val currentLabel = (visibleDestinations + ChoresDestination.Settings + ChoresDestination.Preferences)
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
                        // Issue #60 test fix: disambiguates drawer items from the TopAppBar
                        // subtitle, which can show the same label text (e.g. both "Board" when
                        // Dashboard is current) since drawer labels now match web's PAGES copy.
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("navItem_${destination.route}")
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            // Issue #58: household/app title branding, matching web's
                            // `.app-title`/`.topnav-title` (Playfair Display serif, 1.3rem/700/-0.5px).
                            Text(
                                text = appTitle ?: stringResource(R.string.app_name),
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.8.sp,
                                letterSpacing = (-0.5).sp,
                                modifier = Modifier.testTag("appTitleBranding")
                            )
                            Text(currentLabel, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.open_navigation_menu))
                        }
                    },
                    actions = {
                        // Issue #59: restores web's user identity treatment (`UserAvatarMenu.jsx`
                        // lines 45-51) — a colored circle with the user's initial plus their name —
                        // in place of the generic MoreVert icon.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .testTag("userMenuTrigger")
                                .clickable { userMenuExpanded = true }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text(
                                    text = username?.take(1)?.uppercase().orEmpty(),
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (username != null) {
                                Text(
                                    text = username,
                                    modifier = Modifier.padding(start = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        DropdownMenu(expanded = userMenuExpanded, onDismissRequest = { userMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.nav_preferences)) },
                                leadingIcon = { Icon(Icons.Filled.Palette, contentDescription = null) },
                                onClick = {
                                    userMenuExpanded = false
                                    navController.navigate(ChoresDestination.Preferences.route)
                                }
                            )
                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.nav_settings)) },
                                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                    onClick = {
                                        userMenuExpanded = false
                                        navController.navigate(ChoresDestination.Settings.route)
                                    }
                                )
                            }
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
                    dashboardContent(
                        DashboardNavActions(
                            onNavigateToChores = { assignee, dueWithin ->
                                navController.navigate(choresRouteWithArgs(assignee, dueWithin))
                            },
                            onNavigateToUserDetail = { personId, username ->
                                navController.navigate(userDetailRoute(personId, username))
                            }
                        )
                    )
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
                        backStackEntry.arguments?.getString("dueWithin"),
                        ChoresNavActions(
                            onNavigateToHistory = { choreName ->
                                navController.navigate(logRouteWithArgs(chore = choreName, person = null))
                            },
                            onNavigateToCreateChore = { navController.navigate("chores/new") },
                            onNavigateToEditChore = { choreId -> navController.navigate("chores/$choreId/edit") }
                        )
                    )
                }
                composable("chores/new") {
                    choreFormContent { navController.popBackStack() }
                }
                composable(
                    route = "chores/{choreId}/edit",
                    arguments = listOf(navArgument("choreId") { type = NavType.IntType })
                ) {
                    choreFormContent { navController.popBackStack() }
                }
                composable(
                    route = "${ChoresDestination.ActivityLog.route}?chore={chore}&person={person}",
                    arguments = listOf(
                        navArgument("chore") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("person") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { logContent() }
                composable(
                    route = "users/{personId}?username={username}",
                    arguments = listOf(
                        navArgument("personId") { type = NavType.IntType },
                        navArgument("username") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { backStackEntry ->
                    val username = backStackEntry.arguments?.getString("username")
                    userDetailContent {
                        navController.navigate(logRouteWithArgs(chore = null, person = username))
                    }
                }
                composable(ChoresDestination.Users.route) {
                    usersContent { username -> navController.navigate(logRouteWithArgs(chore = null, person = username)) }
                }
                navigation(startDestination = "settings/menu", route = ChoresDestination.Settings.route) {
                    composable("settings/menu") {
                        SettingsMenuContent(
                            onNavigateToGeneral = { navController.navigate("settings/general") },
                            onNavigateToAuth = { navController.navigate("settings/auth") },
                            onNavigateToChores = { navController.navigate("settings/chores") },
                            onNavigateToTheme = { navController.navigate("settings/theme") },
                            onNavigateToData = { navController.navigate("settings/data") },
                            onNavigateToAbout = { navController.navigate("settings/about") }
                        )
                    }
                    composable("settings/general") {
                        SettingsGeneralScreen(navController = navController)
                    }
                    composable("settings/auth") {
                        SettingsAuthScreen(
                            navController = navController,
                            onNavigateToAuthLog = { navController.navigate("settings/authLog") }
                        )
                    }
                    composable("settings/chores") {
                        SettingsChoresScreen(
                            navController = navController,
                            onNavigateToData = { navController.navigate("settings/data") }
                        )
                    }
                    composable("settings/about") {
                        SettingsAboutScreen(navController = navController)
                    }
                    composable("settings/authLog") { authLogContent() }
                    composable("settings/theme") { themeAdminContent() }
                    composable("settings/data") {
                        dataSettingsContent(DataSettingsNavActions(onNavigateToPointsLog = { navController.navigate("settings/data/pointsLog") }))
                    }
                    composable("settings/data/pointsLog") { pointsLogContent() }
                }
                composable(ChoresDestination.Preferences.route) { preferencesContent() }
                composable(ChoresDestination.Notification.route) { notificationContent() }
            }
        }
    }
}
