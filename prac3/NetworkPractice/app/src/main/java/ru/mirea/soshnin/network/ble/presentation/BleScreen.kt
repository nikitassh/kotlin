package ru.mirea.soshnin.network.ble.presentation

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.mirea.soshnin.network.ble.data.AndroidBleRepository
import ru.mirea.soshnin.network.ble.data.DemoBleRepository
import ru.mirea.soshnin.network.ble.domain.BleDevice
import ru.mirea.soshnin.network.ble.domain.ConnectionState
import ru.mirea.soshnin.network.ble.domain.HeartRateRepository
import ru.mirea.soshnin.network.ui.AppBar

class BleViewModel(app: Application, demo: Boolean) : AndroidViewModel(app) {
    val repo: HeartRateRepository = if (demo) DemoBleRepository() else AndroidBleRepository(app)

    override fun onCleared() {
        repo.stopScan()
        repo.disconnect()
    }

    class Factory(private val app: Application, private val demo: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BleViewModel(app, demo) as T
    }
}

private val blePermissions = if (Build.VERSION.SDK_INT >= 31) {
    arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
} else {
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
}

@Composable
fun BleScreen(demo: Boolean, onExit: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val vm: BleViewModel = viewModel(factory = BleViewModel.Factory(app, demo))
    val repo = vm.repo
    val devices by repo.devices.collectAsStateWithLifecycle()
    val scanning by repo.scanning.collectAsStateWithLifecycle()
    val state by repo.state.collectAsStateWithLifecycle()
    val heartRate by repo.heartRate.collectAsStateWithLifecycle()
    val name by repo.connectedName.collectAsStateWithLifecycle()
    var granted by remember { mutableStateOf(demo) }

    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result.values.all { it }
        if (granted) repo.startScan()
    }

    Scaffold(topBar = { AppBar("Задание 7. Пульсометр BLE", onExit) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text(
                "Статус: " + if (scanning) "Scanning…" else state.title,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (state == ConnectionState.Disconnected) {
                Button(
                    onClick = { if (granted) repo.startScan() else permissions.launch(blePermissions) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (scanning) "Перезапустить сканирование" else "Начать сканирование") }
                Spacer(Modifier.height(8.dp))
                Text("Найдено устройств: ${devices.size}", color = Color.Gray)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices, key = { it.address }) { DeviceRow(it) { repo.connect(it) } }
                }
            } else {
                HeartRateCard(name, state, heartRate)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = repo::refresh,
                    enabled = state == ConnectionState.Connected,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Обновить данные") }
                OutlinedButton(onClick = repo::disconnect, modifier = Modifier.fillMaxWidth()) {
                    Text("Отключиться")
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(device: BleDevice, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.Bold)
                Text(device.address, color = Color.Gray)
            }
            Text("${device.rssi} dBm")
        }
    }
}

@Composable
private fun HeartRateCard(name: String?, state: ConnectionState, bpm: Int?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name ?: "", style = MaterialTheme.typography.titleMedium)
            Text(state.title, color = if (state == ConnectionState.Connected) Color(0xFF2E7D32) else Color.Gray)
            Spacer(Modifier.height(16.dp))
            Icon(Icons.Default.Favorite, null, tint = Color(0xFFE53935), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                "Heart Rate: " + (bpm?.let { "$it bpm" } ?: "—"),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
