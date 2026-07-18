package com.example.studywise

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.studywise.data.ThemeMode
import com.example.studywise.data.repository.AuthRepository
import com.example.studywise.data.repository.ThemePreferencesRepository
import com.example.studywise.ui.components.StatusBarProtection
import com.example.studywise.ui.components.BottomBarProtection
import com.example.studywise.ui.navigation.NavigationRoot
import com.example.studywise.ui.navigation.parseEmailVerificationParams
import com.example.studywise.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository

    @Inject
    lateinit var authRepository: AuthRepository

    private var navigateToTabsAfterVerification by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Appwrite.init(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.BLACK,
            ),
        )
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
        handleEmailVerificationIntent(intent)
        setContent {
            val themeMode by themePreferencesRepository.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            AppTheme(darkTheme = darkTheme) {
                NavigationRoot(
                    navigateToTabsAfterVerification = navigateToTabsAfterVerification,
                    onNavigatedToTabsAfterVerification = {
                        navigateToTabsAfterVerification = false
                    }
                )
                StatusBarProtection()
                BottomBarProtection()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleEmailVerificationIntent(intent)
    }

    private fun handleEmailVerificationIntent(intent: Intent?) {
        val params = intent.parseEmailVerificationParams() ?: return
        lifecycleScope.launch {
            try {
                authRepository.confirmVerification(
                    userId = params.userId,
                    secret = params.secret
                )
                navigateToTabsAfterVerification = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to confirm email verification", e)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
