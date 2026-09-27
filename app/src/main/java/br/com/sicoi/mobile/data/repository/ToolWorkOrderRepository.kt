package br.com.sicoi.mobile.data.repository

import android.util.Log
import br.com.sicoi.mobile.core.network.SupabaseClient
import br.com.sicoi.mobile.data.model.Technician
import br.com.sicoi.mobile.data.model.ToolWorkOrder
import br.com.sicoi.mobile.data.model.UserProfile
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolWorkOrderRepository @Inject constructor() {

    private val postgrest get() = SupabaseClient.client.postgrest

    /**
     * Busca ordens de serviço de Ferramentaria abertas.
     * Retorna ordens atribuídas ao técnico ou ainda não atribuídas.
     */
    suspend fun fetchOpenOrders(technicianName: String?): Result<List<ToolWorkOrder>> {
        return try {
            // Tenta primeiro via RPC dedicada se existir
            try {
                val rpcResult = postgrest.rpc(
                    "get_open_tool_os_by_technician",
                    buildJsonObject {
                        put("p_technician_name", JsonPrimitive(technicianName ?: ""))
                    }
                ).decodeList<ToolWorkOrder>()
                if (rpcResult.isNotEmpty()) {
                    return Result.success(rpcResult)
                }
            } catch (eRpc: Exception) {
                Log.d("ToolWorkOrderRepo", "RPC get_open_tool_os_by_technician indisponível, usando consulta direta: ${eRpc.message}")
            }

            // Fallback direto via select na tabela tool_maint_os
            val allOrders = postgrest["tool_maint_os"]
                .select()
                .decodeList<ToolWorkOrder>()

            val openOrders = allOrders.filter { order ->
                val st = order.status.trim().lowercase()
                st != "concluída" && st != "concluido" && st != "finalizada" && st != "cancelada"
            }.sortedByDescending { it.createdAt ?: it.dataAbertura ?: "" }

            Result.success(openOrders)
        } catch (e: Exception) {
            Log.e("ToolWorkOrderRepo", "Erro ao buscar OS de ferramentaria: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Busca todas as ordens de serviço de Ferramentaria de um técnico (histórico completo).
     */
    suspend fun fetchAllOrdersByTechnician(technicianName: String): Result<List<ToolWorkOrder>> {
        return try {
            val allOrders = postgrest["tool_maint_os"]
                .select()
                .decodeList<ToolWorkOrder>()

            val filtered = allOrders.filter { order ->
                order.tecnicoResponsavel?.equals(technicianName, ignoreCase = true) == true
            }.sortedByDescending { it.createdAt ?: it.dataAbertura ?: "" }

            Result.success(filtered)
        } catch (e: Exception) {
            Log.e("ToolWorkOrderRepo", "Erro ao buscar histórico de ferramentaria: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Busca uma Ordem de Serviço específica por ID.
     */
    suspend fun getWorkOrderById(id: String): Result<ToolWorkOrder?> {
        return try {
            val list = postgrest["tool_maint_os"]
                .select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeList<ToolWorkOrder>()
            Result.success(list.firstOrNull())
        } catch (e: Exception) {
            Log.e("ToolWorkOrderRepo", "Erro ao buscar OS $id: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Busca técnicos aprovados no sistema SICOI.
     */
    suspend fun fetchTechnicians(): Result<List<Technician>> {
        return try {
            val profiles = postgrest["user_profiles"]
                .select {
                    filter {
                        eq("approval_status", "approved")
                    }
                }
                .decodeList<UserProfile>()

            val technicians = profiles
                .filter { it.role.equals("Técnico", ignoreCase = true) || it.role.equals("Ambos", ignoreCase = true) }
                .map { profile ->
                    Technician(
                        id     = profile.id,
                        name   = profile.fullName ?: profile.email,
                        status = "Aprovado",
                        pin    = profile.pin,
                        role   = profile.role
                    )
                }

            Result.success(technicians)
        } catch (e: Exception) {
            Log.e("ToolWorkOrderRepo", "Erro ao carregar técnicos: ${e.message}")
            Result.success(emptyList())
        }
    }

    /**
     * Busca solicitantes aprovados no sistema SICOI.
     */
    suspend fun fetchRequesters(): Result<List<Technician>> {
        return try {
            val profiles = postgrest["user_profiles"]
                .select {
                    filter {
                        eq("approval_status", "approved")
                    }
                }
                .decodeList<UserProfile>()

            val requesters = profiles
                .filter { it.role.equals("Solicitante", ignoreCase = true) || it.role.equals("Ambos", ignoreCase = true) }
                .map { profile ->
                    Technician(
                        id     = profile.id,
                        name   = profile.fullName ?: profile.email,
                        status = "Aprovado",
                        pin    = profile.pin,
                        role   = profile.role
                    )
                }

            Result.success(requesters)
        } catch (e: Exception) {
            Log.e("ToolWorkOrderRepo", "Erro ao carregar solicitantes: ${e.message}")
            Result.success(emptyList())
        }
    }

    /**
     * Cria uma nova Ordem de Serviço de Ferramentaria no Supabase.
     */
    suspend fun createWorkOrder(order: ToolWorkOrder, photoUrls: List<String> = emptyList()): Result<Unit> {
        return try {
            val nextNumber = fetchNextOsNumber()
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val photosJoined = if (photoUrls.isNotEmpty()) photoUrls.joinToString(";") else (order.fotoAntesUrl ?: "")

            val payload = buildJsonObject {
                put("numero_os", JsonPrimitive(nextNumber))
                put("codigo_ferramenta", JsonPrimitive(order.codigoFerramenta ?: ""))
                put("nome_ferramenta", JsonPrimitive(order.nomeFerramenta ?: ""))
                put("tipo_ferramenta", JsonPrimitive(order.tipoFerramenta ?: "Molde"))
                put("maquina_operacao", JsonPrimitive(order.maquinaOperacao ?: ""))
                put("setor", JsonPrimitive(order.setor ?: "Ferramentaria"))
                put("solicitante", JsonPrimitive(order.solicitante ?: ""))
                put("tecnico_responsavel", JsonPrimitive(order.tecnicoResponsavel?.takeIf { it != "Não Atribuído" } ?: ""))
                put("descricao_problema", JsonPrimitive(order.descricaoProblema ?: ""))
                put("prioridade", JsonPrimitive(order.prioridade ?: "Normal"))
                put("status", JsonPrimitive(if (order.tecnicoResponsavel.isNullOrBlank() || order.tecnicoResponsavel == "Não Atribuído") "Aberta" else "Em Execução"))
                put("data_abertura", JsonPrimitive(order.dataAbertura ?: nowIso))
                put("foto_antes_url", JsonPrimitive(photosJoined))
            }

            postgrest["tool_maint_os"].insert(payload)
            Log.i("ToolWorkOrderRepo", "OS de Ferramentaria criada com sucesso: $nextNumber")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ToolWorkOrderRepo", "Falha ao criar OS de ferramentaria: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Atualiza o status da OS (ex: Iniciar, Pausar, Reativar).
     */
    suspend fun updateWorkOrderStatus(
        osId: String,
        status: String,
        pauseReason: String? = null,
        observations: String? = null,
        technicianName: String? = null
    ): Result<Unit> {
        return try {
            val payload = buildJsonObject {
                put("status", JsonPrimitive(status))
                if (pauseReason != null) {
                    put("motivo_pausa", JsonPrimitive(pauseReason))
                }
                if (observations != null) {
                    put("observacoes_pausa", JsonPrimitive(observations))
                }
                if (technicianName != null && technicianName.isNotBlank()) {
                    put("tecnico_responsavel", JsonPrimitive(technicianName))
                }
                if (status == "Em Execução" && pauseReason == null) {
                    put("motivo_pausa", JsonPrimitive(""))
                }
            }

            postgrest["tool_maint_os"].update(payload) {
                filter { eq("id", osId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ToolWorkOrderRepo", "Erro ao atualizar status da OS $osId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Atribui o técnico responsável à OS de ferramentaria.
     */
    suspend fun assignTechnician(osId: String, technicianName: String): Result<Unit> {
        return try {
            val payload = buildJsonObject {
                put("tecnico_responsavel", JsonPrimitive(technicianName))
                put("status", JsonPrimitive("Em Execução"))
            }
            postgrest["tool_maint_os"].update(payload) {
                filter { eq("id", osId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ToolWorkOrderRepo", "Erro ao atribuir técnico: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Finaliza a Ordem de Serviço de Ferramentaria.
     */
    suspend fun finalizeWorkOrder(
        osId: String,
        solucao: String,
        pecas: String,
        tempoGasto: String,
        fotoDepoisUrl: String?,
        assinaturaUrl: String?,
        technicianName: String
    ): Result<Unit> {
        return try {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val payload = buildJsonObject {
                put("status", JsonPrimitive("Concluída"))
                put("solucao_aplicada", JsonPrimitive(solucao))
                put("pecas_utilizadas", JsonPrimitive(pecas))
                put("tempo_gasto", JsonPrimitive(tempoGasto))
                put("data_fim", JsonPrimitive(nowIso))
                put("tecnico_responsavel", JsonPrimitive(technicianName))
                if (!fotoDepoisUrl.isNullOrBlank()) {
                    put("foto_depois_url", JsonPrimitive(fotoDepoisUrl))
                }
                if (!assinaturaUrl.isNullOrBlank()) {
                    put("assinatura_url", JsonPrimitive(assinaturaUrl))
                }
            }

            postgrest["tool_maint_os"].update(payload) {
                filter { eq("id", osId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ToolWorkOrderRepo", "Erro ao finalizar OS de Ferramentaria: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Gera o próximo número sequencial da OS de Ferramentaria no formato FER-NNN/YY (ex: FER-001/26).
     */
    private suspend fun fetchNextOsNumber(): String {
        val currentYear = SimpleDateFormat("yy", Locale.getDefault()).format(Date())
        return try {
            val existing = postgrest["tool_maint_os"]
                .select()
                .decodeList<ToolWorkOrder>()

            val highestSeq = existing.mapNotNull { order ->
                val num = order.numeroOs ?: return@mapNotNull null
                val clean = num.replace("FER-", "").trim()
                if (clean.contains("/$currentYear")) {
                    clean.substringBefore("/").toIntOrNull()
                } else null
            }.maxOrNull() ?: 0

            val nextSeq = highestSeq + 1
            String.format(Locale.getDefault(), "FER-%03d/%s", nextSeq, currentYear)
        } catch (e: Exception) {
            Log.w("ToolWorkOrderRepo", "Falha ao calcular sequencial, gerando fallback: ${e.message}")
            val fallbackSeq = (1..999).random()
            String.format(Locale.getDefault(), "FER-%03d/%s", fallbackSeq, currentYear)
        }
    }
}
