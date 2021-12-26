package com.alangeorge.bleplay.viewmodel

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.right
import com.alangeorge.bleplay.common.Pipeline
import com.alangeorge.bleplay.common.pipe
import com.alangeorge.bleplay.common.toHexString
import com.alangeorge.bleplay.model.SnackbarMessage
import com.alangeorge.bleplay.repository.BleRepository
import com.alangeorge.bleplay.ui.DEVICE_ADDRESS_ARG_NAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
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
    private val scanResult = repository.scanResults[savedStateHandle.get(DEVICE_ADDRESS_ARG_NAME)]

    private val connectedGatts = mutableMapOf<String, BluetoothGatt>()
    private val gattResultFlow = MutableSharedFlow<GattCallbackResult>()
    private val gattCommandFlow = MutableSharedFlow<GattCommand>()

    private val gattResultCommandZip = gattResultFlow.zip(gattCommandFlow) { result, command ->
        Timber.d("zipping $result and $command")
        with(result) {
            when (command) {
                is GattCommand.DiscoverServices -> gatt.discoverServices()
                is GattCommand.RequestMtu -> gatt.requestMtu(gatt.GATT_MAX_MTU_SIZE)
                is GattCommand.StartNotifications -> startNotifications(gatt, command.serviceUuid, command.characteristicUuid)
                is GattCommand.ReadCharacteristic -> readCharacteristic(gatt, command.serviceUuid, command.characteristicUuid)
            }
        }
    }

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

    val gattStatusAndStateFlow = gattResultFlow
            .filterIsInstance<GattCallbackResult.ConnectionStateChanged>()
            .map {
                GattStatusAndState(it.status.gattStatusDescription, it.newState.gattStateDescription)
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

    private val _temperatureFlow = MutableStateFlow<String?>(null)
    val temperatureFlow = _temperatureFlow.asStateFlow()

    private val _temperatureHistoricFlow = MutableStateFlow<List<Float>>(emptyList())
    val temperatureHistoricFlow = _temperatureHistoricFlow.asStateFlow()


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
                    onCharacteristicChanged(gatt, characteristic)
                }
                BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                    viewModelScope.launch { snackbarMessages.produceEvent(SnackbarMessage("read not permitted for characteristic: ${characteristic.uuid}".right())) }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) =
            characteristic pipe (uuidToHandlerMap[characteristic.uuid] ?: ::handleUnknown)

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

    private val uuidToHandlerMap = mapOf(
        KnownServices.HeartRate.characteristics?.get(0)?.uuid to ::handleHeartRateChange,
        KnownServices.BatteryLevel.characteristics?.get(0)?.uuid to ::handleBatteryLevel,
        KnownServices.RaspberryPiTemperature.characteristics?.get(0)?.uuid to ::handleTemperature
    )

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

    private fun handleHeartRateChange(characteristic: BluetoothGattCharacteristic) {
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

    private fun handleTemperature(characteristic: BluetoothGattCharacteristic) {
        characteristic.value?.let {
            val tempAsString = String(it)
            Timber.d("temperature : $tempAsString")
            viewModelScope.launch {
                _temperatureFlow.emit(tempAsString)

                // "103.4 F" -> 103.4f
                tempAsString.split(" ").firstOrNull()?.toFloatOrNull()?.let { tempAsFloat ->
                    _temperatureHistoricFlow.emit(
                        _temperatureHistoricFlow.value.takeLast(49) + tempAsFloat
                    )
                }
            }
        }
    }

    private fun handleUnknown(characteristic: BluetoothGattCharacteristic) {
        Timber.d("unhandled characteristic: ${characteristic.uuid} ${characteristic.value?.toHexString()}")
    }

    private val connectionStateChanged: suspend (GattCallbackResult.ConnectionStateChanged) -> Unit = {
        val (deviceAddress, gatt, status, newState) = it

        _scanResultsFlow.emit(_scanResultsFlow.value?.copy(deviceData = gatt.device.asDeviceData))
        _isConnectingStatusFlow.emit(false)

        if (status == BluetoothGatt.GATT_SUCCESS) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.d("onConnectionStateChange: connected to $deviceAddress")
                    _connectedStatusFlow.emit(true)
                    connectedGatts[gatt.device.address] = gatt
                    gattCommandFlow.emit(GattCommand.DiscoverServices)
                    gattCommandFlow.emit(GattCommand.RequestMtu)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    gatt.close()
                    connectedGatts.remove(gatt.device.address)
                    _connectedStatusFlow.emit(false)
                    snackbarMessages.produceEvent(SnackbarMessage("device disconnected: ${gatt.device.address}".right()))
                }
            }
        } else {
            Timber.e("onConnectionStateChange: status $status encountered for $deviceAddress!")
            gatt.close()
            snackbarMessages.produceEvent(SnackbarMessage("device status changed: status:$status device:${gatt.device.address}".right()))
        }
    }

    private val servicesDiscovered: suspend (GattCallbackResult.ServicesDiscovered) -> Unit = {
        val (gatt, status) = it

        val deviceData = gatt.device.asDeviceData.copy(services = gatt.serviceList)
        _scanResultsFlow.emit(_scanResultsFlow.value?.copy(deviceData = deviceData))

        with(gatt) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.w("Discovered ${gatt.serviceList.size} services for ${device.address}.")
                printGattTable()
            } else {
                Timber.e("Service discovery failed due to status $status")
                close()
                snackbarMessages.produceEvent(SnackbarMessage("device status changed: status:$status device:${gatt.device.address}".right()))
            }
        }
    }

    private fun readCharacteristic(gatt: BluetoothGatt, serviceUuid: UUID, characteristicUuid: UUID) {
        val serviceName = KnownServices[serviceUuid]?.fold({ null }, { it.name }) ?: "unknown service"
        val characteristicName = KnownServices[characteristicUuid]?.fold({ it.name }, { null }) ?: "unknown characteristic"
        Timber.d("readCharacteristic: gatt = [${gatt}], serviceUuid = [${serviceUuid}], characteristicUuid = [${characteristicUuid}]")

        gatt
            .getService(serviceUuid)
            ?.getCharacteristic(characteristicUuid)
            ?.let {
                if (it.isReadable()) {
                    gatt.readCharacteristic(it)
                } else {
                    Timber.e("characteristic in not readable: $serviceName $characteristicName")
                }
            } ?: Timber.d("failed to look up service or characteristic UUID $serviceName $characteristicName")
    }

    private fun startNotifications(gatt: BluetoothGatt, serviceUuid: UUID, characteristicUuid: UUID) {
        val serviceName = KnownServices[serviceUuid]?.fold({ null }, { it.name }) ?: "unknown service"
        val characteristicName = KnownServices[characteristicUuid]?.fold({ it.name }, { null }) ?: "unknown characteristic"
        Timber.d("startNotifications: gatt = [${gatt}], serviceUuid = [${serviceUuid}], characteristicUuid = [${characteristicUuid}]")

        gatt
            .getService(serviceUuid)
            ?.getCharacteristic(characteristicUuid)
            ?.let { characteristic ->
                if (characteristic.isNotifiable()) {
                    characteristic.getDescriptor(CCC_DESCRIPTOR_UUID)?.let { cccDescriptor ->
                        if (gatt.setCharacteristicNotification(characteristic, true)) {
                            cccDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            if (!gatt.writeDescriptor(cccDescriptor)) {
                                Timber.e("writeDescriptor failed: $serviceName $characteristicName")
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

    private val launchKnownServices: suspend CoroutineScope.() -> Unit = {
        scanResultsFlow.filterNotNull().collect { scanData ->
            KnownServices.asList()
                .filter { knownService ->
                    scanData.deviceData.services.map(Service::uuid).contains(knownService.uuid)
                }.forEach { service ->
                    Timber.d("known service found: $service")
                    service.characteristics?.getOrNull(0)?.let { characteristic ->
                        gattCommandFlow.emit(
                            GattCommand.StartNotifications(
                                serviceUuid = service.uuid,
                                characteristicUuid = characteristic.uuid
                            )
                        )

                        when(service) {
                            KnownServices.BatteryLevel, KnownServices.RaspberryPiTemperature -> {
                                gattCommandFlow.emit(
                                    GattCommand.ReadCharacteristic(
                                        serviceUuid = service.uuid,
                                        characteristicUuid = characteristic.uuid
                                    )
                                )
                            }
                        }
                    } ?: Timber.d("unable to startNotifications: characteristic not found")
                }
        }
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
        viewModelScope.launch(context = Dispatchers.IO, block = launchKnownServices)

        viewModelScope.launch(Dispatchers.IO) {
            gattResultCommandZip.collect()
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
    val services: List<Service> = emptyList(),
    val bondState: BleBondState,
)

enum class BleBondState {
    NONE,
    BONDING,
    BONDED,
    UNKNOWN;
}

sealed class GattCommand {
    object DiscoverServices : GattCommand()
    object RequestMtu : GattCommand()
    data class StartNotifications(val serviceUuid: UUID, val characteristicUuid: UUID) : GattCommand()
    data class ReadCharacteristic(val serviceUuid: UUID, val characteristicUuid: UUID) : GattCommand()
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

@Suppress("unused")
sealed class GattCallbackResult(val gatt: BluetoothGatt) {
    class ConnectionStateChanged(
        val address: String,
        gatt: BluetoothGatt,
        val status: Int,
        val newState: Int
    ) : GattCallbackResult(gatt) {
        operator fun component1() = address
        operator fun component2() = gatt
        operator fun component3() = status
        operator fun component4() = newState
    }

    class ServicesDiscovered(
        gatt: BluetoothGatt,
        val status: Int
    ) : GattCallbackResult(gatt) {
        operator fun component1() = gatt
        operator fun component2() = status
    }

    class MtuChanged(
        gatt: BluetoothGatt,
        val mtu: Int,
        val status: Int
    ) : GattCallbackResult(gatt) {
        operator fun component1() = gatt
        operator fun component2() = mtu
        operator fun component3() = status
    }

    class DescriptorWrite(
        gatt: BluetoothGatt,
        val descriptor: BluetoothGattDescriptor,
        val status: Int
    ) : GattCallbackResult(gatt) {
        operator fun component1() = gatt
        operator fun component2() = descriptor
        operator fun component3() = status
    }
}

val BluetoothGatt.serviceList
    get() = services.map { service ->
        Service(
            uuid = service.uuid
        ).apply {
            characteristics = service.characteristics.map { characteristic ->
                Characteristic(uuid = characteristic.uuid)
            }
        }
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

val Int.gattStatusDescription
    get() = when(this) {
        BluetoothGatt.GATT_SUCCESS -> "GATT_SUCCESS"
        BluetoothGatt.GATT_READ_NOT_PERMITTED -> "GATT_READ_NOT_PERMITTED"
        BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "GATT_WRITE_NOT_PERMITTED"
        BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "GATT_INSUFFICIENT_AUTHENTICATION"
        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "GATT_REQUEST_NOT_SUPPORTED"
        BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "GATT_INSUFFICIENT_ENCRYPTION"
        BluetoothGatt.GATT_INVALID_OFFSET -> "GATT_INVALID_OFFSET"
        BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "GATT_INVALID_ATTRIBUTE_LENGTH"
        BluetoothGatt.GATT_CONNECTION_CONGESTED -> "GATT_CONNECTION_CONGESTED"
        BluetoothGatt.GATT_FAILURE -> "GATT_FAILURE"
        else -> "Unknown status: $this"
    }
val Int.gattStateDescription
    get() = when(this) {
        BluetoothProfile.STATE_DISCONNECTED -> "STATE_DISCONNECTED"
        BluetoothProfile.STATE_CONNECTING -> "STATE_CONNECTING"
        BluetoothProfile.STATE_CONNECTED -> "STATE_CONNECTED"
        BluetoothProfile.STATE_DISCONNECTING -> "STATE_DISCONNECTING"
        else -> "Unknown state: $this"
    }

data class GattStatusAndState(
    val status: String,
    val state: String
)