package com.notifsync.app.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notifsync.app.UiState
import com.notifsync.app.data.model.LeaderboardEntryResponse
import com.notifsync.app.data.model.WheelSegment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// ── Theme colors (per spec) ──
private val AppBg = Color(0xFF0F0F14)
private val AppSurface = Color(0xFF1A1A24)
private val Accent = Color(0xFF6C63FF)
private val CoinGold = Color(0xFFF5C542)
private val Success = Color(0xFF4CAF50)
private val TextPrimary = Color(0xFFF0F0F5)
private val TextSecondary = Color(0xFF8888AA)

// 9 segment colors in order
private val SegmentColors = listOf(
    Color(0xFF6C63FF), Color(0xFFFF6584), Color(0xFF43B89C),
    Color(0xFFFFB347), Color(0xFF5C85D6), Color(0xFFFF7043),
    Color(0xFF26C6DA), Color(0xFFAB47BC), Color(0xFF78909C)
)

// Default 9-segment config (used as fallback if Supabase wheel_config missing)
private val DefaultSegments = listOf(
    WheelSegment("0", "coin", 0),
    WheelSegment("2", "coin", 2),
    WheelSegment("4", "coin", 4),
    WheelSegment("📱 Phone", "gift", 0),
    WheelSegment("5", "coin", 5),
    WheelSegment("🎧 Audio", "gift", 0),
    WheelSegment("7", "coin", 7),
    WheelSegment("🎁 Mystery", "gift", 0),
    WheelSegment("9", "coin", 9)
)

private const val COOLDOWN_MILLIS = 24L * 60L * 60L * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: UiState,
    onBack: () -> Unit,
    onSpin: (coinsEarned: Int, label: String) -> Unit,
    onOpenWhitelist: () -> Unit,
    onRedeem: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }

    val spinStatus = state.spinStatus
    val serverNowMillis = state.rewardsServerTimeMillis ?: System.currentTimeMillis()
    val remainingInitial = remember(spinStatus, serverNowMillis) {
        cooldownRemainingMillis(spinStatus?.lastSpinAt, spinStatus?.isUnlocked ?: false, serverNowMillis)
    }
    var remainingMillis by remember(spinStatus, serverNowMillis) { mutableLongStateOf(remainingInitial) }

    LaunchedEffect(remainingInitial) {
        remainingMillis = remainingInitial
        while (remainingMillis > 0) {
            delay(1000)
            remainingMillis = (remainingMillis - 1000).coerceAtLeast(0L)
        }
    }

    val spinReady = spinStatus?.isUnlocked == true ||
        spinStatus?.lastSpinAt == null || remainingMillis <= 0

    val segments = if (state.wheelSegments.isNotEmpty()) state.wheelSegments else DefaultSegments
    val currentCoins = state.leaderboardEntries.firstOrNull { it.userId == state.currentUserId }?.coins ?: 0

    var sheetVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Spin animation
    var isSpinning by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }
    var lastWinIndex by remember { mutableIntStateOf(-1) }
    var lastWinLabel by remember { mutableStateOf<String?>(null) }
    var pulseScale by remember { mutableFloatStateOf(1f) }

    Scaffold(
        containerColor = AppBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { sheetVisible = true },
                containerColor = Accent,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top bar — logo + coin balance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("N", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text("NotifiSync", color = TextSecondary, fontSize = 14.sp)
                }
                Text(
                    "🪙 $currentCoins",
                    color = CoinGold,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Wheel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                WheelCanvas(
                    segments = segments,
                    rotation = rotation.value,
                    pulseScale = pulseScale,
                    lastWinIndex = lastWinIndex,
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape)
                        .background(AppSurface)
                )
                // Top pointer (downward triangle)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 0.dp)
                        .size(0.dp)
                )
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .size(width = 28.dp, height = 22.dp)
                ) {
                    val path = Path().apply {
                        moveTo(size.width / 2f, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(path, Color(0xFFE53935))
                    drawPath(path, Color.White.copy(alpha = 0.85f), style = Stroke(width = 2f))
                }
            }

            // Spin button + cooldown text
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (spinReady) {
                    Text("Ready to spin", color = TextSecondary, fontSize = 13.sp)
                } else {
                    Text(
                        "Next spin in: ${formatDuration(remainingMillis)}",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Button(
                    onClick = {
                        if (!spinReady || isSpinning) return@Button
                        // Pre-determine the winning segment based on weights, then animate.
                        val winIndex = pickWeightedWinner(segments)
                        val targetAngle = computeTargetAngle(winIndex, segments.size, rotation.value)
                        isSpinning = true
                        scope.launch {
                            rotation.animateTo(
                                targetValue = targetAngle,
                                animationSpec = tween(durationMillis = 4500, easing = LinearOutSlowInEasing)
                            )
                            lastWinIndex = winIndex
                            lastWinLabel = segments[winIndex].label
                            // Pulse animation on winning segment
                            repeat(2) {
                                pulseScale = 1.08f
                                delay(180)
                                pulseScale = 1f
                                delay(180)
                            }
                            isSpinning = false
                            // Apply reward: credit coins
                            val winningSegment = segments[winIndex]
                            val earnedCoins = winningSegment.coinValue
                            val label = winningSegment.label
                            snackbarHostState.showSnackbar(
                                if (winningSegment.type == "gift")
                                    "🎁 You won: $label!"
                                else
                                    "🪙 +$earnedCoins coins!"
                            )
                            onSpin(earnedCoins, label)
                        }
                    },
                    enabled = spinReady && !isSpinning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (spinReady) Accent else Color(0xFF555566),
                        disabledContainerColor = Color(0xFF3A3A4A)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 48.dp, vertical = 14.dp)
                ) {
                    Text(
                        if (isSpinning) "Spinning…"
                        else if (spinReady) "SPIN"
                        else "Cooldown",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Leaderboard
            Text(
                "🏆 Leaderboard",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
            val sorted = state.leaderboardEntries.sortedByDescending { it.coins }.take(10)
            if (sorted.isEmpty()) {
                Text(
                    "No leaderboard data yet.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(sorted, key = { it.id }) { entry ->
                        LeaderboardRow(rank = sorted.indexOf(entry) + 1, entry = entry)
                    }
                }
            }

            // Error display
            state.error?.let {
                Text(it, color = Color(0xFFFF8888), fontSize = 12.sp)
            }
        }
    }

    // Bottom sheet
    if (sheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { sheetVisible = false },
            sheetState = sheetState,
            containerColor = AppSurface,
            contentColor = TextPrimary
        ) {
            BottomSheetContent(
                state = state,
                onOpenWhitelist = {
                    sheetVisible = false
                    onOpenWhitelist()
                },
                onRedeem = {
                    sheetVisible = false
                    onRedeem()
                },
                onLogout = {
                    sheetVisible = false
                    onLogout()
                },
                onClose = { sheetVisible = false }
            )
        }
    }
}

@Composable
private fun WheelCanvas(
    segments: List<WheelSegment>,
    rotation: Float,
    pulseScale: Float,
    lastWinIndex: Int,
    modifier: Modifier = Modifier
) {
    val n = segments.size.coerceAtLeast(1)
    val anglePer = 360f / n
    val winPaint = lastWinIndex
    Box(modifier = modifier.scale(pulseScale.coerceAtMost(1.05f))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val size = this.size
            val radius = size.minDimension / 2f
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val padding = 4f
            val arcRadius = radius - padding
            // Draw each segment
            segments.forEachIndexed { i, segment ->
                val startAngle = i * anglePer - 90f + rotation  // -90 to start at top
                rotate(degrees = rotation, pivot = Offset(centerX, centerY)) {
                    drawArc(
                        color = SegmentColors[i % SegmentColors.size],
                        startAngle = startAngle,
                        sweepAngle = anglePer,
                        useCenter = true,
                        topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                        size = Size(arcRadius * 2, arcRadius * 2)
                    )
                }
            }
            // Segment labels (rotated to follow arc)
            segments.forEachIndexed { i, segment ->
                val midAngle = i * anglePer + anglePer / 2f - 90f
                rotate(degrees = midAngle + rotation, pivot = Offset(centerX, centerY)) {
                    val labelRadius = arcRadius * 0.62f
                    val text = segment.label
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = (radius * 0.10f).coerceAtLeast(18f)
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        centerX,
                        centerY + labelRadius,
                        paint
                    )
                }
            }
            // Center hub circle
            drawCircle(
                color = Color(0xFF1A1A24),
                radius = radius * 0.18f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = radius * 0.06f,
                center = Offset(centerX, centerY)
            )
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: LeaderboardEntryResponse) {
    val medal = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "$rank."
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$medal ${entry.displayName}",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (rank <= 3) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                "${entry.coins} 🪙",
                color = CoinGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BottomSheetContent(
    state: UiState,
    onOpenWhitelist: () -> Unit,
    onRedeem: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppBg)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
            }
        }

        // Permissions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15151F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Permissions", color = TextPrimary, fontWeight = FontWeight.Bold)
                PermissionRow("Notification Listener", state.notificationAccessGranted)
                PermissionRow("SMS", state.smsPermissionGranted)
            }
        }

        // Sync status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15151F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Sync status", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    text = "Last synced: ${
                        if (state.lastSyncedAt != null) {
                            java.text.DateFormat.getDateTimeInstance()
                                .format(java.util.Date(state.lastSyncedAt))
                        } else "Never"
                    }",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    "Device: ${state.registeredDeviceName ?: state.deviceName}",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        Button(
            onClick = onOpenWhitelist,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Apps to Sync")
        }

        Button(
            onClick = onRedeem,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43B89C))
        ) {
            Text("Redeem Coins")
        }

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3A2A2A),
                contentColor = Color(0xFFFF8888)
            )
        ) {
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp)
        Text(
            if (granted) "✅ Granted" else "❌ Not granted",
            color = if (granted) Success else Color(0xFFFF8888),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Weighted random — segments with higher coin value have lower probability. */
private fun pickWeightedWinner(segments: List<WheelSegment>): Int {
    if (segments.isEmpty()) return 0
    val weights = segments.map { seg ->
        // Gifts get a moderate weight, coin segments: weight = 10 / (1 + coinValue)
        if (seg.type == "gift") 1.5 else (10.0 / (1.0 + seg.coinValue))
    }
    val total = weights.sum()
    var roll = Random.nextDouble() * total
    for (i in weights.indices) {
        roll -= weights[i]
        if (roll <= 0.0) return i
    }
    return segments.lastIndex
}

/** Compute target rotation so the wheel lands with `winIndex` at the top pointer (12 o'clock). */
private fun computeTargetAngle(winIndex: Int, segmentCount: Int, currentRotation: Float): Float {
    if (segmentCount <= 0) return currentRotation + 1440f
    val anglePer = 360f / segmentCount
    val segmentCenter = winIndex * anglePer + anglePer / 2f
    // We want the segment's center to be at -90° (top) → wheel should rotate so that
    // (currentRot + segmentCenter) ≡ -90 (mod 360)
    // Simplest: add enough full spins + landing rotation.
    val fullSpins = 5f * 360f
    val landingRotation = (360f - segmentCenter) % 360f
    return currentRotation + fullSpins + landingRotation - (currentRotation % 360f)
}

private fun cooldownRemainingMillis(lastSpinAt: String?, isUnlocked: Boolean, nowMillis: Long): Long {
    if (isUnlocked) return 0L
    val parsed = try {
        lastSpinAt?.let { java.time.Instant.parse(it).toEpochMilli() }
    } catch (_: java.time.format.DateTimeParseException) {
        null
    } ?: return 0L
    return (parsed + COOLDOWN_MILLIS - nowMillis).coerceAtLeast(0L)
}

private fun formatDuration(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}