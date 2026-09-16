package br.com.sicoi.mobile.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sicoi.mobile.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Splash Screen animada do SICOI Mobile
 * Executa uma animação de abertura futurista e fluida antes da tela inicial (máximo ~2.2s).
 */
@Composable
fun SplashScreen(
    onAnimationFinish: () -> Unit
) {
    // ── Estados de Animação de Entrada ──
    var startAnimation by remember { mutableStateOf(false) }

    // Escala e opacidade do Logo central
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )

    val subtitleOffsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 25f,
        animationSpec = tween(durationMillis = 900, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "subtitleOffsetY"
    )

    // Barra de progresso animada (0% -> 100% em 1900ms)
    val progressAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1900, easing = LinearOutSlowInEasing),
        label = "progressAnim"
    )

    // Rotação sutil da engrenagem/ícone
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    // Dispara a animação ao abrir e navega após ~2.2 segundos
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200)
        onAnimationFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF070C18),
                        Color(0xFF020817)
                    ),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ── Efeitos de Brilho e Halo no Fundo ──
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SicoiOrange.copy(alpha = 0.35f),
                            SicoiBlue.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // ── Conteúdo Central ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // ── Emblema / Ícone do App ──
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(logoScale)
                    .alpha(contentAlpha),
                contentAlignment = Alignment.Center
            ) {
                // Anel externo sutil
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .rotate(rotationAngle)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    SicoiOrange,
                                    SicoiOrangeLight,
                                    SicoiBlue,
                                    Color.Transparent,
                                    SicoiOrange
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Container com efeito Glassmorphism escuro
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    SicoiOrange,
                                    SicoiOrange.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PrecisionManufacturing,
                        contentDescription = "SICOI",
                        tint = SicoiOrange,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Nome da Marca (SICOI) ──
            Text(
                text = "SICOI",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 8.sp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            SicoiOrange,
                            SicoiOrangeLight,
                            Color.White,
                            SicoiBlueLight
                        )
                    )
                ),
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(contentAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Subtítulo Elegante ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = subtitleOffsetY.dp)
                    .alpha(contentAlpha)
            ) {
                Text(
                    text = "SISTEMA DE CONTROLE INDUSTRIAL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.5.sp
                    ),
                    color = SicoiTextSecondaryDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Badge Versão Mobile
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SicoiOrange.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, SicoiOrange.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(SicoiSuccess, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MOBILE 2.0 • MANUTENÇÃO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = SicoiOrangeLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            // ── Barra de Progresso Futurista ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressAnim)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        SicoiOrangeDark,
                                        SicoiOrange,
                                        SicoiBlueLight
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val statusText = when {
                    progressAnim < 0.4f -> "Iniciando ambiente seguro..."
                    progressAnim < 0.8f -> "Carregando módulos operacionais..."
                    else -> "Pronto!"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = SicoiTextMutedDark,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
