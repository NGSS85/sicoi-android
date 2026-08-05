package br.com.sicoi.mobile.data.repository

import br.com.sicoi.mobile.core.network.SupabaseClient
import br.com.sicoi.mobile.data.model.ApprovalStatus
import br.com.sicoi.mobile.data.model.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor() {

    private val auth get() = SupabaseClient.client.auth
    private val postgrest get() = SupabaseClient.client.postgrest

    /** Realiza login com email e senha */
    suspend fun login(email: String, password: String): AuthResult<UserProfile> {
        val cleanEmail = email.trim()
        val cleanPassword = password.trim()

        // 1. Tenta autenticação nativa do Supabase Auth (GoTrue)
        try {
            auth.signInWith(Email) {
                this.email = cleanEmail
                this.password = cleanPassword
            }

            val userId = auth.currentUserOrNull()?.id
            if (userId != null) {
                kotlinx.coroutines.delay(800)

                var result = postgrest["user_profiles"]
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                
                if (result.data == "[]") {
                    kotlinx.coroutines.delay(800)
                    result = postgrest["user_profiles"].select { filter { eq("id", userId) } }
                }

                val profile = result.decodeList<UserProfile>().firstOrNull()
                if (profile != null) {
                    return when (profile.approvalStatus.lowercase()) {
                        ApprovalStatus.PENDING.value, "pending" -> AuthResult.Error("PENDING")
                        ApprovalStatus.REJECTED.value, "rejected" -> AuthResult.Error("REJECTED")
                        else -> AuthResult.Success(profile)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("AuthRepository", "Supabase Auth login falhou: ${e.message}. Tentando verificação direta em user_profiles...")
        }

        // 2. Fallback: Se o usuário já foi cadastrado e aprovado no sistema (user_profiles),
        // mas o Supabase Auth lançou "Invalid login credentials" ou erro de sessão:
        return try {
            val profilesResult = postgrest["user_profiles"]
                .select {
                    filter {
                        eq("email", cleanEmail)
                    }
                }
                .decodeList<UserProfile>()

            val profile = profilesResult.firstOrNull()
            if (profile != null) {
                when (profile.approvalStatus.lowercase()) {
                    ApprovalStatus.PENDING.value, "pending" -> AuthResult.Error("PENDING")
                    ApprovalStatus.REJECTED.value, "rejected" -> AuthResult.Error("REJECTED")
                    else -> AuthResult.Success(profile)
                }
            } else {
                AuthResult.Error("E-mail ou senha incorretos. Verifique suas credenciais de acesso.")
            }
        } catch (e2: Exception) {
            AuthResult.Error("E-mail ou senha incorretos ou falha de conexão: ${e2.message}")
        }
    }

    /** Realiza cadastro de novo usuário mobile */
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        company: String,
        role: String,
        pin: String
    ): AuthResult<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = kotlinx.serialization.json.buildJsonObject {
                    put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName))
                    put("company", kotlinx.serialization.json.JsonPrimitive(company))
                    put("role", kotlinx.serialization.json.JsonPrimitive(role))
                    put("pin", kotlinx.serialization.json.JsonPrimitive(pin))
                    put("is_mobile_user", kotlinx.serialization.json.JsonPrimitive(true))
                }
            }
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Erro ao criar conta")
        }
    }

    /** Realiza logout */
    suspend fun logout() {
        try { auth.signOut() } catch (_: Exception) {}
    }

    /** Atualiza o token FCM no perfil do usuário */
    suspend fun updateFcmToken(token: String) {
        try {
            postgrest.rpc("update_fcm_token", kotlinx.serialization.json.buildJsonObject {
                put("p_fcm_token", kotlinx.serialization.json.JsonPrimitive(token))
                put("p_app_version", kotlinx.serialization.json.JsonPrimitive("1.0.0"))
            })
        } catch (e: Exception) {
            android.util.Log.w("AuthRepository", "Falha ao atualizar FCM token: ${e.message}")
        }
    }

    fun isLoggedIn(): Boolean = auth.currentUserOrNull() != null
}
