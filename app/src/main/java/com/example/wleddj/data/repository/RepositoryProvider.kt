package com.example.wleddj.data.repository

import android.content.Context

object RepositoryProvider {
    @Volatile
    private var repository: InstallationRepository? = null

    fun getRepository(context: Context): InstallationRepository {
        return repository ?: synchronized(this) {
            val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
            repository ?: FileInstallationRepository(context.applicationContext, appScope).also { repository = it }
        }
    }
}
