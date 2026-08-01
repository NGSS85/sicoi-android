package br.com.sicoi.mobile.ui.osform

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import br.com.sicoi.mobile.ui.theme.*
import java.io.File

/**
 * Componente para captura de 2 fotos (Antes e Depois) do equipamento.
 * Usa a câmera nativa do Android via ActivityResultContracts.TakePicture.
 */
@Composable
fun CameraCapture(
    modifier: Modifier = Modifier,
    beforeBitmap: Bitmap?,
    afterBitmap: Bitmap?,
    onBeforeCaptured: (Uri) -> Unit,
    onAfterCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current

    // URIs temporárias para cada foto
    var beforeUri by remember { mutableStateOf<Uri?>(null) }
    var afterUri  by remember { mutableStateOf<Uri?>(null) }

    // Launchers da câmera
    val beforeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) beforeUri?.let { onBeforeCaptured(it) }
    }

    val afterLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) afterUri?.let { onAfterCaptured(it) }
    }

    Column(modifier = modifier) {
        Text(
            "Registro Fotográfico",
            style = MaterialTheme.typography.titleMedium.copy(color = SicoiTextPrimary),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Foto ANTES
            PhotoSlot(
                modifier = Modifier.weight(1f),
                label = "Antes",
                icon = Icons.Default.CameraAlt,
                bitmap = beforeBitmap,
                accentColor = SicoiWarning,
                onCapture = {
                    val uri = createTempImageUri(context, "before_${System.currentTimeMillis()}")
                    beforeUri = uri
                    beforeLauncher.launch(uri)
                }
            )

            // Foto DEPOIS
            PhotoSlot(
                modifier = Modifier.weight(1f),
                label = "Depois",
                icon = Icons.Default.CameraAlt,
                bitmap = afterBitmap,
                accentColor = SicoiSuccess,
                onCapture = {
                    val uri = createTempImageUri(context, "after_${System.currentTimeMillis()}")
                    afterUri = uri
                    afterLauncher.launch(uri)
                }
            )
        }

        Text(
            "Fotografe o estado do equipamento antes e após o serviço",
            style = MaterialTheme.typography.labelSmall.copy(color = SicoiTextMuted),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun PhotoSlot(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bitmap: Bitmap?,
    accentColor: Color,
    onCapture: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (bitmap != null) Color.Black else SicoiSurface)
                .border(
                    width = if (bitmap != null) 1.5.dp else 1.dp,
                    color = if (bitmap != null) accentColor.copy(alpha = 0.5f) else SicoiCardBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onCapture),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Overlay para retake
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Cached,
                        contentDescription = "Retomar foto",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Tirar Foto",
                        style = MaterialTheme.typography.labelSmall.copy(color = SicoiTextMuted)
                    )
                }
            }
        }

        // Label com status
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (bitmap != null) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (bitmap != null) accentColor else SicoiTextMuted
                )
            )
        }
    }
}

/** Cria um arquivo temporário e retorna seu URI via FileProvider */
private fun createTempImageUri(context: Context, fileName: String): Uri {
    val file = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(file, "$fileName.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}
