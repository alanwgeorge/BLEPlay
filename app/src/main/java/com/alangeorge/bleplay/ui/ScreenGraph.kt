package com.alangeorge.bleplay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme

@Composable
fun ScreenGraph() {
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

@Preview(showBackground = true)
@Composable
fun ScreenGraphPreview() {
    BLEPlayTheme {
        ScreenGraph()
    }
}