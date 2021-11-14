package com.alangeorge.bleplay.ui

import android.bluetooth.le.ScanResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ScreenBleDeviceDetail(scanResult: ScanResult) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = scanResult.toString())
    }
}

const val DEVICE_ADDRESS_ARG_NAME = "address"
const val DEVICE_ROUTE_BASE = "device"