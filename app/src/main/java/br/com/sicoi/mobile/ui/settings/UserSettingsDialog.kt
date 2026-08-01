package br.com.sicoi.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sicoi.mobile.ui.login.sicoiTextFieldColors
import br.com.sicoi.mobile.ui.theme.*

/**
 * Diálogo de Configurações do Usuário:
 * Permite definir se o cadastro é "Solicitante" ou "Técnico" e definir seu PIN.
 */
@Composable
fun UserSettingsDialog(
    currentName: String = "Usuário",
    currentRole: String = "Solicitante",
    currentPin: String = "2839",
    onDismiss: () -> Unit,
    onSaveSettings: (role: String, pin: String) -> Unit
) {
    var selectedRole by remember { mutableStateOf(currentRole) }
    var pinInput by remember { mutableStateOf(currentPin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = SicoiOrange)
                Text(
                    "Configurações de Perfil",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SicoiTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Defina o perfil de acesso para $currentName e configure seu PIN pessoal:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SicoiTextSecondary
                )

                // Perfil: Solicitante ou Técnico
                Column {
                    Text(
                        "Tipo de Perfil *",
                        style = MaterialTheme.typography.labelSmall.copy(color = SicoiOrange, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedRole == "Solicitante",
                            onClick = { selectedRole = "Solicitante" },
                            label = { Text("Solicitante (Usuário)") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SicoiOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedRole == "Técnico",
                            onClick = { selectedRole = "Técnico" },
                            label = { Text("Técnico") },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SicoiOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // PIN de Acesso
                Column {
                    Text(
                        "PIN de Acesso Pessoal *",
                        style = MaterialTheme.typography.labelSmall.copy(color = SicoiOrange, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        placeholder = { Text("Ex: 2839") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = SicoiOrange) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = sicoiTextFieldColors()
                    )
                    Text(
                        "Fallback padrão: 2839",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = SicoiTextMuted),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveSettings(selectedRole, pinInput.ifBlank { "2839" }) },
                colors = ButtonDefaults.buttonColors(containerColor = SicoiOrange)
            ) {
                Text("Salvar Alterações")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SicoiTextMuted)
            }
        },
        containerColor = SicoiCard,
        shape = RoundedCornerShape(18.dp)
    )
}
