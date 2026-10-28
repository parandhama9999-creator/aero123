package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FastenerCalculationResult
import com.example.ui.theme.LocalCustomColors
import com.example.ui.theme.SuccessGreen

@Composable
fun PitchGaugeScreen(
    result: FastenerCalculationResult,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Straighten,
                        contentDescription = "Pitch Gauge",
                        tint = customColors.accentPrimary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Column {
                        Text(
                            text = "1:1 ON-SCREEN THREAD PITCH GAUGE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.textMain
                        )
                        Text(
                            text = "Lay physical bolt against screen to compare thread pitch",
                            fontSize = 9.sp,
                            color = customColors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Active Target Thread: ${result.threadLabel} (Pitch: ${result.pitchMm} mm)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = customColors.accentSecondary
                        )
                        Text(
                            text = "Pre-roll blank Ø: ${result.blankTargetDia} mm • Major Ø: ${result.majorDiaMin}–${result.majorDiaMax} mm • Minor Ø: ${result.minorDiaMin}–${result.minorDiaMax} mm",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = customColors.textMain,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        // Poka-Yoke Thread Gauge Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = customColors.bgElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🛡️ POKA-YOKE ISO 1502 GO/NO-GO THREAD GAUGE LIMITS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = customColors.accentPrimary
                                )
                                Text(
                                    text = "• Class ${result.threadClass} Pitch Ø Max: ${result.pitchDiaMax} mm | Min: ${result.pitchDiaMin} mm\n" +
                                            "• GO Ring Gauge Rule: Must screw smoothly over full thread length without forcing\n" +
                                            "• NO-GO Ring Gauge Rule: Must NOT enter more than 1.5 turns (Poka-Yoke Acceptance Gate)",
                                    fontSize = 9.sp,
                                    color = customColors.textMain,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive 1:1 Pitch Comb Canvas Simulation
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    // True 1:1 scale: the screen's physical pixel density (xdpi) converts mm to pixels
                    val pxPerMm = androidx.compose.ui.platform.LocalContext.current.resources.displayMetrics.xdpi / 25.4f
                    Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val pitchPx = (result.pitchMm.toFloat() * pxPerMm).coerceAtLeast(2f)
                            val toothH = 0.866f * pitchPx // 60° thread form height H = 0.866·P
                            val baseY = canvasHeight * 0.45f

                            // 60° comb teeth exactly one pitch apart
                            var x = 0f
                            while (x + pitchPx <= canvasWidth) {
                                drawLine(Color(0xFF34D399), Offset(x, baseY), Offset(x + pitchPx / 2f, baseY - toothH), strokeWidth = 2f)
                                drawLine(Color(0xFF34D399), Offset(x + pitchPx / 2f, baseY - toothH), Offset(x + pitchPx, baseY), strokeWidth = 2f)
                                x += pitchPx
                            }
                            drawLine(Color.White, Offset(0f, baseY), Offset(canvasWidth, baseY), strokeWidth = 1.5f)

                            // Millimetre ruler to verify the scale with a real rule
                            val rulerY = canvasHeight * 0.75f
                            var mm = 0
                            while (mm * pxPerMm <= canvasWidth) {
                                val tick = when { mm % 10 == 0 -> 22f; mm % 5 == 0 -> 14f; else -> 8f }
                                drawLine(Color.White, Offset(mm * pxPerMm, rulerY), Offset(mm * pxPerMm, rulerY - tick), strokeWidth = 1.2f)
                                mm++
                            }
                        }
                        Text(
                            text = "P = ${result.pitchMm} mm at true scale • ruler in mm (check with a steel rule; zoom must be 100%)",
                            fontSize = 8.5.sp,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Standard Pitch References: 0.5mm, 0.7mm, 0.8mm, 1.0mm, 1.25mm, 1.5mm, 1.75mm, 2.0mm",
                    fontSize = 9.sp,
                    color = customColors.textMuted
                )
            }
        }

        // Statistical Process Control (Cp / Cpk) Canvas
        SpcControlChartCanvas(result = result)
    }
}

@Composable
fun SpcControlChartCanvas(
    result: FastenerCalculationResult,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    // Capability is calculated only from readings the user measured — never assumed
    var readingsText by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val readings = com.example.data.SpcStats.parse(readingsText)
    val spc = com.example.data.SpcStats.compute(readings, result.pitchDiaMin, result.pitchDiaMax)
    val capable = spc != null && spc.cpk >= 1.67 && spc.outOfSpec == 0

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
        modifier = modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STATISTICAL PROCESS CONTROL (Cp / Cpk AUTOMOTIVE GAUSS)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF38BDF8)
                )
                val badgeColor = when {
                    spc == null -> Color(0xFF94A3B8)
                    capable -> Color(0xFF22C55E)
                    else -> Color(0xFFEF4444)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = when {
                            spc == null -> "Enter ≥ 2 readings"
                            capable -> "Cpk ${"%.2f".format(java.util.Locale.US, spc.cpk)} PASS ≥ 1.67"
                            else -> "Cpk ${"%.2f".format(java.util.Locale.US, spc.cpk)} FAIL"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            androidx.compose.material3.OutlinedTextField(
                value = readingsText,
                onValueChange = { readingsText = it },
                label = { Text("Measured pitch Ø readings (mm), separated by spaces or commas", fontSize = 9.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Drawing Normal Gaussian Bell Curve Distribution
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val originY = h - 25f

                    // Spec limits at 20 % / 80 % of the width; the curve is the real normal fit of the readings
                    val lsl = result.pitchDiaMin
                    val usl = result.pitchDiaMax
                    val lslX = w * 0.2f
                    val uslX = w * 0.8f
                    fun xOf(v: Double) = (lslX + ((v - lsl) / (usl - lsl)).toFloat() * (uslX - lslX)).coerceIn(0f, w)

                    drawLine(Color(0xFFEF4444), Offset(lslX, 10f), Offset(lslX, originY), strokeWidth = 2f)
                    drawLine(Color(0xFFEF4444), Offset(uslX, 10f), Offset(uslX, originY), strokeWidth = 2f)
                    drawLine(Color.White, Offset(0f, originY), Offset(w, originY), strokeWidth = 1f)

                    if (spc != null) {
                        val path = androidx.compose.ui.graphics.Path()
                        val steps = 120
                        for (i in 0..steps) {
                            val v = spc.mean - 4 * spc.sigma + 8 * spc.sigma * i / steps
                            val y = originY - (originY - 15f) * kotlin.math.exp(-0.5 * ((v - spc.mean) / spc.sigma).let { it * it }).toFloat()
                            if (i == 0) path.moveTo(xOf(v), y) else path.lineTo(xOf(v), y)
                        }
                        drawPath(path, Color(0xFF38BDF8), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))
                        drawLine(Color(0xFF22C55E), Offset(xOf(spc.mean), 10f), Offset(xOf(spc.mean), originY), strokeWidth = 2f)
                        readings.forEach { drawCircle(Color(0xFFFBBF24), radius = 3f, center = Offset(xOf(it), originY - 4f)) }
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("LSL: ${result.pitchDiaMin}mm", fontSize = 8.5.sp, color = Color(0xFFEF4444), fontFamily = FontFamily.Monospace)
                    Text("Target: ${result.pitchDiaSweet}mm", fontSize = 8.5.sp, color = Color(0xFF22C55E), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("USL: ${result.pitchDiaMax}mm", fontSize = 8.5.sp, color = Color(0xFFEF4444), fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (spc == null)
                    "• Measure at least 2 parts (25+ recommended, ideally 125 in subgroups) with a pitch micrometer or 3-wire method and enter the values above.\n" +
                        "• Spec limits (${result.threadClass}): LSL ${result.pitchDiaMin} mm, USL ${result.pitchDiaMax} mm. Required Cpk ≥ 1.67 for PPAP."
                else String.format(
                    java.util.Locale.US,
                    "• n = %d | Mean X̄ = %.4f mm | σ = %.4f mm\n• Cp = %.2f | Cpk = %.2f (target ≥ 1.67)%s\n• Control limits (X̄ ± 3σ): UCL %.4f | LCL %.4f mm — not the same as spec limits",
                    spc.n, spc.mean, spc.sigma, spc.cp, spc.cpk,
                    if (spc.outOfSpec > 0) "\n• ⛔ ${spc.outOfSpec} reading(s) OUT OF SPEC" else "",
                    spc.ucl, spc.lcl
                ),
                fontSize = 9.sp,
                color = customColors.textMuted
            )
        }
    }
}
