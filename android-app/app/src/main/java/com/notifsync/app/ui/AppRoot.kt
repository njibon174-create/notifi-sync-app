package com.notifsync.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notifsync.app.AppViewModel
import com.notifsync.app.Screen
import com.notifsync.app.ui.common.AppTheme
import com.notifsync.app.ui.common.LoadingOverlay
import com.notifsync.app.ui.device.DeviceScreen
import com.notifsync.app.ui.home.HomeScreen
import com.notifsync.app.ui.login.LoginScreen

@Composable
fun AppRoot(appViewModel: AppViewModel = viewModel()) {
    val state by appViewModel.uiState.collectAsState()

    AppTheme {
        Surface(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
            when (state.screen) {
                Screen.Loading -> LoadingOverlay(state.loadingMessage ?: "Checking session...")
                Screen.Auth -> LoginScreen(
                    state = state,
                    onToggleMode = appViewModel::toggleAuthMode,
                    onEmailChange = appViewModel::updateEmail,
                    onPasswordChange = appViewModel::updatePassword,
                    onSubmit = appViewModel::submitAuth,
                    onContinue = appViewModel::checkSession
                )
                Screen.DeviceRegistration -> DeviceScreen(
                    state = state,
                    onDeviceNameChange = appViewModel::updateDeviceName,
                    onSubmit = appViewModel::submitDeviceRegistration
                )
                Screen.Home -> HomeScreen(
                    email = state.email,
                    deviceName = state.registeredDeviceName ?: state.deviceName,
                    onLogout = appViewModel::logout
                )
            }
        }
    }
}
