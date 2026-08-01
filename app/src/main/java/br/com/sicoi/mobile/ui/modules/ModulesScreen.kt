package br.com.sicoi.mobile.ui.modules

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sicoi.mobile.ui.theme.*
import br.com.sicoi.mobile.ui.settings.UserSettingsDialog

/**
 * Tela 2: Seleção de Módulos
 *
 * 5 módulos listados verticalmente:
 * - 4 desativados ("Em desenvolvimento") com lock visual
 * - 1 ativo: Manutenção Industrial
 */
data class SicoiModule(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isActive: Boolean,
    val accentColor: Color
)

@Composable
fun ModulesScreen(
    userName: String = "Técnico",
    onNavigateToMaintenance: () -> Unit,
    onLogout: () -> Unit
) {

    val modules = listOf(
        SicoiModule("ferramentaria", "Ferramentaria",
            "Gestão de dispositivos, ferramentas e controle de ativos.",
            Icons.Default.Build, isActive = false, SicoiWarning),
        SicoiModule("qualidade", "Qualidade",
            "Instrumentos de medição e relatórios dimensionais.",
            Icons.Default.VerifiedUser, isActive = false, SicoiBlueLight),
        SicoiModule("engenharia", "Engenharia",
            "Documentação técnica e fluxogramas de processos.",
            Icons.Default.Engineering, isActive = false, SicoiSuccess),
        SicoiModule("projetos", "Projetos",
            "Relatórios, cronogramas e gestão de desenvolvimento.",
            Icons.Default.FolderOpen, isActive = false, Color(0xFFE879F9)),
        SicoiModule("manutencao", "Manutenção Industrial",
            "Ordens de serviço, técnicos e ativos industriais.",
            Icons.Default.Settings, isActive = true, SicoiOrange),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SicoiBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SicoiSurface, SicoiBackground)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Olá, ${userName.split(" ").firstOrNull() ?: "Usuário"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SicoiTextMuted
                            )
                            Text(
                                "Escolha o módulo",
                                style = MaterialTheme.typography.headlineLarge,
                                color = SicoiTextPrimary
                            )
                        }
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SicoiCard)
                                .border(1.dp, SicoiCardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "Sair", tint = SicoiError.copy(alpha = 0.8f))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(SicoiSuccess, shape = RoundedCornerShape(50))
                        )
                        Text(
                            "Sistema Online",
                            style = MaterialTheme.typography.labelSmall,
                            color = SicoiSuccess
                        )
                    }
                }
            }

            // Módulos
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                modules.forEach { module ->
                    ModuleCard(
                        module = module,
                        onClick = if (module.isActive) {
                            { onNavigateToMaintenance() }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer
            Text(
                "SICOI Mobile v1.0 · MC Industrial",
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = SicoiTextMuted.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModuleCard(
    module: SicoiModule,
    onClick: (() -> Unit)?
) {
    val borderColor = if (module.isActive)
        module.accentColor.copy(alpha = 0.4f)
    else
        SicoiCardBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (module.isActive) SicoiCard else SicoiSurface
        ),
        border = BorderStroke(
            width = if (module.isActive) 1.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ícone do módulo
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (module.isActive)
                            module.accentColor.copy(alpha = 0.15f)
                        else
                            SicoiCardBorder.copy(alpha = 0.5f),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    module.icon,
                    contentDescription = null,
                    tint = if (module.isActive) module.accentColor else SicoiTextMuted,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Texto
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    module.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (module.isActive) SicoiTextPrimary else SicoiTextMuted
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                if (module.isActive) {
                    Text(
                        module.description,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = SicoiTextSecondary)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SicoiTextMuted, modifier = Modifier.size(11.dp))
                        Text(
                            "Em desenvolvimento",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                            color = SicoiTextMuted
                        )
                    }
                }
            }

            // Seta ou lock
            if (module.isActive) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = module.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = SicoiCardBorder,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
