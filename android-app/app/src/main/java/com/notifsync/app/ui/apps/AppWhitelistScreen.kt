package com.notifsync.app.ui.apps

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.notifsync.app.NotificationSyncApplication
import com.notifsync.app.data.local.WhitelistStore
import com.notifsync.app.data.model.InstalledAppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppWhitelistScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val whitelistStore = remember { (appContext as NotificationSyncApplication).container.whitelistStore }
    val coroutineScope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<InstalledAppModel>>(emptyList()) }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        selectedPackages = whitelistStore.getWhitelistedPackages()
        apps = loadLaunchableApps(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Select Apps to Sync") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text("Back")
                }
            }
        )

        Text(
            text = "Only notifications from checked apps will be forwarded.",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                val checked = selectedPackages.contains(app.packageName)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val next = if (checked) selectedPackages - app.packageName else selectedPackages + app.packageName
                        selectedPackages = next
                        coroutineScope.launch {
                            whitelistStore.saveWhitelistedPackages(next)
                        }
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                            contentDescription = app.appName,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.padding(end = 12.dp)) {
                            Text(app.appName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                app.packageName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { newChecked ->
                                val next = if (newChecked) selectedPackages + app.packageName else selectedPackages - app.packageName
                                selectedPackages = next
                                coroutineScope.launch {
                                    whitelistStore.saveWhitelistedPackages(next)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private suspend fun loadLaunchableApps(context: Context): List<InstalledAppModel> = withContext(Dispatchers.Default) {
    val packageManager = context.packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    }
    packageManager.queryIntentActivities(intent, 0)
        .mapNotNull { resolveInfo ->
            val info = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
            val isSystem = (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem) return@mapNotNull null
            InstalledAppModel(
                packageName = info.packageName,
                appName = resolveInfo.loadLabel(packageManager).toString(),
                icon = resolveInfo.loadIcon(packageManager)
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.appName.lowercase() }
}
