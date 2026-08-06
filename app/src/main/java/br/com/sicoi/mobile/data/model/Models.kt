package br.com.sicoi.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Reflete a tabela public.user_profiles do Supabase */
@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    @SerialName("full_name") val fullName: String? = null,
    val company: String? = null,
    val phone: String? = null,
    @SerialName("approval_status") val approvalStatus: String = "pending",
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("is_mobile_user") val isMobileUser: Boolean = true,
    @SerialName("role") val role: String = "Solicitante", // "Técnico" ou "Solicitante"
    @SerialName("pin") val pin: String = "2839",
    @SerialName("created_at") val createdAt: String? = null
)

/** Reflete a tabela public.ind_maint_technicians */
@Serializable
data class Technician(
    val id: String,
    val name: String,
    val status: String = "Ativo",
    val pin: String = "2839",
    val role: String = "Técnico"
)

/** Reflete a tabela public.ind_maint_os */
@Serializable
data class WorkOrder(
    val id: String,
    @SerialName("numero_os") val numeroOs: String? = null,
    @SerialName("data_abertura") val dataAbertura: String? = null,
    val equipamento: String? = null,
    val setor: String? = null,
    @SerialName("descricao_problema") val descricaoProblema: String? = null,
    val prioridade: String? = null,
    val status: String = "Em Aberto",
    @SerialName("tecnico_responsavel") val tecnicoResponsavel: String? = null,
    val solicitante: String? = null,
    @SerialName("solucao_aplicada") val solucaoAplicada: String? = null,
    @SerialName("pecas_utilizadas") val pecasUtilizadas: String? = null,
    @SerialName("tempo_gasto") val tempoGasto: String? = null,
    @SerialName("data_fim") val dataFim: String? = null,
    @SerialName("assinatura_url") val assinaturaUrl: String? = null,
    @SerialName("foto_antes_url") val fotoAntesUrl: String? = null,
    @SerialName("foto_depois_url") val fotoDepoisUrl: String? = null
)

/** Estado de aprovação do usuário */
enum class ApprovalStatus(val value: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    companion object {
        fun fromValue(value: String) = entries.firstOrNull { it.value == value } ?: PENDING
    }
}

@Serializable
data class AttachedFile(
    val name: String = "",
    val url: String = "",
    val path: String = ""
)

@Serializable
data class MaterialItem(
    val qty: String = "",
    val description: String = "",
    val price: String = ""
)

@Serializable
data class OSExecutionPayload(
    @SerialName("os_number") val osNumber: String = "",
    val date: String = "",
    val time: String = "",
    val sector: String = "",
    val responsible: String = "",
    val equipment: String = "",
    @SerialName("equipment_no") val equipmentNo: String = "",
    val priority: String = "",
    @SerialName("description_to_execute") val descriptionToExecute: String = "",
    @SerialName("assigned_technician") val assignedTechnician: String = "",
    @SerialName("external_service") val externalService: String = "nao",
    @SerialName("external_justification") val externalJustification: String = "",
    @SerialName("external_company") val externalCompany: String = "",
    @SerialName("external_qty") val externalQty: String = "",
    @SerialName("external_value") val externalValue: String = "",
    @SerialName("external_attachments") val externalAttachments: List<AttachedFile> = emptyList(),
    @SerialName("printed_os_attachments") val printedOsAttachments: List<AttachedFile> = emptyList(),
    @SerialName("photo_attachments") val photoAttachments: List<AttachedFile> = emptyList(),
    val materials: List<MaterialItem> = emptyList(),
    @SerialName("pause_state") val pauseState: String = "idle",
    @SerialName("pause_reason") val pauseReason: String = "",
    @SerialName("description_executed") val descriptionExecuted: String = "",
    @SerialName("final_date") val finalDate: String = "",
    @SerialName("final_hour") val finalHour: String = "",
    @SerialName("visto_executante") val vistoExecutante: String = ""
)

