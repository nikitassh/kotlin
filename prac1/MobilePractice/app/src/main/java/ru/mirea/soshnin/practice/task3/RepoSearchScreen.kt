package ru.mirea.soshnin.practice.task3

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.mirea.soshnin.practice.ui.TaskScaffold

@Serializable
data class Repo(
    val id: Long,
    @SerialName("full_name") val fullName: String,
    val description: String? = null,
    @SerialName("stargazers_count") val stars: Int,
    val language: String? = null
)

// Чтение большого json из assets в фоновом потоке
suspend fun loadRepos(context: Context): List<Repo> =
    withContext(Dispatchers.IO) {
        val text = context.assets.open("github_repos.json")
            .bufferedReader().use { it.readText() }
        Json.decodeFromString<List<Repo>>(text)
    }

// "Поиск" с имитацией сети
suspend fun searchRepos(all: List<Repo>, query: String) =
    withContext(Dispatchers.Default) {
        delay(800)
        all.filter {
            it.fullName.contains(query, ignoreCase = true) ||
                it.description.orEmpty()
                    .contains(query, ignoreCase = true)
        }.sortedByDescending { it.stars }
    }

// debounce из методички: задача запускается только
// после паузы waitMs, прошлая задача отменяется
fun <T> CoroutineScope.debounce(
    waitMs: Long = 500L,
    destinationFunction: suspend (T) -> Unit
): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = launch {
            delay(waitMs)
            destinationFunction(param)
        }
    }
}

@Composable
fun RepoSearchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<Repo>()) }
    var loading by remember { mutableStateOf(false) }
    val allRepos = remember {
        scope.async { loadRepos(context) }
    }

    val onQuery = remember {
        scope.debounce<String>(500L) { text ->
            if (text.isBlank()) {
                results = emptyList()
                return@debounce
            }
            loading = true
            try {
                results = searchRepos(allRepos.await(), text)
            } finally {
                // при отмене индикатор тоже гасим
                loading = false
            }
        }
    }

    TaskScaffold("Задание 3. Поиск репозиториев", onBack) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onQuery(it)
            },
            label = { Text("Название репозитория") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Text(
            "Найдено: ${results.size}",
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results, key = { it.id }) { RepoCard(it) }
        }
    }
}

@Composable
fun RepoCard(repo: Repo) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(repo.fullName, fontWeight = FontWeight.Bold)
            Text(
                repo.description.orEmpty(),
                style = MaterialTheme.typography.bodySmall
            )
            Text("★ ${repo.stars}   ${repo.language ?: ""}")
        }
    }
}
