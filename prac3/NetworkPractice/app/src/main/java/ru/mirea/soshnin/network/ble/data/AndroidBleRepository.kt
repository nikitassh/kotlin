package ru.mirea.soshnin.network.ble.data

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ru.mirea.soshnin.network.ble.domain.*

// Реальная работа с BLE: сканирование, GATT, уведомления 0x2A37
@SuppressLint("MissingPermission") // разрешения запрашивает экран
class AndroidBleRepository(private val context: Context) : HeartRateRepository {
    private val adapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

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

    private var gatt: BluetoothGatt? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = BleDevice(
                name = result.device.name ?: result.scanRecord?.deviceName ?: "Без имени",
                address = result.device.address,
                rssi = result.rssi
            )
            // список обновляется в реальном времени, без дублей
            _devices.update { list -> (list.filterNot { it.address == device.address } + device) }
        }
    }

    override fun startScan() {
        val scanner = adapter?.bluetoothLeScanner ?: return
        stopScan()
        _devices.value = emptyList()
        scanner.startScan(scanCallback)
        _scanning.value = true
    }

    override fun stopScan() {
        if (_scanning.value) adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        _scanning.value = false
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _state.value = ConnectionState.Connected
                g.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _state.value = ConnectionState.Disconnected
                _heartRate.value = null
                g.close()
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val ch = g.getService(HeartRateUuids.SERVICE)
                ?.getCharacteristic(HeartRateUuids.MEASUREMENT) ?: return
            // включаем уведомления через CCC-дескриптор 0x2902
            g.setCharacteristicNotification(ch, true)
            val ccc = ch.getDescriptor(HeartRateUuids.CCC) ?: return
            val value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (Build.VERSION.SDK_INT >= 33) {
                g.writeDescriptor(ccc, value)
            } else {
                @Suppress("DEPRECATION")
                ccc.value = value
                @Suppress("DEPRECATION")
                g.writeDescriptor(ccc)
            }
        }

        // Android 13+
        override fun onCharacteristicChanged(
            g: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray
        ) {
            if (ch.uuid == HeartRateUuids.MEASUREMENT) _heartRate.value = parseHeartRate(value)
        }

        @Deprecated("Android 12 и ниже")
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            onCharacteristicChanged(g, ch, ch.value ?: return)
        }

        override fun onCharacteristicRead(
            g: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray, status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) _heartRate.value = parseHeartRate(value)
        }
    }

    override fun connect(device: BleDevice) {
        stopScan()
        val remote = adapter?.getRemoteDevice(device.address) ?: return
        _state.value = ConnectionState.Connecting
        _name.value = device.name
        gatt = remote.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    // кнопка "Обновить данные" - разовое чтение характеристики
    override fun refresh() {
        val g = gatt ?: return
        val ch = g.getService(HeartRateUuids.SERVICE)?.getCharacteristic(HeartRateUuids.MEASUREMENT) ?: return
        g.readCharacteristic(ch)
    }

    override fun disconnect() {
        gatt?.disconnect()
        gatt = null
        _state.value = ConnectionState.Disconnected
        _heartRate.value = null
        _name.value = null
    }

    val isAvailable get() = adapter != null && adapter.isEnabled
}
