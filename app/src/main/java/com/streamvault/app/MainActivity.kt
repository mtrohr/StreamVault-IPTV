package com.streamvault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.streamvault.app.navigation.AppNavigation
import com.streamvault.app.ui.theme.StreamVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamVaultTheme {
                AppNavigation()
            }
        }
    }
}
