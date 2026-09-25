package br.com.sicoi.mobile.core.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import br.com.sicoi.mobile.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Gerenciador singleton da sessão do usuário autenticado no app mobile.
 * Armazena em memória e persiste em SharedPreferences para acesso rápido.
 */
object SessionManager {
    private const val TAG = "SessionManager"
    private const val PREFS_NAME = "SicoiSessionPrefs"
    private const val KEY_USER_PROFILE = "current_user_profile"

    private val json = Json { ignoreUnknownKeys = true }
    private var prefs: SharedPreferences? = null

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedJson = prefs?.getString(KEY_USER_PROFILE, null)
            if (!savedJson.isNullOrBlank()) {
                try {
                    val profile = json.decodeFromString<UserProfile>(savedJson)
                    _currentUser.value = profile
                    Log.i(TAG, "Sessão restaurada para: ${profile.fullName ?: profile.email}")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao restaurar perfil salvo: ${e.message}")
                }
            }
        }
    }

    fun setCurrentUser(profile: UserProfile?) {
        _currentUser.value = profile
        prefs?.let { sp ->
            if (profile != null) {
                try {
                    val str = json.encodeToString(profile)
                    sp.edit().putString(KEY_USER_PROFILE, str).apply()
                    Log.i(TAG, "Sessão salva com sucesso para ${profile.fullName ?: profile.email}")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao serializar perfil do usuário: ${e.message}")
                }
            } else {
                sp.edit().remove(KEY_USER_PROFILE).apply()
            }
        }
    }

    fun getCurrentUser(): UserProfile? = _currentUser.value

    fun clear() {
        _currentUser.value = null
        prefs?.edit()?.clear()?.apply()
    }

    /**
     * Verifica se o usuário autenticado tem permissão para acessar o módulo informado.
     */
    fun isUserAllowedModule(moduleId: String): Boolean {
        val user = _currentUser.value ?: return false
        return user.isModuleAllowed(moduleId)
    }

    /**
     * Verifica se o usuário/técnico tem autorização do sistema para abrir O.S.
     */
    fun canUserOpenOs(): Boolean {
        val user = _currentUser.value ?: return false
        return user.hasPermissionToOpenOs()
    }
}
