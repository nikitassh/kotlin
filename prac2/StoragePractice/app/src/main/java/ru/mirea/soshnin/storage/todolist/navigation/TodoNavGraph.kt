package ru.mirea.soshnin.storage.todolist.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.mirea.soshnin.storage.todolist.presentation.ui.screen.TodoEditScreen
import ru.mirea.soshnin.storage.todolist.presentation.ui.screen.TodoListScreen
import ru.mirea.soshnin.storage.todolist.presentation.viewmodel.TodoViewModel

object Routes {
    const val LIST = "list"
    const val EDIT = "edit/{id}"
    fun edit(id: Int) = "edit/$id"
}

@Composable
fun TodoNavGraph(onExit: () -> Unit) {
    val nav = rememberNavController()
    val vm: TodoViewModel = viewModel()
    NavHost(nav, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            TodoListScreen(vm, onExit, onEdit = { nav.navigate(Routes.edit(it)) })
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { entry ->
            val id = entry.arguments?.getInt("id") ?: 0
            TodoEditScreen(vm, id, onBack = { nav.popBackStack() })
        }
    }
}
