package com.tumuyan.ncnn.realsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Emblèmes dessinés à la main (formes géométriques simples, 100 % originaux) :
 * shuriken, kunai et spirale. Aucun logo, symbole ou personnage officiel.
 */

/** Shuriken à 4 branches avec un trou central. */
fun DrawScope.drawShuriken(
    center: Offset,
    radius: Float,
    color: Color,
    rotationDeg: Float = 0f,
    holeColor: Color = Color.Transparent,
) {
    rotate(rotationDeg, center) {
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            val inner = radius * 0.30f
            for (i in 0 until 8) {
                val a = (PI / 4 * i - PI / 2).toFloat()
                val r = if (i % 2 == 0) radius else inner
                val x = center.x + cos(a) * r
                val y = center.y + sin(a) * r
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(path, color)
        drawCircle(holeColor, radius * 0.16f, center)
    }
}

/** Kunai : lame en feuille, garde, manche et anneau. [length] = hauteur totale. */
fun DrawScope.drawKunai(
    center: Offset,
    length: Float,
    color: Color,
    accent: Color,
    rotationDeg: Float = 0f,
) {
    rotate(rotationDeg, center) {
        val w = length * 0.22f
        val top = center.y - length / 2
        val blade = Path().apply {
            moveTo(center.x, top)
            lineTo(center.x + w / 2, top + length * 0.30f)
            lineTo(center.x + w * 0.18f, top + length * 0.55f)
            lineTo(center.x - w * 0.18f, top + length * 0.55f)
            lineTo(center.x - w / 2, top + length * 0.30f)
            close()
        }
        drawPath(blade, color)
        drawLine(accent, Offset(center.x, top + length * 0.08f), Offset(center.x, top + length * 0.50f), strokeWidth = length * 0.025f)
        // garde
        drawRect(accent, Offset(center.x - w * 0.42f, top + length * 0.55f), Size(w * 0.84f, length * 0.05f))
        // manche
        drawRect(color, Offset(center.x - w * 0.09f, top + length * 0.60f), Size(w * 0.18f, length * 0.26f))
        // anneau
        drawCircle(accent, length * 0.075f, Offset(center.x, top + length * 0.93f), style = Stroke(length * 0.03f))
    }
}

/** Spirale d'Archimède. [sweep] de 0 à 1 pour la « dessiner » progressivement. */
fun DrawScope.drawSpiral(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float,
    turns: Float = 3f,
    sweep: Float = 1f,
    rotationDeg: Float = 0f,
) {
    rotate(rotationDeg, center) {
        val steps = 180
        val path = Path()
        val last = (steps * sweep).toInt().coerceIn(1, steps)
        for (i in 0..last) {
            val t = i / steps.toFloat()
            val a = t * turns * 2f * PI.toFloat()
            val r = radius * t
            val x = center.x + cos(a) * r
            val y = center.y + sin(a) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
fun ShurikenIcon(size: Dp = 24.dp, color: Color = Shinobi.Orange, rotation: Float = 0f, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        drawShuriken(center, this.size.minDimension / 2f, color, rotation, Shinobi.Night)
    }
}

@Composable
fun KunaiIcon(size: Dp = 24.dp, color: Color = Shinobi.Parchment, accent: Color = Shinobi.Orange, rotation: Float = 45f) {
    Canvas(Modifier.size(size)) {
        drawKunai(center, this.size.minDimension * 0.95f, color, accent, rotation)
    }
}

@Composable
fun SpiralIcon(size: Dp = 24.dp, color: Color = Shinobi.Chakra) {
    Canvas(Modifier.size(size)) {
        drawSpiral(center, this.size.minDimension / 2f * 0.92f, color, this.size.minDimension * 0.09f)
    }
}
