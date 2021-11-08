package com.alangeorge.bleplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.alangeorge.bleplay.ui.ScreenOne
import com.alangeorge.bleplay.ui.ScreenTwo
import com.alangeorge.bleplay.ui.StartBottomBar
import com.alangeorge.bleplay.ui.StartScreens
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.systemBarsPadding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.asStateFlow

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
                            navGraph()
                        }
                    }
                }
            }
        }
    }
}

fun NavGraphBuilder.navGraph(modifier: Modifier = Modifier) {
    composable(StartScreens.ScreenOne.route) {
        ScreenOne()
    }
    composable(StartScreens.ScreenTwo.route) {
        ScreenTwo()
    }
    composable(StartScreens.ScreenThree.route) {
        Text("ScreenThree")
    }
    composable(StartScreens.ScreenFour.route) {
        Text("ScreenFour")
    }
}

