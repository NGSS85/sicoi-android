package br.com.sicoi.mobile.ui.toolshop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.sicoi.mobile.data.model.ToolWorkOrder
import br.com.sicoi.mobile.data.repository.ToolWorkOrderRepository
import br.com.sicoi.mobile.ui.login.sicoiTextFieldColors
import br.com.sicoi.mobile.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolTechnicianHistoryScreen(
    technicianName: String,
    onNavigateBack: () -> Unit,
    onSelectOrder: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val repo: ToolWorkOrdersViewModel = hiltViewModel()

    LaunchedEffect(technicianName) {
        // Carrega histórico
        repo.fetchOrders(technicianName)
    }

    val state by repo.state.collectAsState()
    val allOrders = (state as? ToolOrdersUiState.Success)?.orders ?: emptyList()

    val filtered = allOrders.filter {
        val q = searchQuery.lowercase()
        (it.numeroOs?.lowercase()?.contains(q) == true) ||
        (it.codigoFerramenta?.lowercase()?.contains(q) == true) ||
        (it.nomeFerramenta?.lowercase()?.contains(q) == true) ||
        (it.solicitante?.lowercase()?.contains(q) == true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico Ferramentaria", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1D19))
            )
        },
        containerColor = SicoiBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar por código, molde ou solicitante") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SicoiWarning) },
                colors = sicoiTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, tint = SicoiTextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nenhuma ordem encontrada no histórico.", color = SicoiTextMuted)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectOrder(order.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1D19))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(order.numeroOs ?: "FER", fontWeight = FontWeight.Bold, color = SicoiWarning)
                                    Text(order.status, color = if (order.isConcluded()) SicoiSuccess else SicoiBlueLight, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${order.getFullToolCode()} - ${order.getFullToolName()}", color = Color.White, fontWeight = FontWeight.Medium)
                                Text("Solicitante: ${order.getFullRequester()}", color = SicoiTextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
