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
    viewModel: WorkOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing = state is WorkOrdersUiState.Loading

    LaunchedEffect(technicianName) {
        viewModel.fetchOrders(technicianName)
    }

    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        containerColor = SicoiBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(SicoiSurface, SicoiBackground)))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column {
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
                            onClick = { viewModel.fetchOrders(technicianName) },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SicoiCard)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = SicoiTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            technicianName, 
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 34.sp, 
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                            ), 
                            color = SicoiOrange,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            "Bem vindo técnico, essas são suas atividades",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp, 
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            color = SicoiTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Badge offline
                    if (state is WorkOrdersUiState.Success && (state as WorkOrdersUiState.Success).isOffline) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SicoiWarning.copy(alpha = 0.12f))
                                .border(1.dp, SicoiWarning.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.WifiOff, contentDescription = null, tint = SicoiWarning, modifier = Modifier.size(14.dp))
                            Text("Modo offline — dados em cache local", style = MaterialTheme.typography.labelSmall, color = SicoiWarning)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.fetchOrders(technicianName) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                    val visibleOrders = s.orders.filter { it.status != "Aberta" && it.status != "Em Aberto" }
                    val activeOrders = visibleOrders.filter { it.status != "Pausada" && it.status != "Pausado" }
                        .sortedBy { if (it.prioridade?.lowercase() in listOf("emergency", "emergência", "emergencia")) 0 else 1 }
                    val pausedOrders = visibleOrders.filter { it.status == "Pausada" || it.status == "Pausado" }
                        .sortedBy { if (it.prioridade?.lowercase() in listOf("emergency", "emergência", "emergencia")) 0 else 1 }

                    if (visibleOrders.isEmpty()) {
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
                            if (activeOrders.isNotEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Ordens de Serviço Ativas em Tempo Real",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                            color = SicoiTextPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "${activeOrders.size}",
                                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
                                            color = SicoiOrange,
                                            modifier = Modifier
                                                .background(SicoiOrange.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                                items(activeOrders, key = { it.id }) { order ->
                                    WorkOrderCard(
                                        workOrder = order,
                                        onClick = { onSelectWorkOrder(order.id) }
                                    )
                                }
                            }
                            
                            if (pausedOrders.isNotEmpty()) {
                                item {
                                    Text(
                                        "Ordens de Serviço Pausadas",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                        color = SicoiTextMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)
                                    )
                                }
                                items(pausedOrders, key = { it.id }) { order ->
                                    WorkOrderCard(
                                        workOrder = order,
                                        isPaused = true,
                                        onClick = { onSelectWorkOrder(order.id) }
                                    )
                                }
                            }
                            
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun WorkOrderCard(workOrder: WorkOrder, isPaused: Boolean = false, onClick: () -> Unit) {
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(SicoiError.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PAUSADA", style = MaterialTheme.typography.labelSmall.copy(color = SicoiError, letterSpacing = 0.sp))
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(priorityColor.copy(alpha = if (displayPriority == "Emergência") 0.9f * blinkAlpha else 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(priorityIcon, contentDescription = null, tint = if (displayPriority == "Emergência") Color.White else priorityColor, modifier = Modifier.size(12.dp))
                        Text(
                            displayPriority,
                            style = MaterialTheme.typography.labelSmall.copy(
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
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = SicoiTextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botão Abrir (Destacado, Centralizado e Maior)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SicoiOrange)
                        .border(1.dp, SicoiOrange, RoundedCornerShape(12.dp))
                        .padding(horizontal = 28.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Abrir O.S.", 
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White, 
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkOrderInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = SicoiTextMuted, modifier = Modifier.size(14.dp))
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = SicoiTextMuted)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = SicoiTextPrimary),
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
