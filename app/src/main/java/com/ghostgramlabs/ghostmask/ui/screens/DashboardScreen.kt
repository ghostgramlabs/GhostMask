package com.ghostgramlabs.ghostmask.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.ghostmask.ui.components.GhostMaskBrandIcon
import com.ghostgramlabs.ghostmask.ui.components.PillLabel
import com.ghostgramlabs.ghostmask.ui.theme.DarkCard
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurface
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurfaceVariant
import com.ghostgramlabs.ghostmask.ui.theme.NeonBlue
import com.ghostgramlabs.ghostmask.ui.theme.NeonMint
import com.ghostgramlabs.ghostmask.ui.theme.NeonPink
import com.ghostgramlabs.ghostmask.ui.theme.NeonViolet
import com.ghostgramlabs.ghostmask.ui.theme.TextMuted
import com.ghostgramlabs.ghostmask.ui.theme.TextPrimary
import com.ghostgramlabs.ghostmask.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onHideSecrets: () -> Unit,
    onRevealSecrets: () -> Unit,
    onSavedFiles: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit
) {
    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkSurface, DarkSurfaceVariant, DarkSurface)
                    )
                )
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonBlue.copy(alpha = 0.18f),
                                NeonPink.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PillLabel("Offline Privacy Tool")
                    Spacer(Modifier.height(12.dp))
                    GhostMaskBrandIcon(modifier = Modifier.padding(8.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "GhostMask",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Hide text and images inside ordinary PNGs with sender-controlled reveal rules.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            DashboardActionCard(
                title = "Hide Secrets",
                subtitle = "Prepare a cover image, choose protections, and export a clean PNG.",
                icon = Icons.Default.Lock,
                gradientColors = listOf(NeonPink, NeonBlue),
                onClick = onHideSecrets
            )

            Spacer(Modifier.height(20.dp))

            DashboardActionCard(
                title = "Reveal Secrets",
                subtitle = "Open an encoded PNG and apply its embedded privacy rules automatically.",
                icon = Icons.Default.LockOpen,
                gradientColors = listOf(NeonMint, NeonViolet),
                onClick = onRevealSecrets
            )

            Spacer(Modifier.height(20.dp))

            DashboardActionCard(
                title = "Saved Files",
                subtitle = "Review private outputs and recent encoded-image references.",
                icon = Icons.Default.Lock,
                gradientColors = listOf(NeonBlue, NeonMint),
                onClick = onSavedFiles
            )

            Spacer(Modifier.height(20.dp))

            DashboardActionCard(
                title = "Settings",
                subtitle = "Control app lock, biometrics, storage defaults, and cleanup behavior.",
                icon = Icons.Default.Lock,
                gradientColors = listOf(NeonViolet, NeonPink),
                onClick = onSettings
            )

            Spacer(Modifier.height(20.dp))

            DashboardActionCard(
                title = "Help",
                subtitle = "Read safe-sharing guidance and privacy notes before sending files.",
                icon = Icons.Default.LockOpen,
                gradientColors = listOf(NeonMint, NeonBlue),
                onClick = onHelp
            )

            Spacer(Modifier.height(18.dp))
            Text(
                text = "GhostMask stays offline. Save encoded secrets as PNGs and avoid apps that recompress images.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = gradientColors.last().copy(alpha = 0.22f), spotColor = gradientColors.first().copy(alpha = 0.18f))
            .scale(scale)
            .clickable {
                pressed = true
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, gradientColors.last().copy(alpha = 0.38f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            gradientColors.first().copy(alpha = 0.95f),
                            gradientColors.last().copy(alpha = 0.65f),
                            DarkCard
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            gradientColors.last().copy(alpha = 0.42f),
                            gradientColors.first().copy(alpha = 0.18f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.84f)
                    )
                }
            }
        }
    }
}
