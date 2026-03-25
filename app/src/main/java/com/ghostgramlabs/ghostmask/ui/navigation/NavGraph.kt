package com.ghostgramlabs.ghostmask.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ghostgramlabs.ghostmask.ui.screens.DashboardScreen
import com.ghostgramlabs.ghostmask.ui.screens.HideSecretsScreen
import com.ghostgramlabs.ghostmask.ui.screens.RevealSecretsScreen
import com.ghostgramlabs.ghostmask.viewmodel.HideSecretsViewModel
import com.ghostgramlabs.ghostmask.viewmodel.RevealSecretsViewModel

object Destinations {
    const val DASHBOARD = "dashboard"
    const val HIDE_SECRETS = "hide_secrets"
    const val REVEAL_SECRETS = "reveal_secrets"
}

@Composable
fun GhostMaskNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Destinations.DASHBOARD
    ) {
        composable(Destinations.DASHBOARD) {
            DashboardScreen(
                onHideSecrets = { navController.navigate(Destinations.HIDE_SECRETS) },
                onRevealSecrets = { navController.navigate(Destinations.REVEAL_SECRETS) }
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
                onNavigateBack = {
                    viewModel.reset()
                    navController.popBackStack()
                }
            )
        }
    }
}
