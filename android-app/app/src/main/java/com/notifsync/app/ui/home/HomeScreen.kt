package com.notifsync.app.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notifsync.app.UiState
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    state: UiState,
    onLogout: () -> Unit,
    onOpenWhitelist: () -> Unit,
    onRefreshStatus: () -> Unit,
) {
    val context = LocalContext.current
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { onRefreshStatus() }
    val lastSynced = remember(state.lastSyncedAt) {
        state.lastSyncedAt?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "Never"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Home", fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = "Logged in as")
        Text(text = state.email, color = MaterialTheme.colorScheme.primary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Registered Device")
                Text(text = state.registeredDeviceName ?: state.deviceName, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Text(text = "Last synced: $lastSynced")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Permissions & Access")
                StatusRow(
                    label = "Notification access",
                    granted = state.notificationAccessGranted,
                    actionLabel = if (state.notificationAccessGranted) null else "Open settings",
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                )
                Text(
                    text = "Enable notification access so the app can forward selected app notifications to your other devices.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "If the toggle is greyed out or blocked, open App info and enable \"Allow restricted settings\" first.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "SMS access lets the app forward incoming text messages to your other devices.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusRow(
                    label = "SMS permission",
                    granted = state.smsPermissionGranted,
                    actionLabel = if (state.smsPermissionGranted) null else "Grant SMS access",
                    onAction = {
                        smsPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.READ_SMS,
                                android.Manifest.permission.RECEIVE_SMS
                            )
                        )
                    }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Notification Sync")
                Text(text = "Choose which installed apps should forward notifications to the backend.")
                Button(onClick = onOpenWhitelist) {
                    Text("Select Apps to Sync")
                }
            }
        }

        Button(onClick = onLogout) {
            Text("Logout")
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    granted: Boolean,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "$label: ${if (granted) "Granted" else "Not granted"}")
        if (!granted && actionLabel != null && onAction != null) {
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
