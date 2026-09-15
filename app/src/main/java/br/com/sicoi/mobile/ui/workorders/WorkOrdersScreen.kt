package br.com.sicoi.mobile.ui.workorders

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.sicoi.mobile.data.model.WorkOrder
import br.com.sicoi.mobile.data.repository.WorkOrderRepository
import br.com.sicoi.mobile.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel
sealed class WorkOrdersUiState {
    object Loading : WorkOrdersUiState()
    data class Success(val orders: List<WorkOrder>, val isOffline: Boolean = false) : WorkOrdersUiState()
    data class Error(val message: String) : WorkOrdersUiState()
}

@HiltViewModel
class WorkOrdersViewModel @Inject constructor(
    private val repository: WorkOrderRepository
) : ViewModel() {
    private val _state = MutableStateFlow<WorkOrdersUiState>(WorkOrdersUiState.Loading)
    val state: StateFlow<WorkOrdersUiState> = _state.asStateFlow()

    fun fetchOrders(technicianName: String?) {
        viewModelScope.launch {
            _state.value = WorkOrdersUiState.Loading
            repository.fetchOpenOrders(technicianName).fold(
                onSuccess = { _state.value = WorkOrdersUiState.Success(it) },
                onFailure = { _state.value = WorkOrdersUiState.Error(it.message ?: "Erro ao carregar OS") }
            )
        }
    }

    fun reactivateOrder(osId: String, technicianName: String) {
        viewModelScope.launch {
            _state.value = WorkOrdersUiState.Loading
            // Tenta reativar a ordem para o status 'Em Execução'
            repository.updateWorkOrderStatus(osId, "Em Execução", true).fold(
                onSuccess = { fetchOrders(technicianName) },
                onFailure = { _state.value = WorkOrdersUiState.Error(it.message ?: "Erro ao reativar O.S.") }
            )
        }
    }
}

/**
 * Tela 4: Lista de Ordens de Serviço em Aberto
 *
 * Consome dados em tempo real do Supabase via RPC.
 * Cards com: Número OS, Data, Equipamento/Setor, Resumo do Problema, Prioridade.
 * Pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrdersScreen(
    technicianId: String,
    technicianName: String,
    onNavigateBack: () -> Unit,
    onSelectWorkOrder: (workOrderId: String) -> Unit,
    onNavigateToHistory: (technicianName: String) -> Unit = {},
    onNavigateToPausedOrders: (technicianName: String) -> Unit = {},
    viewModel: WorkOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing = state is WorkOrdersUiState.Loading

    LaunchedEffect(technicianName) {
        viewModel.fetchOrders(technicianName)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    
    var showImageViewer by remember { mutableStateOf(false) }
    var viewerImages by remember { mutableStateOf<List<String>>(emptyList()) }

    // Calcular métricas para passar para o header
    val activeOrders = if (state is WorkOrdersUiState.Success) {
        val s = state as WorkOrdersUiState.Success
        val visible = s.orders.filter { it.status != "Aberta" && it.status != "Em Aberto" }
        visible.filter { it.status != "Pausada" && it.status != "Pausado" }
    } else emptyList()

    val pausedOrders = if (state is WorkOrdersUiState.Success) {
        val s = state as WorkOrdersUiState.Success
        val visible = s.orders.filter { it.status != "Aberta" && it.status != "Em Aberto" }
        visible.filter { it.status == "Pausada" || it.status == "Pausado" }
    } else emptyList()

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
                    Spacer(modifier = Modifier.height(32.dp))

                    // Título da sidebar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
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
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                            ),
                            color = Color.White
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Botão: Ordens em Pausa
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SicoiWarning.copy(alpha = 0.12f))
                            .border(1.dp, SicoiWarningBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                onNavigateToPausedOrders(technicianName)
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
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                "${pausedOrders.size} ordem(ns) pausada(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = SicoiWarning
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botão: Histórico - Minhas atividades
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SicoiOrange.copy(alpha = 0.12f))
                            .border(1.dp, SicoiOrangeBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                coroutineScope.launch { drawerState.close() }
                                onNavigateToHistory(technicianName)
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
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                "Minhas atividades",
                                style = MaterialTheme.typography.bodySmall,
                                color = SicoiOrange
                            )
                        }
                    }
                }
            }
        }
    ) {
        // Estrutura principal: Column com header fixo + lista com scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SicoiBackground)
        ) {
            // ── HEADER FIXO (preto) ──────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Linha 1: Voltar | Atualizar | Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Botão Atualizar
                        IconButton(
                            onClick = { viewModel.fetchOrders(technicianName) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Atualizar",
                                tint = Color.White
                            )
                        }
                        // Botão Menu (três tracinhos)
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu Lateral",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── NOVO CARD DO TÉCNICO MODERNO (Aumentado em 20%) ─────────
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF181B22),
                    border = BorderStroke(1.dp, Color(0xFF2E3545)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Avatar com gradiente e ícone (20% maior)
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(SicoiOrange, Color(0xFFD84315))
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Engineering,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SicoiOrange.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, SicoiOrange.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    "PAINEL DO TÉCNICO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = SicoiOrange,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                technicianName.uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 24.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Atividades e ordens em andamento",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                                color = SicoiTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cards de métricas interativos (Ordens ativas | O.S Pausadas com Acesso Direto)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card: Ordens ativas
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E222D),
                        border = BorderStroke(1.dp, SicoiOrange.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(SicoiOrange)
                                )
                                Text(
                                    "Ordens Ativas",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = SicoiOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "${activeOrders.size}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                    fontSize = 28.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    // Card: O.S Pausadas (CLICÁVEL - ACESSO DIRETO)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToPausedOrders(technicianName) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (pausedOrders.isNotEmpty()) SicoiWarning.copy(alpha = 0.12f) else Color(0xFF1E222D),
                        border = BorderStroke(
                            1.dp, 
                            if (pausedOrders.isNotEmpty()) SicoiWarning.copy(alpha = 0.6f) else Color(0xFF2E3545)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.PauseCircleFilled,
                                    contentDescription = null,
                                    tint = SicoiWarning,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    "O.S. Pausadas",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = SicoiWarning
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "${pausedOrders.size}",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                        fontSize = 28.sp
                                    ),
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SicoiWarning.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, SicoiWarning.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        "Acessar →",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = SicoiWarning,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Badge offline (quando aplicável)
                if (state is WorkOrdersUiState.Success && (state as WorkOrdersUiState.Success).isOffline) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SicoiWarning.copy(alpha = 0.12f))
                            .border(1.dp, SicoiWarningBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = SicoiWarning, modifier = Modifier.size(14.dp))
                        Text("Modo offline — dados em cache local", style = MaterialTheme.typography.labelSmall, color = SicoiWarning)
                    }
                }
            }

            // ── ÁREA COM SCROLL ─────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when (val s = state) {
                    is WorkOrdersUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = SicoiOrange, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Carregando ordens de serviço...", style = MaterialTheme.typography.bodyMedium, color = SicoiTextMuted)
                            }
                        }
                    }
                    is WorkOrdersUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SicoiError, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(s.message, style = MaterialTheme.typography.bodyMedium, color = SicoiTextSecondary, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { viewModel.fetchOrders(technicianName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Tentar Novamente")
                                }
                            }
                        }
                    }
                    is WorkOrdersUiState.Success -> {
                        val allVisible = s.orders.filter { it.status != "Aberta" && it.status != "Em Aberto" }
                        val active = allVisible
                            .filter { it.status != "Pausada" && it.status != "Pausado" }
                            .sortedBy { if (it.prioridade?.lowercase() in listOf("emergency", "emergência", "emergencia")) 0 else 1 }

                        if (allVisible.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SicoiSuccess, modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Nenhuma O.S. ativa", style = MaterialTheme.typography.titleMedium, color = SicoiTextPrimary)
                                    Text(
                                        "Todas as ordens de serviço de $technicianName estão finalizadas ou aguardando início.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SicoiTextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(active, key = { it.id }) { order ->
                                    WorkOrderCard(
                                        workOrder = order,
                                        onClick = { onSelectWorkOrder(order.id) },
                                        onShowImages = { urls ->
                                            viewerImages = urls
                                            showImageViewer = true
                                        }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(16.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImageViewer) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showImageViewer = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.95f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = { showImageViewer = false },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                        }
                    }
                    if (viewerImages.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(viewerImages) { url ->
                                coil.compose.AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nenhuma imagem anexada", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun WorkOrderCard(
    workOrder: WorkOrder, 
    isPaused: Boolean = false, 
    onClick: () -> Unit,
    onReactivate: (() -> Unit)? = null,
    onShowImages: ((List<String>) -> Unit)? = null
) {
    val displayPriority = when (workOrder.prioridade?.lowercase()?.trim()) {
        "emergency", "emergência", "emergencia", "crítica", "critica" -> "Emergência"
        "high", "urgente", "urgent_2days", "alta" -> "Urgente"
        else -> "Normal"
    }

    val priorityColor = when (displayPriority) {
        "Emergência" -> SicoiEmergency
        "Urgente"    -> SicoiWarning
        else         -> SicoiSuccess
    }
    val priorityIcon = when (displayPriority) {
        "Emergência" -> Icons.Default.Warning
        "Urgente"    -> Icons.Default.PriorityHigh
        else         -> Icons.Default.CheckCircleOutline
    }

    var isExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "BlinkTransition")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (displayPriority == "Emergência") 0.35f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isPaused) 0.85f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D)),
        border = BorderStroke(
            1.dp, 
            if (isPaused) SicoiError.copy(alpha = 0.4f) else priorityColor.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        val rawOsNumber = workOrder.getFullNumeroOs()
            .replace("OS", "", ignoreCase = true)
            .replace("O.S.", "", ignoreCase = true)
            .replace("#", "")
            .replace("º", "")
            .replace("nº", "", ignoreCase = true)
            .trim()
        val displayOsTitle = if (rawOsNumber.isNotBlank()) "OS nº$rawOsNumber" else "OS Sem Número"

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = if (isExpanded) 14.dp else 16.dp)) {
            // ── LINHA PRINCIPAL COMPACTA (Sempre visível e com largura ampla) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Número da O.S. (ex: OS nº221/26) - Mais largo na vertical
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF282D3C),
                    border = BorderStroke(1.dp, Color(0xFF3B4358)),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayOsTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.5.sp, 
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // 2. Badges + Botão Expandir (Em modo Pausado: Detalhes na vertical centralizado abaixo)
                if (isPaused) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Linha com Badges (PAUSADA + Prioridade)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SicoiError.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, SicoiError.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    "PAUSADA",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SicoiError, 
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, 
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            // Badge de Prioridade
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = priorityColor.copy(alpha = if (displayPriority == "Emergência") 0.85f * blinkAlpha else 0.15f),
                                border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        priorityIcon, 
                                        contentDescription = null, 
                                        tint = if (displayPriority == "Emergência") Color.White else priorityColor, 
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = displayPriority.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            letterSpacing = 0.3.sp,
                                            color = if (displayPriority == "Emergência") Color.White else priorityColor
                                        )
                                    )
                                }
                            }
                        }

                        // Botão Detalhes centralizado na vertical logo abaixo
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isExpanded) SicoiOrange.copy(alpha = 0.2f) else Color(0xFF282D3C),
                            border = BorderStroke(1.dp, if (isExpanded) SicoiOrangeBorder else Color(0xFF3B4358)),
                            modifier = Modifier.clickable { isExpanded = !isExpanded }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isExpanded) "Menos" else "Detalhes",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isExpanded) SicoiOrange else Color.White
                                    )
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Recolher" else "Expandir",
                                    tint = if (isExpanded) SicoiOrange else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Badge de Prioridade
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = priorityColor.copy(alpha = if (displayPriority == "Emergência") 0.85f * blinkAlpha else 0.15f),
                            border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    priorityIcon, 
                                    contentDescription = null, 
                                    tint = if (displayPriority == "Emergência") Color.White else priorityColor, 
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = displayPriority.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.3.sp,
                                        color = if (displayPriority == "Emergência") Color.White else priorityColor
                                    )
                                )
                            }
                        }

                        // Botão Expandir / Recolher
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isExpanded) SicoiOrange.copy(alpha = 0.2f) else Color(0xFF282D3C),
                            border = BorderStroke(1.dp, if (isExpanded) SicoiOrangeBorder else Color(0xFF3B4358)),
                            modifier = Modifier.clickable { isExpanded = !isExpanded }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = if (isExpanded) "Menos" else "Detalhes",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isExpanded) SicoiOrange else Color.White
                                    )
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Recolher" else "Expandir",
                                    tint = if (isExpanded) SicoiOrange else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── CONTEÚDO EXPANSÍVEL (Apenas quando isExpanded == true) ──
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFF2E3545), modifier = Modifier.padding(bottom = 12.dp))

                    // 1. Equipamento completo
                    WorkOrderInfoChip(
                        icon = Icons.Default.PrecisionManufacturing,
                        label = "Equipamento",
                        value = workOrder.getFullEquipment(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Setor e Solicitante
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WorkOrderInfoChip(
                            icon = Icons.Default.Business,
                            label = "Setor",
                            value = workOrder.getFullSector(),
                            modifier = Modifier.weight(1f)
                        )
                        WorkOrderInfoChip(
                            icon = Icons.Default.Person,
                            label = "Solicitante",
                            value = workOrder.getFullRequester(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 3. Descrição do Problema se houver
                    val descricaoProblema = workOrder.descricaoProblema?.trim()
                    if (!descricaoProblema.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF252A36),
                            border = BorderStroke(1.dp, Color(0xFF333B4D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "Descrição do Problema:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = SicoiOrange
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    descricaoProblema,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = SicoiTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Botão Abrir ordem de serviço
                    val buttonColor = if (isPaused && onReactivate != null) SicoiSuccess else SicoiOrange
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(buttonColor)
                            .border(1.dp, buttonColor, RoundedCornerShape(12.dp))
                            .clickable { if (isPaused && onReactivate != null) onReactivate() else onClick() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (isPaused && onReactivate != null) "Reativar ordem de serviço" else "Abrir ordem de serviço", 
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White, 
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            if (isPaused && onReactivate != null) Icons.Default.PlayArrow else Icons.Default.ArrowForward, 
                            contentDescription = null, 
                            tint = Color.White, 
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkOrderInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    value: String,
    isDarkText: Boolean = false
) {
    val mutedColor = if (isDarkText) Color.Gray else SicoiTextMuted
    val primaryColor = if (isDarkText) Color.Black else SicoiTextPrimary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = mutedColor, modifier = Modifier.size(20.dp))
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, color = mutedColor)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = primaryColor),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WorkOrderInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SicoiSurface)
            .border(1.dp, SicoiDivider.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(SicoiOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = SicoiOrange, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = SicoiTextMuted, letterSpacing = 0.5.sp)
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = SicoiTextPrimary),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatTimeElapsed(abertura: String?): String {
    if (abertura == null) return "—"
    try {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = format.parse(abertura) ?: return "—"
        val diff = System.currentTimeMillis() - date.time
        val days = diff / (1000 * 60 * 60 * 24)
        val hours = (diff / (1000 * 60 * 60)) % 24
        
        if (days > 0 && hours > 0) return "${days}D ${hours}h"
        if (days > 0) return "${days}D"
        if (hours > 0) return "${hours}h"
        return "Agora"
    } catch (e: Exception) {
        return abertura.take(10)
    }
}
