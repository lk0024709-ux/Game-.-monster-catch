package com.example.rann.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.example.rann.model.CreatureType
import com.example.rann.render.drawShowcaseChimera
import com.example.rann.render.drawShowcaseCreature
import com.example.rann.render.drawShowcaseWarrior

/**
 * A small self-driving canvas that reuses the in-battle procedural art for menus.
 * The frame clock stays inside this composable so menus never recompose at 60fps.
 */
@Composable
fun CreatureShowcase(
    modifier: Modifier = Modifier,
    creature: CreatureType? = null,
    chimeraIndex: Int? = null,
    showWarrior: Boolean = true,
    warriorScale: Float = 1f,
) {
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            time = (now - start) / 1_000_000_000f
        }
    }
    Canvas(modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.58f
        val unit = minOf(size.width, size.height)
        if (chimeraIndex != null) {
            drawShowcaseChimera(chimeraIndex, cx, cy, unit * 0.22f, time)
        } else if (creature != null) {
            drawShowcaseCreature(creature, cx, cy, unit * 0.22f, time)
        }
        if (showWarrior) {
            drawShowcaseWarrior(
                x = if (creature == null && chimeraIndex == null) cx else cx - unit * 0.34f,
                y = if (creature == null && chimeraIndex == null) cy else cy + unit * 0.06f,
                radius = unit * 0.13f * warriorScale,
                time = time,
            )
        }
    }
}

/** Static, tiny sword-and-shield crest for headers. */
@Composable
fun Crest(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        drawCircle(
            color = androidx.compose.ui.graphics.Color(0x22D97706),
            radius = minOf(w, h) * 0.48f,
            center = c,
        )
        drawCircle(
            color = androidx.compose.ui.graphics.Color(0x55D97706),
            radius = minOf(w, h) * 0.48f,
            center = c,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
        )
        // crossed swords
        for (dir in listOf(-1f, 1f)) {
            val x0 = c.x - dir * w * 0.22f
            val y0 = c.y + h * 0.26f
            val x1 = c.x + dir * w * 0.22f
            val y1 = c.y - h * 0.26f
            drawLine(
                color = androidx.compose.ui.graphics.Color(0xFFEDE7DB),
                start = Offset(x0, y0),
                end = Offset(x1, y1),
                strokeWidth = 7f,
            )
            drawLine(
                color = androidx.compose.ui.graphics.Color(0xFF7C2D12),
                start = Offset(x0 * 0.98f + c.x * 0.02f, y0 * 0.98f + c.y * 0.02f),
                end = Offset(x0 * 0.7f + c.x * 0.3f, y0 * 0.7f + c.y * 0.3f),
                strokeWidth = 10f,
            )
        }
    }
}
