package com.alangeorge.bleplay.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.coroutines.resume

@Composable
fun BleSnackbar(snackbarData: BleSnackbarData) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = snackbarData.color,
            shape = RoundedCornerShape(50),
            elevation = 6.dp
        ) {
            Row(modifier = Modifier.padding(12.dp)) {
                Text(text = snackbarData.message)
            }
        }
    }
}

@Stable
class BleSnackbarHostState {
    private val mutex = Mutex()

    var currentSnackbarData by mutableStateOf<BleSnackbarData?>(null)
        private set

    suspend fun showSnackbar(message: String, color: Color, duration: SnackbarDuration): SnackbarResult = mutex.withLock {
        try {
            return suspendCancellableCoroutine { continuation ->
                currentSnackbarData = SnackbarDataImpl(message, color, duration, continuation)
            }
        } finally {
            currentSnackbarData = null
        }
    }

    class SnackbarDataImpl(
        override val message: String,
        override val color: Color,
        override val duration: SnackbarDuration,
        private val continuation: CancellableContinuation<SnackbarResult>
    ) : BleSnackbarData {
        override fun dismiss() {
            if (continuation.isActive) continuation.resume(SnackbarResult.Dismissed)
        }
    }
}

interface BleSnackbarData {
    val message: String
    val color: Color
    val duration: SnackbarDuration

    fun dismiss()
}

@Composable
fun BleSnackbarHost(
    hostState: BleSnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (BleSnackbarData) -> Unit = { BleSnackbar(it) }
) {
    val currentSnackbarData = hostState.currentSnackbarData

    LaunchedEffect(currentSnackbarData) {
        if (currentSnackbarData != null) {
            val duration = when(currentSnackbarData.duration) {
                SnackbarDuration.Indefinite -> Long.MAX_VALUE
                SnackbarDuration.Long -> 10000L
                SnackbarDuration.Short -> 4000L
            }
            delay(duration)
            currentSnackbarData.dismiss()
        }
    }

    FadeInFadeOutWithScale(
        current = hostState.currentSnackbarData,
        modifier = modifier,
        content = snackbar
    )
}


@Preview(showBackground = true, group = "Snackbar")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, group = "Snackbar")
@Composable
fun BleSnackbarPreview(
    @PreviewParameter(BleSnackbarDataProvider::class) snackbarData: BleSnackbarData
) {
    BLEPlayTheme {
        BleSnackbar(snackbarData)
    }
}

class BleSnackbarDataProvider : PreviewParameterProvider<BleSnackbarData> {
    val one = object: BleSnackbarData {
        override val message = "This is a test message"
        override val color = Color.Green.copy(alpha = .3f)
        override val duration = SnackbarDuration.Short
        override fun dismiss() { }
    }
    val two = object: BleSnackbarData {
        override val message = "This is a long long long long long long long test message"
        override val color = Color.Red.copy(alpha = .3f)
        override val duration = SnackbarDuration.Short
        override fun dismiss() { }
    }

    override val values = sequenceOf(
        one,
        two
    )
}

// This was copied from ScaffoldTemplate's Snackbar implementation and added translationY.
// This seem overly complicated for this use case.
// TODO reimplement
@Composable
private fun FadeInFadeOutWithScale(
    current: BleSnackbarData?,
    modifier: Modifier = Modifier,
    content: @Composable (BleSnackbarData) -> Unit
) {
    val state = remember { FadeInFadeOutState<BleSnackbarData?>() }
    if (current != state.current) {
        state.current = current
        val keys = state.items.map { it.key }.toMutableList()
        if (!keys.contains(current)) {
            keys.add(current)
        }
        state.items.clear()
        keys.filterNotNull().mapTo(state.items) { key ->
            FadeInFadeOutAnimationItem(key) { children ->
                val isVisible = key == current
                val duration = if (isVisible) SnackbarFadeInMillis else SnackbarFadeOutMillis
                val delay = SnackbarFadeOutMillis + SnackbarInBetweenDelayMillis
                val animationDelay = if (isVisible && keys.filterNotNull().size != 1) delay else 0
                val opacity = animatedOpacity(
                    animation = tween(
                        easing = LinearEasing,
                        delayMillis = animationDelay,
                        durationMillis = duration
                    ),
                    visible = isVisible,
                    onAnimationFinish = {
                        if (key != state.current) {
                            // leave only the current in the list
                            state.items.removeAll { it.key == key }
                            state.scope?.invalidate()
                        }
                    }
                )
                val scale = animatedScale(
                    animation = tween(
                        easing = FastOutSlowInEasing,
                        delayMillis = animationDelay,
                        durationMillis = duration
                    ),
                    visible = isVisible
                )
                val y = animatedY(
                    animation = tween(
                        easing = FastOutSlowInEasing,
                        delayMillis = animationDelay,
                        durationMillis = duration
                    ),
                    visible = isVisible
                )
                Box(
                    Modifier
                        .graphicsLayer(
                            translationY = y.value,
                            scaleX = scale.value,
                            scaleY = scale.value,
//                            alpha = opacity.value
                        )
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            dismiss { key.dismiss(); true }
                        }
                ) {
                    children()
                }
            }
        }
    }
    Box(modifier) {
        state.scope = currentRecomposeScope
        state.items.forEach { (item, opacity) ->
            key(item) {
                opacity {
                    content(item!!)
                }
            }
        }
    }
}

private class FadeInFadeOutState<T> {
    // we use Any here as something which will not be equals to the real initial value
    var current: Any? = Any()
    var items = mutableListOf<FadeInFadeOutAnimationItem<T>>()
    var scope: RecomposeScope? = null
}

private data class FadeInFadeOutAnimationItem<T>(
    val key: T,
    val transition: FadeInFadeOutTransition
)

private typealias FadeInFadeOutTransition = @Composable (content: @Composable () -> Unit) -> Unit

@Composable
private fun animatedOpacity(
    animation: AnimationSpec<Float>,
    visible: Boolean,
    onAnimationFinish: () -> Unit = {}
): State<Float> {
    val alpha = remember { Animatable(if (!visible) 1f else 0f) }
    LaunchedEffect(visible) {
        alpha.animateTo(
            if (visible) 1f else 0f,
            animationSpec = animation
        )
        onAnimationFinish()
    }
    return alpha.asState()
}

@Composable
private fun animatedScale(animation: AnimationSpec<Float>, visible: Boolean): State<Float> {
    val scale = remember { Animatable(if (!visible) 1f else 0f) }
    LaunchedEffect(visible) {
        scale.animateTo(
            if (visible) 1f else 0f,
            animationSpec = animation
        )
    }
    return scale.asState()
}

@Composable
private fun animatedY(animation: AnimationSpec<Float>, visible: Boolean): State<Float> {
    val y = remember { Animatable(if (!visible) 0f else -300f) }
    LaunchedEffect(visible) {
        y.animateTo(
            if (visible) 0f else -300f,
            animationSpec = animation
        )
    }
    return y.asState()
}

private const val SnackbarFadeInMillis = 400
private const val SnackbarFadeOutMillis = 200
private const val SnackbarInBetweenDelayMillis = 0