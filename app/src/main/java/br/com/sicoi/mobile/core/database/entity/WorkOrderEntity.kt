package br.com.sicoi.mobile.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade Room para armazenar OS offline.
 * Quando [syncPending] = true, a OS precisa ser enviada ao Supabase.
 */
@Entity(tableName = "work_orders_offline")
data class WorkOrderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "numero_os")        val numeroOs: String?,
    @ColumnInfo(name = "data_abertura")    val dataAbertura: String?,
    @ColumnInfo(name = "equipamento")      val equipamento: String?,
    @ColumnInfo(name = "setor")            val setor: String?,
    @ColumnInfo(name = "descricao")        val descricaoProblema: String?,
    @ColumnInfo(name = "prioridade")       val prioridade: String?,
    @ColumnInfo(name = "status")           val status: String,
    @ColumnInfo(name = "tecnico")          val tecnicoResponsavel: String?,
    @ColumnInfo(name = "solicitante")      val solicitante: String?,
    // Campos preenchidos pelo técnico
    @ColumnInfo(name = "solucao_aplicada") val solucaoAplicada: String?,
    @ColumnInfo(name = "pecas_utilizadas") val pecasUtilizadas: String?,
    @ColumnInfo(name = "tempo_gasto")      val tempoGasto: String?,
    @ColumnInfo(name = "assinatura_url")   val assinaturaUrl: String?,
    @ColumnInfo(name = "foto_antes_url")   val fotoAntesUrl: String?,
    @ColumnInfo(name = "foto_depois_url")  val fotoDepoisUrl: String?,
    // Controle de sincronização
    @ColumnInfo(name = "sync_pending")     val syncPending: Boolean = false,
    @ColumnInfo(name = "updated_at_local") val updatedAtLocal: Long = System.currentTimeMillis()
)
