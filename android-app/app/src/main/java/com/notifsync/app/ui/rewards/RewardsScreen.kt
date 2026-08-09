package com.notifsync.app.ui.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notifsync.app.UiState
import com.notifsync.app.data.model.LeaderboardEntryResponse
import com.notifsync.app.data.model.RewardOfferResponse
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale

private const val COOLDOWN_MILLIS = 24L * 60L * 60L * 1000L

@Composable
fun RewardsScreen(
    state: UiState,
    onBack: () -> Unit,
    onSpin: () -> Unit,
    onClaimOffer: (RewardOfferResponse) -> Unit,
    onRefresh: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onRefresh()
    }

    val spinStatus = state.spinStatus
    val serverNowMillis = state.rewardsServerTimeMillis ?: System.currentTimeMillis()
    val remainingInitial = remember(spinStatus, serverNowMillis) {
        cooldownRemainingMillis(spinStatus?.last_spin_at, spinStatus?.is_unlocked ?: false, serverNowMillis)
    }
    var remainingMillis by remember(spinStatus, serverNowMillis) { mutableLongStateOf(remainingInitial) }

    LaunchedEffect(remainingInitial) {
        remainingMillis = remainingInitial
        while (remainingMillis > 0) {
            delay(1000)
            remainingMillis = (remainingMillis - 1000).coerceAtLeast(0)
        }
    }

    val spinReady = spinStatus?.is_unlocked == true || spinStatus?.last_spin_at == null || remainingMillis <= 0

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Rewards Hub", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
            Button(onClick = onBack) { Text("Back") }
        }

        state.error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(text = it, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        state.rewardsMessage?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(text = it, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(160.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Spin Wheel", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 22.sp)
                }
                Text(text = if (spinReady) "Ready to spin" else "Next spin in: ${formatDuration(remainingMillis)}")
                Button(
                    onClick = onSpin,
                    enabled = spinReady,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (spinReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(if (spinReady) "Spin now" else "Cooldown active")
                }
                Text(
                    text = "Cooldown uses the server timestamp from Supabase so it doesn't depend on the phone clock.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        SectionTitle(title = "Reward offers", subtitle = "Active offers fetched from Supabase")
        state.rewardOffers.ifEmpty {
            listOf<RewardOfferResponse>()
        }.forEach { offer ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = offer.label, style = MaterialTheme.typography.titleMedium)
                    if (!offer.description.isNullOrBlank()) {
                        Text(text = offer.description!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = "${offer.coinCost} coins")
                    Button(onClick = { onClaimOffer(offer) }) {
                        Text("Claim")
                    }
                }
            }
        }

        if (state.rewardOffers.isEmpty()) {
            Text(text = "No active offers available yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(4.dp))
        SectionTitle(title = "Leaderboard", subtitle = "Top 10 entries sorted by coins")
        state.leaderboardEntries
            .sortedByDescending { it.coins }
            .take(10)
            .forEachIndexed { index, entry ->
                LeaderboardRow(rank = index + 1, entry = entry)
            }

        if (state.leaderboardEntries.isEmpty()) {
            Text(text = "No leaderboard data yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LeaderboardRow(rank: Int, entry: LeaderboardEntryResponse) {
    val medal = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "${rank}."
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "$medal ${entry.displayName}")
            Text(text = "${entry.coins} coins")
        }
    }
}

private fun cooldownRemainingMillis(lastSpinAt: String?, isUnlocked: Boolean, nowMillis: Long): Long {
    if (isUnlocked) return 0L
    val parsed = try {
        lastSpinAt?.let { Instant.parse(it).toEpochMilli() }
    } catch (_: DateTimeParseException) {
        null
    } ?: return 0L
    return (parsed + COOLDOWN_MILLIS - nowMillis).coerceAtLeast(0L)
}

private fun formatDuration(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}
