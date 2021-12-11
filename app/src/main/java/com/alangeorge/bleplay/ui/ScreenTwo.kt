package com.alangeorge.bleplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ScreenTwo() {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color.LightGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(100) {
            Text(
                text = "item $it",
                modifier = Modifier.padding(2.dp)
            )
            Divider(color = Color.Black, thickness = 1.dp)
        }
    }
}

@Preview
@Composable
fun ScreenTwoPreview() {
    ScreenTwo()
}

@Composable
fun CustomLayoutPlay() {
    CustomLayout(
        top = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(text = "Top Bar")
            }
        },
        bottom = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(text = "Bottom Bar")
            }
        }
    ) {
        Box(modifier = Modifier
            .background(Color.LightGray)
            .fillMaxWidth()) {
            Text(text = "Context")
        }
    }
}

@Composable
fun CustomLayout(
    modifier: Modifier = Modifier,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val contentPlaceables = subcompose(slotId = SlotsEnum.Content, content = content).map {
            it.measure(constraints = constraints)
        }


        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceables.forEach {
                it.placeRelative(0, 0)
            }
        }
    }
}

@Preview
@Composable
fun CustomLayoutPlayPreview() {
    CustomLayoutPlay()
}

enum class SlotsEnum {Top, Bottom, Content}