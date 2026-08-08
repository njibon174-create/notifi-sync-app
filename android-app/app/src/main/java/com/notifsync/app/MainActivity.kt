package com.notifsync.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notifsync.app.ui.AppRoot
import com.notifsync.app.ui.common.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val appViewModel: AppViewModel = viewModel()
                AppRoot(appViewModel = appViewModel)
            }
        }
    }
}
