package com.alangeorge.bleplay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import kotlinx.coroutines.launch

@Composable
fun ScreenTwo() {
    val snackbarHostState = remember { BleSnackbarHostState() }
    val scope = rememberCoroutineScope()

    val onListScroll: () -> Unit = {
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    CustomLayout(
        top = { TopBar() },
        bottom = { BottomBar() },
        snackbarState = snackbarHostState
    ) {
        ColumnContent(
            onScroll = onListScroll,
            innerPadding = PaddingValues(start = 8.dp, end = 8.dp),
            onClick = { itemNum ->
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val color = (if (itemNum % 2 == 0) Color.Green else Color.Red).copy(alpha = .3f)
                    snackbarHostState.showSnackbar(message = "You clicked item $itemNum", color = color, duration = SnackbarDuration.Long)
                }
            }
        )
    }
}

@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colors.primary.copy(alpha = .5f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Top Slot")
    }
}

@Composable
fun ColumnContent(
    innerPadding: PaddingValues = PaddingValues(),
    onScroll: () -> Unit,
    onClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    var firstVisibleItemRemembered by remember {
       mutableStateOf(listState.firstVisibleItemIndex)
    }

    if (listState.firstVisibleItemIndex != firstVisibleItemRemembered) {
        firstVisibleItemRemembered = listState.firstVisibleItemIndex
        onScroll()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        items(100) { num ->
            Text(modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onClick(num)
                }, text = "Content $num")
        }
   }
}

@Composable
fun BottomBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colors.primary.copy(alpha = .5f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Bottom Slot")
    }
}

@Composable
fun CustomLayout(
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    snackbarState: BleSnackbarHostState = remember { BleSnackbarHostState() },
    content: @Composable () -> Unit
) {
    val snackbar: @Composable (BleSnackbarData) -> Unit = { CenteredColorSnackbar(it) }
    val snackbarHost = @Composable { BleSnackbarHost(hostState = snackbarState, snackbar = snackbar) }

    SubcomposeLayout { constraints ->
        val layoutHeight = constraints.maxHeight

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val topPlaceables = subcompose(slotId = SlotsEnum.Top, content = top).map {
            it.measure(constraints = looseConstraints)
        }

        val bottomPlaceables = subcompose(slotId = SlotsEnum.Bottom, content = bottom).map {
            it.measure(constraints = looseConstraints)
        }

        val snackbarPlaceables = subcompose(SlotsEnum.Snackbar, snackbarHost).map {
            it.measure(looseConstraints)
        }

        val topHeight = topPlaceables.maxOfOrNull { it.height } ?: 0
        val bottomHeight = bottomPlaceables.maxOfOrNull { it.height } ?: 0
        val contentHeight = layoutHeight - topHeight - bottomHeight

        val contentPlaceables = subcompose(slotId = SlotsEnum.Content, content = content).map {
            it.measure(constraints = looseConstraints.copy(maxHeight = contentHeight))
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            topPlaceables.forEach {
                it.place(0, 0)
            }
            bottomPlaceables.forEach {
                it.place(0, layoutHeight - bottomHeight)
            }
            contentPlaceables.forEach {
                it.place(0, topHeight)
            }
            snackbarPlaceables.forEach {
                it.place(0, topHeight + 12.dp.toPx().toInt())
            }
        }
    }
}

enum class SlotsEnum {Top, Bottom, Content, Snackbar}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_4_XL, group = "ScreenTwo")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, group = "ScreenTwo")
@Composable
fun ScreenTwoPreview() {
    BLEPlayTheme {
        ScreenTwo()
    }
}