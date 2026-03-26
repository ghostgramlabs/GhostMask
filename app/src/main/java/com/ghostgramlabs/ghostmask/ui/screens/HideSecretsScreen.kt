package com.ghostgramlabs.ghostmask.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.ghostmask.domain.model.EmbeddingMode
import com.ghostgramlabs.ghostmask.domain.model.PayloadType
import com.ghostgramlabs.ghostmask.ui.components.CapacityMeter
import com.ghostgramlabs.ghostmask.ui.components.GradientButton
import com.ghostgramlabs.ghostmask.ui.components.GhostMaskTitle
import com.ghostgramlabs.ghostmask.ui.components.ImagePickerCard
import com.ghostgramlabs.ghostmask.ui.components.PasswordField
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurface
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurfaceElevated
import com.ghostgramlabs.ghostmask.ui.theme.ErrorRed
import com.ghostgramlabs.ghostmask.ui.theme.Purple60
import com.ghostgramlabs.ghostmask.ui.theme.SuccessGreen
import com.ghostgramlabs.ghostmask.ui.theme.TextMuted
import com.ghostgramlabs.ghostmask.ui.theme.TextPrimary
import com.ghostgramlabs.ghostmask.ui.theme.TextSecondary
import com.ghostgramlabs.ghostmask.ui.theme.WarningYellow
import com.ghostgramlabs.ghostmask.viewmodel.EncodingResult
import com.ghostgramlabs.ghostmask.viewmodel.HideSecretsViewModel
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HideSecretsScreen(
    viewModel: HideSecretsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val coverImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let(viewModel::setCoverImage) }

    val secretImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let(viewModel::setSecretImage) }

    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = { GhostMaskTitle("Hide Secret") },
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val encodingResult = uiState.encodingResult
            if (encodingResult != null) {
                SuccessSection(
                    result = encodingResult,
                    onSave = viewModel::saveToGallery,
                    onShare = {
                        viewModel.share { intent -> context.startActivity(intent) }
                    },
                    onNewEncoding = viewModel::reset,
                    isSaving = uiState.isSaving
                )
            } else {
                Text("Cover Image", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                ImagePickerCard(
                    imageUri = uiState.coverImageUri,
                    label = "PNG or image to use as the visible cover",
                    icon = Icons.Default.Image,
                    onClick = {
                        coverImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                if (uiState.coverImageUri != null) {
                    Text(
                        "${uiState.coverWidth} x ${uiState.coverHeight} px",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                HorizontalDivider(color = DarkSurfaceElevated)

                Text("Hide Mode", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PayloadType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.payloadType == type,
                            onClick = { viewModel.setPayloadType(type) },
                            label = { Text(type.name.lowercase().replaceFirstChar(Char::uppercase)) }
                        )
                    }
                }

                if (uiState.payloadType != PayloadType.IMAGE) {
                    OutlinedTextField(
                        value = uiState.secretText,
                        onValueChange = viewModel::setSecretText,
                        label = { Text("Secret text") },
                        placeholder = { Text("Enter the hidden message") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (uiState.payloadType != PayloadType.TEXT) {
                    ImagePickerCard(
                        imageUri = uiState.secretImageUri,
                        label = "Secret image",
                        icon = Icons.Default.HideImage,
                        onClick = {
                            secretImagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onClear = if (uiState.secretImageUri != null) viewModel::clearSecretImage else null
                    )
                }

                HorizontalDivider(color = DarkSurfaceElevated)

                PasswordField(value = uiState.password, onValueChange = viewModel::setPassword)
                PasswordField(
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::setConfirmPassword,
                    label = "Confirm password"
                )

                OutlinedTextField(
                    value = uiState.senderLabel,
                    onValueChange = viewModel::setSenderLabel,
                    label = { Text("Sender label (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Embedding Strength", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EmbeddingMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.embeddingMode == mode,
                            onClick = { viewModel.setEmbeddingMode(mode) },
                            label = { Text("${mode.label} (${mode.lsbBits}-bit)") }
                        )
                    }
                }

                uiState.embeddingMode.warning?.let { warning ->
                    AssistChip(onClick = {}, label = { Text(warning) })
                }

                SettingsCard(title = "Advanced Protection") {
                    ToggleRow("Secure view", uiState.revealFlags.secureView, viewModel::setSecureView)
                    ToggleRow("Block screenshots", uiState.revealFlags.blockScreenshots, viewModel::setBlockScreenshots)
                    ToggleRow("Hide from recents", uiState.revealFlags.hideFromRecents, viewModel::setHideFromRecents)
                    ToggleRow("Clear on close", uiState.revealFlags.clearOnClose, viewModel::setClearOnClose)
                    ToggleRow("Clear on background", uiState.revealFlags.clearOnBackground, viewModel::setClearOnBackground)
                    ToggleRow("One-time reveal", uiState.revealFlags.oneTimeReveal, viewModel::setOneTimeReveal)
                    ToggleRow("Biometric before reveal", uiState.revealFlags.requireBiometric, viewModel::setRequireBiometric)
                    ToggleRow("Delete encoded file after reveal", uiState.revealFlags.deleteEncodedAfterReveal, viewModel::setDeleteEncodedAfterReveal)
                    ToggleRow("Compression", uiState.compressionEnabled, viewModel::setCompressionEnabled)
                    ToggleRow("Enable expiry", uiState.expiryEnabled, viewModel::setExpiryEnabled)
                    if (uiState.expiryEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "1 hour" to 60 * 60 * 1000L,
                                "24 hours" to 24 * 60 * 60 * 1000L,
                                "7 days" to 7 * 24 * 60 * 60 * 1000L
                            ).forEach { (label, duration) ->
                                FilterChip(
                                    selected = uiState.expiryPresetLabel == label,
                                    onClick = { viewModel.setExpiryPreset(label, duration) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    ToggleRow("Self-destruct timer", uiState.selfDestructEnabled, viewModel::setSelfDestructEnabled)
                    if (uiState.selfDestructEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(10, 30, 60, 300).forEach { seconds ->
                                FilterChip(
                                    selected = uiState.selfDestructSeconds == seconds,
                                    onClick = { viewModel.setSelfDestructSeconds(seconds) },
                                    label = { Text(if (seconds < 60) "$seconds sec" else "${seconds / 60} min") }
                                )
                            }
                        }
                    }
                }

                uiState.capacityInfo?.let { capacityInfo ->
                    CapacityMeter(capacityInfo = capacityInfo)
                    if (!capacityInfo.fits && capacityInfo.recommendedLsbBits > capacityInfo.lsbBits) {
                        Text(
                            "Recommended: switch to ${capacityInfo.recommendedLsbBits}-bit embedding for this payload.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningYellow
                        )
                    }
                }

                uiState.errorMessage?.let { ErrorCard(it) }

                GradientButton(
                    text = "Encode to PNG",
                    onClick = viewModel::encode,
                    enabled = !uiState.isEncoding && !uiState.isLoading,
                    isLoading = uiState.isEncoding || uiState.isLoading
                )

                InfoCard(
                    text = "Secrets are encrypted first, then hidden in a PNG. Social apps that recompress images can destroy hidden data."
                )
                InfoCard(
                    text = "One-time reveal is enforced only on this device. It cannot destroy copies made elsewhere."
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
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
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed)
            Spacer(modifier = Modifier.padding(4.dp))
            Text(message, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WarningYellow.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = WarningYellow)
            Spacer(modifier = Modifier.padding(4.dp))
            Text(text, color = WarningYellow, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SuccessSection(
    result: EncodingResult,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onNewEncoding: () -> Unit,
    isSaving: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (result is EncodingResult.Saved) "Encoded PNG saved." else "Secret hidden successfully.",
                color = SuccessGreen,
                style = MaterialTheme.typography.titleMedium
            )
            GradientButton(text = "Save PNG", onClick = onSave, enabled = !isSaving, isLoading = isSaving)
            TextButton(onClick = onShare) { Text("Share PNG", color = Purple60) }
            TextButton(onClick = onNewEncoding) { Text("Create another", color = TextSecondary) }
        }
    }
}
