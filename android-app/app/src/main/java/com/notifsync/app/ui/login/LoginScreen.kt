package com.notifsync.app.ui.login

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notifsync.app.UiState
import com.notifsync.app.ui.common.AppOutlinedTextField
import com.notifsync.app.ui.common.PrimaryButton

@Composable
fun LoginScreen(
    state: UiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Notification Sync", fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = "Sign in to continue", modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))

        AppOutlinedTextField(
            label = "Email",
            value = state.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            error = state.error?.takeIf { it.contains("email", ignoreCase = true) },
            placeholder = "you@example.com"
        )

        AppOutlinedTextField(
            label = "Password",
            value = state.password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            error = state.error?.takeIf { it.contains("password", ignoreCase = true) || it.contains("credential", ignoreCase = true) },
            visualTransformation = PasswordVisualTransformation(),
            placeholder = "••••••••"
        )

        if (state.error != null && !state.error.contains("email", true) && !state.error.contains("password", true)) {
            Text(text = state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
        }

        PrimaryButton(
            text = "Login",
            onClick = onSubmit,
            loading = false,
            modifier = Modifier.padding(top = 24.dp)
        )

        if (state.loadingMessage != null) {
            Text(text = state.loadingMessage!!, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
