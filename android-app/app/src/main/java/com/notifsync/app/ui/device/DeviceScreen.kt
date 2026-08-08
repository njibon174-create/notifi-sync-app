package com.notifsync.app.ui.device

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notifsync.app.UiState
import com.notifsync.app.ui.common.AppOutlinedTextField
import com.notifsync.app.ui.common.PrimaryButton

@Composable
fun DeviceScreen(
    state: UiState,
    onDeviceNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Register This Device", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = "Model detected: ${Build.MODEL}", modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))

        AppOutlinedTextField(
            label = "Device Name",
            value = state.deviceName,
            onValueChange = onDeviceNameChange,
            modifier = Modifier.fillMaxWidth(),
            error = state.error?.takeIf { it.contains("device", ignoreCase = true) || it.contains("name", ignoreCase = true) },
            placeholder = "e.g. Rahim's Redmi"
        )

        if (state.error != null && !state.error.contains("device", true) && !state.error.contains("name", true)) {
            Text(text = state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
        }

        PrimaryButton(
            text = "Register Device",
            onClick = onSubmit,
            loading = false,
            modifier = Modifier.padding(top = 24.dp)
        )

        if (state.loadingMessage != null) {
            Text(text = state.loadingMessage!!, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
