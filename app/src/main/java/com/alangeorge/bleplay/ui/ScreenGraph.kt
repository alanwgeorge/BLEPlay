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
fun ScreenGraph2() {
    var graphPoints by remember { mutableStateOf(generateSomeGraphPoints()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Graph(modifier = Modifier
            .fillMaxWidth()
            .height(250.dp), points = graphPoints)
        Button(
            onClick = {
                graphPoints = generateSomeGraphPoints()
            }
        ) {
            Text(text = "Generate Graph Data")
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun ScreenGraph() {
    var graphPoints by remember { mutableStateOf(generateSomeGraphPoints()) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                        graphPoints = generateSomeGraphPoints()
                    }
                ) {
                    Text(text = "Generate Graph Data")
                }
            }
        },
        snackbarState = snackbarHostState
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