package ru.mirea.soshnin.network.auth.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import ru.mirea.soshnin.network.auth.domain.User
import ru.mirea.soshnin.network.ui.AppBar
import ru.mirea.soshnin.network.ui.StateContent
import ru.mirea.soshnin.network.ui.UiState

@Composable
fun AuthNav(onExit: () -> Unit) {
    val vm: AuthViewModel = viewModel()
    val loggedIn by vm.loggedIn.collectAsStateWithLifecycle()
    when (loggedIn) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        false -> {
            BackHandler { onExit() }
            LoginScreen(vm, onExit)
        }
        true -> {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "users") {
                composable("users") {
                    BackHandler { onExit() }
                    UsersListScreen(vm, onExit) { nav.navigate("user/$it") }
                }
                composable("user/{id}") { entry ->
                    val id = entry.arguments?.getString("id")!!.toInt()
                    LaunchedEffect(id) { vm.loadUser(id) }
                    UserDetailScreen(vm, id, onBack = { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
fun LoginScreen(vm: AuthViewModel, onExit: () -> Unit) {
    val state by vm.login.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loading = state is UiState.Loading
    Scaffold(topBar = { AppBar("Задание 3. Вход", onExit) }) { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Авторизация", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("dummyjson.com", color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(username, { username = it }, label = { Text("Username") },
                singleLine = true, enabled = !loading, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(password, { password = it }, label = { Text("Password") },
                singleLine = true, enabled = !loading,
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.login(username, password) }, enabled = !loading,
                modifier = Modifier.fillMaxWidth()) { Text("Войти") }
            Spacer(Modifier.height(16.dp))
            when (val s = state) {
                UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
                else -> {}
            }
        }
    }
}

@Composable
fun UsersListScreen(vm: AuthViewModel, onExit: () -> Unit, onOpen: (Int) -> Unit) {
    val state by vm.users.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        AppBar("Пользователи", onExit) {
            TextButton(onClick = vm::logout) { Text("Выйти", color = Color.White) }
        }
    }) { padding ->
        StateContent(state, onRetry = vm::loadUsers, modifier = Modifier.padding(padding)) { users ->
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users, key = { it.id }) { UserRow(it) { onOpen(it.id) } }
            }
        }
    }
}

@Composable
fun UserRow(user: User, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(user.image, user.fullName, Modifier.size(56.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(user.fullName, fontWeight = FontWeight.Bold)
                Text("@${user.username}", color = MaterialTheme.colorScheme.primary)
                Text(user.email, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun UserDetailScreen(vm: AuthViewModel, id: Int, onBack: () -> Unit) {
    val state by vm.user.collectAsStateWithLifecycle()
    Scaffold(topBar = { AppBar("Пользователь", onBack) }) { padding ->
        StateContent(state, onRetry = { vm.loadUser(id) }, modifier = Modifier.padding(padding)) { user ->
            Column(
                Modifier.padding(padding).padding(24.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(user.image, user.fullName, Modifier.size(180.dp).clip(CircleShape))
                Spacer(Modifier.height(16.dp))
                Text(user.firstName, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(user.lastName, fontSize = 22.sp)
                Spacer(Modifier.height(8.dp))
                Text(user.email, color = MaterialTheme.colorScheme.primary)
                Text("Телефон: ${user.phone}", color = Color.Gray)
                Text("Возраст: ${user.age}", color = Color.Gray)
                Spacer(Modifier.weight(1f))
                // выход: очистка токена и возврат на экран входа
                OutlinedButton(onClick = vm::logout, modifier = Modifier.fillMaxWidth()) { Text("Выйти") }
            }
        }
    }
}
