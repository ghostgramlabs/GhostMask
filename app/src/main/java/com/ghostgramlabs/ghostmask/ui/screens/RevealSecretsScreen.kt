package com.ghostgramlabs.ghostmask.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.fragment.app.FragmentActivity
import com.ghostgramlabs.ghostmask.security.BiometricAuthManager
import com.ghostgramlabs.ghostmask.security.SecureViewController
import com.ghostgramlabs.ghostmask.ui.components.GradientButton
import com.ghostgramlabs.ghostmask.ui.components.GhostMaskTitle
import com.ghostgramlabs.ghostmask.ui.components.ImagePickerCard
import com.ghostgramlabs.ghostmask.ui.components.PasswordField
import com.ghostgramlabs.ghostmask.ui.theme.DarkCard
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurface
import com.ghostgramlabs.ghostmask.ui.theme.ErrorRed
import com.ghostgramlabs.ghostmask.ui.theme.SuccessGreen
import com.ghostgramlabs.ghostmask.ui.theme.TextPrimary
import com.ghostgramlabs.ghostmask.ui.theme.TextSecondary
import com.ghostgramlabs.ghostmask.ui.theme.WarningYellow
import com.ghostgramlabs.ghostmask.viewmodel.RevealSecretsViewModel
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevealSecretsScreen(
    viewModel: RevealSecretsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let(viewModel::setEncodedImage) }

    val secureEnabled = uiState.decodingResult?.meta?.let {
        it.flags.secureView || it.flags.blockScreenshots || it.flags.hideFromRecents
    } == true

    DisposableEffect(activity, secureEnabled) {
        val controller = activity?.let(::SecureViewController)
        controller?.setSecure(secureEnabled)
        onDispose {
            controller?.setSecure(false)
            viewModel.closeRevealIfNeeded()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.pendingBiometricPrompt) {
        if (uiState.pendingBiometricPrompt && activity != null) {
            viewModel.onBiometricPromptShown()
            BiometricAuthManager(activity).authenticate(
                title = "Unlock secret",
                subtitle = "Authenticate before GhostMask reveals the hidden content",
                onSuccess = { viewModel.onBiometricResult(true) },
                onError = { viewModel.onBiometricResult(false, it) }
            )
        }
    }

    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = { GhostMaskTitle("Reveal Secret") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.closeRevealIfNeeded()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.decodingResult != null) {
                        TextButton(onClick = viewModel::panic) {
                            Text("Panic", color = ErrorRed)
                        }
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
            ImagePickerCard(
                imageUri = uiState.encodedImageUri,
                label = "Encoded image",
                icon = Icons.Default.ImageSearch,
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            PasswordField(value = uiState.password, onValueChange = viewModel::setPassword)

            uiState.errorMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        message,
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            GradientButton(
                text = "Decrypt & Reveal",
                onClick = viewModel::decode,
                enabled = uiState.encodedImageUri != null && !uiState.isDecoding,
                isLoading = uiState.isDecoding || uiState.isLoading
            )

            uiState.decodingResult?.let { result ->
                HorizontalDivider()
                PolicySummaryCard(result.meta.flags.requireBiometric, result.meta.flags.oneTimeReveal, result.meta.flags.clearOnBackground)
                uiState.countdownRemainingSeconds?.let { seconds ->
                    Card(colors = CardDefaults.cardColors(containerColor = WarningYellow.copy(alpha = 0.1f))) {
                        Text(
                            "Self-destruct in ${formatTimer(seconds)}",
                            color = WarningYellow,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (uiState.revealMasked) {
                    Card(colors = CardDefaults.cardColors(containerColor = DarkCard)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Secret hidden while the app is inactive.", color = TextSecondary)
                        }
                    }
                } else {
                    result.text?.let { text ->
                        Card(colors = CardDefaults.cardColors(containerColor = DarkCard), shape = RoundedCornerShape(14.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Hidden Text", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                                Text(text, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    if (result.hasImage) {
                        Card(colors = CardDefaults.cardColors(containerColor = DarkCard), shape = RoundedCornerShape(14.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Hidden Image", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                                viewModel.getRevealedImageBitmap()?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Revealed secret image",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                GradientButton(
                                    text = if (uiState.savedRevealedImage) "Saved" else "Export image",
                                    onClick = viewModel::saveRevealedImage,
                                    enabled = !uiState.isSaving && !uiState.savedRevealedImage,
                                    isLoading = uiState.isSaving
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (uiState.pendingDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.confirmDeleteEncodedFile(false) },
            title = { Text("Delete encoded file?") },
            text = { Text("This secret requested deletion of the encoded source image after reveal. Delete the selected file now?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteEncodedFile(true) }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmDeleteEncodedFile(false) }) {
                    Text("Keep")
                }
            }
        )
    }
}

@Composable
private fun PolicySummaryCard(requireBiometric: Boolean, oneTimeReveal: Boolean, clearsOnBackground: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen)
                Spacer(modifier = Modifier.weight(0.1f))
                Text("Reveal policies are being enforced automatically.", color = SuccessGreen)
            }
            if (requireBiometric) Text("Biometric required before reveal.", color = TextSecondary)
            if (oneTimeReveal) Text("One-time reveal is enforced locally on this device.", color = TextSecondary)
            if (clearsOnBackground) Text("Content clears immediately if the app backgrounds.", color = TextSecondary)
        }
    }
}

private fun formatTimer(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) "${minutes}:${remainingSeconds.toString().padStart(2, '0')}" else "${remainingSeconds}s"
}
