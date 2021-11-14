package com.alangeorge.bleplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import arrow.core.None
import arrow.core.left
import arrow.core.toOption
import com.alangeorge.bleplay.model.SnackbarMessage
import com.alangeorge.bleplay.ui.*
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import com.alangeorge.bleplay.viewmodel.BleViewModel
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.systemBarsPadding
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@ExperimentalPermissionsApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProvideWindowInsets {
                BLEPlayTheme {
                    val appState = rememberAppState()
                    val currentRoute by appState.currentRoute.collectAsState()
                    Scaffold(
                        bottomBar = {
                            StartBottomBar(
                                navigateToRoute = appState.navController::navigate,
                                items = appState.bottomBarTabs,
                                currentRoute = currentRoute
                            )
                        },
                        snackbarHost = {
                            SnackbarHost(
                                hostState = it,
                                modifier = Modifier.systemBarsPadding(),
                                snackbar = { snackbarData ->
                                    Snackbar(snackbarData)
                                }
                            )
                        },
                        scaffoldState = appState.scaffoldState
                    ) { innerPaddingModifier ->
                        NavHost(
                            navController = appState.navController,
                            startDestination = StartScreens.ScreenOne.route,
                            modifier = Modifier.padding(innerPaddingModifier)
                        ) {
                            navGraph(appState = appState)
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalPermissionsApi
fun NavGraphBuilder.navGraph(modifier: Modifier = Modifier, appState: AppState) {
    composable(StartScreens.ScreenOne.route) {
        ScreenOne()
    }
    composable(StartScreens.ScreenTwo.route) {
        ScreenTwo()
    }
    composable(StartScreens.ScreenFour.route) {
        Text("ScreenFour")
    }
    navigation(startDestination = DEVICE_ROUTE_SCAN, route = DEVICE_ROUTE_BASE) {
        composable(DEVICE_ROUTE_SCAN) {
            val parentEntry = remember {
                appState.navController.getBackStackEntry(DEVICE_ROUTE_BASE)
            }
            val viewModel = hiltViewModel<BleViewModel>(parentEntry)
            val scanResults by viewModel.scanResults.collectAsState(initial = emptyList())
            ScreenBleScan(
                scanResults = scanResults,
                startScan = {
                    appState.coroutineScope.launch {
                        viewModel.startScan()
                        appState.snackbarMessagePipeline.produceEvent(SnackbarMessage(R.string.ble_start_scan_message.left()))
                    }
                },
                stopScan = {
                    viewModel.stopScan()
                    appState.coroutineScope.launch {
                        appState.snackbarMessagePipeline.produceEvent(SnackbarMessage(R.string.ble_stop_scan_message.left()))
                    }
                },
                status = viewModel::scanStatus,
                deviceOnClick = { address ->
                    appState.navController.navigate("$DEVICE_ROUTE_BASE/$address")
                }
            )
        }
        composable(
            route = "$DEVICE_ROUTE_BASE/{$DEVICE_ADDRESS_ARG_NAME}",
            arguments = listOf(navArgument(DEVICE_ADDRESS_ARG_NAME) { type = NavType.StringType })
        ) { backStackEntry ->
            val parentEntry = remember {
                appState.navController.getBackStackEntry(DEVICE_ROUTE_BASE)
            }
            val arguments = requireNotNull(backStackEntry.arguments)
            val deviceAddress = arguments.getString(DEVICE_ADDRESS_ARG_NAME)
            val viewModel = hiltViewModel<BleViewModel>(parentEntry)
            val scanResult = remember {
                deviceAddress?.let { viewModel.findDevice(it).toOption() } ?: None
            }

            scanResult.fold(
                { Text("Device not found") },
                { ScreenBleDeviceDetail(scanResult = it) }
            )
        }
    }
}

