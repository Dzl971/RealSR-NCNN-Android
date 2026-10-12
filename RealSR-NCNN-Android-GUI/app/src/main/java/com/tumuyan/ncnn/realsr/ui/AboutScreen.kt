package com.tumuyan.ncnn.realsr.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tumuyan.ncnn.realsr.R
import com.tumuyan.ncnn.realsr.Screen
import com.tumuyan.ncnn.realsr.ShinobiViewModel

private data class Credit(val name: String, val author: String, val license: String, val url: String, val role: String)

private val engineCredits = listOf(
    Credit("RealSR-NCNN-Android", "tumuyan", "MIT", "https://github.com/tumuyan/RealSR-NCNN-Android",
        "Moteur Android (programmes en ligne de commande) et application d'origine, dont ce projet est dérivé."),
    Credit("realsr-ncnn-vulkan · srmd-ncnn-vulkan · waifu2x-ncnn-vulkan · realcugan-ncnn-vulkan", "nihui", "MIT",
        "https://github.com/nihui/realsr-ncnn-vulkan", "Implémentations ncnn/Vulkan sur lesquelles reposent les programmes du moteur."),
    Credit("Real-ESRGAN", "xinntao (Xintao Wang et al.)", "BSD-3-Clause", "https://github.com/xinntao/Real-ESRGAN",
        "Modèles de super-résolution Real-ESRGAN (général, anime, v2, v3)."),
    Credit("RealSR", "jixiaozhong", "voir le dépôt", "https://github.com/jixiaozhong/RealSR", "Modèles RealSR (DF2K)."),
    Credit("SRMD", "cszn (Kai Zhang)", "voir le dépôt", "https://github.com/cszn/SRMD", "Modèles SRMD."),
    Credit("Real-CUGAN", "bilibili / ailab", "MIT", "https://github.com/bilibili/ailab/tree/main/Real-CUGAN", "Modèles Real-CUGAN."),
    Credit("Waifu2x", "nagadomi", "MIT", "https://github.com/nagadomi/waifu2x", "Modèles waifu2x (via waifu2x-ncnn-vulkan)."),
    Credit("Anime4K / Anime4KCPP", "bloc97 / TianZerL", "MIT", "https://github.com/TianZerL/Anime4KCPP", "Algorithmes Anime4K."),
    Credit("4xNomos8kSC", "Phhofm", "CC BY 4.0", "https://github.com/Phhofm/models/tree/main/4xNomos8kSC", "Modèle Nomos8kSC embarqué."),
    Credit("ncnn", "Tencent", "BSD-3-Clause", "https://github.com/Tencent/ncnn", "Inférence de réseaux de neurones."),
    Credit("MNN", "Alibaba", "Apache-2.0", "https://github.com/alibaba/MNN", "Inférence de réseaux de neurones (modèles .mnn)."),
    Credit("ImageMagick", "ImageMagick Studio LLC", "Licence ImageMagick", "https://github.com/ImageMagick/ImageMagick6", "Conversion, GIF et redimensionnement."),
    Credit("libwebp · stb · AVIR", "Google · Sean Barrett · Aleksey Vaneev", "BSD-3 · MIT/domaine public · MIT", "https://github.com/avaneev/avir", "Décodage / encodage d'images, redimensionnement."),
)

private val uiCredits = listOf(
    Credit("Jetpack Compose · Material 3 · AndroidX", "Google", "Apache-2.0", "https://developer.android.com/jetpack/compose", "Interface."),
    Credit("Coil", "Coil Contributors", "Apache-2.0", "https://coil-kt.github.io/coil/", "Aperçu des GIF animés."),
    Credit("Bangers (police des titres)", "The Bangers Project Authors", "SIL OFL 1.1", "https://github.com/googlefonts/bangers", "Police façon manga."),
)

@Composable
fun AboutScreen(vm: ShinobiViewModel) {
    val uri = LocalUriHandler.current
    ScreenFrame("À propos", { vm.go(Screen.MAIN) }) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Panel(Modifier.fillMaxWidth(), border = Shinobi.Orange, glow = Shinobi.Orange) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ShurikenIcon(56.dp, Shinobi.Orange)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("Shinobi Upscaler", style = MaterialTheme.typography.headlineMedium, color = Shinobi.Orange)
                        Text("Version 1.0.0 · super-résolution d'images sur appareil", color = Shinobi.Muted)
                    }
                }
            }
            Text(
                "Shinobi Upscaler est une nouvelle interface (Jetpack Compose) pour le moteur de RealSR-NCNN-Android. " +
                    "Le moteur (programmes realsr-ncnn, realcugan-ncnn, waifu2x-ncnn, srmd-ncnn, Anime4K, ImageMagick…) " +
                    "et les modèles sont ceux du projet d'origine, inchangés. Tout le traitement se fait sur ton appareil : " +
                    "aucune image n'est envoyée sur Internet.",
                style = MaterialTheme.typography.bodyLarge,
            )

            SectionTitle("Crédits du moteur et des modèles")
            engineCredits.forEach { CreditCard(it) { uri.openUri(it.url) } }

            SectionTitle("Interface")
            uiCredits.forEach { CreditCard(it) { uri.openUri(it.url) } }

            SectionTitle("Licences")
            Text(
                "Le projet d'origine est distribué sous licence MIT. Conformément à celle-ci, l'avis de copyright et " +
                    "le texte de licence sont reproduits ci-dessous. Les autres composants restent sous leurs licences respectives (voir liens).",
                style = MaterialTheme.typography.bodyMedium,
            )
            LicenseText("MIT — RealSR-NCNN-Android (tumuyan) et realsr-ncnn-vulkan (nihui)", R.raw.license_realsr_ncnn_android)
            LicenseText("SIL Open Font License 1.1 — police Bangers", R.raw.license_ofl_bangers)

            SectionTitle("Mentions")
            Text(
                "Direction artistique inspirée de l'univers des mangas de ninjas. Toutes les illustrations de l'application " +
                    "(shuriken, kunai, spirale) sont des dessins originaux en formes géométriques. Aucun logo, personnage, " +
                    "image ou élément officiel d'une œuvre existante n'est utilisé ; l'application n'est ni affiliée ni " +
                    "approuvée par les ayants droit d'une œuvre.\n\n" +
                    "Logiciel fourni « tel quel », sans garantie d'aucune sorte.",
                style = MaterialTheme.typography.bodyMedium, color = Shinobi.Muted,
            )
        }
    }
}

@Composable
private fun CreditCard(c: Credit, onOpen: () -> Unit) {
    Panel(Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = CutShapeSmall) {
        Column(Modifier.padding(12.dp)) {
            Text(c.name, style = MaterialTheme.typography.titleMedium, color = Shinobi.Orange)
            Text("${c.author} · ${c.license}", color = Shinobi.Chakra, style = MaterialTheme.typography.bodyMedium)
            Text(c.role, color = Shinobi.Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            Text(c.url, color = Shinobi.Muted, style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.Underline, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun LicenseText(title: String, resId: Int) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val text = remember(resId) {
        try { context.resources.openRawResource(resId).bufferedReader().use { it.readText() } } catch (e: Exception) { "" }
    }
    Panel(Modifier.fillMaxWidth().clickable { open = !open }, shape = CutShapeSmall) {
        Column(Modifier.padding(12.dp)) {
            Text((if (open) "▼ " else "▶ ") + title, style = MaterialTheme.typography.titleSmall, color = Shinobi.Chakra)
            if (open) Text(text, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, lineHeight = 15.sp,
                color = Shinobi.Parchment, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
