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
import br.com.sicoi.mobile.data.model.*
import br.com.sicoi.mobile.data.repository.WorkOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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

    // Campos automáticos de data/hora e histórico para o solicitante
    var dateForm             by androidx.compose.runtime.mutableStateOf("")
    var timeForm             by androidx.compose.runtime.mutableStateOf("")
    var allWorkOrders        by androidx.compose.runtime.mutableStateOf<List<WorkOrder>>(emptyList())
    var loadingHistory       by androidx.compose.runtime.mutableStateOf(true)

    // Novos campos do técnico para o formulário completo
    var externalService       by androidx.compose.runtime.mutableStateOf("nao")
    var externalJustification by androidx.compose.runtime.mutableStateOf("")
    var externalCompany       by androidx.compose.runtime.mutableStateOf("")
    var externalQty           by androidx.compose.runtime.mutableStateOf("")
    var externalValue         by androidx.compose.runtime.mutableStateOf("")
    
    // Anexos carregados/adicionados
    var loadedExternalAttachments  by androidx.compose.runtime.mutableStateOf<List<AttachedFile>>(emptyList())
    var loadedPrintedOsAttachments by androidx.compose.runtime.mutableStateOf<List<AttachedFile>>(emptyList())
    var loadedPhotoAttachments     by androidx.compose.runtime.mutableStateOf<List<AttachedFile>>(emptyList())

    // Lista de materiais dinâmicos
    val materialsList = androidx.compose.runtime.mutableStateListOf<MaterialItem>()

    // Estados de pausa
    var pauseState            by androidx.compose.runtime.mutableStateOf("idle")
    var pauseReason           by androidx.compose.runtime.mutableStateOf("")

    // Apontamento final
    var descriptionExecuted   by androidx.compose.runtime.mutableStateOf("")
    var finalDate             by androidx.compose.runtime.mutableStateOf("")
    var finalHour             by androidx.compose.runtime.mutableStateOf("")
    var vistoExecutante       by androidx.compose.runtime.mutableStateOf("")

    // Mídias capturadas
    var signatureBitmap: Bitmap? = null
    var beforeBitmap: Bitmap? = null
    var afterBitmap: Bitmap? = null
    var beforeUri: Uri? = null
    var afterUri: Uri? = null

    private var currentWorkOrderId: String = ""

    private fun setupFormFromOrder(order: WorkOrder, defaultTechName: String) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val nowTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())

        // Campos comuns (solicitante)
        solicitanteForm = order.solicitante ?: ""
        equipamentoForm = order.equipamento ?: ""
        patrimonioForm = ""
        prioridadeForm = order.prioridade ?: "Normal"
        descricaoForm = order.descricaoProblema ?: ""
        dateForm = order.dataAbertura ?: today
        timeForm = nowTime

        // Campos do técnico
        externalService = "nao"
        externalJustification = ""
        externalCompany = ""
        externalQty = ""
        externalValue = ""
        loadedExternalAttachments = emptyList()
        loadedPrintedOsAttachments = emptyList()
        loadedPhotoAttachments = emptyList()
        materialsList.clear()
        pauseState = "idle"
        pauseReason = ""
        descriptionExecuted = ""
        finalDate = ""
        finalHour = ""
        vistoExecutante = order.tecnicoResponsavel ?: defaultTechName

        // Parse do JSON da solucao_aplicada
        order.solucaoAplicada?.let { sol ->
            if (sol.startsWith("[RQ-11-DIGITAL]:")) {
                try {
                    val jsonStr = sol.removePrefix("[RQ-11-DIGITAL]:").trim()
                    val payload = Json.decodeFromString<OSExecutionPayload>(jsonStr)
                    
                    solicitanteForm = payload.responsible.ifBlank { order.solicitante ?: "" }
                    equipamentoForm = payload.equipment.ifBlank { order.equipamento ?: "" }
                    patrimonioForm = payload.equipmentNo
                    prioridadeForm = when (payload.priority) {
                        "emergency" -> "Emergência"
                        "urgent_2days" -> "Urgente"
                        else -> "Normal"
                    }
                    descricaoForm = payload.descriptionToExecute.ifBlank { order.descricaoProblema ?: "" }
                    dateForm = payload.date.ifBlank { order.dataAbertura ?: today }
                    timeForm = payload.time.ifBlank { nowTime }
                    
                    externalService = payload.externalService
                    externalJustification = payload.externalJustification
                    externalCompany = payload.externalCompany
                    externalQty = payload.externalQty
                    externalValue = payload.externalValue
                    
                    loadedExternalAttachments = payload.externalAttachments
                    loadedPrintedOsAttachments = payload.printedOsAttachments
                    loadedPhotoAttachments = payload.photoAttachments
                    
                    materialsList.clear()
                    materialsList.addAll(payload.materials)
                    
                    pauseState = payload.pauseState
                    pauseReason = payload.pauseReason
                    descriptionExecuted = payload.descriptionExecuted
                    finalDate = payload.finalDate
                    finalHour = payload.finalHour
                    vistoExecutante = payload.vistoExecutante.ifBlank { order.tecnicoResponsavel ?: defaultTechName }
                } catch (e: Exception) {
                    android.util.Log.e("OSFormVM", "Erro ao decodificar JSON solucao_aplicada: ${e.message}")
                }
            }
        }
    }

    fun fetchAllHistory(technicianName: String) {
        viewModelScope.launch {
            loadingHistory = true
            repository.fetchAllWorkOrders().fold(
                onSuccess = { list ->
                    // Filtra para manter somente as digitais (RQ-11)
                    val filtered = list.filter { it.solucaoAplicada?.contains("[RQ-11-DIGITAL]") == true }
                        .filter { item ->
                            // Mostra apenas ordens de serviço solicitadas por este usuário (technicianName)
                            val matchesSolicitante = item.solicitante?.equals(technicianName, ignoreCase = true) == true
                            var matchesJsonResponsible = false
                            item.solucaoAplicada?.let { sol ->
                                if (sol.startsWith("[RQ-11-DIGITAL]:")) {
                                    try {
                                        val jsonStr = sol.removePrefix("[RQ-11-DIGITAL]:").trim()
                                        val payload = Json.decodeFromString<OSExecutionPayload>(jsonStr)
                                        if (payload.responsible.equals(technicianName, ignoreCase = true)) {
                                            matchesJsonResponsible = true
                                        }
                                    } catch (e: Exception) {
                                        // ignore parsing errors
                                    }
                                }
                            }
                            matchesSolicitante || matchesJsonResponsible
                        }
                    allWorkOrders = filtered
                },
                onFailure = {
                    android.util.Log.e("OSFormVM", "Erro ao buscar histórico de OS: ${it.message}")
                }
            )
            loadingHistory = false
        }
    }

    fun loadWorkOrder(osId: String, technicianName: String) {
        fetchAllHistory(technicianName)
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
            setupFormFromOrder(newOrder, technicianName)
            _state.value = OSFormUiState.Loaded(newOrder)
            return
        }
        viewModelScope.launch {
            _state.value = OSFormUiState.Loading
            repository.fetchOpenOrders(technicianName).fold(
                onSuccess = { orders ->
                    val order = orders.firstOrNull { it.id == osId }
                    if (order != null) {
                        setupFormFromOrder(order, technicianName)
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
                        setupFormFromOrder(fallbackOrder, technicianName)
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
                    setupFormFromOrder(fallbackOrder, technicianName)
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
        photoBitmaps: List<Bitmap>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _state.value = OSFormUiState.Saving
            val isOnline = isNetworkAvailable()
            
            // Busca a numeração sequencial unificada (ex: 001/26, 002/26...) ao salvar
            val generatedOsNumber = if (isOnline) {
                repository.fetchNextOsNumber()
            } else {
                val yearSuffix = "/${java.text.SimpleDateFormat("yy", java.util.Locale.getDefault()).format(java.util.Date())}"
                "001$yearSuffix"
            }

            val todayDate = dateForm.ifBlank { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()) }
            val currentTime = timeForm.ifBlank { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()) }

            val finalTechnician = technicianName.ifBlank { "Não Atribuído" }

            val rq11Json = """
                {
                    "os_number": "$generatedOsNumber",
                    "date": "$todayDate",
                    "time": "$currentTime",
                    "responsible": "$solicitante",
                    "solicitante": "$solicitante",
                    "equipment": "$equipamento",
                    "equipment_no": "$patrimonioForm",
                    "priority": "${prioridade.lowercase()}",
                    "description_to_execute": "$descricaoProblema",
                    "assigned_technician": "$finalTechnician"
                }
            """.trimIndent()

            val newOrder = WorkOrder(
                id = java.util.UUID.randomUUID().toString(),
                numeroOs = generatedOsNumber,
                dataAbertura = todayDate,
                tecnicoResponsavel = finalTechnician,
                solicitante = solicitante.ifBlank { "Solicitante" },
                equipamento = equipamento,
                setor = setor,
                prioridade = prioridade.ifBlank { "Normal" },
                descricaoProblema = descricaoProblema,
                solucaoAplicada = "[RQ-11-DIGITAL]: $rq11Json",
                status = "Em Aberto"
            )

            // Upload de fotos anexadas no celular para o Supabase Storage
            val uploadedUrls = mutableListOf<String>()
            if (isOnline && photoBitmaps.isNotEmpty()) {
                photoBitmaps.forEachIndexed { index, bitmap ->
                    val fileName = "req_${newOrder.id}_${index}.png"
                    uploadBitmap(bitmap, fileName)?.let { url ->
                        uploadedUrls.add(url)
                    }
                }
            }

            repository.createWorkOrder(newOrder, isOnline, uploadedUrls).fold(
                onSuccess = {
                    _state.value = OSFormUiState.SavedOnline("Ordem de Serviço $generatedOsNumber gerada e enviada com sucesso!")
                    fetchAllHistory(technicianName)
                    onSuccess()
                },
                onFailure = {
                    _state.value = OSFormUiState.Error("Erro ao salvar formulário de O.S.")
                }
            )
        }
    }

    fun finalizeWorkOrder(
        technicianName: String,
        serviceBitmaps: List<Bitmap>,
        materialBitmaps: List<Bitmap>,
        onSuccess: () -> Unit
    ) {
        val order = (_state.value as? OSFormUiState.Loaded)?.order ?: return

        if (descriptionExecuted.isBlank() && pauseState != "active") {
            _state.value = OSFormUiState.Error("O campo 'Serviço Executado' é obrigatório para finalizar a O.S.")
            return
        }

        // Ao pausar, salva diretamente via updateWorkOrderStatus (sem precisar do formulário completo)
        if (pauseState == "active") {
            viewModelScope.launch {
                _state.value = OSFormUiState.Saving
                val isOnline = isNetworkAvailable()
                repository.updateWorkOrderStatus(
                    osId = currentWorkOrderId,
                    newStatus = "Pausada",
                    isOnline = isOnline
                ).fold(
                    onSuccess = {
                        if (isOnline) {
                            _state.value = OSFormUiState.SavedOnline("⏸ O.S. pausada com sucesso! Ela aparecerá na lista de pausadas.")
                        } else {
                            _state.value = OSFormUiState.SavedOffline("Pausada offline. Será sincronizado ao reconectar.")
                        }
                        onSuccess()
                    },
                    onFailure = {
                        _state.value = OSFormUiState.Error(it.message ?: "Erro ao pausar O.S.")
                    }
                )
            }
            return
        }

        viewModelScope.launch {
            _state.value = OSFormUiState.Saving
            val isOnline = isNetworkAvailable()

            // Upload das mídias para o Supabase Storage (somente se online)
            var sigUrl: String? = null
            var beforeUrl: String? = null
            var afterUrl: String? = null
            val uploadedServiceUrls = mutableListOf<String>()
            val uploadedMaterialUrls = mutableListOf<String>()

            if (isOnline) {
                signatureBitmap?.let { sigUrl = uploadBitmap(it, "signature_${currentWorkOrderId}.png") }
                beforeUri?.let { beforeUrl = uploadUri(it, "before_${currentWorkOrderId}.jpg") }
                afterUri?.let { afterUrl = uploadUri(it, "after_${currentWorkOrderId}.jpg") }

                serviceBitmaps.forEachIndexed { index, bitmap ->
                    val fileName = "srv_${currentWorkOrderId}_${index}_${System.currentTimeMillis()}.png"
                    uploadBitmap(bitmap, fileName)?.let { url ->
                        uploadedServiceUrls.add(url)
                    }
                }

                materialBitmaps.forEachIndexed { index, bitmap ->
                    val fileName = "mat_${currentWorkOrderId}_${index}_${System.currentTimeMillis()}.png"
                    uploadBitmap(bitmap, fileName)?.let { url ->
                        uploadedMaterialUrls.add(url)
                    }
                }
            }

            // Atualiza os anexos carregados
            val updatedPhotoAttachments = loadedPhotoAttachments.toMutableList()
            uploadedServiceUrls.forEach { url ->
                updatedPhotoAttachments.add(AttachedFile(name = "Foto Execução Mobile", url = url, path = url.substringAfterLast("/")))
            }
            beforeUrl?.let { url ->
                updatedPhotoAttachments.add(AttachedFile(name = "Foto Antes Mobile", url = url, path = url.substringAfterLast("/")))
            }
            afterUrl?.let { url ->
                updatedPhotoAttachments.add(AttachedFile(name = "Foto Depois Mobile", url = url, path = url.substringAfterLast("/")))
            }

            val updatedPrintedOsAttachments = loadedPrintedOsAttachments.toMutableList()
            uploadedMaterialUrls.forEach { url ->
                updatedPrintedOsAttachments.add(AttachedFile(name = "Foto Material Mobile", url = url, path = url.substringAfterLast("/")))
            }

            // Constrói o payload JSON completo
            val payload = OSExecutionPayload(
                osNumber = order.numeroOs ?: "",
                date = order.dataAbertura ?: "",
                time = "",
                sector = order.setor ?: "",
                responsible = solicitanteForm,
                equipment = equipamentoForm,
                equipmentNo = patrimonioForm,
                priority = when (prioridadeForm) {
                    "Emergência" -> "emergency"
                    "Urgente" -> "urgent_2days"
                    else -> "normal"
                },
                descriptionToExecute = descricaoForm,
                assignedTechnician = vistoExecutante.ifBlank { order.tecnicoResponsavel ?: technicianName },
                externalService = externalService,
                externalJustification = externalJustification,
                externalCompany = externalCompany,
                externalQty = externalQty,
                externalValue = externalValue,
                externalAttachments = loadedExternalAttachments,
                printedOsAttachments = updatedPrintedOsAttachments,
                photoAttachments = updatedPhotoAttachments,
                materials = materialsList.toList(),
                pauseState = pauseState,
                pauseReason = pauseReason,
                descriptionExecuted = descriptionExecuted,
                finalDate = if (finalDate.isBlank() && pauseState != "active") {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                } else {
                    finalDate
                },
                finalHour = if (finalHour.isBlank() && pauseState != "active") {
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                } else {
                    finalHour
                },
                vistoExecutante = vistoExecutante.ifBlank { order.tecnicoResponsavel ?: technicianName }
            )

            val jsonString = "[RQ-11-DIGITAL]: " + Json.encodeToString(payload)
            
            val status = if (payload.finalDate.isNotBlank() && pauseState != "active") {
                "Finalizada"
            } else if (pauseState == "active") {
                "Pausada"
            } else {
                "Em Execução"
            }

            repository.finalizeWorkOrder(
                osId = currentWorkOrderId,
                solucao = jsonString,
                pecas = materialsList.joinToString { "${it.qty}x ${it.description}" },
                tempo = payload.finalHour.ifBlank { tempo },
                assinaturaUrl = sigUrl ?: order.assinaturaUrl,
                fotoAntesUrl = beforeUrl ?: order.fotoAntesUrl,
                fotoDepoisUrl = afterUrl ?: order.fotoDepoisUrl,
                isOnline = isOnline,
                status = status
            ).fold(
                onSuccess = {
                    if (isOnline) {
                        _state.value = OSFormUiState.SavedOnline("Ordem de Serviço salva com sucesso!")
                    } else {
                        _state.value = OSFormUiState.SavedOffline("Salvo offline. Sincronização pendente.")
                    }
                    onSuccess()
                },
                onFailure = {
                    _state.value = OSFormUiState.Error(it.message ?: "Erro ao salvar O.S.")
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
