package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FastenerCalculationResult
import com.example.data.PokaYokeEngine
import com.example.data.TorqueCalculator
import kotlin.math.roundToInt
import com.example.ui.theme.LocalCustomColors
import com.example.ui.theme.SuccessGreen

@Composable
fun JointTorqueScreen(
    result: FastenerCalculationResult,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    if (result.fastenerType.isUnthreaded) {
        Text(
            text = "Rivets are not tightened by torque: the joint is set by upsetting (solid) or pulling the mandrel (blind). " +
                "Size it from rivet shear and bearing allowables (MMPDS) and the rivet procurement spec.",
            color = customColors.textMain,
            modifier = modifier
        )
        return
    }
    val scrollState = rememberScrollState()

    // Friction defaults follow the selected finish and reset when it changes; the user can then adjust them
    val typicalMu = remember(result.coating, result.material.key) {
        TorqueCalculator.typicalFriction(result.coating, PokaYokeEngine.materialFamily(result.material))
    }
    var frictionMuThreads by remember(typicalMu) { mutableDoubleStateOf(typicalMu.first) }
    var frictionMuBearing by remember(typicalMu) { mutableDoubleStateOf(typicalMu.second) }
    var targetPreloadRatio by remember { mutableDoubleStateOf(0.75) }

    val d = result.nominalDiaMm
    val proofLoadN = result.proofLoadKn * 1000.0
    val targetPreloadN = proofLoadN * targetPreloadRatio
    val torque = TorqueCalculator.compute(
        d = d,
        pitch = result.pitchMm,
        stressAreaMm2 = result.stressAreaAsMm2,
        yieldMpa = result.yieldStrengthMpa,
        preloadN = targetPreloadN,
        muThread = frictionMuThreads,
        muBearing = frictionMuBearing,
        bearingDiaDw = result.headDimensions?.takeIf { it.isTable }?.bearingDiaDw
    )
    val nutFactorK = torque.nutFactorK.round(3)
    val tighteningTorqueNm = torque.torqueNm
    // VDI 2230: combined tension + torsion during tightening must stay ≤ 90 % of yield
    val overYield = torque.yieldUtilization > 0.90
    val notApplicable = result.fastenerType.isNut || result.fastenerType.isWasher

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
                        Icons.Default.Build,
                        contentDescription = "Joint Torque",
                        tint = customColors.accentSecondary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Column {
                        Text(
                            text = "ADVANCED BOLTED JOINT & TORQUE CALCULATOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.textMain
                        )
                        Text(
                            text = "Friction coefficients, Nut Factor K & Clamping Preload",
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
                        if (notApplicable) {
                            Text(
                                text = "ℹ️ Tightening torque is calculated for the bolt/screw. Select the mating bolt or stud on the Sizing tab.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMuted,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        TorqueSlider("Thread friction μG", frictionMuThreads, 0.06f..0.30f) { frictionMuThreads = it }
                        TorqueSlider("Head/bearing friction μK", frictionMuBearing, 0.06f..0.30f) { frictionMuBearing = it }
                        TorqueSlider("Target preload (% of proof load)", targetPreloadRatio, 0.40f..1.00f, asPercent = true) { targetPreloadRatio = it }
                        Text(
                            text = "Typical for ${result.coating.label}: μ ≈ ${typicalMu.first}. Use measured ISO 16047 values for production.",
                            fontSize = 8.5.sp,
                            color = customColors.textMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            text = "Effective Nut Factor (K): $nutFactorK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = customColors.accentPrimary
                        )
                        Text(
                            text = "Target Clamping Preload: ${(targetPreloadN / 1000.0).round(2)} kN (${(targetPreloadRatio * 100).toInt()}% of Proof Load)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "Tightening Torque: ${tighteningTorqueNm.round(1)} N·m (${(tighteningTorqueNm * 0.737562).round(1)} lbf·ft)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = customColors.accentSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "= pitch ${torque.pitchTorqueNm.round(1)} + thread friction ${torque.threadFrictionTorqueNm.round(1)} + head friction ${torque.bearingFrictionTorqueNm.round(1)} N·m (DKm ${torque.bearingDiaDkmMm.round(1)} mm)\n" +
                                "Assembly stress: σ ${torque.axialStressMpa.round(0)} MPa, τ ${torque.torsionStressMpa.round(0)} MPa → von Mises ${torque.vonMisesStressMpa.round(0)} MPa = ${(torque.yieldUtilization * 100).round(0)}% of yield ${result.yieldStrengthMpa.toInt()} MPa",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = customColors.textMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        // Poka-Yoke VDI 2230 Bolted Joint Safeguard Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = customColors.bgElevated,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (overYield || targetPreloadRatio < 0.50) com.example.ui.theme.AlertRed else customColors.accentPrimary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (overYield)
                                        "⛔ POKA-YOKE OVER-TORQUE: BOLT WILL YIELD WHILE TIGHTENING"
                                    else if (targetPreloadRatio < 0.50)
                                        "⚠️ POKA-YOKE UNDER-TORQUE / JOINT SEPARATION ALERT"
                                    else
                                        "🛡️ VDI 2230 ASSEMBLY STRESS CHECK PASSED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (overYield || targetPreloadRatio < 0.50) com.example.ui.theme.AlertRed else customColors.accentPrimary
                                )
                                Text(
                                    text = if (overYield)
                                        "• Tension plus thread torsion reaches ${(torque.yieldUtilization * 100).toInt()}% of yield (limit 90%). Lower the preload, or use a lubricated finish to reduce torsion."
                                    else if (targetPreloadRatio < 0.50)
                                        "• Target preload is below 50% of proof load (${(targetPreloadRatio * 100).toInt()}%). High risk of self-loosening under vibration."
                                    else
                                        "• Assembly stress ${(torque.yieldUtilization * 100).toInt()}% of yield (≤ 90% per VDI 2230). Torque scatter of the tightening tool still has to be added (αA).",
                                    fontSize = 9.sp,
                                    color = customColors.textMain,
                                    modifier = Modifier.padding(top = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Joint Mechanics Parameters:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = customColors.textMain
                )
                Text(
                    text = "• Thread friction μG: ${frictionMuThreads.round(2)}\n• Head bearing friction μK: ${frictionMuBearing.round(2)}\n• Proof load (${result.propertyClass?.label?.substringBefore(" (") ?: "material"}): ${result.proofLoadKn} kN, stress area As ${result.stressAreaAsMm2} mm²",
                    fontSize = 9.5.sp,
                    color = customColors.textMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // FEA von Mises Stress Heatmap Canvas
        FeaStressHeatmapCanvas(result = result)

        // Goodman Dynamic Fatigue Diagram Canvas
        GoodmanFatigueDiagramCanvas(result = result)
    }
}

@Composable
fun FeaStressHeatmapCanvas(
    result: FastenerCalculationResult,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FEA VON MISES STRESS CONCENTRATION HEATMAP",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = "Kt = ${result.stressConcentrationKt}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFF59E0B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Heatmap Simulation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val gradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF3B82F6), // Blue: Low
                            Color(0xFF22C55E), // Green: Medium
                            Color(0xFFEAB308), // Yellow: High
                            Color(0xFFEF4444)  // Red: Yield Peak
                        )
                    )

                    val headW = 70f
                    val headH = 45f
                    val shankW = 150f
                    val shankH = 30f
                    val startX = 30f
                    val centerY = h / 2f

                    // Head Area
                    drawRect(
                        color = Color(0xFF1E3A8A),
                        topLeft = Offset(startX, centerY - headH / 2f),
                        size = Size(headH, headW)
                    )

                    // Head-Fillet High Stress Zone
                    drawCircle(
                        color = Color(0xFFEAB308),
                        radius = 10f,
                        center = Offset(startX + headH, centerY - shankH / 2f)
                    )

                    // Shank Body
                    drawRect(
                        brush = gradient,
                        topLeft = Offset(startX + headH, centerY - shankH / 2f),
                        size = Size(shankW, shankH)
                    )

                    // Thread Root Stress Concentration Peaks
                    var threadX = startX + headH + shankW * 0.35f
                    while (threadX < startX + headH + shankW) {
                        drawCircle(
                            color = Color(0xFFEF4444),
                            radius = 5f,
                            center = Offset(threadX, centerY - shankH / 2f)
                        )
                        drawCircle(
                            color = Color(0xFFEF4444),
                            radius = 5f,
                            center = Offset(threadX, centerY + shankH / 2f)
                        )
                        threadX += 16f
                    }
                }

                // Legend Overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🟦 <30%", fontSize = 8.sp, color = Color.White)
                    Text("🟩 30-60%", fontSize = 8.sp, color = Color.White)
                    Text("🟨 60-85%", fontSize = 8.sp, color = Color.White)
                    Text("🟥 Peak (${result.peakThreadRootStressMpa} MPa)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "• Thread Root Notch Factor Kt: ${result.stressConcentrationKt} (ISO 898-1 Radiused Root)\n" +
                        "• Elastic peak at thread root (Kt × proof stress): ${result.peakThreadRootStressMpa} MPa vs yield ${result.yieldStrengthMpa} MPa\n" +
                        "• Local root yielding at proof load is normal for threads; static strength is proven by the proof-load test, fatigue by the limit below.",
                fontSize = 9.sp,
                color = customColors.textMuted
            )
        }
    }
}

@Composable
fun GoodmanFatigueDiagramCanvas(
    result: FastenerCalculationResult,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    // Bolt Haigh diagram (VDI 2230): endurance amplitude σA is flat over mean stress until σm + σA reaches yield
    val sigmaA = result.fatigueEnduranceLimitSeMpa
    val rp = result.yieldStrengthMpa
    val sigmaMeanPreload = 0.75 * result.proofLoadKn * 1000.0 / result.stressAreaAsMm2.coerceAtLeast(1.0)

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
                    text = "BOLT FATIGUE LIMIT (VDI 2230, 2×10⁶ CYCLES)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = "±${result.allowableAlternatingForceKn} kN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF22C55E)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Goodman Diagram Canvas Plot
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val originX = 40f
                    val originY = h - 30f

                    // Draw Axes
                    drawLine(Color.White, Offset(originX, originY), Offset(w - 20f, originY), strokeWidth = 2f)
                    drawLine(Color.White, Offset(originX, originY), Offset(originX, 20f), strokeWidth = 2f)

                    // Scale: x = mean stress 0..Rp, y = amplitude 0..(2.5 × σA)
                    val plotW = (w - 20f) - originX
                    val plotH = originY - 20f
                    val yMax = (2.5 * sigmaA).toFloat().coerceAtLeast(1f)
                    fun px(sm: Double) = originX + (sm / rp).toFloat().coerceIn(0f, 1f) * plotW
                    fun py(sa: Double) = originY - (sa.toFloat() / yMax).coerceIn(0f, 1f) * plotH

                    // Endurance line σA (flat) until it meets the yield line σm + σa = Rp
                    val knee = rp - sigmaA
                    drawLine(Color(0xFF38BDF8), Offset(px(0.0), py(sigmaA)), Offset(px(knee), py(sigmaA)), strokeWidth = 2.5f)
                    drawLine(Color(0xFFEF4444), Offset(px(knee), py(sigmaA)), Offset(px(rp), py(0.0)), strokeWidth = 2.5f)

                    // Mean stress from 75 % proof-load preload, with the allowable amplitude σA/1.2
                    val opX = px(sigmaMeanPreload)
                    drawLine(Color(0xFF22C55E), Offset(opX, originY), Offset(opX, py(sigmaA / 1.2)), strokeWidth = 2f)
                    drawCircle(Color(0xFF22C55E), radius = 6f, center = Offset(opX, py(sigmaA / 1.2)))
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🟢 Preload σm + allowable σa", fontSize = 8.5.sp, color = Color.White)
                    Text("➖ σA endurance", fontSize = 8.5.sp, color = Color(0xFF38BDF8))
                    Text("➖ yield", fontSize = 8.5.sp, color = Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "• Thread endurance amplitude σA: ${sigmaA} MPa (${if (result.material.isAero) "rolled after heat treatment" else "rolled before heat treatment"}; 0.85·(150/d + 45))\n" +
                        "• Allowable alternating bolt force: ±${result.allowableAlternatingForceKn} kN (σA·As / 1.2)\n" +
                        "• Check: the service force amplitude on the bolt, from your joint analysis (load factor Φ), must stay below this value. Fatigue strength of threads barely rises with property class.",
                fontSize = 9.sp,
                color = customColors.textMuted
            )
        }
    }
}

@Composable
private fun TorqueSlider(
    label: String,
    value: Double,
    range: ClosedFloatingPointRange<Float>,
    asPercent: Boolean = false,
    onChange: (Double) -> Unit
) {
    val customColors = LocalCustomColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ${if (asPercent) "${(value * 100).toInt()}%" else value.round(2).toString()}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = customColors.textMain
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange((it * 100).roundToInt() / 100.0) },
            valueRange = range,
            modifier = Modifier.height(30.dp)
        )
    }
}

private fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10.0 }
    return kotlin.math.round(this * multiplier) / multiplier
}
