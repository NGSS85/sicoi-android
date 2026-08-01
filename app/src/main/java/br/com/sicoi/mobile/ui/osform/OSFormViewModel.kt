package br.com.sicoi.mobile.ui.osform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import br.com.sicoi.mobile.core.network.SupabaseClient
import br.com.sicoi.mobile.data.model.WorkOrder
import br.com.sicoi.mobile.data.repository.WorkOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

sealed class OSFormUiState {
    object Loading : OSFormUiState()
    data class Loaded(val order: WorkOrder) : OSFormUiState()
    object Saving : OSFormUiState()
    data class SavedOnline(val message: String) : OSFormUiState()
    data class SavedOffline(val message: String) : OSFormUiState()
    data class Error(val message: String) : OSFormUiState()
}

@HiltViewModel
class OSFormViewModel @Inject constructor(
    private val repository: WorkOrderRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<OSFormUiState>(OSFormUiState.Loading)
    val state: StateFlow<OSFormUiState> = _state.asStateFlow()

    // Campos editáveis pelo técnico
    var solucao     by androidx.compose.runtime.mutableStateOf("")
    var pecas       by androidx.compose.runtime.mutableStateOf("")
    var tempo       by androidx.compose.runtime.mutableStateOf("")

    // Campos do formulário do solicitante
    var solicitanteForm      by androidx.compose.runtime.mutableStateOf("")
    var equipamentoForm      by androidx.compose.runtime.mutableStateOf("")
    var patrimonioForm       by androidx.compose.runtime.mutableStateOf("")
    var prioridadeForm       by androidx.compose.runtime.mutableStateOf("Normal")
    var descricaoForm        by androidx.compose.runtime.mutableStateOf("")
    var tiposManutencaoForm  by androidx.compose.runtime.mutableStateOf(setOf<String>())

    // Mídias capturadas
    var signatureBitmap: Bitmap? = null
    var beforeBitmap: Bitmap? = null
    var afterBitmap: Bitmap? = null
    var beforeUri: Uri? = null
    var afterUri: Uri? = null

    private var currentWorkOrderId: String = ""

    fun loadWorkOrder(osId: String, technicianName: String) {
        currentWorkOrderId = osId
        if (osId == "new" || osId.isBlank()) {
            val newOrder = WorkOrder(
                id = "NEW-${System.currentTimeMillis()}",
                numeroOs = "OS-${(1000..9999).random()}",
                dataAbertura = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                tecnicoResponsavel = technicianName,
                solicitante = "",
                equipamento = "",
                setor = "",
                prioridade = "Normal",
                descricaoProblema = ""
            )
            _state.value = OSFormUiState.Loaded(newOrder)
            return
        }
        viewModelScope.launch {
            _state.value = OSFormUiState.Loading
            repository.fetchOpenOrders(technicianName).fold(
                onSuccess = { orders ->
                    val order = orders.firstOrNull { it.id == osId }
                    if (order != null) {
                        _state.value = OSFormUiState.Loaded(order)
                    } else {
                        val fallbackOrder = WorkOrder(
                            id = osId,
                            numeroOs = "OS-$osId",
                            dataAbertura = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                            tecnicoResponsavel = technicianName,
                            solicitante = "",
                            equipamento = "",
                            setor = "",
                            prioridade = "Normal",
                            descricaoProblema = ""
                        )
                        _state.value = OSFormUiState.Loaded(fallbackOrder)
                    }
                },
                onFailure = {
                    val fallbackOrder = WorkOrder(
                        id = osId,
                        numeroOs = "OS-$osId",
                        dataAbertura = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                        tecnicoResponsavel = technicianName,
                        solicitante = "",
                        equipamento = "",
                        setor = "",
                        prioridade = "Normal",
                        descricaoProblema = ""
                    )
                    _state.value = OSFormUiState.Loaded(fallbackOrder)
                }
            )
        }
    }

    fun createRequesterWorkOrder(
        solicitante: String,
        equipamento: String,
        setor: String,
        prioridade: String,
        descricaoProblema: String,
        technicianName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = OSFormUiState.Saving
            val isOnline = isNetworkAvailable()
            val newOsNumber = "OS-${(1000..9999).random()}"
            val todayDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val newOrder = WorkOrder(
                id = "OS-${System.currentTimeMillis()}",
                numeroOs = newOsNumber,
                dataAbertura = todayDate,
                tecnicoResponsavel = technicianName,
                solicitante = solicitante.ifBlank { "Solicitante" },
                equipamento = equipamento,
                setor = setor,
                prioridade = prioridade.ifBlank { "Normal" },
                descricaoProblema = descricaoProblema,
                status = "Em Aberto"
            )
            repository.createWorkOrder(newOrder, isOnline).fold(
                onSuccess = {
                    _state.value = OSFormUiState.SavedOnline("Formulário de Ordem de Serviço enviado com sucesso!")
                    onSuccess()
                },
                onFailure = {
                    _state.value = OSFormUiState.Error("Erro ao salvar formulário de O.S.")
                }
            )
        }
    }

    fun finalizeWorkOrder() {
        if (solucao.isBlank()) {
            _state.value = OSFormUiState.Error("O campo 'Solução Aplicada' é obrigatório.")
            return
        }

        viewModelScope.launch {
            _state.value = OSFormUiState.Saving
            val isOnline = isNetworkAvailable()

            // Upload das mídias para o Supabase Storage (somente se online)
            var sigUrl: String? = null
            var beforeUrl: String? = null
            var afterUrl: String? = null

            if (isOnline) {
                signatureBitmap?.let { sigUrl = uploadBitmap(it, "signature_${currentWorkOrderId}.png") }
                beforeUri?.let { beforeUrl = uploadUri(it, "before_${currentWorkOrderId}.jpg") }
                afterUri?.let { afterUrl = uploadUri(it, "after_${currentWorkOrderId}.jpg") }
            }

            repository.finalizeWorkOrder(
                osId = currentWorkOrderId,
                solucao = solucao,
                pecas = pecas,
                tempo = tempo,
                assinaturaUrl = sigUrl,
                fotoAntesUrl = beforeUrl,
                fotoDepoisUrl = afterUrl,
                isOnline = isOnline
            ).fold(
                onSuccess = {
                    if (isOnline) {
                        _state.value = OSFormUiState.SavedOnline("O.S. finalizada com sucesso!")
                    } else {
                        _state.value = OSFormUiState.SavedOffline(
                            "O.S. salva localmente. Será sincronizada automaticamente quando houver conexão."
                        )
                    }
                },
                onFailure = {
                    _state.value = OSFormUiState.Error(it.message ?: "Erro ao finalizar O.S.")
                }
            )
        }
    }

    fun pauseWorkOrder() {
        updateWorkOrderStatus("Pausada", "O.S. pausada com sucesso!")
    }

    fun externalWorkOrder() {
        updateWorkOrderStatus("Serviço Externo", "O.S. encaminhada para serviço externo!")
    }

    private fun updateWorkOrderStatus(newStatus: String, successMessage: String) {
        viewModelScope.launch {
            _state.value = OSFormUiState.Saving
            val isOnline = isNetworkAvailable()
            
            repository.updateWorkOrderStatus(
                osId = currentWorkOrderId,
                newStatus = newStatus,
                isOnline = isOnline
            ).fold(
                onSuccess = {
                    if (isOnline) {
                        _state.value = OSFormUiState.SavedOnline(successMessage)
                    } else {
                        _state.value = OSFormUiState.SavedOffline(
                            "Status alterado localmente. Será sincronizado automaticamente quando houver conexão."
                        )
                    }
                },
                onFailure = {
                    _state.value = OSFormUiState.Error(it.message ?: "Erro ao atualizar status da O.S.")
                }
            )
        }
    }

    /** Faz upload de um Bitmap como PNG para o bucket os-attachments */
    private suspend fun uploadBitmap(bitmap: Bitmap, fileName: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos)
                val bytes = baos.toByteArray()
                SupabaseClient.client.storage.from("os-attachments").upload(fileName, bytes) { upsert = true }
                SupabaseClient.client.storage.from("os-attachments").publicUrl(fileName)
            } catch (e: Exception) {
                android.util.Log.e("OSFormVM", "Erro ao fazer upload bitmap: ${e.message}")
                null
            }
        }

    /** Faz upload de uma URI de imagem para o bucket os-attachments */
    private suspend fun uploadUri(uri: Uri, fileName: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val bytes = inputStream.readBytes()
                inputStream.close()
                SupabaseClient.client.storage.from("os-attachments").upload(fileName, bytes) { upsert = true }
                SupabaseClient.client.storage.from("os-attachments").publicUrl(fileName)
            } catch (e: Exception) {
                android.util.Log.e("OSFormVM", "Erro ao fazer upload URI: ${e.message}")
                null
            }
        }



    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun resetState() {
        val currentOrder = (_state.value as? OSFormUiState.Loaded)?.order
        if (currentOrder != null) _state.value = OSFormUiState.Loaded(currentOrder)
    }
}
