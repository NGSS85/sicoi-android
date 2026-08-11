package br.com.sicoi.mobile.ui.workorders

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

                Spacer(modifier = Modifier.height(16.dp))

                // Nome do técnico e boas-vindas
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        technicianName.uppercase(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 38.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            letterSpacing = 1.sp,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = SicoiOrange.copy(alpha = 0.6f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                                blurRadius = 8f
                            )
                        ),
                        color = SicoiOrange,
                        textAlign = TextAlign.Center,
                        lineHeight = 42.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Bem vindo técnico, essas são suas atividades",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cards de métricas (Ordens ativas | O.S Pausadas)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card: Ordens ativas
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(1.dp, SicoiOrangeBorder, RoundedCornerShape(14.dp))
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Ordens ativas",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = SicoiOrange
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${activeOrders.size}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                            ),
                            color = Color.White
                        )
                    }

                    // Card: O.S Pausadas
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.07f))
                            .border(1.dp, SicoiWarningBorder, RoundedCornerShape(14.dp))
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "O.S Pausadas",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = SicoiWarning
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${pausedOrders.size}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                            ),
                            color = Color.White
                        )
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
                                        onClick = { onSelectWorkOrder(order.id) }
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
}



@Composable
fun WorkOrderCard(
    workOrder: WorkOrder, 
    isPaused: Boolean = false, 
    onClick: () -> Unit,
    onReactivate: (() -> Unit)? = null
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

    val infiniteTransition = rememberInfiniteTransition(label = "BlinkTransition")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (displayPriority == "Emergência") 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(if (isPaused) 0.7f else 1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPaused) SicoiSurface else SicoiCard),
        border = BorderStroke(1.dp, if (isPaused) SicoiError.copy(alpha = 0.3f) else priorityColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header da OS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        tint = if (isPaused) SicoiError else SicoiOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        workOrder.numeroOs ?: "Sem Número",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp, 
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            textDecoration = if (isPaused) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        ),
                        color = if (isPaused) SicoiTextMuted else SicoiTextPrimary
                    )
                }

                // Badge de prioridade (e badge de Pausa)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isPaused) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SicoiError.copy(alpha = 0.15f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PAUSADA", style = MaterialTheme.typography.titleMedium.copy(color = SicoiError, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 0.sp))
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(priorityColor.copy(alpha = if (displayPriority == "Emergência") 0.9f * blinkAlpha else 0.15f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(priorityIcon, contentDescription = null, tint = if (displayPriority == "Emergência") Color.White else priorityColor, modifier = Modifier.size(22.dp))
                        Text(
                            displayPriority,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                letterSpacing = 0.sp,
                                color = if (displayPriority == "Emergência") Color.White else priorityColor
                            )
                        )
                    }
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = SicoiDivider
            )

            // Informações da OS
            WorkOrderInfoRow(
                icon = Icons.Default.PrecisionManufacturing,
                label = "Equipamento",
                value = workOrder.equipamento ?: "—"
            )

            Spacer(modifier = Modifier.height(6.dp))
            WorkOrderInfoRow(
                icon = Icons.Default.Business,
                label = "Setor",
                value = workOrder.setor ?: "—"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Spacer(modifier = Modifier.height(6.dp))
            WorkOrderInfoRow(
                icon = Icons.Default.Schedule,
                label = "Aberto há",
                value = formatTimeElapsed(workOrder.dataAbertura)
            )

            if (!workOrder.descricaoProblema.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SicoiSurface)
                        .padding(10.dp)
                ) {
                    Text(
                        workOrder.descricaoProblema,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                        color = SicoiTextSecondary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botão Abrir ou Reativar
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val buttonColor = if (isPaused && onReactivate != null) SicoiSuccess else SicoiOrange
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(buttonColor)
                        .border(1.dp, buttonColor, RoundedCornerShape(12.dp))
                        .clickable { if (isPaused && onReactivate != null) onReactivate() else onClick() }
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (isPaused && onReactivate != null) "Reativar O.S." else "Abrir O.S.", 
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White, 
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        if (isPaused && onReactivate != null) Icons.Default.PlayArrow else Icons.Default.ArrowForward, 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(16.dp)
                    )
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
