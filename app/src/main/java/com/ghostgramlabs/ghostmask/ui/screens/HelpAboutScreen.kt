package com.ghostgramlabs.ghostmask.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ghostgramlabs.ghostmask.ui.components.GhostMaskTitle
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurface
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurfaceElevated
import com.ghostgramlabs.ghostmask.ui.theme.TextPrimary
import com.ghostgramlabs.ghostmask.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpAboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = { GhostMaskTitle("Help") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HelpCard("Offline only", "GhostMask never needs cloud, accounts, analytics, or ads.")
            HelpCard("Best sharing practice", "Always send encoded secrets as PNG. Apps that recompress images can destroy hidden data.")
            HelpCard("One-time reveal", "One-time reveal is enforced only on this device. Offline apps cannot destroy copies that already exist elsewhere.")
            HelpCard("Reveal safety", "Secure view, screenshot blocking, background clearing, and biometric-before-reveal are enforced from encrypted metadata.")
        }
    }
}

@Composable
private fun HelpCard(title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(body, color = TextSecondary)
        }
    }
}
