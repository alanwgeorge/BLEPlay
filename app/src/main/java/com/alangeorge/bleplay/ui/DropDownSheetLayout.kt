package com.alangeorge.bleplay.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random


@Composable
fun DropDownSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    sheetState: DropDownSheetState = rememberDropDownSheetState(),
    sheetShape: Shape = MaterialTheme.shapes.large,
    sheetElevation: Dp = DropDownSheetDefaults.Elevation,
    sheetBackgroundColor: Color = MaterialTheme.colors.surface,
    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
    scrimColor: Color = DropDownSheetDefaults.scrimColor,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    val scrim = @Composable {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            Scrim(
                color = scrimColor,
                onDismiss = {
                    if (sheetState.isVisible) {
                        scope.launch { sheetState.hide() }
                    }
                },
                visible = sheetState.targetValue != DropDownSheetValue.Hidden
            )
        }
    }

    val dropDown = @Composable {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(0, (sheetState.offsetY.value * density).roundToInt())
                },
            shape = sheetShape,
            elevation = sheetElevation,
            color = sheetBackgroundColor,
            contentColor = sheetContentColor
        ) {
            Column(content = sheetContent)
        }
    }

    SubcomposeLayout { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val scrimPlaceables = subcompose(DropDownSheetSlots.Scrim, content = scrim).map {
            it.measure(looseConstraints)
        }

        val dropDownPlaceables = subcompose(DropDownSheetSlots.DropDown, content = dropDown).map {
            it.measure(looseConstraints)
        }

        sheetState.dropDownHeight = dropDownPlaceables.maxOfOrNull { it.height }?.toFloat() ?: 0f

        layout(constraints.maxWidth, constraints.maxHeight) {
            scrimPlaceables.forEach {
                it.place(0, 0, -2f)
            }
            dropDownPlaceables.forEach {
                it.place(0, 0, -1f)
            }
        }
    }
}

enum class DropDownSheetSlots { Scrim, DropDown }

@Suppress("unused")
class DropDownSheetState(
    initialValue: DropDownSheetValue = DropDownSheetValue.Hidden
) {
    var dropDownHeight by mutableStateOf(200f)

    var currentValue by mutableStateOf( initialValue )
        private set

    var targetValue by mutableStateOf( initialValue )
        private set

    val isAnimationRunning
        get() = currentValue != targetValue

    val offsetY = Animatable(if (initialValue == DropDownSheetValue.Hidden) -dropDownHeight else 0f)

    val isVisible: Boolean
        get() = currentValue != DropDownSheetValue.Hidden

    suspend fun show() {
        targetValue = DropDownSheetValue.Expanded
        offsetY.animateTo(0f)
        currentValue = DropDownSheetValue.Expanded
    }
    suspend fun hide() {
        targetValue = DropDownSheetValue.Hidden
        offsetY.animateTo(-dropDownHeight)
        currentValue = DropDownSheetValue.Hidden
    }
}

enum class DropDownSheetValue {
    Hidden,
    Expanded
}

@Composable
fun rememberDropDownSheetState(initialValue: DropDownSheetValue = DropDownSheetValue.Hidden): DropDownSheetState {
    return remember {
        DropDownSheetState(initialValue = initialValue)
    }
}

@Composable
private fun Scrim(
    color: Color,
    onDismiss: () -> Unit,
    visible: Boolean
) {
    if (color.isSpecified) {
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = TweenSpec()
        )
        val dismissModifier = if (visible) {
            Modifier
                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
                .semantics(mergeDescendants = true) {
                    contentDescription = "dismiss"
                    onClick { onDismiss(); true }
                }
        } else {
            Modifier
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .then(dismissModifier)
        ) {
            drawRect(color = color, alpha = alpha)
        }
    }
}

object DropDownSheetDefaults {
    val Elevation = 0.dp
    val scrimColor: Color
        @Composable
        get() = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
}


@Preview(showBackground = true)
@Composable
fun DropDownSheetLayoutPreview() {
    var dropDownRows by remember {
        mutableStateOf(Random.nextInt(2, 20))
    }

    BLEPlayTheme {
        val scope = rememberCoroutineScope()
        val dropDownSheetState = rememberDropDownSheetState()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colors.background)
                    .zIndex(2f),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        with(dropDownSheetState) {
                            scope.launch {
                                if (isVisible) {
                                    hide()
                                    dropDownRows = Random.nextInt(2, 20)
                                } else show()
                            }
                        }
                    }
                ) {
                    Text(text = "Animate")
                }
            }
            DropDownSheetLayout(
                sheetContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(dropDownRows) {
                            Text(text = "test $it")
                        }
                    }
                },
                sheetState = dropDownSheetState
            ) {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp)
                ) {
                    items(100) { num ->
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Content $num"
                        )
                    }
                }
            }
        }
    }
}