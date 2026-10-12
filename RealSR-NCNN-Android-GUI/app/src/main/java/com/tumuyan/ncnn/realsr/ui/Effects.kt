package com.tumuyan.ncnn.realsr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Temps en secondes, rafraîchi à chaque frame tant que [active] est vrai. */
@Composable
private fun rememberClock(active: Boolean): Float {
    val t by produceState(0f, active) {
        if (!active) return@produceState
        val start = withFrameNanosCompat()
        while (true) {
            val now = withFrameNanosCompat()
            value = (now - start) / 1_000_000_000f
        }
    }
    return t
}

private suspend fun withFrameNanosCompat(): Long =
    androidx.compose.runtime.withFrameNanos { it }

private fun hash(i: Int, salt: Int): Float {
    var x = i * 374761393 + salt * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFF) / 65535f
}

/**
 * Aura de chakra : lueur pulsante, spirale + shuriken en rotation et particules
 * (braises orange / étincelles bleues) qui montent. [progress] < 0 = indéterminé.
 */
@Composable
fun ChakraAura(modifier: Modifier = Modifier, active: Boolean = true, progress: Float = -1f) {
    val t = rememberClock(active)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h * 0.5f)
        val base = min(w, h) * 0.5f
        val pulse = 0.85f + 0.15f * sin(t * 3.2f)
        val fill = if (progress < 0f) 0.55f else progress

        // Lueurs : orange au centre, bleu chakra sur l'extérieur
        drawCircle(
            Brush.radialGradient(
                listOf(Shinobi.Chakra.copy(alpha = 0.30f * pulse), Color.Transparent),
                center = c, radius = base * 1.55f,
            ),
            radius = base * 1.55f, center = c,
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Shinobi.Orange.copy(alpha = (0.35f + 0.4f * fill) * pulse), Color.Transparent),
                center = c, radius = base * 1.0f,
            ),
            radius = base * 1.0f, center = c,
        )

        // Spirale d'énergie + shuriken
        drawSpiral(c, base * 0.78f, Shinobi.Chakra.copy(alpha = 0.85f), base * 0.045f, turns = 3.2f, rotationDeg = -t * 70f)
        drawSpiral(c, base * 0.60f, Shinobi.Orange.copy(alpha = 0.75f), base * 0.04f, turns = 2.4f, rotationDeg = t * 110f)
        drawShuriken(c, base * 0.34f, Shinobi.Orange, t * 360f, Shinobi.Night)
        drawCircle(Shinobi.Chakra.copy(alpha = 0.9f * pulse), base * 0.05f, c)

        // Particules
        val n = 70
        for (i in 0 until n) {
            val speed = 0.10f + hash(i, 1) * 0.22f
            val life = ((t * speed + hash(i, 2)) % 1f)
            val x0 = hash(i, 3)
            val sway = sin(t * (1f + hash(i, 4) * 2f) + hash(i, 5) * 6.28f) * 0.04f
            val x = (x0 + sway) * w
            val y = h * (1.05f - life * 1.15f)
            val r = (1.2f + hash(i, 6) * 3.2f) * (1f - life * 0.6f) * (w / 360f).coerceIn(0.8f, 2f)
            val col = if (i % 3 == 0) Shinobi.Chakra else if (i % 3 == 1) Shinobi.Orange else Shinobi.OrangeSoft
            val a = (1f - life) * min(1f, life * 8f)
            drawCircle(col.copy(alpha = 0.25f * a), r * 3.2f, Offset(x, y))
            drawCircle(col.copy(alpha = 0.95f * a), r, Offset(x, y))
        }

        // Éclairs de chakra autour du centre (arcs rapides)
        val bolts = 5
        for (b in 0 until bolts) {
            val phase = (t * 1.7f + hash(b, 9)) % 1f
            if (phase > 0.35f) continue
            val a0 = hash(b, 10) * 2f * PI.toFloat() + t * 0.6f
            val path = Path()
            var r = base * 0.45f
            var a = a0
            path.moveTo(c.x + kotlin.math.cos(a) * r, c.y + sin(a) * r)
            for (k in 1..6) {
                r += base * 0.11f
                a += (hash(b * 7 + k, (t * 12).toInt()) - 0.5f) * 0.45f
                path.lineTo(c.x + kotlin.math.cos(a) * r, c.y + sin(a) * r)
            }
            drawPath(path, Shinobi.Chakra.copy(alpha = 0.9f * (1f - phase / 0.35f)), style = Stroke(2.2f))
        }
    }
}

/**
 * Barre de « Chakra » : piste sombre à coins coupés, remplissage orange → bleu chakra
 * avec lueur, étincelle en tête. [progress] < 0 = indéterminé (balayage).
 */
@Composable
fun ChakraBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 22.dp, active: Boolean = true) {
    val t = rememberClock(active)
    Canvas(modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height
        val cut = h * 0.45f
        fun shape(x0: Float, x1: Float) = Path().apply {
            moveTo(x0, 0f); lineTo(max(x0, x1 - cut), 0f); lineTo(x1, cut)
            lineTo(x1, h); lineTo(x0 + cut, h); lineTo(x0, h - cut); close()
        }
        drawPath(shape(0f, w), Shinobi.PanelHigh)
        drawPath(shape(0f, w), Shinobi.Chakra.copy(alpha = 0.35f), style = Stroke(2f))

        val grad = Brush.horizontalGradient(listOf(Shinobi.Red, Shinobi.Orange, Shinobi.Chakra), 0f, w)
        if (progress < 0f) {
            val seg = w * 0.32f
            val x = ((t * 0.9f) % 1.4f - 0.2f) * w
            val x0 = max(0f, x); val x1 = min(w, x + seg)
            if (x1 > x0) {
                drawPath(shape(x0, x1), grad)
                drawPath(shape(x0, x1), Shinobi.Orange.copy(alpha = 0.25f), style = Stroke(h * 0.7f))
            }
        } else {
            val x1 = (w * progress.coerceIn(0f, 1f))
            if (x1 > 2f) {
                // lueur
                drawPath(shape(0f, x1), Shinobi.Orange.copy(alpha = 0.22f), style = Stroke(h * 0.9f))
                drawPath(shape(0f, x1), grad)
                // reflet animé
                val shine = ((t * 0.8f) % 1.3f - 0.15f) * x1
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                        shine - h * 2f, shine + h * 2f,
                    ),
                    Offset(0f, 0f), Size(x1, h),
                )
                // étincelle en tête
                val sparkle = 0.6f + 0.4f * abs(sin(t * 9f))
                drawCircle(Shinobi.Chakra.copy(alpha = 0.35f * sparkle), h * 0.95f, Offset(x1, h / 2))
                drawCircle(Color.White.copy(alpha = 0.9f), h * 0.2f, Offset(x1, h / 2))
            }
        }
    }
}
