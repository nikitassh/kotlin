package ru.mirea.soshnin.practice.task4

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.mirea.soshnin.practice.ui.TaskScaffold
import kotlin.random.Random

@Serializable
data class Post(
    val id: Int, val userId: Int, val author: String,
    val title: String, val body: String,
    val avatarUrl: String, val avatarColor: String
)

@Serializable
data class Comment(
    val postId: Int, val id: Int,
    val name: String, val body: String
)

enum class LoadState { Loading, Ready, Error }

data class PostUi(
    val post: Post,
    val state: LoadState = LoadState.Loading,
    val avatar: Color? = null,
    val comments: List<Comment>? = null
)

val json = Json { ignoreUnknownKeys = true }

suspend inline fun <reified T> readAsset(
    context: Context, name: String
): T = withContext(Dispatchers.IO) {
    val text = context.assets.open(name)
        .bufferedReader().use { it.readText() }
    json.decodeFromString<T>(text)
}

// Имитация сети: задержка и случайный сбой
suspend fun fakeNetwork() {
    delay(Random.nextLong(700, 3500))
    if (Random.nextInt(100) < 15) error("сеть недоступна")
}

suspend fun loadAvatar(post: Post): Color {
    fakeNetwork()
    return Color(post.avatarColor.toColorInt())
}

suspend fun loadComments(
    post: Post, all: List<Comment>
): List<Comment> {
    fakeNetwork()
    return all.filter { it.postId == post.id }
}

// try-catch внутри async: ошибка одной подзадачи
// не отменяет соседнюю, отмену пробрасываем дальше
fun <T> CoroutineScope.safeAsync(
    block: suspend () -> T
) = async {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

// Параллельная загрузка аватарки и комментариев поста
suspend fun loadPost(
    post: Post, all: Deferred<List<Comment>>
): PostUi = supervisorScope {
    val avatar = safeAsync { loadAvatar(post) }
    val comments = safeAsync { loadComments(post, all.await()) }
    val a = avatar.await()
    val c = comments.await()
    PostUi(
        post = post,
        state = if (a != null && c != null) {
            LoadState.Ready
        } else {
            LoadState.Error
        },
        avatar = a,
        comments = c
    )
}

@Composable
fun FeedScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items = remember { mutableStateListOf<PostUi>() }
    var feedJob by remember { mutableStateOf<Job?>(null) }

    fun refresh() {
        // отменяем все текущие загрузки (дочерние задачи тоже)
        feedJob?.cancel()
        feedJob = scope.launch {
            items.clear()
            val posts = readAsset<List<Post>>(
                context, "social_posts.json"
            )
            items.addAll(posts.map { PostUi(it) })
            val comments = async {
                readAsset<List<Comment>>(context, "comments.json")
            }
            // каждый пост грузится сам и показывается сразу
            posts.forEachIndexed { index, post ->
                launch { items[index] = loadPost(post, comments) }
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    TaskScaffold("Задание 4. Лента", onBack) {
        Button(onClick = { refresh() }) { Text("Обновить") }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.post.id }) { PostCard(it) }
        }
    }
}

@Composable
fun PostCard(item: PostUi) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(item)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.post.author, fontWeight = FontWeight.Bold)
                    Text(item.post.title)
                }
                StateLabel(item.state)
            }
            Text(item.post.body, Modifier.padding(vertical = 8.dp))
            Comments(item)
        }
    }
}

@Composable
fun Avatar(item: PostUi) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(item.avatar ?: Color.LightGray, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            item.avatar != null ->
                Text(item.post.author.take(1), color = Color.White)
            item.state == LoadState.Loading ->
                CircularProgressIndicator(Modifier.size(24.dp))
            else -> Icon(Icons.Default.Warning, "Ошибка")
        }
    }
}

@Composable
fun StateLabel(state: LoadState) {
    val color = when (state) {
        LoadState.Loading -> Color.Gray
        LoadState.Ready -> Color(0xFF2E7D32)
        LoadState.Error -> Color(0xFFC62828)
    }
    Text(state.name, color = color)
}

@Composable
fun Comments(item: PostUi) {
    val comments = item.comments
    when {
        comments != null && comments.isEmpty() ->
            Text("Комментариев нет", color = Color.Gray)
        comments != null -> comments.forEach {
            Text("${it.name}: ${it.body}")
        }
        item.state == LoadState.Loading ->
            Text("Загрузка комментариев...", color = Color.Gray)
        else -> Text(
            "⚠ Комментарии не загрузились",
            color = Color(0xFFC62828)
        )
    }
}
