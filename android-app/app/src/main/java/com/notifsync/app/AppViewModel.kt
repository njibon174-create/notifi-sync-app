package com.notifsync.app

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notifsync.app.data.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SupabaseRepository
        get() = (getApplication<NotificationSyncApplication>()).container.repository

    private val sessionStore
        get() = (getApplication<NotificationSyncApplication>()).container.sessionStore

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    private fun setScreen(screen: Screen) {
        _uiState.update { it.copy(screen = screen) }
    }

    fun toggleAuthMode() {
        _uiState.update { it.copy(isSignUp = !it.isSignUp, error = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun updateDeviceName(value: String) {
        _uiState.update { it.copy(deviceName = value, error = null) }
    }

    fun checkSession() {
        viewModelScope.launch {
            setScreen(Screen.Loading)
            try {
                val accessToken = sessionStore.getAccessToken()
                val refreshToken = sessionStore.getRefreshToken()
                val email = sessionStore.getEmail()

                if (accessToken != null && email != null && !sessionStore.isAccessTokenExpired()) {
                    val deviceName = sessionStore.getDeviceName()
                    _uiState.update {
                        it.copy(
                            email = email,
                            deviceName = deviceName ?: Build.MODEL,
                            registeredDeviceName = deviceName,
                            error = null
                        )
                    }
                    setScreen(if (sessionStore.hasRegisteredDevice()) Screen.Home else Screen.DeviceRegistration)
                    return@launch
                }

                if (!refreshToken.isNullOrBlank()) {
                    val refreshed = repository.refreshSession(refreshToken)
                    sessionStore.saveAuth(refreshed)
                    val deviceName = sessionStore.getDeviceName()
                    _uiState.update {
                        it.copy(
                            email = refreshed.user.email,
                            deviceName = deviceName ?: Build.MODEL,
                            registeredDeviceName = deviceName,
                            error = null
                        )
                    }
                    setScreen(if (sessionStore.hasRegisteredDevice()) Screen.Home else Screen.DeviceRegistration)
                    return@launch
                }

                clearToAuth()
            } catch (e: Exception) {
                clearToAuth(error = mapError(e))
            }
        }
    }

    fun submitAuth() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val isSignUp = _uiState.value.isSignUp

        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Email is required") }
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(error = "Invalid email format") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters") }
            return
        }

        viewModelScope.launch {
            setScreen(Screen.Loading)
            _uiState.update { it.copy(error = null, loadingMessage = if (isSignUp) "Creating account..." else "Signing in...") }
            try {
                val auth = if (isSignUp) {
                    repository.signUp(email, password)
                } else {
                    repository.signIn(email, password)
                }

                if (auth.access_token.isNullOrBlank() || auth.refresh_token.isNullOrBlank()) {
                    // Some Supabase projects return no session on signup pending email confirmation.
                    _uiState.update { it.copy(loadingMessage = null, error = if (isSignUp) "Signup successful. Please verify your email and log in." else null) }
                    setScreen(Screen.Auth)
                    return@launch
                }

                sessionStore.saveAuth(auth)
                _uiState.update {
                    it.copy(
                        email = auth.user.email,
                        deviceName = sessionStore.getDeviceName() ?: Build.MODEL,
                        registeredDeviceName = sessionStore.getDeviceName(),
                        password = "",
                        error = null,
                        loadingMessage = null
                    )
                }
                setScreen(if (sessionStore.hasRegisteredDevice()) Screen.Home else Screen.DeviceRegistration)
            } catch (e: Exception) {
                clearToAuth(error = mapError(e))
            }
        }
    }

    fun submitDeviceRegistration() {
        val deviceName = _uiState.value.deviceName.trim()
        if (deviceName.isBlank()) {
            _uiState.update { it.copy(error = "Device name is required") }
            return
        }

        viewModelScope.launch {
            setScreen(Screen.Loading)
            _uiState.update { it.copy(error = null, loadingMessage = "Registering device...") }
            try {
                val auth = sessionStore.requireAuth()
                val device = repository.registerDevice(auth.accessToken, deviceName, Build.MODEL)
                sessionStore.saveDevice(device)
                _uiState.update {
                    it.copy(
                        registeredDeviceName = device.device_name,
                        deviceName = device.device_name,
                        loadingMessage = null,
                        error = null
                    )
                }
                setScreen(Screen.Home)
            } catch (e: Exception) {
                clearToAuth(error = mapError(e))
            }
        }
    }

    fun logout() {
        sessionStore.clearAuth()
        _uiState.update {
            it.copy(
                email = "",
                password = "",
                error = null,
                loadingMessage = null
            )
        }
        setScreen(Screen.Auth)
    }

    private fun clearToAuth(error: String? = null) {
        _uiState.update {
            it.copy(
                password = "",
                loadingMessage = null,
                error = error
            )
        }
        setScreen(Screen.Auth)
    }

    private fun mapError(t: Throwable): String {
        val msg = t.message ?: t.toString()
        return when {
            msg.contains("invalid_credentials", ignoreCase = true) -> "Invalid email or password"
            msg.contains("email_address_invalid", ignoreCase = true) -> "Invalid email format"
            msg.contains("over_email_send_rate_limit", ignoreCase = true) -> "Email rate limit exceeded. Please try later."
            msg.contains("permission denied", ignoreCase = true) -> "Permission denied. Please log in again."
            msg.contains("network", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) -> "Network error. Please check your connection."
            else -> msg
        }
    }
}

data class UiState(
    val screen: Screen = Screen.Loading,
    val isSignUp: Boolean = false,
    val email: String = "",
    val password: String = "",
    val deviceName: String = Build.MODEL,
    val registeredDeviceName: String? = null,
    val loadingMessage: String? = null,
    val error: String? = null
)

sealed class Screen {
    data object Loading : Screen()
    data object Auth : Screen()
    data object DeviceRegistration : Screen()
    data object Home : Screen()
}
