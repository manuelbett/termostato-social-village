package it.termostato.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.termostato.app.data.ApiClient
import it.termostato.app.data.SessionManager
import it.termostato.app.ui.*
import it.termostato.app.ui.theme.TermostatoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Inizializza la WebView per le chiamate API
        ApiClient.init(this)
        setContent {
            TermostatoTheme {
                TermostatoApp()
            }
        }
    }
}

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Home : Screen("home")
    data object Zones : Screen("zones")
    data object ZoneDetail : Screen("zone_detail")
    data object Consumption : Screen("consumption")
    data object History : Screen("history")
    data object NetworkLog : Screen("network_log")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermostatoApp(viewModel: MainViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsState()
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { SessionManager(context) }

    // Se non loggato → login, altrimenti home
    val startRoute = if (ui.isLoggedIn) Screen.Home.route else Screen.Login.route

    Scaffold(
        bottomBar = {
            if (ui.isLoggedIn) {
                var showLogoutDialog by remember { mutableStateOf(false) }
                BottomNavBar(
                    navController = navController,
                    profile = ui.profile,
                    onLogout = { showLogoutDialog = true }
                )
                // Dialog conferma logout
                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        title = { Text("Esci") },
                        text = { Text("Vuoi uscire dall'applicazione?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showLogoutDialog = false
                                viewModel.logout()
                            }) {
                                Text("Esci", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogoutDialog = false }) {
                                Text("Annulla")
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    savedApartmentId = session.savedApartmentId,
                    savedPin = session.savedPin,
                    rememberMe = session.rememberMe,
                    isLoading = ui.isLoading,
                    error = ui.error,
                    sessionExpired = ui.sessionExpired,
                    onLogin = { id, pin, remember ->
                        viewModel.login(id, pin, remember)
                    }
                )
                // Se diventa loggato → naviga a home
                LaunchedEffect(ui.isLoggedIn) {
                    if (ui.isLoggedIn) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    ui = ui,
                    onRefresh = { viewModel.refreshStatus() },
                    onSetStatus = { viewModel.setStatus(it) },
                    onAdjust = { viewModel.adjustSetPoint(it) },
                    onConfirm = { viewModel.confirmSetPoint() },
                    onCancel = { viewModel.cancelSetPoint() }
                )
            }

            composable(Screen.Zones.route) {
                ZonesScreen(
                    ui = ui,
                    onLoadZones = { viewModel.loadZones() },
                    onSelectZone = { zone ->
                        viewModel.selectZone(zone)
                        navController.navigate(Screen.ZoneDetail.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ZoneDetail.route) {
                ZoneDetailScreen(
                    ui = ui,
                    onBack = { navController.popBackStack() },
                    onAdjust = { viewModel.adjustZone(it) },
                    onConfirm = { viewModel.confirmZoneAdjustment() },
                    onCancel = { viewModel.cancelZoneAdjustment() },
                    onRename = { name ->
                        ui.selectedZone?.let { viewModel.renameZone(it.id, name) }
                    }
                )
            }

            composable(Screen.Consumption.route) {
                ConsumptionScreen(
                    ui = ui,
                    onLoad = { viewModel.loadConsumption() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    ui = ui,
                    onLoad = { viewModel.loadMetersHistory() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.NetworkLog.route) {
                NetworkLogScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    // Se errore → mostra snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.error) {
        ui.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Se sessione scaduta → torna a login
    LaunchedEffect(ui.isLoggedIn) {
        if (!ui.isLoggedIn && navController.currentDestination?.route != Screen.Login.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavHostController, profile: String, onLogout: () -> Unit) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == Screen.Home.route,
            onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        )

        // Zone solo per profilo ADVANCED/MEDIUM
        if (profile == "ADVANCED" || profile == "MEDIUM") {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Kitchen, contentDescription = "Zone") },
                label = { Text("Zone") },
                selected = currentRoute == Screen.Zones.route || currentRoute == Screen.ZoneDetail.route,
                onClick = {
                    navController.navigate(Screen.Zones.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        NavigationBarItem(
            icon = { Icon(Icons.Default.Bolt, contentDescription = "Consumi") },
            label = { Text("Consumi") },
            selected = currentRoute == Screen.Consumption.route,
            onClick = {
                navController.navigate(Screen.Consumption.route) {
                    popUpTo(Screen.Home.route)
                }
            }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = "Storico") },
            label = { Text("Storico") },
            selected = currentRoute == Screen.History.route,
            onClick = {
                navController.navigate(Screen.History.route) {
                    popUpTo(Screen.Home.route)
                }
            }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.NetworkCheck, contentDescription = "Rete") },
            label = { Text("Rete") },
            selected = currentRoute == Screen.NetworkLog.route,
            onClick = {
                navController.navigate(Screen.NetworkLog.route) {
                    popUpTo(Screen.Home.route)
                }
            }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Logout, contentDescription = "Esci") },
            label = { Text("Esci") },
            selected = false,
            onClick = onLogout
        )
    }
}
