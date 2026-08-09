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
import com.notifsync.app.ui.common.LoadingOverlay
import com.notifsync.app.ui.device.DeviceScreen
import com.notifsync.app.ui.home.HomeScreen
import com.notifsync.app.ui.login.LoginScreen
import com.notifsync.app.ui.rewards.RewardsScreen

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
        Screen.Loading -> LoadingOverlay(message = state.loadingMessage ?: "Loading...")
        Screen.Auth -> LoginScreen(
            state = state,
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
            state = state,
            onLogout = appViewModel::logout,
            onOpenWhitelist = appViewModel::openAppWhitelist,
            onOpenRewards = appViewModel::openRewards,
            onRefreshStatus = { appViewModel.refreshRuntimeStatus(context) }
        )
        Screen.AppWhitelist -> AppWhitelistScreen(onBack = appViewModel::showHome)
        Screen.Rewards -> RewardsScreen(
            state = state,
            onBack = appViewModel::showHome,
            onSpin = appViewModel::spinNow,
            onClaimOffer = appViewModel::claimRewardOffer,
            onRefresh = appViewModel::loadRewardsHub
        )
    }
}
