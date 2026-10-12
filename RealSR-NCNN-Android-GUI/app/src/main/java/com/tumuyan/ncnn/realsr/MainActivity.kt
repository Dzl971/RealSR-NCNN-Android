package com.tumuyan.ncnn.realsr

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.tumuyan.ncnn.realsr.ui.AboutScreen
import com.tumuyan.ncnn.realsr.ui.MainScreen
import com.tumuyan.ncnn.realsr.ui.SettingsScreen
import com.tumuyan.ncnn.realsr.ui.Shinobi
import com.tumuyan.ncnn.realsr.ui.ShinobiTheme

class MainActivity : ComponentActivity() {

    private val vm: ShinobiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { ShinobiTheme { Root() } }
        if (savedInstanceState == null) handleShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
    }

    /** Réception d'images via « Partager » (une ou plusieurs : seule la première est traitée). */
    private fun handleShare(intent: Intent?) {
        intent ?: return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_SEND -> uriExtra(intent)
            Intent.ACTION_SEND_MULTIPLE -> uriListExtra(intent)?.firstOrNull()
            else -> null
        }
        if (uri != null) vm.importImage(uri)
    }

    @Suppress("DEPRECATION")
    private fun uriExtra(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else intent.getParcelableExtra(Intent.EXTRA_STREAM)

    @Suppress("DEPRECATION")
    private fun uriListExtra(intent: Intent): List<Uri>? =
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)

    @Composable
    private fun Root() {
        val state by vm.state.collectAsState()
        val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) vm.importImage(uri)
        }
        val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) vm.setExtraPathFromTree(uri)
        }
        BackHandler(enabled = state.screen != Screen.MAIN) { vm.go(Screen.MAIN) }

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Shinobi.Night, Shinobi.NightBlue, Shinobi.Night)),
            ),
        ) {
            when (state.screen) {
                Screen.MAIN -> MainScreen(state, vm) {
                    pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                Screen.SETTINGS -> SettingsScreen(state, vm) { pickFolder.launch(null) }
                Screen.ABOUT -> AboutScreen(vm)
            }
        }
    }
}
