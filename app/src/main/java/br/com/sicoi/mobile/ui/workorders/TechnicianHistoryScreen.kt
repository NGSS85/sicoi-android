package br.com.sicoi.mobile.ui.workorders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

// ── ViewModel ──────────────────────────────────────────────────────────────
sealed class HistoryUiState {
    object Loading : HistoryUiState()
    data class Success(val orders: List<WorkOrder>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

@HiltViewModel
class TechnicianHistoryViewModel @Inject constructor(
    private val repository: WorkOrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    fun load(technicianName: String) {
        viewModelScope.launch {
            _state.value = HistoryUiState.Loading
            repository.fetchAllOrdersByTechnician(technicianName).fold(
                onSuccess = { _state.value = HistoryUiState.Success(it) },
                onFailure = { _state.value = HistoryUiState.Error(it.message ?: "Erro ao carregar histórico") }
            )
        }
    }
}

// ── Tela principal ─────────────────────────────────────────────────────────
@Composable
fun TechnicianHistoryScreen(
    technicianName: String,
    onNavigateBack: () -> Unit,
    viewModel: TechnicianHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(technicianName) { viewModel.load(technicianName) }

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
                            onClick = { viewModel.load(technicianName) },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SicoiCard)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = SicoiTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = SicoiOrange,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Histórico de Atividades",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = SicoiTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            technicianName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SicoiOrange,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when (val s = state) {
            is HistoryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SicoiOrange, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Carregando histórico...", style = MaterialTheme.typography.bodyMedium, color = SicoiTextMuted)
                    }
                }
            }
            is HistoryUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SicoiError, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(s.message, style = MaterialTheme.typography.bodyMedium, color = SicoiTextSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = { viewModel.load(technicianName) }, colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange)) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tentar Novamente")
                        }
                    }
                }
            }
            is HistoryUiState.Success -> {
                val allOrders = s.orders

                val finalized = allOrders.filter {
                    it.status.equals("Finalizada", ignoreCase = true) ||
                    it.status.equals("Finalizado", ignoreCase = true) ||
                    it.status.equals("Concluída", ignoreCase = true)
                }
                val paused = allOrders.filter {
                    it.status.equals("Pausada", ignoreCase = true) ||
                    it.status.equals("Pausado", ignoreCase = true)
                }
                val external = allOrders.filter {
                    it.tecnicoResponsavel?.lowercase()?.trim() == "externo" ||
                    it.solucaoAplicada?.contains("\"external_service\":\"Sim\"", ignoreCase = true) == true ||
                    it.solucaoAplicada?.contains("\"external_service\":\"sim\"", ignoreCase = true) == true
                }

                var showFinalized by remember { mutableStateOf(false) }
                var showPaused    by remember { mutableStateOf(false) }
                var showExternal  by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        HistoryDashboardCard(
                            icon = Icons.Default.CheckCircle,
                            label = "OS Finalizadas",
                            count = finalized.size,
                            accentColor = SicoiSuccess,
                            expanded = showFinalized,
                            onToggle = { showFinalized = !showFinalized }
                        )
                        AnimatedVisibility(
                            visible = showFinalized,
                            enter = expandVertically(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300))
                        ) {
                            HistoryOrderList(orders = finalized)
                        }
                    }
                    item {
                        HistoryDashboardCard(
                            icon = Icons.Default.Pause,
                            label = "OS Pausadas",
                            count = paused.size,
                            accentColor = SicoiWarning,
                            expanded = showPaused,
                            onToggle = { showPaused = !showPaused }
                        )
                        AnimatedVisibility(
                            visible = showPaused,
                            enter = expandVertically(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300))
                        ) {
                            HistoryOrderList(orders = paused)
                        }
                    }
                    item {
                        HistoryDashboardCard(
                            icon = Icons.Default.CallMade,
                            label = "Serviço Externo",
                            count = external.size,
                            accentColor = SicoiOrange,
                            expanded = showExternal,
                            onToggle = { showExternal = !showExternal }
                        )
                        AnimatedVisibility(
                            visible = showExternal,
                            enter = expandVertically(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300))
                        ) {
                            HistoryOrderList(orders = external)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ── Card do Dashboard ──────────────────────────────────────────────────────
@Composable
private fun HistoryDashboardCard(
    icon: ImageVector,
    label: String,
    count: Int,
    accentColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                }
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = SicoiTextPrimary
                    )
                    Text(
                        if (count == 0) "Nenhum registro" else "$count registro(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = SicoiTextMuted
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp),
                    color = accentColor
                )
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .clickable { onToggle() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (expanded) "Ocultar" else "Ver",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Lista de OS ────────────────────────────────────────────────────────────
@Composable
private fun HistoryOrderList(orders: List<WorkOrder>) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SicoiSurface).padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma O.S. encontrada", style = MaterialTheme.typography.bodyMedium, color = SicoiTextMuted)
            }
        } else {
            orders.forEach { order -> HistoryOrderItem(order = order) }
        }
    }
}

// ── Item individual ────────────────────────────────────────────────────────
@Composable
private fun HistoryOrderItem(order: WorkOrder) {
    val patrimonio = remember(order.solucaoAplicada) {
        extractJsonField(order.solucaoAplicada, "equipment_no")
            ?: extractJsonField(order.solucaoAplicada, "patrimonio")
            ?: "—"
    }
    val resumo = remember(order.solucaoAplicada, order.descricaoProblema) {
        val executed = extractJsonField(order.solucaoAplicada, "description_executed")
        if (!executed.isNullOrBlank()) executed
        else order.descricaoProblema?.take(120) ?: "—"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiSurface),
        border = BorderStroke(1.dp, SicoiDivider)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Assignment, contentDescription = null, tint = SicoiOrange, modifier = Modifier.size(18.dp))
                    Text(
                        "OS ${order.numeroOs ?: "S/N"}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                        color = SicoiTextPrimary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Tag, contentDescription = null, tint = SicoiTextMuted, modifier = Modifier.size(14.dp))
                    Text(
                        patrimonio,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SicoiTextSecondary
                    )
                }
            }
            if (!order.equipamento.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = SicoiTextMuted, modifier = Modifier.size(15.dp))
                    Text(
                        order.equipamento,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = SicoiTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (resumo != "—") {
                Divider(color = SicoiDivider)
                Text(
                    resumo,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = SicoiTextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Helper ─────────────────────────────────────────────────────────────────
private fun extractJsonField(json: String?, key: String): String? {
    if (json.isNullOrBlank()) return null
    return Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
}
