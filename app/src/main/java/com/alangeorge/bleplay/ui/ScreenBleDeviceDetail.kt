package com.alangeorge.bleplay.ui

import android.app.Activity
import android.bluetooth.*
import android.content.*
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import com.alangeorge.bleplay.viewmodel.BleBondState
import com.alangeorge.bleplay.viewmodel.DeviceData
import com.alangeorge.bleplay.viewmodel.ScanData
import com.alangeorge.bleplay.viewmodel.printProperties
import timber.log.Timber
import java.util.*

@Composable
fun ScreenBleDeviceDetail(
    scanData: ScanData,
    discoveredServices: List<BluetoothGattService>,
    mtu: Int?,
    heartRate: Int?,
    batteryLevel: Int?,
    bondOnClick: () -> Unit,
    connectOnClick: () -> Unit,
    isConnecting: Boolean,
    isConnected: Boolean,
    onBondStateChange: (Intent?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        with(scanData) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "address : ${deviceData.address}")
                batteryLevel?.let {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "battery: $it%")
                }
            }
            if (deviceData.name != null) Text(text = "name : ${deviceData.name}")
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "signal strength : $rssi")
                mtu?.let {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "MTU: $it")
                }
            }
            Text(text = "isConnectable : $isConnectable")
            Text(text = "bond state : ${deviceData.bondState}")
            BondButton(scanData = scanData, bondOnClick = bondOnClick, onBondStateChange = onBondStateChange)
            Text(text = "service ids: ${scanData.serviceIds}")
            ConnectButton(isConnected = isConnected, isConnecting = isConnecting, onClick = connectOnClick)
            heartRate?.let { Text(text = "Heart Rete: $it") }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(discoveredServices) { service ->
                    BleServiceItem(bleService = service)
                }
            }
        }
    }
}

const val DEVICE_ADDRESS_ARG_NAME = "address"
const val DEVICE_ROUTE_BASE = "device"
const val DEVICE_ROUTE = "$DEVICE_ROUTE_BASE/{$DEVICE_ADDRESS_ARG_NAME}"

@Composable
fun BondButton(
    scanData: ScanData,
    bondOnClick: () -> Unit,
    onBondStateChange: (Intent?) -> Unit
) {
    var registerBondingReceiver by remember {
        mutableStateOf(false)
    }

    if (scanData.deviceData.bondState == BleBondState.NONE) {
        if (registerBondingReceiver) {
            BleBondBroadcastReceiver(onEvent = {
                onBondStateChange(it)
                registerBondingReceiver = false
            })
        }
        AnimatedVisibility(
            visible = registerBondingReceiver.not(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Button(
                onClick = {
                    bondOnClick()
                    registerBondingReceiver = true
                }
            ) {
                Text(text = "Start Bond")
            }
        }
    }
}

@Composable
fun ConnectButton(isConnected: Boolean, isConnecting: Boolean, onClick: () -> Unit) {
    if (isConnected) {
        KeepScreenOn()
    }
    Button(onClick = onClick, enabled = isConnecting.not()) {
        Crossfade(targetState = isConnected to isConnecting) {
            when(it) {
                true to false -> Text(text = "Disconnect Gatt")
                false to false -> Text(text = "Connect Gatt")
                else -> Text(text = "Connecting")
            }
        }
    }
}

@Composable
fun BleServiceItem(bleService: BluetoothGattService) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Service: ", fontWeight = FontWeight.Bold)
            Text(text = bleService.uuid.toString(), style = MaterialTheme.typography.caption)
        }

        bleService.characteristics.forEach { char ->
            Text(text = "${char.uuid} ${char.printProperties()}", style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
fun BleBondBroadcastReceiver(onEvent: (intent: Intent?) -> Unit) {
    val context = LocalContext.current
    val currentOnEvent by rememberUpdatedState(newValue = onEvent)

    DisposableEffect(key1 = onEvent) {
        val intentFilter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        val broadcast = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                currentOnEvent(intent)
            }
        }

        Timber.d("registering bonding receiver")
        context.registerReceiver(broadcast, intentFilter)

        onDispose {
            Timber.d("unregistering bonding receiver")
            context.unregisterReceiver(broadcast)
        }
    }
}

@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private val previewService
    get() =
        BluetoothGattService(UUID.randomUUID(), 1).apply {
            repeat(5) {
                addCharacteristic(
                    BluetoothGattCharacteristic(
                        UUID.randomUUID(),
                        BluetoothGattCharacteristic.PROPERTY_READ,
                        BluetoothGattDescriptor.PERMISSION_READ
                    )
                )
            }
        }

@Preview(showBackground = true)
@Composable
fun BleServiceItemPreview() {
    BLEPlayTheme {
        BleServiceItem(bleService = previewService)
    }
}

@Preview(showBackground = true)
@Composable
fun ScreenBleDeviceDetailPreview() {
    BLEPlayTheme {
        ScreenBleDeviceDetail(
            scanData = ScanData(
                rssi = -76,
                isConnectable = true,
                serviceIds = listOf("service Id1", "service id2"),
                deviceData = DeviceData(
                    address = "C4:8E:8F:6C:43:9A",
                    name = "Some BLE Device Name",
                    bondState = BleBondState.NONE
                )
            ),
            mtu = 517,
            discoveredServices = listOf(previewService, previewService),
            batteryLevel = 50,
            heartRate = 100,
            bondOnClick = {},
            connectOnClick = {},
            isConnected = true,
            isConnecting = false,
            onBondStateChange = {}
        )
    }
}