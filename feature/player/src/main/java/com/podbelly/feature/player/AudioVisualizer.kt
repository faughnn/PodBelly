package com.podbelly.feature.player

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.podbelly.core.common.VisualizerStyle
import com.podbelly.core.playback.visualizer.VisualizerFrame
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The Now Playing audio visualizer. Draws the selected [style] from the live
 * [frame] on a single canvas, in the current theme's accent colors.
 *
 * [frame] is read inside the draw phase (`drawBehind`), so new frames (~40/sec)
 * repaint only this canvas — they do not recompose the player screen.
 */
@Composable
fun AudioVisualizer(
    frame: State<VisualizerFrame>,
    style: VisualizerStyle,
    modifier: Modifier = Modifier,
) {
    val palette = VizPalette(
        primary = MaterialTheme.colorScheme.primary,
        secondary = MaterialTheme.colorScheme.secondary,
        tertiary = MaterialTheme.colorScheme.tertiary,
    )
    Spacer(
        modifier = modifier.drawBehind {
            val f = frame.value
            when (style) {
                VisualizerStyle.BARS -> drawBars(f.bands, palette)
                VisualizerStyle.MIRRORED_BARS -> drawMirroredBars(f.bands, palette)
                VisualizerStyle.SPECTRUM_CURVE -> drawSpectrumCurve(f.bands, palette)
                VisualizerStyle.RADIAL_BARS -> drawRadialBars(f.bands, palette)
                VisualizerStyle.OSCILLOSCOPE -> drawOscilloscope(f.waveform, palette)
                VisualizerStyle.FILLED_WAVE -> drawFilledWave(f.waveform, palette)
                VisualizerStyle.PULSE -> drawPulse(f.level, palette)
                VisualizerStyle.RIPPLE -> drawRipple(f.level, palette)
                VisualizerStyle.VU_LADDER -> drawVuLadder(f.level, palette)
                VisualizerStyle.DOT_MATRIX -> drawDotMatrix(f.bands, palette)
            }
        }
    )
}

private class VizPalette(val primary: Color, val secondary: Color, val tertiary: Color)

/** Small floor so silence still shows a faint resting shape rather than nothing. */
private const val FLOOR = 0.015f

// ── Spectrum styles (FFT bands) ──────────────────────────────────────────────

private fun DrawScope.drawBars(bands: FloatArray, palette: VizPalette) {
    if (bands.isEmpty()) return
    val count = bands.size
    val slot = size.width / count
    val barWidth = slot * 0.7f
    val radius = CornerRadius(barWidth / 2f)
    for (i in 0 until count) {
        val v = (bands[i]).coerceAtLeast(FLOOR)
        val h = v * size.height
        val color = lerp(palette.primary, palette.secondary, i / (count - 1f))
        drawRoundRect(
            color = color,
            topLeft = Offset(i * slot + (slot - barWidth) / 2f, size.height - h),
            size = Size(barWidth, h),
            cornerRadius = radius,
        )
    }
}

private fun DrawScope.drawMirroredBars(bands: FloatArray, palette: VizPalette) {
    if (bands.isEmpty()) return
    val count = bands.size
    val slot = size.width / count
    val barWidth = slot * 0.7f
    val mid = size.height / 2f
    val radius = CornerRadius(barWidth / 2f)
    for (i in 0 until count) {
        val v = bands[i].coerceAtLeast(FLOOR)
        val half = v * mid
        val color = lerp(palette.primary, palette.tertiary, i / (count - 1f))
        drawRoundRect(
            color = color,
            topLeft = Offset(i * slot + (slot - barWidth) / 2f, mid - half),
            size = Size(barWidth, half * 2f),
            cornerRadius = radius,
        )
    }
}

private fun DrawScope.drawSpectrumCurve(bands: FloatArray, palette: VizPalette) {
    if (bands.size < 2) return
    val count = bands.size
    val stepX = size.width / (count - 1f)
    fun x(i: Int) = i * stepX
    fun y(i: Int) = size.height - bands[i].coerceAtLeast(FLOOR) * size.height

    val line = Path().apply {
        moveTo(x(0), y(0))
        for (i in 1 until count) lineTo(x(i), y(i))
    }
    val fill = Path().apply {
        addPath(line)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(
        path = fill,
        brush = Brush.verticalGradient(
            listOf(palette.primary.copy(alpha = 0.45f), palette.primary.copy(alpha = 0.02f)),
        ),
    )
    drawPath(
        path = line,
        brush = Brush.horizontalGradient(listOf(palette.primary, palette.secondary)),
        style = Stroke(width = 3.dp.toPx()),
    )
}

private fun DrawScope.drawRadialBars(bands: FloatArray, palette: VizPalette) {
    if (bands.isEmpty()) return
    val count = bands.size
    val center = Offset(size.width / 2f, size.height / 2f)
    val inner = min(size.width, size.height) * 0.24f
    val maxLen = min(size.width, size.height) * 0.22f
    val strokeW = (2f * Math.PI.toFloat() * inner / count) * 0.6f
    for (i in 0 until count) {
        val angle = (i / count.toFloat()) * 2f * Math.PI.toFloat() - (Math.PI.toFloat() / 2f)
        val len = inner + bands[i].coerceAtLeast(FLOOR) * maxLen
        val cosA = cos(angle)
        val sinA = sin(angle)
        drawLine(
            color = lerp(palette.primary, palette.secondary, bands[i].coerceIn(0f, 1f)),
            start = Offset(center.x + cosA * inner, center.y + sinA * inner),
            end = Offset(center.x + cosA * len, center.y + sinA * len),
            strokeWidth = strokeW.coerceAtLeast(2f),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawDotMatrix(bands: FloatArray, palette: VizPalette) {
    if (bands.isEmpty()) return
    val cols = 24
    val rows = 12
    val cellW = size.width / cols
    val cellH = size.height / rows
    val dotR = min(cellW, cellH) * 0.28f
    for (c in 0 until cols) {
        // Downsample the bands to the column count.
        val band = bands[(c * bands.size / cols).coerceIn(0, bands.size - 1)]
        val lit = (band * rows).toInt()
        for (r in 0 until rows) {
            val on = r <= lit
            val fromBottom = rows - 1 - r
            val color = lerp(palette.primary, palette.tertiary, fromBottom / (rows - 1f))
            drawCircle(
                color = if (on) color else palette.primary.copy(alpha = 0.08f),
                radius = dotR,
                center = Offset(c * cellW + cellW / 2f, r * cellH + cellH / 2f),
            )
        }
    }
}

// ── Waveform styles (time domain) ────────────────────────────────────────────

private fun DrawScope.drawOscilloscope(wave: FloatArray, palette: VizPalette) {
    if (wave.size < 2) return
    val count = wave.size
    val stepX = size.width / (count - 1f)
    val mid = size.height / 2f
    val amp = size.height * 0.42f
    val path = Path().apply {
        moveTo(0f, mid - wave[0] * amp)
        for (i in 1 until count) lineTo(i * stepX, mid - wave[i] * amp)
    }
    drawPath(
        path = path,
        brush = Brush.horizontalGradient(listOf(palette.primary, palette.secondary, palette.primary)),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawFilledWave(wave: FloatArray, palette: VizPalette) {
    if (wave.size < 2) return
    val count = wave.size
    val stepX = size.width / (count - 1f)
    val mid = size.height / 2f
    val amp = size.height * 0.45f
    val path = Path().apply {
        moveTo(0f, mid)
        for (i in 0 until count) lineTo(i * stepX, mid - kotlin.math.abs(wave[i]) * amp)
        for (i in count - 1 downTo 0) lineTo(i * stepX, mid + kotlin.math.abs(wave[i]) * amp)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            listOf(
                palette.secondary.copy(alpha = 0.85f),
                palette.primary.copy(alpha = 0.85f),
                palette.secondary.copy(alpha = 0.85f),
            ),
        ),
    )
}

// ── Level styles (RMS) ───────────────────────────────────────────────────────

private fun DrawScope.drawPulse(level: Float, palette: VizPalette) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxR = min(size.width, size.height) / 2f
    val base = maxR * 0.28f
    val r = base + level * (maxR * 0.62f)
    // Soft outer glow.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.primary.copy(alpha = 0.35f), Color.Transparent),
            center = center,
            radius = (r * 1.7f).coerceAtLeast(1f),
        ),
        radius = (r * 1.7f).coerceAtLeast(1f),
        center = center,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.secondary, palette.primary),
            center = center,
            radius = r.coerceAtLeast(1f),
        ),
        radius = r.coerceAtLeast(1f),
        center = center,
    )
}

private fun DrawScope.drawRipple(level: Float, palette: VizPalette) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val maxR = min(size.width, size.height) / 2f
    val rings = 4
    for (ring in 0 until rings) {
        // Rings sit at growing radii; louder audio pushes them out and brightens them.
        val base = (ring + 1) / rings.toFloat()
        val radius = maxR * base * (0.55f + level * 0.5f)
        val alpha = (1f - base) * (0.25f + level * 0.75f)
        drawCircle(
            color = lerp(palette.primary, palette.secondary, base).copy(alpha = alpha.coerceIn(0f, 1f)),
            radius = radius.coerceAtLeast(1f),
            center = center,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

private fun DrawScope.drawVuLadder(level: Float, palette: VizPalette) {
    val segments = 20
    val gap = size.width * 0.006f
    val segW = (size.width - gap * (segments - 1)) / segments
    val segH = size.height * 0.5f
    val top = (size.height - segH) / 2f
    val lit = (level * segments).toInt()
    val radius = CornerRadius(segW * 0.18f)
    for (i in 0 until segments) {
        val frac = i / (segments - 1f)
        // Green/primary low → amber/tertiary → red peaks, blended from the theme.
        val litColor = when {
            frac < 0.6f -> lerp(palette.primary, palette.secondary, frac / 0.6f)
            frac < 0.85f -> lerp(palette.secondary, palette.tertiary, (frac - 0.6f) / 0.25f)
            else -> lerp(palette.tertiary, Color(0xFFE53935), (frac - 0.85f) / 0.15f)
        }
        drawRoundRect(
            color = if (i <= lit) litColor else palette.primary.copy(alpha = 0.10f),
            topLeft = Offset(i * (segW + gap), top),
            size = Size(segW, segH),
            cornerRadius = radius,
        )
    }
}
