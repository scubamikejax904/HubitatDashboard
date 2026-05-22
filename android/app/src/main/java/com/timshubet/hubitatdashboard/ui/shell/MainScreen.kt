package com.timshubet.hubitatdashboard.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.timshubet.hubitatdashboard.ui.group.GroupScreen
import com.timshubet.hubitatdashboard.ui.ring.RingListenerScreen
import com.timshubet.hubitatdashboard.ui.settings.SettingsScreen
import com.timshubet.hubitatdashboard.viewmodel.DeviceViewModel
import com.timshubet.hubitatdashboard.viewmodel.GroupEditViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    isConfigured: Boolean,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val viewModel: DeviceViewModel = hiltViewModel()
    val groupEditViewModel: GroupEditViewModel = hiltViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    DisposableEffect(lifecycleOwner, isConfigured) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && isConfigured) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route ?: NavRoutes.DEFAULT_GROUP
    val currentGroupId = if (currentRoute.startsWith("group/"))
        currentRoute.removePrefix("group/") else ""

    val isEditMode by groupEditViewModel.isEditMode.collectAsState()
    val resolvedGroups by groupEditViewModel.resolvedGroups.collectAsState()
    val customGroups by groupEditViewModel.customGroups.collectAsState()
    val defaultGroupId by groupEditViewModel.defaultGroupId.collectAsState()

    val showEditToggle = currentRoute.startsWith("group/")

    val startDestination = if (isConfigured) NavRoutes.group(defaultGroupId) else NavRoutes.SETTINGS

    // Navigate to the default group whenever edit mode is turned off
    var wasEditing by remember { mutableStateOf(false) }
    LaunchedEffect(isEditMode) {
        if (wasEditing && !isEditMode) {
            navController.navigate(NavRoutes.group(defaultGroupId)) {
                launchSingleTop = true
            }
        }
        wasEditing = isEditMode
    }

    val currentGroupLabel = resolvedGroups.find { it.id == currentGroupId }?.displayName
        ?: allDrawerGroups.find { it.id == currentGroupId }?.label
        ?: when (currentRoute) {
            NavRoutes.SETTINGS -> "Settings"
            NavRoutes.RING_LISTENER -> "Ring Listener"
            else -> "Hubitat Dashboard"
        }

    // Breadcrumb: find if current group is a child of another group
    val parentGroupId = customGroups.find { it.id == currentGroupId }?.parentId
    val parentGroupLabel = parentGroupId?.let { pid ->
        resolvedGroups.find { it.id == pid }?.displayName
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            GroupDrawer(
                currentGroupId = currentGroupId,
                drawerState = drawerState,
                scope = scope,
                onGroupSelected = { navController.navigate(NavRoutes.group(it)) },
                groupEditViewModel = groupEditViewModel,
                isEditMode = isEditMode
            )
        }
    ) {
        Scaffold(
            topBar = {
                HubitatTopBar(
                    title = currentGroupLabel,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                    onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) },
                    isEditMode = isEditMode,
                    showEditToggle = showEditToggle,
                    onToggleEditMode = { groupEditViewModel.toggleEditMode() },
                    parentGroupLabel = parentGroupLabel,
                    onParentClick = parentGroupId?.let { pid ->
                        { navController.navigate(NavRoutes.group(pid)) }
                    }
                )
            },
            bottomBar = {
                if (currentRoute != NavRoutes.SETTINGS && currentRoute != NavRoutes.RING_LISTENER) {
                    GroupBottomNav(
                        currentGroupId = currentGroupId,
                        onGroupSelected = { navController.navigate(NavRoutes.group(it)) },
                        onMoreClick = { scope.launch { drawerState.open() } }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                SystemStatusRow(viewModel = viewModel)
                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(NavRoutes.SETTINGS) {
                            SettingsScreen(
                                onSaveSuccess = {
                                    viewModel.refresh()
                                    navController.navigate(NavRoutes.group(defaultGroupId)) {
                                        popUpTo(NavRoutes.SETTINGS) { inclusive = true }
                                    }
                                },
                                onRingListenerClick = {
                                    navController.navigate(NavRoutes.RING_LISTENER)
                                }
                            )
                        }
                        composable(NavRoutes.RING_LISTENER) {
                            RingListenerScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(NavRoutes.GROUP_PATTERN) { backStackEntry ->
                            val groupId = backStackEntry.arguments?.getString("groupId") ?: "environment"
                            GroupScreen(
                                groupId = groupId,
                                viewModel = viewModel,
                                groupEditViewModel = groupEditViewModel,
                                onNavigateToGroup = { navController.navigate(NavRoutes.group(it)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
