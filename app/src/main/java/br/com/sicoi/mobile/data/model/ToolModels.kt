package br.com.sicoi.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de dados para Ordem de Serviço de Ferramentaria.
 * Reflete a tabela public.tool_maint_os no Supabase.
 */
@Serializable
data class ToolWorkOrder(
    val id: String = "",
    @SerialName("numero_os") val numeroOs: String? = null,
    @SerialName("codigo_ferramenta") val codigoFerramenta: String? = null,
    @SerialName("nome_ferramenta") val nomeFerramenta: String? = null,
    @SerialName("tipo_ferramenta") val tipoFerramenta: String? = null,
    @SerialName("maquina_operacao") val maquinaOperacao: String? = null,
    val setor: String? = null,
    val solicitante: String? = null,
    @SerialName("tecnico_responsavel") val tecnicoResponsavel: String? = null,
    @SerialName("descricao_problema") val descricaoProblema: String? = null,
    val prioridade: String? = "Normal",
    val status: String = "Aberta",
    @SerialName("solucao_aplicada") val solucaoAplicada: String? = null,
    @SerialName("pecas_utilizadas") val pecasUtilizadas: String? = null,
    @SerialName("tempo_gasto") val tempoGasto: String? = null,
    @SerialName("data_abertura") val dataAbertura: String? = null,
    @SerialName("data_fim") val dataFim: String? = null,
    @SerialName("foto_antes_url") val fotoAntesUrl: String? = null,
    @SerialName("foto_depois_url") val fotoDepoisUrl: String? = null,
    @SerialName("assinatura_url") val assinaturaUrl: String? = null,
    @SerialName("motivo_pausa") val motivoPausa: String? = null,
    @SerialName("observacoes_pausa") val observacoesPausa: String? = null,
    val atualizacao: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun isPaused(): Boolean {
        val st = status.trim().lowercase()
        return st == "pausada" || st == "pausado" || st == "pausa"
    }

    fun isConcluded(): Boolean {
        val st = status.trim().lowercase()
        return st == "concluída" || st == "concluida" || st == "finalizada" || st == "concluido"
    }

    fun getFullToolCode(): String {
        return codigoFerramenta?.takeIf { it.isNotBlank() } ?: "Tag não inf."
    }

    fun getFullToolName(): String {
        return nomeFerramenta?.takeIf { it.isNotBlank() } ?: "Molde / Dispositivo não inf."
    }

    fun getFullMachine(): String {
        return maquinaOperacao?.takeIf { it.isNotBlank() } ?: "Máquina não inf."
    }

    fun getFullRequester(): String {
        return solicitante?.takeIf { it.isNotBlank() } ?: "Solicitante não inf."
    }

    fun getFullTechnician(): String {
        val direct = tecnicoResponsavel?.trim()
        if (!direct.isNullOrBlank() &&
            direct.lowercase() != "não atribuído" &&
            direct.lowercase() != "nao atribuido"
        ) {
            return direct
        }
        return "Aguardando técnico"
    }

    fun getPhotoList(): List<String> {
        val list = mutableListOf<String>()
        fotoAntesUrl?.takeIf { it.isNotBlank() }?.let {
            if (it.contains(";")) {
                list.addAll(it.split(";").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })
            } else {
                list.add(it)
            }
        }
        return list
    }

    fun getAfterPhotoList(): List<String> {
        val list = mutableListOf<String>()
        fotoDepoisUrl?.takeIf { it.isNotBlank() }?.let {
            if (it.contains(";")) {
                list.addAll(it.split(";").map { s -> s.trim() }.filter { s -> s.isNotEmpty() })
            } else {
                list.add(it)
            }
        }
        return list
    }
}
