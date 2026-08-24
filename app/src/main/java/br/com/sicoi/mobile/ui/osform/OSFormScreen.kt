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
    var afterBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Fotos dos solicitante (lista de bitmaps)
    var photoBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var servicePhotoBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var materialPhotoBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var activeAttachmentSection by remember { mutableStateOf("requester") }

    // Estados para controle de exibição do histórico
    var isGridView by remember { mutableStateOf(false) }
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
                                    setor = "",
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
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SicoiOrange.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SicoiOrangeBorder),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (isNewOrder) Icons.Default.AddCircle else Icons.Default.Tag,
                                contentDescription = null,
                                tint = SicoiOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                osNumber,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SicoiOrange
                                )
                            )
                        }
                    }

                    if (!isRequesterMode && viewModel.loadedPhotoAttachments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "FOTOS DO SOLICITANTE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold, 
                                color = SicoiOrange,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = "Anexos originais da abertura da OS:",
                            style = MaterialTheme.typography.bodySmall.copy(color = SicoiTextMuted, fontSize = 10.5.sp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        viewModel.loadedPhotoAttachments.forEachIndexed { index, file ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = SicoiSurface),
                                border = BorderStroke(1.dp, SicoiDivider),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    AsyncImage(
                                        model = file.url,
                                        contentDescription = file.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Anexo ${index + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    CentralDoSolicitanteContent(
                        isRequesterMode = isRequesterMode,
                        viewModel = viewModel,
                        isGridView = isGridView,
                        onGridViewChange = { isGridView = it },
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
                    if (isRequesterMode) {
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
                                                    Text("Dados do Solicitante", style = MaterialTheme.typography.titleMedium, color = SicoiTextPrimary)
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

                                            // Data de Abertura
                                            ReadOnlyOSField(
                                                label = "Data de Abertura (automático)",
                                                value = viewModel.dateForm,
                                                icon = Icons.Default.DateRange,
                                                isTab0 = true
                                            )

                                            // Hora
                                            ReadOnlyOSField(
                                                label = "Hora (automático)",
                                                value = viewModel.timeForm,
                                                icon = Icons.Default.Schedule,
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

                                            // Prioridade — Seleção com chips
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
                                                        FilterChip(
                                                            selected = isSelected,
                                                            onClick = { viewModel.prioridadeForm = label },
                                                            label = {
                                                                Text(
                                                                    label,
                                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                        fontSize = 13.2.sp
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
                            // ── Subtítulo "Intervenções" ──────────────────────
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(SicoiOrange, shape = RoundedCornerShape(2.dp))
                                )
                                Text(
                                    "Intervenções",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp
                                    ),
                                    color = SicoiOrange
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = SicoiOrange.copy(alpha = 0.3f)
                                )
                            }
                            TechnicianExecutionSection(
                                viewModel = viewModel,
                                serviceBitmaps = servicePhotoBitmaps,
                                onServiceBitmapsChange = { servicePhotoBitmaps = it },
                                materialBitmaps = materialPhotoBitmaps,
                                onMaterialBitmapsChange = { materialPhotoBitmaps = it },
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
    } // End of ModalNavigationDrawer
}

@Composable
fun CentralDoSolicitanteContent(
    isRequesterMode: Boolean,
    viewModel: OSFormViewModel,
    isGridView: Boolean,
    onGridViewChange: (Boolean) -> Unit,
    expandedCardIds: MutableMap<String, Boolean>
) {
    if (isRequesterMode) {
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = SicoiDivider)
        Spacer(modifier = Modifier.height(16.dp))

        // Cabeçalho da seção de histórico com opção Quadro/Linha
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SicoiBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = SicoiBlue, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Central do Solicitante", style = MaterialTheme.typography.titleMedium, color = SicoiTextPrimary)
                    Text("Acompanhe o andamento das solicitações", style = MaterialTheme.typography.bodySmall, color = SicoiTextMuted)
                }
            }

            // Seletor Linha / Quadro
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    false to "Linha",
                    true to "Quadro"
                ).forEach { (gridMode, label) ->
                    val isSel = isGridView == gridMode
                    Surface(
                        modifier = Modifier.clickable { onGridViewChange(gridMode) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) SicoiOrange.copy(alpha = 0.2f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSel) SicoiOrangeBorder else Color.Transparent)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) SicoiOrange else SicoiTextMuted
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (!viewModel.loadingHistory && viewModel.allWorkOrders.isNotEmpty()) {
            val openCount = viewModel.allWorkOrders.count { !(it.status.trim().equals("Finalizada", ignoreCase = true) || it.status.trim().equals("Finalizado", ignoreCase = true)) }
            val closedCount = viewModel.allWorkOrders.count { it.status.trim().equals("Finalizada", ignoreCase = true) || it.status.trim().equals("Finalizado", ignoreCase = true) }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Badge Abertas
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SicoiOrange.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SicoiOrangeBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SicoiOrange))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Abertas: $openCount",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SicoiOrange
                        )
                    }
                }

                // Badge Finalizadas
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SicoiSuccess.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, SicoiSuccess.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SicoiSuccess))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Finalizadas: $closedCount",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SicoiSuccess
                        )
                    }
                }
            }
        }

        if (viewModel.loadingHistory) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SicoiOrange)
            }
        } else if (viewModel.allWorkOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, SicoiDivider), RoundedCornerShape(12.dp))
                    .background(SicoiSurface)
                    .padding(vertical = 24.dp, horizontal = 16.dp),
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
            if (isGridView) {
                val chunks = viewModel.allWorkOrders.chunked(2)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    chunks.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    OSHistoryCard(
                                        item = item,
                                        isExpanded = expandedCardIds[item.id] == true,
                                        onToggleExpand = { expandedCardIds[item.id] = !(expandedCardIds[item.id] == true) }
                                    )
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechnicianExecutionSection(
    viewModel: OSFormViewModel,
    serviceBitmaps: List<Bitmap>,
    onServiceBitmapsChange: (List<Bitmap>) -> Unit,
    materialBitmaps: List<Bitmap>,
    onMaterialBitmapsChange: (List<Bitmap>) -> Unit,
    onRequestAttach: (String) -> Unit,
    onRequestCamera: (String) -> Unit
) {
    val context = LocalContext.current

    // Fotos anexadas pelo solicitante na abertura da OS (Exibidas para o Técnico)
    if (viewModel.loadedPhotoAttachments.isNotEmpty()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SicoiCard),
            border = BorderStroke(1.dp, SicoiOrangeBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(SicoiOrange.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = SicoiOrange, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Fotos do Solicitante", style = MaterialTheme.typography.titleMedium, color = SicoiTextPrimary)
                        Text("Fotos anexadas durante a abertura desta OS", style = MaterialTheme.typography.bodySmall, color = SicoiTextMuted)
                    }
                }

                HorizontalDivider(color = SicoiDivider)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.loadedPhotoAttachments) { file ->
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, SicoiCardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    Toast.makeText(context, "Visualizando foto do solicitante", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            AsyncImage(
                                model = file.url,
                                contentDescription = file.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
    
    // Card 1: Controle de Pausa da O.S. + Serviço Externo
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.dp, SicoiOrangeBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Cabeçalho: Apontamento de Pausas ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SicoiOrange.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = SicoiOrange, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Apontamento de Pausas", style = MaterialTheme.typography.titleMedium, color = SicoiTextPrimary)
                    Text("Pausar a execução da ordem de serviço", style = MaterialTheme.typography.bodySmall, color = SicoiTextMuted)
                }
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
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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
                                val context = androidx.compose.ui.platform.LocalContext.current
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

            // ── Sub-seção: Serviço Externo ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SicoiBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = SicoiBlue, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Serviço Externo", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = SicoiTextPrimary)
                    Text("Necessidade de intervenção externa", style = MaterialTheme.typography.bodySmall, color = SicoiTextMuted)
                }
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

    // Card 3: Materiais Utilizados
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.dp, SicoiSuccess.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(SicoiSuccess.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = SicoiSuccess, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Material Utilizado", style = MaterialTheme.typography.titleMedium, color = SicoiTextPrimary)
                        Text("Peças e insumos aplicados", style = MaterialTheme.typography.bodySmall, color = SicoiTextMuted)
                    }
                }
                
                // Botão Adicionar Linha
                OutlinedButton(
                    onClick = {
                        viewModel.materialsList.add(MaterialItem(qty = "", description = "", price = ""))
                    },
                    border = BorderStroke(1.dp, SicoiSuccess),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SicoiSuccess),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adicionar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            HorizontalDivider(color = SicoiDivider)

            if (viewModel.materialsList.isEmpty()) {
                Text(
                    "Nenhum material adicionado.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = SicoiTextMuted),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            } else {
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(materialBitmaps.size) { index ->
                        Box(modifier = Modifier.size(64.dp)) {
                            Image(
                                bitmap = materialBitmaps[index].asImageBitmap(),
                                contentDescription = "Foto ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, SicoiCardBorder, RoundedCornerShape(8.dp))
                            )
                            IconButton(
                                onClick = {
                                    onMaterialBitmapsChange(materialBitmaps.toMutableList().also { it.removeAt(index) })
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .background(SicoiError.copy(alpha = 0.85f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remover", tint = Color.White, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }
                Text(
                    "${materialBitmaps.size} foto(s) selecionada(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SicoiTextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Card 4: Descrição do Serviço Executado
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.dp, SicoiOrange.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    Text("Serviço Executado", style = MaterialTheme.typography.titleMedium, color = SicoiTextPrimary)
                    Text("Detalhamento da solução aplicada", style = MaterialTheme.typography.bodySmall, color = SicoiTextMuted)
                }
            }

            HorizontalDivider(color = SicoiDivider)

            OutlinedTextField(
                value = viewModel.descriptionExecuted,
                onValueChange = { viewModel.descriptionExecuted = it },
                placeholder = { Text("Relate o que foi feito para solucionar o problema...", style = MaterialTheme.typography.bodyMedium.copy(color = SicoiTextMuted)) },
                label = { Text("Descrição do Serviço Executado") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                minLines = 4,
                colors = sicoiTextFieldColors()
            )

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = SicoiDivider)
            Spacer(modifier = Modifier.height(4.dp))

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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(serviceBitmaps.size) { index ->
                        Box(modifier = Modifier.size(64.dp)) {
                            Image(
                                bitmap = serviceBitmaps[index].asImageBitmap(),
                                contentDescription = "Foto ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, SicoiCardBorder, RoundedCornerShape(8.dp))
                            )
                            IconButton(
                                onClick = {
                                    onServiceBitmapsChange(serviceBitmaps.toMutableList().also { it.removeAt(index) })
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .background(SicoiError.copy(alpha = 0.85f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remover", tint = Color.White, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }
                Text(
                    "${serviceBitmaps.size} foto(s) selecionada(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SicoiTextMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Card 5: Encerramento
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.dp, SicoiBlue.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(SicoiBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SicoiBlue, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Encerramento da O.S.", style = MaterialTheme.typography.titleMedium, color = SicoiTextPrimary)
                    Text("Data, hora e visto do técnico executor", style = MaterialTheme.typography.bodySmall, color = SicoiTextMuted)
                }
            }

            HorizontalDivider(color = SicoiDivider)

            // Data e hora de encerramento — preenchidas automaticamente com o momento atual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.finalDate,
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
                    value = viewModel.finalHour,
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
            Text(
                "⏱ Preenchido automaticamente no momento de salvar",
                style = MaterialTheme.typography.labelSmall,
                color = SicoiTextMuted,
                modifier = Modifier.padding(top = 2.dp)
            )

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
    val displayStatus = if (isFinished) "Finalizada" else "Em Aberto"
    val borderCol = if (isFinished) SicoiSuccess.copy(alpha = 0.4f) else SicoiOrange.copy(alpha = 0.4f)
    
    // Busca o JSON decodificado da solucao_aplicada (se RQ-11 digital)
    var equipmentNo = "Não informado"
    var descriptionToExecute = item.descricaoProblema ?: ""
    var descriptionExecuted = "Sem descrição cadastrada"
    var dateFinished = "N/A"
    
    item.solucaoAplicada?.let { sol ->
        if (sol.startsWith("[RQ-11-DIGITAL]:")) {
            try {
                val jsonStr = sol.removePrefix("[RQ-11-DIGITAL]:").trim()
                val payload = Json.decodeFromString<OSExecutionPayload>(jsonStr)
                equipmentNo = payload.equipmentNo.ifBlank { "Não informado" }
                if (payload.descriptionToExecute.isNotBlank()) {
                    descriptionToExecute = payload.descriptionToExecute
                }
                if (payload.descriptionExecuted.isNotBlank()) {
                    descriptionExecuted = payload.descriptionExecuted
                }
                if (payload.finalDate.isNotBlank()) {
                    dateFinished = "${payload.finalDate} às ${payload.finalHour}"
                }
            } catch (e: Exception) {
                android.util.Log.e("OSFormScreen", "Erro ao decodificar JSON do histórico: ${e.message}")
            }
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top row: OS Number + Status
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OS: #${item.numeroOs ?: item.id.take(8).uppercase()}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = SicoiTextPrimary
                )
                
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = (if (isFinished) SicoiSuccess else SicoiOrange).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, (if (isFinished) SicoiSuccess else SicoiOrange).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = displayStatus,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isFinished) SicoiSuccess else SicoiOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = SicoiDivider, modifier = Modifier.padding(bottom = 8.dp))

            // Detalhes Gerais
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Equipamento:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = SicoiTextMuted)
                    Text(item.equipamento ?: "Equipamento Geral", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = SicoiTextPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Patrimônio:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = SicoiTextMuted)
                    Text(equipmentNo, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = SicoiTextPrimary)
                }
            }

            // Expanded content section
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                if (isFinished) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SicoiSuccess.copy(alpha = 0.05f))
                            .border(1.dp, SicoiSuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Detalhamento Técnico:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SicoiSuccess)
                        Text(
                            text = descriptionExecuted,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SicoiTextPrimary
                        )
                        HorizontalDivider(color = SicoiSuccess.copy(alpha = 0.1f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Encerrado em: ${if (dateFinished != "N/A") dateFinished else item.dataFim ?: "—"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = SicoiTextMuted
                            )
                            Text(
                                text = "Téc: ${item.tecnicoResponsavel ?: "—"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = SicoiTextMuted
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SicoiOrange.copy(alpha = 0.05f))
                            .border(1.dp, SicoiOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Motivo da Solicitação:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SicoiOrange)
                        Text(
                            text = descriptionToExecute,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = SicoiTextPrimary
                        )
                        HorizontalDivider(color = SicoiOrange.copy(alpha = 0.1f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SicoiError)
                            )
                            Text(
                                text = "Não finalizado",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = SicoiError
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = SicoiDivider)

            // Toggle Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Recuar" else "Expandir Detalhes",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = SicoiBlueLight)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = SicoiBlueLight,
                    modifier = Modifier.size(16.dp)
                )
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
