package com.alangeorge.bleplay.viewmodel

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import java.util.*

const val SERVICE_NAME_HEART_RATE = "Heart Rate"
const val SERVICE_NAME_BATTERY_LEVEL = "Battery Level"
const val SERVICE_NAME_CYCLING_POWER = "Cycling Power"
const val SERVICE_NAME_SRAM = "SRAM"

const val CHARACTERISTIC_NAME_BATTERY_LEVEL = "Battery Level"
const val CHARACTERISTIC_NAME_HEART_RATE = "Heart Rate"

val CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

val serviceUuids = mapOf<String, UUID>(
    SERVICE_NAME_HEART_RATE to UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"),
    SERVICE_NAME_BATTERY_LEVEL to UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"),
    SERVICE_NAME_CYCLING_POWER to UUID.fromString("00001818-0000-1000-8000-00805f9b34fb"),
    SERVICE_NAME_SRAM to UUID.fromString("0000fe51-0000-1000-8000-00805f9b34fb")
)

val characteristicUuid = mapOf<String, UUID>(
    CHARACTERISTIC_NAME_BATTERY_LEVEL to UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"),
    CHARACTERISTIC_NAME_HEART_RATE to UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
)

val serviceFilters = listOf(
    ScanFilter.Builder().build() to "No Filter",
    ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(serviceUuids[SERVICE_NAME_HEART_RATE]))
        .build() to SERVICE_NAME_HEART_RATE,
    ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(serviceUuids[SERVICE_NAME_BATTERY_LEVEL]))
        .build() to SERVICE_NAME_BATTERY_LEVEL,
    ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(serviceUuids[SERVICE_NAME_CYCLING_POWER]))
        .build() to SERVICE_NAME_CYCLING_POWER,
    ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(serviceUuids[SERVICE_NAME_SRAM]))
        .build() to SERVICE_NAME_SRAM
)
