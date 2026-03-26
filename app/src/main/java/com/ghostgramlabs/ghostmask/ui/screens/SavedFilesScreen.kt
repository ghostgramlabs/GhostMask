package com.ghostgramlabs.ghostmask.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ghostgramlabs.ghostmask.data.reveal.RecentEncodedFilesStore
import com.ghostgramlabs.ghostmask.ui.components.GhostMaskTitle
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurface
import com.ghostgramlabs.ghostmask.ui.theme.DarkSurfaceElevated
import com.ghostgramlabs.ghostmask.ui.theme.TextPrimary
import com.ghostgramlabs.ghostmask.ui.theme.TextSecondary
import com.ghostgramlabs.ghostmask.util.FileSaveManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedFilesScreen(
    recentStore: RecentEncodedFilesStore,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val recents by recentStore.recentFiles.collectAsState(initial = emptyList())
    val privateFiles = FileSaveManager.listPrivateEncodedFiles(context)

    Scaffold(
        containerColor = DarkSurface,
        topBar = {
            TopAppBar(
                title = { GhostMaskTitle("Saved Files") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Private encoded PNGs", color = TextSecondary, style = MaterialTheme.typography.titleMedium) }
            if (privateFiles.isEmpty()) {
                item { EmptyCard("No private encoded PNGs saved yet.") }
            } else {
                items(privateFiles) { file ->
                    SavedItemCard(
                        title = file.file.name,
                        subtitle = "App-private storage",
                        onClick = { ContextCompat.startActivity(context, FileSaveManager.createOpenFileIntent(file.uri), null) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.padding(top = 12.dp))
                Text("Recent encoded images", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
            }
            if (recents.isEmpty()) {
                item { EmptyCard("Recent encoded files are not being remembered.") }
            } else {
                items(recents) { recent ->
                    SavedItemCard(
                        title = recent.uri,
                        subtitle = "Previously selected encoded image",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse(recent.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            ContextCompat.startActivity(context, intent, null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedItemCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextPrimary)
            Column {
                Text(title, color = TextPrimary)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)) {
        Text(message, modifier = Modifier.padding(16.dp), color = TextSecondary)
    }
}
