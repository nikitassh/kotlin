package ru.mirea.soshnin.practice.task10

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import ru.mirea.soshnin.practice.ui.TaskScaffold
import java.util.Locale
import kotlin.coroutines.resume

sealed interface LocationUi {
    data object Idle : LocationUi
    data object Loading : LocationUi
    data class Done(
        val address: String, val lat: Double, val lng: Double
    ) : LocationUi
    data class Error(val message: String) : LocationUi
}

class LocationException(message: String) : Exception(message)

// Получение текущих координат через FusedLocationProviderClient
@SuppressLint("MissingPermission")
suspend fun currentLocation(context: Context): Location {
    val manager = context.getSystemService(LocationManager::class.java)
    if (!LocationManagerCompat.isLocationEnabled(manager)) {
        throw LocationException("Геолокация выключена. Включите GPS")
    }
    val client = LocationServices
        .getFusedLocationProviderClient(context)
    val token = CancellationTokenSource()
    val fresh = withTimeoutOrNull(15_000) {
        client.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY, token.token
        ).await()
    }
    if (fresh == null) token.cancel()
    return fresh ?: client.lastLocation.await()
        ?: throw LocationException("Не удалось определить координаты")
}

// Обратное геокодирование: координаты -> адрес
suspend fun addressOf(context: Context, loc: Location): String {
    if (!Geocoder.isPresent()) {
        throw LocationException("Геокодер недоступен на устройстве")
    }
    val geocoder = Geocoder(context, Locale("ru"))
    val list: List<Address> = if (Build.VERSION.SDK_INT >= 33) {
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(
                loc.latitude, loc.longitude, 1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(a: MutableList<Address>) =
                        cont.resume(a)

                    override fun onError(msg: String?) =
                        cont.resume(emptyList())
                }
            )
        }
    } else {
        withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                .orEmpty()
        }
    }
    val a = list.firstOrNull() ?: throw LocationException(
        "Адрес не найден. Проверьте интернет"
    )
    return a.getAddressLine(0) ?: listOfNotNull(
        a.thoroughfare, a.subThoroughfare, a.locality, a.countryName
    ).joinToString(", ")
}

@Composable
fun LocationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ui by remember { mutableStateOf<LocationUi>(LocationUi.Idle) }

    fun load() {
        ui = LocationUi.Loading
        scope.launch {
            ui = try {
                val loc = currentLocation(context)
                try {
                    val address = addressOf(context, loc)
                    LocationUi.Done(address, loc.latitude, loc.longitude)
                } catch (e: Exception) {
                    // координаты есть, адреса нет
                    LocationUi.Done(
                        e.message ?: "Адрес не получен",
                        loc.latitude, loc.longitude
                    )
                }
            } catch (e: LocationException) {
                LocationUi.Error(e.message!!)
            } catch (e: Exception) {
                LocationUi.Error("Ошибка: ${e.message}")
            }
        }
    }

    val permissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            load()
        } else {
            ui = LocationUi.Error("Нет разрешения на геолокацию")
        }
    }

    TaskScaffold("Задание 10. Мой адрес", onBack) {
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            permissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }, enabled = ui != LocationUi.Loading) {
            Text("Получить мой адрес")
        }
        Spacer(Modifier.height(32.dp))
        when (val s = ui) {
            LocationUi.Idle -> Text("Нажмите кнопку")
            LocationUi.Loading -> CircularProgressIndicator()
            is LocationUi.Done -> {
                Text(
                    s.address,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text("lat: %.5f / lng: %.5f".format(s.lat, s.lng))
            }
            is LocationUi.Error -> Text(
                s.message,
                color = Color(0xFFC62828),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}
