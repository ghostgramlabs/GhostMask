package com.ghostgramlabs.ghostmask

import android.os.Bundle
import androidx.activity.viewModels
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ghostgramlabs.ghostmask.security.BiometricAuthManager
import com.ghostgramlabs.ghostmask.ui.components.GradientButton
import com.ghostgramlabs.ghostmask.ui.navigation.GhostMaskNavGraph
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurface
import com.ghostgramlabs.ghostmask.ui.theme.GhostMaskTheme
import com.ghostgramlabs.ghostmask.ui.theme.TextPrimary
import com.ghostgramlabs.ghostmask.ui.theme.TextSecondary
import com.ghostgramlabs.ghostmask.viewmodel.AppSecurityViewModel

/**
 * Single-activity entry point for GhostMask.
 * Uses Jetpack Compose with edge-to-edge display.
 */
class MainActivity : FragmentActivity() {
    private val appSecurityViewModel: AppSecurityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GhostMaskTheme {
                val navController = rememberNavController()
                val uiState by appSecurityViewModel.uiState.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                LaunchedEffect(currentRoute) {
                    appSecurityViewModel.onRouteChanged(currentRoute)
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    GhostMaskNavGraph(
                        navController = navController,
                        appSecurityViewModel = appSecurityViewModel
                    )
                    if (appSecurityViewModel.shouldRequireUnlock(currentRoute)) {
                        AppLockOverlay(
                            activity = this@MainActivity,
                            state = uiState,
                            onUnlockWithPin = appSecurityViewModel::unlockWithPin,
                            onBiometricSuccess = appSecurityViewModel::onBiometricUnlockSucceeded,
                            onBiometricError = appSecurityViewModel::onBiometricUnlockFailed,
                            onDismissMessage = appSecurityViewModel::clearMessage
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appSecurityViewModel.onAppForegrounded()
    }

    override fun onStop() {
        appSecurityViewModel.onAppBackgrounded()
        super.onStop()
    }
}

@Composable
private fun AppLockOverlay(
    activity: FragmentActivity,
    state: com.ghostgramlabs.ghostmask.viewmodel.AppSecurityUiState,
    onUnlockWithPin: (String) -> Unit,
    onBiometricSuccess: () -> Unit,
    onBiometricError: (String) -> Unit,
    onDismissMessage: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val settings = state.settings ?: return
    val biometricManager = remember(activity) { BiometricAuthManager(activity) }
    var biometricPromptRequested by remember(state.unlockPending, state.currentRoute) { mutableStateOf(false) }

    LaunchedEffect(state.unlockPending, state.currentRoute, settings.biometricEnabled, state.pinConfigured) {
        if (
            state.unlockPending &&
            settings.biometricEnabled &&
            biometricManager.canAuthenticate() &&
            !biometricPromptRequested
        ) {
            biometricPromptRequested = true
            biometricManager.authenticate(
                title = "Unlock GhostMask",
                subtitle = "Authenticate to continue",
                onSuccess = onBiometricSuccess,
                onError = onBiometricError
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("GhostMask Locked", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Text("Authenticate to continue.", color = TextSecondary)

                if (settings.biometricEnabled && biometricManager.canAuthenticate()) {
                    GradientButton(
                        text = "Unlock with biometrics",
                        onClick = {
                            biometricPromptRequested = true
                            biometricManager.authenticate(
                                title = "Unlock GhostMask",
                                subtitle = "Authenticate to continue",
                                onSuccess = onBiometricSuccess,
                                onError = onBiometricError
                            )
                        }
                    )
                }

                if (state.pinConfigured) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            pin = it.filter(Char::isDigit).take(8)
                            if (state.errorMessage != null) onDismissMessage()
                        },
                        label = { Text("PIN") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    GradientButton(
                        text = "Unlock with PIN",
                        onClick = {
                            onUnlockWithPin(pin)
                            pin = ""
                        },
                        enabled = pin.length >= 4
                    )
                }

                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
