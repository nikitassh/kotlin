package ru.mirea.soshnin.network.auth.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.mirea.soshnin.network.auth.data.AuthRepositoryImpl
import ru.mirea.soshnin.network.auth.domain.*
import ru.mirea.soshnin.network.ui.UiState
import ru.mirea.soshnin.network.ui.toUserMessage

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepositoryImpl(app)
    private val loginUseCase = LoginUseCase(repo)
    private val getUsers = GetUsersUseCase(repo)
    private val getUser = GetUserUseCase(repo)
    private val logoutUseCase = LogoutUseCase(repo)

    private val _loggedIn = MutableStateFlow<Boolean?>(null)
    val loggedIn = _loggedIn.asStateFlow()

    private val _login = MutableStateFlow<UiState<User>?>(null)
    val login = _login.asStateFlow()

    private val _users = MutableStateFlow<UiState<List<User>>>(UiState.Loading)
    val users = _users.asStateFlow()

    private val _user = MutableStateFlow<UiState<User>>(UiState.Loading)
    val user = _user.asStateFlow()

    init {
        // токен уже сохранен - сразу показываем список
        viewModelScope.launch {
            val has = repo.hasToken()
            _loggedIn.value = has
            if (has) loadUsers()
        }
    }

    private fun Throwable.text() = when (this) {
        is WrongCredentialsException, is IllegalArgumentException -> message!!
        else -> toUserMessage()
    }

    fun login(username: String, password: String) {
        _login.value = UiState.Loading
        viewModelScope.launch {
            _login.value = runCatching { loginUseCase(username, password) }
                .fold({ UiState.Success(it) }, { UiState.Error(it.text()) })
            if (_login.value is UiState.Success) {
                _loggedIn.value = true
                loadUsers()
            }
        }
    }

    fun loadUsers() {
        _users.value = UiState.Loading
        viewModelScope.launch {
            _users.value = runCatching { getUsers() }
                .fold({ UiState.Success(it) }, { UiState.Error(it.text()) })
        }
    }

    fun loadUser(id: Int) {
        _user.value = UiState.Loading
        viewModelScope.launch {
            _user.value = runCatching { getUser(id) }
                .fold({ UiState.Success(it) }, { UiState.Error(it.text()) })
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _login.value = null
            _loggedIn.value = false
        }
    }
}
