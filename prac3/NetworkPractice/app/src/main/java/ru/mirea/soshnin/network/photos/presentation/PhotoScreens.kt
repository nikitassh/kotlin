package ru.mirea.soshnin.network.photos.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import ru.mirea.soshnin.network.photos.domain.Photo
import ru.mirea.soshnin.network.ui.AppBar
import ru.mirea.soshnin.network.ui.StateContent

@Composable
fun PhotosNav(onExit: () -> Unit) {
    val nav = rememberNavController()
    val vm: PhotosViewModel = viewModel()
    NavHost(nav, startDestination = "list") {
        composable("list") {
            PhotoListScreen(vm, onExit) { nav.navigate("detail/$it") }
        }
        composable("detail/{id}") { entry ->
            val id = entry.arguments?.getString("id")!!
            LaunchedEffect(id) { vm.loadDetail(id) }
            PhotoDetailScreen(vm, id) { nav.popBackStack() }
        }
    }
    androidx.activity.compose.BackHandler(nav.previousBackStackEntry == null) { onExit() }
}

@Composable
fun PhotoListScreen(vm: PhotosViewModel, onExit: () -> Unit, onOpen: (String) -> Unit) {
    val state by vm.list.collectAsStateWithLifecycle()
    Scaffold(topBar = { AppBar("Задание 1. Фотокаталог", onExit) }) { padding ->
        StateContent(state, onRetry = vm::loadList, modifier = Modifier.padding(padding)) { photos ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    PhotoCard(photo) { onOpen(photo.id) }
                }
            }
        }
    }
}

@Composable
fun PhotoCard(photo: Photo, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        AsyncImage(
            model = photo.thumbnailUrl,
            contentDescription = photo.author,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.5f)
        )
        Column(Modifier.padding(8.dp)) {
            Text(photo.author, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${photo.width} × ${photo.height}", color = Color.Gray)
        }
    }
}

const val DOWNLOADS_URI =
    "content://com.android.externalstorage.documents/document/primary%3ADownload"

// SAF-диалог создания файла, сразу открытый в папке Downloads
class CreateJpeg :
    ActivityResultContracts.CreateDocument("image/jpeg") {
    override fun createIntent(c: Context, input: String): Intent =
        super.createIntent(c, input).putExtra(
            DocumentsContract.EXTRA_INITIAL_URI,
            Uri.parse(DOWNLOADS_URI)
        )
}

@Composable
fun PhotoDetailScreen(vm: PhotosViewModel, id: String, onBack: () -> Unit) {
    val state by vm.detail.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    var toSave by remember { mutableStateOf<Photo?>(null) }
    val saver = rememberLauncherForActivityResult(CreateJpeg()) { uri ->
        val photo = toSave
        if (uri != null && photo != null) vm.saveTo(photo, uri)
    }
    Scaffold(
        topBar = { AppBar("Фото #$id", onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        StateContent(state, onRetry = { vm.loadDetail(id) }, modifier = Modifier.padding(padding)) { photo ->
            Column(
                Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
            ) {
                AsyncImage(
                    model = photo.downloadUrl,
                    contentDescription = photo.author,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                        .aspectRatio(photo.width.toFloat() / photo.height)
                )
                Spacer(Modifier.height(16.dp))
                Text("Автор: ${photo.author}", style = MaterialTheme.typography.titleLarge)
                Text("Размер: ${photo.width} × ${photo.height}")
                Text("Ссылка: ${photo.pageUrl}", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        toSave = photo
                        saver.launch("picsum_${photo.id}.jpg")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Скачать фото") }
            }
        }
    }
}
