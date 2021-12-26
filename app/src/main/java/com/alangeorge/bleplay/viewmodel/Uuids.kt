package com.alangeorge.bleplay.viewmodel

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.left
import arrow.core.right
import java.util.*

const val SERVICE_NAME_HEART_RATE = "Heart Rate"
const val SERVICE_NAME_BATTERY_LEVEL = "Battery Level"
const val SERVICE_NAME_CYCLING_POWER = "Cycling Power"
const val SERVICE_NAME_SRAM = "SRAM"
const val SERVICE_NAME_RASPBERRY_PI_TEMPERATURE = "Raspberry PI Temperature"

const val CHARACTERISTIC_NAME_BATTERY_LEVEL = "Battery Level"
const val CHARACTERISTIC_NAME_HEART_RATE = "Heart Rate"
const val CHARACTERISTIC_NAME_TEMPERATURE = "Temperature"
const val CHARACTERISTIC_NAME_TEMPERATURE_UNITS = "Temperature Units"

val CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

object KnownServices {
    val HeartRate = Service(
        name = SERVICE_NAME_HEART_RATE,
        uuid = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"),
    ).apply {
        characteristics = listOf(
            Characteristic(
                name = CHARACTERISTIC_NAME_HEART_RATE,
                uuid = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"),
                service = this
            )
        )
    }

    val BatteryLevel = Service(
        name = SERVICE_NAME_BATTERY_LEVEL,
        uuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"),
    ).apply {
        characteristics = listOf(
            Characteristic(
                name = CHARACTERISTIC_NAME_BATTERY_LEVEL,
                uuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"),
                service = this
            )
        )
    }

    // https://github.com/Douglas6/cputemp
    val RaspberryPiTemperature = Service(
        name = SERVICE_NAME_RASPBERRY_PI_TEMPERATURE,
        uuid = UUID.fromString("00000001-710e-4a5b-8d75-3e5b444bc3cf"),
    ).apply {
        characteristics = listOf(
            Characteristic(
                name = CHARACTERISTIC_NAME_TEMPERATURE,
                uuid = UUID.fromString("00000002-710e-4a5b-8d75-3e5b444bc3cf"),
                service = this
            ),
            Characteristic(
                name = CHARACTERISTIC_NAME_TEMPERATURE_UNITS,
                uuid = UUID.fromString("00000003-710e-4a5b-8d75-3e5b444bc3cf"),
                service = this
            )
        )
    }

    fun asList() = listOf(HeartRate, BatteryLevel, RaspberryPiTemperature)

    operator fun get(uuid: UUID): Either<Characteristic, Service>? =
        asList()
            .firstOrNull {
                it.uuid == uuid
            }
            ?.right() ?: asList()
            .flatMap { it.characteristics ?: emptyList() }
            .firstOrNull {
                it.uuid == uuid
            }
            ?.left()
}

val serviceUuids = mapOf(
    SERVICE_NAME_HEART_RATE to UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"),
    SERVICE_NAME_BATTERY_LEVEL to UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"),
    SERVICE_NAME_CYCLING_POWER to UUID.fromString("00001818-0000-1000-8000-00805f9b34fb"),
    SERVICE_NAME_SRAM to UUID.fromString("0000fe51-0000-1000-8000-00805f9b34fb")
)

val characteristicUuid = mapOf(
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

data class Service(
    val name: String? = null,
    val uuid: UUID,
) {
    var characteristics: List<Characteristic>? = null
}

data class Characteristic(
    val name: String? = null,
    val uuid: UUID,
    val service: Service? = null
)
