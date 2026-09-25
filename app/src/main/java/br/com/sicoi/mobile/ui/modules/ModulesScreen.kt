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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sicoi.mobile.core.session.SessionManager
import br.com.sicoi.mobile.ui.theme.*

/**
 * Tela 2: Seleção de Módulos SICOI
 *
 * Módulos autorizados pelo Administrador recebem destaque especial
 * com efeito de luz intensa (glow radiante), enquanto módulos não liberados
 * permanecem com visual atenuado e bloqueados.
 *
 * O acesso ao módulo liberado redireciona diretamente:
 * - Solicitante -> Formulário de O.S.
 * - Técnico / Ambos -> Painel de Atividades do Técnico
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
    onNavigateToRequesterForm: (requesterName: String) -> Unit = {},
    onNavigateToTechnician: (technicianId: String, technicianName: String) -> Unit = { _, _ -> },
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by SessionManager.currentUser.collectAsState()

    val displayName = currentUser?.fullName?.takeIf { it.isNotBlank() } ?: userName
    val role = currentUser?.role ?: "Técnico"
    val userId = currentUser?.id ?: ""

    // Lista de módulos do sistema SICOI
    val baseModules = listOf(
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

    // Calcula permissões dinâmicas de cada módulo baseado no usuário
    val modules = baseModules.map { mod ->
        val isAllowed = SessionManager.isUserAllowedModule(mod.id) || (mod.id == "manutencao")
        mod.copy(isActive = isAllowed)
    }

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
                                "Olá, $displayName",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SicoiTextPrimary
                            )
                            Text(
                                "Perfil: $role",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                color = SicoiOrange
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themeController = LocalThemeController.current
                            IconButton(
                                onClick = { themeController.toggleTheme() },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SicoiCard)
                                    .border(1.dp, SicoiCardBorder, RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    if (themeController.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, 
                                    contentDescription = "Tema", 
                                    tint = SicoiTextPrimary
                                )
                            }
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SicoiCard)
                                    .border(1.dp, SicoiCardBorder, RoundedCornerShape(12.dp))
                            ) {
                                @Suppress("DEPRECATION")
                                Icon(Icons.Default.Logout, contentDescription = "Sair", tint = SicoiError.copy(alpha = 0.8f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Escolha o módulo",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = SicoiTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(SicoiSuccess, shape = RoundedCornerShape(50))
                        )
                        Text(
                            "Sistema Online · Acesso via PIN",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                            color = SicoiSuccess
                        )
                    }
                }
            }

            // Lista de Módulos
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                modules.forEach { module ->
                    ModuleCard(
                        module = module,
                        onClick = if (module.isActive) {
                            {
                                if (module.id == "manutencao") {
                                    if (role.equals("Solicitante", ignoreCase = true)) {
                                        onNavigateToRequesterForm(displayName)
                                    } else {
                                        // Técnico ou Ambos
                                        onNavigateToTechnician(userId, displayName)
                                    }
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Módulo ${module.title} em fase de integração.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Footer
            Text(
                "SICOI Mobile v1.0 · Acesso Autorizado",
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = SicoiTextMuted.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Card de Módulo com destaque de "Luz Mais Intensa" para o módulo liberado,
 * mantendo exatamente o mesmo tamanho e proporção dos demais módulos não abertos.
 */
@Composable
private fun ModuleCard(
    module: SicoiModule,
    onClick: (() -> Unit)?
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Efeito de LUZ MAIS INTENSA (aura/halo luminoso) para o módulo liberado
        if (module.isActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                module.accentColor.copy(alpha = 0.35f),
                                module.accentColor.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            radius = 450f
                        )
                    )
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .then(
                    if (onClick != null) Modifier.clickable { onClick() }
                    else Modifier
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (module.isActive) Color(0xFF1E1D19) else SicoiSurface
            ),
            border = BorderStroke(
                width = if (module.isActive) 1.8.dp else 1.dp,
                color = if (module.isActive) module.accentColor else SicoiCardBorder
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (module.isActive) {
                            Brush.horizontalGradient(
                                listOf(
                                    module.accentColor.copy(alpha = 0.16f),
                                    module.accentColor.copy(alpha = 0.04f),
                                    Color.Transparent
                                )
                            )
                        } else {
                            Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ícone do módulo (tamanho uniforme de 50.dp para todos)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                if (module.isActive)
                                    Brush.radialGradient(
                                        listOf(
                                            module.accentColor.copy(alpha = 0.30f),
                                            module.accentColor.copy(alpha = 0.12f)
                                        )
                                    )
                                else
                                    Brush.linearGradient(
                                        listOf(
                                            SicoiCardBorder.copy(alpha = 0.5f),
                                            SicoiCardBorder.copy(alpha = 0.5f)
                                        )
                                    ),
                                RoundedCornerShape(14.dp)
                            )
                            .border(
                                width = if (module.isActive) 1.2.dp else 0.dp,
                                color = if (module.isActive) module.accentColor.copy(alpha = 0.7f) else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
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

                    // Informações do módulo (Título + Subtítulo)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            module.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = if (module.isActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (module.isActive) Color.White else SicoiTextMuted
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        if (module.isActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = module.accentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    "Acesso Liberado · Manutenção",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = module.accentColor
                                    ),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SicoiTextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "Em desenvolvimento",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        letterSpacing = 0.sp,
                                        fontSize = 13.sp,
                                        color = SicoiTextMuted
                                    ),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Seta iluminada ou cadeado
                    if (module.isActive) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Acessar",
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
    }
}
