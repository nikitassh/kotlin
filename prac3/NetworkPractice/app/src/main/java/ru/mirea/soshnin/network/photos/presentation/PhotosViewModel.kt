package ru.mirea.soshnin.network.photos.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mirea.soshnin.network.photos.data.PhotoRepositoryImpl
import ru.mirea.soshnin.network.photos.domain.*
import ru.mirea.soshnin.network.ui.UiState
import ru.mirea.soshnin.network.ui.toUserMessage

class PhotosViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PhotoRepositoryImpl()
    private val getPhotos = GetPhotosUseCase(repo)
    private val getPhoto = GetPhotoUseCase(repo)
    private val downloadPhoto = DownloadPhotoUseCase(repo)

    private val _list = MutableStateFlow<UiState<List<Photo>>>(UiState.Loading)
    val list = _list.asStateFlow()

    private val _detail = MutableStateFlow<UiState<Photo>>(UiState.Loading)
    val detail = _detail.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    init {
        loadList()
    }

    fun loadList() {
        _list.value = UiState.Loading
        viewModelScope.launch {
            _list.value = runCatching { getPhotos() }
                .fold({ UiState.Success(it) }, { UiState.Error(it.toUserMessage()) })
        }
    }

    fun loadDetail(id: String) {
        _detail.value = UiState.Loading
        viewModelScope.launch {
            _detail.value = runCatching { getPhoto(id) }
                .fold({ UiState.Success(it) }, { UiState.Error(it.toUserMessage()) })
        }
    }

    // SAF: пользователь выбрал файл, пишем в него байты картинки
    fun saveTo(photo: Photo, uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val bytes = downloadPhoto(photo)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openOutputStream(uri)!!.use { it.write(bytes) }
                }
                bytes.size
            }
            _messages.emit(
                result.fold({ "Фото сохранено (${it / 1024} КБ)" }, { "Ошибка: ${it.toUserMessage()}" })
            )
        }
    }
}
