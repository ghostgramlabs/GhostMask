package com.ghostgramlabs.ghostmask.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.ghostmask.ui.components.GhostMaskTitle
import com.ghostgramlabs.ghostmask.ui.components.GradientButton
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurface
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurfaceElevated
import com.ghostgramlabs.ghostmask.ui.theme.TextPrimary
import com.ghostgramlabs.ghostmask.ui.theme.TextSecondary
import com.ghostgramlabs.ghostmask.ui.theme.WarningYellow
import com.ghostgramlabs.ghostmask.viewmodel.AppSecurityViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: AppSecurityViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings ?: return
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = { GhostMaskTitle("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsBlock("App Lock") {
                ToggleRow(
                    "Enable app lock",
                    settings.appLockEnabled,
                    viewModel::updateAppLockEnabled,
                    "Keeps the entire app behind authentication."
                )
                ToggleRow(
                    "Use biometrics/device credential",
                    settings.biometricEnabled,
                    viewModel::updateBiometricEnabled,
                    "Uses fingerprint, face, or device credential on the lock screen."
                )
                ToggleRow(
                    "Lock on every launch",
                    settings.lockOnLaunch,
                    viewModel::updateLockOnLaunch,
                    "Requires authentication whenever the app is opened."
                )
                Text("Authentication protects the entire app.", color = TextSecondary)
                Text("Auto-lock timeout", color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Immediately", 1 to "1 min", 5 to "5 min", 15 to "15 min").forEach { (minutes, label) ->
                        FilterChip(
                            selected = settings.autoLockTimeoutMinutes == minutes,
                            onClick = { viewModel.updateAutoLockTimeoutMinutes(minutes) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            SettingsBlock("PIN") {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                    label = { Text(if (settings.pinConfigured) "New PIN" else "Set PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it.filter(Char::isDigit).take(8) },
                    label = { Text("Confirm PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                GradientButton(
                    text = if (settings.pinConfigured) "Change PIN" else "Save PIN",
                    onClick = {
                        if (pin.length >= 4 && pin == confirmPin) {
                            viewModel.savePin(pin)
                            pin = ""
                            confirmPin = ""
                        }
                    },
                    enabled = pin.length >= 4 && pin == confirmPin
                )
                if (settings.pinConfigured) {
                    TextButton(onClick = viewModel::clearPin) {
                        Text("Remove PIN", color = WarningYellow)
                    }
                }
            }

            SettingsBlock("Privacy Defaults") {
                ToggleRow(
                    "Remember recent encoded files",
                    settings.rememberRecentEncodedFiles,
                    viewModel::updateRememberRecentFiles,
                    "Stores only encoded image references, never revealed plaintext."
                )
                ToggleRow(
                    "Default secure view",
                    settings.defaultSecureView,
                    viewModel::updateDefaultSecureView,
                    "Turns on protected reveal behavior by default for new secrets."
                )
                ToggleRow(
                    "Default screenshot blocking",
                    settings.defaultScreenshotBlocking,
                    viewModel::updateDefaultScreenshotBlocking,
                    "Preselects screenshot blocking for new secrets."
                )
                ToggleRow(
                    "Cleanup temp files aggressively",
                    settings.cleanupTempFiles,
                    viewModel::updateCleanupTempFiles,
                    "Clears temporary private-cache artifacts when sessions end."
                )
                ToggleRow(
                    "Save encoded PNGs privately by default",
                    settings.saveToPrivateStorage,
                    viewModel::updateSaveToPrivateStorage,
                    "Saves encoded files inside app storage unless you explicitly export them."
                )
                ToggleRow(
                    "Show warning dialogs",
                    settings.warningDialogsEnabled,
                    viewModel::updateWarningDialogs,
                    "Keeps extra safety prompts visible for sensitive actions."
                )
                HorizontalDivider(color = DarkSurfaceElevated)
                TextButton(onClick = viewModel::clearSensitiveCache) {
                    Text("Clear temp cache now", color = TextSecondary)
                }
            }

            uiState.errorMessage?.let {
                Card(colors = CardDefaults.cardColors(containerColor = WarningYellow.copy(alpha = 0.1f))) {
                    Text(it, modifier = Modifier.padding(12.dp), color = WarningYellow)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsBlock(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextSecondary)
            subtitle?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(it, color = WarningYellow.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
