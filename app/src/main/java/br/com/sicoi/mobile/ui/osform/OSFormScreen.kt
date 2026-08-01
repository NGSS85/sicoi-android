package br.com.sicoi.mobile.ui.osform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.sicoi.mobile.ui.login.sicoiTextFieldColors
import br.com.sicoi.mobile.ui.theme.*


/**
 * Tela 5: Formulário Dinâmico da O.S. — Visão do Técnico
 *
 * Layout em ScrollView garantindo enquadramento perfeito em todas as telas.
 *
 * ÁREA DO SOLICITANTE (Read-Only):
 * - Número OS, Data, Equipamento, Setor, Solicitante, Descrição do Problema
 *
 * ÁREA DO TÉCNICO (Editável):
 * - Solução Aplicada, Peças Utilizadas, Tempo Gasto
 * - Canvas de Assinatura Digital
 * - Câmera Antes/Depois
 * - Botão [Finalizar O.S.]
 */
@Composable
fun OSFormScreen(
    workOrderId: String,
    technicianName: String,
    onNavigateBack: () -> Unit,
    onFinalized: () -> Unit,
    isRequesterMode: Boolean = false,
    viewModel: OSFormViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var showConfirmDialog by remember { mutableStateOf(false) }
    var beforeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var afterBitmap  by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(workOrderId) {
        viewModel.loadWorkOrder(workOrderId, technicianName)
    }

    // Reage a estados de salvamento
    LaunchedEffect(state) {
        when (state) {
            is OSFormUiState.SavedOnline, is OSFormUiState.SavedOffline -> onFinalized()
            else -> {}
        }
    }

    // Dialog de confirmação para finalizar
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.Assignment, contentDescription = null, tint = SicoiOrange) },
            title = { Text("Finalizar O.S.?", style = MaterialTheme.typography.titleLarge, color = SicoiTextPrimary) },
            text = {
                Text(
                    "Esta ação não pode ser desfeita. A ordem de serviço será marcada como Finalizada e sincronizada com o sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SicoiTextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.finalizeWorkOrder()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SicoiSuccess)
                ) {
                    Text("Confirmar Finalização")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar", color = SicoiTextMuted)
                }
            },
            containerColor = SicoiCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = SicoiBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(SicoiSurface, SicoiBackground)))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SicoiCard)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = SicoiTextSecondary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ordem de Serviço", style = MaterialTheme.typography.labelSmall, color = SicoiOrange)
                        Text(
                            if (isRequesterMode) "Formulário do Solicitante" else "Formulário do Técnico",
                            style = MaterialTheme.typography.titleLarge,
                            color = SicoiTextPrimary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        when (val s = state) {
            is OSFormUiState.Loading, is OSFormUiState.Saving -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SicoiOrange)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (s is OSFormUiState.Saving) "Salvando O.S...." else "Carregando...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SicoiTextMuted
                        )
                    }
                }
            }
            is OSFormUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SicoiError, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(s.message, style = MaterialTheme.typography.bodyMedium, color = SicoiTextSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadWorkOrder(workOrderId, technicianName) },
                            colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange)
                        ) { Text("Tentar Novamente") }
                    }
                }
            }
            is OSFormUiState.Loaded -> {
                val order = s.order
                
                var solicitanteVal by remember(order) { mutableStateOf(order.solicitante.takeIf { !it.isNullOrBlank() } ?: "") }
                var equipamentoVal  by remember(order) { mutableStateOf(order.equipamento.takeIf { !it.isNullOrBlank() } ?: "") }
                var setorVal        by remember(order) { mutableStateOf(order.setor.takeIf { !it.isNullOrBlank() } ?: "") }
                var prioridadeVal   by remember(order) { mutableStateOf(order.prioridade.takeIf { !it.isNullOrBlank() } ?: "Normal") }
                var descricaoVal    by remember(order) { mutableStateOf(order.descricaoProblema.takeIf { !it.isNullOrBlank() } ?: "") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // =========================================================
                    // BLOCO 1: Identificação Geral
                    // =========================================================
                    FormSection(
                        title = "Identificação Geral",
                        subtitle = "Dados do chamado e registro",
                        icon = Icons.Default.ConfirmationNumber,
                        accentColor = SicoiBlue,
                        isReadOnly = true
                    ) {
                        ReadOnlyField("Número O.S.", order.numeroOs ?: "OS-NOVA")
                        ReadOnlyField("Data de Abertura", order.dataAbertura?.take(10) ?: "Hoje")
                        ReadOnlyField("Técnico Responsável", technicianName)
                    }

                    // =========================================================
                    // BLOCO 2: Dados do Equipamento
                    // =========================================================
                    FormSection(
                        title = "Dados do Equipamento",
                        subtitle = "Identificação do ativo e localização",
                        icon = Icons.Default.Build,
                        accentColor = SicoiOrange,
                        isReadOnly = !isRequesterMode && !order.equipamento.isNullOrBlank()
                    ) {
                        if (isRequesterMode) {
                            EditableField(
                                label = "Equipamento *",
                                value = equipamentoVal,
                                onValueChange = { equipamentoVal = it },
                                placeholder = "Ex: Prensa Hidráulica 50T",
                                minLines = 1
                            )
                            EditableField(
                                label = "Setor / Localização *",
                                value = setorVal,
                                onValueChange = { setorVal = it },
                                placeholder = "Ex: Usinagem - Galpão B",
                                minLines = 1
                            )
                            EditableField(
                                label = "Prioridade",
                                value = prioridadeVal,
                                onValueChange = { prioridadeVal = it },
                                placeholder = "Ex: Alta / Normal / Baixa",
                                minLines = 1
                            )
                        } else {
                            ReadOnlyField("Equipamento", order.equipamento ?: "—")
                            ReadOnlyField("Setor", order.setor ?: "—")
                            ReadOnlyField("Prioridade", order.prioridade ?: "Normal")
                        }
                    }

                    // =========================================================
                    // BLOCO 3: Dados do Solicitante
                    // =========================================================
                    FormSection(
                        title = "Dados do Solicitante",
                        subtitle = "Quem solicitou e descrição da ocorrência",
                        icon = Icons.Default.Person,
                        accentColor = SicoiSuccess,
                        isReadOnly = !isRequesterMode && !order.solicitante.isNullOrBlank()
                    ) {
                        if (isRequesterMode) {
                            EditableField(
                                label = "Nome do Solicitante *",
                                value = solicitanteVal,
                                onValueChange = { solicitanteVal = it },
                                placeholder = "Digite seu nome...",
                                minLines = 1
                            )
                            EditableField(
                                label = "Descrição do Problema *",
                                value = descricaoVal,
                                onValueChange = { descricaoVal = it },
                                placeholder = "Descreva detalhadamente a falha ou necessidade...",
                                minLines = 3
                            )
                        } else {
                            ReadOnlyField("Solicitante", order.solicitante ?: "—")
                            if (!order.descricaoProblema.isNullOrBlank()) {
                                ReadOnlyField("Descrição do Problema", order.descricaoProblema, isMultiline = true)
                            }
                        }
                    }

                    // ─────────────────────────────────────────────────────────
                    // REGRA DE CORTE:
                    // Se estiver em modo Solicitante (isRequesterMode == true),
                    // o formulário PARA AQUI. Não exibe nenhuma seção do técnico.
                    // ─────────────────────────────────────────────────────────
                    if (isRequesterMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                viewModel.createRequesterWorkOrder(
                                    solicitante = solicitanteVal,
                                    equipamento = equipamentoVal,
                                    setor = setorVal,
                                    prioridade = prioridadeVal,
                                    descricaoProblema = descricaoVal,
                                    technicianName = technicianName,
                                    onSuccess = onFinalized
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SicoiOrange,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Enviar Formulário Ordem de serviço",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {

                        // =========================================================
                        // SEÇÃO 2: Área do Técnico (EDITÁVEL)
                        // =========================================================
                        FormSection(
                            title = "Resolução do Técnico",
                            subtitle = "Preencha com os dados do atendimento",
                            icon = Icons.Default.Engineering,
                            accentColor = SicoiOrange,
                            isReadOnly = false
                        ) {
                            // Solução Aplicada
                            EditableField(
                                label = "Solução Aplicada *",
                                value = viewModel.solucao,
                                onValueChange = { viewModel.solucao = it },
                                placeholder = "Descreva a solução aplicada ao problema...",
                                minLines = 3
                            )

                            // Peças Utilizadas
                            EditableField(
                                label = "Peças Utilizadas",
                                value = viewModel.pecas,
                                onValueChange = { viewModel.pecas = it },
                                placeholder = "Liste as peças substituídas (ex: Rolamento SKF 6205, Correia V-10)...",
                                minLines = 2
                            )

                            // Tempo Gasto
                            EditableField(
                                label = "Tempo Gasto",
                                value = viewModel.tempo,
                                onValueChange = { viewModel.tempo = it },
                                placeholder = "Ex: 2h 30min",
                                minLines = 1
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.pauseWorkOrder() },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SicoiWarning)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pausar", style = MaterialTheme.typography.titleSmall)
                                }
                                Button(
                                    onClick = { viewModel.externalWorkOrder() },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SicoiBlue)
                                ) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Externo", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }

                        // =========================================================
                        // SEÇÃO 3: Assinatura Digital
                        // =========================================================
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SicoiCard),
                            border = BorderStroke(1.dp, SicoiCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SignatureCanvas(
                                    modifier = Modifier.fillMaxWidth(),
                                    onSignatureChanged = { viewModel.signatureBitmap = it }
                                )
                            }
                        }

                        // =========================================================
                        // SEÇÃO 4: Fotos Antes/Depois
                        // =========================================================
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SicoiCard),
                            border = BorderStroke(1.dp, SicoiCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                CameraCapture(
                                    modifier = Modifier.fillMaxWidth(),
                                    beforeBitmap = beforeBitmap,
                                    afterBitmap = afterBitmap,
                                    onBeforeCaptured = { uri ->
                                        viewModel.beforeUri = uri
                                        context.contentResolver.openInputStream(uri)?.use {
                                            beforeBitmap = BitmapFactory.decodeStream(it)
                                        }
                                    },
                                    onAfterCaptured = { uri ->
                                        viewModel.afterUri = uri
                                        context.contentResolver.openInputStream(uri)?.use {
                                            afterBitmap = BitmapFactory.decodeStream(it)
                                        }
                                    }
                                )
                            }
                        }

                        // =========================================================
                        // BOTÃO FINALIZAR (TÉCNICO)
                        // =========================================================
                        Button(
                            onClick = { showConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SicoiSuccess,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Finalizar Ordem de Serviço",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            else -> {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes auxiliares do formulário
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormSection(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isReadOnly: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = if (isReadOnly) 0.15f else 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header da seção
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = SicoiTextPrimary)
                        if (isReadOnly) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SicoiCardBorder.copy(alpha = 0.5f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("READ-ONLY", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = SicoiTextMuted))
                            }
                        }
                    }
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, color = SicoiTextMuted))
                }
            }

            Divider(color = SicoiDivider, modifier = Modifier.padding(bottom = 16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
private fun ReadOnlyField(label: String, value: String, isMultiline: Boolean = false) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = SicoiTextMuted, letterSpacing = 0.5.sp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SicoiSurface)
                .border(1.dp, SicoiDivider, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = if (isMultiline) 10.dp else 8.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = SicoiTextPrimary.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            )
        }
    }
}

@Composable
private fun EditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = SicoiTextSecondary, letterSpacing = 0.5.sp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = SicoiTextMuted)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            minLines = minLines,
            colors = sicoiTextFieldColors()
        )
    }
}
