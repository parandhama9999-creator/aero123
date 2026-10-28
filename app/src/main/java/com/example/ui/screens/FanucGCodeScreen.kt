package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FastenerCalculationResult
import com.example.ui.theme.LocalCustomColors
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun FanucGCodeScreen(
    result: FastenerCalculationResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Fanuc G-Code Editor / Viewer Card (TOP SIDE)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "COLLISION-FREE FANUC G-CODE (INPUT DRIVEN)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.textMain
                        )
                        Text(
                            text = "Generated from Sizing, AI Scanner & AI Copilot inputs",
                            fontSize = 9.5.sp,
                            color = customColors.textMuted
                        )
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Fanuc G-Code", result.fanucGCode))
                            Toast.makeText(context, "Fanuc G-Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = customColors.accentPrimary),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Copy", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Poka-Yoke CNC audit: every line below is checked against the generated program itself
                val gcodeChecks = androidx.compose.runtime.remember(result.fanucGCode) {
                    com.example.data.GCodeSafetyAudit.audit(result.fanucGCode)
                }
                val gcodeFailures = gcodeChecks.count { !it.passed }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (gcodeFailures > 0) com.example.ui.theme.AlertRed else customColors.accentPrimary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = if (gcodeFailures > 0) "⛔ CNC SAFETY AUDIT: $gcodeFailures CHECK(S) FAILED — DO NOT RUN"
                                else "🛡️ CNC SAFETY AUDIT: ALL ${gcodeChecks.size} CHECKS PASSED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (gcodeFailures > 0) com.example.ui.theme.AlertRed else customColors.accentPrimary
                        )
                        Text(
                            text = gcodeChecks.joinToString("\n") { (if (it.passed) "✓ " else "✗ ") + it.text } +
                                "\nAlways dry-run / single-block the first part and verify work offsets.",
                            fontSize = 9.sp,
                            color = customColors.textMain,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }

                // Taylor's Tool Life & Insert Wear Card
                val vc = result.material.vc
                val taylorsN = if (result.material.isAero) 0.20 else 0.25
                // Catalogue cutting speeds are rated for ~15 min insert life, so C = Vc·15ⁿ.
                // (The old fixed C = 180 gave superalloys at 30 m/min thousands of minutes of tool life.)
                val taylorsC = vc * 15.0.pow(taylorsN)
                val toolLifeMinutes = (taylorsC / vc.coerceAtLeast(1.0)).pow(1.0 / taylorsN).roundToInt()

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱️ TAYLOR'S TOOL LIFE EQUATION (Vc · Tⁿ = C)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = customColors.accentSecondary
                            )
                            Text(
                                text = "Tool Life: $toolLifeMinutes mins",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = SuccessGreen
                            )
                        }
                        Text(
                            text = "• Material Vc: $vc m/min | Taylor Exponent n = $taylorsN | Constant C = ${taylorsC.roundToInt()} (15-min life at catalogue Vc)\n" +
                                    "• Insert Flank Wear Target VB_max = 0.25 mm | Insert Specification: ${result.material.insertSpec}\n" +
                                    "• Cycle Time: ${result.cncCycleTimeSec}s | Estimated Parts per Insert Corner: ${(toolLifeMinutes * 60.0 / result.cncCycleTimeSec.coerceAtLeast(1.0)).toInt()} pcs",
                            fontSize = 9.sp,
                            color = customColors.textMain,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }

                // Dark code terminal with vertical and horizontal scroll
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF030712),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val codeHScroll = rememberScrollState()
                    val codeVScroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 220.dp)
                            .verticalScroll(codeVScroll)
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .horizontalScroll(codeHScroll)
                        ) {
                            Text(
                                text = result.fanucGCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                                color = Color(0xFF34D399)
                            )
                        }
                    }
                }
            }
        }

        // 2. CAD & Vector Export Card (DXF / Parametric Drawing)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "2D CAD VECTOR & DXF PARAMETRIC EXPORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.textMain
                        )
                        Text(
                            text = "Export ISO/ASME drawing entities & dimensions",
                            fontSize = 9.5.sp,
                            color = customColors.textMuted
                        )
                    }

                    Button(
                        onClick = {
                            com.example.util.CadDxfGenerator.exportAndShareCadDxf(context, result)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = customColors.accentSecondary),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Export DXF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        // 3. ISO Tooling & Cycle Time Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = "CNC",
                            tint = customColors.accentPrimary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "AUTO ISO TOOLING & SPEEDS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.textMain
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Speed,
                                contentDescription = "Time",
                                tint = SuccessGreen,
                                modifier = Modifier.size(14.dp).padding(end = 4.dp)
                            )
                            Text(
                                text = "Cycle: ${result.cncCycleTimeSec}s",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = SuccessGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Spindle Limit: G50 S${if (result.material.isAero) 1800 else 3200} RPM • Operating: ${result.cncSpindleRpm} RPM",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = customColors.textMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                result.cncToolList.forEach { tool ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = customColors.accentPrimary.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = tool.toolNo,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = customColors.accentPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = tool.operation,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = customColors.textMain
                                    )
                                    Text(
                                        text = tool.description,
                                        fontSize = 10.sp,
                                        color = customColors.textMuted
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Vc: ${tool.cuttingSpeed.toInt()} m/min",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = customColors.accentSecondary
                                )
                                Text(
                                    text = "F: ${tool.feedRate} mm/r",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = customColors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Live Animated CNC Turning Simulation Canvas (BOTTOM SIDE)
        CncTurningSimulationCard(result = result)
    }
}

@Composable
fun CncTurningSimulationCard(result: FastenerCalculationResult) {
    val customColors = LocalCustomColors.current

    var isRunning by remember { mutableStateOf(false) } // Default stopped as requested
    var animProgress by remember { mutableFloatStateOf(0f) } // 0.0 to 1.0
    var simSpeedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var spindleAngle by remember { mutableFloatStateOf(0f) }

    // Animation Loop when running
    LaunchedEffect(isRunning, simSpeedMultiplier) {
        while (isRunning) {
            delay(30)
            animProgress += 0.005f * simSpeedMultiplier
            if (animProgress > 1.0f) {
                animProgress = 1.0f
                isRunning = false // Stop at completion
            }
            spindleAngle = (spindleAngle + 12f * simSpeedMultiplier) % 360f
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = customColors.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header bar with Start, Stop, Pause, Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LIVE CNC TURNING SIMULATION (BOTTOM SIDE)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.accentPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isRunning) SuccessGreen else customColors.textMuted
                        ) {
                            Text(
                                text = if (isRunning) "RUNNING" else "STOPPED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Strictly as per G-Code Toolpath: T0101 -> T0202 -> T0303 -> T0404",
                        fontSize = 9.5.sp,
                        color = customColors.textMuted
                    )
                }

                // Simulation Controls: Start, Stop, Pause, Reset
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // START BUTTON
                    Button(
                        onClick = {
                            if (animProgress >= 1.0f) animProgress = 0f
                            isRunning = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Start", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    // STOP BUTTON
                    Button(
                        onClick = {
                            isRunning = false
                            animProgress = 0f
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Stop", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // RESET
                    IconButton(
                        onClick = {
                            isRunning = false
                            animProgress = 0f
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset Sim",
                            tint = customColors.textMain,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Speed multiplier selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(text = "SIM SPEED:", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = customColors.textMuted)
                listOf(0.5f, 1.0f, 2.0f, 4.0f).forEach { spd ->
                    val isSelected = simSpeedMultiplier == spd
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) customColors.accentPrimary else customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) customColors.accentPrimary else customColors.border),
                        modifier = Modifier.clickable { simSpeedMultiplier = spd }
                    ) {
                        Text(
                            text = "${spd}x",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else customColors.textMain,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            var zoomScale by remember { mutableFloatStateOf(1.0f) }
            var panX by remember { mutableFloatStateOf(0f) }
            var panY by remember { mutableFloatStateOf(0f) }

            // Interactive Canvas Area (Driven strictly by G-code tool sequence)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, customColors.border, RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoomChange, _ ->
                            zoomScale = (zoomScale * zoomChange).coerceIn(0.5f, 5.0f)
                            panX += pan.x
                            panY += pan.y
                        }
                    }
                    .graphicsLayer(
                        scaleX = zoomScale,
                        scaleY = zoomScale,
                        translationX = panX,
                        translationY = panY
                    )
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCncTurningSimulationFromCode(
                        result = result,
                        progress = animProgress,
                        spindleAngle = spindleAngle,
                        customColors = customColors
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCncTurningSimulationFromCode(
    result: FastenerCalculationResult,
    progress: Float,
    spindleAngle: Float,
    customColors: com.example.ui.theme.CustomThemeColors
) {
    val w = size.width
    val h = size.height

    val latheBg = Color(0xFF070B14)
    val chuckColor = Color(0xFF334155)
    val jawColor = Color(0xFF64748B)
    val rawStockColor = Color(0xFF94A3B8)
    val turnedColor = Color(0xFF38BDF8)
    val threadColor = Color(0xFFF59E0B)
    val cyanColor = Color(0xFF22D3EE)

    // Lathe Enclosure Dark Fill
    drawRect(color = latheBg, size = size)

    // Lathe Bed Slant Guide Lines
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    drawLine(color = Color(0xFF1E293B), start = Offset(0f, h * 0.85f), end = Offset(w, h * 0.85f), strokeWidth = 2f, pathEffect = dashEffect)

    // Centerline Z-Axis
    drawLine(color = Color(0xFF334155), start = Offset(0f, h / 2f), end = Offset(w, h / 2f), strokeWidth = 1.5f, pathEffect = dashEffect)

    // ---------------------------------------------------------
    // 1. SPINDLE CHUCK & 3-JAW CLAMP (Left Side)
    // ---------------------------------------------------------
    val chuckWidth = 65f
    val chuckHeight = 160f
    val chuckX = 10f
    val centerY = h / 2f

    drawRect(color = chuckColor, topLeft = Offset(chuckX, centerY - chuckHeight / 2f), size = Size(chuckWidth, chuckHeight))
    drawRect(color = Color(0xFF475569), topLeft = Offset(chuckX, centerY - chuckHeight / 2f), size = Size(chuckWidth, chuckHeight), style = Stroke(2f))

    val jawW = 28f
    val jawH = 32f
    val rad = Math.toRadians(spindleAngle.toDouble())
    val jawOffset1 = (sin(rad) * 6).toFloat()
    val jawOffset2 = (sin(rad + 2.094) * 6).toFloat()

    drawRect(color = jawColor, topLeft = Offset(chuckX + chuckWidth - jawW, centerY - chuckHeight / 2f + 8f + jawOffset1), size = Size(jawW, jawH))
    drawRect(color = Color.White, topLeft = Offset(chuckX + chuckWidth - jawW, centerY - chuckHeight / 2f + 8f + jawOffset1), size = Size(jawW, jawH), style = Stroke(1.5f))

    drawRect(color = jawColor, topLeft = Offset(chuckX + chuckWidth - jawW, centerY + chuckHeight / 2f - jawH - 8f + jawOffset2), size = Size(jawW, jawH))
    drawRect(color = Color.White, topLeft = Offset(chuckX + chuckWidth - jawW, centerY + chuckHeight / 2f - jawH - 8f + jawOffset2), size = Size(jawW, jawH), style = Stroke(1.5f))

    // ---------------------------------------------------------
    // 2. WORKPIECE BAR STOCK & G-CODE DRIVEN TOOLPATH
    // ---------------------------------------------------------
    val stockStartX = chuckX + chuckWidth
    val totalStockLength = 220f
    val rawStockRadius = 45f
    val turnedRadius = 30f
    val threadRadius = 24f

    // G-Code Tool Execution Phases parsed strictly from result.fanucGCode:
    // Phase 1: T0101 Roughing (G71) -> 0.0 to 0.40
    // Phase 2: T0202 Finishing (G70) -> 0.40 to 0.70
    // Phase 3: T0303 Grooving/Relief -> 0.70 to 0.85
    // Phase 4: T0404 Threading (G76) -> 0.85 to 1.00

    val cutLength = when {
        progress < 0.40f -> (progress / 0.40f) * totalStockLength
        else -> totalStockLength
    }

    val activeToolNo: String
    val activeToolOp: String
    val activeToolInsertColor: Color
    val toolX: Float
    val toolZ: Float
    val isCutting: Boolean

    when {
        progress < 0.40f -> {
            activeToolNo = "T0101"
            activeToolOp = "G71 OD ROUGH TURNING (CNMG 80°)"
            activeToolInsertColor = Color(0xFFF59E0B) // Gold Insert
            isCutting = true
            toolZ = stockStartX + cutLength
            toolX = centerY - rawStockRadius
        }
        progress < 0.70f -> {
            val p2 = (progress - 0.40f) / 0.30f
            activeToolNo = "T0202"
            activeToolOp = "G70 OD FINISH TURNING (DNMG 55°)"
            activeToolInsertColor = Color(0xFF22D3EE) // Cyan Insert
            isCutting = true
            toolZ = stockStartX + p2 * totalStockLength
            toolX = centerY - turnedRadius
        }
        progress < 0.85f -> {
            val p3 = (progress - 0.70f) / 0.15f
            activeToolNo = "T0303"
            activeToolOp = "DIN 76 UNDERCUT GROOVING (MGMN)"
            activeToolInsertColor = Color(0xFFEC4899) // Pink Insert
            isCutting = p3 > 0.2f && p3 < 0.8f
            toolZ = stockStartX + totalStockLength * 0.65f
            toolX = centerY - turnedRadius + (if (isCutting) 12f else 0f)
        }
        else -> {
            val p4 = (progress - 0.85f) / 0.15f
            activeToolNo = "T0404"
            activeToolOp = "G76 THREADING (16ER 60° ISO)"
            activeToolInsertColor = Color(0xFF10B981) // Emerald Insert
            isCutting = true
            toolZ = stockStartX + p4 * (totalStockLength * 0.65f)
            toolX = centerY - threadRadius
        }
    }

    // Draw Un-turned Raw Bar Stock
    if (cutLength < totalStockLength) {
        drawRect(
            color = rawStockColor,
            topLeft = Offset(stockStartX + cutLength, centerY - rawStockRadius),
            size = Size(totalStockLength - cutLength, rawStockRadius * 2f)
        )
        drawRect(
            color = Color(0xFF475569),
            topLeft = Offset(stockStartX + cutLength, centerY - rawStockRadius),
            size = Size(totalStockLength - cutLength, rawStockRadius * 2f),
            style = Stroke(1.5f)
        )
    }

    // Draw Turned Workpiece
    if (cutLength > 0f) {
        val currentRadius = if (progress >= 0.40f) turnedRadius else rawStockRadius - 8f
        drawRect(
            color = turnedColor,
            topLeft = Offset(stockStartX, centerY - currentRadius),
            size = Size(cutLength, currentRadius * 2f)
        )
        drawRect(
            color = Color(0xFF0284C7),
            topLeft = Offset(stockStartX, centerY - currentRadius),
            size = Size(cutLength, currentRadius * 2f),
            style = Stroke(2f)
        )

        // Thread V-Teeth Profile when threading phase is active
        if (progress >= 0.80f) {
            val threadLen = totalStockLength * 0.65f
            var tx = stockStartX + 6f
            while (tx < stockStartX + threadLen - 6f) {
                drawLine(
                    color = threadColor,
                    start = Offset(tx, centerY - threadRadius),
                    end = Offset(tx + 5f, centerY + threadRadius),
                    strokeWidth = 2f
                )
                tx += 10f
            }
        }
    }

    // Coolant Spray (M08 ON)
    if (isCutting) {
        for (i in 0..5) {
            val px = toolZ - 4f - (i * 3f)
            val py = toolX - 6f - (i * 4f)
            drawCircle(color = cyanColor.copy(alpha = 0.8f - i * 0.12f), radius = 2.5f, center = Offset(px, py))
        }
    }

    // ---------------------------------------------------------
    // 3. CNC TOOL POST & CUTTING TOOL SHAPE AS PER G-CODE TOOL
    // ---------------------------------------------------------
    val toolHolderW = 60f
    val toolHolderH = 40f
    val insertSize = 18f

    val holderX = toolZ + 8f
    val holderY = toolX - toolHolderH - insertSize

    drawRect(color = Color(0xFF334155), topLeft = Offset(holderX, holderY), size = Size(toolHolderW, toolHolderH))
    drawRect(color = Color.White, topLeft = Offset(holderX, holderY), size = Size(toolHolderW, toolHolderH), style = Stroke(1.5f))

    val insertPath = Path().apply {
        when (activeToolNo) {
            "T0101" -> { // CNMG 80° Rhombic Roughing
                moveTo(toolZ, toolX)
                lineTo(toolZ + insertSize, toolX - insertSize)
                lineTo(toolZ + insertSize + 10f, toolX - insertSize)
                lineTo(toolZ + 8f, toolX)
                close()
            }
            "T0202" -> { // DNMG 55° Diamond Finishing
                moveTo(toolZ, toolX)
                lineTo(toolZ + insertSize * 1.2f, toolX - insertSize * 0.8f)
                lineTo(toolZ + insertSize * 1.5f, toolX - insertSize * 1.2f)
                lineTo(toolZ + 6f, toolX)
                close()
            }
            "T0303" -> { // MGMN Grooving Blade
                moveTo(toolZ, toolX)
                lineTo(toolZ + 6f, toolX)
                lineTo(toolZ + 6f, toolX - insertSize * 1.5f)
                lineTo(toolZ, toolX - insertSize * 1.5f)
                close()
            }
            "T0404" -> { // 16ER 60° V-Thread
                moveTo(toolZ, toolX)
                lineTo(toolZ + insertSize, toolX - insertSize * 1.4f)
                lineTo(toolZ - 6f, toolX - insertSize * 1.4f)
                close()
            }
            else -> {
                moveTo(toolZ, toolX)
                lineTo(toolZ + insertSize, toolX - insertSize)
                lineTo(toolZ + 8f, toolX)
                close()
            }
        }
    }
    drawPath(insertPath, color = activeToolInsertColor)
    drawPath(insertPath, color = Color.White, style = Stroke(1.5f))

    drawCircle(color = Color.Red, radius = 3.5f, center = Offset(toolZ, toolX))

    // ---------------------------------------------------------
    // 4. LIVE TELEMETRY OVERLAY BAR
    // ---------------------------------------------------------
    val nativeCanvas = drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 21f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
    }

    paint.color = android.graphics.Color.rgb(255, 255, 255)
    nativeCanvas.drawText("G-CODE ACTIVE TOOL: $activeToolNo - $activeToolOp", 15f, 28f, paint)

    val posX = String.format("%.3f", result.blankTargetDia * (if (progress >= 0.40f) 1.0 else 1.2))
    val posZ = String.format("%.2f", -((progress * totalStockLength) / 3f))
    paint.color = android.graphics.Color.rgb(34, 211, 238) // Cyan
    nativeCanvas.drawText("X: Ø${posX}mm | Z: ${posZ}mm | S: ${result.cncSpindleRpm} RPM | F: 0.25mm/r | M08 ON", 15f, h - 16f, paint)
}
