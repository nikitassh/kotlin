package ru.mirea.soshnin.storage.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    val repo = GalleryRepository(app)

    private val _photos = MutableStateFlow<List<File>>(emptyList())
    val photos = _photos.asStateFlow()

    // одноразовые сообщения для Snackbar
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    // файл, который сейчас снимает камера
    var pending: File? = null

    init {
        refresh()
    }

    // сканирование папки: при запуске и после нового фото
    fun refresh() {
        viewModelScope.launch { _photos.value = repo.scan() }
    }

    fun onPhotoTaken(success: Boolean) {
        val file = pending ?: return
        pending = null
        if (!success) file.delete()
        refresh()
    }

    fun export(file: File) {
        viewModelScope.launch {
            val ok = runCatching { repo.exportToGallery(file) }.getOrDefault(false)
            _messages.emit(if (ok) "Фото добавлено в галерею" else "Не удалось экспортировать фото")
        }
    }
}
