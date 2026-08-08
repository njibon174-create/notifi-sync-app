package com.notifsync.app.ui.home

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    email: String,
    deviceName: String,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Home", fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
        Text(text = "Logged in as", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        Text(text = email, color = MaterialTheme.colorScheme.primary)

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Registered Device")
                Text(text = deviceName, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Text(text = "Stage 4 sync features will appear here later.", modifier = Modifier.padding(top = 8.dp))
            }
        }

        Button(onClick = onLogout, modifier = Modifier.padding(top = 24.dp)) {
            Text("Logout")
        }
    }
}
