package br.com.sicoi.mobile.ui.osform

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import br.com.sicoi.mobile.data.model.*
import br.com.sicoi.mobile.ui.theme.*
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OSFormScreen(
    workOrderId: String,
    technicianName: String,
    onNavigateBack: () -> Unit,
    onFinalized: () -> Unit,
    onNavigateToHistory: ((String) -> Unit)? = null,
    onNavigateToPausedOrders: ((String) -> Unit)? = null,
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
    var showImagesDialog by remember { mutableStateOf(false) }
    var afterBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var photoBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var servicePhotoBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var materialPhotoBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var activeAttachmentSection by remember { mutableStateOf("requester") }
    var viewingImageUrl by remember { mutableStateOf<String?>(null) }
    var viewingBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val expandedCardIds = remember { mutableStateMapOf<String, Boolean>() }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // 1. Launchers para o Solicitante
    val requesterPhotoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newBitmaps = uris.mapNotNull { uri -> decodeUriAsScaledBitmap(context, uri) }
        photoBitmaps = photoBitmaps + newBitmaps
    }

    val requesterCameraPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { photoBitmaps = photoBitmaps + it }
    }

    val requesterCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            requesterCameraPhotoLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Launchers para o Técnico (Serviço Executado)
    val servicePhotoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newBitmaps = uris.mapNotNull { uri -> decodeUriAsScaledBitmap(context, uri) }
        servicePhotoBitmaps = servicePhotoBitmaps + newBitmaps
    }

    val serviceCameraPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { servicePhotoBitmaps = servicePhotoBitmaps + it }
    }

    val serviceCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            serviceCameraPhotoLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
        }
    }

    // 3. Launchers para o Técnico (Materiais Utilizados)
    val materialPhotoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val newBitmaps = uris.mapNotNull { uri -> decodeUriAsScaledBitmap(context, uri) }
        materialPhotoBitmaps = materialPhotoBitmaps + newBitmaps
    }

    val materialCameraPhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { materialPhotoBitmaps = materialPhotoBitmaps + it }
    }

    val materialCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            materialCameraPhotoLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(workOrderId) {
        viewModel.loadWorkOrder(workOrderId, technicianName)
    }

    LaunchedEffect(state) {
        when (val currentState = state) {
            is OSFormUiState.SavedOnline -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                onFinalized()
            }
            is OSFormUiState.SavedOffline -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                onFinalized()
            }
            is OSFormUiState.Error -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
            }
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
                            if (isRequesterMode) {
                                viewModel.createRequesterWorkOrder(
                                    solicitante = viewModel.solicitanteForm,
                                    equipamento = viewModel.equipamentoForm,
                                    setor = viewModel.setorForm,
                                    prioridade = viewModel.prioridadeForm,
                                    descricaoProblema = viewModel.descricaoForm,
                                    technicianName = technicianName,
                                    photoBitmaps = photoBitmaps,
                                    onSuccess = onFinalized
                                )
                            } else {
                                // Atualiza data/hora com o momento exato do salvamento
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                val stf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                val now = java.util.Date()
                                viewModel.finalDate = sdf.format(now)
                                viewModel.finalHour = stf.format(now)
                                viewModel.finalizeWorkOrder(
                                    technicianName = technicianName,
                                    serviceBitmaps = servicePhotoBitmaps,
                                    materialBitmaps = materialPhotoBitmaps,
                                    onSuccess = onFinalized
                                )
                            }
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

    val isNewOrder = workOrderId == "new" || workOrderId.isBlank() || workOrderId.startsWith("NEW")
    val osNumber = if (isRequesterMode) {
        "Gerado ao Salvar"
    } else if (isNewOrder) {
        "Nova O.S."
    } else {
        ((state as? OSFormUiState.Loaded)?.order?.numeroOs ?: "—")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1A1A1A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Título da sidebar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = null,
                            tint = SicoiOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Menu",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color.White
                        )
                    }

                    if (!isRequesterMode) {
                        // 1. Botão: Ordens em Pausa
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SicoiWarning.copy(alpha = 0.12f))
                                .border(1.dp, SicoiWarningBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    coroutineScope.launch { drawerState.close() }
                                    onNavigateToPausedOrders?.invoke(technicianName)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = null,
                                tint = SicoiWarning,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    "Ordens em Pausa",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    "Ver ordens pausadas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SicoiWarning
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Botão: Histórico
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SicoiOrange.copy(alpha = 0.12f))
                                .border(1.dp, SicoiOrangeBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    coroutineScope.launch { drawerState.close() }
                                    onNavigateToHistory?.invoke(technicianName)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = SicoiOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    "Histórico",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    "Minhas atividades",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SicoiTextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    CentralDoSolicitanteContent(
                        isRequesterMode = isRequesterMode,
                        viewModel = viewModel,
                        expandedCardIds = expandedCardIds
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = SicoiBackground,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(SicoiSurface, SicoiBackground)))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Linha do topo: seta de retorno + botão menu
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

                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SicoiCard)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Lateral", tint = SicoiTextSecondary)
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
                        if (isRequesterMode) "Formulário do Solicitante" else "Formulario do Técnico",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
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
                    val tabs = if (isRequesterMode) {
                        listOf("Dados do Solicitante", "Dados do Equipamento")
                    } else {
                        listOf("Informações do Solicitante", "Execução do Técnico")
                    }
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
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 15.sp
                                        ),
                                        color = if (selectedTabIndex == index) SicoiOrange else SicoiTextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
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
                        if (isRequesterMode) {
                            when (selectedTabIndex) {
                                // ══════════════════════════════════════════════
                                // ABA 0: Dados do Solicitante
                                // ══════════════════════════════════════════════
                                0 -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = SicoiCard),
                                            border = BorderStroke(1.dp, SicoiSuccess.copy(alpha = 0.35f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                verticalArrangement = Arrangement.spacedBy(18.dp)
                                            ) {
                                                // Cabeçalho da seção
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    modifier = Modifier.padding(bottom = 2.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .background(SicoiSuccess.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Person, contentDescription = null, tint = SicoiSuccess, modifier = Modifier.size(22.dp))
                                                    }
                                                    Column {
                                                        Text(
                                                            "Dados do Solicitante", 
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                fontSize = 19.sp,
                                                                fontWeight = FontWeight.Bold
                                                            ), 
                                                            color = SicoiTextPrimary
                                                        )
                                                        Text("Quem está abrindo a ordem de serviço", style = MaterialTheme.typography.bodySmall, color = SicoiTextMuted)
                                                    }
                                                }

                                                HorizontalDivider(color = SicoiDivider)

                                                // Solicitante
                                                EditableOSField(
                                                    label = "Nome do Solicitante *",
                                                    value = viewModel.solicitanteForm,
                                                    onValueChange = { viewModel.solicitanteForm = it },
                                                    placeholder = "Ex: João Silva",
                                                    icon = Icons.Default.Person,
                                                    isTab0 = true
                                                )

                                                // Setor
                                                EditableOSField(
                                                    label = "Setor / Localização *",
                                                    value = viewModel.setorForm,
                                                    onValueChange = { viewModel.setorForm = it },
                                                    placeholder = "Ex: Usinagem, Montagem, Linha 1",
                                                    icon = Icons.Default.Business,
                                                    isTab0 = true
                                                )

                                                // Data e Hora lado a lado para melhor distribuição
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        ReadOnlyOSField(
                                                            label = "Data de Abertura",
                                                            value = viewModel.dateForm,
                                                            icon = Icons.Default.DateRange,
                                                            isTab0 = true
                                                        )
                                                    }
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        ReadOnlyOSField(
                                                            label = "Hora",
                                                            value = viewModel.timeForm,
                                                            icon = Icons.Default.Schedule,
                                                            isTab0 = true
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Botão para avançar para a próxima aba
                                        Button(
                                            onClick = { selectedTabIndex = 1 },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SicoiOrange,
                                                contentColor = Color.White
                                            ),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                                        ) {
                                            Text(
                                                "Avançar para Dados do Equipamento",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
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
                                        border = BorderStroke(1.dp, SicoiOrangeBorder)
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
                                                    Text(
                                                        "Dados do Equipamento", 
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontSize = 19.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ), 
                                                        color = SicoiTextPrimary
                                                    )
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
                                                icon = Icons.Default.Settings,
                                                isTab1 = true
                                            )

                                            // Número do Patrimônio
                                            EditableOSField(
                                                label = "Número do Patrimônio",
                                                value = viewModel.patrimonioForm,
                                                onValueChange = { viewModel.patrimonioForm = it },
                                                placeholder = "Ex: PAT-00123",
                                                icon = Icons.Default.Tag,
                                                isTab1 = true
                                            )

                                            // Prioridade — Seleção sem quebra de texto
                                            Column {
                                                Text(
                                                    "Prioridade *",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = SicoiTextPrimary,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                        fontSize = 12.1.sp,
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
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(if (isSelected) color.copy(alpha = 0.2f) else SicoiSurface)
                                                                .border(
                                                                    width = 1.dp,
                                                                    color = if (isSelected) color.copy(alpha = 0.8f) else SicoiCardBorder,
                                                                    shape = RoundedCornerShape(10.dp)
                                                                )
                                                                .clickable { viewModel.prioridadeForm = label }
                                                                .padding(vertical = 10.dp, horizontal = 2.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                label,
                                                                style = MaterialTheme.typography.labelMedium.copy(
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                    fontSize = 12.5.sp
                                                                ),
                                                                color = if (isSelected) color else SicoiTextMuted,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Tipo de Manutenção — Seleção múltipla com chips
                                            Column {
                                                Text(
                                                    "Tipo de Manutenção *",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = SicoiTextPrimary,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                        fontSize = 12.1.sp,
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
                                                                        fontSize = 12.1.sp
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
                                                                        fontSize = 12.1.sp
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
                                                        color = SicoiTextPrimary,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                        fontSize = 12.1.sp,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                OutlinedTextField(
                                                    value = viewModel.descricaoForm,
                                                    onValueChange = { viewModel.descricaoForm = it },
                                                    textStyle = LocalTextStyle.current.copy(fontSize = 17.6.sp),
                                                    placeholder = {
                                                        Text(
                                                            "Descreva detalhadamente o problema ou falha observada...",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.2.sp, color = SicoiTextMuted)
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
                                                        color = SicoiTextPrimary,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                        fontSize = 12.1.sp,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Botão 1: Anexar imagens e arquivos da galeria/dispositivo
                                                OutlinedButton(
                                                    onClick = {
                                                        requesterPhotoPickerLauncher.launch("image/*")
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, SicoiOrangeBorder),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SicoiOrange)
                                                ) {
                                                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "Anexar imagens e arquivos",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.2.sp
                                                        )
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Botão 2: Fotografar imagens diretamente pela câmera do celular
                                                Button(
                                                    onClick = {
                                                        val permissionCheck = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                                                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                                            requesterCameraPhotoLauncher.launch(null)
                                                        } else {
                                                            requesterCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = SicoiOrange,
                                                        contentColor = Color.White
                                                    )
                                                ) {
                                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "Fotografar imagens",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.2.sp
                                                        )
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

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
                        } else {
                            when (selectedTabIndex) {
                                // ══════════════════════════════════════════════
                                // ABA 0 (TÉCNICO): Informações do Solicitante
                                // ══════════════════════════════════════════════
                                0 -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // 1. MENSAGEM DO SOLICITANTE (CARD MODERNO E DESTACADO)
                                        Card(
                                            shape = RoundedCornerShape(18.dp),
                                            colors = CardDefaults.cardColors(containerColor = SicoiCard),
                                            border = BorderStroke(1.5.dp, SicoiOrange.copy(alpha = 0.8f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(18.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Cabeçalho Centralizado com Ícone e Título Maior
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .background(SicoiOrange.copy(alpha = 0.18f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.ReportProblem,
                                                            contentDescription = null,
                                                            tint = SicoiOrange,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        "Mensagem do Solicitante",
                                                        style = MaterialTheme.typography.titleLarge.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 18.sp,
                                                            letterSpacing = 0.3.sp
                                                        ),
                                                        color = SicoiOrange,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }

                                                HorizontalDivider(color = SicoiOrange.copy(alpha = 0.25f))

                                                // Caixa com o conteúdo da mensagem do solicitante
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(SicoiSurface)
                                                        .border(1.dp, SicoiOrangeBorder.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                                        .padding(16.dp)
                                                ) {
                                                    Text(
                                                        text = viewModel.descricaoForm.ifBlank { "Nenhuma descrição detalhada informada pelo solicitante." },
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            lineHeight = 23.sp
                                                        ),
                                                        color = SicoiTextPrimary
                                                    )
                                                }
                                            }
                                        }

                                        // 2. IMAGENS DA OCORRÊNCIA (CARD MODERNO E DESTACADO)
                                        val requesterPhotos = remember(viewModel.loadedPhotoAttachments, s.order) {
                                            (viewModel.loadedPhotoAttachments.map { it.url } + s.order.getPhotoUrls())
                                                .filter { it.isNotBlank() }
                                                .distinct()
                                        }

                                        Card(
                                            shape = RoundedCornerShape(18.dp),
                                            colors = CardDefaults.cardColors(containerColor = SicoiCard),
                                            border = BorderStroke(1.5.dp, SicoiBlue.copy(alpha = 0.6f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(18.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Cabeçalho Centralizado com Ícone e Título Maior
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .background(SicoiBlue.copy(alpha = 0.18f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.PhotoLibrary,
                                                            contentDescription = null,
                                                            tint = SicoiBlue,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        "Imagens da Ocorrência",
                                                        style = MaterialTheme.typography.titleLarge.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 18.sp,
                                                            letterSpacing = 0.3.sp
                                                        ),
                                                        color = SicoiBlueLight,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }

                                                HorizontalDivider(color = SicoiBlue.copy(alpha = 0.25f))

                                                if (requesterPhotos.isNotEmpty()) {
                                                    if (requesterPhotos.size == 1) {
                                                        // Foto única centralizada com visual moderno
                                                        val photoUrl = requesterPhotos.first()
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(150.dp)
                                                                    .clip(RoundedCornerShape(16.dp))
                                                                    .background(SicoiSurface)
                                                                    .border(2.dp, SicoiBlue.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                                                    .clickable { viewingImageUrl = photoUrl }
                                                            ) {
                                                                AsyncImage(
                                                                    model = photoUrl,
                                                                    contentDescription = "Imagem da Ocorrência",
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier.fillMaxSize()
                                                                )
                                                                Box(
                                                                    modifier = Modifier
                                                                        .align(Alignment.BottomEnd)
                                                                        .padding(8.dp)
                                                                        .size(28.dp)
                                                                        .background(Color.Black.copy(alpha = 0.7f), CircleShape),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.ZoomIn,
                                                                        contentDescription = "Ampliar",
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        // Múltiplas fotos centralizadas em relação à tela
                                                        LazyRow(
                                                            horizontalArrangement = Arrangement.Center,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            items(requesterPhotos) { photoUrl ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .padding(horizontal = 6.dp)
                                                                        .size(120.dp)
                                                                        .clip(RoundedCornerShape(14.dp))
                                                                        .background(SicoiSurface)
                                                                        .border(1.5.dp, SicoiBlue.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                                                        .clickable { viewingImageUrl = photoUrl }
                                                                ) {
                                                                    AsyncImage(
                                                                        model = photoUrl,
                                                                        contentDescription = "Imagem da Ocorrência",
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.fillMaxSize()
                                                                    )
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .align(Alignment.BottomEnd)
                                                                            .padding(6.dp)
                                                                            .size(24.dp)
                                                                            .background(Color.Black.copy(alpha = 0.7f), CircleShape),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            Icons.Default.ZoomIn,
                                                                            contentDescription = "Ampliar",
                                                                            tint = Color.White,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        "Toque na miniatura para abrir e ampliar a imagem",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                        color = SicoiTextMuted,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                } else {
                                                    // Estado vazio refinado
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(SicoiSurface)
                                                            .border(1.dp, SicoiDivider.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                                            .padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.ImageNotSupported,
                                                            contentDescription = null,
                                                            tint = SicoiTextMuted,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            "Nenhuma imagem anexada nesta O.S.",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = SicoiTextMuted,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // 3. DADOS GERAIS DA SOLICITAÇÃO (CARD MODERNO E DESTACADO)
                                        Card(
                                            shape = RoundedCornerShape(18.dp),
                                            colors = CardDefaults.cardColors(containerColor = SicoiCard),
                                            border = BorderStroke(1.5.dp, SicoiSuccess.copy(alpha = 0.5f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(18.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Cabeçalho Centralizado com Ícone e Título Maior
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .background(SicoiSuccess.copy(alpha = 0.18f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Info,
                                                            contentDescription = null,
                                                            tint = SicoiSuccess,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        "Dados Gerais da Solicitação",
                                                        style = MaterialTheme.typography.titleLarge.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 18.sp,
                                                            letterSpacing = 0.3.sp
                                                        ),
                                                        color = SicoiTextPrimary,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }

                                                HorizontalDivider(color = SicoiSuccess.copy(alpha = 0.25f))

                                                ReadOnlyOSField(
                                                    label = "Equipamento",
                                                    value = viewModel.equipamentoForm.ifBlank { "Não informado" },
                                                    icon = Icons.Default.PrecisionManufacturing
                                                )
                                                ReadOnlyOSField(
                                                    label = "Setor / Localização",
                                                    value = viewModel.setorForm.ifBlank { "Não informado" },
                                                    icon = Icons.Default.Business
                                                )
                                                ReadOnlyOSField(
                                                    label = "Solicitante",
                                                    value = viewModel.solicitanteForm.ifBlank { "Não informado" },
                                                    icon = Icons.Default.Person
                                                )

                                                if (viewModel.patrimonioForm.isNotBlank()) {
                                                    ReadOnlyOSField(
                                                        label = "Número do Patrimônio (Tag)",
                                                        value = viewModel.patrimonioForm,
                                                        icon = Icons.Default.ConfirmationNumber
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        ReadOnlyOSField(
                                                            label = "Prioridade",
                                                            value = viewModel.prioridadeForm,
                                                            icon = Icons.Default.PriorityHigh
                                                        )
                                                    }
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        ReadOnlyOSField(
                                                            label = "Data de Abertura",
                                                            value = viewModel.dateForm,
                                                            icon = Icons.Default.DateRange
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // ══════════════════════════════════════════════
                                // ABA 1 (TÉCNICO): Execução do Técnico
                                // ══════════════════════════════════════════════
                                1 -> {
                                    TechnicianExecutionSection(
                                        viewModel = viewModel,
                                        serviceBitmaps = servicePhotoBitmaps,
                                        onServiceBitmapsChange = { servicePhotoBitmaps = it },
                                        materialBitmaps = materialPhotoBitmaps,
                                        onMaterialBitmapsChange = { materialPhotoBitmaps = it },
                                        onViewImage = { viewingImageUrl = it },
                                        onViewBitmap = { viewingBitmap = it },
                                        onRequestAttach = { section ->
                                            if (section == "service") {
                                                servicePhotoPickerLauncher.launch("image/*")
                                            } else {
                                                materialPhotoPickerLauncher.launch("image/*")
                                            }
                                        },
                                        onRequestCamera = { section ->
                                            if (section == "service") {
                                                val permissionCheck = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                                    serviceCameraPhotoLauncher.launch(null)
                                                } else {
                                                    serviceCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                                }
                                            } else {
                                                val permissionCheck = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                                    materialCameraPhotoLauncher.launch(null)
                                                } else {
                                                    materialCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // ─── Botões de Ação ───────────────────────────────────
                        // O botão Salvar Formulário deve aparecer APENAS na aba 1 (Execução do Técnico / Dados do Equipamento)
                        if (selectedTabIndex == 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Botão Salvar Formulário (Caixa branca com texto em Preto)
                                Button(
                                    onClick = {
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                        val stf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                        val now = java.util.Date()
                                        viewModel.finalDate = sdf.format(now)
                                        viewModel.finalHour = stf.format(now)
                                        showConfirmDialog = true
                                    },
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.5.dp, SicoiOrange),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color.Black
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, tint = SicoiOrange, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Salvar Formulário",
                                        color = Color.Black,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.5.sp
                                        )
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
                    }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            is OSFormUiState.SavedOffline, is OSFormUiState.SavedOnline -> {
                // Redirecionamento e Toast tratados no LaunchedEffect
            }

            else -> {}
        }

        if (viewingImageUrl != null || viewingBitmap != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {
                    viewingImageUrl = null
                    viewingBitmap = null
                },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.95f)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    viewingImageUrl = null
                                    viewingBitmap = null
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                            }
                            Text(
                                "Visualização da Foto",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewingBitmap != null) {
                                Image(
                                    bitmap = viewingBitmap!!.asImageBitmap(),
                                    contentDescription = "Foto Expandida",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                )
                            } else if (viewingImageUrl != null) {
                                AsyncImage(
                                    model = viewingImageUrl,
                                    contentDescription = "Foto Expandida",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo para visualização de Imagens do Solicitante (Grade/LazyColumn)
    if (showImagesDialog && viewModel.loadedPhotoAttachments.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showImagesDialog = false },
            title = {
                Text(
                    "Imagens do Solicitante",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SicoiTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Fotos anexadas na abertura da O.S.:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SicoiTextMuted
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(viewModel.loadedPhotoAttachments) { file ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = SicoiSurface),
                                border = BorderStroke(1.dp, SicoiDivider),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = file.url,
                                        contentDescription = file.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(file.url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("OSFormScreen", "Erro ao abrir imagem: ${e.message}")
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showImagesDialog = false }
                ) {
                    Text("Fechar", color = SicoiOrange, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SicoiCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
    } // End of ModalNavigationDrawer
}

@Composable
fun CentralDoSolicitanteContent(
    isRequesterMode: Boolean,
    viewModel: OSFormViewModel,
    expandedCardIds: MutableMap<String, Boolean>
) {
    if (isRequesterMode) {
        Spacer(modifier = Modifier.height(8.dp))

        // Cabeçalho Premium com Alto Destaque para "Central do Solicitante"
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E2638),
            border = BorderStroke(1.dp, SicoiBlue.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        "Central do Solicitante",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            letterSpacing = 0.3.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }

        if (!viewModel.loadingHistory && viewModel.allWorkOrders.isNotEmpty()) {
            val openCount = viewModel.allWorkOrders.count { !(it.status.trim().equals("Finalizada", ignoreCase = true) || it.status.trim().equals("Finalizado", ignoreCase = true)) }
            val closedCount = viewModel.allWorkOrders.count { it.status.trim().equals("Finalizada", ignoreCase = true) || it.status.trim().equals("Finalizado", ignoreCase = true) }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Badge Abertas
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SicoiOrange.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SicoiOrangeBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SicoiOrange))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Abertas: $openCount",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = SicoiOrange
                        )
                    }
                }

                // Badge Finalizadas
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SicoiSuccess.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SicoiSuccess.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SicoiSuccess))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Finalizadas: $closedCount",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = SicoiSuccess
                        )
                    }
                }
            }
        }

        if (viewModel.loadingHistory) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SicoiOrange)
            }
        } else if (viewModel.allWorkOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, SicoiDivider), RoundedCornerShape(14.dp))
                    .background(SicoiSurface)
                    .padding(vertical = 28.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nenhuma solicitação encontrada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SicoiTextMuted,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.allWorkOrders.forEach { item ->
                    OSHistoryCard(
                        item = item,
                        isExpanded = expandedCardIds[item.id] == true,
                        onToggleExpand = { expandedCardIds[item.id] = !(expandedCardIds[item.id] == true) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechnicianExecutionSection(
    viewModel: OSFormViewModel,
    serviceBitmaps: List<Bitmap>,
    onServiceBitmapsChange: (List<Bitmap>) -> Unit,
    materialBitmaps: List<Bitmap>,
    onMaterialBitmapsChange: (List<Bitmap>) -> Unit,
    onViewImage: (String) -> Unit = {},
    onViewBitmap: (Bitmap) -> Unit = {},
    onRequestAttach: (String) -> Unit,
    onRequestCamera: (String) -> Unit
) {
    val context = LocalContext.current

    // ── CARD 1: RELATÓRIO TÉCNICO (PRIMEIRO QUADRO COM TÍTULO CENTRALIZADO) ──
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.5.dp, SicoiOrange.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Título Centralizado acima
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SicoiOrange.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = SicoiOrange, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Relatório Técnico",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = SicoiOrange,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(color = SicoiOrange.copy(alpha = 0.25f))

            OutlinedTextField(
                value = viewModel.descriptionExecuted,
                onValueChange = { viewModel.descriptionExecuted = it },
                placeholder = { Text("Relate o que foi feito para solucionar o problema...", style = MaterialTheme.typography.bodyMedium.copy(color = SicoiTextMuted)) },
                label = { Text("Descrição do Serviço Executado") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 4,
                colors = sicoiTextFieldColors()
            )

            HorizontalDivider(color = SicoiDivider)

            Text(
                "Fotos / Comprovantes de Serviço Executado (Opcional)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SicoiTextSecondary,
                    letterSpacing = 0.5.sp
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botão 1: Anexar
                OutlinedButton(
                    onClick = { onRequestAttach("service") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SicoiOrange.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SicoiOrange)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Anexar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                // Botão 2: Câmera
                Button(
                    onClick = { onRequestCamera("service") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tirar Foto", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            if (serviceBitmaps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(serviceBitmaps.size) { index ->
                        val bmp = serviceBitmaps[index]
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(84.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, SicoiOrange.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .clickable { onViewBitmap(bmp) }
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Foto ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Ícone de Zoom translúcido
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Ampliar", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            // Botão de remover
                            IconButton(
                                onClick = {
                                    onServiceBitmapsChange(serviceBitmaps.toMutableList().also { it.removeAt(index) })
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(22.dp)
                                    .background(SicoiError.copy(alpha = 0.9f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remover", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
                Text(
                    "${serviceBitmaps.size} foto(s) anexada(s) • Toque para ampliar",
                    style = MaterialTheme.typography.labelSmall,
                    color = SicoiTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // ── CARD 2: APONTAMENTO DE PAUSAS & SERVIÇO EXTERNO (QUADRO ABAIXO) ──
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.dp, SicoiOrangeBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Apontamento de Pausas (Título 15% maior e alinhado perfeitamente com o ícone) ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SicoiOrange.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = SicoiOrange, modifier = Modifier.size(22.dp))
                }
                Text(
                    "Apontamento de Pausas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    ),
                    color = SicoiTextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Botão de Toggle Pausa
                val isPaused = viewModel.pauseState == "active"
                Button(
                    onClick = {
                        if (isPaused) {
                            if (viewModel.pauseReason.isNotBlank()) {
                                val date = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                viewModel.pauseObservations.add("$date - ${viewModel.pauseReason}")
                                viewModel.pauseReason = ""
                            }
                            viewModel.pauseState = "inactive"
                        } else {
                            viewModel.pauseState = "active"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaused) SicoiError else SicoiOrange
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (isPaused) "Pausa Ativada" else "Ativar Pausa",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            }

            if (viewModel.pauseState == "active") {
                OutlinedTextField(
                    value = viewModel.pauseReason,
                    onValueChange = { viewModel.pauseReason = it },
                    placeholder = { Text("Motivo / Razão da Pausa...", style = MaterialTheme.typography.bodyMedium.copy(color = SicoiTextMuted)) },
                    label = { Text("Motivo da Pausa *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    colors = sicoiTextFieldColors()
                )
            }

            if (viewModel.pauseObservations.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Histórico de Pausas:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SicoiTextSecondary)
                    )
                    viewModel.pauseObservations.forEach { obs ->
                        val regex = Regex("^(.*?) \\[Anexo: (https?://.*?)\\]$")
                        val matchResult = regex.matchEntire(obs)
                        val text = matchResult?.groups?.get(1)?.value ?: obs
                        val attachmentUrl = matchResult?.groups?.get(2)?.value

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SicoiSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, SicoiDivider, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall.copy(color = SicoiTextMuted)
                            )
                            if (attachmentUrl != null) {
                                val isImage = attachmentUrl.endsWith(".jpg", ignoreCase = true) || 
                                              attachmentUrl.endsWith(".jpeg", ignoreCase = true) || 
                                              attachmentUrl.endsWith(".png", ignoreCase = true) || 
                                              attachmentUrl.endsWith(".webp", ignoreCase = true) || 
                                              attachmentUrl.endsWith(".gif", ignoreCase = true)
                                
                                if (isImage) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .border(1.dp, SicoiDivider, RoundedCornerShape(4.dp))
                                            .clickable {
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(attachmentUrl))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("OSFormScreen", "Erro ao abrir imagem: ${e.message}")
                                                }
                                            }
                                    ) {
                                        AsyncImage(
                                            model = attachmentUrl,
                                            contentDescription = "Anexo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "📎 Ver Arquivo Anexo",
                                        style = MaterialTheme.typography.bodySmall.copy(color = SicoiOrange, fontWeight = FontWeight.Bold),
                                        modifier = Modifier.clickable {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(attachmentUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                android.util.Log.e("OSFormScreen", "Erro ao abrir anexo: ${e.message}")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = SicoiDivider)

            // ── Serviço Externo (Título 15% maior e alinhado perfeitamente com o ícone) ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SicoiBlue.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = SicoiBlue, modifier = Modifier.size(22.dp))
                }
                Text(
                    "Serviço Externo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    ),
                    color = SicoiTextPrimary
                )
            }

            Text(
                "Necessidade de Serviço Externo?",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = SicoiTextSecondary,
                    fontWeight = FontWeight.Bold
                )
            )

            val isExternal = viewModel.externalService == "sim"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.externalService = "sim" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExternal) SicoiBlue else SicoiSurface
                    ),
                    border = BorderStroke(1.dp, if (isExternal) SicoiBlue else SicoiDivider),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sim", color = if (isExternal) Color.White else SicoiTextSecondary)
                }
                Button(
                    onClick = {
                        viewModel.externalService = "nao"
                        viewModel.externalJustification = ""
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isExternal) SicoiBlue else SicoiSurface
                    ),
                    border = BorderStroke(1.dp, if (!isExternal) SicoiBlue else SicoiDivider),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Não", color = if (!isExternal) Color.White else SicoiTextSecondary)
                }
            }

            // Campos extras que aparecem ao selecionar "Sim"
            if (isExternal) {
                // Justificativa do Técnico
                OutlinedTextField(
                    value = viewModel.externalJustification,
                    onValueChange = { viewModel.externalJustification = it },
                    placeholder = { Text("Descreva o motivo da necessidade...", style = MaterialTheme.typography.bodyMedium.copy(color = SicoiTextMuted)) },
                    label = { Text("Justificativa do Técnico") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2,
                    colors = sicoiTextFieldColors()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // ── CARD 3: MATERIAIS UTILIZADOS (COMPACTO NA VERTICAL QUANDO VAZIO) ──
    val hasMaterialsOrPhotos = viewModel.materialsList.isNotEmpty() || materialBitmaps.isNotEmpty()
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.dp, SicoiSuccess.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(if (hasMaterialsOrPhotos) 18.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (hasMaterialsOrPhotos) 12.dp else 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(SicoiSuccess.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = SicoiSuccess, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        "Material Utilizado",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = SicoiTextPrimary
                    )
                }
                
                // Botão Adicionar Linha
                Button(
                    onClick = {
                        viewModel.materialsList.add(MaterialItem(qty = "", description = "", price = ""))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SicoiSuccess, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adicionar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            // Conteúdo expandido apenas se houver itens ou fotos
            if (hasMaterialsOrPhotos) {
                HorizontalDivider(color = SicoiDivider)

                viewModel.materialsList.forEachIndexed { index, material ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SicoiSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, SicoiDivider, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Qtd
                        OutlinedTextField(
                            value = material.qty,
                            onValueChange = { qty ->
                                viewModel.materialsList[index] = material.copy(qty = qty)
                            },
                            placeholder = { Text("Qtd", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)) },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            colors = sicoiTextFieldColors()
                        )

                        // Descrição
                        OutlinedTextField(
                            value = material.description,
                            onValueChange = { desc ->
                                viewModel.materialsList[index] = material.copy(description = desc)
                            },
                            placeholder = { Text("Descrição", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)) },
                            modifier = Modifier.weight(3.5f),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            colors = sicoiTextFieldColors()
                        )

                        // Preço
                        OutlinedTextField(
                            value = material.price,
                            onValueChange = { price ->
                                viewModel.materialsList[index] = material.copy(price = price)
                            },
                            placeholder = { Text("Preço", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)) },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            colors = sicoiTextFieldColors()
                        )

                        // Delete
                        IconButton(
                            onClick = { viewModel.materialsList.removeAt(index) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover", tint = SicoiError, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = SicoiDivider)
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Fotos / Comprovantes de Insumos (Opcional)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SicoiTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botão 1: Anexar
                    OutlinedButton(
                        onClick = { onRequestAttach("material") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SicoiSuccess.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SicoiSuccess)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Anexar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    // Botão 2: Câmera
                    Button(
                        onClick = { onRequestCamera("material") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SicoiSuccess, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tirar Foto", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (materialBitmaps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(materialBitmaps.size) { index ->
                            val bmp = materialBitmaps[index]
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, SicoiSuccess.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                    .clickable { onViewBitmap(bmp) }
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Foto ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Ícone de Zoom translúcido
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Ampliar", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                                // Botão de remover
                                IconButton(
                                    onClick = {
                                        onMaterialBitmapsChange(materialBitmaps.toMutableList().also { it.removeAt(index) })
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .background(SicoiError.copy(alpha = 0.85f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remover", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                    Text(
                        "Toque na miniatura para ampliar",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = SicoiTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // ── CARD 4: ENCERRAMENTO DA O.S. (TÍTULO CENTRALIZADO E DATAS PREENCHIDAS) ──
    val displayFinalDate = viewModel.finalDate.ifBlank {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()).also {
            viewModel.finalDate = it
        }
    }
    val displayFinalHour = viewModel.finalHour.ifBlank {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()).also {
            viewModel.finalHour = it
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.dp, SicoiBlue.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Título Centralizado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SicoiBlue.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SicoiBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Encerramento da O.S.",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = SicoiBlueLight,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(color = SicoiBlue.copy(alpha = 0.25f))

            // Data e hora de encerramento — preenchidas automaticamente com a data e hora atuais
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = displayFinalDate,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("YYYY-MM-DD", style = MaterialTheme.typography.bodyMedium.copy(color = SicoiTextMuted)) },
                    label = { Text("Data Final") },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = SicoiBlue, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = sicoiTextFieldColors()
                )

                OutlinedTextField(
                    value = displayFinalHour,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("HH:MM", style = MaterialTheme.typography.bodyMedium.copy(color = SicoiTextMuted)) },
                    label = { Text("Hora") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = SicoiBlue, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = sicoiTextFieldColors()
                )
            }

            OutlinedTextField(
                value = viewModel.vistoExecutante,
                onValueChange = { viewModel.vistoExecutante = it },
                placeholder = { Text("Nome do Técnico", style = MaterialTheme.typography.bodyMedium.copy(color = SicoiTextMuted)) },
                label = { Text("Visto Executante") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = sicoiTextFieldColors()
            )
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
    isTab0: Boolean = false,
    isTab1: Boolean = false
) {
    val scaleLabel = if (isTab0) 1.1f else if (isTab1) 1.1f else 1.0f
    val scaleValue = if (isTab0) 1.15f else if (isTab1) 1.1f else 1.0f
    val weight = if (isTab0) androidx.compose.ui.text.font.FontWeight.Bold else if (isTab1) androidx.compose.ui.text.font.FontWeight.Bold else null
    val defaultLabelSize = 11.sp
    val defaultBodySize = 16.sp

    val labelColor = if (isTab1) SicoiTextPrimary else SicoiTextSecondary
    val labelWeight = if (isTab1) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal

    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = labelColor, 
                letterSpacing = 0.5.sp, 
                fontSize = defaultLabelSize * scaleLabel,
                fontWeight = labelWeight
            )
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

@Composable
private fun OSHistoryCard(
    item: WorkOrder,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val isFinished = item.status.trim().equals("Finalizada", ignoreCase = true) || item.status.trim().equals("Finalizado", ignoreCase = true)
    val isPaused = item.status.trim().equals("Pausada", ignoreCase = true) || item.status.trim().equals("Pausado", ignoreCase = true)
    
    val displayStatus = when {
        isFinished -> "Finalizada"
        isPaused -> "Pausada"
        else -> "Em Aberto"
    }
    
    val statusColor = when {
        isFinished -> SicoiSuccess
        isPaused -> SicoiWarning
        else -> SicoiOrange
    }

    val finalOsNumber = item.getFullNumeroOs()
    val finalEquipment = item.getFullEquipment()
    val finalPatrimonio = item.getFullPatrimonio()
    val finalTechnician = item.getFullTechnician()
    val finalDescriptionExecuted = item.getFullDescriptionExecuted()
    val finalCompletionDateTime = item.getFullFinalDateTime()
    val descriptionToExecute = item.descricaoProblema?.ifBlank { "Sem descrição informada" } ?: "Sem descrição informada"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D)),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val rawOsNumber = finalOsNumber
                .replace("OS", "", ignoreCase = true)
                .replace("O.S.", "", ignoreCase = true)
                .replace("#", "")
                .replace("º", "")
                .replace("nº", "", ignoreCase = true)
                .trim()
            val displayOsTitle = if (rawOsNumber.isNotBlank()) "OS nº$rawOsNumber" else "OS Sem Número"

            // Linha Superior: Badge O.S. + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge O.S.
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF282D3C),
                    border = BorderStroke(1.dp, Color(0xFF3B4358))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayOsTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }
                
                // Status Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = displayStatus.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Equipamento em destaque
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SicoiBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = SicoiBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Equipamento",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = SicoiTextMuted
                    )
                    Text(
                        text = finalEquipment,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Linha com Chips de Patrimônio e Técnico
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chip Patrimônio
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF252A36),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.Tag,
                            contentDescription = null,
                            tint = SicoiTextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Column {
                            Text("Patrimônio", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SicoiTextMuted)
                            Text(
                                finalPatrimonio,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                color = Color.White
                            )
                        }
                    }
                }

                // Chip Técnico
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF252A36),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = if (finalTechnician != "Não atribuído") SicoiOrange else SicoiTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Column {
                            Text("Técnico", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SicoiTextMuted)
                            Text(
                                finalTechnician,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = if (finalTechnician != "Não atribuído") Color.White else SicoiTextMuted
                            )
                        }
                    }
                }
            }

            // Bloco de Comentário / Execução / Detalhes
            Spacer(modifier = Modifier.height(12.dp))

            if (isFinished) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SicoiSuccess.copy(alpha = 0.08f))
                        .border(1.dp, SicoiSuccess.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SicoiSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Serviço Executado pelo Técnico:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = SicoiSuccess
                        )
                    }
                    Text(
                        text = finalDescriptionExecuted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        color = SicoiTextPrimary
                    )
                    HorizontalDivider(color = SicoiSuccess.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = SicoiSuccess, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Finalizado em: ${finalCompletionDateTime ?: item.dataFim ?: "Data não informada"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = SicoiTextPrimary
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SicoiOrange.copy(alpha = 0.08f))
                        .border(1.dp, SicoiOrange.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = SicoiOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Motivo da Solicitação:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = SicoiOrange
                        )
                    }
                    Text(
                        text = descriptionToExecute,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        color = SicoiTextPrimary
                    )
                    HorizontalDivider(color = SicoiOrange.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = SicoiTextMuted, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Aberta em: ${item.dataAbertura ?: "—"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = SicoiTextMuted
                            )
                        }
                        Text(
                            text = if (isPaused) "Pausada" else "Aguardando atendimento",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = if (isPaused) SicoiWarning else SicoiOrange
                        )
                    }
                }
            }
        }
    }
}

// Helper para decodificação segura de Bitmaps evitando OutOfMemoryError no Android
private fun decodeUriAsScaledBitmap(context: android.content.Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
    return try {
        // 1. Abre e lê todos os bytes da imagem do ContentResolver em uma única leitura
        val bytes = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: return null

        // 2. Obtém as dimensões originais da imagem a partir dos bytes (sem decodificar na RAM)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        val width = options.outWidth
        val height = options.outHeight
        if (width <= 0 || height <= 0) return null

        // 3. Calcula o fator de escala de amostragem (inSampleSize)
        var inSampleSize = 1
        if (width > maxDimension || height > maxDimension) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / inSampleSize) >= maxDimension && (halfHeight / inSampleSize) >= maxDimension) {
                inSampleSize *= 2
            }
        }

        // 4. Decodifica efetivamente o bitmap a partir dos bytes na escala calculada
        options.apply {
            inJustDecodeBounds = false
            this.inSampleSize = inSampleSize
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
