package com.ghostgramlabs.ghostmask.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.ghostmask.R
import com.ghostgramlabs.ghostmask.ui.theme.NeonBlue
import com.ghostgramlabs.ghostmask.ui.theme.NeonCyan
import com.ghostgramlabs.ghostmask.ui.theme.NeonPink
import com.ghostgramlabs.ghostmask.ui.theme.TextPrimary

@Composable
fun GhostMaskBrandIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        NeonPink.copy(alpha = 0.24f),
                        NeonBlue.copy(alpha = 0.18f),
                        NeonCyan.copy(alpha = 0.08f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "GhostMask icon",
            modifier = Modifier.size(72.dp)
        )
    }
}

@Composable
fun GhostMaskTitle(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GhostMaskBrandIcon(modifier = Modifier.size(34.dp))
        Text(text = text, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
    }
}
