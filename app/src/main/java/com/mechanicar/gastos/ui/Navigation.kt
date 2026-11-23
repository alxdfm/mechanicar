package com.mechanicar.gastos.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mechanicar.gastos.GastosCorsaApp
import com.mechanicar.gastos.data.ExpenseRepository
import com.mechanicar.gastos.ui.screens.DashboardScreen
import com.mechanicar.gastos.ui.screens.ImportScreen
import com.mechanicar.gastos.ui.screens.VehicleScreen
import com.mechanicar.gastos.ui.screens.VisitDetailScreen
import com.mechanicar.gastos.ui.screens.VisitEditorScreen
import com.mechanicar.gastos.ui.screens.VisitListScreen

private object Routes {
    const val DASHBOARD = "dashboard"
    const val VISITS = "visits"
    const val IMPORT = "import"
    const val VEHICLE = "vehicle"
    const val VISIT_NEW = "visit/new"
    const val VISIT_DETAIL = "visit/{id}"
    const val VISIT_EDIT = "visit/{id}/edit"

    fun detail(id: Long) = "visit/$id"
    fun edit(id: Long) = "visit/$id/edit"
}

@Composable
fun GastosNavHost(@Suppress("UNUSED_PARAMETER") repository: ExpenseRepository) {
    val app = LocalContext.current.applicationContext as GastosCorsaApp
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(app))
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbar.showSnackbar(it) }
    }

    val showFab = route == Routes.VISITS
    val showBottomBar = route in setOf(Routes.DASHBOARD, Routes.VISITS, Routes.IMPORT)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(onClick = { navController.navigate(Routes.VISIT_NEW) }) {
                    Icon(Icons.Default.Add, contentDescription = "Nova manutenção")
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == Routes.DASHBOARD,
                        onClick = {
                            navController.navigate(Routes.DASHBOARD) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        label = { Text("Resumo") }
                    )
                    NavigationBarItem(
                        selected = route == Routes.VISITS,
                        onClick = {
                            navController.navigate(Routes.VISITS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text("Manutenções") }
                    )
                    NavigationBarItem(
                        selected = route == Routes.IMPORT,
                        onClick = {
                            navController.navigate(Routes.IMPORT) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                        label = { Text("Importar") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onOpenVisits = { navController.navigate(Routes.VISITS) },
                    onEditVehicle = { navController.navigate(Routes.VEHICLE) }
                )
            }
            composable(Routes.VISITS) {
                VisitListScreen(
                    viewModel = viewModel,
                    onOpenVisit = { id -> navController.navigate(Routes.detail(id)) }
                )
            }
            composable(Routes.IMPORT) {
                ImportScreen(viewModel = viewModel)
            }
            composable(Routes.VEHICLE) {
                VehicleScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.VISIT_NEW) {
                VisitEditorScreen(
                    visitId = null,
                    viewModel = viewModel,
                    onDone = { id ->
                        navController.popBackStack()
                        navController.navigate(Routes.detail(id))
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.VISIT_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                VisitDetailScreen(
                    visitId = id,
                    viewModel = viewModel,
                    onEdit = { navController.navigate(Routes.edit(id)) },
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        navController.popBackStack(Routes.VISITS, inclusive = false)
                    }
                )
            }
            composable(
                route = Routes.VISIT_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                VisitEditorScreen(
                    visitId = id,
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}
