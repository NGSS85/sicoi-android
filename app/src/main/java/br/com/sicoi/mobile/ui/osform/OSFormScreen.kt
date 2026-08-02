package br.com.sicoi.mobile.ui.osform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.sicoi.mobile.ui.login.sicoiTextFieldColors
import br.com.sicoi.mobile.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenuDropdown by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var beforeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var afterBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Fotos dos solicitante (lista de bitmaps)
    var photoBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    // Launcher para selecionar múltiplas fotos da galeria
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newBitmaps = uris.mapNotNull { uri ->
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        }
        photoBitmaps = photoBitmaps + newBitmaps
    }

    LaunchedEffect(workOrderId) {
        viewModel.loadWorkOrder(workOrderId, technicianName)
    }

    LaunchedEffect(state) {
        when (state) {
            is OSFormUiState.SavedOnline, is OSFormUiState.SavedOffline -> onFinalized()
            else -> {}
        }
    }

    // Dialog finalizar
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.Assignment, contentDescription = null, tint = SicoiOrange) },
            title = { Text("Salvar Formulário?", style = MaterialTheme.typography.titleLarge, color = SicoiTextPrimary) },
            text = {
                Text(
                    "O formulário será enviado ao sistema. Deseja continuar?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SicoiTextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        (state as? OSFormUiState.Loaded)?.let { loaded ->
                            viewModel.createRequesterWorkOrder(
                                solicitante = viewModel.solicitanteForm,
                                equipamento = viewModel.equipamentoForm,
                                setor = "",
                                prioridade = viewModel.prioridadeForm,
                                descricaoProblema = viewModel.descricaoForm,
                                technicianName = technicianName,
                                onSuccess = onFinalized
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SicoiSuccess)
                ) { Text("Confirmar") }
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

    // Dialog excluir
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SicoiError) },
            title = { Text("Excluir O.S.?", style = MaterialTheme.typography.titleLarge, color = SicoiTextPrimary) },
            text = {
                Text(
                    "Esta ação não pode ser desfeita. A ordem de serviço será excluída permanentemente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SicoiTextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onFinalized()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SicoiError)
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
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
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Linha do topo: seta de retorno + número da OS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SicoiCard)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = SicoiTextSecondary)
                    }

                    // Número da OS em destaque no canto direito
                    val osNumber = (state as? OSFormUiState.Loaded)?.order?.numeroOs ?: "—"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SicoiOrange.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SicoiOrange.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Tag,
                                contentDescription = null,
                                tint = SicoiOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                osNumber,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = SicoiOrange
                                )
                            )
                        }
                    }
                }

                // Títulos abaixo da seta
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Ordens de Serviço",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = SicoiOrange
                        )
                    )
                    Text(
                        "Formulário do Solicitante",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = SicoiTextPrimary
                        )
                    )
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
                        Button(onClick = { viewModel.loadWorkOrder(workOrderId, technicianName) },
                            colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange)
                        ) { Text("Tentar Novamente") }
                    }
                }
            }

            is OSFormUiState.Loaded -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // ─── Abas ─────────────────────────────────────────────
                    val tabs = listOf("Dados do Solicitante", "Dados do Equipamento")
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = SicoiSurface,
                        contentColor = SicoiOrange,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = SicoiOrange
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        ),
                                        color = if (selectedTabIndex == index) SicoiOrange else SicoiTextMuted
                                    )
                                }
                            )
                        }
                    }

                    // ─── Conteúdo das Abas ────────────────────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedTabIndex) {

                            // ══════════════════════════════════════════════
                            // ABA 0: Dados do Solicitante
                            // ══════════════════════════════════════════════
                            0 -> {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SicoiCard),
                                    border = BorderStroke(1.dp, SicoiSuccess.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Cabeçalho da seção
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .background(SicoiSuccess.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = null, tint = SicoiSuccess, modifier = Modifier.size(20.dp))
                                            }
                                            Column {
                                                Text("Dados do Solicitante", style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.6.sp), color = SicoiTextPrimary)
                                                Text("Quem está solicitando a O.S.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.2.sp), color = SicoiTextMuted)
                                            }
                                        }

                                        HorizontalDivider(color = SicoiDivider)

                                        // Nome do Solicitante
                                        EditableOSField(
                                            label = "Nome do Solicitante *",
                                            value = viewModel.solicitanteForm,
                                            onValueChange = { viewModel.solicitanteForm = it },
                                            placeholder = "Digite seu nome completo...",
                                            icon = Icons.Default.Person,
                                            isTab0 = true
                                        )

                                        // Data (somente leitura — preenchida automaticamente)
                                        val today = remember {
                                            SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
                                        }
                                        ReadOnlyOSField(
                                            label = "Data da Solicitação (automático)",
                                            value = today,
                                            icon = Icons.Default.CalendarToday,
                                            isTab0 = true
                                        )

                                        // Hora (somente leitura — preenchida automaticamente)
                                        val currentTime = remember {
                                            SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date())
                                        }
                                        ReadOnlyOSField(
                                            label = "Hora (automático ao salvar)",
                                            value = currentTime,
                                            icon = Icons.Default.AccessTime,
                                            isTab0 = true
                                        )
                                    }
                                }
                            }

                            // ══════════════════════════════════════════════
                            // ABA 1: Dados do Equipamento
                            // ══════════════════════════════════════════════
                            1 -> {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SicoiCard),
                                    border = BorderStroke(1.dp, SicoiOrange.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Cabeçalho da seção
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .background(SicoiOrange.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Build, contentDescription = null, tint = SicoiOrange, modifier = Modifier.size(20.dp))
                                            }
                                            Column {
                                                Text("Dados do Equipamento", style = MaterialTheme.typography.titleMedium, color = SicoiTextPrimary)
                                                Text("Informações do ativo e ocorrência", style = MaterialTheme.typography.bodySmall, color = SicoiTextMuted)
                                            }
                                        }

                                        HorizontalDivider(color = SicoiDivider)

                                        // Equipamento
                                        EditableOSField(
                                            label = "Equipamento *",
                                            value = viewModel.equipamentoForm,
                                            onValueChange = { viewModel.equipamentoForm = it },
                                            placeholder = "Ex: Prensa Hidráulica 50T",
                                            icon = Icons.Default.Settings
                                        )

                                        // Número do Patrimônio
                                        EditableOSField(
                                            label = "Número do Patrimônio",
                                            value = viewModel.patrimonioForm,
                                            onValueChange = { viewModel.patrimonioForm = it },
                                            placeholder = "Ex: PAT-00123",
                                            icon = Icons.Default.Tag
                                        )

                                        // Prioridade — Seleção com chips
                                        Column {
                                            Text(
                                                "Prioridade *",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = SicoiTextSecondary,
                                                    letterSpacing = 0.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                listOf(
                                                    "Emergência" to SicoiError,
                                                    "Urgente" to SicoiWarning,
                                                    "Normal" to SicoiSuccess
                                                ).forEach { (label, color) ->
                                                    val isSelected = viewModel.prioridadeForm == label
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = { viewModel.prioridadeForm = label },
                                                        label = {
                                                            Text(
                                                                label,
                                                                style = MaterialTheme.typography.labelMedium.copy(
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                                )
                                                            )
                                                        },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = color.copy(alpha = 0.2f),
                                                            selectedLabelColor = color,
                                                            containerColor = SicoiSurface,
                                                            labelColor = SicoiTextMuted
                                                        ),
                                                        border = FilterChipDefaults.filterChipBorder(
                                                            enabled = true,
                                                            selected = isSelected,
                                                            selectedBorderColor = color.copy(alpha = 0.5f),
                                                            borderColor = SicoiCardBorder
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }

                                        // Tipo de Manutenção — Seleção múltipla com chips
                                        Column {
                                            Text(
                                                "Tipo de Manutenção *",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = SicoiTextSecondary,
                                                    letterSpacing = 0.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val tiposManutencao = listOf("Mecânica", "Elétrica", "Hidráulica", "Pneumática")
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                tiposManutencao.take(2).forEach { tipo ->
                                                    val isSelected = viewModel.tiposManutencaoForm.contains(tipo)
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = {
                                                            viewModel.tiposManutencaoForm = if (isSelected)
                                                                viewModel.tiposManutencaoForm - tipo
                                                            else
                                                                viewModel.tiposManutencaoForm + tipo
                                                        },
                                                        label = {
                                                            Text(
                                                                tipo,
                                                                style = MaterialTheme.typography.labelMedium.copy(
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                    fontSize = 11.sp
                                                                )
                                                            )
                                                        },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = SicoiBlue.copy(alpha = 0.2f),
                                                            selectedLabelColor = SicoiBlueLight,
                                                            containerColor = SicoiSurface,
                                                            labelColor = SicoiTextMuted
                                                        ),
                                                        border = FilterChipDefaults.filterChipBorder(
                                                            enabled = true,
                                                            selected = isSelected,
                                                            selectedBorderColor = SicoiBlue.copy(alpha = 0.5f),
                                                            borderColor = SicoiCardBorder
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                tiposManutencao.drop(2).forEach { tipo ->
                                                    val isSelected = viewModel.tiposManutencaoForm.contains(tipo)
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = {
                                                            viewModel.tiposManutencaoForm = if (isSelected)
                                                                viewModel.tiposManutencaoForm - tipo
                                                            else
                                                                viewModel.tiposManutencaoForm + tipo
                                                        },
                                                        label = {
                                                            Text(
                                                                tipo,
                                                                style = MaterialTheme.typography.labelMedium.copy(
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                    fontSize = 11.sp
                                                                )
                                                            )
                                                        },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = SicoiBlue.copy(alpha = 0.2f),
                                                            selectedLabelColor = SicoiBlueLight,
                                                            containerColor = SicoiSurface,
                                                            labelColor = SicoiTextMuted
                                                        ),
                                                        border = FilterChipDefaults.filterChipBorder(
                                                            enabled = true,
                                                            selected = isSelected,
                                                            selectedBorderColor = SicoiBlue.copy(alpha = 0.5f),
                                                            borderColor = SicoiCardBorder
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }

                                        // Descrição do Problema
                                        Column {
                                            Text(
                                                "Descrição do Problema *",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = SicoiTextSecondary,
                                                    letterSpacing = 0.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = viewModel.descricaoForm,
                                                onValueChange = { viewModel.descricaoForm = it },
                                                placeholder = {
                                                    Text(
                                                        "Descreva detalhadamente o problema ou falha observada...",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = SicoiTextMuted)
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                minLines = 4,
                                                colors = sicoiTextFieldColors()
                                            )
                                        }

                                        // Seção de Anexo de Fotos
                                        Column {
                                            Text(
                                                "Fotos do Problema (Opcional)",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = SicoiTextSecondary,
                                                    letterSpacing = 0.5.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Botão de adicionar fotos
                                            OutlinedButton(
                                                onClick = { photoPickerLauncher.launch("image/*") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, SicoiOrange.copy(alpha = 0.5f)),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SicoiOrange)
                                            ) {
                                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "Anexar Foto(s) do Problema",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }

                                            // Miniaturas das fotos selecionadas
                                            if (photoBitmaps.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    items(photoBitmaps.size) { index ->
                                                        Box(
                                                            modifier = Modifier.size(72.dp)
                                                        ) {
                                                            Image(
                                                                bitmap = photoBitmaps[index].asImageBitmap(),
                                                                contentDescription = "Foto ${index + 1}",
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .border(1.dp, SicoiCardBorder, RoundedCornerShape(10.dp))
                                                            )
                                                            // Botão de remover
                                                            IconButton(
                                                                onClick = {
                                                                    photoBitmaps = photoBitmaps.toMutableList().also { it.removeAt(index) }
                                                                },
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .size(20.dp)
                                                                    .background(SicoiError.copy(alpha = 0.85f), CircleShape)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Close,
                                                                    contentDescription = "Remover",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                Text(
                                                    "${photoBitmaps.size} foto(s) selecionada(s)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SicoiTextMuted,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // ─── Botões de Ação ───────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Botão Salvar Formulário
                            Button(
                                onClick = { showConfirmDialog = true },
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SicoiOrange,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Salvar Formulário",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // Botão de três pontos (⋮) — Editar / Excluir
                            Box {
                                FilledTonalIconButton(
                                    onClick = { showMenuDropdown = true },
                                    modifier = Modifier.size(54.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = SicoiCard,
                                        contentColor = SicoiTextSecondary
                                    )
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                                }
                                DropdownMenu(
                                    expanded = showMenuDropdown,
                                    onDismissRequest = { showMenuDropdown = false },
                                    modifier = Modifier.background(SicoiCard)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = SicoiOrange, modifier = Modifier.size(18.dp))
                                                Text("Editar O.S.", color = SicoiTextPrimary)
                                            }
                                        },
                                        onClick = {
                                            editMode = true
                                            showMenuDropdown = false
                                        }
                                    )
                                    HorizontalDivider(color = SicoiDivider)
                                    DropdownMenuItem(
                                        text = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = SicoiError, modifier = Modifier.size(18.dp))
                                                Text("Excluir O.S.", color = SicoiError)
                                            }
                                        },
                                        onClick = {
                                            showMenuDropdown = false
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            else -> {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes auxiliares
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditableOSField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    minLines: Int = 1,
    isTab0: Boolean = false
) {
    val scaleLabel = if (isTab0) 1.1f else 1.0f
    val scaleValue = if (isTab0) 1.15f else 1.0f
    val weight = if (isTab0) androidx.compose.ui.text.font.FontWeight.Bold else null
    val defaultLabelSize = 11.sp
    val defaultBodySize = 16.sp

    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = SicoiTextSecondary, letterSpacing = 0.5.sp, fontSize = defaultLabelSize * scaleLabel)
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = LocalTextStyle.current.copy(fontSize = defaultBodySize * scaleValue, fontWeight = weight),
            placeholder = {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp * scaleLabel, color = SicoiTextMuted))
            },
            leadingIcon = {
                Icon(icon, contentDescription = null, tint = SicoiTextMuted, modifier = Modifier.size(18.dp))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            minLines = minLines,
            colors = sicoiTextFieldColors()
        )
    }
}

@Composable
private fun ReadOnlyOSField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isTab0: Boolean = false
) {
    val scaleLabel = if (isTab0) 1.1f else 1.0f
    val scaleValue = if (isTab0) 1.15f else 1.0f
    val weight = if (isTab0) androidx.compose.ui.text.font.FontWeight.Bold else null
    val defaultLabelSize = 11.sp

    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = SicoiTextMuted, letterSpacing = 0.5.sp, fontSize = defaultLabelSize * scaleLabel)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SicoiSurface)
                .border(1.dp, SicoiDivider, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = SicoiTextMuted, modifier = Modifier.size(16.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = SicoiTextPrimary.copy(alpha = 0.85f),
                        fontSize = 13.sp * scaleValue,
                        fontWeight = weight
                    )
                )
            }
        }
    }
}

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
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isReadOnly) 0.15f else 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    Text(title, style = MaterialTheme.typography.titleMedium, color = SicoiTextPrimary)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, color = SicoiTextMuted))
                }
            }
            HorizontalDivider(color = SicoiDivider, modifier = Modifier.padding(bottom = 16.dp))
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
            placeholder = {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = SicoiTextMuted))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            minLines = minLines,
            colors = sicoiTextFieldColors()
        )
    }
}
