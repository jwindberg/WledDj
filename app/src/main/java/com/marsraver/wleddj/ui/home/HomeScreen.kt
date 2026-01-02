package com.marsraver.wleddj.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marsraver.wleddj.model.Installation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    installations: List<Installation>,
    onCreateClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onInstallationClick: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var installToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Installations") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.navigationBarsPadding() // Prevent nav bar overlap
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Installation")
            }
        }
    ) { padding ->
        if (installations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No installations found. Create one!")
            }
        } else {
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(installations) { install ->
                    InstallationItem(
                        installation = install,
                        onClick = { onInstallationClick(install.id) },
                        onDelete = { 
                            installToDelete = install.id
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("New Installation") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreateClick(newName)
                                newName = ""
                                showDialog = false
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteConfirm && installToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirm = false 
                    installToDelete = null
                },
                title = { Text("Delete Installation?") },
                text = { Text("Are you sure you want to delete this installation?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            installToDelete?.let { onDeleteClick(it) }
                            showDeleteConfirm = false
                            installToDelete = null
                        }
                    ) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showDeleteConfirm = false 
                        installToDelete = null
                    }) {
                        Text("No")
                    }
                }
            )
        }
    }
}

@Composable
fun InstallationItem(
    installation: Installation,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = installation.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "Devices: ${installation.devices.size}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
