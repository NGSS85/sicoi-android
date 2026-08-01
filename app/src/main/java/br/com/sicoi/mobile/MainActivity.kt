package br.com.sicoi.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    // Launcher de permissão de notificação (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchAndStoreFcmToken()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Determina se já está logado

        val isLoggedIn = SupabaseClient.client.auth.currentUserOrNull() != null
        val startRoute = if (isLoggedIn) Routes.modules("Técnico") else Routes.LOGIN

        // Configura o WorkManager para sincronização offline periódica
        OfflineSyncWorker.schedule(this)

        // Solicita permissão de notificação
        requestNotificationPermission()

        setContent {
            SicoiMobileTheme {
                SicoiNavGraph(
                    startDestination = startRoute,
                    onLogout = { SupabaseClient.client.auth.signOut() }
                )
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
