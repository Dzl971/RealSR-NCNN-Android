package com.tumuyan.ncnn.realsr.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tumuyan.ncnn.realsr.Screen
import com.tumuyan.ncnn.realsr.ShinobiViewModel
import com.tumuyan.ncnn.realsr.UiState

@Composable
fun ScreenFrame(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NinjaOutlineButton("Retour", onBack, color = Shinobi.Orange, leading = { SpiralIcon(20.dp, Shinobi.Orange) })
            Box(Modifier.width(14.dp))
            Text(title.uppercase(), color = Shinobi.Orange, style = MaterialTheme.typography.headlineMedium)
        }
        content()
    }
}

@Composable
fun SettingsScreen(s: UiState, vm: ShinobiViewModel, onBrowse: () -> Unit) {
    val context = LocalContext.current
    var path by remember(s.extraPath) { mutableStateOf(s.extraPath) }
    val readPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun applyPath() {
        if (Build.VERSION.SDK_INT >= 23 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) readPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        vm.setExtraPath(path)
        vm.toast(if (path.isBlank()) "Dossier perso retiré." else "Dossier appliqué : ${vm.state.value.techniques.size} techniques.")
    }

    ScreenFrame("Réglages", { vm.go(Screen.MAIN) }) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Source de chakra")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeCard("GPU", "Rapide (Vulkan). Peut planter sur certains Snapdragon récents.", !s.useCpu, Shinobi.Chakra, Modifier.weight(1f)) { vm.setUseCpu(false) }
                ModeCard("CPU", "Lent mais stable. À choisir si le GPU fait fermer l'app.", s.useCpu, Shinobi.Red, Modifier.weight(1f)) { vm.setUseCpu(true) }
            }
            Hint("Le mode CPU ajoute l'option « -g -1 » à la commande du moteur.")

            SectionTitle("Taille des tuiles")
            val tiles = listOf(0 to "Auto", 100 to "100", 200 to "200", 400 to "400")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((v, name) in tiles) {
                    val sel = s.tile == v
                    Box(
                        Modifier.weight(1f).height(44.dp)
                            .background(if (sel) Shinobi.Orange else Shinobi.Panel, CutShapeSmall)
                            .border(1.5.dp, if (sel) Shinobi.OrangeSoft else Shinobi.PanelHigh, CutShapeSmall)
                            .clickable { vm.setTile(v) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name, color = if (sel) Color(0xFF1A0B00) else Shinobi.Parchment, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            Hint("Des tuiles plus petites utilisent moins de mémoire (utile si le jutsu échoue sur une grande image).")

            SectionTitle("Modèles personnalisés")
            OutlinedTextField(
                value = path, onValueChange = { path = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Dossier des modèles (ex. /storage/emulated/0/Models)") },
                singleLine = true, shape = CutShapeSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Shinobi.Orange, unfocusedBorderColor = Shinobi.PanelHigh,
                    focusedLabelColor = Shinobi.Orange, cursorColor = Shinobi.Orange,
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NinjaOutlineButton("Parcourir", onBrowse, color = Shinobi.Chakra)
                NinjaOutlineButton("Appliquer", { applyPath() }, color = Shinobi.Orange)
                if (s.extraPath.isNotEmpty()) NinjaOutlineButton("Retirer", { path = ""; applyPath() }, color = Shinobi.Red)
            }
            Hint(
                "Chaque sous-dossier « models-… » contenant des fichiers .param/.bin (ncnn) ou des .mnn est ajouté " +
                    "à la liste des techniques. Les modèles : https://huggingface.co/tumuyan2/realsr-models",
            )
            Hint("Techniques disponibles actuellement : ${s.techniques.size}", color = Shinobi.Chakra)
        }
    }
}

@Composable
private fun ModeCard(title: String, text: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Panel(
        modifier.clickable(onClick = onClick),
        border = if (selected) color else Shinobi.PanelHigh,
        borderWidth = if (selected) 2.5.dp else 1.dp,
        glow = if (selected) color else null,
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShurikenIcon(24.dp, if (selected) color else Shinobi.PanelHigh)
                Box(Modifier.width(8.dp))
                Text(title, color = if (selected) color else Shinobi.Parchment, style = MaterialTheme.typography.headlineSmall)
            }
            Hint(text, Modifier.padding(top = 6.dp))
        }
    }
}
