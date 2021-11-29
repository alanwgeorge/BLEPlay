package com.alangeorge.bleplay.ui

import android.Manifest
import android.bluetooth.le.ScanResult
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alangeorge.bleplay.common.plus
import com.alangeorge.bleplay.common.then
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionsRequired
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@ExperimentalPermissionsApi
@Composable
fun ScreenBleScanPermissionsWrapper(
    startScan: () -> Unit,
    stopScan: () -> Unit,
    isScanning: Boolean,
    status: () -> Unit,
    clearResults: () -> Unit,
    deviceOnClick: (String) -> Unit,
    setFilter: (Int) -> Unit,
    filters: List<String>,
    selectedFilter: Int,
    scanResults: List<ScanResult>
) {
    val (doNotShowRationale, setDoNotShowRationale) = rememberSaveable { mutableStateOf(false)  }
    val bleScanPermissionsState =
        rememberMultiplePermissionsState(
                permissions =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    )
                } else {
                    listOf(Manifest.permission.ACCESS_FINE_LOCATION)
                }
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
        ScreenBleScan(
            startScan = startScan,
            stopScan = stopScan,
            isScanning = isScanning,
            clearResults = clearResults,
            deviceOnClick = deviceOnClick,
            setFilter =setFilter,
            filters = filters,
            selectedFilter = selectedFilter,
            scanResults = scanResults.map { it.uiData }
        )
    }
}

@Composable
fun ScreenBleScan(
    startScan: () -> Unit,
    stopScan: () -> Unit,
    isScanning: Boolean,
    clearResults: () -> Unit,
    deviceOnClick: (String) -> Unit,
    setFilter: (Int) -> Unit,
    filters: List<String>,
    selectedFilter: Int,
    scanResults: List<ScanResultUiData>
) {
    var filterListExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        )
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                    onClick = startScan,
                    enabled = isScanning.not()
            ) { Text(text = "Start Scan") }
            Button(
                    onClick = stopScan,
                    enabled = isScanning
            ) { Text(text = "Stop Scan") }
            Button(
                    onClick = clearResults
            ) { Text(text = "Clear Results") }
        }
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = filters[selectedFilter], modifier = Modifier.padding(start = 16.dp))
                IconButton(
                    onClick = { filterListExpanded = true },
                    enabled = isScanning.not()
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "TODO")
                }
            }
            DropdownMenu(
                expanded = filterListExpanded,
                onDismissRequest = { filterListExpanded = false }) {
                filters.forEachIndexed() { index, label ->
                    DropdownMenuItem(onClick = {
                        setFilter(index)
                        filterListExpanded = false
                    }) {
                        Text(text = label)
                    }
                }
            }
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

data class ScanResultUiData(val address: String, val name: String, val rssi: Int?)

val ScanResult.uiData
        get() = ScanResultUiData(address = device.address, name = device.name ?: "N/A", rssi = rssi)

@Composable
fun ScanResultRow(result: ScanResultUiData, deviceOnClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp)
                .clickable {
                    deviceOnClick(result.address)
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = result.address, fontWeight = FontWeight.Bold)
            Text(text = result.name)
            Text(text = "rssi:${result.rssi}")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Divider(modifier = Modifier.height(1.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun ScanResultRowPreview() {
    val result = ScanResultUiData(address = "CF:19:E3:97:E2:9C", name = "TICKR 3D5C", rssi = -45)
    BLEPlayTheme {
        ScanResultRow(result = result, deviceOnClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ScreenBleScanPreview() {
    BLEPlayTheme {
        ScreenBleScan(
            startScan = { },
            stopScan = { },
            isScanning = false,
            clearResults = { },
            deviceOnClick = { },
            setFilter = { },
            filters = listOf("No Filter", "Heart Rate Service"),
            selectedFilter = 1,
            scanResults = List(6) {
                ScanResultUiData(address = "CF:19:E3:97:E2:9C", name = "TICKR 3D5C", rssi = -45)
            }
        )
    }
}

const val DEVICE_ROUTE_SCAN = "$DEVICE_ROUTE_BASE/Scan"