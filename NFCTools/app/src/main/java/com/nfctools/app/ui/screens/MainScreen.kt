package com.nfctools.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.nfctools.app.viewmodel.NFCViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: NFCViewModel) {
    val navController = rememberNavController()
    val errorMessage by viewModel.errorMessage
    val successMessage by viewModel.successMessage
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage, successMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long); viewModel.clearMessages() }
        successMessage?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NFC Tools Pro") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = { BottomNavigationBar(navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        NavHost(navController = navController, startDestination = "scan", modifier = Modifier.padding(padding)) {
            composable("scan") { ScanScreen(viewModel) }
            composable("info") { TagInfoScreen(viewModel) }
            composable("write") { WriteScreen(viewModel) }
            composable("tools") { ToolsScreen(viewModel) }
            composable("history") { HistoryScreen(viewModel) }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Triple("scan", Icons.Default.Nfc, "Scan"),
        Triple("info", Icons.Default.Info, "Info"),
        Triple("write", Icons.Default.Edit, "Write"),
        Triple("tools", Icons.Default.Build, "Tools"),
        Triple("history", Icons.Default.History, "History")
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { (route, icon, label) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == route,
                onClick = { navController.navigate(route) { popUpTo(navController.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } }
            )
        }
    }
}
