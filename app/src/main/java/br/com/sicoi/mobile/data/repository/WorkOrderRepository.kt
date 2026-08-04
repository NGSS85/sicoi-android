package br.com.sicoi.mobile.data.repository

import android.util.Log
import br.com.sicoi.mobile.core.database.AppDatabase
import br.com.sicoi.mobile.core.database.entity.WorkOrderEntity
import br.com.sicoi.mobile.core.network.SupabaseClient
import br.com.sicoi.mobile.data.model.Technician
import br.com.sicoi.mobile.data.model.UserProfile
import br.com.sicoi.mobile.data.model.WorkOrder
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkOrderRepository @Inject constructor(
    private val database: AppDatabase
) {

    private val postgrest get() = SupabaseClient.client.postgrest

    /** Busca OS abertas por técnico do Supabase e cacheia no Room */
    suspend fun fetchOpenOrders(technicianName: String?): Result<List<WorkOrder>> {
        return try {
            val result = postgrest.rpc(
                "get_open_os_by_technician",
                buildJsonObject {
                    put("p_technician_name", JsonPrimitive(technicianName))
                }
            ).decodeList<WorkOrder>()

            // Cacheia no Room para modo offline
            val entities = result.map { it.toEntity() }
            database.workOrderDao().upsertAll(entities)

            Result.success(result)
        } catch (e: Exception) {
            Log.e("WorkOrderRepo", "Falha ao buscar OS online, usando cache: ${e.message}")
            // Retorna do cache local como fallback
            val cached = database.workOrderDao().getPendingSync()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toWorkOrder() })
            } else {
                Result.failure(e)
            }
        }
    }

    /** OS em aberto como Flow do Room (reativo, funciona offline) */
    fun getOpenOrdersFlow(): Flow<List<WorkOrderEntity>> =
        database.workOrderDao().getOpenOrders()

    /** Busca lista de técnicos de user_profiles (Aprovados e com role Técnico ou Ambos) */
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
                        status = if (profile.approvalStatus == "approved") "Aprovado" else "Pendente",
                        pin    = profile.pin,
                        role   = profile.role
                    )
                }

            Result.success(technicians)
        } catch (e: Exception) {
            Log.e("WorkOrderRepo", "fetchTechnicians erro: ${e.message}")
            Result.success(emptyList())
        }
    }

    /**
     * Busca solicitantes aprovados diretamente de user_profiles.
     * Só aparecem quem o admin aprovou na Gestão de Acessos.
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
                        status = if (profile.approvalStatus == "approved") "Aprovado" else "Pendente",
                        pin    = profile.pin,
                        role   = profile.role
                    )
                }

            Result.success(requesters)
        } catch (e: Exception) {
            Log.e("WorkOrderRepo", "fetchRequesters erro: ${e.message}")
            Result.success(emptyList())
        }
    }

    /**
     * Cria uma nova Ordem de Serviço (Solicitação).
     * Salva no Supabase e no Room offline.
     */
    suspend fun createWorkOrder(order: WorkOrder, isOnline: Boolean = true): Result<Unit> {
        return try {
            if (isOnline) {
                try {
                    postgrest["ind_maint_os"].insert(order)
                } catch (e: Exception) {
                    Log.e("WorkOrderRepo", "Erro ao salvar no Supabase ind_maint_os: ${e.message}")
                    try {
                        postgrest["ind_maint_work_orders"].insert(order)
                    } catch (e2: Exception) {
                        Log.e("WorkOrderRepo", "Erro no fallback ind_maint_work_orders: ${e2.message}")
                    }
                }
            }
            database.workOrderDao().upsert(order.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("WorkOrderRepo", "Falha ao criar OS: ${e.message}")
            try {
                database.workOrderDao().upsert(order.toEntity())
                Result.success(Unit)
            } catch (eLocal: Exception) {
                Result.failure(eLocal)
            }
        }
    }

    /**
     * Finaliza uma OS.
     * Se online: chama a RPC do Supabase diretamente.
     * Se offline: salva no Room com [syncPending] = true.
     */
    suspend fun finalizeWorkOrder(
        osId: String,
        solucao: String,
        pecas: String,
        tempo: String,
        assinaturaUrl: String?,
        fotoAntesUrl: String?,
        fotoDepoisUrl: String?,
        isOnline: Boolean
    ): Result<Unit> {
        return if (isOnline) {
            try {
                postgrest.rpc("finalize_os", buildJsonObject {
                    put("p_os_id", JsonPrimitive(osId))
                    put("p_solucao_aplicada", JsonPrimitive(solucao))
                    put("p_pecas_utilizadas", JsonPrimitive(pecas))
                    put("p_tempo_gasto", JsonPrimitive(tempo))
                    assinaturaUrl?.let { put("p_assinatura_url", JsonPrimitive(it)) }
                    fotoAntesUrl?.let { put("p_foto_antes_url", JsonPrimitive(it)) }
                    fotoDepoisUrl?.let { put("p_foto_depois_url", JsonPrimitive(it)) }
                })
                // Marca como sincronizado no Room também
                database.workOrderDao().markSynced(osId)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("WorkOrderRepo", "Falha online, salvando offline: ${e.message}")
                saveOffline(osId, solucao, pecas, tempo, assinaturaUrl, fotoAntesUrl, fotoDepoisUrl)
            }
        } else {
            saveOffline(osId, solucao, pecas, tempo, assinaturaUrl, fotoAntesUrl, fotoDepoisUrl)
        }
    }

    private suspend fun saveOffline(
        osId: String, solucao: String, pecas: String, tempo: String,
        assinaturaUrl: String?, fotoAntesUrl: String?, fotoDepoisUrl: String?
    ): Result<Unit> {
        return try {
            database.workOrderDao().finalizeOffline(
                id = osId,
                solucao = solucao,
                pecas = pecas,
                tempo = tempo,
                assinatura = assinaturaUrl,
                fotoAntes = fotoAntesUrl,
                fotoDepois = fotoDepoisUrl
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atualiza o status de uma OS.
     */
    suspend fun updateWorkOrderStatus(
        osId: String,
        newStatus: String,
        isOnline: Boolean
    ): Result<Unit> {
        return if (isOnline) {
            try {
                postgrest["ind_maint_work_orders"].update(
                    buildJsonObject {
                        put("status", JsonPrimitive(newStatus))
                    }
                ) {
                    filter {
                        eq("id", osId)
                    }
                }
                database.workOrderDao().updateStatus(osId, newStatus, 0)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("WorkOrderRepo", "Falha online ao atualizar status, salvando offline: ${e.message}")
                saveStatusOffline(osId, newStatus)
            }
        } else {
            saveStatusOffline(osId, newStatus)
        }
    }

    private suspend fun saveStatusOffline(osId: String, newStatus: String): Result<Unit> {
        return try {
            database.workOrderDao().updateStatus(osId, newStatus, 1)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Extensões de conversão
    private fun WorkOrder.toEntity() = WorkOrderEntity(
        id = id, numeroOs = numeroOs, dataAbertura = dataAbertura,
        equipamento = equipamento, setor = setor, descricaoProblema = descricaoProblema,
        prioridade = prioridade, status = status, tecnicoResponsavel = tecnicoResponsavel,
        solicitante = solicitante, solucaoAplicada = solucaoAplicada,
        pecasUtilizadas = pecasUtilizadas, tempoGasto = tempoGasto,
        assinaturaUrl = assinaturaUrl, fotoAntesUrl = fotoAntesUrl,
        fotoDepoisUrl = fotoDepoisUrl, syncPending = false
    )

    private fun WorkOrderEntity.toWorkOrder() = WorkOrder(
        id = id, numeroOs = numeroOs, dataAbertura = dataAbertura,
        equipamento = equipamento, setor = setor, descricaoProblema = descricaoProblema,
        prioridade = prioridade, status = status, tecnicoResponsavel = tecnicoResponsavel,
        solicitante = solicitante, solucaoAplicada = solucaoAplicada,
        pecasUtilizadas = pecasUtilizadas, tempoGasto = tempoGasto
    )
}
