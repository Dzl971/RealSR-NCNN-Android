package com.tumuyan.ncnn.realsr.ui

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tumuyan.ncnn.realsr.R

/** Palette « nuit de shinobi » : noir / bleu nuit, orange vif, bleu chakra, rouge. */
object Shinobi {
    val Night = Color(0xFF070A14)
    val NightBlue = Color(0xFF0C1330)
    val Panel = Color(0xFF111A33)
    val PanelHigh = Color(0xFF1A2548)
    val Orange = Color(0xFFFF6A00)
    val OrangeSoft = Color(0xFFFFA45C)
    val Chakra = Color(0xFF2EA8FF)
    val Red = Color(0xFFE5262F)
    val Parchment = Color(0xFFF3E9D2)
    val Muted = Color(0xFF8C99BD)
}

/** Titres en police épaisse façon manga (Bangers, licence OFL — voir « À propos »). */
val MangaFont = FontFamily(Font(R.font.bangers, FontWeight.Normal))

/** Bords nets : coins coupés plutôt qu'arrondis. */
val CutShape = CutCornerShape(topStart = 0.dp, topEnd = 14.dp, bottomEnd = 0.dp, bottomStart = 14.dp)
val CutShapeSmall = CutCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomEnd = 0.dp, bottomStart = 8.dp)

private val scheme = darkColorScheme(
    primary = Shinobi.Orange,
    onPrimary = Color(0xFF1A0B00),
    primaryContainer = Color(0xFF5A2600),
    onPrimaryContainer = Color(0xFFFFDBC4),
    secondary = Shinobi.Chakra,
    onSecondary = Color(0xFF00243D),
    secondaryContainer = Color(0xFF0B3556),
    onSecondaryContainer = Color(0xFFCDE8FF),
    tertiary = Shinobi.Red,
    onTertiary = Color.White,
    background = Shinobi.Night,
    onBackground = Shinobi.Parchment,
    surface = Shinobi.Panel,
    onSurface = Shinobi.Parchment,
    surfaceVariant = Shinobi.PanelHigh,
    onSurfaceVariant = Shinobi.Muted,
    outline = Color(0xFF3A4A7A),
    error = Shinobi.Red,
)

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = MangaFont, fontSize = 44.sp, letterSpacing = 2.sp),
    headlineLarge = TextStyle(fontFamily = MangaFont, fontSize = 34.sp, letterSpacing = 1.5.sp),
    headlineMedium = TextStyle(fontFamily = MangaFont, fontSize = 28.sp, letterSpacing = 1.5.sp),
    headlineSmall = TextStyle(fontFamily = MangaFont, fontSize = 24.sp, letterSpacing = 1.2.sp),
    titleLarge = TextStyle(fontFamily = MangaFont, fontSize = 22.sp, letterSpacing = 1.sp),
    titleMedium = TextStyle(fontFamily = MangaFont, fontSize = 18.sp, letterSpacing = 1.sp),
    titleSmall = TextStyle(fontFamily = MangaFont, fontSize = 16.sp, letterSpacing = 0.8.sp),
    labelLarge = TextStyle(fontFamily = MangaFont, fontSize = 20.sp, letterSpacing = 1.5.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
)

@Composable
fun ShinobiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        shapes = Shapes(
            extraSmall = CutShapeSmall, small = CutShapeSmall, medium = CutShape,
            large = CutShape, extraLarge = CutShape,
        ),
        content = content,
    )
}
