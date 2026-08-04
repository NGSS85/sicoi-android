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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

// ─────────────────────────────────────────────────────────────
// ViewModel dedicado a Solicitantes (lê user_profiles aprovados)
// ─────────────────────────────────────────────────────────────
sealed class RequestersUiState {
    object Loading : RequestersUiState()
    data class Success(val requesters: List<Technician>) : RequestersUiState()
    data class Error(val message: String) : RequestersUiState()
}

@HiltViewModel
class RequestersViewModel @Inject constructor(
    private val repository: WorkOrderRepository
) : ViewModel() {
    private val _state = MutableStateFlow<RequestersUiState>(RequestersUiState.Loading)
    val state: StateFlow<RequestersUiState> = _state.asStateFlow()

    init { fetchRequesters() }

    fun fetchRequesters() {
        viewModelScope.launch {
            _state.value = RequestersUiState.Loading
            repository.fetchRequesters().fold(
                onSuccess = { _state.value = RequestersUiState.Success(it) },
                onFailure = { _state.value = RequestersUiState.Error(it.message ?: "Erro ao carregar solicitantes") }
            )
        }
    }
}

/**
 * Tela de Solicitantes Cadastrados.
 * Lista usuários aprovados pelo admin na Gestão de Acessos (web).
 * Ao clicar no nome, solicita o PIN cadastrado pelo solicitante.
 */
@Composable
fun TechniciansPinScreen(
    onNavigateBack: () -> Unit,
    onSelectTechnician: (technicianId: String, technicianName: String) -> Unit,
    viewModel: RequestersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Estado do diálogo de PIN
    var selectedRequester by remember { mutableStateOf<Technician?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var pinVisible by remember { mutableStateOf(false) }

    // ── Diálogo de verificação de PIN ────────────────────────
    if (showPinDialog && selectedRequester != null) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pinInput = ""
                pinError = false
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(SicoiOrange.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = SicoiOrange, modifier = Modifier.size(26.dp))
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Verificação de Acesso",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = SicoiTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        selectedRequester!!.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SicoiOrange,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Digite seu PIN de acesso para continuar:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SicoiTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it.filter { c -> c.isDigit() }.take(8)
                            pinError = false
                        },
                        label = { Text("PIN de Acesso") },
                        isError = pinError,
                        singleLine = true,
                        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { pinVisible = !pinVisible }) {
                                Icon(
                                    if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = SicoiTextMuted
                                )
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        colors = sicoiTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = SicoiError, modifier = Modifier.size(14.dp))
                            Text(
                                "PIN incorreto. Verifique e tente novamente.",
                                style = MaterialTheme.typography.labelSmall.copy(color = SicoiError)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput == selectedRequester!!.pin) {
                            showPinDialog = false
                            pinInput = ""
                            pinError = false
                            onSelectTechnician(selectedRequester!!.id, selectedRequester!!.name)
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Entrar", fontWeight = FontWeight.Bold)
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

    // ── Scaffold principal ───────────────────────────────────
    Scaffold(
        containerColor = SicoiBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(SicoiSurface, SicoiBackground)))
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

                    IconButton(onClick = { viewModel.fetchRequesters() }) {
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
                    "Selecione seu nome e insira seu PIN",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = SicoiTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Aprovados pelo administrador via Gestão de Acessos",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.3.sp
                    ),
                    color = SicoiTextMuted,
                    textAlign = TextAlign.Center
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar solicitante...", color = SicoiTextMuted) },
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
                is RequestersUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SicoiOrange)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Carregando solicitantes...", style = MaterialTheme.typography.bodyMedium, color = SicoiTextMuted)
                        }
                    }
                }
                is RequestersUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.WifiOff, contentDescription = null, tint = SicoiError, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(s.message, style = MaterialTheme.typography.bodyMedium, color = SicoiError, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.fetchRequesters() },
                                colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange)
                            ) {
                                Text("Tentar Novamente")
                            }
                        }
                    }
                }
                is RequestersUiState.Success -> {
                    val filtered = s.requesters.filter { tech ->
                        searchQuery.isBlank() || tech.name.contains(searchQuery, ignoreCase = true)
                    }

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.PersonOff,
                                    contentDescription = null,
                                    tint = SicoiTextMuted,
                                    modifier = Modifier.size(56.dp)
                                )
                                Text(
                                    if (searchQuery.isBlank())
                                        "Nenhum solicitante aprovado ainda."
                                    else
                                        "Nenhum resultado para \"$searchQuery\".",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = SicoiTextMuted,
                                    textAlign = TextAlign.Center
                                )
                                if (searchQuery.isBlank()) {
                                    Text(
                                        "Aguarde o administrador aprovar seu cadastro em:\nConfigurações → Gestão de Acessos",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SicoiTextMuted.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "${filtered.size} solicitante${if (filtered.size != 1) "s" else ""} disponíve${if (filtered.size != 1) "is" else "l"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SicoiTextMuted,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered, key = { it.id }) { tech ->
                                RequesterCard(
                                    technician = tech,
                                    onClick = {
                                        selectedRequester = tech
                                        pinInput = ""
                                        pinError = false
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
private fun RequesterCard(technician: Technician, onClick: () -> Unit) {
    val avatarColor = remember(technician.name) {
        val colors = listOf(SicoiOrange, SicoiBlue, SicoiSuccess, SicoiWarning, Color(0xFF7C3AED))
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
            // Avatar com iniciais
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(avatarColor.copy(alpha = 0.2f), CircleShape)
                    .border(2.dp, avatarColor.copy(alpha = 0.7f), CircleShape),
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
                        fontSize = 17.sp
                    ),
                    color = SicoiTextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SicoiSuccess, CircleShape)
                    )
                    Text(
                        "Aprovado · Toque para acessar",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SicoiSuccess
                        )
                    )
                }
            }

            // Botão indicativo de PIN
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SicoiOrange.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, SicoiOrange.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = SicoiOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "PIN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SicoiOrange,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
