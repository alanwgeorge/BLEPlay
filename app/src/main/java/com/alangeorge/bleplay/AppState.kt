package com.alangeorge.bleplay

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.alangeorge.bleplay.common.Pipeline
import com.alangeorge.bleplay.model.SnackbarMessage
import com.alangeorge.bleplay.ui.StartScreens
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

@Composable
fun rememberAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    navController: NavHostController = rememberNavController(),
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

    @Composable
    fun shouldShowBottomBar() = true

    val bottomBarTabs = listOf(
        StartScreens.ScreenOne,
        StartScreens.ScreenTwo,
        StartScreens.ScreenThree,
        StartScreens.ScreenFour
    )
}
