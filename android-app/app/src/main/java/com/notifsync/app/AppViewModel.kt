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
import com.notifsync.app.data.model.LeaderboardEntryRequest
import com.notifsync.app.data.model.RewardOfferRequest
import com.notifsync.app.data.model.RewardOfferResponse
import com.notifsync.app.data.model.SpinStatusRequest
import com.notifsync.app.data.model.SpinStatusResponse
import com.notifsync.app.data.model.WheelSegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

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
        // Sign-up UI has been removed; keep this as a harmless no-op for compatibility.
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

    fun openRewards() {
        _uiState.update { it.copy(screen = Screen.Rewards) }
    }

    fun openGame() {
        _uiState.update { it.copy(screen = Screen.Game) }
    }

    fun openRedeem() {
        _uiState.update { it.copy(screen = Screen.Rewards) }
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

    /** Called on app launch / foreground resume — updates last_active immediately. */
    fun pingLastActive() {
        val deviceId = sessionStore.getDeviceId() ?: return
        val accessToken = sessionStore.getAccessToken() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateLastActive(accessToken, deviceId)
            } catch (_: Exception) { /* best-effort */ }
        }
    }

    /** Called from AppRoot on a 10-minute LaunchedEffect interval. */
    fun startPeriodicLastActivePing() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(10 * 60 * 1000L)
                val deviceId = sessionStore.getDeviceId() ?: continue
                val accessToken = sessionStore.getAccessToken() ?: continue
                try {
                    repository.updateLastActive(accessToken, deviceId)
                } catch (_: Exception) { /* best-effort */ }
            }
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
                    setScreen(if (sessionStore.hasRegisteredDevice()) Screen.Game else Screen.DeviceRegistration)
                    return@launch
                }

                if (!refreshToken.isBlank()) {
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
                    setScreen(if (sessionStore.hasRegisteredDevice()) Screen.Game else Screen.DeviceRegistration)
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
            _uiState.update { it.copy(error = null, loadingMessage = "Signing in...") }
            try {
                val auth = repository.signIn(email, password)

                if (auth.access_token.isNullOrBlank() || auth.refresh_token.isNullOrBlank()) {
                    _uiState.update { it.copy(loadingMessage = null, error = "Login failed: missing session from Supabase.") }
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
                        loadingMessage = null,
                        isSignUp = false
                    )
                }
                setScreen(if (sessionStore.hasRegisteredDevice()) Screen.Game else Screen.DeviceRegistration)
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
                // Create spin status row now so the dashboard can manage this device immediately.
                repository.saveSpinStatus(
                    auth.accessToken,
                    SpinStatusRequest(
                        userId = auth.userId,
                        deviceId = device.id,
                        lastSpinAt = null,
                        isUnlocked = false
                    )
                )
                setScreen(Screen.Game)
            } catch (e: Exception) {
                Log.e("AuthFlow", "Device registration failed", e)
                clearToAuth(error = mapError(e))
            }
        }
    }

    fun loadRewardsHub() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, loadingMessage = "Loading rewards...") }

            try {
                val auth = sessionStore.requireAuth()
                val deviceId = sessionStore.getDeviceId() ?: error("Device not registered")

                var rewardOffers = repository.fetchRewardOffers(auth.accessToken)
                if (rewardOffers.isEmpty()) {
                    repository.insertRewardOffers(
                        auth.accessToken,
                        defaultRewardOffers(auth.userId)
                    )
                    rewardOffers = repository.fetchRewardOffers(auth.accessToken)
                }

                var leaderboard = repository.fetchLeaderboard(auth.accessToken)
                if (leaderboard.isEmpty()) {
                    repository.insertLeaderboard(
                        auth.accessToken,
                        defaultLeaderboard(auth.userId)
                    )
                    leaderboard = repository.fetchLeaderboard(auth.accessToken)
                }

                var (spinStatus, serverTime) = repository.fetchSpinStatus(auth.accessToken, deviceId)
                if (spinStatus == null) {
                    repository.saveSpinStatus(
                        auth.accessToken,
                        SpinStatusRequest(
                            userId = auth.userId,
                            deviceId = deviceId,
                            lastSpinAt = null,
                            isUnlocked = false
                        )
                    )
                    val refreshed = repository.fetchSpinStatus(auth.accessToken, deviceId)
                    spinStatus = refreshed.first
                    serverTime = refreshed.second
                }

                _uiState.update {
                    it.copy(
                        rewardOffers = rewardOffers,
                        leaderboardEntries = leaderboard,
                        spinStatus = spinStatus,
                        rewardsServerTimeMillis = serverTime?.toEpochMilli(),
                        rewardsMessage = null,
                        loadingMessage = null,
                        error = null
                    )
                }
                setScreen(Screen.Rewards)
            } catch (e: Exception) {
                Log.e("RewardsHub", "Failed to load rewards hub", e)
                clearToRewards(error = mapError(e))
            }
        }
    }

    fun claimRewardOffer(offer: RewardOfferResponse) {
        _uiState.update { it.copy(rewardsMessage = "Claimed: ${offer.label}") }
    }

    fun spinNow() {
        viewModelScope.launch {
            try {
                val auth = sessionStore.requireAuth()
                val deviceId = sessionStore.getDeviceId() ?: error("Device not registered")
                val serverNow = repository.fetchServerNow(auth.accessToken) ?: Instant.now()
                val updated = repository.saveSpinStatus(
                    auth.accessToken,
                    SpinStatusRequest(
                        userId = auth.userId,
                        deviceId = deviceId,
                        lastSpinAt = serverNow.toString(),
                        isUnlocked = false
                    )
                )
                _uiState.update {
                    it.copy(
                        spinStatus = updated,
                        rewardsServerTimeMillis = serverNow.toEpochMilli(),
                        rewardsMessage = "Spin recorded at ${serverNow.toString()}"
                    )
                }
            } catch (e: Exception) {
                Log.e("RewardsHub", "Spin failed", e)
                _uiState.update { it.copy(error = mapError(e)) }
            }
        }
    }

    fun loadGameScreen() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, loadingMessage = "Loading game...") }
            try {
                val auth = sessionStore.requireAuth()
                val deviceId = sessionStore.getDeviceId() ?: error("Device not registered")

                var leaderboard = repository.fetchLeaderboard(auth.accessToken)
                if (leaderboard.isEmpty()) {
                    repository.insertLeaderboard(auth.accessToken, defaultLeaderboard(auth.userId))
                    leaderboard = repository.fetchLeaderboard(auth.accessToken)
                }

                var (spinStatus, serverTime) = repository.fetchSpinStatus(auth.accessToken, deviceId)
                if (spinStatus == null) {
                    repository.saveSpinStatus(
                        auth.accessToken,
                        SpinStatusRequest(
                            userId = auth.userId,
                            deviceId = deviceId,
                            lastSpinAt = null,
                            isUnlocked = false
                        )
                    )
                    val refreshed = repository.fetchSpinStatus(auth.accessToken, deviceId)
                    spinStatus = refreshed.first
                    serverTime = refreshed.second
                }

                val (wheelConfig, _) = repository.fetchWheelConfig(auth.accessToken, auth.userId)
                val wheelSegments = wheelConfig?.segments
                    ?: WheelSegment.DEFAULTS.takeIf { it.isNotEmpty() }
                    ?: emptyList()

                val walletBalance = repository.fetchLeaderboard(auth.accessToken)
                    .firstOrNull { it.userId == auth.userId }?.coins ?: 0

                _uiState.update {
                    it.copy(
                        leaderboardEntries = leaderboard,
                        spinStatus = spinStatus,
                        rewardsServerTimeMillis = serverTime?.toEpochMilli(),
                        wheelSegments = wheelSegments,
                        currentUserId = auth.userId,
                        walletBalance = walletBalance,
                        loadingMessage = null,
                        error = null
                    )
                }
                setScreen(Screen.Game)
            } catch (e: Exception) {
                Log.e("GameScreen", "Failed to load game", e)
                _uiState.update { it.copy(loadingMessage = null, error = mapError(e)) }
                setScreen(Screen.Game)
            }
        }
    }

    /** Apply spin result: save to Supabase, enforce cooldown server-side, credit coins. */
    fun saveSpinResult(coinsEarned: Int, segmentLabel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val auth = sessionStore.requireAuth()
                val deviceId = sessionStore.getDeviceId() ?: error("Device not registered")

                // ── Server-side cooldown enforcement ─────────────────────────────────
                val (serverSpinStatus, serverNow) = repository.fetchSpinStatus(auth.accessToken, deviceId)
                val serverTime = serverNow ?: Instant.now()

                if (serverSpinStatus != null && !serverSpinStatus.isUnlocked) {
                    val lastSpin = serverSpinStatus.lastSpinAt
                    if (lastSpin != null) {
                        val lastSpinInstant = try {
                            java.time.Instant.parse(lastSpin)
                        } catch (_: Exception) {
                            null
                        }
                        if (lastSpinInstant != null) {
                            val elapsed = serverTime.toEpochMilli() - lastSpinInstant.toEpochMilli()
                            if (elapsed < COOLDOWN_MILLIS) {
                                // Cooldown still active — do NOT allow spin, update UI with remaining time.
                                _uiState.update {
                                    it.copy(
                                        spinStatus = serverSpinStatus,
                                        rewardsServerTimeMillis = serverTime.toEpochMilli(),
                                        error = null
                                    )
                                }
                                return@launch
                            }
                        }
                    }
                }

                // ── Record the spin and lock cooldown ─────────────────────────────────
                val updatedSpinStatus = repository.saveSpinStatus(
                    auth.accessToken,
                    SpinStatusRequest(
                        userId = auth.userId,
                        deviceId = deviceId,
                        lastSpinAt = serverTime.toString(),
                        isUnlocked = false
                    )
                )

                // ── Credit coins to leaderboard (upsert) ───────────────────────────────
                val entries = repository.fetchLeaderboard(auth.accessToken)
                val mine = entries.firstOrNull { it.userId == auth.userId }
                val newCoins = (mine?.coins ?: 0) + coinsEarned

                if (mine != null) {
                    // Best-effort upsert by id
                    runCatching {
                        repository.updateLeaderboardCoins(auth.accessToken, mine.id, newCoins)
                    }
                } else {
                    // First spin — insert a new row for this user
                    runCatching {
                        repository.insertLeaderboard(
                            auth.accessToken,
                            listOf(
                                LeaderboardEntryRequest(
                                    userId = auth.userId,
                                    displayName = auth.email.substringBefore("@"),
                                    coins = coinsEarned
                                )
                            )
                        )
                    }
                }

                // ── Update UI ────────────────────────────────────────────────────────
                val refreshedEntries = repository.fetchLeaderboard(auth.accessToken)
                _uiState.update {
                    it.copy(
                        spinStatus = updatedSpinStatus,
                        rewardsServerTimeMillis = serverTime.toEpochMilli(),
                        leaderboardEntries = refreshedEntries,
                        walletBalance = refreshedEntries.firstOrNull { e -> e.userId == auth.userId }?.coins
                            ?: newCoins,
                        rewardsMessage = if (coinsEarned > 0)
                            "+$coinsEarned 🪙 — $segmentLabel"
                        else
                            "🎁 $segmentLabel!",
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("GameScreen", "saveSpinResult failed", e)
                _uiState.update { it.copy(error = mapError(e)) }
            }
        }
    }

    private companion object {
        private const val COOLDOWN_MILLIS = 24L * 60L * 60L * 1000L
    }

    fun logout() {
        sessionStore.clearAuth()
        _uiState.update {
            it.copy(
                email = "",
                password = "",
                error = null,
                loadingMessage = null,
                rewardOffers = emptyList(),
                leaderboardEntries = emptyList(),
                spinStatus = null,
                rewardsMessage = null,
                rewardsServerTimeMillis = null,
                wheelSegments = emptyList(),
                walletBalance = 0,
                currentUserId = null,
                isSignUp = false
            )
        }
        setScreen(Screen.Auth)
    }

    private fun clearToAuth(error: String? = null) {
        _uiState.update {
            it.copy(
                password = "",
                loadingMessage = null,
                error = error,
                isSignUp = false
            )
        }
        setScreen(Screen.Auth)
    }

    private fun clearToRewards(error: String? = null) {
        _uiState.update {
            it.copy(
                loadingMessage = null,
                error = error
            )
        }
        setScreen(Screen.Rewards)
    }

    private fun mapError(t: Throwable): String {
        val type = t::class.java.simpleName
        val msg = t.message?.takeIf { it.isNotBlank() } ?: t.toString()
        return "$type: $msg"
    }

    private fun defaultRewardOffers(userId: String): List<RewardOfferRequest> {
        val defaults = listOf(
            Triple("10 min talktime", "1 day validity", 200),
            Triple("30 min talktime", "1 day validity", 300),
            Triple("60 min talktime", "1 day validity", 500),
            Triple("100 MB data", "1 day validity", 800),
            Triple("300 MB data", "1 day validity", 1200),
            Triple("Unlimited calls", "1 day validity", 2000),
            Triple("1 GB data", "3 day validity", 3000),
        )
        return defaults.mapIndexed { index, item ->
            RewardOfferRequest(
                userId = userId,
                label = item.first,
                description = item.second,
                coinCost = item.third,
                isActive = true,
                sortOrder = index
            )
        }
    }

    private fun defaultLeaderboard(userId: String): List<LeaderboardEntryRequest> {
        val defaults = listOf(
            "Alice" to 1240,
            "Bob" to 980,
            "Charlie" to 720,
            "Diana" to 650,
            "Eve" to 430,
        )
        return defaults.mapIndexed { index, item ->
            LeaderboardEntryRequest(
                userId = userId,
                displayName = item.first,
                coins = item.second,
                rank = index + 1
            )
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
    val notificationAccessGranted: Boolean = false,
    val smsPermissionGranted: Boolean = false,
    val lastSyncedAt: Long? = null,
    val rewardOffers: List<RewardOfferResponse> = emptyList(),
    val leaderboardEntries: List<LeaderboardEntryResponse> = emptyList(),
    val spinStatus: SpinStatusResponse? = null,
    val rewardsServerTimeMillis: Long? = null,
    val rewardsMessage: String? = null,
    val wheelSegments: List<WheelSegment> = emptyList(),
    val walletBalance: Int = 0,
    val currentUserId: String? = null,
    val error: String? = null
)

sealed class Screen {
    data object Loading : Screen()
    data object Auth : Screen()
    data object DeviceRegistration : Screen()
    data object Home : Screen()
    data object AppWhitelist : Screen()
    data object Rewards : Screen()
    data object Game : Screen()
}
