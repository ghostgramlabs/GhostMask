package com.ghostgramlabs.ghostmask.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ghostgramlabs.ghostmask.stego.CapacityInfo
import com.ghostgramlabs.ghostmask.ui.theme.*

/**
 * Password text field with visibility toggle.
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Password"
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Purple60,
            unfocusedBorderColor = TextMuted,
            cursorColor = Purple60,
            focusedLabelColor = Purple60
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Image picker card with dashed border and preview support.
 */
@Composable
fun ImagePickerCard(
    imageUri: Any?,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                // Overlay gradient for readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )
                // Label at bottom
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                // Clear button
                if (onClear != null) {
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Text("Clear", color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                // Empty state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    Icon(
                        icon, contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        "Tap to select",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

/**
 * Capacity meter showing payload fit status.
 * Colors: green (fits easily), yellow (tight), red (doesn't fit).
 */
@Composable
fun CapacityMeter(
    capacityInfo: CapacityInfo,
    modifier: Modifier = Modifier
) {
    val color = when {
        !capacityInfo.fits -> ErrorRed
        capacityInfo.usageRatio > 0.8f -> WarningYellow
        else -> SuccessGreen
    }

    val animatedProgress by animateFloatAsState(
        targetValue = capacityInfo.usageRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "capacity_progress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Capacity", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Text(
                if (capacityInfo.fits)
                    "${formatBytes(capacityInfo.usedBytes)} / ${formatBytes(capacityInfo.totalBytes)}"
                else
                    "Need ${formatBytes(capacityInfo.usedBytes - capacityInfo.totalBytes)} more",
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = DarkSurfaceElevated
        )
        if (!capacityInfo.fits) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Cover image is too small for this payload. Use a larger image or reduce secret content.",
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed
            )
        }
    }
}

/**
 * Gradient-filled primary action button.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val gradient = Brush.horizontalGradient(
        colors = if (enabled) listOf(Purple40, Purple60) else listOf(TextMuted, TextMuted)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(gradient)
            .then(
                if (enabled && !isLoading) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) Color.White else TextSecondary
            )
        }
    }
}

/**
 * Formats bytes into human-readable string (e.g., "1.5 KB", "3.2 MB").
 */
fun formatBytes(bytes: Int): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
        else -> "%.1f MB".format(bytes / (1024f * 1024f))
    }
}
