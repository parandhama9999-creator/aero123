package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CoatingType
import com.example.data.ExtractedFastenerSpec
import com.example.data.FastenerCalculationResult
import com.example.data.FastenerCatalog
import com.example.data.FastenerType
import com.example.data.ProductStandard
import com.example.data.PropertyClass
import com.example.data.SelectionFilter
import com.example.data.ThreadClassCatalog
import com.example.data.UnitSystem
import com.example.ui.components.BlueprintCanvasView
import com.example.ui.components.GaugeSimulatorView
import com.example.ui.components.MaterialCatalogDialog
import com.example.util.CadDxfGenerator
import com.example.util.InspectionReportGenerator
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Verified
import com.example.ui.components.PokaYokePanel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LocalCustomColors
import com.example.ui.theme.SuccessGreen

@Composable
fun FastenerSizingScreen(
    result: FastenerCalculationResult,
    selectedFastenerType: FastenerType,
    onFastenerTypeSelected: (FastenerType) -> Unit,
    selectedUnitSystem: UnitSystem,
    onUnitSystemSelected: (UnitSystem) -> Unit,
    selectedStandard: ProductStandard,
    onStandardSelected: (ProductStandard) -> Unit,
    selectedSizeIndex: Int,
    onSizeIndexSelected: (Int) -> Unit,
    selectedPitchValue: Double,
    onPitchSelected: (Double) -> Unit,
    selectedLength: Double,
    onLengthSelected: (Double) -> Unit,
    selectedPropClass: PropertyClass,
    onPropClassSelected: (PropertyClass) -> Unit,
    selectedMaterialKey: String,
    onMaterialSelected: (String) -> Unit,
    selectedThreadClass: String,
    onThreadClassSelected: (String) -> Unit,
    selectedCoating: CoatingType,
    onCoatingSelected: (CoatingType) -> Unit,
    batchQty: Int,
    onBatchQtyChanged: (Int) -> Unit,
    appliedExtractedSpec: ExtractedFastenerSpec? = null,
    onClearCustomScan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val scrollState = rememberScrollState()
    var showMaterialDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Strict poka-yoke gate: every rule checked against the governing standards
            PokaYokePanel(issues = result.pokaYokeIssues)

        // Quick CAD & MTC Export Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = customColors.bgElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentPrimary.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f).clickable {
                    CadDxfGenerator.exportAndShareCadDxf(context, result)
                }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Architecture,
                        contentDescription = "Export DXF",
                        tint = customColors.accentPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Export CAD DXF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = customColors.accentPrimary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = customColors.bgElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentSecondary.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f).clickable {
                    InspectionReportGenerator.exportAndShareReport(context, result)
                }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Export MTC",
                        tint = customColors.accentSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Export MTC Report",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = customColors.accentSecondary
                    )
                }
            }
        }

        // Poka-Yoke Selector Card
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
                            Icons.Default.Tune,
                            contentDescription = "Selectors",
                            tint = customColors.accentPrimary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "POKA-YOKE FASTENER SELECTOR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.textMain
                        )
                    }

                    // Copy Shop Spec
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                        modifier = Modifier.clickable {
                            val spec = "${result.threadLabel} × ${result.lengthMm.toInt()}mm (${result.material.name}) | " +
                                    "Blank: Ø${result.blankTargetDia}mm (GO: ${result.blankUpperLimit}, NO-GO: ${result.blankLowerLimit}) | " +
                                    "Sweet-Spot: Ø${result.pitchDiaSweet}mm"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Shop Spec", spec))
                            Toast.makeText(context, "Copied Shop Spec to Clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = customColors.accentPrimary,
                                modifier = Modifier.padding(end = 4.dp).height(14.dp)
                            )
                            Text(
                                text = "Copy Spec",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMain
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Selectors Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 1: Fastener Type & Unit System
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectorDropdown(
                            label = "1. Fastener Type",
                            selectedText = selectedFastenerType.label,
                            items = FastenerType.values().map { it.label },
                            onItemSelected = { onFastenerTypeSelected(FastenerType.values()[it]) },
                            modifier = Modifier.weight(1.3f)
                        )
                        SelectorDropdown(
                            label = "2. Unit System",
                            selectedText = selectedUnitSystem.label,
                            items = UnitSystem.values().map { it.label },
                            onItemSelected = { onUnitSystemSelected(UnitSystem.values()[it]) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Standard & Nominal Size
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val validStandards = SelectionFilter.standards(selectedFastenerType, selectedUnitSystem)
                        SelectorDropdown(
                            label = "3. Product Standard",
                            selectedText = selectedStandard.label,
                            items = validStandards.map { it.label },
                            onItemSelected = { onStandardSelected(validStandards[it]) },
                            modifier = Modifier.weight(1.2f)
                        )

                        val sizeLabels = if (selectedUnitSystem == UnitSystem.METRIC) {
                            FastenerCatalog.METRIC_SIZES.map { it.label }
                        } else {
                            FastenerCatalog.INCH_SIZES.map { it.label }
                        }
                        val currentSizeLabel = if (appliedExtractedSpec != null) {
                            "Custom Ø${appliedExtractedSpec.nominalDiaMm} mm (Scan)"
                        } else {
                            sizeLabels.getOrElse(selectedSizeIndex) { sizeLabels.first() }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            SelectorDropdown(
                                label = "4. Nominal Size (d)",
                                selectedText = currentSizeLabel,
                                items = sizeLabels,
                                onItemSelected = { onSizeIndexSelected(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (appliedExtractedSpec != null) {
                                Surface(
                                    color = customColors.accentSecondary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentSecondary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable { onClearCustomScan() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Custom Ø${appliedExtractedSpec.nominalDiaMm}mm",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = customColors.accentSecondary
                                        )
                                        Text(
                                            text = "× Reset to Standard",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = customColors.accentPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Row 3: Pitch & Length
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val threaded = SelectionFilter.hasThread(selectedFastenerType)
                        val pitchList = if (!threaded) {
                            listOf(NOT_APPLICABLE_THREAD)
                        } else if (selectedUnitSystem == UnitSystem.METRIC) {
                            val metric = FastenerCatalog.METRIC_SIZES.getOrElse(selectedSizeIndex) { FastenerCatalog.METRIC_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.METRIC)] }
                            metric.pitches.mapIndexed { idx, p -> "$p mm (${if (idx == 0) "Coarse" else "Fine"})" }
                        } else {
                            val inch = FastenerCatalog.INCH_SIZES.getOrElse(selectedSizeIndex) { FastenerCatalog.INCH_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.INCH)] }
                            inch.tpis.mapIndexed { idx, tpi ->
                                val series = when {
                                    idx == 0 && inch.hasUnc -> "UNC"
                                    idx == 0 || (idx == 1 && inch.hasUnc) -> "UNF"
                                    else -> "UNS"
                                }
                                "$tpi TPI ($series)"
                            }
                        }
                        val currentPitchText = if (!threaded) {
                            NOT_APPLICABLE_THREAD
                        } else if (selectedUnitSystem == UnitSystem.METRIC) {
                            "$selectedPitchValue mm"
                        } else {
                            "${selectedPitchValue.toInt()} TPI"
                        }

                        SelectorDropdown(
                            label = if (selectedUnitSystem == UnitSystem.METRIC) "5. Pitch (mm)" else "5. Pitch (TPI)",
                            selectedText = currentPitchText,
                            items = pitchList,
                            onItemSelected = { idx ->
                                if (!threaded) {
                                    Unit
                                } else if (selectedUnitSystem == UnitSystem.METRIC) {
                                    val metric = FastenerCatalog.METRIC_SIZES.getOrElse(selectedSizeIndex) { FastenerCatalog.METRIC_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.METRIC)] }
                                    onPitchSelected(metric.pitches[idx])
                                } else {
                                    val inch = FastenerCatalog.INCH_SIZES.getOrElse(selectedSizeIndex) { FastenerCatalog.INCH_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.INCH)] }
                                    onPitchSelected(inch.tpis[idx].toDouble())
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        val lengthOptions = if (selectedUnitSystem == UnitSystem.METRIC) {
                            FastenerCatalog.METRIC_LENGTHS.map { "${it.toInt()} mm" }
                        } else {
                            FastenerCatalog.INCH_LENGTHS.map { "${(it / 25.4 * 10).toInt() / 10.0}\" (${it.toInt()}mm)" }
                        }

                        SelectorDropdown(
                            label = "6. Length (L)",
                            selectedText = "${selectedLength.toInt()} mm",
                            items = lengthOptions,
                            onItemSelected = { idx ->
                                if (selectedUnitSystem == UnitSystem.METRIC) {
                                    onLengthSelected(FastenerCatalog.METRIC_LENGTHS[idx])
                                } else {
                                    onLengthSelected(FastenerCatalog.INCH_LENGTHS[idx])
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 4: Property Class & Material Alloy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val materialsList = FastenerCatalog.MATERIALS.values.toList()
                        val currentMat = FastenerCatalog.MATERIALS[selectedMaterialKey] ?: materialsList[2]
                        val graded = SelectionFilter.usesPropertyClass(selectedFastenerType)
                        val validPropClasses = SelectionFilter.propertyClasses(selectedFastenerType, selectedUnitSystem, result.nominalDiaMm)
                        SelectorDropdown(
                            label = "7. Property Class",
                            selectedText = if (graded) selectedPropClass.label else NOT_APPLICABLE_WASHER,
                            items = if (graded) validPropClasses.map { it.label } else listOf(NOT_APPLICABLE_WASHER),
                            onItemSelected = {
                                if (graded) {
                                    val pc = validPropClasses[it]
                                    onPropClassSelected(pc)
                                    onMaterialSelected(SelectionFilter.materialForClass(selectedFastenerType, pc, result.nominalDiaMm, selectedMaterialKey, FastenerCatalog.MATERIALS))
                                }
                            },
                            modifier = Modifier.weight(1.1f)
                        )

                        Column(modifier = Modifier.weight(1.2f)) {
                            val filteredMaterials = SelectionFilter.materials(selectedFastenerType, selectedPropClass, result.nominalDiaMm, FastenerCatalog.MATERIALS.values)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "8. Material (k_m)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = customColors.accentSecondary
                                )
                                Text(
                                    text = "+ Add Alloy",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = customColors.accentPrimary,
                                    modifier = Modifier
                                        .clickable { showMaterialDialog = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            SelectorDropdown(
                                label = "",
                                selectedText = "${currentMat.name} (km=${currentMat.km})",
                                items = filteredMaterials.map { "${it.name} (k_m=${it.km})" },
                                onItemSelected = { idx ->
                                    onMaterialSelected(filteredMaterials[idx].key)
                                }
                            )
                        }
                    }

                    if (showMaterialDialog) {
                        MaterialCatalogDialog(
                            selectedMaterialKey = selectedMaterialKey,
                            onMaterialSelected = { key ->
                                onMaterialSelected(key)
                            },
                            onDismissRequest = { showMaterialDialog = false }
                        )
                    }

                    // Row 5: Thread Class & Coating / Batch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val threadedPart = SelectionFilter.hasThread(selectedFastenerType)
                        val threadClasses = SelectionFilter.threadClasses(selectedUnitSystem, selectedFastenerType, selectedStandard)
                        val threadClassLabels = if (threadedPart) threadClasses.map { ThreadClassCatalog.getLabel(it) } else listOf(NOT_APPLICABLE_THREAD)
                        val materialForCoating = FastenerCatalog.MATERIALS[selectedMaterialKey] ?: FastenerCatalog.MATERIALS.values.toList()[2]
                        val validCoatings = SelectionFilter.coatings(selectedFastenerType, materialForCoating, selectedPropClass, result.nominalDiaMm, selectedThreadClass)

                        SelectorDropdown(
                            label = "9. Thread Class",
                            selectedText = if (threadedPart) ThreadClassCatalog.getLabel(selectedThreadClass) else NOT_APPLICABLE_THREAD,
                            items = threadClassLabels,
                            onItemSelected = { if (threadedPart) onThreadClassSelected(threadClasses[it]) },
                            modifier = Modifier.weight(1.0f)
                        )
                        SelectorDropdown(
                            label = "10. Coating",
                            selectedText = selectedCoating.label,
                            items = validCoatings.map { it.label },
                            onItemSelected = { onCoatingSelected(validCoatings[it]) },
                            modifier = Modifier.weight(1.2f)
                        )

                        // Batch Qty
                        Column(modifier = Modifier.weight(0.9f)) {
                            Text(
                                text = "Batch Qty",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMuted
                            )
                            OutlinedTextField(
                                value = batchQty.toString(),
                                onValueChange = { str ->
                                    val parsed = str.toIntOrNull()
                                    if (parsed != null && parsed > 0) onBatchQtyChanged(parsed)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = customColors.textMain
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = customColors.accentPrimary,
                                    unfocusedBorderColor = customColors.border
                                ),
                                modifier = Modifier.height(48.dp).fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Thread length b (standard minimum) and plain shank for bolts and screws
        if (SelectionFilter.hasThread(selectedFastenerType) && !selectedFastenerType.isNut && !selectedFastenerType.isStud) {
            val dMm = result.nominalDiaMm
            val pitchMm = if (selectedUnitSystem == UnitSystem.METRIC) selectedPitchValue else 25.4 / selectedPitchValue
            val bMin = com.example.data.PokaYokeEngine.threadLengthB(selectedFastenerType, selectedUnitSystem, dMm, selectedLength)
            val fullThread = com.example.data.PokaYokeEngine.isFullyThreaded(selectedFastenerType) || bMin + 2 * pitchMm >= selectedLength
            val bRule = when {
                selectedUnitSystem == UnitSystem.INCH -> if (selectedLength <= 152.4) "2D + 1/4\" (L ≤ 6\")" else "2D + 1/2\" (L > 6\")"
                selectedFastenerType == FastenerType.SHCS || selectedFastenerType == FastenerType.HEXALOBULAR_SCREW -> "2d + 12 (ISO 4762)"
                selectedLength <= 125.0 -> "2d + 6 (L ≤ 125)"
                selectedLength <= 200.0 -> "2d + 12 (125 < L ≤ 200)"
                else -> "2d + 25 (L > 200)"
            }
            fun mm(v: Double) = "%.1f".format(java.util.Locale.US, v)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = customColors.bgCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "THREAD & SHANK LENGTH (L = ${mm(selectedLength)} mm)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = customColors.accentSecondary)
                    if (fullThread) {
                        Text(text = "Full thread: ${mm(selectedLength)} mm (no plain shank)", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = customColors.textMain)
                        if (!com.example.data.PokaYokeEngine.isFullyThreaded(selectedFastenerType)) {
                            Text(text = "L ≤ b + 2P, so this length is made fully threaded (e.g. ISO 4017 instead of ISO 4014).", fontSize = 10.sp, color = customColors.textMuted)
                        }
                    } else {
                        Text(text = "Thread length b : min ${mm(bMin)}  max ${mm(bMin + 2 * pitchMm)} mm", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = customColors.textMain)
                        Text(text = "Plain shank     : min ${mm(selectedLength - bMin - 2 * pitchMm)}  max ${mm(selectedLength - bMin)} mm", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = customColors.textMain)
                        Text(text = "b = $bRule = ${mm(bMin)} mm min; max adds 2P (${mm(2 * pitchMm)} mm). Shank + thread = L.", fontSize = 10.sp, color = customColors.textMuted)

                        // Forward extrusion of the thread section: Ø shank (wire) → Ø rolling blank through a 15°/side (30° included) cone.
                        // Volume is conserved, so the stock length before extrusion is the reduced volume over the shank area.
                        val ds = result.wireDiaMm
                        val db = result.blankTargetDia
                        if (ds > db) {
                            val coneLen = (ds - db) / 2.0 / kotlin.math.tan(Math.toRadians(15.0))
                            val coneVol = Math.PI * coneLen / 12.0 * (ds * ds + ds * db + db * db)
                            val aShank = Math.PI / 4.0 * ds * ds
                            val aBlank = Math.PI / 4.0 * db * db
                            fun preLen(bLen: Double) = (aBlank * bLen + coneVol) / aShank
                            fun underHeadBefore(bLen: Double) = selectedLength - bLen - coneLen + preLen(bLen)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "BEFORE EXTRUSION (Ø${mm(ds)} → Ø${"%.3f".format(java.util.Locale.US, db)}, 15°/side)", fontSize = 11.sp, fontWeight = FontWeight.Black, color = customColors.accentSecondary)
                            Text(text = "Cone length     : ${"%.2f".format(java.util.Locale.US, coneLen)} mm", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = customColors.textMain)
                            Text(text = "Stock to extrude: ${"%.2f".format(java.util.Locale.US, preLen(bMin))} – ${"%.2f".format(java.util.Locale.US, preLen(bMin + 2 * pitchMm))} mm", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = customColors.textMain)
                            Text(text = "Length under head before extrusion: ${"%.2f".format(java.util.Locale.US, underHeadBefore(bMin))} – ${"%.2f".format(java.util.Locale.US, underHeadBefore(bMin + 2 * pitchMm))} mm", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = customColors.textMain)
                            Text(text = "Extruded section = Ø blank × b plus the 30° cone; plain shank stays at Ø${mm(ds)}. Range is for b min → b + 2P. Volume constant: L_pre = (A_blank·b + V_cone) / A_shank.", fontSize = 10.sp, color = customColors.textMuted)

                            // Head stock: forged round head (with trim cheese) as a length of shank wire
                            val headDims = result.headDimensions
                            val isHex = selectedFastenerType in setOf(FastenerType.HEX_BOLT, FastenerType.HEX_BOLT_FULL, FastenerType.HEAVY_HEX_BOLT)
                            val isFlange = selectedFastenerType == FastenerType.FLANGE_BOLT
                            if (headDims != null && (isHex || isFlange)) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "HEAD STOCK & TRIM CHEESE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = customColors.accentSecondary)
                                var headForgedVol = 0.0
                                var cheeseVol = 0.0
                                if (isHex) {
                                    val sHex = headDims.acrossFlatsS
                                    val k = headDims.headHeightK
                                    val e = sHex / kotlin.math.cos(Math.toRadians(30.0))
                                    val dForged = 1.2 * e
                                    headForgedVol = Math.PI / 4.0 * dForged * dForged * k
                                    cheeseVol = headForgedVol - kotlin.math.sqrt(3.0) / 2.0 * sHex * sHex * k
                                    Text(text = "Forged round head: Ø${mm(dForged)} (1.2 × e ${mm(e)}) × k ${mm(k)} mm", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = customColors.textMain)
                                } else {
                                    // DIN 6921 (1983) dc max, k max, s from supplier catalogues; flange thickness c must come from the drawing
                                    val din6921 = mapOf<Double, Triple<Double, Double, Double?>>(5.0 to Triple(11.8, 5.4, 8.0), 20.0 to Triple(43.0, 17.1, null), 6.0 to Triple(14.2, 6.6, 10.0), 8.0 to Triple(18.0, 8.1, 13.0), 10.0 to Triple(22.3, 9.2, 15.0),
                                        12.0 to Triple(26.6, 11.5, 16.0), 16.0 to Triple(35.0, 14.4, 21.0))
                                    val row = din6921.entries.firstOrNull { kotlin.math.abs(it.key - dMm) < 0.01 }?.value
                                    var dcText by remember(dMm) { mutableStateOf(row?.first?.toString() ?: "") }
                                    var kText by remember(dMm) { mutableStateOf(row?.second?.toString() ?: headDims.headHeightK.toString()) }
                                    var sText by remember(dMm) { mutableStateOf(row?.let { r -> r.third?.toString() ?: "" } ?: headDims.acrossFlatsS.toString()) }
                                    var cText by remember(dMm) { mutableStateOf("") }
                                    Text(text = if (row != null) "Pre-filled from DIN 6921 (1983 edition, supplier tables) where known. Enter flange thickness c (and any blank value) from the drawing." else "No DIN 6921 row for this size: enter dc, k, s and c from the drawing.", fontSize = 10.sp, color = customColors.textMuted)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                        listOf(Triple<String, String, (String) -> Unit>("dc", dcText, { v: String -> dcText = v }), Triple<String, String, (String) -> Unit>("k", kText, { v: String -> kText = v }),
                                            Triple<String, String, (String) -> Unit>("s", sText, { v: String -> sText = v }), Triple<String, String, (String) -> Unit>("c", cText, { v: String -> cText = v })).forEach { (lbl, value, set) ->
                                            OutlinedTextField(
                                                value = value, onValueChange = set, singleLine = true, label = { Text(lbl, fontSize = 10.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = customColors.textMain),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    val dc = dcText.toDoubleOrNull(); val kF = kText.toDoubleOrNull(); val sF = sText.toDoubleOrNull(); val c = cText.toDoubleOrNull()
                                    if (dc != null && kF != null && sF != null && c != null && c > 0 && kF > c) {
                                        val dCollar = dc + 4.0
                                        val collarVol = Math.PI / 4.0 * dCollar * dCollar * c
                                        cheeseVol = Math.PI / 4.0 * (dCollar * dCollar - dc * dc) * c
                                        headForgedVol = collarVol + kotlin.math.sqrt(3.0) / 2.0 * sF * sF * (kF - c)
                                        Text(text = "Forged collar: Ø${mm(dCollar)} (dc + 2×2) × c ${mm(c)} mm, trimmed to Ø${mm(dc)}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = customColors.textMain)
                                    }
                                }
                                if (headForgedVol > 0.0) {
                                    val density = (FastenerCatalog.MATERIALS[selectedMaterialKey]?.density ?: 7.85) / 1000.0
                                    val headStock = headForgedVol / aShank
                                    Text(text = "Head stock      : ${"%.2f".format(java.util.Locale.US, headStock)} mm of Ø${mm(ds)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = customColors.textMain)
                                    Text(text = "Trim cheese     : ${"%.0f".format(java.util.Locale.US, cheeseVol)} mm³ = ${"%.2f".format(java.util.Locale.US, cheeseVol * density)} g", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = customColors.textMain)
                                    Text(text = "Cut-off length  : ${"%.2f".format(java.util.Locale.US, headStock + underHeadBefore(bMin))} – ${"%.2f".format(java.util.Locale.US, headStock + underHeadBefore(bMin + 2 * pitchMm))} mm", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = customColors.accentPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Authoritative Turning Blank / Tap Drill Card
        if (!result.fastenerType.isUnthreaded) {
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
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Surface(
                            color = customColors.accentSecondary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (result.isInternalNut) "INTERNAL NUT TAP DRILL SIZING" else "AUTHORITATIVE PRE-ROLL BLANK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = customColors.accentSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (result.isInternalNut) "Nut Boring / Tap Drill Dia (D_drill)" else "Optimised Blank Diameter (D_blank)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.textMain,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Ø${result.blankTargetDia} mm",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = customColors.accentSecondary
                        )
                        Text(
                            text = "${result.threadLabel} • ±${result.blankTolHalf} mm",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = customColors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3 Limit Cards: Upper, Target, Lower
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Upper Limit
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "UPPER LIMIT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = customColors.textMuted)
                            Text(
                                text = "${result.blankUpperLimit} mm",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = SuccessGreen
                            )
                            Text(text = "100% GO Free", fontSize = 9.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Target Nominal
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "TARGET NOMINAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = customColors.textMuted)
                            Text(
                                text = "${result.blankTargetDia} mm",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.accentPrimary
                            )
                            Text(text = "94% Crest Fill", fontSize = 9.sp, color = customColors.accentPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Lower Limit
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "LOWER LIMIT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = customColors.textMuted)
                            Text(
                                text = "${result.blankLowerLimit} mm",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = AlertRed
                            )
                            Text(text = "NO-GO ≤ 1.5 Turns", fontSize = 9.sp, color = AlertRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Gauge Simulator embedded
                GaugeSimulatorView(
                    targetBlankDia = result.blankTargetDia,
                    upperLimit = result.blankUpperLimit,
                    lowerLimit = result.blankLowerLimit,
                    tolHalf = result.blankTolHalf
                )
            }
        }
        } // end blank card (threaded parts only)

        // Pitch Sweet-Spot & Limits Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Surface(
                    color = customColors.accentPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (result.fastenerType.isUnthreaded) "RIVET \u2014 NO THREAD" else "ISO 965 / ASME B1.1 / AS8879 LIMITS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.accentPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = if (result.fastenerType.isUnthreaded) "Head & shank geometry (thread limits not applicable)" else "Authoritative Thread Diameter Limits (${result.threadClass})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = customColors.textMain,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                if (!result.fastenerType.isUnthreaded) {
                // 1. Major Diameter (Min / Mean / Max)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (result.isInternalNut) "MAJOR DIAMETER (D)" else "MAJOR DIAMETER (d)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMuted
                            )
                            Text(
                                text = "es/EI: ${if (result.fundamentalDeviationMm >= 0.0) "+" else ""}${result.fundamentalDeviationMm} mm",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = customColors.accentSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "MIN", fontSize = 9.sp, color = customColors.textMuted)
                                Text(
                                    text = "Ø${result.majorDiaMin} mm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = customColors.textMain
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "MEAN", fontSize = 9.sp, color = customColors.textMuted)
                                Text(
                                    text = "Ø${result.majorDiaMean} mm",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = customColors.accentPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "MAX", fontSize = 9.sp, color = customColors.textMuted)
                                Text(
                                    text = "Ø${result.majorDiaMax} mm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = customColors.textMain
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Pitch Diameter (Min / Sweet-Spot / Max)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (result.isInternalNut) "PITCH DIAMETER (D2) • 62% SWEET-SPOT" else "PITCH DIAMETER (d2) • 38% SWEET-SPOT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.accentPrimary
                            )
                            Text(
                                text = "Tol: ${((result.pitchDiaMax - result.pitchDiaMin) * 1000).toInt()} µm",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "MIN (NO-GO)", fontSize = 9.sp, color = customColors.textMuted)
                                Text(
                                    text = "Ø${result.pitchDiaMin} mm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = AlertRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "SWEET-SPOT", fontSize = 9.sp, color = customColors.accentPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Ø${result.pitchDiaSweet} mm",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = customColors.accentPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "MAX (GO)", fontSize = 9.sp, color = customColors.textMuted)
                                Text(
                                    text = "Ø${result.pitchDiaMax} mm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Minor Diameter / Root Limits — Before & After Plating
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val coatGrowthMm = result.coating.thicknessMaxUm * 2.0 / 1000.0
                    val isExternal = !result.isInternalNut
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (result.isInternalNut) "MINOR DIAMETER / TAP DRILL BORE (D1)" else "MINOR ROOT DIAMETER (d1 / d3)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMuted
                            )
                            if (result.rootRadiusMinMm != null) {
                                Text(
                                    text = "Root: R${result.rootRadiusMinMm}–R${result.rootRadiusMaxMm} mm",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = customColors.accentSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        // Before plating (thread-rolled / machined)
                        Text(text = "BEFORE PLATING (as rolled)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "MIN", fontSize = 9.sp, color = customColors.textMuted)
                                Text(
                                    text = "Ø${result.minorDiaMin} mm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = customColors.textMain
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "SWEET SPOT", fontSize = 9.sp, color = customColors.textMuted)
                                Text(
                                    text = "Ø${String.format("%.3f", (result.minorDiaMin + result.minorDiaMax) / 2.0)} mm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF22C55E)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "MAX", fontSize = 9.sp, color = customColors.textMuted)
                                Text(
                                    text = "Ø${result.minorDiaMax} mm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = customColors.textMain
                                )
                            }
                        }

                        // After plating
                        if (coatGrowthMm > 0.001) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "AFTER PLATING (${result.coating.code} +${String.format("%.0f", result.coating.thicknessMaxUm)}µm ×2 flanks)",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val postMin = if (isExternal) result.minorDiaMin + coatGrowthMm else result.minorDiaMin - coatGrowthMm
                            val postMax = if (isExternal) result.minorDiaMax + coatGrowthMm else result.minorDiaMax - coatGrowthMm
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "MIN", fontSize = 9.sp, color = customColors.textMuted)
                                    Text(
                                        text = "Ø${String.format("%.3f", postMin)} mm",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "GROWTH", fontSize = 9.sp, color = customColors.textMuted)
                                    Text(
                                        text = "${if (isExternal) "+" else "-"}${String.format("%.1f", coatGrowthMm * 1000)} µm",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "MAX", fontSize = 9.sp, color = customColors.textMuted)
                                    Text(
                                        text = "Ø${String.format("%.3f", postMax)} mm",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4. Coating Compensation & Allowance
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.4f)) {
                            Text(
                                text = "PLATING COMPENSATION (${result.coating.code})",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = customColors.accentSecondary
                            )
                            Text(
                                text = "${result.coatingSpecText}",
                                fontSize = 10.sp,
                                color = customColors.textMuted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pre-Plate Pitch Offset",
                                fontSize = 9.sp,
                                color = customColors.textMuted
                            )
                            Text(
                                text = "-${result.prePlatePitchAllowanceUm} µm (4×t)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.accentSecondary
                            )
                        }
                    }
                }

                } // end thread-only sections
                // 5. ISO/ASME Dimension Table
                result.headDimensions?.let { hd ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "NOMINAL DIMENSIONS (${hd.source})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                @Composable fun DimCell(label: String, value: String, mod: Modifier = Modifier) {
                                    Column(modifier = mod, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = label, fontSize = 8.sp, color = customColors.textMuted)
                                        Text(
                                            text = value,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = customColors.textMain
                                        )
                                    }
                                }
                                DimCell("S (AF)", "${hd.acrossFlatsS} mm")
                                DimCell("k (Head)", "${hd.headHeightK} mm")
                                DimCell("m (Nut)", "${hd.nutHeightM} mm")
                                DimCell("dw (Brg)", "${hd.bearingDiaDw} mm")
                            }
                            if (!hd.isTable) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "⚠ Interpolated — not from ISO/ASME table",
                                    fontSize = 8.sp,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3 Bottom Stats: Part Wt, Slug L0, Upset L/D
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = customColors.bgElevated
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Net/Gross Wt", fontSize = 9.sp, color = customColors.textMuted)
                            Text(
                                text = "${result.netWeightGrams}g / ${result.grossWeightGrams}g",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.textMain
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = customColors.bgElevated
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Cut Slug L0", fontSize = 9.sp, color = customColors.textMuted)
                            Text(
                                text = "${result.cutOffLengthMm} mm",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.textMain
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        color = customColors.bgElevated
                    ) {
                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Upset L/D", fontSize = 9.sp, color = customColors.textMuted)
                            Text(
                                text = "${result.upsetLdRatio}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.accentSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Raw Material Sourcing UTS & Tensile Proof Load Summary
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Raw Sourcing Wire UTS:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMuted
                            )
                            Text(
                                text = result.rawMaterialSourcingUts,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.accentSecondary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tensile Proof Load / Break:",
                                fontSize = 9.sp,
                                color = customColors.textMuted
                            )
                            Text(
                                text = "Fp: ${result.proofLoadKn} kN  •  Fm: ${result.tensileBreakingLoadKn} kN (UTS: ${result.postForgingTargetUtsMpa.toInt()} MPa)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }
        }

        // Interactive 2D Blueprint / 3D Parametric CAD Canvas Card
        var canvasViewMode3D by remember { mutableStateOf(false) }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (canvasViewMode3D) "3D PARAMETRIC CAD MODEL" else "2D ENGINEERING BLUEPRINT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.accentPrimary
                    )

                    // Segmented Toggle Button: 2D vs 3D
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (!canvasViewMode3D) customColors.accentPrimary else Color.Transparent,
                                modifier = Modifier.clickable { canvasViewMode3D = false }
                            ) {
                                Text(
                                    text = "2D Canvas",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!canvasViewMode3D) Color.White else customColors.textMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (canvasViewMode3D) customColors.accentPrimary else Color.Transparent,
                                modifier = Modifier.clickable { canvasViewMode3D = true }
                            ) {
                                Text(
                                    text = "3D CAD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canvasViewMode3D) Color.White else customColors.textMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                if (canvasViewMode3D) {
                    com.example.ui.components.Interactive3dFastenerView(result = result)
                } else {
                    BlueprintCanvasView(result = result)
                }
            }
        }
    }
}

private const val NOT_APPLICABLE_THREAD = "N/A — no thread"
private const val NOT_APPLICABLE_WASHER = "N/A — washer (graded by hardness)"

@Composable
fun SelectorDropdown(
    label: String,
    selectedText: String,
    items: List<String>,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) {
            items.mapIndexed { idx, s -> idx to s }
        } else {
            items.mapIndexed { idx, s -> idx to s }.filter { it.second.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = customColors.textMuted,
            maxLines = 1
        )
        Box(modifier = Modifier.padding(top = 2.dp)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = customColors.bgElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        searchQuery = ""
                        expanded = true
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = customColors.textMain,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Expand",
                        tint = customColors.textMuted
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(customColors.bgCard)
                    .heightIn(max = 380.dp)
            ) {
                if (items.size > 3) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search catalog...", fontSize = 11.sp, color = customColors.textMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = customColors.bgElevated,
                            unfocusedContainerColor = customColors.bgElevated,
                            focusedBorderColor = customColors.accentPrimary,
                            unfocusedBorderColor = customColors.border,
                            focusedTextColor = customColors.textMain,
                            unfocusedTextColor = customColors.textMain
                        )
                    )
                }

                if (filteredItems.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No matching items found", fontSize = 11.sp, color = customColors.textMuted) },
                        onClick = { }
                    )
                } else {
                    filteredItems.forEach { (originalIndex, itemText) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = itemText,
                                    fontSize = 12.sp,
                                    fontWeight = if (itemText == selectedText) FontWeight.Black else FontWeight.Normal,
                                    color = if (itemText == selectedText) customColors.accentPrimary else customColors.textMain
                                )
                            },
                            onClick = {
                                expanded = false
                                onItemSelected(originalIndex)
                            }
                        )
                    }
                }
            }
        }
    }
}
