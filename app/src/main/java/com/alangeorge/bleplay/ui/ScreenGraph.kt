package com.alangeorge.bleplay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme

@ExperimentalComposeUiApi
@Composable
fun ScreenGraph() {
    val pointsCount = 100
    val variance = 5

    var graphPoints by remember { mutableStateOf(generateSomeGraphPoints(pointsCount, variance)) }

    CustomLayout(
        top = {
              Spacer(modifier = Modifier.height(50.dp))
        },
        bottom = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        graphPoints = generateSomeGraphPoints(pointsCount, variance)
                    }
                ) {
                    Text(text = "Generate Graph Data")
                }
            }
        }
    ) {
        Graph(
            modifier = Modifier.padding(16.dp)
            .fillMaxSize(),
            points = graphPoints
        )
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun ScreenGraphPreview() {
    BLEPlayTheme {
        ScreenGraph()
    }
}