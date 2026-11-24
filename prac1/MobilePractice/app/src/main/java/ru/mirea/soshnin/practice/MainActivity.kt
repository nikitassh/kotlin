package ru.mirea.soshnin.practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ru.mirea.soshnin.practice.task10.LocationScreen
import ru.mirea.soshnin.practice.task11.PillScreen
import ru.mirea.soshnin.practice.task12.FactsScreen
import ru.mirea.soshnin.practice.task13.RateScreen
import ru.mirea.soshnin.practice.task14.CompassScreen
import ru.mirea.soshnin.practice.task3.RepoSearchScreen
import ru.mirea.soshnin.practice.task4.FeedScreen
import ru.mirea.soshnin.practice.task5.CounterScreen
import ru.mirea.soshnin.practice.task6.TimerScreen
import ru.mirea.soshnin.practice.task7.RandomScreen
import ru.mirea.soshnin.practice.task8.PhotoScreen
import ru.mirea.soshnin.practice.task9.WeatherScreen
import ru.mirea.soshnin.practice.ui.AppTheme
import ru.mirea.soshnin.practice.ui.MenuScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.createChannels(this)
        // можно сразу открыть задание: --ei task N
        val start = intent.getIntExtra("task", 0)
        setContent { AppTheme { App(start) } }
    }
}

@Composable
fun App(start: Int = 0) {
    // номер открытого задания, 0 - меню
    var task by rememberSaveable { mutableIntStateOf(start) }
    val back = { task = 0 }
    BackHandler(enabled = task != 0) { back() }

    when (task) {
        3 -> RepoSearchScreen(back)
        4 -> FeedScreen(back)
        5 -> CounterScreen(back)
        6 -> TimerScreen(back)
        7 -> RandomScreen(back)
        8 -> PhotoScreen(back)
        9 -> WeatherScreen(back)
        10 -> LocationScreen(back)
        11 -> PillScreen(back)
        12 -> FactsScreen(back)
        13 -> RateScreen(back)
        14 -> CompassScreen(back)
        else -> MenuScreen(onOpen = { task = it })
    }
}
