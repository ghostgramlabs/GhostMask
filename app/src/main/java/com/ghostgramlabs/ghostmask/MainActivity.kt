package com.ghostgramlabs.ghostmask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.ghostgramlabs.ghostmask.ui.navigation.GhostMaskNavGraph
import com.ghostgramlabs.ghostmask.ui.theme.GhostMaskTheme

/**
 * Single-activity entry point for GhostMask.
 * Uses Jetpack Compose with edge-to-edge display.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GhostMaskTheme {
                val navController = rememberNavController()
                GhostMaskNavGraph(navController = navController)
            }
        }
    }
}
