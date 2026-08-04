package br.com.sicoi.mobile.ui.technicians

import androidx.compose.animation.AnimatedVisibility
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.sicoi.mobile.data.model.Technician
import br.com.sicoi.mobile.data.repository.WorkOrderRepository
import br.com.sicoi.mobile.ui.login.sicoiTextFieldColors
import br.com.sicoi.mobile.ui.theme.*

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel
sealed class TechniciansUiState {
    object Loading : TechniciansUiState()
    data class Success(val technicians: List<Technician>) : TechniciansUiState()
    data class Error(val message: String) : TechniciansUiState()
}

@HiltViewModel
class TechniciansViewModel @Inject constructor(
    private val repository: WorkOrderRepository
) : ViewModel() {
    private val _state = MutableStateFlow<TechniciansUiState>(TechniciansUiState.Loading)
    val state: StateFlow<TechniciansUiState> = _state.asStateFlow()

    init { fetchTechnicians() }

    fun fetchTechnicians() {
        viewModelScope.launch {
            _state.value = TechniciansUiState.Loading
            repository.fetchTechnicians().fold(
                onSuccess = { _state.value = TechniciansUiState.Success(it) },
                onFailure = { _state.value = TechniciansUiState.Error(it.message ?: "Erro ao carregar técnicos") }
            )
        }
    }
}

/**
 * Tela 3: Seleção de Técnicos (Módulo Manutenção Industrial)
 *
 * Lista os técnicos ativos da tabela ind_maint_technicians.
 * Ao clicar em um técnico, abre a lista de OS daquele técnico.
 */
@Composable
fun TechniciansScreen(
    onNavigateBack: () -> Unit,
    onSelectTechnician: (technicianId: String, technicianName: String) -> Unit,
    onNavigateToPinList: () -> Unit = {},
    viewModel: TechniciansViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    var selectedTech by remember { mutableStateOf<Technician?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    if (showPinDialog && selectedTech != null) {
        AlertDialog(
            onDismissRequest = { 
                showPinDialog = false
                pinInput = ""
                pinError = false
            },
            title = { Text("Acesso Restrito", color = SicoiTextPrimary) },
            text = {
                Column {
                    Text("Digite o PIN para acessar como ${selectedTech!!.name}:", color = SicoiTextSecondary, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { 
                            pinInput = it
                            pinError = false
                        },
                        label = { Text("PIN de Acesso") },
                        isError = pinError,
                        singleLine = true,
                        colors = sicoiTextFieldColors()
                    )
                    if (pinError) {
                        Text("PIN incorreto. O padrão é 2839.", color = SicoiError, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pinInput == "2839" || pinInput == selectedTech!!.pin) {
                        showPinDialog = false
                        pinInput = ""
                        pinError = false
                        onSelectTechnician(selectedTech!!.id, selectedTech!!.name)
                    } else {
                        pinError = true
                    }
                }) {
                    Text("Entrar", color = SicoiOrange)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPinDialog = false
                    pinInput = ""
                    pinError = false
                }) {
                    Text("Cancelar", color = SicoiTextMuted)
                }
            },
            containerColor = SicoiCard
        )
    }

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
                Column {
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "Manutenção Industrial Mobile",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                            letterSpacing = 0.3.sp
                        ),
                        color = Color(0xFFF59E0B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Frase instrucional centralizada com setas
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = SicoiTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Usuarios abrir Formulario",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.4.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = SicoiTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = SicoiTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Botão Formulário de OS - fundo branco, texto escuro negrito, largura 85%
            Button(
                onClick = onNavigateToPinList,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1A237E)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF1A237E).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color(0xFF1A237E),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Abrir - Formulário O.S",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4. Título da Seção
            Text(
                "Técnicos, Abaixo",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    letterSpacing = 0.5.sp
                ),
                color = SicoiTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Barra de busca / Filtro
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar técnico...", color = SicoiTextMuted) },
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
                            Text("Carregando técnicos...", style = MaterialTheme.typography.bodyMedium, color = SicoiTextMuted)
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
                    // Remove duplicatas e nomes excluídos
                    val seenRodrigo = mutableSetOf<String>()
                    val seenLuiz = mutableSetOf<String>()
                    val filtered = s.technicians
                        .filter { tech ->
                            val hasValidName = tech.name.isNotBlank() &&
                                               !tech.name.contains("Sem cadastro", ignoreCase = true) &&
                                               !tech.name.contains("Sem nome", ignoreCase = true) &&
                                               !tech.name.equals("Nenhum", ignoreCase = true) &&
                                               !tech.name.equals("João Silva", ignoreCase = true)
                            val isTecnico = tech.role.equals("Técnico", ignoreCase = true) ||
                                            tech.role.equals("Ambos", ignoreCase = true) ||
                                            tech.role.isBlank()
                            val isNotDuplicate = when {
                                tech.name.contains("Rodrigo", ignoreCase = true) -> seenRodrigo.add(tech.name)
                                tech.name.contains("Luiz", ignoreCase = true)    -> seenLuiz.add(tech.name)
                                else -> true
                            }
                            hasValidName && isTecnico && isNotDuplicate &&
                                (searchQuery.isBlank() || tech.name.contains(searchQuery, ignoreCase = true))
                        }

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (searchQuery.isBlank()) "Nenhum técnico cadastrado."
                                else "Nenhum técnico encontrado para \"$searchQuery\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SicoiTextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            "${filtered.size} técnico${if (filtered.size != 1) "s" else ""} disponíve${if (filtered.size != 1) "is" else "l"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SicoiTextMuted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered, key = { it.id }) { tech ->
                                TechnicianCard(
                                    technician = tech,
                                    onClick = { 
                                        selectedTech = tech
                                        showPinDialog = true
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

@Composable
private fun TechnicianCard(technician: Technician, onClick: () -> Unit) {
    // Gera uma cor de avatar baseada no nome
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
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar circular com iniciais
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(avatarColor.copy(alpha = 0.25f), CircleShape)
                    .border(2.dp, avatarColor.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = avatarColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    technician.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = SicoiTextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SicoiSuccess, CircleShape)
                    )
                    Text(
                        technician.status,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SicoiSuccess
                        )
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SicoiOrange,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
