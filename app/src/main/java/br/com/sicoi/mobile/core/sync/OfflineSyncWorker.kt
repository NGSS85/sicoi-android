package br.com.sicoi.mobile.core.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import br.com.sicoi.mobile.core.database.AppDatabase
import br.com.sicoi.mobile.core.network.SupabaseClient
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.TimeUnit

/**
 * Worker que sincroniza OS finalizadas offline com o Supabase.
 * Executado pelo WorkManager quando há conectividade de rede disponível.
 */
@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: AppDatabase
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "OfflineSyncWorker"
        const val UNIQUE_WORK_NAME = "sicoi_offline_sync"

        /**
         * Agenda o worker para rodar periodicamente com restrição de rede.
         * Usa [ExistingPeriodicWorkPolicy.KEEP] para não duplicar.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val dao = database.workOrderDao()
            val pendingOrders = dao.getPendingSync()

            if (pendingOrders.isEmpty()) {
                Log.d(TAG, "Nenhuma OS pendente de sincronização.")
                return Result.success()
            }

            Log.i(TAG, "Sincronizando ${pendingOrders.size} OS pendentes...")

            var successCount = 0
            for (order in pendingOrders) {
                try {
                    // Chama a função RPC finalize_os no Supabase
                    SupabaseClient.client.postgrest.rpc("finalize_os", buildJsonObject {
                        put("p_os_id", order.id)
                        put("p_solucao_aplicada", order.solucaoAplicada ?: "")
                        put("p_pecas_utilizadas", order.pecasUtilizadas ?: "")
                        put("p_tempo_gasto", order.tempoGasto ?: "")
                        order.assinaturaUrl?.let { put("p_assinatura_url", it) }
                        order.fotoAntesUrl?.let { put("p_foto_antes_url", it) }
                        order.fotoDepoisUrl?.let { put("p_foto_depois_url", it) }
                    })

                    dao.markSynced(order.id)
                    successCount++
                    Log.i(TAG, "OS ${order.numeroOs} sincronizada com sucesso.")
                } catch (e: Exception) {
                    Log.e(TAG, "Falha ao sincronizar OS ${order.numeroOs}: ${e.message}")
                }
            }

            // Limpa cache de OS já finalizadas e sincronizadas
            if (successCount > 0) dao.clearFinalized()

            Log.i(TAG, "Sincronização concluída: $successCount/${pendingOrders.size} OS enviadas.")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Erro crítico no worker de sync: ${e.message}")
            Result.retry()
        }
    }
}
