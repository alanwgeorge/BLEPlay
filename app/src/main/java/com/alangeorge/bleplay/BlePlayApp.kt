package com.alangeorge.bleplay

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.alangeorge.bleplay.ui.*
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import com.alangeorge.bleplay.viewmodel.BleDeviceViewModel
import com.alangeorge.bleplay.viewmodel.BleViewModel
import com.alangeorge.bleplay.viewmodel.serviceFilters
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.systemBarsPadding
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@ExperimentalPermissionsApi
@ExperimentalAnimationApi
@Composable
fun BlePlayApp() {
    ProvideWindowInsets {
        BLEPlayTheme {
            val appState = rememberAppState()
            val currentRoute by appState.currentRoute.collectAsState()
            val shouldShowBottomBar by appState.shouldShowBottomBarFlow.collectAsState(initial = true)
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = shouldShowBottomBar,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        BottomBar(
                            navigateToRoute = appState::navigateBottomBar,
                            items = appState.bottomBarTabs,
                            currentRoute = currentRoute
                        )
                    }
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
                AnimatedNavHost(
                    navController = appState.navController,
                    startDestination = DEVICE_ROUTE_BASE,
                    modifier = Modifier
                        .padding(innerPaddingModifier)
                        .fillMaxSize()
                ) {
                    navGraph(appState = appState)
                }
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalPermissionsApi
fun NavGraphBuilder.navGraph(modifier: Modifier = Modifier, appState: AppState) {
    composable(BottomBarScreens.ScreenThree.route) {
        ScreenThree()
    }
    composable(BottomBarScreens.ScreenTwo.route) {
        ScreenTwo()
    }
    composable(BottomBarScreens.ScreenFour.route) {
        Text("ScreenFour")
    }
    navigation(startDestination = BottomBarScreens.ScreenOne.route, route = DEVICE_ROUTE_BASE) {
        val deviceScreenAnimationSpec = tween<IntOffset>(700)
        composable(
            BottomBarScreens.ScreenOne.route,
            enterTransition = { _, _ -> fadeIn() },
            exitTransition = { _, target ->
                when(target.destination.route) {
                    DEVICE_ROUTE -> slideOutOfContainer(AnimatedContentScope.SlideDirection.Left, animationSpec = deviceScreenAnimationSpec)
                    else -> fadeOut()
                }
            },
            popEnterTransition = { initial, _ ->
                when(initial.destination.route) {
                    DEVICE_ROUTE -> slideIntoContainer(AnimatedContentScope.SlideDirection.Right, animationSpec = deviceScreenAnimationSpec)
                    else -> fadeIn()
                }
            }
        ) {
            val parentEntry = remember {
                appState.navController.getBackStackEntry(DEVICE_ROUTE_BASE)
            }
            val viewModel = hiltViewModel<BleViewModel>(parentEntry)
            val scanResults by viewModel.scanResults.collectAsState(initial = emptyList())
            val selectedFilter by viewModel.selectedServiceFilter.collectAsState()
            val isScanning by viewModel.isScanning.collectAsState()
            ScreenBleScanPermissionsWrapper(
                startScan = viewModel::startScan,
                stopScan =  viewModel::stopScan,
                isScanning = isScanning,
                status = viewModel::scanStatus,
                clearResults = viewModel::clearResults,
                deviceOnClick = { address ->
                    viewModel.stopScan()
                    appState.navController.navigate("$DEVICE_ROUTE_BASE/$address")
                },
                setFilter = { filterIndex ->
                    viewModel.selectedServiceFilter.tryEmit(filterIndex)
                },
                filters = serviceFilters.map(Pair<*, String>::second) ,
                selectedFilter = selectedFilter,
                scanResults = scanResults,
            )
        }
        composable(
            route = DEVICE_ROUTE,
            arguments = listOf(navArgument(DEVICE_ADDRESS_ARG_NAME) { type = NavType.StringType }),
            enterTransition = { _, _ ->
                slideIntoContainer(AnimatedContentScope.SlideDirection.Left, animationSpec = deviceScreenAnimationSpec)
            },
            popExitTransition = { _, _ ->
                slideOutOfContainer(AnimatedContentScope.SlideDirection.Right, animationSpec = deviceScreenAnimationSpec)
            }
        ) {
            val deviceViewModel = hiltViewModel<BleDeviceViewModel>()
            val scanResults by deviceViewModel.scanResultsFlow.collectAsState()
            val discoveredServices by deviceViewModel.discoveredServiceFlow.collectAsState(initial = emptyList())
            val mtu by deviceViewModel.mtuChangedFlow.collectAsState(initial = null)
            val batteryLevel by deviceViewModel.batteryLevelFlow.collectAsState(initial = null)
            val isConnected by deviceViewModel.connectedStatusFlow.collectAsState()
            val isConnecting by deviceViewModel.isConnectingStatusFlow.collectAsState()
            val connectOnClick = remember { {
                if (isConnected) deviceViewModel.disconnectGatts() else deviceViewModel.connectGatt()
            } }

            scanResults?.let {
                ScreenBleDeviceDetail(
                    scanData = it,
                    mtu = mtu,
                    batteryLevel = batteryLevel,
                    discoveredServices = discoveredServices,
                    bondOnClick = deviceViewModel::bond,
                    isConnected = isConnected,
                    connectOnClick = connectOnClick,
                    isConnecting = isConnecting,
                    onBondStateChange = deviceViewModel::handleBleBondIntent
                )

            } ?: Text("Device not found")
        }
    }
}

