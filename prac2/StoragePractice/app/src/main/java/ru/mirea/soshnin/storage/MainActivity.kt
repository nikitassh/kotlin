package ru.mirea.soshnin.storage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ru.mirea.soshnin.storage.diary.DiaryApp
import ru.mirea.soshnin.storage.gallery.GalleryApp
import ru.mirea.soshnin.storage.todolist.navigation.TodoNavGraph
import ru.mirea.soshnin.storage.ui.AppTheme
import ru.mirea.soshnin.storage.ui.MenuScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    when (task) {
        1 -> DiaryApp(back)
        2 -> GalleryApp(back)
        4 -> TodoNavGraph(back)
        else -> MenuScreen(onOpen = { task = it })
    }
    BackHandler(enabled = task != 0 && task != 4) { back() }
}
