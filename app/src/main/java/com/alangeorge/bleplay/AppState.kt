package com.alangeorge.bleplay

import android.content.Context
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.alangeorge.bleplay.common.Pipeline
import com.alangeorge.bleplay.model.SnackbarMessage
import com.alangeorge.bleplay.ui.BottomBarScreens
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber


@EntryPoint
@InstallIn(SingletonComponent::class)
interface SnackbarMessagePipelineEntryPoint {
    fun getSnackbarMessagePipeline(): Pipeline<SnackbarMessage>
}

@ExperimentalAnimationApi
@Composable
fun rememberAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    navController: NavHostController = rememberAnimatedNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current.applicationContext
) =
    remember(scaffoldState, navController, coroutineScope) {
        AppState(scaffoldState, navController, coroutineScope, context)
    }

class AppState(
    val scaffoldState: ScaffoldState,
    val navController: NavHostController,
    val coroutineScope: CoroutineScope,
    context: Context
) {
    val snackbarMessagePipeline = EntryPoints.get(context, SnackbarMessagePipelineEntryPoint::class.java).getSnackbarMessagePipeline()

    val bottomBarTabs = listOf(
            BottomBarScreens.ScreenOne,
            BottomBarScreens.ScreenTwo,
            BottomBarScreens.ScreenThree,
            BottomBarScreens.ScreenFour
    )
    private val routesWithBottomNav = bottomBarTabs.map(BottomBarScreens::route)

    init {
        coroutineScope.launch {
            snackbarMessagePipeline.events
                .map { message ->
                    message.message.fold(
                        { context.getString(it) },
                        { it }
                    )
                }
                .collect { message ->
                    Timber.d("snackbar message: $message")
                    scaffoldState.snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                }
        }

        navController.addOnDestinationChangedListener { controller, destination, args ->
            Timber.d("navigation change: controller:$controller, destination:$destination, args:$args")

            destination.route?.let {
                coroutineScope.launch {
                    currentRouteFlow.emit(it)
                }
            }
        }
    }

    private val currentRouteFlow = MutableStateFlow("")
    val currentRoute = currentRouteFlow.asStateFlow()

    val shouldShowBottomBarFlow = currentRouteFlow.map(routesWithBottomNav::contains)

    fun navigateBottomBar(screen: BottomBarScreens) {
        navController.navigate(screen.route) {
            popUpTo(0) {
                inclusive = true
            }
        }
    }
}
