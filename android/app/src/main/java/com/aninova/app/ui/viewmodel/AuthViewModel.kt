package com.aninova.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aninova.app.data.model.*
import com.aninova.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    val isLoggedIn: StateFlow<Boolean> = authRepository.tokenFlow
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        fetchMe()
    }

    fun fetchMe() {
        viewModelScope.launch {
            val result = authRepository.getMe()
            if (result is Result.Success) _currentUser.value = result.data
        }
    }

    fun sendOtp(email: String, username: String = "", password: String = "") {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val r = authRepository.sendOtp(email, username, password)) {
                is Result.Success -> AuthUiState.Success("OTP sent to $email")
                is Result.Error -> AuthUiState.Error(r.message)
                else -> AuthUiState.Error("Unknown error")
            }
        }
    }

    fun register(email: String, username: String, password: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val r = authRepository.register(email, username, password, otp)) {
                is Result.Success -> {
                    _currentUser.value = r.data.user
                    AuthUiState.Success("Registered successfully")
                }
                is Result.Error -> AuthUiState.Error(r.message)
                else -> AuthUiState.Error("Unknown error")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _uiState.value = when (val r = authRepository.login(email, password)) {
                is Result.Success -> {
                    _currentUser.value = r.data.user
                    AuthUiState.Success("Login successful")
                }
                is Result.Error -> AuthUiState.Error(r.message)
                else -> AuthUiState.Error("Unknown error")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _currentUser.value = null
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
