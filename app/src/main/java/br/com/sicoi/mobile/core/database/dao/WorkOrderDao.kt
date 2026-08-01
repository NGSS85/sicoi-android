package br.com.sicoi.mobile.core.database.dao

import androidx.room.*
import br.com.sicoi.mobile.core.database.entity.WorkOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOrderDao {

    /** Retorna todas as OS em aberto como Flow reativo */
    @Query("SELECT * FROM work_orders_offline WHERE status = 'Em Aberto' ORDER BY updated_at_local DESC")
    fun getOpenOrders(): Flow<List<WorkOrderEntity>>

    /** Retorna OS pendentes de sincronização */
    @Query("SELECT * FROM work_orders_offline WHERE sync_pending = 1")
    suspend fun getPendingSync(): List<WorkOrderEntity>

    /** Insere ou substitui uma OS (ex: cache após fetch do Supabase) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(orders: List<WorkOrderEntity>)

    /** Insere ou substitui uma OS individual */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(order: WorkOrderEntity)

    /** Marca uma OS como finalizada offline (aguardando sync) */
    @Query("""
        UPDATE work_orders_offline
        SET status = 'Finalizada',
            solucao_aplicada = :solucao,
            pecas_utilizadas = :pecas,
            tempo_gasto = :tempo,
            assinatura_url = :assinatura,
            foto_antes_url = :fotoAntes,
            foto_depois_url = :fotoDepois,
            sync_pending = 1,
            updated_at_local = :timestamp
        WHERE id = :id
    """)
    suspend fun finalizeOffline(
        id: String,
        solucao: String,
        pecas: String,
        tempo: String,
        assinatura: String?,
        fotoAntes: String?,
        fotoDepois: String?,
        timestamp: Long = System.currentTimeMillis()
    )

    /** Marca OS como sincronizada após envio ao Supabase */
    @Query("UPDATE work_orders_offline SET sync_pending = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    /** Remove OS finalizadas e já sincronizadas (limpeza de cache) */
    @Query("DELETE FROM work_orders_offline WHERE status = 'Finalizada' AND sync_pending = 0")
    suspend fun clearFinalized()

    /** Atualiza o status de uma OS (ex: Pausar ou Externo) */
    @Query("""
        UPDATE work_orders_offline
        SET status = :status,
            sync_pending = :syncPending,
            updated_at_local = :timestamp
        WHERE id = :id
    """)
    suspend fun updateStatus(
        id: String,
        status: String,
        syncPending: Int,
        timestamp: Long = System.currentTimeMillis()
    )
}
