package br.com.sicoi.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.sicoi.mobile.ui.theme.LocalThemeController
import br.com.sicoi.mobile.ui.theme.ThemeController
import br.com.sicoi.mobile.core.network.SupabaseClient
import br.com.sicoi.mobile.core.sync.OfflineSyncWorker
import br.com.sicoi.mobile.ui.theme.SicoiMobileTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Launcher de permissÃ£o de notificaÃ§Ã£o (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchAndStoreFcmToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Determina se jÃ¡ estÃ¡ logado
        val isLoggedIn = SupabaseClient.client.auth.currentUserOrNull() != null
        val startRoute = if (isLoggedIn) Routes.modules("TÃ©cnico") else Routes.LOGIN

        // Configura o WorkManager para sincronizaÃ§Ã£o offline periÃ³dica
        OfflineSyncWorker.schedule(this)

        // Solicita permissÃ£o de notificaÃ§Ã£o
        requestNotificationPermission()

        setContent {
            val prefs = remember { getSharedPreferences("SicoiThemePrefs", Context.MODE_PRIVATE) }
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("is_dark_theme", true)) }

            val themeController = remember {
                object : ThemeController {
                    override val isDarkTheme: Boolean get() = isDarkTheme
                    override fun toggleTheme() {
                        isDarkTheme = !isDarkTheme
                        prefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
                    }
                }
            }

            CompositionLocalProvider(LocalThemeController provides themeController) {
                SicoiMobileTheme {
                    SicoiNavGraph(
                        startDestination = startRoute,
                        onLogout = {
                            scope.launch {
                                try {
                                    SupabaseClient.client.auth.signOut()
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        // Firebase desativado para build local sem google-services.json
    }

    private fun fetchAndStoreFcmToken() {
        // Firebase desativado para build local sem google-services.json
    }
}