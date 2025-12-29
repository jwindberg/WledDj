package com.marsraver.wleddj.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

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
