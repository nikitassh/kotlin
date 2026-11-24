package ru.mirea.soshnin.network.nobel.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.mirea.soshnin.network.nobel.data.PublicNobelRepository
import ru.mirea.soshnin.network.nobel.data.ServerNobelRepository
import ru.mirea.soshnin.network.nobel.domain.*
import ru.mirea.soshnin.network.ui.UiState
import ru.mirea.soshnin.network.ui.toUserMessage

class NobelViewModel(app: Application, val ownServer: Boolean) : AndroidViewModel(app) {
    private val server = if (ownServer) ServerNobelRepository(app) else null
    private val repo: NobelRepository = server ?: PublicNobelRepository()
    private val getLaureates = GetLaureatesUseCase(repo)
    private val getDetail = GetLaureateDetailUseCase(repo)

    var year = MutableStateFlow("")
    var category = MutableStateFlow("")

    private val _list = MutableStateFlow<UiState<List<LaureateItem>>>(UiState.Loading)
    val list = _list.asStateFlow()

    private val _detail = MutableStateFlow<UiState<LaureateDetail>>(UiState.Loading)
    val detail = _detail.asStateFlow()

    // null - еще не проверили
    private val _loggedIn = MutableStateFlow<Boolean?>(if (ownServer) null else true)
    val loggedIn = _loggedIn.asStateFlow()

    private val _loginState = MutableStateFlow<UiState<Unit>?>(null)
    val loginState = _loginState.asStateFlow()

    init {
        viewModelScope.launch {
            if (server != null) _loggedIn.value = server.isLoggedIn()
            if (_loggedIn.value == true) load()
        }
    }

    private fun Throwable.message(): String =
        if (this is ClientRequestException && response.status == HttpStatusCode.Unauthorized)
            "Нужно войти заново" else toUserMessage()

    fun load() {
        _list.value = UiState.Loading
        viewModelScope.launch {
            _list.value = runCatching { getLaureates(year.value.toIntOrNull(), category.value) }
                .fold({ UiState.Success(it) }, { UiState.Error(it.message()) })
        }
    }

    private var current: LaureateItem? = null

    fun open(item: LaureateItem) {
        current = item
        _detail.value = UiState.Loading
        viewModelScope.launch {
            _detail.value = runCatching { getDetail(item) }
                .fold({ UiState.Success(it) }, { UiState.Error(it.message()) })
        }
    }

    fun reloadDetail() = current?.let(::open)

    fun login(username: String, password: String) {
        val s = server ?: return
        _loginState.value = UiState.Loading
        viewModelScope.launch {
            _loginState.value = runCatching { s.login(username, password) }.fold(
                { UiState.Success(Unit) },
                {
                    val unauthorized = it is ClientRequestException &&
                        it.response.status == HttpStatusCode.Unauthorized
                    UiState.Error(if (unauthorized) "Неверный логин или пароль" else it.toUserMessage())
                }
            )
            if (_loginState.value is UiState.Success) {
                _loggedIn.value = true
                load()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            server?.logout()
            _loginState.value = null
            _loggedIn.value = false
        }
    }

    fun toggleFavorite(detail: LaureateDetail) {
        val s = server ?: return
        viewModelScope.launch {
            runCatching { s.setFavorite(detail.item.prizeId, !detail.isFavorite) }
            open(detail.item)
        }
    }

    class Factory(private val app: Application, private val own: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = NobelViewModel(app, own) as T
    }
}
