package com.marsraver.wleddj.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment

import androidx.compose.ui.res.stringResource
import com.marsraver.wleddj.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marsraver.wleddj.engine.color.Palette
import com.marsraver.wleddj.ui.components.PalettePickerDialog

@Composable
fun AnimationControlsBar(
    state: PlayerViewModel.AnimationControlsState,
    onPrimaryColorChange: (Int) -> Unit,
    onSecondaryColorChange: (Int) -> Unit,
    onPaletteChange: (Palette) -> Unit,
    onTextChange: (String) -> Unit
) {
    if (!state.hasSelection) return

    val hasControls = state.supportsPrimary || state.supportsSecondary || state.supportsPalette || state.supportsText
    if (!hasControls) return

    var showPrimaryPicker by remember { mutableStateOf(false) }
    var showSecondaryPicker by remember { mutableStateOf(false) }
    var showPalettePicker by remember { mutableStateOf(false) }
    var showTextPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp), // Align height roughly with FAB
        horizontalArrangement = Arrangement.Start, // Left aligned, next to FAB on right?
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.supportsPrimary) {
            ControlItem(
                label = stringResource(R.string.label_primary), 
                color = state.primaryColor, 
                onClick = { showPrimaryPicker = true }
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        if (state.supportsSecondary) {
            ControlItem(
                label = stringResource(R.string.label_secondary), 
                color = state.secondaryColor, 
                onClick = { showSecondaryPicker = true }
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        if (state.supportsPalette) {
             Button(
                 onClick = { showPalettePicker = true },
                 colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                 shape = RoundedCornerShape(8.dp)
             ) {
                 Text(state.currentPalette.displayName)
             }
        }
        
        if (state.supportsText) {
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { showTextPicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.action_text))
            }
        }
    }
    
    // ... (rest of file)

    if (showPrimaryPicker) {
        com.marsraver.wleddj.ui.components.ProColorPickerDialog(
            initialColor = state.primaryColor,
            onColorSelected = { 
                onPrimaryColorChange(it)
                showPrimaryPicker = false 
            },
            onDismiss = { showPrimaryPicker = false }
        )
    }

    if (showSecondaryPicker) {
        com.marsraver.wleddj.ui.components.ProColorPickerDialog(
            initialColor = state.secondaryColor,
            onColorSelected = { 
                onSecondaryColorChange(it)
                showSecondaryPicker = false 
            },
            onDismiss = { showSecondaryPicker = false }
        )
    }

    if (showPalettePicker) {
        PalettePickerDialog(
            palettes = Palette.entries,
            onPaletteSelected = { 
                onPaletteChange(it)
                showPalettePicker = false 
            },
            onDismiss = { showPalettePicker = false }
        )
    }

    if (showTextPicker) {
        com.marsraver.wleddj.ui.components.TextInputDialog(
            initialText = state.currentText,
            onTextEntered = {
                onTextChange(it)
                showTextPicker = false
            },
            onDismiss = { showTextPicker = false }
        )
    }
}

@Composable
fun ControlItem(label: String, color: Int, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(color), RoundedCornerShape(4.dp))
                .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
        )
    }
}
