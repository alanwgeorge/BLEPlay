package com.alangeorge.bleplay.viewmodel

import android.app.Application
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.right
import com.alangeorge.bleplay.common.Pipeline
import com.alangeorge.bleplay.model.SnackbarMessage
import com.alangeorge.bleplay.repository.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BleViewModel @Inject constructor(
    private val repository: BleRepository,
    private val snackbarMessagePipeline: Pipeline<SnackbarMessage>,
    application: Application
) : ViewModel() {
    private val manager = application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter = manager?.adapter
    private val scanner = adapter?.bluetoothLeScanner

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            Timber.d("onScanResult: callbackType = [${callbackType}], result = [${result}]")
            super.onScanResult(callbackType, result)

            result?.let {
                viewModelScope.launch {
                    _scanResultsMap.emit(
                        _scanResultsMap.value.toMutableMap().apply {
                            put(result.device.address, result)
                        }
                    )
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            Timber.d("onBatchScanResults: results = [${results}]")
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("onScanFailed: errorCode = [${errorCode}]")
            super.onScanFailed(errorCode)

            viewModelScope.launch {
                snackbarMessagePipeline.produceEvent(SnackbarMessage("Ble scan failed: $errorCode".right()))
            }
        }
    }

    private val settings =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ScanSettings.Builder()
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        } else {
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        }

    private val _scanResultsMap = MutableStateFlow<Map<String, ScanResult>>(value = emptyMap())

    val scanResults
        get() = _scanResultsMap.map { map ->
            map.toList().map { it.second }
        }

    val selectedServiceFilter = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            selectedServiceFilter.collect {
                _scanResultsMap.emit(emptyMap())
            }
        }
    }

    fun startScan() {
        if (adapter?.isEnabled == true && scanner != null) {
            val filter = listOf(serviceFilters[selectedServiceFilter.value].first)
            scanner.startScan(filter, settings, scanCallback)
        } else {
            viewModelScope.launch {
                snackbarMessagePipeline.produceEvent(SnackbarMessage("Ble adapter not enabled".right()))
            }
        }
    }

    fun stopScan() {
        scanner?.stopScan(scanCallback)
    }

    fun scanStatus() {
        val message = "status: enabled=${adapter?.isEnabled} mode=${adapter?.scanMode} state=${adapter?.state} name=${adapter?.name}"
        viewModelScope.launch {
            snackbarMessagePipeline.produceEvent(SnackbarMessage(message.right()))
        }
    }

    fun clearResults() {
        _scanResultsMap.value = emptyMap()
    }

    fun findDevice(address: String): ScanResult? = _scanResultsMap.value[address]

    companion object {
        val serviceFilters = listOf(
            ScanFilter.Builder().build() to "No Filter",
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")))
                .build() to "Heart Rate",
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")))
                .build() to "Cycling Power",
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString("0000fe51-0000-1000-8000-00805f9b34fb")))
                .build() to "SRAM"
        )
    }
}