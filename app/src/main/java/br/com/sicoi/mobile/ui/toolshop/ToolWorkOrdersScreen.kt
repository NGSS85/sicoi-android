package br.com.sicoi.mobile.ui.toolshop

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.sicoi.mobile.data.model.ToolWorkOrder
import br.com.sicoi.mobile.data.repository.ToolWorkOrderRepository
import br.com.sicoi.mobile.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ToolOrdersUiState {
    object Loading : ToolOrdersUiState()
    data class Success(val orders: List<ToolWorkOrder>) : ToolOrdersUiState()
    data class Error(val message: String) : ToolOrdersUiState()
}

@HiltViewModel
class ToolWorkOrdersViewModel @Inject constructor(
    private val repository: ToolWorkOrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ToolOrdersUiState>(ToolOrdersUiState.Loading)
    val state: StateFlow<ToolOrdersUiState> = _state.asStateFlow()

    fun fetchOrders(technicianName: String?) {
        viewModelScope.launch {
            _state.value = ToolOrdersUiState.Loading
            repository.fetchOpenOrders(technicianName).fold(
                onSuccess = { _state.value = ToolOrdersUiState.Success(it) },
                onFailure = { _state.value = ToolOrdersUiState.Error(it.message ?: "Erro ao carregar OS de Ferramentaria") }
            )
        }
    }

    fun assignToMe(osId: String, technicianName: String) {
        viewModelScope.launch {
            _state.value = ToolOrdersUiState.Loading
            repository.assignTechnician(osId, technicianName).fold(
                onSuccess = { fetchOrders(technicianName) },
                onFailure = { _state.value = ToolOrdersUiState.Error(it.message ?: "Erro ao assumir O.S.") }
            )
        }
    }

    fun reactivateOrder(osId: String, technicianName: String) {
        viewModelScope.launch {
            _state.value = ToolOrdersUiState.Loading
            repository.updateWorkOrderStatus(osId, "Em Execução", pauseReason = null).fold(
                onSuccess = { fetchOrders(technicianName) },
                onFailure = { _state.value = ToolOrdersUiState.Error(it.message ?: "Erro ao reativar O.S.") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolWorkOrdersScreen(
    technicianId: String,
    technicianName: String,
    canOpenOs: Boolean = true,
    onNavigateBack: () -> Unit,
    onSelectWorkOrder: (workOrderId: String) -> Unit,
    onNavigateToHistory: (technicianName: String) -> Unit = {},
    onNavigateToPausedOrders: (technicianName: String) -> Unit = {},
    onOpenNewOs: () -> Unit = {},
    viewModel: ToolWorkOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var selectedFilterTab by remember { mutableStateOf("minhas") } // "minhas" ou "todas"

    LaunchedEffect(technicianName) {
        viewModel.fetchOrders(technicianName)
    }

    val allOrders = (state as? ToolOrdersUiState.Success)?.orders ?: emptyList()
    val myActiveOrders = allOrders.filter {
        it.tecnicoResponsavel?.equals(technicianName, ignoreCase = true) == true && !it.isPaused()
    }
    val unassignedOrders = allOrders.filter {
        it.tecnicoResponsavel.isNullOrBlank() ||
        it.tecnicoResponsavel.equals("Não Atribuído", ignoreCase = true) ||
        it.tecnicoResponsavel.equals("Aguardando técnico", ignoreCase = true)
    }
    val pausedOrders = allOrders.filter { it.isPaused() }

    val displayedOrders = when (selectedFilterTab) {
        "minhas" -> myActiveOrders
        "livres" -> unassignedOrders
        "pausadas" -> pausedOrders
        else -> allOrders
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Color(0xFF161512)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SicoiWarning.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = SicoiWarning, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                "Ferramentaria",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text("Menu de Ações", style = MaterialTheme.typography.bodySmall, color = SicoiTextMuted)
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Item: Minhas O.S.
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.BuildCircle, contentDescription = null, tint = SicoiWarning) },
                        label = { Text("O.S. Ativas (${myActiveOrders.size})", color = Color.White) },
                        selected = selectedFilterTab == "minhas",
                        onClick = {
                            selectedFilterTab = "minhas"
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SicoiWarning.copy(alpha = 0.2f),
                            unselectedContainerColor = Color.Transparent
                        )
                    )

                    // Item: Não Atribuídas
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AssignmentLate, contentDescription = null, tint = SicoiBlueLight) },
                        label = { Text("Livre / Não Atribuídas (${unassignedOrders.size})", color = Color.White) },
                        selected = selectedFilterTab == "livres",
                        onClick = {
                            selectedFilterTab = "livres"
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SicoiBlueLight.copy(alpha = 0.2f),
                            unselectedContainerColor = Color.Transparent
                        )
                    )

                    // Item: Pausadas
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.PauseCircle, contentDescription = null, tint = Color(0xFFF97316)) },
                        label = { Text("O.S. Pausadas (${pausedOrders.size})", color = Color.White) },
                        selected = selectedFilterTab == "pausadas",
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToPausedOrders(technicianName)
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFF97316).copy(alpha = 0.2f),
                            unselectedContainerColor = Color.Transparent
                        )
                    )

                    // Item: Histórico Concluído
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.History, contentDescription = null, tint = SicoiSuccess) },
                        label = { Text("Histórico Geral", color = Color.White) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToHistory(technicianName)
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SicoiSuccess.copy(alpha = 0.2f),
                            unselectedContainerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E1D19), SicoiBackground)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SicoiWarning.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = SicoiWarning, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    "Ferramentaria",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                    color = Color.White
                                )
                            }

                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filtros rápidos em Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedFilterTab == "minhas",
                                onClick = { selectedFilterTab = "minhas" },
                                label = { Text("Minhas (${myActiveOrders.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SicoiWarning.copy(alpha = 0.25f),
                                    selectedLabelColor = SicoiWarning
                                )
                            )
                            FilterChip(
                                selected = selectedFilterTab == "livres",
                                onClick = { selectedFilterTab = "livres" },
                                label = { Text("Livres (${unassignedOrders.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SicoiBlueLight.copy(alpha = 0.25f),
                                    selectedLabelColor = SicoiBlueLight
                                )
                            )
                            FilterChip(
                                selected = selectedFilterTab == "pausadas",
                                onClick = { selectedFilterTab = "pausadas" },
                                label = { Text("Pausa (${pausedOrders.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFF97316).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFFF97316)
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (canOpenOs) {
                    ExtendedFloatingActionButton(
                        onClick = onOpenNewOs,
                        containerColor = SicoiWarning,
                        contentColor = Color.Black,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Nova O.S.", fontWeight = FontWeight.Bold) }
                    )
                }
            },
            containerColor = SicoiBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (val curState = state) {
                    is ToolOrdersUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SicoiWarning)
                        }
                    }
                    is ToolOrdersUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = SicoiError, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(curState.message, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.fetchOrders(technicianName) },
                                colors = ButtonDefaults.buttonColors(containerColor = SicoiWarning, contentColor = Color.Black)
                            ) {
                                Text("Tentar Novamente")
                            }
                        }
                    }
                    is ToolOrdersUiState.Success -> {
                        if (displayedOrders.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SicoiSuccess, modifier = Modifier.size(56.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Nenhuma O.S. nesta aba!",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Tudo em dia na Ferramentaria ou não há ordens com este filtro.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SicoiTextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                items(displayedOrders, key = { it.id }) { order ->
                                    ToolOrderCard(
                                        order = order,
                                        technicianName = technicianName,
                                        onClick = { onSelectWorkOrder(order.id) },
                                        onAssign = { viewModel.assignToMe(order.id, technicianName) },
                                        onReactivate = { viewModel.reactivateOrder(order.id, technicianName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolOrderCard(
    order: ToolWorkOrder,
    technicianName: String,
    onClick: () -> Unit,
    onAssign: () -> Unit,
    onReactivate: () -> Unit
) {
    val isMyOrder = order.tecnicoResponsavel?.equals(technicianName, ignoreCase = true) == true
    val isUnassigned = order.tecnicoResponsavel.isNullOrBlank() ||
            order.tecnicoResponsavel.equals("Não Atribuído", ignoreCase = true)

    val priorityColor = when (order.prioridade?.lowercase()) {
        "emergência", "emergencia" -> SicoiError
        "urgente" -> Color(0xFFF97316)
        else -> SicoiBlueLight
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1D19)),
        border = BorderStroke(1.2.dp, if (isMyOrder) SicoiWarning.copy(alpha = 0.5f) else SicoiCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Linha do Topo: Número da OS + Prioridade + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SicoiWarning.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            order.numeroOs ?: "FER-S/N",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = SicoiWarning
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            order.prioridade ?: "Normal",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = priorityColor
                        )
                    }
                }

                // Status chip
                val statusBg = if (order.isPaused()) Color(0xFFF97316).copy(alpha = 0.2f) else SicoiSuccess.copy(alpha = 0.18f)
                val statusColor = if (order.isPaused()) Color(0xFFF97316) else SicoiSuccess
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        order.status,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tag / Código da Ferramenta e Nome
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = SicoiWarning, modifier = Modifier.size(20.dp))
                Column {
                    Text(
                        "${order.getFullToolCode()} - ${order.getFullToolName()}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Tipo: ${order.tipoFerramenta ?: "Molde"} · Máquina/Prensa: ${order.getFullMachine()}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = SicoiTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Descrição do Problema
            Text(
                order.descricaoProblema ?: "Sem descrição informada.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Solicitante + Ação
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Solicitante: ${order.getFullRequester()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = SicoiTextMuted
                    )
                    Text(
                        "Técnico: ${order.getFullTechnician()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isMyOrder) SicoiWarning else SicoiTextMuted
                    )
                }

                if (isUnassigned) {
                    Button(
                        onClick = onAssign,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SicoiWarning, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Assumir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (order.isPaused() && isMyOrder) {
                    Button(
                        onClick = onReactivate,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316), contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retomar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SicoiWarning.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SicoiWarning),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Atender", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
