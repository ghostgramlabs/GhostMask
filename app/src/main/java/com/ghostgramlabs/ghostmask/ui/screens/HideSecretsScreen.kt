package com.ghostgramlabs.ghostmask.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.ghostmask.ui.components.*
import com.ghostgramlabs.ghostmask.ui.theme.*
import com.ghostgramlabs.ghostmask.viewmodel.EncodingResult
import com.ghostgramlabs.ghostmask.viewmodel.HideSecretsViewModel

/**
 * Screen for hiding secrets inside a cover image.
 * Supports text-only, image-only, or both text + image payloads.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideSecretsScreen(
    viewModel: HideSecretsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Photo picker for cover image
    val coverImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.setCoverImage(it) }
    }

    // Photo picker for secret image
    val secretImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSecretImage(it) }
    }

    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = { Text("Hide Secrets") },
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encoding success state
            val result = uiState.encodingResult
            if (result != null) {
                SuccessSection(
                    result = result,
                    onSave = { viewModel.saveToGallery() },
                    onShare = {
                        viewModel.share { intent ->
                            context.startActivity(intent)
                        }
                    },
                    onNewEncoding = { viewModel.reset() },
                    isSaving = uiState.isSaving
                )
            } else {
                // --- Input Section ---

                // 1. Cover image
                Text("Cover Image", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                ImagePickerCard(
                    imageUri = uiState.coverImageUri,
                    label = "Cover Image",
                    icon = Icons.Default.Image,
                    onClick = {
                        coverImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                if (uiState.coverImageUri != null) {
                    Text(
                        "${uiState.coverWidth} × ${uiState.coverHeight} px",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                HorizontalDivider(color = DarkSurfaceElevated)

                // 2. Secret text
                Text("Secret Text (optional)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                OutlinedTextField(
                    value = uiState.secretText,
                    onValueChange = { viewModel.setSecretText(it) },
                    placeholder = { Text("Enter secret message…") },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple60,
                        unfocusedBorderColor = TextMuted,
                        cursorColor = Purple60,
                        focusedLabelColor = Purple60
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = DarkSurfaceElevated)

                // 3. Secret image
                Text("Secret Image (optional)", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                ImagePickerCard(
                    imageUri = uiState.secretImageUri,
                    label = "Secret Image",
                    icon = Icons.Default.HideImage,
                    onClick = {
                        secretImagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onClear = if (uiState.secretImageUri != null) {
                        { viewModel.clearSecretImage() }
                    } else null
                )

                HorizontalDivider(color = DarkSurfaceElevated)

                // 4. Password
                PasswordField(
                    value = uiState.password,
                    onValueChange = { viewModel.setPassword(it) }
                )

                // 5. Capacity meter
                uiState.capacityInfo?.let { capacity ->
                    CapacityMeter(capacityInfo = capacity)
                }

                // Error message
                uiState.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = ErrorRed.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(error, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                        }
                    }
                }

                // 6. Encode button
                val canEncode = uiState.coverImageUri != null &&
                        (uiState.secretText.isNotEmpty() || uiState.secretImageUri != null) &&
                        uiState.password.isNotEmpty() &&
                        (uiState.capacityInfo?.fits != false)

                GradientButton(
                    text = "Encode & Hide",
                    onClick = { viewModel.encode() },
                    enabled = canEncode && !uiState.isEncoding,
                    isLoading = uiState.isEncoding || uiState.isLoading
                )

                // Steganography notice
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = WarningYellow.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(
                            Icons.Default.Info, null,
                            tint = WarningYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Apps that compress images (social media, messaging) may destroy hidden data. " +
                                    "Save locally or share through channels that preserve exact file bytes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningYellow.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
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
        colors = CardDefaults.cardColors(
            containerColor = SuccessGreen.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle, null,
                tint = SuccessGreen,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                when (result) {
                    is EncodingResult.Success -> "Secrets hidden successfully!"
                    is EncodingResult.Saved -> "Saved to gallery!"
                },
                style = MaterialTheme.typography.titleMedium,
                color = SuccessGreen
            )
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onNewEncoding) {
                Text("Hide more secrets", color = Purple60)
            }
        }
    }
}
