package br.com.sicoi.mobile.ui.toolshop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import br.com.sicoi.mobile.core.network.SupabaseClient
import br.com.sicoi.mobile.data.model.Technician
import br.com.sicoi.mobile.data.model.ToolWorkOrder
import br.com.sicoi.mobile.data.repository.ToolWorkOrderRepository
import br.com.sicoi.mobile.ui.login.sicoiTextFieldColors
import br.com.sicoi.mobile.ui.osform.SignatureCanvas
import br.com.sicoi.mobile.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

sealed class ToolOSFormUiState {
    object Idle : ToolOSFormUiState()
    object Loading : ToolOSFormUiState()
    data class Loaded(val order: ToolWorkOrder) : ToolOSFormUiState()
    object Submitting : ToolOSFormUiState()
    data class Success(val message: String) : ToolOSFormUiState()
    data class Error(val message: String) : ToolOSFormUiState()
}

@HiltViewModel
class ToolOSFormViewModel @Inject constructor(
    private val repository: ToolWorkOrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ToolOSFormUiState>(ToolOSFormUiState.Idle)
    val uiState: StateFlow<ToolOSFormUiState> = _uiState.asStateFlow()

    var codigoFerramenta by mutableStateOf("")
    var nomeFerramenta by mutableStateOf("")
    var tipoFerramenta by mutableStateOf("Molde de Injeção")
    var maquinaOperacao by mutableStateOf("")
    var setor by mutableStateOf("Ferramentaria")
    var solicitante by mutableStateOf("")
    var tecnicoResponsavel by mutableStateOf("")
    var prioridade by mutableStateOf("Normal")
    var descricaoProblema by mutableStateOf("")

    // Solução e apontamentos
    var solucaoAplicada by mutableStateOf("")
    var pecasUtilizadas by mutableStateOf("")
    var tempoGasto by mutableStateOf("")

    // Motivo de Pausa
    var pauseReason by mutableStateOf("")
    var pauseObservations by mutableStateOf("")

    val techniciansList = mutableStateListOf<Technician>()

    fun initForNew(defaultRequester: String) {
        solicitante = defaultRequester
        loadTechnicians()
    }

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.value = ToolOSFormUiState.Loading
            loadTechnicians()
            repository.getWorkOrderById(orderId).fold(
                onSuccess = { order ->
                    if (order != null) {
                        codigoFerramenta = order.codigoFerramenta ?: ""
                        nomeFerramenta = order.nomeFerramenta ?: ""
                        tipoFerramenta = order.tipoFerramenta ?: "Molde de Injeção"
                        maquinaOperacao = order.maquinaOperacao ?: ""
                        setor = order.setor ?: "Ferramentaria"
                        solicitante = order.solicitante ?: ""
                        tecnicoResponsavel = order.tecnicoResponsavel ?: ""
                        prioridade = order.prioridade ?: "Normal"
                        descricaoProblema = order.descricaoProblema ?: ""
                        solucaoAplicada = order.solucaoAplicada ?: ""
                        pecasUtilizadas = order.pecasUtilizadas ?: ""
                        tempoGasto = order.tempoGasto ?: ""
                        pauseReason = order.motivoPausa ?: ""
                        pauseObservations = order.observacoesPausa ?: ""
                        _uiState.value = ToolOSFormUiState.Loaded(order)
                    } else {
                        _uiState.value = ToolOSFormUiState.Error("O.S. não encontrada")
                    }
                },
                onFailure = {
                    _uiState.value = ToolOSFormUiState.Error(it.message ?: "Erro ao carregar O.S.")
                }
            )
        }
    }

    private fun loadTechnicians() {
        viewModelScope.launch {
            repository.fetchTechnicians().onSuccess {
                techniciansList.clear()
                techniciansList.addAll(it)
            }
        }
    }

    fun submitNewOrder(photoBitmaps: List<Bitmap>, onDone: () -> Unit) {
        if (codigoFerramenta.isBlank() || nomeFerramenta.isBlank() || descricaoProblema.isBlank()) {
            _uiState.value = ToolOSFormUiState.Error("Preencha Código da Ferramenta, Nome do Molde e Descrição da Avaria.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ToolOSFormUiState.Submitting

            // Upload das fotos
            val uploadedUrls = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                photoBitmaps.forEach { bmp ->
                    uploadBitmapToStorage(bmp)?.let { uploadedUrls.add(it) }
                }
            }

            val order = ToolWorkOrder(
                codigoFerramenta = codigoFerramenta.trim(),
                nomeFerramenta = nomeFerramenta.trim(),
                tipoFerramenta = tipoFerramenta.trim(),
                maquinaOperacao = maquinaOperacao.trim(),
                setor = setor.trim(),
                solicitante = solicitante.trim(),
                tecnicoResponsavel = tecnicoResponsavel.trim(),
                prioridade = prioridade,
                descricaoProblema = descricaoProblema.trim(),
                status = if (tecnicoResponsavel.isNotBlank()) "Em Execução" else "Aberta"
            )

            repository.createWorkOrder(order, uploadedUrls).fold(
                onSuccess = {
                    _uiState.value = ToolOSFormUiState.Success("Solicitação de Ferramentaria enviada!")
                    onDone()
                },
                onFailure = {
                    _uiState.value = ToolOSFormUiState.Error(it.message ?: "Erro ao enviar O.S.")
                }
            )
        }
    }

    fun pauseOrder(orderId: String, reason: String, obs: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = ToolOSFormUiState.Submitting
            repository.updateWorkOrderStatus(orderId, "Pausada", pauseReason = reason, observations = obs).fold(
                onSuccess = {
                    _uiState.value = ToolOSFormUiState.Success("O.S. pausada com sucesso!")
                    onDone()
                },
                onFailure = {
                    _uiState.value = ToolOSFormUiState.Error(it.message ?: "Erro ao pausar O.S.")
                }
            )
        }
    }

    fun resumeOrder(orderId: String, technicianName: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = ToolOSFormUiState.Submitting
            repository.updateWorkOrderStatus(orderId, "Em Execução", pauseReason = null, technicianName = technicianName).fold(
                onSuccess = {
                    _uiState.value = ToolOSFormUiState.Success("Atendimento retomado!")
                    onDone()
                },
                onFailure = {
                    _uiState.value = ToolOSFormUiState.Error(it.message ?: "Erro ao retomar O.S.")
                }
            )
        }
    }

    fun concludeOrder(
        orderId: String,
        technicianName: String,
        afterPhotos: List<Bitmap>,
        signatureBitmap: Bitmap?,
        onDone: () -> Unit
    ) {
        if (solucaoAplicada.isBlank()) {
            _uiState.value = ToolOSFormUiState.Error("Informe a solução aplicada na ferramenta.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ToolOSFormUiState.Submitting

            var afterUrl: String? = null
            var sigUrl: String? = null

            withContext(Dispatchers.IO) {
                if (afterPhotos.isNotEmpty()) {
                    val urls = afterPhotos.mapNotNull { uploadBitmapToStorage(it) }
                    afterUrl = urls.joinToString(";")
                }
                signatureBitmap?.let { sig ->
                    sigUrl = uploadBitmapToStorage(sig, prefix = "sig_tool")
                }
            }

            repository.finalizeWorkOrder(
                osId = orderId,
                solucao = solucaoAplicada.trim(),
                pecas = pecasUtilizadas.trim(),
                tempoGasto = tempoGasto.trim(),
                fotoDepoisUrl = afterUrl,
                assinaturaUrl = sigUrl,
                technicianName = technicianName
            ).fold(
                onSuccess = {
                    _uiState.value = ToolOSFormUiState.Success("O.S. de Ferramentaria Concluída com Sucesso!")
                    onDone()
                },
                onFailure = {
                    _uiState.value = ToolOSFormUiState.Error(it.message ?: "Erro ao finalizar O.S.")
                }
            )
        }
    }

    private suspend fun uploadBitmapToStorage(bitmap: Bitmap, prefix: String = "tool"): String? {
        return try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()
            val fileName = "${prefix}_${UUID.randomUUID()}.jpg"
            SupabaseClient.client.storage.from("os-attachments").upload(fileName, bytes) { upsert = true }
            SupabaseClient.client.storage.from("os-attachments").publicUrl(fileName)
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolOSFormScreen(
    workOrderId: String,
    technicianName: String,
    isRequesterMode: Boolean,
    onNavigateBack: () -> Unit,
    onFinalized: () -> Unit,
    viewModel: ToolOSFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val photoBitmaps = remember { mutableStateListOf<Bitmap>() }
    val afterPhotoBitmaps = remember { mutableStateListOf<Bitmap>() }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showPauseDialog by remember { mutableStateOf(false) }
    var pauseReasonInput by remember { mutableStateOf("") }
    var pauseObsInput by remember { mutableStateOf("") }

    var showConcludeSheet by remember { mutableStateOf(false) }

    // Launchers de fotos
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            decodeBitmap(context, uri)?.let { photoBitmaps.add(it) }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        bmp?.let { photoBitmaps.add(it) }
    }

    val afterGalleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            decodeBitmap(context, uri)?.let { afterPhotoBitmaps.add(it) }
        }
    }

    val afterCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        bmp?.let { afterPhotoBitmaps.add(it) }
    }

    LaunchedEffect(workOrderId) {
        if (workOrderId == "new" || isRequesterMode) {
            viewModel.initForNew(technicianName)
        } else {
            viewModel.loadOrder(workOrderId)
        }
    }

    val isNew = workOrderId == "new" || isRequesterMode

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) "Nova O.S. Ferramentaria" else "Atendimento Ferramentaria",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1D19))
            )
        },
        containerColor = SicoiBackground
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val st = uiState) {
                is ToolOSFormUiState.Loading, is ToolOSFormUiState.Submitting -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SicoiWarning)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                if (st is ToolOSFormUiState.Submitting) "Salvando informações no Supabase..." else "Carregando dados da ferramenta...",
                                color = Color.White
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isNew) {
                            // ── MODO SOLICITANTE: CRIAÇÃO DE CHAMADO ──
                            Text(
                                "Dados do Dispositivo / Molde",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SicoiWarning
                            )

                            // 1. Código da Ferramenta
                            OutlinedTextField(
                                value = viewModel.codigoFerramenta,
                                onValueChange = { viewModel.codigoFerramenta = it },
                                label = { Text("Código / Tag da Ferramenta ou Molde *") },
                                placeholder = { Text("Ex: MLD-042 / EST-108") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = sicoiTextFieldColors(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = SicoiWarning) }
                            )

                            // 2. Nome da Ferramenta
                            OutlinedTextField(
                                value = viewModel.nomeFerramenta,
                                onValueChange = { viewModel.nomeFerramenta = it },
                                label = { Text("Nome / Descrição do Molde ou Dispositivo *") },
                                placeholder = { Text("Ex: Molde Injeção Tampa Frontal 8 Cav.") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = sicoiTextFieldColors(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = SicoiWarning) }
                            )

                            // 3. Tipo de Ferramenta
                            var tipoExpanded by remember { mutableStateOf(false) }
                            val toolTypes = listOf(
                                "Molde de Injeção",
                                "Estampo de Corte / Dobra",
                                "Dispositivo de Solda / Montagem",
                                "Matriz de Extrusão",
                                "Gabarito de Usinagem",
                                "Outro"
                            )
                            ExposedDropdownMenuBox(
                                expanded = tipoExpanded,
                                onExpandedChange = { tipoExpanded = !tipoExpanded }
                            ) {
                                OutlinedTextField(
                                    value = viewModel.tipoFerramenta,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tipo de Ferramenta") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    colors = sicoiTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = tipoExpanded,
                                    onDismissRequest = { tipoExpanded = false }
                                ) {
                                    toolTypes.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                viewModel.tipoFerramenta = item
                                                tipoExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 4. Máquina / Prensa onde opera
                            OutlinedTextField(
                                value = viewModel.maquinaOperacao,
                                onValueChange = { viewModel.maquinaOperacao = it },
                                label = { Text("Máquina / Prensa onde Opera") },
                                placeholder = { Text("Ex: Injetora Romi 220T / Prensa 150T") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = sicoiTextFieldColors(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = SicoiWarning) }
                            )

                            // 5. Setor
                            OutlinedTextField(
                                value = viewModel.setor,
                                onValueChange = { viewModel.setor = it },
                                label = { Text("Setor de Origem") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = sicoiTextFieldColors(),
                                singleLine = true
                            )

                            // 6. Solicitante
                            OutlinedTextField(
                                value = viewModel.solicitante,
                                onValueChange = { viewModel.solicitante = it },
                                label = { Text("Nome do Solicitante") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = sicoiTextFieldColors(),
                                singleLine = true
                            )

                            // 7. Descrição do Problema
                            OutlinedTextField(
                                value = viewModel.descricaoProblema,
                                onValueChange = { viewModel.descricaoProblema = it },
                                label = { Text("Descrição da Avaria / Reparo Solicitado *") },
                                placeholder = { Text("Descreva detalhadamente o problema (ex: pino quebrado, rebarba, travamento)") },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                colors = sicoiTextFieldColors(),
                                maxLines = 5
                            )

                            // 8. Prioridade
                            Text("Prioridade da Manutenção", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Normal", "Urgente", "Emergência").forEach { prio ->
                                    val isSelected = viewModel.prioridade.equals(prio, ignoreCase = true)
                                    val chipColor = when (prio) {
                                        "Emergência" -> SicoiError
                                        "Urgente" -> Color(0xFFF97316)
                                        else -> SicoiBlueLight
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.prioridade = prio },
                                        label = { Text(prio) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = chipColor.copy(alpha = 0.25f),
                                            selectedLabelColor = chipColor
                                        )
                                    )
                                }
                            }

                            // 9. Fotos da Ferramenta / Molde
                            Text("Fotos da Avaria / Dispositivo", style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { cameraLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2C24), contentColor = SicoiWarning)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Câmera")
                                }
                                Button(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2C24), contentColor = SicoiWarning)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Galeria")
                                }
                            }

                            if (photoBitmaps.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(photoBitmaps) { bmp ->
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, SicoiWarning, RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Botão de envio
                            Button(
                                onClick = {
                                    viewModel.submitNewOrder(photoBitmaps) {
                                        Toast.makeText(context, "O.S. de Ferramentaria enviada!", Toast.LENGTH_SHORT).show()
                                        onFinalized()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SicoiWarning, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enviar Ordem de Serviço", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        } else {
                            // ── MODO TÉCNICO: VISUALIZAÇÃO E EXECUÇÃO ──
                            val curOrder = (uiState as? ToolOSFormUiState.Loaded)?.order

                            // Card Resumo da Ferramenta
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1D19)),
                                border = BorderStroke(1.dp, SicoiWarning.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            curOrder?.numeroOs ?: "FER-S/N",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = SicoiWarning
                                        )
                                        Text(
                                            curOrder?.status ?: "Aberta",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (curOrder?.isPaused() == true) Color(0xFFF97316) else SicoiSuccess
                                        )
                                    }

                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                                    Text("Código: ${curOrder?.getFullToolCode()}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("Nome / Molde: ${curOrder?.getFullToolName()}", color = Color.White)
                                    Text("Tipo: ${curOrder?.tipoFerramenta ?: "Molde"}", color = SicoiTextMuted)
                                    Text("Máquina / Prensa: ${curOrder?.getFullMachine()}", color = SicoiTextMuted)
                                    Text("Setor: ${curOrder?.setor ?: "Ferramentaria"}", color = SicoiTextMuted)
                                    Text("Solicitante: ${curOrder?.getFullRequester()}", color = SicoiTextMuted)
                                    Text("Prioridade: ${curOrder?.prioridade ?: "Normal"}", color = SicoiWarning)

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Problema Relatado:", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(curOrder?.descricaoProblema ?: "Sem detalhes", color = Color.White.copy(alpha = 0.85f))
                                }
                            }

                            // Fotos do Solicitante
                            val photos = curOrder?.getPhotoList() ?: emptyList()
                            if (photos.isNotEmpty()) {
                                Text("Fotos da Avaria", fontWeight = FontWeight.Bold, color = Color.White)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(photos) { url ->
                                        AsyncImage(
                                            model = url,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, SicoiCardBorder, RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            // Se estiver pausada, mostra motivo
                            if (curOrder?.isPaused() == true) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1C12)),
                                    border = BorderStroke(1.dp, Color(0xFFF97316))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("O.S. EM PAUSA", fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                                        Text("Motivo: ${curOrder.motivoPausa ?: "Não informado"}", color = Color.White)
                                        if (!curOrder.observacoesPausa.isNullOrBlank()) {
                                            Text("Observações: ${curOrder.observacoesPausa}", color = SicoiTextMuted)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Painel de Ações do Técnico
                            if (curOrder?.isConcluded() == true) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SicoiSuccess.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, SicoiSuccess)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("O.S. CONCLUÍDA", fontWeight = FontWeight.Bold, color = SicoiSuccess)
                                        Text("Solução: ${curOrder.solucaoAplicada ?: ""}", color = Color.White)
                                        Text("Peças: ${curOrder.pecasUtilizadas ?: "Nenhuma"}", color = SicoiTextMuted)
                                        Text("Tempo gasto: ${curOrder.tempoGasto ?: "-"}", color = SicoiTextMuted)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (curOrder?.isPaused() == true) {
                                        Button(
                                            onClick = {
                                                viewModel.resumeOrder(workOrderId, technicianName) {
                                                    Toast.makeText(context, "Atendimento retomado!", Toast.LENGTH_SHORT).show()
                                                    viewModel.loadOrder(workOrderId)
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316), contentColor = Color.White)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Retomar")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { showPauseDialog = true },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            border = BorderStroke(1.dp, Color(0xFFF97316)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF97316))
                                        ) {
                                            Icon(Icons.Default.Pause, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pausar")
                                        }
                                    }

                                    Button(
                                        onClick = { showConcludeSheet = true },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SicoiSuccess, contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Concluir")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Pausa
    if (showPauseDialog) {
        AlertDialog(
            onDismissRequest = { showPauseDialog = false },
            title = { Text("Pausar Atendimento da Ferramenta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selecione ou digite o motivo da pausa:")
                    OutlinedTextField(
                        value = pauseReasonInput,
                        onValueChange = { pauseReasonInput = it },
                        label = { Text("Motivo da Pausa *") },
                        placeholder = { Text("Ex: Aguardando inserto / Aguardando eletrodo") },
                        colors = sicoiTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pauseObsInput,
                        onValueChange = { pauseObsInput = it },
                        label = { Text("Observações adicionais") },
                        colors = sicoiTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pauseReasonInput.isNotBlank()) {
                            showPauseDialog = false
                            viewModel.pauseOrder(workOrderId, pauseReasonInput, pauseObsInput) {
                                viewModel.loadOrder(workOrderId)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
                ) {
                    Text("Confirmar Pausa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPauseDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Conclusão da O.S.
    if (showConcludeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConcludeSheet = false },
            containerColor = Color(0xFF1E1D19)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Conclusão da Manutenção de Ferramentaria",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SicoiSuccess
                )

                OutlinedTextField(
                    value = viewModel.solucaoAplicada,
                    onValueChange = { viewModel.solucaoAplicada = it },
                    label = { Text("Solução Aplicada / Reparo Realizado *") },
                    placeholder = { Text("Ex: Usinado novo pino guia, ajustado encaixe da cavidade 2 e polido espelho.") },
                    colors = sicoiTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )

                OutlinedTextField(
                    value = viewModel.pecasUtilizadas,
                    onValueChange = { viewModel.pecasUtilizadas = it },
                    label = { Text("Componentes / Peças Trocadas") },
                    placeholder = { Text("Ex: 1 Pino Guia 12mm, 2 Molas de Retorno") },
                    colors = sicoiTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.tempoGasto,
                    onValueChange = { viewModel.tempoGasto = it },
                    label = { Text("Tempo Gasto de Manutenção") },
                    placeholder = { Text("Ex: 2h 30min") },
                    colors = sicoiTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Fotos do Reparo Concluído", color = Color.White, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { afterCameraLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2C24), contentColor = SicoiWarning)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Câmera")
                    }
                    Button(
                        onClick = { afterGalleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2C24), contentColor = SicoiWarning)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galeria")
                    }
                }

                if (afterPhotoBitmaps.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(afterPhotoBitmaps) { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, SicoiSuccess, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Assinatura Digital
                SignatureCanvas(
                    modifier = Modifier.fillMaxWidth(),
                    onSignatureChanged = { signatureBitmap = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        showConcludeSheet = false
                        viewModel.concludeOrder(
                            orderId = workOrderId,
                            technicianName = technicianName,
                            afterPhotos = afterPhotoBitmaps,
                            signatureBitmap = signatureBitmap
                        ) {
                            Toast.makeText(context, "O.S. Concluída com Sucesso!", Toast.LENGTH_LONG).show()
                            onFinalized()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SicoiSuccess, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Finalizar e Concluir O.S.", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        original?.let {
            val maxDim = 1200
            val width = it.width
            val height = it.height
            if (width > maxDim || height > maxDim) {
                val ratio = width.toFloat() / height.toFloat()
                val targetW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                val targetH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                Bitmap.createScaledBitmap(it, targetW, targetH, true)
            } else it
        }
    } catch (e: Exception) {
        null
    }
}
