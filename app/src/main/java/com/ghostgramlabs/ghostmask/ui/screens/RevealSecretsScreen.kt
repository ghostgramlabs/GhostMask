package com.ghostgramlabs.ghostmask.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ghostgramlabs.ghostmask.ui.components.*
import com.ghostgramlabs.ghostmask.ui.theme.*
import com.ghostgramlabs.ghostmask.viewmodel.RevealSecretsViewModel

/**
 * Screen for revealing hidden secrets from an encoded image.
 * Shows decoded text and/or image with option to save revealed image.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevealSecretsScreen(
    viewModel: RevealSecretsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Photo picker for encoded image
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.setEncodedImage(it) }
    }

    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = { Text("Reveal Secrets") },
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
            // 1. Encoded image picker
            Text("Encoded Image", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            ImagePickerCard(
                imageUri = uiState.encodedImageUri,
                label = "Encoded Image",
                icon = Icons.Default.ImageSearch,
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            // 2. Password
            PasswordField(
                value = uiState.password,
                onValueChange = { viewModel.setPassword(it) }
            )

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

            // 3. Decode button
            val canDecode = uiState.encodedImageUri != null && uiState.password.isNotEmpty()
            GradientButton(
                text = "Decode & Reveal",
                onClick = { viewModel.decode() },
                enabled = canDecode && !uiState.isDecoding,
                isLoading = uiState.isDecoding || uiState.isLoading
            )

            // 4. Results area
            uiState.decodingResult?.let { result ->
                HorizontalDivider(color = DarkSurfaceElevated)

                Text("Revealed Secrets", style = MaterialTheme.typography.titleMedium, color = SuccessGreen)

                // Decoded text
                result.text?.let { text ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TextSnippet, null, tint = Purple60, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Hidden Text", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }

                // Decoded image
                if (result.hasImage) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Image, null, tint = Teal60, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Hidden Image", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            }
                            Spacer(Modifier.height(12.dp))

                            val bitmap = viewModel.getRevealedImageBitmap()
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Revealed secret image",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { viewModel.saveRevealedImage() },
                                enabled = !uiState.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else if (uiState.savedRevealedImage) {
                                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Saved!", color = SuccessGreen)
                                } else {
                                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save Image")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
