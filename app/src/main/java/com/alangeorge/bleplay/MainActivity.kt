package com.alangeorge.bleplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alangeorge.bleplay.ui.MainViewModel
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(viewModels<MainViewModel>().value)

        setContent {
            BLEPlayTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val viewModel: MainViewModel = viewModel()
                    val name by viewModel.title.observeAsState()
                    val number by viewModel.data.observeAsState()

                    Greeting(name ?: "default", number ?: -1)
                }
            }
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