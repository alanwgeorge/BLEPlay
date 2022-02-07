package com.alangeorge.bleplay.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

@ExperimentalMaterialApi
@Composable
fun Choice(
    choice: Either<Left, Right>,
    onChoiceChange: (Either<Left, Right>) -> Unit,
    leftLabel: String,
    rightLabel: String,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: ChoiceColors = ChoiceDefaults.colors(),
    contentPaddingValues: PaddingValues = ChoiceDefaults.contentPadding()
) {
    val scope = rememberCoroutineScope()

    val swipeableState = rememberSwipeableStateFor(
        choice,
        onChoiceChange,
        TweenSpec(durationMillis = 200)
    )
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val toggleableModifier =
            Modifier.toggleable(
                value = choice.isLeft(),
                onValueChange = {
                    if (it) {
                        onChoiceChange(Left.left())
                    } else {
                        onChoiceChange(Right.right())
                    }
                },
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null
            )

    val left = @Composable {
        Text(
            modifier = modifier.padding(contentPaddingValues),
            text = leftLabel,
            style = BLEPlayTheme.typography.button,
            color = colors.contentColor().value
        )
    }
    val right = @Composable {
        Text(
            modifier = modifier.padding(contentPaddingValues),
            text = rightLabel,
            style = BLEPlayTheme.typography.button,
            color = colors.contentColor().value
        )
    }

    val track = @Composable { animationPathLength: Float ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colors.trackColor().value,
                    shape = RoundedCornerShape(50)
                )
                .then(toggleableModifier)
                .swipeable(
                    state = swipeableState,
                    anchors = mapOf(0f to Left.left(), animationPathLength to Right.right()),
                    thresholds = { _, _ -> FractionalThreshold(0.5f) },
                    orientation = Orientation.Horizontal,
                    enabled = true,
                    reverseDirection = isRtl,
                    interactionSource = interactionSource,
                    resistance = null
                )
        )
    }
    val thumb = @Composable {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colors.thumbColor().value,
                    shape = RoundedCornerShape(50)
                )
                .indication(
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = false, radius = 24.dp)
                )
        )
    }

    val gap = with(LocalDensity.current) {
        0 * density
    }.roundToInt()

    val thumbOffset by swipeableState.offset

    SubcomposeLayout { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val leftPlaceables = subcompose(slotId = ChoiceSlots.Left, content = left).map {
            it.measure(constraints = looseConstraints)
        }
        val rightPlaceables = subcompose(slotId = ChoiceSlots.Right, content = right).map {
            it.measure(constraints = looseConstraints)
        }

        val leftWidth = leftPlaceables.maxOfOrNull { it.width } ?: 0
        val leftHeight = leftPlaceables.maxOfOrNull { it.height } ?: 0
        val rightWidth = rightPlaceables.maxOfOrNull { it.width } ?: 0
        val rightHeight = rightPlaceables.maxOfOrNull { it.height } ?: 0

        val layoutWidth = leftWidth + gap + rightWidth
        val layoutHeight = maxOf(leftHeight, rightHeight)

        val animationPathLength = leftWidth + gap



        val thumbWidth = when {
            thumbOffset.roundToInt() == 0 -> leftWidth
            swipeableState.direction == 1f -> (((rightWidth - leftWidth) * swipeableState.progress.fraction) + leftWidth).roundToInt()
            swipeableState.direction == -1f -> (((leftWidth - rightWidth) * swipeableState.progress.fraction) + rightWidth).roundToInt()
            thumbOffset.roundToInt() == animationPathLength -> rightWidth
            choice.isRight() -> rightWidth
            else -> leftWidth
        }

//        scope.launch {
//            Timber.d("thumbOffset:$thumbOffset thumbOffset.roundToInt():${thumbOffset.roundToInt()} direction:${swipeableState.direction} fraction:${swipeableState.progress.fraction} leftWidth:$leftWidth rightWidth:$rightWidth thumbWidth:$thumbWidth")
//        }

        val trackPlaceables = subcompose(slotId = ChoiceSlots.Track, content = { track((animationPathLength).toFloat()) }).map {
            it.measure(constraints = looseConstraints.copy(maxWidth = layoutWidth, minHeight = layoutHeight))
        }

        val thumbPlaceables = subcompose(slotId = ChoiceSlots.Thumb, content = thumb).map {
            it.measure(constraints = looseConstraints.copy(maxWidth = thumbWidth, minHeight = layoutHeight))
        }

        layout(layoutWidth, layoutHeight) {
            trackPlaceables.forEach {
                it.place(0, 0)
            }
            thumbPlaceables.forEach {
                it.place(thumbOffset.roundToInt(), 0)
            }
            leftPlaceables.forEach {
                it.place(0, 0)
            }
            rightPlaceables.forEach {
                it.place(leftWidth + gap, 0)
            }
        }
    }

    LaunchedEffect(key1 = swipeableState) {
        snapshotFlow { swipeableState.offset.value }
            .onCompletion {
                Timber.d("offset flow completion")
            }
            .collect {
                Timber.d("offset flow: $it")
            }
    }
}

object Left
object Right

private enum class ChoiceSlots {
    Left, Right, Track, Thumb
}

@Stable
interface ChoiceColors {
    @Composable
    fun thumbColor(): State<Color>

    @Composable
    fun trackColor(): State<Color>

    @Composable
    fun contentColor(): State<Color>
}


object ChoiceDefaults {
    @Composable
    fun colors(
        trackColor: Color = BLEPlayTheme.colors.primary,
        thumbAlpha: Float = 0.54f,
        thumbColor: Color = BLEPlayTheme.colors.onPrimary.copy(alpha = thumbAlpha),
        contentColor: Color = BLEPlayTheme.colors.onPrimary
    ): ChoiceColors = DefaultChoiceColors(
        thumbColor = thumbColor,
        trackColor = trackColor,
        contentColor = contentColor
    )
    @Composable
    fun contentPadding(paddingValues: PaddingValues = PaddingValues(all = 12.dp)) = paddingValues
}


@Immutable
private class DefaultChoiceColors(
    private val thumbColor: Color,
    private val trackColor: Color,
    private val contentColor: Color,
) : ChoiceColors {
    @Composable
    override fun thumbColor(): State<Color> = rememberUpdatedState(thumbColor)
    @Composable
    override fun trackColor(): State<Color> = rememberUpdatedState(trackColor)
    @Composable
    override fun contentColor(): State<Color> = rememberUpdatedState(contentColor)
}

@Composable
@ExperimentalMaterialApi
private fun <T : Any> rememberSwipeableStateFor(
    value: T,
    onValueChange: (T) -> Unit,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec
): SwipeableState<T> {
    val swipeableState = remember {
        SwipeableState(
            initialValue = value,
            animationSpec = animationSpec,
            confirmStateChange = { true }
        )
    }
    val forceAnimationCheck = remember { mutableStateOf(false) }
    LaunchedEffect(value, forceAnimationCheck.value) {
        if (value != swipeableState.currentValue) {
            swipeableState.animateTo(value)
        }
    }
    DisposableEffect(swipeableState.currentValue) {
        if (value != swipeableState.currentValue) {
            onValueChange(swipeableState.currentValue)
            forceAnimationCheck.value = !forceAnimationCheck.value
        }
        onDispose { }
    }
    return swipeableState
}

@ExperimentalMaterialApi
@Preview(showBackground = true)
@Composable
fun ChoicePreview() {
    var isLeft by rememberSaveable {
        mutableStateOf(false)
    }

    BLEPlayTheme {
        Column(Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(50.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Choice(
                    choice = if (isLeft) Left.left() else Right.right(),
                    onChoiceChange = {
                        isLeft = it.isLeft()
                    },
                    leftLabel = "Imperial Longish",
                    rightLabel = "Metric",
                )
            }
        }
    }
}
