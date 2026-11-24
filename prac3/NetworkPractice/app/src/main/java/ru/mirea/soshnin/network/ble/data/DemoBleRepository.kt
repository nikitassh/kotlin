package ru.mirea.soshnin.network.ble.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.mirea.soshnin.network.ble.domain.*
import kotlin.random.Random

// Демо-источник для эмулятора, где нет BLE-устройств:
// отдает те же байты Heart Rate Measurement, что и настоящий датчик
class DemoBleRepository : HeartRateRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val devices = _devices.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    override val scanning = _scanning.asStateFlow()
    private val _state = MutableStateFlow(ConnectionState.Disconnected)
    override val state = _state.asStateFlow()
    private val _heartRate = MutableStateFlow<Int?>(null)
    override val heartRate = _heartRate.asStateFlow()
    private val _name = MutableStateFlow<String?>(null)
    override val connectedName = _name.asStateFlow()

    private var scanJob: Job? = null
    private var hrJob: Job? = null
    private var bpm = 72

    private val nearby = listOf(
        BleDevice("Polar H10 7A3B21", "C4:7C:8D:6A:3B:21", -58),
        BleDevice("HR Monitor", "E2:15:9F:40:11:0C", -66),
        BleDevice("Mi Smart Band 7", "D8:3A:DD:12:7F:90", -74),
        BleDevice("Без имени", "5F:21:A8:03:C1:4E", -88)
    )

    override fun startScan() {
        stopScan()
        _devices.value = emptyList()
        _scanning.value = true
        scanJob = scope.launch {
            for (d in nearby) {
                delay(Random.nextLong(400, 900))
                _devices.value = _devices.value + d.copy(rssi = d.rssi + Random.nextInt(-3, 4))
            }
        }
    }

    override fun stopScan() {
        scanJob?.cancel()
        _scanning.value = false
    }

    override fun connect(device: BleDevice) {
        stopScan()
        _name.value = device.name
        _state.value = ConnectionState.Connecting
        hrJob?.cancel()
        hrJob = scope.launch {
            delay(1200)
            _state.value = ConnectionState.Connected
            delay(800)
            while (isActive) {
                bpm = (bpm + Random.nextInt(-2, 3)).coerceIn(62, 96)
                // флаги 0x00: значение UINT8
                _heartRate.value = parseHeartRate(byteArrayOf(0x00, bpm.toByte()))
                delay(1000)
            }
        }
    }

    override fun refresh() {
        if (_state.value == ConnectionState.Connected) {
            _heartRate.value = parseHeartRate(byteArrayOf(0x00, bpm.toByte()))
        }
    }

    override fun disconnect() {
        hrJob?.cancel()
        _state.value = ConnectionState.Disconnected
        _heartRate.value = null
        _name.value = null
    }
}
