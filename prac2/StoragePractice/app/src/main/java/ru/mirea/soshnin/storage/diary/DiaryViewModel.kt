package ru.mirea.soshnin.storage.diary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiaryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = DiaryRepository(app)

    private val _entries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    init {
        // единственное полное чтение папки
        viewModelScope.launch {
            _entries.value = repo.loadAll()
            _loading.value = false
        }
    }

    fun save(title: String, text: String, old: DiaryEntry?) {
        viewModelScope.launch {
            val saved = repo.save(title, text, old)
            _entries.update { list ->
                if (old == null) {
                    // новая запись - вручную в начало списка
                    listOf(saved) + list
                } else {
                    list.map { if (it.fileName == old.fileName) saved else it }
                }
            }
        }
    }

    fun delete(entry: DiaryEntry) {
        viewModelScope.launch {
            repo.delete(entry)
            // убираем по имени файла, без пересканирования
            _entries.update { list ->
                list.filterNot { it.fileName == entry.fileName }
            }
        }
    }
}
