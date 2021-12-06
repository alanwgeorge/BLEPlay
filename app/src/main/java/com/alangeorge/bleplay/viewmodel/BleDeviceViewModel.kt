package com.alangeorge.bleplay.viewmodel

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.os.PowerManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.right
import com.alangeorge.bleplay.common.Pipeline
import com.alangeorge.bleplay.common.toHexString
import com.alangeorge.bleplay.model.SnackbarMessage
import com.alangeorge.bleplay.repository.BleRepository
import com.alangeorge.bleplay.ui.DEVICE_ADDRESS_ARG_NAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BleDeviceViewModel @Inject constructor(
        repository: BleRepository,
        val snackbarMessages: Pipeline<SnackbarMessage>,
        savedStateHandle: SavedStateHandle,
        val application: Application
) : ViewModel() {
    val scanResult = repository.scanResults[savedStateHandle.get(DEVICE_ADDRESS_ARG_NAME)]

    private val connectedGatts = mutableMapOf<String, BluetoothGatt>()
    private val gattResultFlow = MutableSharedFlow<GattCallbackResult>()
    val discoveredServiceFlow: Flow<List<BluetoothGattService>> = gattResultFlow
        .filterIsInstance<GattCallbackResult.ServicesDiscovered>()
        .map {
            it.gatt.services
        }
    val mtuChangedFlow: Flow<Int> = gattResultFlow
        .filterIsInstance<GattCallbackResult.MtuChanged>()
        .map {
            it.mtu
        }

    private val _connectedStatusFlow = MutableStateFlow(false)
    val connectedStatusFlow = _connectedStatusFlow.asStateFlow()
    private val _isConnectingStatusFlow = MutableStateFlow(false)
    val isConnectingStatusFlow = _isConnectingStatusFlow.asStateFlow()

    private val _scanResultsFlow = MutableStateFlow(scanResult?.asScanData)
    val scanResultsFlow = _scanResultsFlow.asStateFlow()

    private val _batteryLevelFlow = MutableStateFlow(-1)
    val batteryLevelFlow = _batteryLevelFlow.asStateFlow()

    private val _heartRateFlow = MutableStateFlow(-1)
    val heartRateFlow = _heartRateFlow.asStateFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            viewModelScope.launch {
                gattResultFlow.emit(
                    GattCallbackResult.ConnectionStateChanged(
                        address = gatt.device.address,
                        gatt = gatt,
                        status = status,
                        newState = newState
                    )
                )
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            viewModelScope.launch {
                gattResultFlow.emit(
                    GattCallbackResult.ServicesDiscovered(
                        gatt = gatt,
                        status = status
                    )
                )
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            viewModelScope.launch {
                gattResultFlow.emit(
                    GattCallbackResult.MtuChanged(
                        gatt = gatt,
                        mtu = mtu,
                        status = status
                    )
                )
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Timber.d("onCharacteristicRead: $status ${characteristic.uuid}")
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    when(characteristic.uuid) {
                        characteristicUuid[CHARACTERISTIC_NAME_BATTERY_LEVEL] -> handleBatteryLevel(characteristic)
                        else -> Timber.d("unhandled characteristic: ${characteristic.uuid} ${characteristic.value?.toHexString()}")
                    }
                }
                BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                    viewModelScope.launch { snackbarMessages.produceEvent(SnackbarMessage("read not permitted for characteristic: ${characteristic.uuid}".right())) }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            when (characteristic.uuid) {
                characteristicUuid[CHARACTERISTIC_NAME_HEART_RATE] -> handleHeartReteChange(characteristic)
                characteristicUuid[CHARACTERISTIC_NAME_BATTERY_LEVEL] -> handleBatteryLevel(characteristic)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            viewModelScope.launch {
                gattResultFlow.emit(
                    GattCallbackResult.DescriptorWrite(
                        gatt = gatt,
                        descriptor = descriptor,
                        status = status
                    )
                )
            }
        }
    }

    private fun handleBatteryLevel(characteristic: BluetoothGattCharacteristic) {
        characteristic
            .value
            ?.getOrNull(0)
            ?.toUByte()
            ?.toInt()
            ?.let {
                Timber.d("battery level: $it")
                viewModelScope.launch { _batteryLevelFlow.emit(it) }
            }

    }

    private fun handleHeartReteChange(characteristic: BluetoothGattCharacteristic) {
         characteristic.value?.getOrNull(0)?.let { flags -> // first byte are the flags
            val bitSet = BitSet.valueOf(byteArrayOf(flags))

             if (bitSet.length() > 0 && !bitSet[0]) { // first flag bit == 0 means unit8 value
                 characteristic
                     .value
                     ?.getOrNull(1) // unit8 heart rate value
                     ?.toUByte()
                     ?.toInt()
                     ?.let {
                         Timber.d("heart rate: $it")
                         viewModelScope.launch { _heartRateFlow.emit(it) }
                     }
             }
        }
    }

    private val connectionStateChanged: suspend (GattCallbackResult.ConnectionStateChanged) -> Unit = {
        val (deviceAddress, gatt, status, newState) = it

        _scanResultsFlow.emit(_scanResultsFlow.value?.copy(deviceData = gatt.device.asDeviceData))
        _isConnectingStatusFlow.emit(false)

        if (status == BluetoothGatt.GATT_SUCCESS) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.w("onConnectionStateChange: connected to $deviceAddress")
                    _connectedStatusFlow.emit(true)
                    connectedGatts[gatt.device.address] = gatt
                    gatt.discoverServices()

                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    gatt.close()
                    connectedGatts.remove(gatt.device.address)
                    _connectedStatusFlow.emit(false)
                    snackbarMessages.produceEvent(SnackbarMessage("device disconnected: ${gatt.device.address}".right()))
                }
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Timber.w("onConnectionStateChange: connected to $deviceAddress")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Timber.e("onConnectionStateChange: disconnected from $deviceAddress")
                gatt.close()
                snackbarMessages.produceEvent(SnackbarMessage("device disconnected: ${gatt.device.address}".right()))
            }
        } else {
            Timber.e("onConnectionStateChange: status $status encountered for $deviceAddress!")
            gatt.close()
            snackbarMessages.produceEvent(SnackbarMessage("device status changed: status:$status device:${gatt.device.address}".right()))
        }
    }

    private val servicesDiscovered: suspend (GattCallbackResult.ServicesDiscovered) -> Unit = {
        val (gatt, status) = it

        _scanResultsFlow.emit(_scanResultsFlow.value?.copy(deviceData = gatt.device.asDeviceData))

        with(gatt) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.w("Discovered ${services.size} services for ${device.address}.")
                printGattTable()
                gatt.requestMtu(GATT_MAX_MTU_SIZE)
            } else {
                Timber.e("Service discovery failed due to status $status")
                close()
                snackbarMessages.produceEvent(SnackbarMessage("device status changed: status:$status device:${gatt.device.address}".right()))
            }
        }
    }

    private fun readCharacteristic(gatt: BluetoothGatt, serviceName: String, characteristicName: String) {
        gatt
            .getService(serviceUuids[serviceName])
            ?.getCharacteristic(characteristicUuid[characteristicName])
            ?.let {
                if (it.isReadable()) {
                    gatt.readCharacteristic(it)
                }
            }
    }

    private fun startNotifications(gatt: BluetoothGatt, serviceName: String, characteristicName: String) {
        gatt
            .getService(serviceUuids[serviceName])
            ?.getCharacteristic(characteristicUuid[characteristicName])
            ?.let { heartRateChar ->
                if (heartRateChar.isNotifiable()) {
                    heartRateChar.getDescriptor(CCC_DESCRIPTOR_UUID)?.let { cccDescriptor ->
                        if (gatt.setCharacteristicNotification(heartRateChar, true)) {
                            cccDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            if (!gatt.writeDescriptor(cccDescriptor)) {
                                Timber.d("writeDescriptor failed: $serviceName $characteristicName")
                            }
                        } else {
                            Timber.d("startNotifications() $serviceName $characteristicName failed")
                        }
                    }
                } else {
                    Timber.d("$serviceName $characteristicName is not notifiable")
                }
            } ?: Timber.d("failed to look up service or characteristic UUID $serviceName $characteristicName")
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            gattResultFlow.filterIsInstance<GattCallbackResult.ConnectionStateChanged>()
                .collect(connectionStateChanged)
        }
        viewModelScope.launch(Dispatchers.IO) {
            gattResultFlow.filterIsInstance<GattCallbackResult.ServicesDiscovered>()
                .collect(servicesDiscovered)
        }
        viewModelScope.launch {
            gattResultFlow
                .filterIsInstance<GattCallbackResult.MtuChanged>()
                .first()
                .run {
                    // gatt reference before mtu change seem to no longer work after mtu update
                    connectedGatts[gatt.device.address] = gatt
                    startNotifications(gatt, SERVICE_NAME_BATTERY_LEVEL, CHARACTERISTIC_NAME_BATTERY_LEVEL)
                }
        }
        viewModelScope.launch {
            gattResultFlow
                .filterIsInstance<GattCallbackResult.DescriptorWrite>()
                .first()
                .run {
                    connectedGatts[gatt.device.address] = gatt
                    startNotifications(gatt, SERVICE_NAME_HEART_RATE,CHARACTERISTIC_NAME_HEART_RATE)
                }
        }
    }

    fun connectGatt() {
        viewModelScope.launch { _isConnectingStatusFlow.emit(true) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scanResult?.device?.connectGatt(application, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            scanResult?.device?.connectGatt(application, false, gattCallback)
        }
    }

    fun disconnectGatts() {
        viewModelScope.launch { _isConnectingStatusFlow.emit(false) }
        connectedGatts.values.forEach {
            it.close()
        }

        viewModelScope.launch {
            _connectedStatusFlow.emit(value = false)
        }
        connectedGatts.clear()
    }

    fun bond() {
        scanResult?.device?.createBond()
    }

    fun handleBleBondIntent(intent: Intent?) {
        Timber.d("ble bond state changed: $intent")

        intent?.extras?.let {
            val newBondState = it.getInt(BluetoothDevice.EXTRA_BOND_STATE).bondState
            val oldBondState = it.getInt(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE).bondState
            val device = it.getParcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            Timber.d("newBondState:$newBondState oldBondState:$oldBondState device:$device")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared()")
        disconnectGatts()
    }
}

data class ScanData(
        val rssi: Int,
        val isConnectable: Boolean?,
        val serviceIds: List<String>,
        val deviceData: DeviceData
)

data class DeviceData(
    val address: String,
    val name: String?,
    val bondState: BleBondState,
)

enum class BleBondState {
    NONE,
    BONDING,
    BONDED,
    UNKNOWN;
}

private val BluetoothGatt.GATT_MAX_MTU_SIZE
    get() = 517

val Int.bondState
    get() = when(this) {
        10 -> BleBondState.NONE
        11 -> BleBondState.BONDING
        12 -> BleBondState.BONDED
        else -> BleBondState.UNKNOWN
    }

val ScanResult.asScanData
    get() = ScanData(
        rssi = rssi,
        isConnectable = null,
        serviceIds = scanRecord?.serviceUuids?.map(ParcelUuid::toString) ?: emptyList(),
        deviceData = device.asDeviceData
    ).let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.copy(isConnectable = isConnectable)
            } else {
                it
            }
        }

val BluetoothDevice.asDeviceData
    get() = DeviceData(
        address = address,
        name = name,
        bondState = bondState.bondState
    )

sealed class GattCallbackResult {
    data class ConnectionStateChanged(
        val address: String,
        val gatt: BluetoothGatt,
        val status: Int,
        val newState: Int
    ) : GattCallbackResult()

    data class ServicesDiscovered(
        val gatt: BluetoothGatt, val status: Int
    ) : GattCallbackResult()

    data class MtuChanged(
        val gatt: BluetoothGatt, val mtu: Int, val status: Int
    ) : GattCallbackResult()

    data class DescriptorWrite(
        val gatt: BluetoothGatt,
        val descriptor: BluetoothGattDescriptor,
        val status: Int
    ) : GattCallbackResult()
}

fun BluetoothGatt.printGattTable() {
    if (services.isEmpty()) {
        Timber.i("No service and characteristic available, call discoverServices() first?")
        return
    }
    services.forEach { service ->
        val characteristicsTable = service.characteristics.joinToString(
            separator = "\n|--",
            prefix = "|--"
        ) { char ->
            var description = "${char.uuid}: ${char.printProperties()}"
            if (char.descriptors.isNotEmpty()) {
                description += "\n" + char.descriptors.joinToString(
                    separator = "\n|------",
                    prefix = "|------"
                ) { descriptor ->
                    "${descriptor.uuid}: ${descriptor.printProperties()}"
                }
            }
            description
        }
        Timber.i("Service ${service.uuid}\nCharacteristics:\n$characteristicsTable")
    }
}

fun BluetoothGattCharacteristic.printProperties(): String = mutableListOf<String>().apply {
    if (isReadable()) add("READABLE")
    if (isWritable()) add("WRITABLE")
    if (isWritableWithoutResponse()) add("WRITABLE WITHOUT RESPONSE")
    if (isIndicatable()) add("INDICATABLE")
    if (isNotifiable()) add("NOTIFIABLE")
    if (isEmpty()) add("EMPTY")
}.joinToString()

fun BluetoothGattCharacteristic.isReadable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

fun BluetoothGattCharacteristic.isWritable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

fun BluetoothGattCharacteristic.isIndicatable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_INDICATE)

fun BluetoothGattCharacteristic.isNotifiable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
    properties and property != 0

fun BluetoothGattDescriptor.printProperties(): String = mutableListOf<String>().apply {
    if (isReadable()) add("READABLE")
    if (isWritable()) add("WRITABLE")
    if (isEmpty()) add("EMPTY")
}.joinToString()

fun BluetoothGattDescriptor.isReadable(): Boolean =
    containsPermission(BluetoothGattDescriptor.PERMISSION_READ)

fun BluetoothGattDescriptor.isWritable(): Boolean =
    containsPermission(BluetoothGattDescriptor.PERMISSION_WRITE)

fun BluetoothGattDescriptor.containsPermission(permission: Int): Boolean =
    permissions and permission != 0
