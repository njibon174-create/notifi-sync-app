package com.notifsync.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.notifsync.app.AppViewModel
import com.notifsync.app.Screen
import com.notifsync.app.ui.apps.AppWhitelistScreen
import com.notifsync.app.ui.auth.AuthScreen
import com.notifsync.app.ui.device.DeviceSetupScreen
import com.notifsync.app.ui.home.HomeScreen
import com.notifsync.app.ui.loading.LoadingScreen

@Composable
fun AppRoot(appViewModel: AppViewModel) {
    val state by appViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                appViewModel.refreshRuntimeStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        appViewModel.refreshRuntimeStatus(context)
    }

    when (state.screen) {
        Screen.Loading -> LoadingScreen(message = state.loadingMessage)
        Screen.Auth -> AuthScreen(
            state = state,
            onEmailChange = appViewModel::updateEmail,
            onPasswordChange = appViewModel::updatePassword,
            onSubmit = appViewModel::submitAuth,
            onToggleMode = appViewModel::toggleMode
        )
        Screen.DeviceRegistration -> DeviceSetupScreen(
            state = state,
            onDeviceNameChange = appViewModel::updateDeviceName,
            onSubmit = appViewModel::registerDevice,
            onBackToLogin = appViewModel::logout
        )
        Screen.Home -> HomeScreen(
            state = state,
            onLogout = appViewModel::logout,
            onOpenWhitelist = appViewModel::openAppWhitelist,
            onRefreshStatus = { appViewModel.refreshRuntimeStatus(context) }
        )
        Screen.AppWhitelist -> AppWhitelistScreen(onBack = appViewModel::showHome)
    }
}
