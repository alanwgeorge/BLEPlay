package com.alangeorge.bleplay.ui

import android.Manifest
import android.bluetooth.le.ScanResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alangeorge.bleplay.common.plus
import com.alangeorge.bleplay.common.then
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionsRequired
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@ExperimentalPermissionsApi
@Composable
fun ScreenBleScan(
    startScan: () -> Unit,
    stopScan: () -> Unit,
    status: () -> Unit,
    deviceOnClick: (String) -> Unit,
    scanResults: List<ScanResult>
) {
    val (isScanning, setIsScanning) = rememberSaveable { mutableStateOf(false) }
    val toggleScan = { setIsScanning(isScanning.not()) }

    val (doNotShowRationale, setDoNotShowRationale) = rememberSaveable { mutableStateOf(false)  }
    val bleScanPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    PermissionsRequired(
        multiplePermissionsState = bleScanPermissionsState,
        permissionsNotGrantedContent = {
            if (doNotShowRationale) {
                Text("Feature not available")
            } else {
                Column {
                    Text("Bluetooth scanning is important for this app. Please grant the permissions.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = { bleScanPermissionsState.launchMultiplePermissionRequest() }) {
                            Text("Ok!")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { setDoNotShowRationale(true) }) {
                            Text("Nope")
                        }
                    }
                }
            }
        },
        permissionsNotAvailableContent = {
            Column {
                Text(
                    "Bluetooth permissions denied. See this FAQ with information about why we " +
                            "need this permission. Please, grant us access on the Settings screen."
                )
//                Spacer(modifier = Modifier.height(8.dp))
//                Button(onClick = navigateToSettingsScreen) {
//                    Text("Open Settings")
//                }
            }

        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
            Button(
                onClick = startScan then toggleScan,
                enabled = isScanning.not()
            ) {
                Text(text = "Start BLE Scan")
            }
            Button(
                onClick = stopScan + toggleScan,
                enabled = isScanning
            ) {
                Text(text = "Stop BLE Scan")
            }
            Button(
                onClick = status
            ) {
                Text(text = "BLE adapter status")
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(scanResults) { result ->
                    ScanResultRow(result = result, deviceOnClick = deviceOnClick)
                }
            }
        }
    }
}

@Composable
fun ScanResultRow(result: ScanResult, deviceOnClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp)
                .clickable {
                    deviceOnClick(result.device.address)
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = result.device.address, fontWeight = FontWeight.Bold)
            Text(text = result.scanRecord?.deviceName ?: "N/A")
            Text(text = "rssi:${result.rssi}")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Divider(modifier = Modifier.height(1.dp))
    }
}

const val DEVICE_ROUTE_SCAN = "$DEVICE_ROUTE_BASE/Scan"