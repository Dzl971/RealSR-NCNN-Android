package com.tumuyan.ncnn.realsr.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.tumuyan.ncnn.realsr.Category
import com.tumuyan.ncnn.realsr.Phase
import com.tumuyan.ncnn.realsr.Screen
import com.tumuyan.ncnn.realsr.ShinobiViewModel
import com.tumuyan.ncnn.realsr.Technique
import com.tumuyan.ncnn.realsr.UiState
import java.io.File

@Composable
fun MainScreen(s: UiState, vm: ShinobiViewModel, onPick: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(s.message) {
        s.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }
    val savePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.saveToGallery() else vm.toast("Permission refusée : impossible d'écrire dans la galerie.")
    }
    val onSave = {
        if (Build.VERSION.SDK_INT < 29 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) savePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else vm.saveToGallery()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { RunBar(s, vm) },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(vm) }
            item { SectionTitle("Parchemin") }
            item { ScrollCard(s, onPick) }

            if (s.phase == Phase.PREPARING || s.phase == Phase.IMPORTING || s.phase == Phase.RUNNING) {
                item { ChakraPanel(s) }
            }

            item {
                SectionTitle("Techniques") {
                    NinjaOutlineButton(
                        if (s.useCpu) "CPU" else "GPU",
                        onClick = { vm.setUseCpu(!s.useCpu) },
                        color = if (s.useCpu) Shinobi.Red else Shinobi.Chakra,
                    )
                }
            }
            item { CategoryTabs(s, vm) }
            val shown = s.visibleTechniques
            if (shown.isEmpty()) {
                item { Hint("Aucune technique dans cette catégorie.") }
            }
            items(shown, key = { it.id }) { t -> TechniqueRow(t, t.id == s.selectedId) { vm.selectTechnique(t) } }

            if (s.phase == Phase.DONE && s.afterPath != null) {
                item { SectionTitle("Résultat") }
                item {
                    BeforeAfterSlider(
                        s.beforePath, s.afterPath, s.resultVersion,
                        Modifier.fillMaxWidth().height(380.dp),
                    )
                }
                item { Hint("Glisse la poignée pour comparer · pince pour zoomer · double-tape pour recentrer") }
                if (s.animatedGifPath != null) item { GifPreview(s.animatedGifPath) }
                item {
                    NinjaButton(
                        "Enregistrer dans la galerie", onSave, Modifier.fillMaxWidth(),
                        color = Shinobi.Chakra, contentColor = Color(0xFF00243D),
                        leading = { KunaiIcon(26.dp, Color(0xFF00243D), Color(0xFF00243D)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(vm: ShinobiViewModel) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        ShurikenIcon(44.dp, Shinobi.Orange)
        Box(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("SHINOBI", color = Shinobi.Orange, style = MaterialTheme.typography.headlineLarge)
            Text("UPSCALER", color = Shinobi.Chakra, style = MaterialTheme.typography.titleLarge)
        }
        NinjaOutlineButton("Réglages", { vm.go(Screen.SETTINGS) }, color = Shinobi.Orange,
            leading = { KunaiIcon(22.dp) })
        Box(Modifier.width(8.dp))
        NinjaOutlineButton("?", { vm.go(Screen.ABOUT) }, color = Shinobi.Chakra)
    }
}

@Composable
private fun ScrollCard(s: UiState, onPick: () -> Unit) {
    val img = s.image
    Panel(Modifier.fillMaxWidth(), border = if (img == null) Shinobi.Orange.copy(alpha = 0.6f) else Shinobi.PanelHigh, glow = Shinobi.Chakra) {
        if (img == null) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SpiralIcon(56.dp, Shinobi.Chakra)
                SpacerV(8.dp)
                Text("Déroule un parchemin", style = MaterialTheme.typography.titleLarge)
                Hint("Choisis une image ou un GIF animé dans ta galerie, ou partage-le depuis une autre app.",
                    Modifier.padding(vertical = 8.dp))
                NinjaButton("Ouvrir la galerie", onPick, enabled = s.phase != Phase.RUNNING,
                    leading = { ShurikenIcon(24.dp, Color(0xFF1A0B00)) })
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val thumb by produceState<ImageBitmap?>(null, s.beforePath, s.imageVersion) { value = loadBitmap(s.beforePath, 400) }
                Box(
                    Modifier.size(84.dp).clip(CutShapeSmall).background(Shinobi.Night).border(1.5.dp, Shinobi.Orange, CutShapeSmall),
                    contentAlignment = Alignment.Center,
                ) {
                    thumb?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                }
                Box(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(img.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text("${img.width} × ${img.height} px", color = Shinobi.Muted, style = MaterialTheme.typography.bodyMedium)
                    if (img.isAnimatedGif) {
                        Text("GIF animé · ${img.frameCount} images", color = Shinobi.Chakra,
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                NinjaOutlineButton("Changer", onPick, color = Shinobi.Orange)
            }
        }
    }
}

@Composable
private fun ChakraPanel(s: UiState) {
    val running = s.phase == Phase.RUNNING
    Panel(Modifier.fillMaxWidth(), border = Shinobi.Chakra, glow = Shinobi.Orange) {
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            ChakraAura(Modifier.fillMaxSize(), active = true, progress = if (running) s.progress else -1f)
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
                Text(
                    when (s.phase) {
                        Phase.PREPARING -> "Aiguisage des kunai… (première utilisation)"
                        Phase.IMPORTING -> "Déroulement du parchemin…"
                        else -> if (s.progress < 0) "Concentration du chakra…" else "CHAKRA  ${(s.progress * 100).toInt()} %"
                    },
                    style = MaterialTheme.typography.titleLarge, color = Shinobi.Parchment,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                SpacerV(8.dp)
                ChakraBar(if (running) s.progress else -1f)
                if (running) {
                    SpacerV(6.dp)
                    Hint("Temps écoulé : ${s.elapsedSeconds} s", Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(s: UiState, vm: ShinobiViewModel) {
    val tabs = listOf(Category.ANIME to "Anime / Illustration", Category.PHOTO to "Photo", Category.CLASSIC to "Classique")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((cat, name) in tabs) {
            val sel = s.category == cat
            val count = s.techniques.count { it.category == cat }
            Box(
                Modifier.weight(if (cat == Category.ANIME) 1.5f else 1f).height(44.dp)
                    .background(if (sel) Shinobi.Orange else Shinobi.Panel, CutShapeSmall)
                    .border(1.5.dp, if (sel) Shinobi.OrangeSoft else Shinobi.PanelHigh, CutShapeSmall)
                    .clickable { vm.selectCategory(cat) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$name ($count)", maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (sel) Color(0xFF1A0B00) else Shinobi.Parchment,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

private fun programName(cmd: String): String = when {
    cmd.startsWith("./realsr-ncnn") -> "Real-ESRGAN / RealSR · ncnn"
    cmd.startsWith("./realcugan-ncnn") -> "Real-CUGAN · ncnn"
    cmd.startsWith("./waifu2x-ncnn") -> "Waifu2x · ncnn"
    cmd.startsWith("./srmd-ncnn") -> "SRMD · ncnn"
    cmd.startsWith("./mnnsr-ncnn") -> "MNN"
    cmd.startsWith("./Anime4k") -> "Anime4K"
    cmd.startsWith("./resize-ncnn") -> "Interpolation classique"
    cmd.startsWith("./magick") -> "ImageMagick"
    else -> "Moteur"
}

@Composable
private fun TechniqueRow(t: Technique, selected: Boolean, onClick: () -> Unit) {
    Panel(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        border = if (selected) Shinobi.Orange else Shinobi.PanelHigh,
        borderWidth = if (selected) 2.dp else 1.dp,
        shape = CutShapeSmall,
        glow = if (selected) Shinobi.Orange else null,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) KunaiIcon(28.dp, Shinobi.Parchment, Shinobi.Orange) else ShurikenIcon(24.dp, Shinobi.PanelHigh)
            Box(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.label, color = if (selected) Shinobi.Orange else Shinobi.Parchment,
                    style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(programName(t.command), color = Shinobi.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun GifPreview(path: String) {
    val context = LocalContext.current
    val loader = remember {
        ImageLoader.Builder(context).components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }.build()
    }
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("Animation du résultat", style = MaterialTheme.typography.titleMedium)
            SpacerV(8.dp)
            AsyncImage(
                model = File(path), imageLoader = loader, contentDescription = "Résultat animé",
                modifier = Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun RunBar(s: UiState, vm: ShinobiViewModel) {
    val busy = s.phase == Phase.RUNNING
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Shinobi.Night.copy(alpha = 0.96f), Shinobi.Night)))
            .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (busy) {
            NinjaButton("Interrompre le jutsu", { vm.cancelJutsu() }, Modifier.fillMaxWidth(),
                color = Shinobi.Red, contentColor = Color.White)
        } else {
            NinjaButton(
                "Lancer le jutsu", { vm.launchJutsu() }, Modifier.fillMaxWidth(),
                enabled = s.image != null && s.selected != null && s.phase != Phase.PREPARING && s.phase != Phase.IMPORTING,
                leading = { ShurikenIcon(28.dp, Color(0xFF1A0B00)) },
            )
        }
    }
}
