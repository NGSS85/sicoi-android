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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
     * Cria uma nova Ordem de Serviço (Solicitação) compatível com o sistema web.
     *
     * O insert usa o formato [RQ-11-DIGITAL]: {...} na coluna solucao_aplicada,
     * idêntico ao que a página web (ManutencaoIndustrialRQ11) faz — assim a OS
     * aparece na aba "Ordens de Serviço em Aberto" do dashboard desktop.
     *
     * A coluna asset_id é NOT NULL, então buscamos um ativo existente antes do insert.
     */
    suspend fun createWorkOrder(order: WorkOrder, isOnline: Boolean = true, photoUrls: List<String> = emptyList()): Result<Unit> {
        // Salva no Room local (sempre, para modo offline também)
        try {
            database.workOrderDao().upsert(order.toEntity())
        } catch (eLocal: Exception) {
            Log.w("WorkOrderRepo", "Falha ao salvar no Room: ${eLocal.message}")
        }

        if (!isOnline) {
            Log.i("WorkOrderRepo", "Offline: OS salva localmente, será sincronizada depois.")
            return Result.success(Unit)
        }

        // --- Insert online no Supabase ---
        return try {
            // 1. Buscar asset_id válido (campo NOT NULL na ind_maint_os)
            val assetId = fetchFallbackAssetId()
                ?: return Result.failure(
                    Exception(
                        "Nenhum equipamento cadastrado no sistema. " +
                        "Contate o administrador para cadastrar ao menos um ativo de manutenção."
                    )
                )

            // 2. Buscar/gerar o próximo número sequencial da OS no formato NNN/YY (igual à web)
            val sequencialOsNumber = fetchNextOsNumber()

            // 3. Montar a prioridade no formato do sistema web
            val prioridadeWeb = when (order.prioridade?.lowercase()) {
                "emergência", "emergencia", "emergency" -> "emergency"
                "urgente", "urgent", "urgent_2days"    -> "urgent_2days"
                else                                    -> "normal"
            }

            // 4. Montar o status igual ao web
            val statusWeb = when (prioridadeWeb) {
                "emergency" -> "Em Execução"
                else        -> "Aberta"
            }

            // 5. Montar a lista de anexos de fotos
            val photoAttachmentsJson = kotlinx.serialization.json.buildJsonArray {
                photoUrls.forEachIndexed { i, url ->
                    add(buildJsonObject {
                        put("name", JsonPrimitive("Foto Mobile ${i + 1}"))
                        put("url", JsonPrimitive(url))
                        put("path", JsonPrimitive(url.substringAfterLast("/")))
                    })
                }
            }

            // 6. Montar JSON no formato [RQ-11-DIGITAL] — igual ao formulário web
            val rq11Json = buildJsonObject {
                put("os_number",              JsonPrimitive(sequencialOsNumber))
                put("date",                   JsonPrimitive(order.dataAbertura ?: ""))
                put("time",                   JsonPrimitive(""))
                put("sector",                 JsonPrimitive(order.setor ?: ""))
                put("responsible",            JsonPrimitive(order.solicitante ?: ""))
                put("equipment",              JsonPrimitive(order.equipamento ?: ""))
                put("equipment_no",           JsonPrimitive(""))
                put("priority",               JsonPrimitive(prioridadeWeb))
                put("description_to_execute", JsonPrimitive(order.descricaoProblema ?: ""))
                put("assigned_technician",    JsonPrimitive(order.tecnicoResponsavel ?: ""))
                put("external_service",       JsonPrimitive("nao"))
                put("maint_types",            kotlinx.serialization.json.JsonArray(emptyList()))
                put("photo_attachments",      photoAttachmentsJson)
                put("pause_state",            JsonPrimitive("idle"))
            }

            // 7. Inserir na ind_maint_os com o payload correto
            val payload = buildJsonObject {
                put("asset_id",           JsonPrimitive(assetId))
                put("descricao_problema", JsonPrimitive(order.descricaoProblema ?: "Abertura de OS pelo App Mobile."))
                put("solucao_aplicada",   JsonPrimitive("[RQ-11-DIGITAL]: $rq11Json"))
                put("tecnico_responsavel",JsonPrimitive(order.tecnicoResponsavel ?: order.solicitante ?: "Não Atribuído"))
                put("status",             JsonPrimitive(statusWeb))
                put("data_abertura",      JsonPrimitive(
                    order.dataAbertura?.let { "$it T00:00:00" }
                        ?: java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date())
                ))
            }

            postgrest["ind_maint_os"].insert(payload)
            Log.i("WorkOrderRepo", "✅ OS criada no Supabase com numero=$sequencialOsNumber, asset_id=$assetId, status=$statusWeb")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("WorkOrderRepo", "❌ Falha ao criar OS no Supabase: ${e.message}", e)
            Result.failure(Exception("Falha ao enviar OS: ${e.message}"))
        }
    }

    /**
     * Busca as ordens de serviço existentes com o prefixo [RQ-11-DIGITAL]:
     * e calcula o próximo número sequencial no formato NNN/YY (ex: 001/26) para o ano atual.
     */
    private suspend fun fetchNextOsNumber(): String {
        var maxNumber = 0
        val currentYearShort = java.text.SimpleDateFormat("yy", java.util.Locale.getDefault()).format(java.util.Date())
        val suffix = "/$currentYearShort"
        
        try {
            val results = postgrest["ind_maint_os"]
                .select {
                    filter {
                        like("solucao_aplicada", "[RQ-11-DIGITAL]:%")
                    }
                }
                .decodeList<JsonObject>()
            
            for (row in results) {
                val solucao = row["solucao_aplicada"]?.jsonPrimitive?.content ?: continue
                if (solucao.startsWith("[RQ-11-DIGITAL]: ")) {
                    try {
                        val jsonString = solucao.replace("[RQ-11-DIGITAL]: ", "")
                        val json = kotlinx.serialization.json.Json.parseToJsonElement(jsonString).jsonObject
                        val osNumber = json["os_number"]?.jsonPrimitive?.content
                        if (!osNumber.isNullOrBlank()) {
                            val parts = osNumber.split("/")
                            if (parts.isNotEmpty()) {
                                val num = parts[0].toIntOrNull()
                                if (num != null && num > maxNumber) {
                                    maxNumber = num
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore parsing errors
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WorkOrderRepo", "Erro ao buscar próximo número sequencial de OS: ${e.message}")
        }
        
        val nextNumStr = (maxNumber + 1).toString().padStart(3, '0')
        return "$nextNumStr$suffix"
    }

    /**
     * Busca qualquer asset_id válido da ind_maint_assets para usar como fallback.
     * Necessário pois asset_id é NOT NULL na ind_maint_os.
     */
    private suspend fun fetchFallbackAssetId(): String? {
        return try {
            val results = postgrest["ind_maint_assets"]
                .select()
                .decodeList<JsonObject>()
            results.firstOrNull()?.get("id")?.jsonPrimitive?.content
        } catch (e: Exception) {
            Log.e("WorkOrderRepo", "Falha ao buscar asset fallback: ${e.message}")
            null
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
