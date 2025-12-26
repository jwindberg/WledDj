package com.marsraver.wleddj.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize

@Composable
fun ProColorPickerDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentColor by remember { mutableStateOf(Color(initialColor)) }
    var selectedTab by remember { mutableStateOf(0) } // 0=Palette, 1=Spectrum, 2=RGB, 3=HSV, 4=Web
    val tabs = listOf("Palette", "Spectrum", "RGB", "HSV", "Web")

    // State for HSB/RGB syncing
    // We update these when tabs switch or internal changes happen
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var value by remember { mutableFloatStateOf(0f) }
    
    // Sync HSV from Color
    fun syncHsvFromColor(c: Color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(c.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }
    
    LaunchedEffect(Unit) {
        syncHsvFromColor(currentColor)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(min=350.dp)) {
                
                // Preview
                Box(
                     modifier = Modifier
                         .fillMaxWidth()
                         .height(40.dp)
                         .background(currentColor, RoundedCornerShape(4.dp))
                         .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { 
                                Text(
                                    text = title, 
                                    maxLines = 1, 
                                    style = MaterialTheme.typography.bodyMedium
                                ) 
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> PaletteTab(onColorPick = { 
                        currentColor = Color(it)
                        syncHsvFromColor(currentColor)
                    })
                    1 -> SpectrumTab(
                        hue = hue, saturation = saturation, value = value,
                        onHsvChange = { h, s, v ->
                            hue = h; saturation = s; value = v
                            currentColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
                        }
                    )
                    2 -> RgbTab(
                        color = currentColor,
                        onColorChange = { 
                            currentColor = it 
                            syncHsvFromColor(it)
                        }
                    )
                    3 -> HsvTab(
                        hue = hue, saturation = saturation, value = value,
                        onHsvChange = { h, s, v ->
                            hue = h; saturation = s; value = v
                            currentColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
                        }
                    )
                    4 -> WebTab(
                        color = currentColor,
                        onColorChange = { 
                            currentColor = it
                            syncHsvFromColor(it)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor.toArgb()) }) { Text("Select") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// --- Tabs ---

@Composable
fun PaletteTab(onColorPick: (Int) -> Unit) {
    var selectedPaletteName by remember { mutableStateOf("Standard") }
    var expanded by remember { mutableStateOf(false) }
    val currentPalette = com.marsraver.wleddj.engine.color.Palettes.get(selectedPaletteName) 
        ?: com.marsraver.wleddj.engine.color.Palettes.getDefault()
    val colors = currentPalette.colors

    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Source: $selectedPaletteName")
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp) // Limit height
            ) {
                com.marsraver.wleddj.engine.color.Palettes.getNames().forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedPaletteName = name
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(250.dp)
        ) {
            items(colors.size) { index ->
                val c = Color(colors[index].toInt())
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(c, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onColorPick(c.toArgb()) }
                )
            }
        }
    }
}

@Composable
fun SpectrumTab(
    hue: Float, saturation: Float, value: Float,
    onHsvChange: (Float, Float, Float) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 2D Saturation/Value Box
        SaturationValueBox(
            hue = hue,
            saturation = saturation,
            value = value,
            onSatValChange = { s, v -> onHsvChange(hue, s, v) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hue Bar
        HueBar(
            hue = hue,
            onHueChange = { onHsvChange(it, saturation, value) },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline)
        )
    }
}



@Composable
fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onSatValChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color.hsv(hue, 1f, 1f)
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val w = boxSize.width.toFloat().coerceAtLeast(1f)
                        val h = boxSize.height.toFloat().coerceAtLeast(1f)
                        val s = (offset.x / w).coerceIn(0f, 1f)
                        val v = 1f - (offset.y / h).coerceIn(0f, 1f)
                        onSatValChange(s, v)
                        tryAwaitRelease()
                    }
                )
            }
            .pointerInput(Unit) {
               detectDragGestures { change, _ ->
                   val w = boxSize.width.toFloat().coerceAtLeast(1f)
                   val h = boxSize.height.toFloat().coerceAtLeast(1f)
                   val s = (change.position.x / w).coerceIn(0f, 1f)
                   val v = 1f - (change.position.y / h).coerceIn(0f, 1f)
                   onSatValChange(s, v)
               }
            }
    ) {
        // Layer 1: White -> Hue Color (Horizontal)
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(Color.White, color)
            )
        )
        // Layer 2: Transparent -> Black (Vertical)
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black)
            )
        )
        
        // Cursor
        val cx = saturation * size.width
        val cy = (1f - value) * size.height
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
        drawCircle(
             color = Color.Black,
             radius = 8.dp.toPx(),
             center = Offset(cx, cy),
             style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun HueBar(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    androidx.compose.foundation.Canvas(
         modifier = modifier
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val w = boxSize.width.toFloat().coerceAtLeast(1f)
                        val h = (offset.x / w).coerceIn(0f, 1f) * 360f
                        onHueChange(h)
                    }
                )
            }
            .pointerInput(Unit) {
               detectDragGestures { change, _ ->
                   val w = boxSize.width.toFloat().coerceAtLeast(1f)
                   val h = (change.position.x / w).coerceIn(0f, 1f) * 360f
                   onHueChange(h)
               }
            }
    ) {
        // Rainbow Gradient
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                )
            )
        )
        
        // Cursor
        val cx = (hue / 360f) * size.width
        drawLine(
            color = Color.White,
            start = Offset(cx, 0f),
            end = Offset(cx, size.height),
            strokeWidth = 4.dp.toPx()
        )
        drawLine(
            color = Color.Black,
            start = Offset(cx, 0f),
            end = Offset(cx, size.height),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun RgbTab(color: Color, onColorChange: (Color) -> Unit) {
    Column {
        val red = color.red
        val green = color.green
        val blue = color.blue
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R", Modifier.width(20.dp))
            Slider(value = red, onValueChange = { onColorChange(Color(it, green, blue)) }, modifier = Modifier.weight(1f))
            Text("${(red*255).toInt()}")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G", Modifier.width(20.dp))
            Slider(value = green, onValueChange = { onColorChange(Color(red, it, blue)) }, modifier = Modifier.weight(1f))
            Text("${(green*255).toInt()}")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B", Modifier.width(20.dp))
            Slider(value = blue, onValueChange = { onColorChange(Color(red, green, it)) }, modifier = Modifier.weight(1f))
            Text("${(blue*255).toInt()}")
        }
    }
}

@Composable
fun WebTab(color: Color, onColorChange: (Color) -> Unit) {
    var text by remember { mutableStateOf(String.format("#%06X", (color.toArgb() and 0xFFFFFF))) }
    
    // Update text if external color changes
    LaunchedEffect(color) {
        val newHex = String.format("#%06X", (color.toArgb() and 0xFFFFFF))
        if (text != newHex) text = newHex
    }

    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                try {
                    val parsed = android.graphics.Color.parseColor(it)
                    onColorChange(Color(parsed))
                } catch (e: Exception) { /* Ignore invalid */ }
            },
            label = { Text("Hex Code") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Web Colors (Coming Soon)", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PalettePickerDialog(
    paletteNames: List<String>,
    onPaletteSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Palette") },
        text = {
            Column(modifier = Modifier.heightIn(max=300.dp)) {
                androidx.compose.foundation.lazy.LazyColumn {
                     items(paletteNames.size) { index ->
                         val name = paletteNames[index]
                         TextButton(
                             onClick = { onPaletteSelected(name) },
                             modifier = Modifier.fillMaxWidth()
                         ) {
                             Text(name)
                         }
                     }
                }
            }
        },
        confirmButton = {
             TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
@Composable
fun HsvTab(
    hue: Float, saturation: Float, value: Float,
    onHsvChange: (Float, Float, Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("H", Modifier.width(20.dp), style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = hue, 
                onValueChange = { onHsvChange(it, saturation, value) }, 
                valueRange = 0f..360f, 
                modifier = Modifier.weight(1f)
            )
            Text("${hue.toInt()}°", Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("S", Modifier.width(20.dp), style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = saturation, 
                onValueChange = { onHsvChange(hue, it, value) }, 
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text("${(saturation*100).toInt()}%", Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("V", Modifier.width(20.dp), style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = value, 
                onValueChange = { onHsvChange(hue, saturation, it) }, 
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text("${(value*100).toInt()}%", Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}
