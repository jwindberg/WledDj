package com.marsraver.wleddj.data.repository

import android.content.Context
import com.marsraver.wleddj.data.model.Installation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

interface InstallationRepository {
    val installations: StateFlow<List<Installation>>
    suspend fun createInstallation(name: String)
    suspend fun deleteInstallation(id: String)
    suspend fun getInstallation(id: String): Installation?
    suspend fun updateInstallation(installation: Installation)
}

class FileInstallationRepository(
    private val context: Context,
    private val externalScope: kotlinx.coroutines.CoroutineScope
) : InstallationRepository {
    private val _installations = MutableStateFlow<List<Installation>>(emptyList())
    override val installations = _installations.asStateFlow()

    private val directory: File
        get() = File(context.filesDir, "installations").apply { mkdirs() }

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadInstallations()
    }

    private fun loadInstallations() {
        // ... (Keep existing load logic or reload it? load logic is fine)
        val files = directory.listFiles { _, name -> name.endsWith(".json") }
        val loaded = files?.mapNotNull { file ->
            try {
                json.decodeFromString<Installation>(file.readText())
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } ?: emptyList()
        _installations.value = loaded
    }

    override suspend fun createInstallation(name: String) {
        val newInstall = Installation(name = name)
        externalScope.launch {
            saveToFile(newInstall)
            loadInstallations() 
        }.join() // Wait for creation? Usually user wants to see it.
        // Actually, let's just make it fire and forget + optimistic?
        // For creation, we usually want to wait to get the ID.
        // But here we construct ID in memory.
        // Let's stick to withContext for creation to be safe, or migrate all.
        // For Persistence Fix, focusing on updateInstallation.
    }

    override suspend fun deleteInstallation(id: String) = withContext(Dispatchers.IO) {
        File(directory, "$id.json").delete()
        loadInstallations()
    }

    override suspend fun getInstallation(id: String): Installation? = withContext(Dispatchers.IO) {
         _installations.value.find { it.id == id }
             ?: try {
                 val file = File(directory, "$id.json")
                 if (file.exists()) json.decodeFromString<Installation>(file.readText()) else null
             } catch (e: Exception) { null }
    }

    override suspend fun updateInstallation(installation: Installation) {
        // Optimistic Update: Update cache immediately
        val currentList = _installations.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == installation.id }
        if (index != -1) {
            currentList[index] = installation
        } else {
            currentList.add(installation)
        }
        _installations.value = currentList
        
        // Fire and Forget Save
        externalScope.launch {
            saveToFile(installation)
        }
    }

    private fun saveToFile(installation: Installation) {
        val file = File(directory, "${installation.id}.json")
        file.writeText(json.encodeToString(installation))
    }
}
