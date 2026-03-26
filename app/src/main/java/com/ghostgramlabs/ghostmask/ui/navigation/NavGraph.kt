package com.ghostgramlabs.ghostmask.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ghostgramlabs.ghostmask.ui.screens.DashboardScreen
import com.ghostgramlabs.ghostmask.ui.screens.HelpAboutScreen
import com.ghostgramlabs.ghostmask.ui.screens.HideSecretsScreen
import com.ghostgramlabs.ghostmask.ui.screens.RevealSecretsScreen
import com.ghostgramlabs.ghostmask.ui.screens.SavedFilesScreen
import com.ghostgramlabs.ghostmask.ui.screens.SettingsScreen
import com.ghostgramlabs.ghostmask.data.reveal.RecentEncodedFilesStore
import androidx.compose.ui.platform.LocalContext
import com.ghostgramlabs.ghostmask.viewmodel.AppSecurityViewModel
import com.ghostgramlabs.ghostmask.viewmodel.HideSecretsViewModel
import com.ghostgramlabs.ghostmask.viewmodel.RevealSecretsViewModel

object Destinations {
    const val DASHBOARD = "dashboard"
    const val HIDE_SECRETS = "hide_secrets"
    const val REVEAL_SECRETS = "reveal_secrets"
    const val SAVED_FILES = "saved_files"
    const val SETTINGS = "settings"
    const val HELP = "help"
}

@Composable
fun GhostMaskNavGraph(
    navController: NavHostController,
    appSecurityViewModel: AppSecurityViewModel
) {
    val context = LocalContext.current
    val recentStore = RecentEncodedFilesStore(context)
    NavHost(
        navController = navController,
        startDestination = Destinations.DASHBOARD
    ) {
        composable(Destinations.DASHBOARD) {
            DashboardScreen(
                onHideSecrets = { navController.navigate(Destinations.HIDE_SECRETS) },
                onRevealSecrets = { navController.navigate(Destinations.REVEAL_SECRETS) },
                onSavedFiles = { navController.navigate(Destinations.SAVED_FILES) },
                onSettings = { navController.navigate(Destinations.SETTINGS) },
                onHelp = { navController.navigate(Destinations.HELP) }
            )
        }

        composable(Destinations.HIDE_SECRETS) {
            val viewModel: HideSecretsViewModel = viewModel()
            HideSecretsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    viewModel.reset()
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.REVEAL_SECRETS) {
            val viewModel: RevealSecretsViewModel = viewModel()
            RevealSecretsScreen(
                viewModel = viewModel,
                appSecurityViewModel = appSecurityViewModel,
                onNavigateBack = {
                    viewModel.reset()
                    navController.popBackStack()
                }
            )
        }

        composable(Destinations.SAVED_FILES) {
            SavedFilesScreen(
                recentStore = recentStore,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                viewModel = appSecurityViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.HELP) {
            HelpAboutScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
