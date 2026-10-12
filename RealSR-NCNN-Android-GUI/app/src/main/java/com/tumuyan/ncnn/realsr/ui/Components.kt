package com.tumuyan.ncnn.realsr.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Bouton aux bords nets (coins coupés) avec liseré lumineux. */
@Composable
fun NinjaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Shinobi.Orange,
    contentColor: Color = Color(0xFF1A0B00),
    enabled: Boolean = true,
    leading: @Composable (() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = CutShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = color, contentColor = contentColor,
            disabledContainerColor = Shinobi.PanelHigh, disabledContentColor = Shinobi.Muted,
        ),
        border = BorderStroke(2.dp, if (enabled) color.copy(alpha = 0.55f) else Color.Transparent),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (leading != null) {
            leading()
            Box(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Bouton secondaire : contour fin, fond sombre. */
@Composable
fun NinjaOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Shinobi.Chakra,
    leading: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .height(44.dp)
            .background(Shinobi.Panel, CutShapeSmall)
            .border(1.5.dp, color, CutShapeSmall)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) {
            leading()
            Box(Modifier.width(8.dp))
        }
        Text(text, color = color, style = MaterialTheme.typography.titleSmall)
    }
}

/** Panneau « parchemin » : fond bleu nuit, coins coupés, liseré. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    border: Color = Shinobi.PanelHigh,
    borderWidth: Dp = 1.5.dp,
    shape: Shape = CutShape,
    glow: Color? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .then(
                if (glow != null) Modifier.drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(glow.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(size.width / 2, size.height / 2), radius = size.maxDimension * 0.75f,
                        ),
                    )
                } else Modifier
            )
            .background(Shinobi.Panel, shape)
            .border(borderWidth, border, shape),
    ) { content() }
}

/** Titre de section façon manga : barre orange + texte épais. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 6.dp, height = 26.dp).background(Shinobi.Orange))
        Box(Modifier.width(10.dp))
        Text(
            text.uppercase(), color = Shinobi.Parchment, style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier, color: Color = Shinobi.Muted) {
    Text(text, modifier, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal)
}

@Composable
fun SpacerV(h: Dp) = Box(Modifier.height(h))
