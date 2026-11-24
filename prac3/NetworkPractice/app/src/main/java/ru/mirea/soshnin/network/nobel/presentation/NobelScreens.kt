package ru.mirea.soshnin.network.nobel.presentation

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import ru.mirea.soshnin.network.nobel.domain.LaureateDetail
import ru.mirea.soshnin.network.nobel.domain.LaureateItem
import ru.mirea.soshnin.network.nobel.domain.categories
import ru.mirea.soshnin.network.ui.AppBar
import ru.mirea.soshnin.network.ui.StateContent
import ru.mirea.soshnin.network.ui.UiState

@Composable
fun NobelNav(useOwnServer: Boolean, onExit: () -> Unit) {
    val app = LocalContext.current.applicationContext as Application
    val vm: NobelViewModel = viewModel(
        key = "nobel-$useOwnServer",
        factory = NobelViewModel.Factory(app, useOwnServer)
    )
    val loggedIn by vm.loggedIn.collectAsStateWithLifecycle()
    when (loggedIn) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        false -> {
            BackHandler { onExit() }
            ServerLoginScreen(vm, onExit)
        }
        true -> {
            val nav = rememberNavController()
            NavHost(nav, startDestination = "list") {
                composable("list") {
                    BackHandler { onExit() }
                    LaureatesScreen(vm, onExit) {
                        vm.open(it)
                        nav.navigate("detail")
                    }
                }
                composable("detail") { LaureateDetailScreen(vm) { nav.popBackStack() } }
            }
        }
    }
}

@Composable
fun ServerLoginScreen(vm: NobelViewModel, onExit: () -> Unit) {
    val state by vm.loginState.collectAsStateWithLifecycle()
    var username by remember { mutableStateOf("student") }
    var password by remember { mutableStateOf("") }
    Scaffold(topBar = { AppBar("Задание 6. Вход на сервер", onExit) }) { padding ->
        Column(
            Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nobel Prize API", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("свой сервер на Ktor + PostgreSQL (Neon)", color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(username, { username = it }, label = { Text("Логин") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(password, { password = it }, label = { Text("Пароль") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            val loading = state is UiState.Loading
            Button(onClick = { vm.login(username, password) }, enabled = !loading,
                modifier = Modifier.fillMaxWidth()) { Text("Войти") }
            Spacer(Modifier.height(16.dp))
            when (val s = state) {
                UiState.Loading -> CircularProgressIndicator()
                is UiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaureatesScreen(vm: NobelViewModel, onExit: () -> Unit, onOpen: (LaureateItem) -> Unit) {
    val state by vm.list.collectAsStateWithLifecycle()
    val year by vm.year.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    val title = if (vm.ownServer) "Задание 6. Свой сервер" else "Задание 2. Лауреаты"
    Scaffold(topBar = {
        AppBar(title, onExit) {
            if (vm.ownServer) TextButton(onClick = vm::logout) { Text("Выйти", color = Color.White) }
        }
    }) { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                Modifier.padding(12.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { vm.year.value = it.filter(Char::isDigit).take(4) },
                    label = { Text("Год") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(96.dp)
                )
                // выпадающий список категорий
                ExposedDropdownMenuBox(expanded, { expanded = it }, Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = categories[category].orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded, { expanded = false }) {
                        categories.forEach { (code, name) ->
                            DropdownMenuItem(text = { Text(name) }, onClick = {
                                vm.category.value = code
                                expanded = false
                            })
                        }
                    }
                }
            }
            Button(onClick = vm::load, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                Text("Найти")
            }
            StateContent(state, onRetry = vm::load) { list ->
                if (list.isEmpty()) {
                    Text("Ничего не найдено", Modifier.padding(16.dp))
                }
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list, key = { it.key }) { LaureateCard(it) { onOpen(it) } }
                }
            }
        }
    }
}

@Composable
fun LaureateCard(item: LaureateItem, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "${item.year} · ${item.categoryName}",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(item.fullName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            // первые 100 символов мотивации
            val m = item.motivation
            Text(if (m.length > 100) m.take(100) + "…" else m, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun LaureateDetailScreen(vm: NobelViewModel, onBack: () -> Unit) {
    val state by vm.detail.collectAsStateWithLifecycle()
    Scaffold(topBar = { AppBar("Лауреат", onBack) }) { padding ->
        StateContent(state, onRetry = { vm.reloadDetail() }, modifier = Modifier.padding(padding)) { d ->
            DetailContent(d, vm, Modifier.padding(padding))
        }
    }
}

@Composable
private fun DetailContent(d: LaureateDetail, vm: NobelViewModel, modifier: Modifier) {
    val item = d.item
    Column(
        modifier.padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (d.portraitUrl != null) {
            AsyncImage(d.portraitUrl, item.fullName, Modifier.size(140.dp))
        } else {
            // портрета нет - кружок с инициалами
            Box(
                Modifier.size(120.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val initials = item.fullName.split(" ").filter { it.isNotBlank() }
                    .take(2).joinToString("") { it.take(1) }
                Text(initials, color = Color.White, fontSize = 40.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(item.fullName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("${item.year} · ${item.categoryName} · доля ${item.portion}", color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Text("«${item.motivation}»", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Info("Страна", d.birthCountry ?: "нет данных")
        Info("Место рождения", d.birthPlace ?: "нет данных")
        Info("Дата рождения", d.birthDate ?: "нет данных")
        if (vm.ownServer) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = { vm.toggleFavorite(d) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Star, null, tint = if (d.isFavorite) Color(0xFFFFB300) else Color.Gray)
                Spacer(Modifier.width(8.dp))
                Text(if (d.isFavorite) "Убрать из избранного" else "В избранное")
            }
        }
    }
}

@Composable
private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.width(150.dp), color = Color.Gray)
        Text(value)
    }
}
