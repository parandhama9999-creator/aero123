package com.aistudio.aerofast.trmst.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.aistudio.aerofast.trmst.data.AnimFrame
import com.aistudio.aerofast.trmst.data.HeadingAnimation
import com.aistudio.aerofast.trmst.data.HeadingPlan
import com.aistudio.aerofast.trmst.data.HeadingProgression
import com.aistudio.aerofast.trmst.data.ToolingDesign

private object AnimColors {
    val background = Color(0xFF0F172A)
    val die = Color(0xFF334155)
    val dieEdge = Color(0xFF64748B)
    val punch = Color(0xFF1E3A5F)
    val metal = Color(0xFFE8A04C)
    val outline = Color(0xFFF8FAFC)
    val ko = Color(0xFF4ADE80)
    val text = Color(0xFFE2E8F0)
    val muted = Color(0xFF94A3B8)
}

private fun textPaint(color: Color, px: Float, bold: Boolean = false) = Paint().apply {
    this.color = color.toArgb(); textSize = px; isAntiAlias = true
    typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
}

/** One animation frame: die (with the bore of this blow), knock-out pin, the part and the punch, with captions. */
fun DrawScope.drawHeadingFrame(plan: HeadingPlan, f: AnimFrame, textPx: Float, partLabel: String) {
    drawRect(AnimColors.background)
    val to = plan.stations[f.toIdx]
    val top = textPx * 5.2f
    val bottomBand = textPx * 3.0f
    val maxH = plan.stations.maxOf { it.heightMm }
    val lift = maxH * 0.45
    val koMm = maxH * 0.12
    val maxW = plan.stations.maxOf { it.maxWidthMm }
    val areaH = size.height - top - bottomBand
    val scale = minOf(areaH / (maxH + lift + koMm + maxH * 0.15).toFloat(), size.width / (maxW * 3.0).toFloat())
    val cx = size.width / 2f
    val baseY = top + areaH - (koMm * scale).toFloat()           // die bottom (part bottom)
    fun Y(mm: Double) = baseY - (mm * scale).toFloat()
    fun X(mm: Double, side: Int) = cx + side * (mm * scale).toFloat()
    val toolHalf = maxW * 1.35

    // Die block with the bore of this blow
    val inDie = to.inDieMm
    if (inDie > 0.0) {
        drawRect(AnimColors.die, Offset(X(toolHalf, -1), Y(inDie)), Size((toolHalf * 2 * scale).toFloat(), (inDie * scale).toFloat()))
        val bore = profile(ToolingDesign.split(to.segments, inDie).first, clearance = true)
        val path = Path().apply {
            bore.forEachIndexed { i, (y, w) -> if (i == 0) moveTo(X(w, 1), Y(y)) else lineTo(X(w, 1), Y(y)) }
            bore.asReversed().forEach { (y, w) -> lineTo(X(w, -1), Y(y)) }
            close()
        }
        drawPath(path, AnimColors.background)
        drawLine(AnimColors.dieEdge, Offset(X(toolHalf, -1), Y(inDie)), Offset(X(toolHalf, 1), Y(inDie)), 2f)
        // Knock-out pin under the part
        val koHalf = (bore.firstOrNull()?.second ?: 2.0) * 0.9
        drawRect(AnimColors.ko.copy(alpha = 0.7f), Offset(X(koHalf, -1), baseY), Size((koHalf * 2 * scale).toFloat(), (koMm * scale).toFloat()))
    }

    // The part
    val pts = HeadingAnimation.outline(plan, f)
    val part = Path().apply {
        pts.forEachIndexed { i, (y, w) -> if (i == 0) moveTo(X(w, 1), Y(y)) else lineTo(X(w, 1), Y(y)) }
        pts.asReversed().forEach { (y, w) -> lineTo(X(w, -1), Y(y)) }
        close()
    }
    drawPath(part, AnimColors.metal)
    drawPath(part, AnimColors.outline, style = Stroke(2f))
    val partTop = pts.last().first
    // Socket / recess appears as the blow that forms it closes
    val from = plan.stations[f.fromIdx]
    to.recesses.filter { it.fromTop }.forEach { rc ->
        val alpha = if (from.recesses.any { it.fromTop }) 1f else f.morph.toFloat()
        val w = rc.sizeMm / 2
        drawRect(AnimColors.background.copy(alpha = alpha), Offset(X(w, -1), Y(partTop)), Size((w * 2 * scale).toFloat(), (rc.depthMm * scale).toFloat()))
    }

    // Punch coming down on the part
    val punchY = Y(partTop + f.punchGapMm)
    drawRect(AnimColors.punch, Offset(X(toolHalf, -1), top), Size((toolHalf * 2 * scale).toFloat(), (punchY - top).coerceAtLeast(0f)))
    drawLine(AnimColors.dieEdge, Offset(X(toolHalf, -1), punchY), Offset(X(toolHalf, 1), punchY), 2f)

    // Captions
    val c = drawContext.canvas.nativeCanvas
    val title = if (f.blowCount == 0) to.title else "Blow ${f.blowNumber} of ${f.blowCount}: ${to.title}"
    c.drawText(partLabel, textPx * 0.8f, textPx * 1.4f, textPaint(AnimColors.muted, textPx * 0.85f))
    c.drawText(title, textPx * 0.8f, textPx * 2.8f, textPaint(AnimColors.text, textPx * 1.1f, bold = true))
    val opPaint = textPaint(AnimColors.muted, textPx * 0.8f)
    com.aistudio.aerofast.trmst.util.DrawingExporter.wrap(to.operation, opPaint, size.width - textPx * 1.6f).take(2).forEachIndexed { i, l ->
        c.drawText(l, textPx * 0.8f, textPx * (3.9f + i * 1.0f), opPaint)
    }
    val capW = size.width - textPx * 1.6f
    fun fitted(t: String): Paint = textPaint(AnimColors.muted, textPx * 0.7f).also { p -> while (p.measureText(t) > capW && p.textSize > 8f) p.textSize -= 0.5f }
    val l1 = "Key shapes = the app's stations • flow between them interpolated, not simulated"
    val l2 = "Wire Ø${"%.2f".format(java.util.Locale.US, plan.slugDiaMm)} • cut-off ${"%.2f".format(java.util.Locale.US, plan.slugLengthMm)} mm • ${plan.machineDescription}"
    c.drawText(l1, textPx * 0.8f, size.height - textPx * 1.6f, fitted(l1))
    c.drawText(l2, textPx * 0.8f, size.height - textPx * 0.6f, fitted(l2))
}

/** Plays the heading sequence in a loop. */
@Composable
fun HeadingAnimationPlayer(plan: HeadingPlan, partLabel: String, playing: Boolean, modifier: Modifier = Modifier.fillMaxWidth().height(420.dp)) {
    var t by remember(plan) { mutableDoubleStateOf(0.0) }
    val total = HeadingAnimation.durationSec(plan)
    LaunchedEffect(plan, playing) {
        if (!playing) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) t = (t + (now - last) / 1e9) % total
                last = now
            }
        }
    }
    Canvas(modifier) { drawHeadingFrame(plan, HeadingAnimation.frameAt(plan, t), 28f, partLabel) }
}
