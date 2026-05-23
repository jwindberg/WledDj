package com.marsraver.wleddj.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream

@Composable
fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    // Gesture state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    // Output settings - 64x64 is the recommended sweet spot for WLED grids
    var targetResolution by remember { mutableStateOf(64) }

    // Load original bitmap on launch
    LaunchedEffect(imageUri) {
        try {
            isLoading = true
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                originalBitmap = BitmapFactory.decodeStream(stream)
            }
            isLoading = false
        } catch (e: Exception) {
            android.util.Log.e("ImageCropDialog", "Failed to decode image from uri", e)
            loadError = true
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
            color = Color(0xFF121212), // Sleek deep premium dark mode
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "Crop & Scale Image",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Position and zoom your photo. It will be optimized for WLED panels.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Render area / Crop Viewport
                val density = LocalDensity.current
                val viewportDp = 260.dp
                val viewportPx = remember { with(density) { viewportDp.toPx() } }

                Box(
                    modifier = Modifier
                        .size(viewportDp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(2.dp, Color(0xFF2196F3).copy(alpha = 0.8f), RoundedCornerShape(12.dp)), // Vibrant HSL Blue crop box
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color(0xFF2196F3))
                    } else if (loadError || originalBitmap == null) {
                        Text(
                            text = "Error loading photo.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        // Interactive crop editor
                        Image(
                            bitmap = originalBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 6.0f)
                                        offset = offset + pan
                                    }
                                }
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Resolution Sweet Spot Selector
                Text(
                    text = "WLED Target Resolution",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val options = listOf(32, 64, 128, 256)
                    options.forEach { res ->
                        val isSelected = targetResolution == res
                        val label = when(res) {
                            64 -> "64x64\n(Rec.)"
                            else -> "${res}x${res}"
                        }
                        Button(
                            onClick = { targetResolution = res },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFF1E1E1E),
                                contentColor = if (isSelected) Color.White else Color.LightGray
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val original = originalBitmap
                            if (original != null) {
                                // Execute mathematical matrix cropping
                                val cropped = cropAndResize(
                                    bitmap = original,
                                    scale = scale,
                                    offset = offset,
                                    viewportSize = viewportPx,
                                    targetSize = targetResolution
                                )
                                // Save locally to private storage
                                val path = saveCroppedBitmap(context, cropped)
                                cropped.recycle()
                                onSuccess(path)
                            }
                        },
                        enabled = originalBitmap != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50), // Vibrant aesthetic green
                            contentColor = Color.White
                        )
                    ) {
                        Text("Apply & Place")
                    }
                }
            }
        }
    }
}

/**
 * High-fidelity pixel matrix crop and resize transformation engine.
 */
private fun cropAndResize(
    bitmap: Bitmap,
    scale: Float,
    offset: androidx.compose.ui.geometry.Offset,
    viewportSize: Float,
    targetSize: Int
): Bitmap {
    val matrix = android.graphics.Matrix()

    // 1. Calculate fit scale and center original bitmap to fit screen crop Box size
    val fitScale = minOf(viewportSize / bitmap.width, viewportSize / bitmap.height)
    val dx = (viewportSize - bitmap.width * fitScale) / 2f
    val dy = (viewportSize - bitmap.height * fitScale) / 2f
    matrix.postScale(fitScale, fitScale)
    matrix.postTranslate(dx, dy)

    // 2. Map multithreshold user pinch scale adjustments around viewport pivot center
    matrix.postScale(scale, scale, viewportSize / 2f, viewportSize / 2f)

    // 3. Map user panning offset translations
    matrix.postTranslate(offset.x, offset.y)

    // 4. Transform viewport pixel size back down to target pixel resolution (e.g. 64x64)
    val downscaleFactor = targetSize.toFloat() / viewportSize
    matrix.postScale(downscaleFactor, downscaleFactor)

    // 5. Draw high quality bicubic filtered bitmap onto optimized pixel surface
    val targetBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(targetBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(bitmap, matrix, paint)

    return targetBitmap
}

/**
 * Saves cropped image file to application local private cache folder.
 */
private fun saveCroppedBitmap(context: android.content.Context, bitmap: Bitmap): String {
    val imagesDir = File(context.filesDir, "images")
    if (!imagesDir.exists()) {
        imagesDir.mkdirs()
    }
    val file = File(imagesDir, "img_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return file.absolutePath
}
