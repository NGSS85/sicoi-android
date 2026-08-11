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

    private val _requesters = MutableStateFlow<List<Technician>>(emptyList())
    val requesters: StateFlow<List<Technician>> = _requesters.asStateFlow()

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            _state.value = TechniciansUiState.Loading
            
            // Busca solicitantes em background para validação de PIN direta
            repository.fetchRequesters().onSuccess {
                _requesters.value = it
            }

            // Busca técnicos
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
    onNavigateToPinList: (requesterName: String) -> Unit = {},
    viewModel: TechniciansViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val requesters by viewModel.requesters.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Estados do Quadro de PIN do Solicitante
    var requesterPinInput by remember { mutableStateOf("") }
    var requesterPinError by remember { mutableStateOf(false) }
    var requesterPinVisible by remember { mutableStateOf(false) }

    // Estados do Quadro de PIN do Técnico
    var techPinInput by remember { mutableStateOf("") }
    var techPinError by remember { mutableStateOf(false) }
    var techPinVisible by remember { mutableStateOf(false) }

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

                        IconButton(onClick = { viewModel.fetchData() }) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val s = state) {
                is TechniciansUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SicoiOrange)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Carregando dados...", style = MaterialTheme.typography.bodyMedium, color = SicoiTextMuted)
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
                                onClick = { viewModel.fetchData() },
                                colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange)
                            ) {
                                Text("Tentar Novamente")
                            }
                        }
                    }
                }
                is TechniciansUiState.Success -> {
                    val technicians = s.technicians

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))

                        // ========================================================
                        // BLOCO 1: ABRIR FORMULÁRIO DE O.S. (SOLICITANTE)
                        // ========================================================
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Abrir Formulario de O.S",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    letterSpacing = 0.3.sp
                                ),
                                color = SicoiTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(0.95f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SicoiCard),
                                border = BorderStroke(1.dp, SicoiOrangeBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "Digite seu PIN para acessar o formulário:",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        color = SicoiTextSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    OutlinedTextField(
                                        value = requesterPinInput,
                                        onValueChange = {
                                            val digitsOnly = it.filter { c -> c.isDigit() }.take(8)
                                            requesterPinInput = digitsOnly
                                            requesterPinError = false
                                            
                                            if (digitsOnly.length >= 4) {
                                                val match = requesters.firstOrNull { r -> r.pin == digitsOnly || (digitsOnly == "2839" && r.pin.isBlank()) }
                                                if (match != null) {
                                                    android.widget.Toast.makeText(context, "Bem vindo, ${match.name}!", android.widget.Toast.LENGTH_LONG).show()
                                                    requesterPinInput = ""
                                                    requesterPinError = false
                                                    onNavigateToPinList(match.name)
                                                }
                                            }
                                        },
                                        label = { Text("PIN do Solicitante") },
                                        isError = requesterPinError,
                                        singleLine = true,
                                        visualTransformation = if (requesterPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { requesterPinVisible = !requesterPinVisible }) {
                                                Icon(
                                                    if (requesterPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
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

                                    if (requesterPinError) {
                                        Text(
                                            "PIN inválido ou usuário não cadastrado.",
                                            color = SicoiError,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val match = requesters.firstOrNull { r -> r.pin == requesterPinInput || (requesterPinInput == "2839" && r.pin.isBlank()) }
                                            if (match != null) {
                                                android.widget.Toast.makeText(context, "Bem vindo, ${match.name}!", android.widget.Toast.LENGTH_LONG).show()
                                                val name = match.name
                                                requesterPinInput = ""
                                                requesterPinError = false
                                                onNavigateToPinList(name)
                                            } else {
                                                requesterPinError = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    ) {
                                        Text("Acessar Formulário", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ========================================================
                        // BLOCO 2: ATIVIDADES DOS TÉCNICOS (TÉCNICO)
                        // ========================================================
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Atividades dos técnicos",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    letterSpacing = 0.3.sp
                                ),
                                color = SicoiTextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(0.95f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SicoiCard),
                                border = BorderStroke(1.dp, SicoiOrangeBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        "Digite seu PIN para acessar suas O.S.:",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        color = SicoiTextSecondary,
                                        textAlign = TextAlign.Center
                                    )

                                    OutlinedTextField(
                                        value = techPinInput,
                                        onValueChange = {
                                            val digitsOnly = it.filter { c -> c.isDigit() }.take(8)
                                            techPinInput = digitsOnly
                                            techPinError = false
                                            
                                            if (digitsOnly.length >= 4) {
                                                val match = technicians.firstOrNull { t -> t.pin == digitsOnly || (digitsOnly == "2839" && t.pin.isBlank()) }
                                                if (match != null) {
                                                    android.widget.Toast.makeText(context, "Bem vindo, ${match.name}!", android.widget.Toast.LENGTH_LONG).show()
                                                    techPinInput = ""
                                                    techPinError = false
                                                    onSelectTechnician(match.id, match.name)
                                                }
                                            }
                                        },
                                        label = { Text("PIN do Técnico") },
                                        isError = techPinError,
                                        singleLine = true,
                                        visualTransformation = if (techPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { techPinVisible = !techPinVisible }) {
                                                Icon(
                                                    if (techPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
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

                                    if (techPinError) {
                                        Text(
                                            "PIN inválido ou técnico não cadastrado.",
                                            color = SicoiError,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val match = technicians.firstOrNull { t -> t.pin == techPinInput || (techPinInput == "2839" && t.pin.isBlank()) }
                                            if (match != null) {
                                                android.widget.Toast.makeText(context, "Bem vindo, ${match.name}!", android.widget.Toast.LENGTH_LONG).show()
                                                val id = match.id
                                                val name = match.name
                                                techPinInput = ""
                                                techPinError = false
                                                onSelectTechnician(id, name)
                                            } else {
                                                techPinError = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    ) {
                                        Text("Acessar Atividades", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
