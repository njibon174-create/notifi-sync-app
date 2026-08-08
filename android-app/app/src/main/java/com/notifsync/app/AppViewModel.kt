package com.notifsync.app

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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

    fun openAppWhitelist() {
        _uiState.update { it.copy(screen = Screen.AppWhitelist) }
    }

    fun showHome() {
        _uiState.update { it.copy(screen = if (sessionStore.hasRegisteredDevice()) Screen.Home else Screen.DeviceRegistration) }
    }

    fun refreshRuntimeStatus(context: Context) {
        val notificationAccessGranted = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        val smsPermissionGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val lastSyncedAt = sessionStore.getLastSyncedAt()
        _uiState.update {
            it.copy(
                notificationAccessGranted = notificationAccessGranted,
                smsPermissionGranted = smsPermissionGranted,
                lastSyncedAt = lastSyncedAt.takeIf { ts -> ts > 0L }
            )
        }
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
                Log.e("AuthFlow", "Session check failed", e)
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
                Log.e("AuthFlow", "Auth submit failed", e)
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
                Log.e("AuthFlow", "Device registration failed", e)
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
        val type = t::class.java.simpleName
        val msg = t.message?.takeIf { it.isNotBlank() } ?: t.toString()
        return "$type: $msg"
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
    val notificationAccessGranted: Boolean = false,
    val smsPermissionGranted: Boolean = false,
    val lastSyncedAt: Long? = null,
    val error: String? = null
)

sealed class Screen {
    data object Loading : Screen()
    data object Auth : Screen()
    data object DeviceRegistration : Screen()
    data object Home : Screen()
    data object AppWhitelist : Screen()
}
