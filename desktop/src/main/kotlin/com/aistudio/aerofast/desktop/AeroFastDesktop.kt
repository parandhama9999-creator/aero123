package com.aistudio.aerofast.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aerofast.trmst.data.CalculationEngine
import com.aistudio.aerofast.trmst.data.CoatingType
import com.aistudio.aerofast.trmst.data.FastenerCalculationResult
import com.aistudio.aerofast.trmst.data.FastenerCatalog
import com.aistudio.aerofast.trmst.data.FastenerType
import com.aistudio.aerofast.trmst.data.HeaderChoice
import com.aistudio.aerofast.trmst.data.HeatTreatCycleEngine
import com.aistudio.aerofast.trmst.data.AnnealCycleEngine
import com.aistudio.aerofast.trmst.data.MachineSetupStore
import com.aistudio.aerofast.trmst.data.PokaYokeEngine
import com.aistudio.aerofast.trmst.data.PokaYokeSeverity
import com.aistudio.aerofast.trmst.data.ProductionReport
import com.aistudio.aerofast.trmst.data.PropertyClass
import com.aistudio.aerofast.trmst.data.SelectionFilter
import com.aistudio.aerofast.trmst.data.ThreadClassCatalog
import com.aistudio.aerofast.trmst.data.ToolingDesign
import com.aistudio.aerofast.trmst.data.UnitSystem
import com.aistudio.aerofast.trmst.ui.platform.LocalPlatformActions
import com.aistudio.aerofast.trmst.ui.screens.QualityScreen
import com.aistudio.aerofast.trmst.ui.theme.AlertRed
import com.aistudio.aerofast.trmst.ui.theme.AppThemeMode
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors
import com.aistudio.aerofast.trmst.ui.theme.SuccessGreen
import com.aistudio.aerofast.trmst.ui.theme.getCustomColors
import java.util.Locale

private enum class Tab(val title: String) { RESULTS("Results"), TOOLING("Tooling"), HEAT("Heat Treatment"), QUALITY("Quality") }

private fun f(v: Double, dp: Int = 2) = String.format(Locale.US, "%.${dp}f", v)

/** The part being designed; every field the calculation takes. */
data class PartInputs(
    val type: FastenerType = FastenerType.HEX_BOLT,
    val unit: UnitSystem = UnitSystem.METRIC,
    val sizeIndex: Int = FastenerCatalog.defaultSizeIndex(UnitSystem.METRIC),
    val pitch: Double = 1.5,
    val lengthText: String = "60",
    val threadClass: String = "6g",
    val materialKey: String = "10B21",
    val propertyClass: PropertyClass? = PropertyClass.CLASS_10_9,
    val coating: CoatingType = CoatingType.ZINC_BLUE,
    val batchText: String = "10000",
    val header: HeaderChoice = HeaderChoice.AUTO,
    val patch: Boolean = false
) {
    fun sizes(): List<String> = if (unit == UnitSystem.METRIC) FastenerCatalog.METRIC_SIZES.map { it.label } else FastenerCatalog.INCH_SIZES.map { it.label }
    fun pitches(): List<Double> = if (unit == UnitSystem.METRIC) FastenerCatalog.METRIC_SIZES.getOrNull(sizeIndex)?.pitches.orEmpty()
        else FastenerCatalog.INCH_SIZES.getOrNull(sizeIndex)?.tpis.orEmpty().map { it.toDouble() }
    fun threadClasses() = ThreadClassCatalog.getOptions(unit, type.isNut)
    fun classes() = SelectionFilter.propertyClasses(type, unit)

    /** Keeps every dependent choice valid after the type, unit or size changes. */
    fun normalised(): PartInputs {
        var p = this
        if (p.sizeIndex !in p.sizes().indices) p = p.copy(sizeIndex = FastenerCatalog.defaultSizeIndex(p.unit))
        if (p.pitch !in p.pitches()) p = p.copy(pitch = p.pitches().firstOrNull() ?: p.pitch)
        if (p.threadClass !in p.threadClasses()) p = p.copy(threadClass = p.threadClasses().first())
        val cls = p.classes()
        if (!SelectionFilter.usesPropertyClass(p.type)) p = p.copy(propertyClass = null)
        else if (p.propertyClass == null || p.propertyClass !in cls) p = p.copy(propertyClass = cls.firstOrNull())
        return p
    }

    fun calculate(): Result<FastenerCalculationResult> = runCatching {
        val length = lengthText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: error("Enter a length in mm")
        CalculationEngine.calculate(
            fastenerType = type, unitSystem = unit, standard = PokaYokeEngine.defaultStandard(type, unit), nominalSizeIndex = sizeIndex,
            pitchValue = pitch, lengthMm = length, threadClass = threadClass,
            material = FastenerCatalog.MATERIALS[materialKey] ?: error("Unknown material $materialKey"),
            coating = coating, batchQty = batchText.toIntOrNull()?.coerceAtLeast(1) ?: 1000, propertyClass = propertyClass,
            threadLockPatch = patch, headerChoice = header, openExtrusionLimitPct = MachineSetupStore.setup.openLimit
        )
    }
}

@Composable
fun AeroFastDesktop(platform: DesktopPlatform, startTab: Int = 0, start: PartInputs = PartInputs()) {
    val colors = getCustomColors(AppThemeMode.INDUSTRIAL_LIGHT)
    var inputs by remember { mutableStateOf(start.normalised()) }
    var tab by remember { mutableStateOf(Tab.entries[startTab]) }
    var message by remember { mutableStateOf("") }
    platform.lastMessage = { message = it }
    val result = remember(inputs) { inputs.calculate() }

    MaterialTheme(colorScheme = lightColorScheme(primary = colors.accentPrimary, secondary = colors.accentSecondary)) {
        CompositionLocalProvider(LocalCustomColors provides colors, LocalPlatformActions provides platform) {
            Column(Modifier.fillMaxSize().background(colors.bgMain)) {
                Row(Modifier.fillMaxWidth().background(Color(0xFF0E1A2E)).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("AEROFAST PRO", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFEAF0F7))
                    Text("  by Parandama Pathipati", fontSize = 13.sp, color = Color(0xFFFFD27A))
                    Box(Modifier.weight(1f))
                    if (message.isNotBlank()) Text(message, fontSize = 11.sp, color = Color(0xFF9AA8BA))
                }
                Row(Modifier.fillMaxSize()) {
                    InputPanel(inputs) { inputs = it.normalised() }
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Tab.entries.forEach { t ->
                                val on = t == tab
                                Text(t.title, fontSize = 13.sp, fontWeight = if (on) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (on) Color.White else colors.textMain,
                                    modifier = Modifier.background(if (on) colors.accentPrimary else colors.bgCard, RoundedCornerShape(16.dp))
                                        .border(1.dp, if (on) colors.accentPrimary else colors.border, RoundedCornerShape(16.dp))
                                        .clickable { tab = t }.padding(horizontal = 16.dp, vertical = 8.dp))
                            }
                        }
                        val r = result.getOrNull()
                        if (r == null) {
                            Text("Cannot calculate: ${result.exceptionOrNull()?.message}", color = AlertRed, modifier = Modifier.padding(16.dp))
                        } else when (tab) {
                            Tab.QUALITY -> Box(Modifier.fillMaxSize()) { QualityScreen(r, wrapSections = true) }
                            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                when (tab) {
                                    Tab.RESULTS -> ResultsTab(r, platform)
                                    Tab.TOOLING -> ToolingTab(r, platform)
                                    Tab.HEAT -> HeatTab(r)
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputPanel(p: PartInputs, onChange: (PartInputs) -> Unit) {
    val c = LocalCustomColors.current
    Column(Modifier.width(360.dp).fillMaxHeight().background(c.bgElevated).verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("PART", fontSize = 13.sp, fontWeight = FontWeight.Black, color = c.accentPrimary)
        Picker("Fastener type", FastenerType.entries, p.type, { it.label }) { onChange(p.copy(type = it)) }
        Picker("Unit system", UnitSystem.entries, p.unit, { it.label }) { onChange(p.copy(unit = it, sizeIndex = FastenerCatalog.defaultSizeIndex(it))) }
        Picker("Size", p.sizes().indices.toList(), p.sizeIndex, { p.sizes()[it] }) { onChange(p.copy(sizeIndex = it)) }
        if (p.pitches().isNotEmpty()) Picker(if (p.unit == UnitSystem.METRIC) "Pitch (mm)" else "Threads per inch", p.pitches(), p.pitch,
            { if (p.unit == UnitSystem.METRIC) f(it, 3).trimEnd('0').trimEnd('.') else "${it.toInt()} TPI" }) { onChange(p.copy(pitch = it)) }
        OutlinedTextField(p.lengthText, { onChange(p.copy(lengthText = it)) }, label = { Text("Length (mm)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Picker("Thread class", p.threadClasses(), p.threadClass, { it }) { onChange(p.copy(threadClass = it)) }
        Text("MATERIAL & FINISH", fontSize = 13.sp, fontWeight = FontWeight.Black, color = c.accentPrimary)
        val materials = FastenerCatalog.MATERIALS.values.sortedBy { it.name }
        Picker("Material", materials.map { it.key }, p.materialKey, { k -> FastenerCatalog.MATERIALS[k]?.name ?: k }) { onChange(p.copy(materialKey = it)) }
        if (SelectionFilter.usesPropertyClass(p.type)) Picker("Strength class", p.classes(), p.propertyClass, { it.label }) { onChange(p.copy(propertyClass = it)) }
        Picker("Coating", CoatingType.entries, p.coating, { it.label }) { onChange(p.copy(coating = it)) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(p.patch, { onChange(p.copy(patch = it)) })
            Text("Nylon thread-locking patch", fontSize = 12.sp, color = c.textMain)
        }
        Text("PRODUCTION", fontSize = 13.sp, fontWeight = FontWeight.Black, color = c.accentPrimary)
        OutlinedTextField(p.batchText, { onChange(p.copy(batchText = it.filter { ch -> ch.isDigit() })) }, label = { Text("Batch quantity") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Picker("Heading machine", HeaderChoice.entries, p.header, { it.label }) { onChange(p.copy(header = it)) }
        Text("Standard: ${PokaYokeEngine.defaultStandard(p.type, p.unit).let { "${it.code} (${it.label})" }}", fontSize = 11.sp, color = c.textMuted)
    }
}

@Composable
private fun ExportButton(label: String, onClick: () -> Unit) {
    val c = LocalCustomColors.current
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = c.accentPrimary)) { Text(label, color = Color.White, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ResultsTab(r: FastenerCalculationResult, platform: DesktopPlatform) {
    val c = LocalCustomColors.current
    Text("${r.threadLabel} × ${f(r.lengthMm, 1)} mm  •  ${r.fastenerType.label}  •  ${r.material.name}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = c.textMain)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card("Thread and blank") {
                KV("Standard", "${r.standard.code} (${r.standard.label})")
                KV("Thread class", r.threadClass)
                if (!r.fastenerType.isUnthreaded && !r.fastenerType.isWasher) {
                    KV("Major diameter", "${f(r.majorDiaMin, 3)} – ${f(r.majorDiaMax, 3)} mm")
                    KV("Pitch diameter", "${f(r.pitchDiaMin, 3)} – ${f(r.pitchDiaMax, 3)} mm")
                    KV("Minor diameter", "${f(r.minorDiaMin, 3)} – ${f(r.minorDiaMax, 3)} mm")
                    if (!r.isInternalNut) KV("Rolling blank", "${f(r.blankLowerLimit, 3)} – ${f(r.blankUpperLimit, 3)} mm", SuccessGreen)
                }
                KV("Coating", "${r.coating.label} (${r.coating.specStandard})")
            }
            Card("Material and strength") {
                KV("Strength", r.tensileRange?.label ?: r.propertyClass?.label ?: "Per material spec")
                KV("Wire (mill)", r.rawMaterialSourcingUts)
                KV("Target UTS / yield", "${f(r.postForgingTargetUtsMpa, 0)} / ${f(r.yieldStrengthMpa, 0)} MPa")
                KV("Stress area As", "${f(r.stressAreaAsMm2, 2)} mm²")
                KV("Proof load", "${f(r.proofLoadKn, 1)} kN")
                KV("Breaking load", "${f(r.tensileBreakingLoadKn, 1)} kN")
                KV("Hardness", r.heatTreatmentSpec.targetHardnessHrc)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card("Heading") {
                KV("Wire diameter", "${f(r.wireDiaMm)} mm")
                KV("Cut-off length", "${f(r.cutOffLengthMm)} mm")
                KV("Net / slug weight", "${f(r.netWeightGrams, 3)} / ${f(r.grossWeightGrams, 3)} g")
                KV("Upset L/D", f(r.upsetLdRatio))
                KV("Heading force", "${f(r.forgingTonnage, 1)} t")
                KV("Machine", r.headingPlan?.machineDescription ?: r.recommendedMachine)
                r.headingPlan?.let { pl -> KV("Stations", pl.stations.drop(1).joinToString("  →  ") { it.title }) }
            }
            Card("Cost estimate") {
                KV("Unit price", "${r.currencySymbol}${f(r.finalUnitPrice, 2)}")
                KV("Batch (${r.batchQty} pcs)", "${r.currencySymbol}${f(r.batchTotalPrice, 0)}")
            }
        }
    }
    if (r.pokaYokeIssues.isNotEmpty()) Card("Poka-yoke checks", AlertRed) {
        r.pokaYokeIssues.forEach { i ->
            val col = when (i.severity) { PokaYokeSeverity.CRITICAL -> AlertRed; PokaYokeSeverity.MANDATORY -> Color(0xFFB45309); else -> c.accentPrimary }
            Text("${i.severity}: ${i.title}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = col)
            Text(i.detail, fontSize = 11.sp, color = c.textMain)
            Text("Fix: ${i.fix}", fontSize = 11.sp, color = c.textMuted)
        }
    }
    Card("Process sequence") { ProductionReport.processSequence(r).forEachIndexed { n, s -> Text("${(n + 1) * 10}. $s", fontSize = 12.sp, color = c.textMain) } }
    Card("Forging risks and recommendations") {
        r.forgingRisks.forEach { Text(it, fontSize = 12.sp, color = c.textMain) }
        r.forgingRecommendations.forEach { Text("✔ $it", fontSize = 12.sp, color = SuccessGreen) }
    }
    val report = remember(r) { ProductionReport.build(r) }
    ExportButton("Save / copy production report") { platform.shareText("Production report ${r.threadLabel}", report) }
    Card("Production report") { Mono(report) }
}

@Composable
private fun ToolingTab(r: FastenerCalculationResult, platform: DesktopPlatform) {
    val c = LocalCustomColors.current
    val machine = MachineSetupStore.setup
    val sheet = remember(r, machine) { ToolingDesign.build(r, machine) }
    if (sheet == null) { Text("No heading plan for this part, so there is no header tooling.", color = c.textMuted); return }
    val text = remember(r, sheet) { ToolingDesign.text(r, sheet, machine) }
    if (sheet.problems.isNotEmpty()) Card("Does not fit the machine", AlertRed) { sheet.problems.forEach { Text("• $it", fontSize = 12.sp, color = AlertRed) } }
    ExportButton("Save / copy tooling sheet") { platform.shareText("Tooling sheet ${r.threadLabel}", text) }
    Card("Header tooling sheet") { Mono(text) }
}

@Composable
private fun HeatTab(r: FastenerCalculationResult) {
    val c = LocalCustomColors.current
    val ht = r.heatTreatmentSpec
    Card("Heat treatment — ${r.material.name}") {
        KV("Solution / austenitize", ht.austenitizingOrSolutionTempC)
        KV("Soak", ht.soakingTimeMinutes)
        KV("Quench / cool", ht.quenchMedium)
        if (ht.quenchDetails.isNotBlank()) KV("Quench detail", ht.quenchDetails)
        KV(if (ht.isAging) "Age" else "Temper", ht.temperingOrAgingTempC)
        KV("Time", ht.temperingOrAgingTimeHours)
        if (ht.coolingMethod.isNotBlank()) KV("Cooling", ht.coolingMethod)
        if (ht.atmosphere.isNotBlank()) KV("Atmosphere", ht.atmosphere)
        KV("Hardness", ht.targetHardnessHrc)
        KV("Hydrogen bake", if (r.hasHydrogenEmbrittlementRisk) "${ht.bakingTempC}, ${ht.bakingSoakTime}" else ht.hydrogenBakingCycle)
        KV("Standard", ht.standardReference)
    }
    val hardening = HeatTreatCycleEngine.quenchTemper(r.material, r.wireDiaMm, ht) ?: HeatTreatCycleEngine.precipitationHarden(r.material, r.wireDiaMm)
    hardening?.let { Card(it.title) { StageList(it.stages, it.totalText, it.basis, it.cautions, it.sources) } }
    r.softAnnealSpec?.let { a ->
        Card("Soft anneal — wire preparation") {
            KV("Route", a.route.label)
            KV("Supplied", a.suppliedCondition)
            KV("Temperature", a.temperatureC)
            if (a.soakTime.isNotBlank()) KV("Soak", a.soakTime)
            KV("Cooling", a.coolingRate)
            KV("Target", a.targetUtsMpa)
            a.notes.forEach { Text("• $it", fontSize = 11.sp, color = c.textMuted) }
        }
        val sph = AnnealCycleEngine.spheroidize(r.material, AnnealCycleEngine.DEFAULT_CHARGE_TONNES, r.wireDiaMm)
        if (sph != null && a.inHouseAnnealNeeded) Card("Spheroidize anneal — complete cycle") { StageList(sph.stages, "${sph.totalText} • ${sph.hoursPerTonne}", sph.basis, sph.cautions, sph.sources) }
        else HeatTreatCycleEngine.softAnneal(r.material, r.wireDiaMm)?.let { Card(it.title) { StageList(it.stages, it.totalText, it.basis, it.cautions, it.sources) } }
    }
}
