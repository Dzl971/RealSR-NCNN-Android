package com.tumuyan.ncnn.realsr.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Décode une image en limitant sa taille (évite les OOM / limites de texture GPU). */
suspend fun loadBitmap(path: String?, maxDim: Int = 3072): ImageBitmap? {
    if (path == null) return null
    return withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return@withContext null
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / sample > maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
        } catch (e: Throwable) {
            null
        }
    }
}

/**
 * Comparateur avant / après : un curseur glissant (poignée shuriken) découpe l'image.
 * Pincer pour zoomer, glisser pour déplacer, double-tap pour recentrer.
 */
@Composable
fun BeforeAfterSlider(beforePath: String?, afterPath: String?, version: Int, modifier: Modifier = Modifier) {
    val before by produceState<ImageBitmap?>(null, beforePath, version) { value = loadBitmap(beforePath) }
    val after by produceState<ImageBitmap?>(null, afterPath, version) { value = loadBitmap(afterPath) }

    var box by remember { mutableStateOf(IntSize.Zero) }
    var frac by remember { mutableFloatStateOf(0.5f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(version) { scale = 1f; offset = Offset.Zero; frac = 0.5f }

    val density = LocalDensity.current
    Box(
        modifier
            .background(Shinobi.Night)
            .border(2.dp, Shinobi.Orange.copy(alpha = 0.8f))
            .onSizeChanged { box = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 10f)
                    offset += pan
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { scale = 1f; offset = Offset.Zero })
            },
    ) {
        val a = after
        val b = before
        if (a == null || b == null) {
            Text(
                "Déploiement du parchemin…",
                Modifier.align(Alignment.Center),
                color = Shinobi.Muted,
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val fit = min(w / a.width, h / a.height)
                val dw = a.width * fit * scale
                val dh = a.height * fit * scale
                val maxX = max(0f, (dw - w) / 2f)
                val maxY = max(0f, (dh - h) / 2f)
                val ox = offset.x.coerceIn(-maxX, maxX)
                val oy = offset.y.coerceIn(-maxY, maxY)
                if (ox != offset.x || oy != offset.y) offset = Offset(ox, oy)
                val left = (w - dw) / 2f + ox
                val top = (h - dh) / 2f + oy
                val sx = w * frac
                clipRect(0f, 0f, sx, h) {
                    drawImage(
                        b, dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(dw.roundToInt(), dh.roundToInt()), filterQuality = FilterQuality.Low,
                    )
                }
                clipRect(sx, 0f, w, h) {
                    drawImage(
                        a, dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(dw.roundToInt(), dh.roundToInt()), filterQuality = FilterQuality.High,
                    )
                }
                // ligne de coupe avec lueur
                drawLine(Shinobi.Orange.copy(alpha = 0.30f), Offset(sx, 0f), Offset(sx, h), 10f)
                drawLine(Shinobi.Orange, Offset(sx, 0f), Offset(sx, h), 3f)
            }
            Tag("AVANT", Modifier.align(Alignment.TopStart).padding(8.dp), Shinobi.Chakra)
            Tag("APRÈS", Modifier.align(Alignment.TopEnd).padding(8.dp), Shinobi.Orange)

            // Poignée : zone de saisie large autour de la ligne
            val handleW = 56.dp
            val handleXpx = box.width * frac
            val handleX = with(density) { handleXpx.toDp() } - handleW / 2
            Box(
                Modifier
                    .offset(x = handleX)
                    .width(handleW)
                    .fillMaxHeight()
                    .pointerInput(box.width) {
                        detectHorizontalDragGestures { change, drag ->
                            change.consume()
                            if (box.width > 0) frac = (frac + drag / box.width).coerceIn(0.02f, 0.98f)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(40.dp).background(Shinobi.Night.copy(alpha = 0.85f), CutShapeSmall)
                        .border(2.dp, Shinobi.Orange, CutShapeSmall),
                    contentAlignment = Alignment.Center,
                ) { ShurikenIcon(26.dp, Shinobi.Orange) }
            }
        }
    }
}

@Composable
private fun Tag(text: String, modifier: Modifier, color: Color) {
    Text(
        text,
        modifier.background(Shinobi.Night.copy(alpha = 0.75f), CutShapeSmall).padding(horizontal = 8.dp, vertical = 2.dp),
        color = color,
        style = MaterialTheme.typography.titleSmall,
    )
}
