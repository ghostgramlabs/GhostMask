package com.ghostgramlabs.ghostmask.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.ghostgramlabs.ghostmask.ui.theme.DarkCard
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurfaceElevated
import com.ghostgramlabs.ghostmask.ui.theme.ErrorRed
import com.ghostgramlabs.ghostmask.ui.theme.NeonBlue
import com.ghostgramlabs.ghostmask.ui.theme.NeonCyan
import com.ghostgramlabs.ghostmask.ui.theme.NeonMint
import com.ghostgramlabs.ghostmask.ui.theme.NeonPink
import com.ghostgramlabs.ghostmask.ui.theme.Purple40
import com.ghostgramlabs.ghostmask.ui.theme.Purple60
import com.ghostgramlabs.ghostmask.ui.theme.SuccessGreen
import com.ghostgramlabs.ghostmask.ui.theme.TextMuted
import com.ghostgramlabs.ghostmask.ui.theme.TextPrimary
import com.ghostgramlabs.ghostmask.ui.theme.TextSecondary
import com.ghostgramlabs.ghostmask.ui.theme.WarningYellow

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
            focusedLabelColor = Purple60,
            focusedContainerColor = DarkCard.copy(alpha = 0.92f),
            unfocusedContainerColor = DarkCard.copy(alpha = 0.72f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(14.dp), ambientColor = NeonBlue.copy(alpha = 0.18f), spotColor = NeonPink.copy(alpha = 0.12f))
    )
}

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
            .shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = NeonBlue.copy(alpha = 0.14f), spotColor = NeonCyan.copy(alpha = 0.12f))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            DarkCard.copy(alpha = 0.96f),
                            DarkSurfaceElevated.copy(alpha = 0.92f),
                            NeonBlue.copy(alpha = 0.18f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.7f),
                            NeonPink.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
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
                        .clip(RoundedCornerShape(18.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    NeonBlue.copy(alpha = 0.12f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    NeonPink.copy(alpha = 0.1f),
                                    NeonCyan.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = NeonBlue.copy(alpha = 0.24f), spotColor = NeonPink.copy(alpha = 0.18f))
                            .clip(RoundedCornerShape(18.dp))
                            .background(DarkSurfaceElevated.copy(alpha = 0.84f))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = NeonMint,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("Tap to select", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }
    }
}

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard.copy(alpha = 0.72f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
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
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = color,
            trackColor = DarkSurfaceElevated
        )
        if (!capacityInfo.fits) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Cover image is too small for this payload. Use a larger image or reduce secret content.",
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed
            )
        }
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val gradient = if (enabled) {
        Brush.linearGradient(
            colors = listOf(
                NeonPink.copy(alpha = 0.95f),
                Purple40.copy(alpha = 0.92f),
                NeonBlue.copy(alpha = 0.95f)
            )
        )
    } else {
        Brush.horizontalGradient(colors = listOf(TextMuted, TextMuted))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(18.dp), ambientColor = NeonBlue.copy(alpha = 0.28f), spotColor = NeonPink.copy(alpha = 0.22f))
            .clip(RoundedCornerShape(18.dp))
            .background(gradient)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (enabled) 0.48f else 0.12f),
                        NeonCyan.copy(alpha = if (enabled) 0.44f else 0.12f),
                        NeonPink.copy(alpha = if (enabled) 0.3f else 0.12f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .then(if (enabled && !isLoading) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 15.dp),
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

fun formatBytes(bytes: Int): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
        else -> "%.1f MB".format(bytes / (1024f * 1024f))
    }
}
