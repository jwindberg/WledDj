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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.marsraver.wleddj.R
import com.marsraver.wleddj.engine.color.Palette

@Composable
fun PalettePickerDialog(
    palettes: List<Palette>,
    onPaletteSelected: (Palette) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Palette") },
        text = {
            Column(modifier = Modifier.heightIn(max=300.dp)) {
                androidx.compose.foundation.lazy.LazyColumn {
                     items(palettes.size) { index ->
                         val pal = palettes[index]
                         TextButton(
                             onClick = { onPaletteSelected(pal) },
                             modifier = Modifier.fillMaxWidth()
                         ) {
                             Text(pal.displayName)
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
    var selectedPalette by remember { mutableStateOf(Palette.STANDARD) }
    var expanded by remember { mutableStateOf(false) }
    val colors = selectedPalette.colors

    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.label_source, selectedPalette.displayName))
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp) // Limit height
            ) {
                Palette.entries.filter { it != Palette.STANDARD }.forEach { pal ->
                    DropdownMenuItem(
                        text = { Text(pal.displayName) },
                        onClick = {
                            selectedPalette = pal
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
