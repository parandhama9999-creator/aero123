package com.aistudio.aerofast.trmst.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aerofast.trmst.data.FastenerCalculationResult
import com.aistudio.aerofast.trmst.data.HeadingProgression
import com.aistudio.aerofast.trmst.data.MachineSetup
import com.aistudio.aerofast.trmst.data.MachineSetupStore
import com.aistudio.aerofast.trmst.data.ProfileSegment
import com.aistudio.aerofast.trmst.data.SectionShape
import com.aistudio.aerofast.trmst.data.StationTooling
import com.aistudio.aerofast.trmst.data.ToolBasis
import com.aistudio.aerofast.trmst.data.ToolItem
import com.aistudio.aerofast.trmst.data.ToolingDesign
import com.aistudio.aerofast.trmst.ui.theme.AlertRed
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors
import com.aistudio.aerofast.trmst.ui.theme.SuccessGreen

/** Header tooling for the current part: die bore, punch cavity, pins and knock-out, checked against the machine. */
@Composable
fun ToolingScreen(result: FastenerCalculationResult) {
    val context = LocalContext.current
    val c = LocalCustomColors.current
    val machine = MachineSetupStore.setup
    val sheet = remember(result, machine) { ToolingDesign.build(result, machine) }

    Column(
        modifier = Modifier.fillMaxSize().background(c.bgMain).verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("HEADER TOOLING", fontSize = 14.sp, fontWeight = FontWeight.Black, color = c.accentPrimary)
        if (sheet == null) {
            Text("No heading plan for this part, so there is no header tooling to show.", fontSize = 12.sp, color = c.textMuted)
            return@Column
        }
        Text("${result.threadLabel} × ${"%.1f".format(java.util.Locale.US, result.lengthMm)} mm ${result.fastenerType.label} • ${sheet.machine}",
            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.textMain)
        Text(
            "Every figure carries its source: calculated from the part, your machine, the material catalogue, or the toolmaker's decision where no published rule exists.",
            fontSize = 10.sp, color = c.textMuted
        )

        MachineSetupCard(machine) { MachineSetupStore.save(context, it) }

        if (sheet.problems.isNotEmpty()) {
            ToolCard("⛔ DOES NOT FIT YOUR MACHINE", AlertRed) {
                sheet.problems.forEach { Text("• $it", fontSize = 11.sp, color = AlertRed, fontWeight = FontWeight.Bold) }
            }
        }

        ToolCard("0. CUT-OFF", c.accentSecondary) { sheet.cutOff.forEach { ItemRow(it) } }
        sheet.stations.forEach { st -> StationCard(st) }
        ToolCard("TOOL MATERIALS", c.accentSecondary) { sheet.materials.forEach { ItemRow(it) } }
        ToolCard("NOT SET BY THE APP", c.textMuted) {
            sheet.notDecided.forEach { Text("• $it", fontSize = 10.sp, color = c.textMain) }
        }

        Button(
            onClick = {
                val text = ToolingDesign.text(result, sheet, machine)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Header tooling sheet")
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, "Share tooling sheet"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = c.accentPrimary),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Share tooling sheet", color = Color.White, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ToolCard(title: String, accent: Color, content: @Composable () -> Unit) {
    val c = LocalCustomColors.current
    Surface(
        shape = RoundedCornerShape(10.dp), color = c.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, c.border), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Black, color = accent)
            content()
        }
    }
}

@Composable
private fun ItemRow(i: ToolItem) {
    val c = LocalCustomColors.current
    val tagColor = when (i.basis) {
        ToolBasis.CALCULATED -> SuccessGreen
        ToolBasis.MACHINE -> c.accentPrimary
        ToolBasis.CATALOGUE -> c.accentSecondary
        ToolBasis.TOOLMAKER -> c.textMuted
    }
    Column {
        Text(i.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c.textMuted)
        Text(i.value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = c.textMain)
        Text("[${i.basis.tag}]", fontSize = 9.sp, color = tagColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StationCard(st: StationTooling) {
    val c = LocalCustomColors.current
    ToolCard("${st.station.index}. ${st.station.title.uppercase()}", c.accentPrimary) {
        Text(st.station.operation, fontSize = 10.sp, color = c.textMuted)
        ToolSection(st, Modifier.fillMaxWidth().height(230.dp))
        Text("Schematic section, to scale with the part; tool outer sizes are not to scale.", fontSize = 9.sp, color = c.textMuted)
        st.items.forEach { ItemRow(it) }
        st.problems.forEach { Text("⛔ $it", fontSize = 11.sp, color = AlertRed, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun MachineSetupCard(m: MachineSetup, onChange: (MachineSetup) -> Unit) {
    val c = LocalCustomColors.current
    ToolCard("YOUR MACHINE (from the header manual, mm)", c.accentPrimary) {
        Text("Leave blank what you do not know: the app checks only what you enter.", fontSize = 10.sp, color = c.textMuted)
        @Composable
        fun field(label: String, value: Double?, modifier: Modifier, set: (Double?) -> MachineSetup) {
            var text by remember { mutableStateOf(value?.let { "%.1f".format(java.util.Locale.US, it).removeSuffix(".0") } ?: "") }
            OutlinedTextField(
                value = text,
                onValueChange = { t -> text = t; onChange(set(t.toDoubleOrNull()?.takeIf { it > 0.0 })) },
                singleLine = true, label = { Text(label, fontSize = 10.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = c.textMain),
                modifier = modifier
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            field("Die case OD", m.dieCaseOdMm, Modifier.weight(1f)) { m.copy(dieCaseOdMm = it) }
            field("Die case length", m.dieCaseLengthMm, Modifier.weight(1f)) { m.copy(dieCaseLengthMm = it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            field("Punch case OD", m.punchCaseOdMm, Modifier.weight(1f)) { m.copy(punchCaseOdMm = it) }
            field("Punch case length", m.punchCaseLengthMm, Modifier.weight(1f)) { m.copy(punchCaseLengthMm = it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            field("Knock-out stroke", m.koStrokeMm, Modifier.weight(1f)) { m.copy(koStrokeMm = it) }
            field("Planned insert OD", m.insertOdMm, Modifier.weight(1f)) { m.copy(insertOdMm = it) }
        }
    }
}

/** Half-width profile of segments stacked upward from y = 0: (axial mm, half-width mm) points. */
private fun profile(segs: List<ProfileSegment>, clearance: Boolean): List<Pair<Double, Double>> {
    val pts = mutableListOf<Pair<Double, Double>>()
    var y = 0.0
    fun hw(d: Double) = (if (clearance) d + HeadingProgression.stageClearance(d) else d) / 2.0
    for (s in segs) {
        when (s.shape) {
            SectionShape.DOME -> {
                val r0 = s.dBottomMm / 2.0
                val h = s.heightMm
                val rs = (r0 * r0 + h * h) / (2 * h) // sphere radius of the cap
                for (k in 0..8) {
                    val yy = h * k / 8.0
                    val x = kotlin.math.sqrt((rs * rs - (rs - h + yy).let { it * it }).coerceAtLeast(0.0))
                    pts += (y + yy) to x
                }
            }
            SectionShape.ROUND -> { pts += y to hw(s.dBottomMm); pts += (y + s.heightMm) to hw(s.dTopMm) }
            else -> { pts += y to hw(s.maxWidthMm); pts += (y + s.heightMm) to hw(s.maxWidthMm) }
        }
        y += s.heightMm
    }
    return pts
}

@Composable
private fun ToolSection(st: StationTooling, modifier: Modifier) {
    val c = LocalCustomColors.current
    val dieColor = c.textMuted.copy(alpha = 0.25f)
    val punchColor = c.accentPrimary.copy(alpha = 0.22f)
    val partColor = c.accentSecondary.copy(alpha = 0.75f)
    val koColor = SuccessGreen.copy(alpha = 0.5f)
    val line = c.textMain
    Canvas(modifier) {
        val inDieH = st.inDie.sumOf { it.heightMm }
        val aboveH = st.aboveDie.sumOf { it.heightMm }
        val koH = (inDieH * 0.35).coerceAtLeast(3.0)
        val totalH = inDieH + aboveH + koH + 6.0
        val maxW = (st.station.segments.maxOfOrNull { it.maxWidthMm } ?: 10.0)
        val toolW = maxW * 2.6
        val scale = minOf(size.height / totalH.toFloat(), size.width / toolW.toFloat()) * 0.95f
        val cx = size.width / 2f
        val dieFaceY = size.height - ((koH + inDieH + 2.0) * scale).toFloat()
        fun X(mm: Double, side: Int) = cx + side * (mm * scale).toFloat()
        fun Yup(mm: Double) = dieFaceY - (mm * scale).toFloat()        // above the die face
        fun Ydown(mm: Double) = dieFaceY + (mm * scale).toFloat()      // into the die

        // Die block with the bore cut out
        val half = (toolW / 2.0)
        val dieBottom = Ydown(inDieH + koH + 1.0)
        drawRect(dieColor, Offset(X(half, -1), dieFaceY), androidx.compose.ui.geometry.Size((toolW * scale).toFloat(), dieBottom - dieFaceY))
        // Punch block above, with its cavity
        val punchTop = Yup(aboveH + 4.0).coerceAtLeast(0f)
        drawRect(punchColor, Offset(X(half, -1), punchTop), androidx.compose.ui.geometry.Size((toolW * scale).toFloat(), dieFaceY - punchTop - 2f))

        fun shapePath(pts: List<Pair<Double, Double>>, yOf: (Double) -> Float, flip: Boolean): Path = Path().apply {
            val ordered = pts
            ordered.forEachIndexed { i, (y, w) -> if (i == 0) moveTo(X(w, 1), yOf(if (flip) inDieH - y else y)) else lineTo(X(w, 1), yOf(if (flip) inDieH - y else y)) }
            ordered.asReversed().forEach { (y, w) -> lineTo(X(w, -1), yOf(if (flip) inDieH - y else y)) }
            close()
        }
        val bg = c.bgCard
        // Bore (with clearance) cut through the die, part sitting in it
        if (st.inDie.isNotEmpty()) {
            val bore = profile(st.inDie, clearance = true)
            drawPath(shapePath(bore, ::Ydown, flip = true), bg)
            drawPath(shapePath(bore, ::Ydown, flip = true), line, style = Stroke(1.2f))
            drawPath(shapePath(profile(st.inDie, clearance = false), ::Ydown, flip = true), partColor)
            // Knock-out pin in the bottom bore
            val koHalf = (st.inDie.first().maxWidthMm / 2.0) * 0.92
            drawRect(koColor, Offset(X(koHalf, -1), Ydown(inDieH)), androidx.compose.ui.geometry.Size((koHalf * 2 * scale).toFloat(), (koH * scale).toFloat()))
        }
        // Cavity in the punch, part above the die face
        if (st.aboveDie.isNotEmpty()) {
            val cav = profile(st.aboveDie, clearance = false)
            drawPath(shapePath(cav, ::Yup, flip = false), bg)
            drawPath(shapePath(cav, ::Yup, flip = false), partColor)
            drawPath(shapePath(cav, ::Yup, flip = false), line, style = Stroke(1.2f))
        }
        // Recess pin from the punch
        st.station.recesses.filter { it.fromTop }.forEach { rc ->
            val w = rc.sizeMm / 2.0
            drawRect(koColor, Offset(X(w, -1), Yup(aboveH)), androidx.compose.ui.geometry.Size((w * 2 * scale).toFloat(), (rc.depthMm * scale).toFloat()))
        }
        // Die face and axis
        drawLine(line, Offset(X(half, -1), dieFaceY), Offset(X(half, 1), dieFaceY), 1.5f)
        drawLine(line.copy(alpha = 0.4f), Offset(cx, punchTop), Offset(cx, dieBottom), 1f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        labels(this, c.textMuted, X(half, 1) - 4f, punchTop + 14f, dieFaceY + 14f, Ydown(inDieH + koH * 0.5))
    }
}

private fun labels(d: DrawScope, color: Color, xRight: Float, punchY: Float, dieY: Float, koY: Float) {
    val p = android.graphics.Paint().apply {
        this.color = android.graphics.Color.argb((color.alpha * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
        textSize = 22f; textAlign = android.graphics.Paint.Align.RIGHT; isAntiAlias = true
    }
    d.drawContext.canvas.nativeCanvas.apply {
        drawText("PUNCH", xRight, punchY, p)
        drawText("DIE", xRight, dieY, p)
        drawText("KO PIN", xRight, koY, p)
    }
}
