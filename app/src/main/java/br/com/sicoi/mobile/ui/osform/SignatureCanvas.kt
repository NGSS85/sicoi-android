package br.com.sicoi.mobile.ui.osform

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import br.com.sicoi.mobile.ui.theme.*

/**
 * Componente de Canvas para Assinatura Digital.
 *
 * O técnico ou responsável desenha a assinatura com o dedo.
 * Ao finalizar, exporta um Bitmap em PNG para ser enviado ao Supabase Storage.
 */
@Composable
fun SignatureCanvas(
    modifier: Modifier = Modifier,
    onSignatureChanged: (Bitmap?) -> Unit
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    val currentPath = remember { mutableStateListOf<Offset>() }
    var hasSignature by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Column(modifier = modifier) {
        // Label
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Assinatura Digital",
                style = MaterialTheme.typography.titleMedium.copy(color = SicoiTextPrimary)
            )
            if (hasSignature) {
                TextButton(
                    onClick = {
                        paths.clear()
                        currentPath.clear()
                        hasSignature = false
                        onSignatureChanged(null)
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, tint = SicoiError, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Limpar", style = MaterialTheme.typography.bodyMedium.copy(color = SicoiError))
                }
            }
        }

        // Área de desenho
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(
                    width = if (hasSignature) 1.5.dp else 1.dp,
                    color = if (hasSignature) SicoiSuccess.copy(alpha = 0.5f) else SicoiCardBorder,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath.clear()
                                currentPath.add(offset)
                            },
                            onDrag = { change, _ ->
                                currentPath.add(change.position)
                                hasSignature = true
                            },
                            onDragEnd = {
                                paths.add(currentPath.toList())
                                currentPath.clear()

                                // Exporta o bitmap após cada traço
                                val bitmap = exportBitmap(paths, canvasSize)
                                onSignatureChanged(bitmap)
                            }
                        )
                    }
            ) {
                canvasSize = this.size

                // Linha guia
                drawLine(
                    color = Color.LightGray,
                    start = Offset(size.width * 0.1f, size.height * 0.7f),
                    end = Offset(size.width * 0.9f, size.height * 0.7f),
                    strokeWidth = 1f
                )

                // Desenha os traços já completados
                val paint = Paint().apply {
                    color = Color.Black
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                for (path in paths) {
                    if (path.size < 2) continue
                    for (i in 1 until path.size) {
                        drawLine(
                            color = Color.Black,
                            start = path[i - 1],
                            end = path[i],
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Desenha o traço atual
                if (currentPath.size >= 2) {
                    for (i in 1 until currentPath.size) {
                        drawLine(
                            color = Color.Black,
                            start = currentPath[i - 1],
                            end = currentPath[i],
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Placeholder quando vazio
                if (!hasSignature) {
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(size.width * 0.1f, size.height * 0.5f),
                        end = Offset(size.width * 0.9f, size.height * 0.5f),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }
            }

            // Placeholder de texto
            if (!hasSignature) {
                Text(
                    "Assine aqui",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.LightGray),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Instrução
        Text(
            if (hasSignature) "✓ Assinatura registrada" else "Toque e arraste para assinar",
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (hasSignature) SicoiSuccess else SicoiTextMuted
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** Exporta os caminhos desenhados como Bitmap PNG */
private fun exportBitmap(paths: List<List<Offset>>, size: androidx.compose.ui.geometry.Size): Bitmap? {
    if (size.width <= 0 || size.height <= 0) return null
    val bitmap = Bitmap.createBitmap(size.width.toInt(), size.height.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 3f
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        isAntiAlias = true
    }

    for (path in paths) {
        if (path.size < 2) continue
        val androidPath = android.graphics.Path()
        androidPath.moveTo(path[0].x, path[0].y)
        for (i in 1 until path.size) {
            androidPath.lineTo(path[i].x, path[i].y)
        }
        canvas.drawPath(androidPath, paint)
    }

    return bitmap
}
