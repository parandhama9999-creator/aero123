package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FastenerCalculationResult
import com.example.data.FastenerType
import com.example.ui.theme.LocalCustomColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun BlueprintCanvasView(
    result: FastenerCalculationResult,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current

    var canvasScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var rotationDeg by remember { mutableFloatStateOf(0f) }
    var brightPaper by remember { mutableStateOf(!customColors.isDark) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = customColors.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "2D CAD BLUEPRINT: ${result.fastenerType.label.uppercase()}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.accentPrimary
                        )
                    }
                    Text(
                        text = "✌️ 2-Finger Pinch Zoom • Double-Tap Reset • Drag Pan • Exact Fastener Geometry",
                        fontSize = 9.5.sp,
                        color = customColors.textMuted
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Zoom In
                    IconButton(
                        onClick = { canvasScale = (canvasScale + 0.25f).coerceIn(0.4f, 4.0f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = customColors.textMain,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Zoom Out
                    IconButton(
                        onClick = { canvasScale = (canvasScale - 0.25f).coerceIn(0.4f, 4.0f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = customColors.textMain,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Zoom badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
                    ) {
                        Text(
                            text = "${(canvasScale * 100).roundToInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = customColors.textMain,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Step rotate
                    IconButton(
                        onClick = {
                            rotationDeg = ((rotationDeg / 90).roundToInt() * 90 + 90) % 360f
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate 90",
                            tint = customColors.accentSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Toggle paper background
                    IconButton(
                        onClick = { brightPaper = !brightPaper },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (brightPaper) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Paper Style",
                            tint = customColors.accentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Reset 1:1
                    IconButton(
                        onClick = {
                            canvasScale = 1.0f
                            panOffsetX = 0f
                            panOffsetY = 0f
                            rotationDeg = 0f
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset Transform",
                            tint = customColors.accentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Rotation Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Angle: ${rotationDeg.roundToInt()}°",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = customColors.textMuted
                )
                Slider(
                    value = rotationDeg,
                    onValueChange = { rotationDeg = it },
                    valueRange = 0f..360f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = customColors.accentPrimary,
                        activeTrackColor = customColors.accentPrimary
                    )
                )
            }

            // Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, customColors.border, RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                canvasScale = 1.0f
                                panOffsetX = 0f
                                panOffsetY = 0f
                                rotationDeg = 0f
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            canvasScale = (canvasScale * zoom).coerceIn(0.3f, 5.0f)
                            panOffsetX += pan.x
                            panOffsetY += pan.y
                            rotationDeg = (rotationDeg + rotation) % 360f
                        }
                    }
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawBlueprint(
                        result = result,
                        scale = canvasScale,
                        panX = panOffsetX,
                        panY = panOffsetY,
                        rotation = rotationDeg,
                        brightPaper = brightPaper
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBlueprint(
    result: FastenerCalculationResult,
    scale: Float,
    panX: Float,
    panY: Float,
    rotation: Float,
    brightPaper: Boolean
) {
    val w = size.width
    val h = size.height

    val paperBg = if (brightPaper) Color(0xFFFFFFFF) else Color(0xFF0B0F19)
    val gridColor = if (brightPaper) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val fgColor = if (brightPaper) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cyanColor = if (brightPaper) Color(0xFF0284C7) else Color(0xFF22D3EE)
    val amberColor = if (brightPaper) Color(0xFFD97706) else Color(0xFFF59E0B)
    val greenColor = if (brightPaper) Color(0xFF059669) else Color(0xFF10B981)

    // Background
    drawRect(color = paperBg, size = size)

    // Grid lines
    val step = 28f
    var x = 0f
    while (x < w) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        x += step
    }
    var y = 0f
    while (y < h) {
        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        y += step
    }

    // Apply Transformation
    translate(left = w / 2f + panX, top = h / 2f + panY) {
        scale(scale, Offset.Zero) {
            rotate(rotation, Offset.Zero) {

                // Centerline
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
                drawLine(
                    color = Color(0xFF94A3B8),
                    start = Offset(-280f, 0f),
                    end = Offset(280f, 0f),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )

                // Dispatch to specific Fastener Type CAD geometry renderer
                when (result.fastenerType) {
                    FastenerType.SHCS, FastenerType.HEXALOBULAR_SCREW -> drawShcsBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.SOCKET_CSK -> drawCountersunkBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.SOLID_RIVET, FastenerType.BLIND_RIVET -> drawRivetBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.SOCKET_BUTTON -> drawButtonHeadBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.SOCKET_SET -> drawSetScrewBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.FLANGE_BOLT, FastenerType.TTY_HEAD_BOLT -> drawFlangeBoltBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.TWELVE_POINT -> drawTwelvePointBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.CARRIAGE_BOLT -> drawCarriageBoltBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.T_BOLT -> drawTBoltBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.EYE_BOLT -> drawEyeBoltBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.STUD, FastenerType.WHEEL_STUD -> drawStudBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.U_BOLT -> drawUBoltBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.HEX_NUT, FastenerType.HEAVY_HEX_NUT, FastenerType.WELD_NUT, FastenerType.WHEEL_NUT,
                    FastenerType.HI_LOK_COLLAR, FastenerType.NUTPLATE -> drawHexNutBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.FLANGE_NUT -> drawFlangeNutBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.NYLOC_NUT, FastenerType.METAL_LOCK_NUT, FastenerType.AERO_LOCK_NUT -> drawNylocNutBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.CASTLE_NUT -> drawCastleNutBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.ACORN_NUT -> drawAcornNutBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.FLAT_WASHER -> drawFlatWasherBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    FastenerType.SPRING_WASHER -> drawSpringWasherBlueprint(result, fgColor, cyanColor, amberColor, brightPaper)
                    else -> drawHexBoltBlueprint(result, fgColor, cyanColor, amberColor, brightPaper) // Default Hex Bolt
                }

                // Overlay Dimension Callout Text
                val nativeCanvas = drawContext.canvas.nativeCanvas
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 26f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                }

                // Title label
                paint.color = android.graphics.Color.rgb((fgColor.red * 255).toInt(), (fgColor.green * 255).toInt(), (fgColor.blue * 255).toInt())
                nativeCanvas.drawText(
                    "${result.threadLabel} × ${result.lengthMm.toInt()}mm | Sweet-Spot: Ø${result.pitchDiaSweet}mm",
                    -180f,
                    -85f,
                    paint
                )

                // Blank callout
                paint.color = android.graphics.Color.rgb((amberColor.red * 255).toInt(), (amberColor.green * 255).toInt(), (amberColor.blue * 255).toInt())
                val targetText = if (result.isInternalNut) "TAP DRILL: Ø${result.blankTargetDia}mm" else if (result.fastenerType.isWasher) "INNER BORE: Ø${result.nominalDiaMm + 0.5}mm" else "TARGET BLANK: Ø${result.blankTargetDia}mm"
                nativeCanvas.drawText(targetText, 10f, -50f, paint)

                // Tolerance bounds
                paint.color = android.graphics.Color.rgb((greenColor.red * 255).toInt(), (greenColor.green * 255).toInt(), (greenColor.blue * 255).toInt())
                paint.textSize = 22f
                nativeCanvas.drawText(
                    "GO Max: ${result.blankUpperLimit} | NO-GO Min: ${result.blankLowerLimit}",
                    10f,
                    65f,
                    paint
                )
            }
        }
    }
}

// -------------------------------------------------------------
// INDIVIDUAL FASTENER TYPE CAD GEOMETRY DRAWING FUNCTIONS
// -------------------------------------------------------------

private fun DrawScope.drawHexBoltBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -180f
    val headW = 50f
    val headH = 80f
    val shankPx = 100f
    val threadPx = 160f
    val shankH = 50f
    val blankH = 40f

    // Hex Head with 3 visible facets
    drawRect(color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B), topLeft = Offset(startX, -headH / 2f), size = Size(headW, headH))
    drawRect(color = fgColor, topLeft = Offset(startX, -headH / 2f), size = Size(headW, headH), style = Stroke(2.2f))
    drawLine(color = fgColor, start = Offset(startX, -headH / 4f), end = Offset(startX + headW, -headH / 4f), strokeWidth = 1.2f)
    drawLine(color = fgColor, start = Offset(startX, headH / 4f), end = Offset(startX + headW, headH / 4f), strokeWidth = 1.2f)
    drawLine(color = amberColor, start = Offset(startX + headW, -headH / 2f - 4f), end = Offset(startX + headW, headH / 2f + 4f), strokeWidth = 2f)

    // Shank
    val shankX = startX + headW
    drawRect(color = if (brightPaper) Color(0xFFF1F5F9) else Color(0xFF0F172A), topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH))
    drawRect(color = cyanColor, topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH), style = Stroke(2f))

    // Thread V-Teeth
    val threadX = shankX + shankPx
    drawThreadVTeeth(threadX, threadPx, blankH, amberColor, brightPaper)
}

private fun DrawScope.drawShcsBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -180f
    val headW = 60f
    val headH = 85f
    val shankPx = 90f
    val threadPx = 160f
    val shankH = 48f
    val blankH = 38f

    // Cylindrical Socket Head
    drawRect(color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B), topLeft = Offset(startX, -headH / 2f), size = Size(headW, headH))
    drawRect(color = fgColor, topLeft = Offset(startX, -headH / 2f), size = Size(headW, headH), style = Stroke(2.5f))

    // Internal Hex Socket Recess
    val hexW = 32f
    val hexH = 42f
    val hexPath = Path().apply {
        moveTo(startX + 10f, -hexH / 2f)
        lineTo(startX + 10f + hexW, -hexH / 2f)
        lineTo(startX + 10f + hexW + 8f, 0f)
        lineTo(startX + 10f + hexW, hexH / 2f)
        lineTo(startX + 10f, hexH / 2f)
        close()
    }
    drawPath(hexPath, color = cyanColor, style = Stroke(2f))

    // Shank
    val shankX = startX + headW
    drawRect(color = if (brightPaper) Color(0xFFF1F5F9) else Color(0xFF0F172A), topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH))
    drawRect(color = cyanColor, topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH), style = Stroke(2f))

    // Thread V-Teeth
    drawThreadVTeeth(shankX + shankPx, threadPx, blankH, amberColor, brightPaper)
}

private fun DrawScope.drawCountersunkBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -180f
    val headW = 55f
    val headTopH = 90f
    val bodyH = 40f
    val threadPx = 230f

    // Conical 90° Countersunk Head
    val cskPath = Path().apply {
        moveTo(startX, -headTopH / 2f)
        lineTo(startX + headW, -bodyH / 2f)
        lineTo(startX + headW, bodyH / 2f)
        lineTo(startX, headTopH / 2f)
        close()
    }
    drawPath(cskPath, color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B))
    drawPath(cskPath, color = fgColor, style = Stroke(2.2f))

    // Internal Hex Socket on Flat Top
    drawLine(color = cyanColor, start = Offset(startX + 8f, -20f), end = Offset(startX + 28f, -20f), strokeWidth = 2f)
    drawLine(color = cyanColor, start = Offset(startX + 8f, 20f), end = Offset(startX + 28f, 20f), strokeWidth = 2f)
    drawLine(color = cyanColor, start = Offset(startX + 28f, -20f), end = Offset(startX + 28f, 20f), strokeWidth = 2f)

    // Full Thread V-Teeth
    drawThreadVTeeth(startX + headW, threadPx, bodyH, amberColor, brightPaper)
}

private fun DrawScope.drawButtonHeadBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -180f
    val headW = 50f
    val headH = 80f
    val shankPx = 90f
    val threadPx = 160f
    val shankH = 46f

    // Domed Button Head Path
    val domePath = Path().apply {
        moveTo(startX + headW, -shankH / 2f)
        lineTo(startX + headW, -headH / 2f)
        cubicTo(startX, -headH / 2f, startX, headH / 2f, startX + headW, headH / 2f)
        lineTo(startX + headW, shankH / 2f)
        close()
    }
    drawPath(domePath, color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B))
    drawPath(domePath, color = fgColor, style = Stroke(2.2f))

    // Internal Hex Socket
    drawRect(color = cyanColor, topLeft = Offset(startX + 12f, -16f), size = Size(24f, 32f), style = Stroke(1.8f))

    // Shank & Thread
    val shankX = startX + headW
    drawRect(color = if (brightPaper) Color(0xFFF1F5F9) else Color(0xFF0F172A), topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH))
    drawRect(color = cyanColor, topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH), style = Stroke(2f))

    drawThreadVTeeth(shankX + shankPx, threadPx, shankH - 6f, amberColor, brightPaper)
}

/** Rivet: domed head and a plain shank. No socket and no thread. */
private fun DrawScope.drawRivetBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -180f
    val headW = 40f
    val headH = 88f
    val shankPx = 250f
    val shankH = 44f

    val domePath = Path().apply {
        moveTo(startX + headW, -shankH / 2f)
        lineTo(startX + headW, -headH / 2f)
        cubicTo(startX, -headH / 2f, startX, headH / 2f, startX + headW, headH / 2f)
        lineTo(startX + headW, shankH / 2f)
        close()
    }
    drawPath(domePath, color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B))
    drawPath(domePath, color = fgColor, style = Stroke(2.2f))

    val shankX = startX + headW
    drawRect(color = if (brightPaper) Color(0xFFF1F5F9) else Color(0xFF0F172A), topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH))
    drawRect(color = cyanColor, topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH), style = Stroke(2f))
    if (result.fastenerType == FastenerType.BLIND_RIVET) {
        // Mandrel through the sleeve
        drawLine(color = amberColor, start = Offset(startX + 6f, 0f), end = Offset(shankX + shankPx + 40f, 0f), strokeWidth = 6f)
    }
}

private fun DrawScope.drawSetScrewBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -140f
    val threadPx = 260f
    val bodyH = 42f

    // Hex Drive Recess in top end
    drawRect(color = cyanColor, topLeft = Offset(startX, -16f), size = Size(35f, 32f), style = Stroke(2f))

    // Continuous Thread V-Teeth
    drawThreadVTeeth(startX, threadPx, bodyH, amberColor, brightPaper)

    // Flat / Cup Point tip
    val tipPath = Path().apply {
        moveTo(startX + threadPx, -bodyH / 2f)
        lineTo(startX + threadPx + 12f, -bodyH / 4f)
        lineTo(startX + threadPx + 12f, bodyH / 4f)
        lineTo(startX + threadPx, bodyH / 2f)
        close()
    }
    drawPath(tipPath, color = amberColor)
}

private fun DrawScope.drawFlangeBoltBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -190f
    val hexW = 42f
    val hexH = 70f
    val flangeW = 16f
    val flangeH = 100f
    val shankPx = 80f
    val threadPx = 160f
    val shankH = 48f

    // Hex Head
    drawRect(color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B), topLeft = Offset(startX, -hexH / 2f), size = Size(hexW, hexH))
    drawRect(color = fgColor, topLeft = Offset(startX, -hexH / 2f), size = Size(hexW, hexH), style = Stroke(2.2f))

    // Wide Flange Base
    val flangeX = startX + hexW
    val flangePath = Path().apply {
        moveTo(flangeX, -hexH / 2f)
        lineTo(flangeX + flangeW, -flangeH / 2f)
        lineTo(flangeX + flangeW, flangeH / 2f)
        lineTo(flangeX, hexH / 2f)
        close()
    }
    drawPath(flangePath, color = amberColor)
    drawPath(flangePath, color = fgColor, style = Stroke(2f))

    // Shank & Thread
    val shankX = flangeX + flangeW
    drawRect(color = if (brightPaper) Color(0xFFF1F5F9) else Color(0xFF0F172A), topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH))
    drawRect(color = cyanColor, topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH), style = Stroke(2f))

    drawThreadVTeeth(shankX + shankPx, threadPx, shankH - 6f, amberColor, brightPaper)
}

private fun DrawScope.drawTwelvePointBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -180f
    val headW = 50f
    val headH = 75f
    val flangeW = 14f
    val flangeH = 95f
    val shankPx = 80f
    val threadPx = 160f
    val shankH = 48f

    // Stepped 12-point double hex head
    drawRect(color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B), topLeft = Offset(startX, -headH / 2f), size = Size(headW, headH))
    drawRect(color = fgColor, topLeft = Offset(startX, -headH / 2f), size = Size(headW, headH), style = Stroke(2.2f))
    for (i in 1..4) {
        drawLine(color = cyanColor, start = Offset(startX, -headH / 2f + i * (headH / 5)), end = Offset(startX + headW, -headH / 2f + i * (headH / 5)), strokeWidth = 1.2f)
    }

    // Flange Base
    drawRect(color = amberColor, topLeft = Offset(startX + headW, -flangeH / 2f), size = Size(flangeW, flangeH))

    val shankX = startX + headW + flangeW
    drawRect(color = cyanColor, topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH), style = Stroke(2f))
    drawThreadVTeeth(shankX + shankPx, threadPx, shankH - 6f, amberColor, brightPaper)
}

private fun DrawScope.drawCarriageBoltBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -180f
    val headW = 35f
    val headH = 90f
    val squareW = 25f
    val squareH = 50f
    val shankPx = 70f
    val threadPx = 160f
    val shankH = 48f

    // Smooth Round Dome Head
    val domePath = Path().apply {
        moveTo(startX + headW, -headH / 2f)
        cubicTo(startX - 15f, -headH / 2f, startX - 15f, headH / 2f, startX + headW, headH / 2f)
        close()
    }
    drawPath(domePath, color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B))
    drawPath(domePath, color = fgColor, style = Stroke(2.2f))

    // Square Neck Collar under head
    val sqX = startX + headW
    drawRect(color = amberColor, topLeft = Offset(sqX, -squareH / 2f), size = Size(squareW, squareH))
    drawRect(color = fgColor, topLeft = Offset(sqX, -squareH / 2f), size = Size(squareW, squareH), style = Stroke(2f))

    // Shank & Thread
    val shankX = sqX + squareW
    drawRect(color = cyanColor, topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH), style = Stroke(2f))
    drawThreadVTeeth(shankX + shankPx, threadPx, shankH - 6f, amberColor, brightPaper)
}

private fun DrawScope.drawTBoltBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -180f
    val headW = 30f
    val headH = 110f
    val shankPx = 80f
    val threadPx = 170f
    val shankH = 48f

    // Rectangular T-Head
    drawRect(color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B), topLeft = Offset(startX, -headH / 2f), size = Size(headW, headH))
    drawRect(color = fgColor, topLeft = Offset(startX, -headH / 2f), size = Size(headW, headH), style = Stroke(2.2f))

    val shankX = startX + headW
    drawRect(color = cyanColor, topLeft = Offset(shankX, -shankH / 2f), size = Size(shankPx, shankH), style = Stroke(2f))
    drawThreadVTeeth(shankX + shankPx, threadPx, shankH - 6f, amberColor, brightPaper)
}

private fun DrawScope.drawEyeBoltBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -160f
    val eyeRadius = 45f
    val eyeHoleRadius = 22f
    val shoulderW = 18f
    val shoulderH = 55f
    val threadPx = 200f
    val threadH = 44f

    // Outer Eye Ring
    drawCircle(color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B), radius = eyeRadius, center = Offset(startX + eyeRadius, 0f))
    drawCircle(color = fgColor, radius = eyeRadius, center = Offset(startX + eyeRadius, 0f), style = Stroke(2.5f))

    // Inner Eye Hole
    drawCircle(color = if (brightPaper) Color(0xFFFFFFFF) else Color(0xFF0B0F19), radius = eyeHoleRadius, center = Offset(startX + eyeRadius, 0f))
    drawCircle(color = cyanColor, radius = eyeHoleRadius, center = Offset(startX + eyeRadius, 0f), style = Stroke(2f))

    // Shoulder Collar
    val shoulderX = startX + eyeRadius * 2f
    drawRect(color = amberColor, topLeft = Offset(shoulderX, -shoulderH / 2f), size = Size(shoulderW, shoulderH))

    // Threaded Shank
    val threadX = shoulderX + shoulderW
    drawThreadVTeeth(threadX, threadPx, threadH, amberColor, brightPaper)
}

private fun DrawScope.drawStudBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val startX = -160f
    val tapThreadPx = 80f
    val middleShankPx = 80f
    val nutThreadPx = 140f
    val bodyH = 44f

    // Tap-End Thread
    drawThreadVTeeth(startX, tapThreadPx, bodyH, amberColor, brightPaper)

    // Unthreaded Middle Collar
    val shankX = startX + tapThreadPx
    drawRect(color = if (brightPaper) Color(0xFFF1F5F9) else Color(0xFF0F172A), topLeft = Offset(shankX, -bodyH / 2f), size = Size(middleShankPx, bodyH))
    drawRect(color = cyanColor, topLeft = Offset(shankX, -bodyH / 2f), size = Size(middleShankPx, bodyH), style = Stroke(2f))

    // Nut-End Thread
    val nutThreadX = shankX + middleShankPx
    drawThreadVTeeth(nutThreadX, nutThreadPx, bodyH, amberColor, brightPaper)
}

private fun DrawScope.drawUBoltBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val uRadius = 45f
    val legLength = 160f
    val threadLength = 80f
    val rodThickness = 16f

    // Arch at top
    val uPath = Path().apply {
        moveTo(-uRadius, 40f)
        lineTo(-uRadius, -40f)
        cubicTo(-uRadius, -40f - uRadius, uRadius, -40f - uRadius, uRadius, -40f)
        lineTo(uRadius, 40f)
    }
    drawPath(uPath, color = cyanColor, style = Stroke(rodThickness))

    // Left & Right Leg Threads
    drawRect(color = amberColor, topLeft = Offset(-uRadius - rodThickness / 2f, legLength - threadLength), size = Size(rodThickness, threadLength))
    drawRect(color = amberColor, topLeft = Offset(uRadius - rodThickness / 2f, legLength - threadLength), size = Size(rodThickness, threadLength))
}

private fun DrawScope.drawHexNutBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val nutW = 100f
    val nutH = 85f
    val nutX = -nutW / 2f
    val boreH = 46f

    // Hexagon Nut Body
    drawRect(color = if (brightPaper) Color(0xFFE2E8F0) else Color(0xFF1E293B), topLeft = Offset(nutX, -nutH / 2f), size = Size(nutW, nutH))
    drawRect(color = cyanColor, topLeft = Offset(nutX, -nutH / 2f), size = Size(nutW, nutH), style = Stroke(2.2f))
    drawLine(color = cyanColor, start = Offset(nutX, -nutH / 3f), end = Offset(nutX + nutW, -nutH / 3f), strokeWidth = 1.2f)
    drawLine(color = cyanColor, start = Offset(nutX, nutH / 3f), end = Offset(nutX + nutW, nutH / 3f), strokeWidth = 1.2f)

    // Center Threaded Bore
    drawRect(color = if (brightPaper) Color(0xFFFEF3C7) else Color(0xFF451A03), topLeft = Offset(nutX, -boreH / 2f), size = Size(nutW, boreH))
    drawRect(color = amberColor, topLeft = Offset(nutX, -boreH / 2f), size = Size(nutW, boreH), style = Stroke(2f))

    // Thread V-Teeth lines
    var tx = nutX + 8f
    while (tx < nutX + nutW - 8f) {
        drawLine(color = amberColor, start = Offset(tx, -boreH / 2f), end = Offset(tx + 6f, boreH / 2f), strokeWidth = 1.5f)
        tx += 12f
    }
}

private fun DrawScope.drawFlangeNutBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val nutW = 90f
    val nutH = 75f
    val flangeW = 120f
    val flangeH = 14f
    val nutX = -nutW / 2f
    val boreH = 44f

    // Main Nut Body
    drawRect(color = if (brightPaper) Color(0xFFE2E8F0) else Color(0xFF1E293B), topLeft = Offset(nutX, -nutH / 2f), size = Size(nutW, nutH))
    drawRect(color = cyanColor, topLeft = Offset(nutX, -nutH / 2f), size = Size(nutW, nutH), style = Stroke(2.2f))

    // Flange Base
    drawRect(color = amberColor, topLeft = Offset(-flangeW / 2f, nutH / 2f - flangeH), size = Size(flangeW, flangeH))

    // Thread Bore
    drawRect(color = amberColor, topLeft = Offset(-flangeW / 2f + 10f, -boreH / 2f), size = Size(flangeW - 20f, boreH), style = Stroke(1.8f))
}

private fun DrawScope.drawNylocNutBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val nutW = 100f
    val nutH = 75f
    val crownW = 70f
    val crownH = 22f
    val nutX = -nutW / 2f
    val boreH = 46f

    // Hexagon Nut Body
    drawRect(color = if (brightPaper) Color(0xFFE2E8F0) else Color(0xFF1E293B), topLeft = Offset(nutX, -nutH / 2f), size = Size(nutW, nutH))
    drawRect(color = cyanColor, topLeft = Offset(nutX, -nutH / 2f), size = Size(nutW, nutH), style = Stroke(2.2f))

    // Nylon Locking Crown Top Ring
    drawRect(color = Color(0xFF0284C7), topLeft = Offset(-crownW / 2f, -nutH / 2f - crownH), size = Size(crownW, crownH))
    drawRect(color = fgColor, topLeft = Offset(-crownW / 2f, -nutH / 2f - crownH), size = Size(crownW, crownH), style = Stroke(1.8f))

    // Thread Bore
    drawRect(color = amberColor, topLeft = Offset(nutX, -boreH / 2f), size = Size(nutW, boreH), style = Stroke(1.8f))
}

private fun DrawScope.drawCastleNutBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val nutW = 100f
    val nutH = 70f
    val crownH = 25f
    val nutX = -nutW / 2f
    val boreH = 44f

    // Main Nut Body
    drawRect(color = if (brightPaper) Color(0xFFE2E8F0) else Color(0xFF1E293B), topLeft = Offset(nutX, -nutH / 2f), size = Size(nutW, nutH))
    drawRect(color = cyanColor, topLeft = Offset(nutX, -nutH / 2f), size = Size(nutW, nutH), style = Stroke(2.2f))

    // Castellated crown slots
    for (i in 0..2) {
        val slotX = nutX + 10f + i * 30f
        drawRect(color = amberColor, topLeft = Offset(slotX, -nutH / 2f - crownH), size = Size(18f, crownH))
    }

    // Thread Bore
    drawRect(color = amberColor, topLeft = Offset(nutX, -boreH / 2f), size = Size(nutW, boreH), style = Stroke(1.8f))
}

private fun DrawScope.drawAcornNutBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val nutW = 95f
    val nutH = 65f
    val domeRadius = 45f
    val nutX = -nutW / 2f
    val boreH = 42f

    // Hexagon Body
    drawRect(color = if (brightPaper) Color(0xFFE2E8F0) else Color(0xFF1E293B), topLeft = Offset(nutX, -nutH / 2f), size = Size(nutW, nutH))
    drawRect(color = cyanColor, topLeft = Offset(nutX, -nutH / 2f), size = Size(nutW, nutH), style = Stroke(2.2f))

    // Closed Dome Cap Top
    val domePath = Path().apply {
        moveTo(-domeRadius, -nutH / 2f)
        cubicTo(-domeRadius, -nutH / 2f - domeRadius, domeRadius, -nutH / 2f - domeRadius, domeRadius, -nutH / 2f)
        close()
    }
    drawPath(domePath, color = amberColor)
    drawPath(domePath, color = fgColor, style = Stroke(2f))

    // Thread Bore
    drawRect(color = amberColor, topLeft = Offset(nutX, -boreH / 2f), size = Size(nutW, boreH), style = Stroke(1.8f))
}

private fun DrawScope.drawFlatWasherBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val outerH = 120f
    val innerH = 50f
    val thicknessW = 28f
    val washerX = -thicknessW / 2f

    // Top Section
    drawRect(color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B), topLeft = Offset(washerX, -outerH / 2f), size = Size(thicknessW, (outerH - innerH) / 2f))
    drawRect(color = cyanColor, topLeft = Offset(washerX, -outerH / 2f), size = Size(thicknessW, (outerH - innerH) / 2f), style = Stroke(2.2f))

    // Bottom Section
    drawRect(color = if (brightPaper) Color(0xFFCBD5E1) else Color(0xFF1E293B), topLeft = Offset(washerX, innerH / 2f), size = Size(thicknessW, (outerH - innerH) / 2f))
    drawRect(color = cyanColor, topLeft = Offset(washerX, innerH / 2f), size = Size(thicknessW, (outerH - innerH) / 2f), style = Stroke(2.2f))

    // Inner Hole Centerline
    drawLine(color = amberColor, start = Offset(-60f, 0f), end = Offset(60f, 0f), strokeWidth = 1.5f)
}

private fun DrawScope.drawSpringWasherBlueprint(
    result: FastenerCalculationResult,
    fgColor: Color, cyanColor: Color, amberColor: Color, brightPaper: Boolean
) {
    val outerH = 110f
    val innerH = 48f
    val thicknessW = 32f
    val washerX = -thicknessW / 2f

    // Split Washer Sections with offset gaps
    drawRect(color = amberColor, topLeft = Offset(washerX - 6f, -outerH / 2f), size = Size(thicknessW, (outerH - innerH) / 2f))
    drawRect(color = cyanColor, topLeft = Offset(washerX + 6f, innerH / 2f), size = Size(thicknessW, (outerH - innerH) / 2f))
}

private fun DrawScope.drawThreadVTeeth(
    startX: Float, threadPx: Float, bodyH: Float, amberColor: Color, brightPaper: Boolean
) {
    val blankBg = if (brightPaper) Color(0xFFFEF3C7) else Color(0xFF451A03)
    drawRect(color = blankBg, topLeft = Offset(startX, -bodyH / 2f), size = Size(threadPx, bodyH))
    drawRect(color = amberColor, topLeft = Offset(startX, -bodyH / 2f), size = Size(threadPx, bodyH), style = Stroke(2.2f))

    // Thread V-Teeth Serrations
    val toothStep = 10f
    var tx = startX + 4f
    while (tx < startX + threadPx - 4f) {
        drawLine(
            color = amberColor.copy(alpha = 0.8f),
            start = Offset(tx, -bodyH / 2f),
            end = Offset(tx + 6f, bodyH / 2f),
            strokeWidth = 1.6f
        )
        tx += toothStep
    }

    // Lead chamfer
    val chamferPath = Path().apply {
        moveTo(startX + threadPx, -bodyH / 2f)
        lineTo(startX + threadPx + 8f, 0f)
        lineTo(startX + threadPx, bodyH / 2f)
        close()
    }
    drawPath(chamferPath, color = amberColor)
}

@Composable
fun Interactive3dFastenerView(
    result: FastenerCalculationResult,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    var rotX by remember { mutableFloatStateOf(-20f) }
    var rotY by remember { mutableFloatStateOf(35f) }
    var scaleFactor by remember { mutableFloatStateOf(1.0f) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F172A), // Dark CAD Metallic Viewport
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        rotY += pan.x * 0.4f
                        rotX = (rotX - pan.y * 0.4f).coerceIn(-85f, 85f)
                        scaleFactor = (scaleFactor * zoom).coerceIn(0.5f, 2.5f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // 3D Matrix Projection Helpers
                val radX = Math.toRadians(rotX.toDouble())
                val radY = Math.toRadians(rotY.toDouble())

                fun project3D(x: Float, y: Float, z: Float): Offset {
                    // Rotate Y (yaw)
                    val x1 = x * Math.cos(radY) + z * Math.sin(radY)
                    val z1 = -x * Math.sin(radY) + z * Math.cos(radY)

                    // Rotate X (pitch)
                    val y2 = y * Math.cos(radX) - z1 * Math.sin(radX)
                    val z2 = y * Math.sin(radX) + z1 * Math.cos(radX)

                    // Perspective Factor
                    val fov = 400.0
                    val distance = 350.0
                    val scale = (fov / (fov + z2 + distance)).toFloat() * scaleFactor

                    return Offset(
                        cx + (x1 * scale).toFloat(),
                        cy + (y2 * scale).toFloat()
                    )
                }

                // Render 3D Fastener Model Geometry
                val d = (result.nominalDiaMm * 4.5).toFloat()
                val l = (result.lengthMm * 1.8).toFloat().coerceIn(80f, 220f)
                val headW = d * 1.6f
                val headH = d * 0.7f

                // 1. Render Hex Head (6-sided 3D Prism)
                val headCenterZ = -l / 2f - headH / 2f
                val hexRadius = headW / 2f
                val numSides = 6

                for (s in 0 until numSides) {
                    val angle1 = Math.toRadians((s * 60.0))
                    val angle2 = Math.toRadians(((s + 1) * 60.0))

                    val x1 = (hexRadius * Math.cos(angle1)).toFloat()
                    val y1 = (hexRadius * Math.sin(angle1)).toFloat()
                    val x2 = (hexRadius * Math.cos(angle2)).toFloat()
                    val y2 = (hexRadius * Math.sin(angle2)).toFloat()

                    val zFront = headCenterZ + headH / 2f
                    val zBack = headCenterZ - headH / 2f

                    val p1 = project3D(x1, y1, zBack)
                    val p2 = project3D(x2, y2, zBack)
                    val p3 = project3D(x2, y2, zFront)
                    val p4 = project3D(x1, y1, zFront)

                    // Directional lighting
                    val normalY = (Math.sin(angle1) + Math.sin(angle2)) / 2.0
                    val lightIntensity = (0.3 + 0.7 * Math.max(0.0, normalY)).toFloat()
                    val faceColor = Color(
                        red = (0.2f + 0.6f * lightIntensity).coerceIn(0f, 1f),
                        green = (0.5f + 0.4f * lightIntensity).coerceIn(0f, 1f),
                        blue = (0.8f + 0.2f * lightIntensity).coerceIn(0f, 1f)
                    )

                    val facePath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        lineTo(p4.x, p4.y)
                        close()
                    }
                    drawPath(facePath, color = faceColor)
                    drawPath(facePath, color = Color(0xFF38BDF8), style = Stroke(1.5f))
                }

                // 2. Render Cylindrical Shank
                val shankRadius = d / 2f
                val cylinderSteps = 16

                for (c in 0 until cylinderSteps) {
                    val a1 = Math.toRadians((c * (360.0 / cylinderSteps)))
                    val a2 = Math.toRadians(((c + 1) * (360.0 / cylinderSteps)))

                    val x1 = (shankRadius * Math.cos(a1)).toFloat()
                    val y1 = (shankRadius * Math.sin(a1)).toFloat()
                    val x2 = (shankRadius * Math.cos(a2)).toFloat()
                    val y2 = (shankRadius * Math.sin(a2)).toFloat()

                    val zBack = -l / 2f
                    val zFront = l / 2f

                    val p1 = project3D(x1, y1, zBack)
                    val p2 = project3D(x2, y2, zBack)
                    val p3 = project3D(x2, y2, zFront)
                    val p4 = project3D(x1, y1, zFront)

                    val normalY = (Math.sin(a1) + Math.sin(a2)) / 2.0
                    val lightIntensity = (0.2 + 0.8 * Math.max(0.0, normalY)).toFloat()
                    val faceColor = Color(
                        red = (0.15f + 0.4f * lightIntensity).coerceIn(0f, 1f),
                        green = (0.35f + 0.5f * lightIntensity).coerceIn(0f, 1f),
                        blue = (0.65f + 0.3f * lightIntensity).coerceIn(0f, 1f)
                    )

                    val cylinderPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        lineTo(p4.x, p4.y)
                        close()
                    }
                    drawPath(cylinderPath, color = faceColor)
                    drawPath(cylinderPath, color = Color(0xFF0EA5E9).copy(alpha = 0.5f), style = Stroke(1.0f))
                }

                // 3. Spiral Thread Helix
                val pitchPx = (result.pitchMm * 6.0).toFloat().coerceIn(12f, 35f)
                val threadLengthPx = (result.threadLengthMm * 1.8).toFloat().coerceIn(40f, l)
                val threadStartZ = l / 2f - threadLengthPx
                var tz = threadStartZ

                while (tz < l / 2f) {
                    val spiralPath = Path()
                    var firstPoint = true
                    for (step in 0..16) {
                        val angle = Math.toRadians(step * 22.5)
                        val rx = ((shankRadius + 2f) * Math.cos(angle)).toFloat()
                        val ry = ((shankRadius + 2f) * Math.sin(angle)).toFloat()
                        val rz = tz + (step / 16f) * pitchPx
                        val pt = project3D(rx, ry, rz)

                        if (firstPoint) {
                            spiralPath.moveTo(pt.x, pt.y)
                            firstPoint = false
                        } else {
                            spiralPath.lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(spiralPath, color = Color(0xFFF59E0B), style = Stroke(2.5f))
                    tz += pitchPx
                }
            }

            // Overlay Instruction Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = "Rotate",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "3D CAD Model • Drag finger to 360° orbit rotate • Pinch to zoom",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
