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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.alangeorge.bleplay.ui.theme.BLEPlayTheme
import com.alangeorge.bleplay.viewmodel.*
import timber.log.Timber
import java.util.*
import kotlin.random.Random

@ExperimentalComposeUiApi
@Composable
fun ScreenBleDeviceDetail(
    scanData: ScanData,
    gattStatusAndState: GattStatusAndState?,
    discoveredServices: List<BluetoothGattService>,
    mtu: Int?,
    heartRate: Int?,
    batteryLevel: Int?,
    temperature: String?,
    temperatureHistoric: List<Float>?,
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
            gattStatusAndState?.let {
                Text(text = "Gatt Status: ${it.status}")
                Text(text = "Gatt State: ${it.state}")
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
        }
        BondButton(scanData = scanData, bondOnClick = bondOnClick, onBondStateChange = onBondStateChange)
        Text(text = "service ids: ${scanData.serviceIds}")
        ConnectButton(isConnected = isConnected, isConnecting = isConnecting, onClick = connectOnClick)
        heartRate?.let { Text(text = "Heart Rate: $it") }
        temperature?.let { Text(text = "CPU temperature: $temperature")}
        temperatureHistoric?.let { rawList ->
            if (rawList.size > 2) {
                val max = rawList.maxByOrNull { it } ?: 100f
                val min = rawList.minByOrNull { it } ?: 0f
                val diff = max - min
                val points = rawList.map { (it - min) / diff }
                Graph(modifier = Modifier.fillMaxWidth().height(150.dp), points = points, avgLine = false)
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(discoveredServices) { service ->
                BleServiceItem(bleService = service)
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

@Preview(showBackground = true)
@Composable
fun BleServiceItemPreview(
    @PreviewParameter(ScreenBleDeviceDetailDataProvider::class) data: DeviceDetail
) {
    BLEPlayTheme {
        if (data.discoveredServices.isNotEmpty()) {
            BleServiceItem(bleService = data.discoveredServices.first())
        } else {
            Text(text = "no discovered services")
        }
    }
}

@ExperimentalComposeUiApi
@Preview(showBackground = true, group = "ScreenBleDeviceDetail")
@Composable
fun ScreenBleDeviceDetailPreview(
    @PreviewParameter(ScreenBleDeviceDetailDataProvider::class) data: DeviceDetail
) {
    with (data) {
        BLEPlayTheme {
            ScreenBleDeviceDetail(
                scanData = scanData,
                gattStatusAndState = gattStatusAndState,
                mtu = mtu,
                discoveredServices = discoveredServices,
                batteryLevel = batteryLevel,
                heartRate = heartRate,
                temperature = temperature,
                temperatureHistoric = temperatureHistoric,
                bondOnClick = {},
                connectOnClick = {},
                isConnected = isConnected,
                isConnecting = isConnecting,
                onBondStateChange = {}
            )
        }
    }
}

data class DeviceDetail(
    val scanData: ScanData,
    val discoveredServices: List<BluetoothGattService>,
    val mtu: Int? = null,
    val heartRate: Int? = null,
    val temperature: String? = null,
    val temperatureHistoric: List<Float>? = null,
    val batteryLevel: Int? = null,
    val gattStatusAndState: GattStatusAndState? = null,
    val isConnecting: Boolean,
    val isConnected: Boolean
)

class ScreenBleDeviceDetailDataProvider : PreviewParameterProvider<DeviceDetail> {
    object ScanDatas {
        val connectable = ScanData(
            rssi = -76,
            isConnectable = true,
            serviceIds = listOf("service Id1", "service id2"),
            deviceData = DeviceData(
                address = "C4:8E:8F:6C:43:9A",
                name = "Some BLE Device Name",
                bondState = BleBondState.NONE
            )
        )
        val notConnectable = ScanData(
            rssi = -89,
            isConnectable = true,
            serviceIds = listOf("service Id1", "service id2"),
            deviceData = DeviceData(
                address = "C4:8E:8F:6C:43:9A",
                name = "Some BLE Device Name",
                bondState = BleBondState.BONDED
            )
        )
    }

    object DiscoveredServices {
        val default = listOf(
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
            },
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
        )
        val none = emptyList<BluetoothGattService>()
    }

    override val values =  sequenceOf(
        DeviceDetail(
            scanData = ScanDatas.connectable,
            discoveredServices = DiscoveredServices.none,
            isConnecting = false,
            isConnected = false
        ),
        DeviceDetail(
            scanData = ScanDatas.notConnectable,
            discoveredServices = DiscoveredServices.none,
            isConnecting = true,
            isConnected = false,
        ),
        DeviceDetail(
            scanData = ScanDatas.connectable,
            gattStatusAndState = GattStatusAndState(
                BluetoothGatt.GATT_SUCCESS.gattStatusDescription,
                BluetoothProfile.STATE_CONNECTED.gattStateDescription
            ),
            discoveredServices = DiscoveredServices.default,
            isConnecting = false,
            isConnected = true,
            mtu = 134,
            heartRate = 150,
            batteryLevel = 45,
            temperature = "103.2 F",
            temperatureHistoric = generateSomeGraphPoints(50, 98.2f, 105.4f, .3f)
        )
    )

    fun generateSomeGraphPoints(number: Int = 50, min: Float, max: Float, variance: Float) =
        (1..number).runningFold(Random.nextDouble(min.toDouble(), max.toDouble())) { previous, _ ->
            val limitLow = (previous - variance).coerceIn(min.toDouble(), max.toDouble())
            val limitHigh = (previous + variance).coerceIn(min.toDouble(), max.toDouble())

            Random.nextDouble(limitLow, limitHigh)
        }.map(Double::toFloat)
}