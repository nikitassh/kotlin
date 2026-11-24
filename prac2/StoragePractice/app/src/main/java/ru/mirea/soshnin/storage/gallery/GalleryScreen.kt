package ru.mirea.soshnin.storage.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import ru.mirea.soshnin.storage.ui.AppBar
import java.io.File

@Composable
fun GalleryApp(onExit: () -> Unit) {
    val vm: GalleryViewModel = viewModel()
    val context = LocalContext.current
    val photos by vm.photos.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var opened by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok -> vm.onPhotoTaken(ok) }

    fun launchCamera() {
        val file = vm.repo.newPhotoFile()
        vm.pending = file
        camera.launch(vm.repo.uriFor(file))
    }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.CAMERA] == true) launchCamera()
    }

    fun takePhoto() {
        val need = buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT <= 28) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (need.isEmpty()) launchCamera() else permission.launch(need.toTypedArray())
    }

    val file = photos.find { it.name == opened }
    if (file != null) {
        BackHandler { opened = null }
        PhotoScreen(file, snackbar, onExport = { vm.export(file) }, onBack = { opened = null })
        return
    }

    Scaffold(
        topBar = { AppBar("Задания 2–3. Галерея", onExit) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (photos.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = ::takePhoto,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Сделать фото") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (photos.isEmpty()) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("У вас пока нет фото", fontSize = 22.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = ::takePhoto) { Text("Сделать первое фото") }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(photos, key = { it.name }) { photo ->
                        PhotoCell(
                            photo,
                            onClick = { opened = photo.name },
                            onExport = { vm.export(photo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoCell(file: File, onClick: () -> Unit, onExport: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.aspectRatio(1f)) {
        AsyncImage(
            model = file,
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick)
        )
        // Задание 3: меню у каждой фотографии
        Box(Modifier.align(Alignment.TopEnd)) {
            Box(
                Modifier
                    .padding(4.dp)
                    .size(28.dp)
                    .background(Color(0x88000000), CircleShape)
                    .clickable { menu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MoreVert, "Меню", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Экспорт в галерею") },
                    onClick = {
                        menu = false
                        onExport()
                    }
                )
            }
        }
    }
}

@Composable
fun PhotoScreen(
    file: File,
    snackbar: SnackbarHostState,
    onExport: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { AppBar(file.name, onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = file,
                contentDescription = file.name,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Spacer(Modifier.height(16.dp))
            Text("Размер: ${file.length() / 1024} КБ")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text("Экспорт в галерею")
            }
        }
    }
}
