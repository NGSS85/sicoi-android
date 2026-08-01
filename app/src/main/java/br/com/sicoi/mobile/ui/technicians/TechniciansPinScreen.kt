package br.com.sicoi.mobile.ui.technicians

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.sicoi.mobile.data.model.Technician
import br.com.sicoi.mobile.ui.login.sicoiTextFieldColors
import br.com.sicoi.mobile.ui.theme.*

/**
 * Tela de Listagem de Técnicos com PINs para inserção de Formulário O.S. (Solicitante)
 */
@Composable
fun TechniciansPinScreen(
    onNavigateBack: () -> Unit,
    onSelectTechnician: (technicianId: String, technicianName: String) -> Unit,
    viewModel: TechniciansViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = SicoiBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(SicoiSurface, SicoiBackground))
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SicoiCard)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = SicoiTextSecondary)
                    }

                    IconButton(onClick = { viewModel.fetchTechnicians() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = SicoiTextSecondary)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Títulos abaixo da linha da seta de retorno (+40% de tamanho)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "SOLICITANTES CADASTRADOS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        letterSpacing = 1.2.sp
                    ),
                    color = SicoiOrange,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Selecione o Solicitante (PIN)",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    color = SicoiTextPrimary,
                    textAlign = TextAlign.Center
                )
            }

            // Barra de busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar solicitante ou PIN...", color = SicoiTextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SicoiTextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = SicoiTextMuted)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = sicoiTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val s = state) {
                is TechniciansUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SicoiOrange)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Carregando solicitantes...", style = MaterialTheme.typography.bodyMedium, color = SicoiTextMuted)
                        }
                    }
                }
                is TechniciansUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.WifiOff, contentDescription = null, tint = SicoiError, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(s.message, style = MaterialTheme.typography.bodyMedium, color = SicoiError, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.fetchTechnicians() },
                                colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange)
                            ) {
                                Text("Tentar Novamente")
                            }
                        }
                    }
                }
                is TechniciansUiState.Success -> {
                    val filtered = s.technicians.filter { tech ->
                        val hasValidName = tech.name.isNotBlank() &&
                                           !tech.name.contains("Sem cadastro", ignoreCase = true) &&
                                           !tech.name.contains("Sem nome", ignoreCase = true) &&
                                           !tech.name.equals("Nenhum", ignoreCase = true)
                        val isSolicitante = tech.role.equals("Solicitante", ignoreCase = true) || tech.role.equals("Ambos", ignoreCase = true)
                        hasValidName && isSolicitante && (
                            searchQuery.isBlank() ||
                            tech.name.contains(searchQuery, ignoreCase = true) ||
                            (tech.pin != null && tech.pin.contains(searchQuery, ignoreCase = true))
                        )
                    }

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (searchQuery.isBlank()) "Nenhum solicitante cadastrado."
                                else "Nenhum solicitante encontrado para \"$searchQuery\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SicoiTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            "${filtered.size} solicitante${if (filtered.size != 1) "s" else ""} cadastrado${if (filtered.size != 1) "s" else ""} com PIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = SicoiTextMuted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered, key = { it.id }) { tech ->
                                TechnicianPinCard(
                                    technician = tech,
                                    onClick = { onSelectTechnician(tech.id, tech.name) }
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

@Composable
private fun TechnicianPinCard(technician: Technician, onClick: () -> Unit) {
    val avatarColor = remember(technician.name) {
        val colors = listOf(SicoiOrange, SicoiBlue, SicoiSuccess, SicoiWarning, Color(0xFFE879F9))
        colors[technician.name.length % colors.size]
    }
    val initials = technician.name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SicoiCard),
        border = BorderStroke(1.dp, SicoiCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar circular com iniciais
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(avatarColor.copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, avatarColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = avatarColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    technician.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SicoiTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = "PIN",
                        tint = SicoiOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "PIN: ${technician.pin}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SicoiOrange
                        )
                    )
                }
            }

            // Badge PIN Destacado
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SicoiOrange.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, SicoiOrange.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Selecionar",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SicoiOrange
                        )
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = SicoiOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
