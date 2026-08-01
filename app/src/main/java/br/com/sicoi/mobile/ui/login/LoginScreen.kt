package br.com.sicoi.mobile.ui.login

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.sicoi.mobile.ui.theme.*

/**
 * Tela 1: Login e Cadastro do SICOI Mobile
 *
 * Layout:
 * - Logo SICOI centralizada no topo
 * - Subtítulo "Sistema de Controle Industrial"
 * - Campos de E-mail e Senha
 * - Botão [Entrar] principal
 * - Link discreto [Criar Nova Conta]
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    val focusManager = LocalFocusManager.current
    
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("SicoiPrefs", android.content.Context.MODE_PRIVATE) }

    var email    by remember { mutableStateOf(prefs.getString("saved_email", "") ?: "") }
    var password by remember { mutableStateOf(prefs.getString("saved_password", "") ?: "") }
    var rememberMe by remember { mutableStateOf(prefs.getBoolean("remember_me", false)) }
    var showPassword by remember { mutableStateOf(false) }
    var showPendingDialog by remember { mutableStateOf(false) }
    var pendingMessage by remember { mutableStateOf("") }

    // Reage ao estado do login
    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginUiState.Success -> onLoginSuccess()
            is LoginUiState.PendingApproval -> {
                pendingMessage = (loginState as LoginUiState.PendingApproval).message
                showPendingDialog = true
            }
            else -> {}
        }
    }

    // Dialog de aprovação pendente
    if (showPendingDialog) {
        AlertDialog(
            onDismissRequest = {
                showPendingDialog = false
                viewModel.resetLoginState()
            },
            icon = { Icon(Icons.Default.HourglassTop, contentDescription = null, tint = SicoiWarning) },
            title = {
                Text(
                    "Cadastro Pendente",
                    style = MaterialTheme.typography.titleLarge,
                    color = SicoiTextPrimary
                )
            },
            text = {
                Text(
                    pendingMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SicoiTextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPendingDialog = false
                    viewModel.resetLoginState()
                }) {
                    Text("OK", color = SicoiOrange)
                }
            },
            containerColor = SicoiCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SicoiBackground, SicoiSurface, SicoiBackground)
                )
            )
    ) {
        // Decoração de fundo — círculos de luz
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset((-80).dp, (-50).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SicoiOrange.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(60.dp, 60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(SicoiBlue.copy(alpha = 0.06f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo + Identidade visual
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = br.com.sicoi.mobile.R.drawable.logo_sicoi),
                    contentDescription = "SICOI Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SICOI",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    letterSpacing = 6.sp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(SicoiOrange, SicoiOrangeLight, Color.White)
                    )
                )
            )

            Text(
                text = "Sistema de Controle Industrial",
                style = MaterialTheme.typography.bodyMedium.copy(
                    letterSpacing = 1.sp,
                    color = SicoiTextSecondary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Card do formulário
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SicoiCard),
                border = BorderStroke(1.dp, SicoiCardBorder)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Text(
                        "Acesso ao Sistema",
                        style = MaterialTheme.typography.titleMedium,
                        color = SicoiTextPrimary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Campo E-mail
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = SicoiTextMuted)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = sicoiTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Campo Senha
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SicoiTextMuted)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Ocultar" else "Mostrar",
                                    tint = SicoiTextMuted
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (rememberMe) {
                                    prefs.edit().putString("saved_email", email).putString("saved_password", password).putBoolean("remember_me", true).apply()
                                } else {
                                    prefs.edit().remove("saved_email").remove("saved_password").putBoolean("remember_me", false).apply()
                                }
                                viewModel.login(email, password)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = sicoiTextFieldColors()
                    )

                    // Mensagem de erro
                    AnimatedVisibility(visible = loginState is LoginUiState.Error) {
                        val errorMsg = (loginState as? LoginUiState.Error)?.message ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SicoiError.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = SicoiError, modifier = Modifier.size(16.dp))
                            Text(errorMsg, style = MaterialTheme.typography.bodyMedium.copy(color = SicoiError, fontSize = 12.sp))
                        }
                    }

                    // Checkbox Lembrar-me
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SicoiOrange,
                                uncheckedColor = SicoiTextMuted
                            )
                        )
                        Text(
                            "Lembrar meu acesso",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SicoiTextSecondary,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botão Entrar
                    Button(
                        onClick = { 
                            if (rememberMe) {
                                prefs.edit().putString("saved_email", email).putString("saved_password", password).putBoolean("remember_me", true).apply()
                            } else {
                                prefs.edit().remove("saved_email").remove("saved_password").putBoolean("remember_me", false).apply()
                            }
                            viewModel.login(email, password) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SicoiOrange,
                            contentColor = Color.White
                        ),
                        enabled = loginState !is LoginUiState.Loading
                    ) {
                        if (loginState is LoginUiState.Loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Entrar",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Link para cadastro
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Não tem uma conta? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SicoiTextMuted
                )
                TextButton(
                    onClick = onNavigateToSignup,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Criar Nova Conta",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SicoiOrange,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Badge da versão
            Text(
                "SICOI Mobile v1.0 · Ambiente Industrial",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = SicoiTextMuted.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun sicoiTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor       = SicoiOrange,
    unfocusedBorderColor     = SicoiCardBorder,
    focusedLabelColor        = SicoiOrange,
    unfocusedLabelColor      = SicoiTextMuted,
    cursorColor              = SicoiOrange,
    focusedTextColor         = SicoiTextPrimary,
    unfocusedTextColor       = SicoiTextPrimary,
    focusedContainerColor    = SicoiSurface,
    unfocusedContainerColor  = SicoiSurface
)
