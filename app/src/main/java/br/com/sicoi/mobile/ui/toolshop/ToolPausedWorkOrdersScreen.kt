package br.com.sicoi.mobile.ui.toolshop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.sicoi.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolPausedWorkOrdersScreen(
    technicianName: String,
    onNavigateBack: () -> Unit,
    onSelectWorkOrder: (String) -> Unit,
    viewModel: ToolWorkOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(technicianName) {
        viewModel.fetchOrders(technicianName)
    }

    val allOrders = (state as? ToolOrdersUiState.Success)?.orders ?: emptyList()
    val pausedOrders = allOrders.filter { it.isPaused() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("O.S. de Ferramentaria em Pausa", fontWeight = FontWeight.Bold, color = Color.White) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (pausedOrders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PauseCircle, contentDescription = null, tint = SicoiTextMuted, modifier = Modifier.size(52.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Nenhuma ordem de ferramentaria pausada.", color = SicoiTextMuted)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(pausedOrders, key = { it.id }) { order ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectWorkOrder(order.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF241C16))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(order.numeroOs ?: "FER-S/N", fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
                                    Text("PAUSADA", fontWeight = FontWeight.Bold, color = Color(0xFFF97316), fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("${order.getFullToolCode()} - ${order.getFullToolName()}", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Motivo: ${order.motivoPausa ?: "Não informado"}", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)

                                if (!order.observacoesPausa.isNullOrBlank()) {
                                    Text("Obs: ${order.observacoesPausa}", color = SicoiTextMuted, fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        viewModel.reactivateOrder(order.id, technicianName)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316), contentColor = Color.White),
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retomar Atendimento", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
