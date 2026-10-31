package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Lets other UI (e.g. the first-launch disclaimer) wait until the intro has played. */
object AppIntro {
    var finished by mutableStateOf(false)
}

private val Night = Color(0xFF070B14)
private val NightEdge = Color(0xFF0E1A2E)
private val Grid = Color(0xFF3FA9FF)
private val SteelDark = Color(0xFF5A6677)
private val SteelMid = Color(0xFF9AA8BA)
private val SteelLight = Color(0xFFEAF0F7)
private val Molten = Color(0xFFFF7A1A)
private val Spark = Color(0xFFFFD27A)

private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun ease(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)

/**
 * Opening sequence: a wire slug is cut, its head is upset by the punch, the round head is trimmed to a hex
 * (the cheese flies off), the thread is rolled, and the finished bolt is dimensioned before the name appears.
 * Tap anywhere to skip. Skipped entirely when the system has animations turned off.
 */
@Composable
fun ForgeIntro(onFinished: () -> Unit) {
    val context = LocalContext.current
    val animationsOff = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val progress = remember { Animatable(0f) }
    val fadeOut = remember { Animatable(1f) }
    var done by remember { mutableStateOf(false) }

    fun finish() {
        if (done) return
        done = true
        AppIntro.finished = true
        onFinished()
    }

    LaunchedEffect(Unit) {
        if (animationsOff) { finish(); return@LaunchedEffect }
        progress.animateTo(1f, tween(durationMillis = 3400, easing = LinearEasing))
        kotlinx.coroutines.delay(450)
        fadeOut.animateTo(0f, tween(380))
        finish()
    }

    val t = progress.value
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(fadeOut.value)
            .background(Brush.radialGradient(listOf(NightEdge, Night)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawForge(t) }

        val logoIn = ease(seg(t, 0.80f, 0.97f))
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 96.dp)
                .alpha(logoIn),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AEROFAST PRO",
                color = SteelLight,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (2 + 10 * (1 - logoIn)).sp
            )
            Text(
                text = "From wire to bolt — every number checked.",
                color = SteelMid,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp)
            )
            Text(
                text = "ISO · ASME · SAE · AMS · NAS",
                color = Molten,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        Text(
            text = "tap to skip",
            color = SteelDark,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp).alpha(1f - logoIn)
        )
    }
}

private fun DrawScope.drawForge(t: Float) {
    val w = size.width
    val h = size.height
    val u = min(w, h) / 10f

    // Blueprint grid drifts slowly
    val step = u * 0.75f
    val drift = (t * step) % step
    var gx = -drift
    while (gx < w) { drawLine(Grid.copy(alpha = 0.06f), Offset(gx, 0f), Offset(gx, h), 1f); gx += step }
    var gy = -drift
    while (gy < h) { drawLine(Grid.copy(alpha = 0.06f), Offset(0f, gy), Offset(w, gy), 1f); gy += step }

    // Bolt geometry (side view, head on the left), rising slightly at the end to make room for the name
    val lift = ease(seg(t, 0.78f, 0.95f)) * h * 0.12f
    val cy = h * 0.42f - lift
    val shankLen = 5.2f * u
    val d = 0.8f * u
    val headStock = 1.7f * u
    val kHead = 0.75f * u
    val headH = 2.1f * u
    val totalFinished = kHead + shankLen
    val headLeftFinal = (w - totalFinished) / 2f

    // 1. Slug slides in
    val slide = ease(seg(t, 0.0f, 0.16f))
    // 2. Upset: head stock shortens, diameter grows
    val upset = ease(seg(t, 0.18f, 0.36f))
    // 3. Trim to hex
    val trim = ease(seg(t, 0.38f, 0.52f))
    // 4. Thread rolling
    val roll = seg(t, 0.55f, 0.76f)

    val headLen = headStock + (kHead - headStock) * upset
    val headHeight = d + (headH - d) * upset
    val shankRight = headLeftFinal + totalFinished
    val headRight = shankRight - shankLen
    val headLeft = headRight - headLen
    val offX = (1f - slide) * -(shankRight + u)

    val steel = Brush.verticalGradient(listOf(SteelDark, SteelLight, SteelMid, SteelDark), startY = cy - headHeight / 2, endY = cy + headHeight / 2)
    val shankBrush = Brush.verticalGradient(listOf(SteelDark, SteelLight, SteelMid, SteelDark), startY = cy - d / 2, endY = cy + d / 2)

    // Punch strikes from the left during the upset
    if (t in 0.14f..0.42f) {
        val strike = if (t < 0.30f) ease(seg(t, 0.14f, 0.30f)) else 1f - seg(t, 0.32f, 0.42f)
        val punchRight = headLeft - (1f - strike) * 1.2f * u + offX
        drawRoundRect(Brush.horizontalGradient(listOf(SteelDark.copy(alpha = 0.3f), SteelMid)), Offset(punchRight - 1.6f * u, cy - 1.4f * u), Size(1.6f * u, 2.8f * u), CornerRadius(6f))
    }

    // Shank
    val threadStart = headRight + shankLen * 0.45f
    drawRoundRect(shankBrush, Offset(headRight + offX, cy - d / 2), Size(shankLen, d), CornerRadius(d * 0.12f))

    // Head: round preform, then trimmed hex (side view shows the corner facets)
    val roundness = (1f - trim) * headHeight * 0.18f
    val hexHeight = headHeight * (1f - 0.08f * trim)
    drawRoundRect(steel, Offset(headLeft + offX, cy - hexHeight / 2), Size(headLen, hexHeight), CornerRadius(roundness + 4f))
    if (trim > 0f) {
        val facet = SteelDark.copy(alpha = 0.8f * trim)
        drawLine(facet, Offset(headLeft + offX, cy - hexHeight / 4), Offset(headRight + offX, cy - hexHeight / 4), 2f)
        drawLine(facet, Offset(headLeft + offX, cy + hexHeight / 4), Offset(headRight + offX, cy + hexHeight / 4), 2f)
        // Trim cheese flying off
        if (trim < 1f) {
            val cx = (headLeft + headRight) / 2 + offX
            for (i in 0 until 16) {
                val ang = i * (Math.PI * 2 / 16) + 0.2
                val r = headHeight / 2 + trim * 2.4f * u
                drawCircle(SteelMid.copy(alpha = 1f - trim), radius = 3f + 3f * (i % 3), center = Offset(cx + (cos(ang) * r).toFloat(), cy + (sin(ang) * r).toFloat()))
            }
        }
    }

    // Impact flash and sparks when the punch lands
    val flash = 1f - seg(t, 0.30f, 0.40f)
    if (t in 0.29f..0.40f) {
        val c = Offset(headLeft + offX, cy)
        drawCircle(Brush.radialGradient(listOf(Spark.copy(alpha = 0.9f * flash), Color.Transparent), center = c, radius = 2.2f * u * (1.2f - flash)), radius = 2.2f * u, center = c)
        for (i in 0 until 12) {
            val ang = Math.PI * (0.55 + i / 11.0 * 0.9)
            val r = (1f - flash) * 2.8f * u
            drawLine(Molten.copy(alpha = flash), c, Offset(c.x + (cos(ang) * r).toFloat(), c.y + (sin(ang) * r).toFloat()), 3f)
        }
    }

    // Thread rolling: dies sweep along the thread section and leave crests behind
    if (roll > 0f) {
        val threadLen = shankRight - threadStart
        val rolledTo = threadStart + threadLen * roll
        clipRect(left = threadStart, top = cy - d, right = rolledTo, bottom = cy + d) {
            val pitch = u * 0.22f
            var x = threadStart
            while (x < shankRight) {
                drawLine(SteelDark, Offset(x, cy - d / 2 - 1.5f), Offset(x + pitch * 0.6f, cy + d / 2 + 1.5f), 2.2f)
                x += pitch
            }
            drawLine(SteelLight.copy(alpha = 0.6f), Offset(threadStart, cy - d / 2 - 1f), Offset(shankRight, cy - d / 2 - 1f), 1.5f)
        }
        if (roll < 1f) {
            val dieW = 0.9f * u
            val dieH = 0.55f * u
            val glow = Molten.copy(alpha = 0.35f)
            drawRoundRect(SteelMid, Offset(rolledTo - dieW / 2, cy - d / 2 - dieH), Size(dieW, dieH), CornerRadius(4f))
            drawRoundRect(SteelMid, Offset(rolledTo - dieW / 2, cy + d / 2), Size(dieW, dieH), CornerRadius(4f))
            drawCircle(glow, radius = d * 0.9f, center = Offset(rolledTo, cy))
        }
    }

    // Dimension line drawn under the finished bolt: the app's subject, labelled
    val dim = ease(seg(t, 0.74f, 0.88f))
    if (dim > 0f) {
        val y = cy + headH / 2 + 0.7f * u
        val x1 = headRight
        val x2 = headRight + shankLen * dim
        val c = Grid.copy(alpha = 0.85f)
        drawLine(c, Offset(x1, cy + d / 2 + 6f), Offset(x1, y + 10f), 1.5f)
        drawLine(c, Offset(x1, y), Offset(x2, y), 2f)
        val arrow = Path().apply { moveTo(x2, y); lineTo(x2 - 12f, y - 6f); lineTo(x2 - 12f, y + 6f); close() }
        drawPath(arrow, c)
        if (dim >= 1f) drawLine(c, Offset(x2, cy + d / 2 + 6f), Offset(x2, y + 10f), 1.5f)
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb((255 * dim).toInt(), 0x3F, 0xA9, 0xFF)
            textSize = 0.36f * u
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("M10 × 1.5 × 60   6g   10.9", (x1 + headRight + shankLen) / 2, y - 10f, paint)
    }

    // Faint outline glow once finished
    val done = seg(t, 0.76f, 1f)
    if (done > 0f) {
        drawRoundRect(Molten.copy(alpha = 0.25f * done), Offset(headLeftFinal - 6f, cy - headH / 2 - 6f), Size(totalFinished + 12f, headH + 12f), CornerRadius(14f), style = Stroke(1.5f))
    }
}
