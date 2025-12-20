package com.example.wleddj.data.repository

import android.content.Context
import com.example.wleddj.data.model.Installation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

interface InstallationRepository {
    val installations: Flow<List<Installation>>
    suspend fun createInstallation(name: String)
    suspend fun deleteInstallation(id: String)
    suspend fun getInstallation(id: String): Installation?
    suspend fun updateInstallation(installation: Installation)
}

class FileInstallationRepository(private val context: Context) : InstallationRepository {
    private val _installations = MutableStateFlow<List<Installation>>(emptyList())
    override val installations = _installations.asStateFlow()

    private val directory: File
        get() = File(context.filesDir, "installations").apply { mkdirs() }

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadInstallations()
    }

    private fun loadInstallations() {
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

    override suspend fun createInstallation(name: String) = withContext(Dispatchers.IO) {
        val newInstall = Installation(name = name)
        saveToFile(newInstall)
        loadInstallations() // Refresh
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

    override suspend fun updateInstallation(installation: Installation) = withContext(Dispatchers.IO) {
        saveToFile(installation)
        loadInstallations()
    }

    private fun saveToFile(installation: Installation) {
        val file = File(directory, "${installation.id}.json")
        file.writeText(json.encodeToString(installation))
    }
}
