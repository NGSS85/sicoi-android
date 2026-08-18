package br.com.sicoi.mobile.ui.workorders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.SolidColor
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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    data class Success(val orders: List<WorkOrder>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

enum class HistoryCategory {
    FINALIZED,
    EXTERNAL
}

enum class HistoryTimeFilter(val label: String) {
    WEEK("Semana"),
    MONTH("M\u00EAs"),
    YEAR("Ano"),
    ALL("Todos")
}

data class ChartBarData(
    val label: String,
    val finalizedCount: Int,
    val externalCount: Int
)

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
                onFailure = { _state.value = HistoryUiState.Error(it.message ?: "Erro ao carregar hist\u00F3rico") }
            )
        }
    }
}

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
                            "Hist\u00F3rico de Atividades",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = SicoiTextPrimary
                        )
                        Text(
                            "T\u00E9cnico: $technicianName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SicoiTextMuted
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
                        CircularProgressIndicator(color = SicoiOrange, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Carregando hist\u00F3rico...", style = MaterialTheme.typography.bodyMedium, color = SicoiTextMuted)
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

                var selectedTimeFilter by remember { mutableStateOf(HistoryTimeFilter.ALL) }

                val filteredOrders = remember(allOrders, selectedTimeFilter) {
                    allOrders.filter { isOrderInTimeFilter(it, selectedTimeFilter) }
                }

                val finalized = remember(filteredOrders) {
                    filteredOrders.filter {
                        it.status.equals("Finalizada", ignoreCase = true) ||
                        it.status.equals("Finalizado", ignoreCase = true) ||
                        it.status.equals("Conclu\u00EDda", ignoreCase = true) ||
                        it.status.equals("Concluida", ignoreCase = true)
                    }
                }

                val external = remember(filteredOrders) {
                    filteredOrders.filter {
                        it.tecnicoResponsavel?.lowercase()?.trim() == "externo" ||
                        it.solucaoAplicada?.contains("\"external_service\":\"Sim\"", ignoreCase = true) == true ||
                        it.solucaoAplicada?.contains("\"external_service\":\"sim\"", ignoreCase = true) == true
                    }
                }

                val chartData = remember(filteredOrders, selectedTimeFilter) {
                    buildChartData(filteredOrders, selectedTimeFilter)
                }

                var showFinalized by remember { mutableStateOf(false) }
                var showExternal  by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── 1. Filtro de Período (Semana / Mês / Ano / Todos) ──
                    item {
                        TimeFilterSelector(
                            selectedFilter = selectedTimeFilter,
                            onFilterSelected = { selectedTimeFilter = it }
                        )
                    }

                    // ── 2. Área com Gráfico de Barras Aprimorado ──
                    item {
                        HistoryBarChartCard(
                            chartData = chartData,
                            filter = selectedTimeFilter,
                            totalFinalized = finalized.size,
                            totalExternal = external.size
                        )
                    }

                    // ── 3. Card OS Finalizadas ──
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
                            HistoryOrderList(orders = finalized, category = HistoryCategory.FINALIZED)
                        }
                    }

                    // ── 4. Card Serviço Externo ──
                    item {
                        HistoryDashboardCard(
                            icon = Icons.Default.CallMade,
                            label = "Servi\u00E7o Externo",
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
                            HistoryOrderList(orders = external, category = HistoryCategory.EXTERNAL)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

// ── Componente Seletor de Período ────────────────────────────────────────────
@Composable
private fun TimeFilterSelector(
    selectedFilter: HistoryTimeFilter,
    onFilterSelected: (HistoryTimeFilter) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                tint = SicoiOrange,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "Filtrar por per\u00EDodo:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SicoiTextSecondary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SicoiCard)
                .border(1.dp, SicoiDivider, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HistoryTimeFilter.entries.forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SicoiOrange else Color.Transparent)
                        .clickable { onFilterSelected(filter) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = if (isSelected) Color.White else SicoiTextSecondary
                    )
                }
            }
        }
    }
}

// ── Componente Gráfico de Barras Aprimorado ──────────────────────────────────
@Composable
private fun HistoryBarChartCard(
    chartData: List<ChartBarData>,
    filter: HistoryTimeFilter,
    totalFinalized: Int,
    totalExternal: Int
) {
    val totalOrders = totalFinalized + totalExternal
    val maxCount = chartData.maxOfOrNull { maxOf(it.finalizedCount, it.externalCount) }?.coerceAtLeast(1) ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.dp, SicoiDivider)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabeçalho com Título e Badges Informativos
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SicoiOrange.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            tint = SicoiOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            "Desempenho de Atividades",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            ),
                            color = SicoiTextPrimary
                        )
                        Text(
                            "Filtro: ${filter.label} ($totalOrders ${if (totalOrders == 1) "ordem" else "ordens"})",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = SicoiTextMuted
                        )
                    }
                }

                // Badges de Legenda com Totais
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SicoiSuccess.copy(alpha = 0.12f))
                            .border(1.dp, SicoiSuccess.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SicoiSuccess))
                            Text(
                                "$totalFinalized",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = SicoiSuccess
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SicoiOrange.copy(alpha = 0.12f))
                            .border(1.dp, SicoiOrange.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SicoiOrange))
                            Text(
                                "$totalExternal",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = SicoiOrange
                            )
                        }
                    }
                }
            }

            // Área Central das Barras
            if (chartData.isEmpty() || totalOrders == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SicoiSurface)
                        .border(1.dp, SicoiDivider, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SicoiTextMuted, modifier = Modifier.size(22.dp))
                        Text(
                            "Nenhuma atividade registrada neste per\u00EDodo",
                            style = MaterialTheme.typography.bodySmall,
                            color = SicoiTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SicoiSurface)
                        .border(1.dp, SicoiDivider, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chartData.forEach { bar ->
                            val finRatio = (bar.finalizedCount.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                            val extRatio = (bar.externalCount.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)

                            val animFinRatio by animateFloatAsState(
                                targetValue = finRatio,
                                animationSpec = tween(600),
                                label = "finRatio"
                            )
                            val animExtRatio by animateFloatAsState(
                                targetValue = extRatio,
                                animationSpec = tween(600),
                                label = "extRatio"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                // Barras lado a lado
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    // 1. Barra Finalizadas (Verde)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (bar.finalizedCount > 0) {
                                            Text(
                                                "${bar.finalizedCount}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = SicoiSuccess
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(14.dp)
                                                .height((78.dp * animFinRatio).coerceAtLeast(if (bar.finalizedCount > 0) 8.dp else 2.dp))
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(
                                                    if (bar.finalizedCount > 0)
                                                        Brush.verticalGradient(listOf(SicoiSuccess, SicoiSuccess.copy(alpha = 0.65f)))
                                                    else SolidColor(SicoiDivider.copy(alpha = 0.4f))
                                                )
                                        )
                                    }

                                    // 2. Barra Serviços Externos (Laranja)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (bar.externalCount > 0) {
                                            Text(
                                                "${bar.externalCount}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                ),
                                                color = SicoiOrange
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(14.dp)
                                                .height((78.dp * animExtRatio).coerceAtLeast(if (bar.externalCount > 0) 8.dp else 2.dp))
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(
                                                    if (bar.externalCount > 0)
                                                        Brush.verticalGradient(listOf(SicoiOrange, SicoiOrange.copy(alpha = 0.65f)))
                                                    else SolidColor(SicoiDivider.copy(alpha = 0.4f))
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Rótulo inferior da coluna
                                Text(
                                    text = bar.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = SicoiTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Rodapé com Legenda Detalhada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SicoiSuccess))
                    Text(
                        "Finalizadas: $totalFinalized",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                        color = SicoiTextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SicoiOrange))
                    Text(
                        "Servi\u00E7o Externo: $totalExternal",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
                        color = SicoiTextSecondary
                    )
                }
            }
        }
    }
}

// ── Card do Dashboard ────────────────────────────────────────────────────────
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
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
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
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp),
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

// ── Lista de OS ──────────────────────────────────────────────────────────────
@Composable
private fun HistoryOrderList(orders: List<WorkOrder>, category: HistoryCategory) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SicoiSurface).padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma O.S. encontrada para este per\u00EDodo", style = MaterialTheme.typography.bodyMedium, color = SicoiTextMuted)
            }
        } else {
            orders.forEach { order ->
                HistoryOrderItem(order = order, category = category)
            }
        }
    }
}

// ── Item individual de Histórico ─────────────────────────────────────────────
@Composable
private fun HistoryOrderItem(order: WorkOrder, category: HistoryCategory) {
    val osNumber = remember(order.numeroOs, order.solucaoAplicada, order.id) {
        order.numeroOs?.takeIf { it.isNotBlank() }
            ?: extractJsonField(order.solucaoAplicada, "os_number")
            ?: order.id.take(6).uppercase()
    }

    val equipamento = remember(order.equipamento, order.solucaoAplicada) {
        order.equipamento?.takeIf { it.isNotBlank() }
            ?: extractJsonField(order.solucaoAplicada, "equipment")
            ?: "Equipamento Geral"
    }

    val patrimonio = remember(order.solucaoAplicada) {
        extractJsonField(order.solucaoAplicada, "equipment_no")
            ?: extractJsonField(order.solucaoAplicada, "patrimonio")
            ?: "AVULSO"
    }

    val comentarioTecnico = remember(order.solucaoAplicada, order.descricaoProblema, category) {
        when (category) {
            HistoryCategory.FINALIZED -> {
                val executed = extractJsonField(order.solucaoAplicada, "description_executed")
                val notes = extractJsonField(order.solucaoAplicada, "general_notes")
                val techNotes = extractJsonField(order.solucaoAplicada, "technical_notes")
                when {
                    !executed.isNullOrBlank() -> executed
                    !notes.isNullOrBlank() -> notes
                    !techNotes.isNullOrBlank() -> techNotes
                    !order.descricaoProblema.isNullOrBlank() -> order.descricaoProblema
                    else -> "Servi\u00E7o finalizado com sucesso."
                }
            }
            HistoryCategory.EXTERNAL -> {
                val extJust = extractJsonField(order.solucaoAplicada, "external_justification")
                val toExec = extractJsonField(order.solucaoAplicada, "description_to_execute")
                when {
                    !extJust.isNullOrBlank() -> extJust
                    !toExec.isNullOrBlank() -> toExec
                    !order.descricaoProblema.isNullOrBlank() -> order.descricaoProblema
                    else -> "Sem justificativa de servi\u00E7o externo registrada."
                }
            }
        }
    }

    val (badgeText, badgeColor, commentLabelColor) = when (category) {
        HistoryCategory.FINALIZED -> Triple("Finalizada", SicoiSuccess, SicoiSuccess)
        HistoryCategory.EXTERNAL -> Triple("Servi\u00E7o Externo", SicoiOrange, SicoiOrange)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiSurface),
        border = BorderStroke(1.dp, SicoiDivider)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Linha 1: Número da OS e Badge de Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SicoiCard)
                            .border(1.dp, SicoiDivider, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "OS N\u00BA $osNumber",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            ),
                            color = SicoiOrange
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = badgeColor
                    )
                }
            }

            // Linha 2: Nome do Equipamento e Patrimônio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.PrecisionManufacturing,
                        contentDescription = null,
                        tint = SicoiTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        equipamento,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SicoiTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SicoiCard)
                        .border(1.dp, SicoiDivider, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Tag,
                            contentDescription = null,
                            tint = SicoiTextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "PAT: $patrimonio",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = SicoiTextSecondary
                        )
                    }
                }
            }

            // Linha 3: Comentário do Técnico
            if (comentarioTecnico.isNotBlank()) {
                Divider(color = SicoiDivider, modifier = Modifier.padding(vertical = 2.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SicoiCard.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "Coment\u00E1rio do T\u00E9cnico:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = commentLabelColor
                    )
                    Text(
                        "\"$comentarioTecnico\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = SicoiTextSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Funções de Gráfico e Filtros de Data ──────────────────────────────────────
private fun buildChartData(orders: List<WorkOrder>, filter: HistoryTimeFilter): List<ChartBarData> {
    val cal = Calendar.getInstance()
    val sdfDay = SimpleDateFormat("EEE", Locale("pt", "BR"))
    val sdfMonth = SimpleDateFormat("MMM", Locale("pt", "BR"))

    return when (filter) {
        HistoryTimeFilter.WEEK -> {
            val days = mutableListOf<ChartBarData>()
            for (i in 6 downTo 0) {
                val dayCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                }
                val rawLabel = sdfDay.format(dayCal.time).replace(".", "").trim()
                val dayLabel = if (rawLabel.isNotEmpty()) rawLabel.substring(0, 1).uppercase() + rawLabel.substring(1) else "Dia"

                val dayOrders = orders.filter { o ->
                    val millis = getOrderDateMillis(o) ?: return@filter false
                    val oCal = Calendar.getInstance().apply { timeInMillis = millis }
                    oCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                    oCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
                }

                val fin = dayOrders.count { it.status.equals("Finalizada", true) || it.status.equals("Finalizado", true) || it.status.equals("Conclu\u00EDda", true) || it.status.equals("Concluida", true) }
                val ext = dayOrders.count { it.tecnicoResponsavel?.lowercase()?.trim() == "externo" || it.solucaoAplicada?.contains("\"external_service\":\"Sim\"", true) == true }
                days.add(ChartBarData(label = dayLabel, finalizedCount = fin, externalCount = ext))
            }
            days
        }
        HistoryTimeFilter.MONTH -> {
            listOf(
                ChartBarData("Sem 1", 0, 0),
                ChartBarData("Sem 2", 0, 0),
                ChartBarData("Sem 3", 0, 0),
                ChartBarData("Sem 4", 0, 0)
            ).mapIndexed { idx, item ->
                val startDay = idx * 7 + 1
                val endDay = (idx + 1) * 7
                val weekOrders = orders.filter { o ->
                    val millis = getOrderDateMillis(o) ?: return@filter false
                    val oCal = Calendar.getInstance().apply { timeInMillis = millis }
                    val dayOfMonth = oCal.get(Calendar.DAY_OF_MONTH)
                    dayOfMonth in startDay..endDay
                }
                val fin = weekOrders.count { it.status.equals("Finalizada", true) || it.status.equals("Finalizado", true) || it.status.equals("Conclu\u00EDda", true) || it.status.equals("Concluida", true) }
                val ext = weekOrders.count { it.tecnicoResponsavel?.lowercase()?.trim() == "externo" || it.solucaoAplicada?.contains("\"external_service\":\"Sim\"", true) == true }
                item.copy(finalizedCount = fin, externalCount = ext)
            }
        }
        HistoryTimeFilter.YEAR -> {
            val months = mutableListOf<ChartBarData>()
            for (i in 5 downTo 0) {
                val mCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -i)
                }
                val rawLabel = sdfMonth.format(mCal.time).replace(".", "").trim()
                val mLabel = if (rawLabel.isNotEmpty()) rawLabel.substring(0, 1).uppercase() + rawLabel.substring(1) else "M\u00EAs"

                val mOrders = orders.filter { o ->
                    val millis = getOrderDateMillis(o) ?: return@filter false
                    val oCal = Calendar.getInstance().apply { timeInMillis = millis }
                    oCal.get(Calendar.YEAR) == mCal.get(Calendar.YEAR) &&
                    oCal.get(Calendar.MONTH) == mCal.get(Calendar.MONTH)
                }

                val fin = mOrders.count { it.status.equals("Finalizada", true) || it.status.equals("Finalizado", true) || it.status.equals("Conclu\u00EDda", true) || it.status.equals("Concluida", true) }
                val ext = mOrders.count { it.tecnicoResponsavel?.lowercase()?.trim() == "externo" || it.solucaoAplicada?.contains("\"external_service\":\"Sim\"", true) == true }
                months.add(ChartBarData(label = mLabel, finalizedCount = fin, externalCount = ext))
            }
            months
        }
        HistoryTimeFilter.ALL -> {
            val months = mutableListOf<ChartBarData>()
            for (i in 5 downTo 0) {
                val mCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -i)
                }
                val rawLabel = sdfMonth.format(mCal.time).replace(".", "").trim()
                val mLabel = if (rawLabel.isNotEmpty()) rawLabel.substring(0, 1).uppercase() + rawLabel.substring(1) else "M\u00EAs"

                val mOrders = orders.filter { o ->
                    val millis = getOrderDateMillis(o) ?: return@filter false
                    val oCal = Calendar.getInstance().apply { timeInMillis = millis }
                    oCal.get(Calendar.YEAR) == mCal.get(Calendar.YEAR) &&
                    oCal.get(Calendar.MONTH) == mCal.get(Calendar.MONTH)
                }

                val fin = mOrders.count { it.status.equals("Finalizada", true) || it.status.equals("Finalizado", true) || it.status.equals("Conclu\u00EDda", true) || it.status.equals("Concluida", true) }
                val ext = mOrders.count { it.tecnicoResponsavel?.lowercase()?.trim() == "externo" || it.solucaoAplicada?.contains("\"external_service\":\"Sim\"", true) == true }
                months.add(ChartBarData(label = mLabel, finalizedCount = fin, externalCount = ext))
            }
            months
        }
    }
}

private fun getOrderDateMillis(order: WorkOrder): Long? {
    val rawDate = order.dataFim
        ?: order.dataAbertura
        ?: extractJsonField(order.solucaoAplicada, "final_date")
        ?: extractJsonField(order.solucaoAplicada, "date")
        ?: return null
    return parseDateToMillis(rawDate)
}

private fun isOrderInTimeFilter(order: WorkOrder, filter: HistoryTimeFilter): Boolean {
    if (filter == HistoryTimeFilter.ALL) return true

    val orderMillis = getOrderDateMillis(order) ?: return true
    val now = System.currentTimeMillis()
    val diffMillis = now - orderMillis

    if (diffMillis < 0) return true

    val daysDiff = diffMillis / (1000L * 60 * 60 * 24)

    return when (filter) {
        HistoryTimeFilter.WEEK -> daysDiff <= 7
        HistoryTimeFilter.MONTH -> daysDiff <= 30
        HistoryTimeFilter.YEAR -> daysDiff <= 365
        HistoryTimeFilter.ALL -> true
    }
}

private fun parseDateToMillis(raw: String): Long? {
    val clean = raw.trim()
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "dd-MM-yyyy"
    )
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.getDefault())
            val date = sdf.parse(clean)
            if (date != null) return date.time
        } catch (_: Exception) {}
    }
    try {
        if (clean.length >= 10 && clean[4] == '-' && clean[7] == '-') {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(clean.substring(0, 10))
            if (date != null) return date.time
        }
    } catch (_: Exception) {}
    return null
}

// ── Helper de extração JSON ──────────────────────────────────────────────────
private fun extractJsonField(json: String?, key: String): String? {
    if (json.isNullOrBlank()) return null
    return Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
}
