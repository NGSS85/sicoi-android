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
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val userId = auth.currentUserOrNull()?.id
                ?: return AuthResult.Error("Falha ao obter sessão")

            // IMPORTANTE: Aguarda 1 segundo para garantir que o token JWT seja propagado para o plugin Postgrest
            kotlinx.coroutines.delay(1000)

            // Busca o perfil e verifica o status de aprovação
            var result = postgrest["user_profiles"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
            
            // Se vier vazio, tenta mais uma vez após mais 1 segundo (fallback para lentidão de rede/estado)
            if (result.data == "[]") {
                kotlinx.coroutines.delay(1000)
                result = postgrest["user_profiles"].select { filter { eq("id", userId) } }
            }

            val jsonBody = result.data
            val profile = result.decodeList<UserProfile>().firstOrNull()


            when {
                profile == null -> AuthResult.Error("Perfil não encontrado.\nID: $userId\nResp: $jsonBody")
                profile.approvalStatus == ApprovalStatus.PENDING.value ->
                    AuthResult.Error("PENDING") // Código especial para o UI mostrar o alerta correto
                profile.approvalStatus == ApprovalStatus.REJECTED.value ->
                    AuthResult.Error("REJECTED")
                else -> AuthResult.Success(profile)
            }


        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Erro desconhecido ao fazer login")
        }
    }

    /** Realiza cadastro de novo usuário mobile */
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        company: String
    ): AuthResult<Unit> {
        return try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = kotlinx.serialization.json.buildJsonObject {
                    put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName))
                    put("company", kotlinx.serialization.json.JsonPrimitive(company))
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
