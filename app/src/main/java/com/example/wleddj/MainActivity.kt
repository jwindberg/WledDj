package com.example.wleddj

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wleddj.data.repository.FileInstallationRepository
import com.example.wleddj.ui.editor.LayoutEditorScreen
import com.example.wleddj.ui.home.HomeScreen
import com.example.wleddj.ui.player.PlayerScreen
import com.example.wleddj.ui.theme.WledDjTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Manual DI for now
        val repository = FileInstallationRepository(applicationContext)

        setContent {
            WledDjTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val scope = rememberCoroutineScope()
                    val installations by repository.installations.collectAsState()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                installations = installations,
                                onCreateClick = { name ->
                                    scope.launch { repository.createInstallation(name) }
                                },
                                onDeleteClick = { id ->
                                    scope.launch { repository.deleteInstallation(id) }
                                },
                                onInstallationClick = { id ->
                                    navController.navigate("editor/$id")
                                }
                            )
                        }
                        composable("editor/{installationId}") { backStackEntry ->
                            val installationId = backStackEntry.arguments?.getString("installationId")
                            LayoutEditorScreen(
                                installationId = installationId,
                                onBack = { navController.popBackStack() },
                                onPlay = { navController.navigate("player/$installationId") }
                            )
                        }
                        composable("player/{installationId}") { backStackEntry ->
                            val installationId = backStackEntry.arguments?.getString("installationId")
                            PlayerScreen(
                                installationId = installationId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}