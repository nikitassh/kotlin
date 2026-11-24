package ru.mirea.soshnin.network

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ru.mirea.soshnin.network.auth.presentation.AuthNav
import ru.mirea.soshnin.network.ble.presentation.BleScreen
import ru.mirea.soshnin.network.nobel.presentation.NobelNav
import ru.mirea.soshnin.network.photos.presentation.PhotosNav
import ru.mirea.soshnin.network.ui.AppTheme
import ru.mirea.soshnin.network.ui.MenuScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // можно сразу открыть задание: --ei task N
        val start = intent.getIntExtra("task", 0)
        val demoBle = intent.getBooleanExtra("demo", false)
        setContent { AppTheme { App(start, demoBle) } }
    }
}

@Composable
fun App(start: Int, demoBle: Boolean) {
    var task by rememberSaveable { mutableIntStateOf(start) }
    val back = { task = 0 }
    when (task) {
        1 -> PhotosNav(back)
        2 -> NobelNav(useOwnServer = false, onExit = back)
        3 -> AuthNav(back)
        6 -> NobelNav(useOwnServer = true, onExit = back)
        7 -> BleScreen(demoBle, back)
        else -> MenuScreen(onOpen = { task = it })
    }
    // в заданиях с навигацией назад обрабатывает NavHost
    BackHandler(enabled = task == 7) { back() }
}
