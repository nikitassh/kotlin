package ru.mirea.soshnin.network.ble.domain

import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

data class BleDevice(val name: String, val address: String, val rssi: Int)

enum class ConnectionState(val title: String) {
    Disconnected("Disconnected"),
    Connecting("Connecting"),
    Connected("Connected")
}

object HeartRateUuids {
    val SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    val MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    val CCC: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
}

interface HeartRateRepository {
    val devices: StateFlow<List<BleDevice>>
    val scanning: StateFlow<Boolean>
    val state: StateFlow<ConnectionState>
    val heartRate: StateFlow<Int?>
    val connectedName: StateFlow<String?>
    fun startScan()
    fun stopScan()
    fun connect(device: BleDevice)
    fun refresh()
    fun disconnect()
}

// Heart Rate Measurement: бит 0 флагов - формат значения (UINT8 или UINT16)
fun parseHeartRate(value: ByteArray): Int? {
    if (value.isEmpty()) return null
    val flags = value[0].toInt()
    return if (flags and 0x01 == 0) {
        value.getOrNull(1)?.toInt()?.and(0xFF)
    } else {
        if (value.size < 3) null
        else (value[1].toInt() and 0xFF) or
            ((value[2].toInt() and 0xFF) shl 8)
    }
}
