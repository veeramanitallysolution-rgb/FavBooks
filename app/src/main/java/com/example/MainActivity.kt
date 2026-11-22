package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.data.model.AppThemeMode
import com.example.data.repository.ThemePreferencesStore
import com.example.notifications.ReadingReminderScheduler
import com.example.ui.screens.AuthenticatedLibraryRoot
import com.example.ui.theme.DigitalBookshelfTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        ReadingReminderScheduler.ensureNotificationChannel(this)
        setContent {
            DigitalBookshelfApp()
        }
    }
}

@Composable
fun DigitalBookshelfApp() {
    val context = LocalContext.current
    val themeStore = remember { ThemePreferencesStore(context.applicationContext) }
    val appTheme by themeStore.appThemeFlow.collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = Firebase.auth.currentUser
    )

    DigitalBookshelfTheme(appThemeMode = appTheme) {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                onAuthSuccess = {},
                themeStore = themeStore
            )
        } else {
            key(user.uid) {
                AuthenticatedLibraryRoot(
                    currentUserId = user.uid,
                    themeStore = themeStore,
                    onSignedOut = {}
                )
            }
        }
    }
}
