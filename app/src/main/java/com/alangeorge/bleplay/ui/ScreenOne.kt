package com.alangeorge.bleplay.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.alangeorge.bleplay.R
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import com.alangeorge.bleplay.viewmodel.MainViewModel

@Composable
fun ScreenOne(
    viewModel: MainViewModel = hiltViewModel()
) {
    val _name by viewModel.title.observeAsState()
    val _number by viewModel.data.observeAsState()

    Column {
        _name?.let { name ->
            _number?.let { number ->
                Greeting(_name ?: "default", _number ?: -1)
            }  ?: Text("Waiting for data")
        } ?: Text("Waiting for data")

        Button(
            onClick = {
                viewModel.displaySnackbar(R.string.test_snakebar)
            }
        ) {
            Text(text = "test button")
        }
    }
}

@Composable
fun Greeting(name: String, number: Int) {
    Column {
        Text(text = "Hello $name!")
        Text(text = "Your new number is $number")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BLEPlayTheme {
        Greeting("Android", 4)
    }
}