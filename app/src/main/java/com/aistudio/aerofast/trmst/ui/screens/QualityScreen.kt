package com.aistudio.aerofast.trmst.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.aerofast.trmst.data.ControlPlanRow
import com.aistudio.aerofast.trmst.data.FastenerCalculationResult
import com.aistudio.aerofast.trmst.data.ProductionReport
import com.aistudio.aerofast.trmst.data.QualityDocs
import com.aistudio.aerofast.trmst.data.QualityEngine
import com.aistudio.aerofast.trmst.data.UnitSystem
import com.aistudio.aerofast.trmst.ui.theme.AlertRed
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors
import com.aistudio.aerofast.trmst.ui.theme.SuccessGreen
import com.aistudio.aerofast.trmst.ui.platform.LocalPlatformActions
import java.util.Locale

private enum class QSection(val title: String) {
    CONTROL("Control Plan"), APQP("APQP"), PPAP("PPAP & PSW"), CPK("Cp / Cpk"), TORQUE("Torque-Tension"),
    COATING("Coating & Tests"), INSPECT("GD&T & Inspection"), LEAN("Lean / Six Sigma"), ECN("Change (ECN)"), STANDARDS("Standards / JIS")
}

private fun f(v: Double, dp: Int = 2) = String.format(Locale.US, "%.${dp}f", v)

private const val PREFS = "quality_prefs"

@OptIn(ExperimentalLayoutApi::class)
/** Quality systems for the current part: control plan, APQP, PPAP, capability, torque, tests, GD&T, lean and change management. */
@Composable
fun QualityScreen(result: FastenerCalculationResult, wrapSections: Boolean = false) {
    val c = LocalCustomColors.current
    val platform = LocalPlatformActions.current
    var section by rememberSaveable { mutableStateOf(QSection.CONTROL) }
    var csr by remember { mutableStateOf(platform.loadString(PREFS, "csr") ?: "") }
    fun saveCsr(v: String) { csr = v; platform.saveString(PREFS, "csr", v) }

    Column(Modifier.fillMaxSize().background(c.bgMain)) {
        // Phones scroll the section row sideways; a desktop window wraps it so every section stays visible
        val sectionRow: @Composable (@Composable () -> Unit) -> Unit = { content ->
            if (wrapSections) FlowRow(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
            else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) { content() }
        }
        sectionRow {
            QSection.entries.forEach { s ->
                val on = s == section
                Text(
                    s.title, fontSize = 11.sp, fontWeight = if (on) FontWeight.Black else FontWeight.SemiBold,
                    color = if (on) Color.White else c.textMain,
                    modifier = Modifier
                        .background(if (on) c.accentPrimary else c.bgCard, RoundedCornerShape(14.dp))
                        .border(1.dp, if (on) c.accentPrimary else c.border, RoundedCornerShape(14.dp))
                        .clickable { section = s }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("${result.threadLabel} × ${f(result.lengthMm, 1)} mm  •  ${result.fastenerType.label}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.textMain)
            when (section) {
                QSection.CONTROL -> ControlPlanSection(result)
                QSection.APQP -> ApqpSection(result)
                QSection.PPAP -> PpapSection(result, csr, ::saveCsr)
                QSection.CPK -> CapabilitySection()
                QSection.TORQUE -> TorqueSection(result)
                QSection.COATING -> CoatingSection(result)
                QSection.INSPECT -> InspectionSection()
                QSection.LEAN -> LeanSection()
                QSection.ECN -> EcnSection(result, csr)
                QSection.STANDARDS -> StandardsSection(result)
            }
        }
    }
}

@Composable
private fun QCard(title: String, accent: Color = LocalCustomColors.current.accentPrimary, content: @Composable () -> Unit) {
    val c = LocalCustomColors.current
    Surface(shape = RoundedCornerShape(10.dp), color = c.bgCard, border = androidx.compose.foundation.BorderStroke(1.dp, c.border), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Black, color = accent)
            content()
        }
    }
}

@Composable
private fun Note(text: String) = Text(text, fontSize = 10.sp, color = LocalCustomColors.current.textMuted)

@Composable
private fun KV(label: String, value: String, valueColor: Color = LocalCustomColors.current.textMain) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 11.sp, color = LocalCustomColors.current.textMuted, modifier = Modifier.weight(0.45f))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = valueColor, modifier = Modifier.weight(0.55f))
    }
}

@Composable
private fun ShareButton(label: String, subject: String, text: () -> String) {
    val c = LocalCustomColors.current
    val platform = LocalPlatformActions.current
    Button(onClick = { platform.shareText(subject, text()) }, colors = ButtonDefaults.buttonColors(containerColor = c.accentPrimary), modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NumField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label, fontSize = 10.sp) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
    )
}

@Composable
private fun TextIn(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), lines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label, fontSize = 10.sp) }, singleLine = lines == 1,
        minLines = lines, modifier = modifier, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
    )
}

private fun String.num() = replace(',', '.').trim().toDoubleOrNull()

// ---------------- Control plan ----------------

private fun controlPlanText(r: FastenerCalculationResult, rows: List<ControlPlanRow>) = buildString {
    append("CONTROL PLAN (production)\n${r.fastenerType.label}  ${r.threadLabel} x ${f(r.lengthMm, 1)} mm  ${r.material.key}  ${r.coating.label}\n")
    append("★ = special characteristic (apply your customer's symbol)\n\n")
    rows.forEach { row ->
        append("${row.step} | ${if (row.special) "★ " else ""}${row.characteristic} (${if (row.isProduct) "product" else "process"})\n")
        append("   Spec: ${row.spec}\n   Method: ${row.method}\n   Sample / frequency: ${row.sampling}\n   Control: ${row.control}\n   Reaction: ${row.reaction}\n")
    }
}

@Composable
private fun ControlPlanSection(r: FastenerCalculationResult) {
    val c = LocalCustomColors.current
    val rows = remember(r) { QualityDocs.controlPlan(r) }
    Note("Built from this part's process route, tolerances and tests. ★ marks characteristics customers usually flag as special; use your customer's symbol. Sample sizes and frequencies are your plant's decision (PFMEA / customer).")
    var step = ""
    rows.forEach { row ->
        if (row.step != step) { step = row.step; Text(step.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, color = c.accentSecondary, modifier = Modifier.padding(top = 4.dp)) }
        QCard((if (row.special) "★ " else "") + row.characteristic, if (row.special) AlertRed else c.accentPrimary) {
            KV(if (row.isProduct) "Product spec" else "Process spec", row.spec)
            KV("Method", row.method)
            KV("Sample / freq.", row.sampling)
            KV("Control", row.control)
            KV("Reaction plan", row.reaction)
        }
    }
    ShareButton("Share control plan", "Control plan ${r.threadLabel}") { controlPlanText(r, rows) }
}

// ---------------- APQP ----------------

@Composable
private fun ApqpSection(r: FastenerCalculationResult) {
    val c = LocalCustomColors.current
    val phases = remember(r) { QualityDocs.apqp(r) }
    val done = remember { mutableStateOf(setOf<String>()) }
    Note("AIAG APQP phases and outputs, filled in for this part. Tick items as your team completes them (ticks are not saved).")
    phases.forEach { ph ->
        QCard(ph.title) {
            ph.items.forEach { (item, detail) ->
                val key = ph.title + item
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(checked = key in done.value, onCheckedChange = { done.value = if (it) done.value + key else done.value - key })
                    Column(Modifier.padding(top = 10.dp)) {
                        Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.textMain)
                        Text(detail, fontSize = 10.sp, color = c.textMuted)
                    }
                }
            }
        }
    }
    ShareButton("Share APQP checklist", "APQP ${r.threadLabel}") {
        phases.joinToString("\n\n") { ph -> ph.title.uppercase() + "\n" + ph.items.joinToString("\n") { (i, d) -> " [${if (ph.title + i in done.value) "x" else " "}] $i: $d" } }
    }
}

// ---------------- PPAP ----------------

@Composable
private fun PpapSection(r: FastenerCalculationResult, csr: String, onCsr: (String) -> Unit) {
    val c = LocalCustomColors.current
    val platform = LocalPlatformActions.current
    var level by rememberSaveable { mutableStateOf(3) }
    var reason by rememberSaveable { mutableStateOf(QualityDocs.PSW_REASONS[0]) }
    var partNo by remember { mutableStateOf(platform.loadString(PREFS, "partNo") ?: "") }
    var rev by remember { mutableStateOf(platform.loadString(PREFS, "rev") ?: "") }
    var customer by remember { mutableStateOf(platform.loadString(PREFS, "customer") ?: "") }
    val elements = remember(r) { QualityDocs.ppapElements(r) }

    QCard("Submission level") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { l ->
                val on = l == level
                Text("Level $l", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (on) Color.White else c.textMain,
                    modifier = Modifier.background(if (on) c.accentPrimary else c.bgMain, RoundedCornerShape(8.dp)).clickable { level = l }.padding(horizontal = 8.dp, vertical = 6.dp))
            }
        }
        Note(when (level) {
            1 -> "Level 1: warrant only (and appearance report if applicable)."
            2 -> "Level 2: warrant with product samples and limited supporting data."
            3 -> "Level 3: warrant with product samples and complete supporting data (the usual default)."
            4 -> "Level 4: warrant and other requirements as defined by the customer."
            else -> "Level 5: warrant with samples and complete data, reviewed at the supplier's plant."
        })
    }
    QCard("Elements (AIAG PPAP 4th ed.)") {
        Note("S = submit to customer and retain, R = retain and make available, * = retain, submit if the customer asks.")
        elements.forEach { e ->
            val code = e.levels.split(' ')[level - 1]
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(code, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace,
                    color = if (code == "S") AlertRed else c.textMuted, modifier = Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("${e.no}. ${e.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.textMain)
                    Text(e.forThisPart, fontSize = 10.sp, color = c.textMuted)
                }
            }
        }
    }
    QCard("Part submission warrant") {
        TextIn("Customer part number", partNo) { partNo = it; platform.saveString(PREFS, "partNo", it) }
        TextIn("Drawing revision", rev) { rev = it; platform.saveString(PREFS, "rev", it) }
        TextIn("Customer", customer) { customer = it; platform.saveString(PREFS, "customer", it) }
        Text("Reason for submission", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.textMain)
        QualityDocs.PSW_REASONS.forEach { rs ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { reason = rs }) {
                Checkbox(checked = reason == rs, onCheckedChange = { reason = rs })
                Text(rs, fontSize = 11.sp, color = c.textMain)
            }
        }
    }
    QCard("Customer-specific requirements") {
        Note("The app does not hold OEM specifications (VDA, GMW, Ford WSS and others are the customers' documents). List the ones on your drawing / purchase order, one per line; they go into the PSW and the change notice.")
        TextIn("e.g. VDA 235-101, customer coating spec, packaging spec", csr, lines = 3, onChange = onCsr)
    }
    ShareButton("Share PSW + element list", "PSW ${partNo.ifBlank { r.threadLabel }}") { QualityDocs.psw(r, partNo, rev, customer, level, reason, csr) }
}

// ---------------- Capability ----------------

@Composable
private fun CapabilitySection() {
    val c = LocalCustomColors.current
    var lsl by rememberSaveable { mutableStateOf("") }
    var usl by rememberSaveable { mutableStateOf("") }
    var data by rememberSaveable { mutableStateOf("") }
    val values = QualityEngine.parseValues(data)
    val cap = QualityEngine.capability(values, lsl.num(), usl.num())
    QCard("Process capability (Cp, Cpk, Pp, Ppk)") {
        Note("Enter the specification limits and individual measurements in production order (spaces, commas or new lines). Leave one limit empty for a one-sided spec.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("LSL", lsl, Modifier.weight(1f)) { lsl = it }
            NumField("USL", usl, Modifier.weight(1f)) { usl = it }
        }
        TextIn("Measurements", data, lines = 4) { data = it }
    }
    if (cap == null) { Note("Enter at least 2 measurements."); return }
    fun grade(v: Double?) = when { v == null -> c.textMain; v >= 1.67 -> SuccessGreen; v >= 1.33 -> Color(0xFFF59E0B); else -> AlertRed }
    QCard("Results (n = ${cap.n})") {
        KV("Mean", f(cap.mean, 4))
        KV("Min / max", "${f(cap.min, 4)} / ${f(cap.max, 4)}")
        KV("σ within (MR̄ / 1.128)", f(cap.sigmaWithin, 5))
        KV("σ overall (n − 1)", f(cap.sigmaOverall, 5))
        KV("Cp", cap.cp?.let { f(it) } ?: "needs both limits")
        KV("Cpk", cap.cpk?.let { f(it) } ?: "–", grade(cap.cpk))
        KV("Pp", cap.pp?.let { f(it) } ?: "needs both limits")
        KV("Ppk", cap.ppk?.let { f(it) } ?: "–", grade(cap.ppk))
        KV("Expected out of spec", "${f(cap.ppmOutside, 0)} PPM (normal model)")
        KV("Measured out of spec", "${cap.outOfSpecCount} of ${cap.n}", if (cap.outOfSpecCount > 0) AlertRed else c.textMain)
        Note("PPAP initial study: Ppk > 1.67 acceptable, 1.33–1.67 may be acceptable, < 1.33 not acceptable. Cp/Cpk assume a stable process: check the data on a control chart first. At least 25 subgroups / 100 readings are usual for a study.")
        Histogram(values, lsl.num(), usl.num())
    }
}

@Composable
private fun Histogram(values: List<Double>, lsl: Double?, usl: Double?) {
    val c = LocalCustomColors.current
    if (values.size < 5) return
    val lo = listOfNotNull(values.min(), lsl).min()
    val hi = listOfNotNull(values.max(), usl).max()
    if (hi <= lo) return
    val bins = 30
    val counts = IntArray(bins)
    values.forEach { v -> counts[((v - lo) / (hi - lo) * bins).toInt().coerceIn(0, bins - 1)]++ }
    val maxCount = counts.max().coerceAtLeast(1)
    val bar = c.accentPrimary
    Canvas(Modifier.fillMaxWidth().height(110.dp).padding(top = 6.dp)) {
        val bw = size.width / bins
        counts.forEachIndexed { i, n ->
            val h = size.height * n / maxCount
            drawRect(bar.copy(alpha = 0.75f), Offset(i * bw + 1f, size.height - h), Size(bw - 2f, h))
        }
        fun x(v: Double) = ((v - lo) / (hi - lo) * size.width).toFloat()
        lsl?.let { drawLine(AlertRed, Offset(x(it), 0f), Offset(x(it), size.height), 3f) }
        usl?.let { drawLine(AlertRed, Offset(x(it), 0f), Offset(x(it), size.height), 3f) }
    }
}

// ---------------- Torque-tension ----------------

@Composable
private fun TorqueSection(r: FastenerCalculationResult) {
    val c = LocalCustomColors.current
    val metric = r.unitSystem == UnitSystem.METRIC
    var d by remember(r) { mutableStateOf(f(r.nominalDiaMm, 3).trimEnd('0').trimEnd('.')) }
    var p by remember(r) { mutableStateOf(f(r.pitchMm, 4).trimEnd('0').trimEnd('.')) }
    var rp by remember(r) { mutableStateOf(f(r.yieldStrengthMpa, 0)) }
    var dw by remember(r) { mutableStateOf(r.headDimensions?.bearingDiaDw?.let { f(it) } ?: "") }
    var dh by remember(r) { mutableStateOf(QualityEngine.clearanceHoleMedium(r.nominalDiaMm)?.let { f(it, 1) } ?: "") }
    var muG by rememberSaveable { mutableStateOf("0.12") }
    var muK by rememberSaveable { mutableStateOf("0.12") }
    var nu by rememberSaveable { mutableStateOf("0.9") }
    QCard("Tightening torque and preload (VDI 2230)") {
        Note("Torque-controlled tightening to ν × Rp0.2 including thread torsion (VDI 2230 Part 1). Filled from the part: Rp0.2 min of the strength class, bearing diameter dw from the standard, ISO 273 medium clearance hole dh." +
            if (!metric) " This part is inch: the formula is the same in mm; check d and P." else "")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("d (mm)", d, Modifier.weight(1f)) { d = it }
            NumField("P (mm)", p, Modifier.weight(1f)) { p = it }
            NumField("Rp0.2 (MPa)", rp, Modifier.weight(1f)) { rp = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("dw (mm)", dw, Modifier.weight(1f)) { dw = it }
            NumField("dh (mm)", dh, Modifier.weight(1f)) { dh = it }
            NumField("ν", nu, Modifier.weight(1f)) { nu = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("µ thread", muG, Modifier.weight(1f)) { muG = it }
            NumField("µ head", muK, Modifier.weight(1f)) { muK = it }
        }
        Note("Use the friction window of your coating / lubricant specification; friction is the biggest source of preload scatter. Measure it per ISO 16047.")
    }
    val t = listOf(d, p, rp, muG, muK, dw, dh, nu).map { it.num() }.let { v ->
        if (v.any { it == null }) null else QualityEngine.torqueTension(v[0]!!, v[1]!!, v[2]!!, v[3]!!, v[4]!!, v[5]!!, v[6]!!, v[7]!!)
    }
    if (t == null) { Note("Check the inputs: dw must be larger than dh, ν between 0 and 1."); return }
    QCard("Result") {
        KV("Assembly preload FM", "${f(t.preloadKn, 1)} kN", SuccessGreen)
        KV("Tightening torque MA", "${f(t.torqueNm, 1)} N·m", SuccessGreen)
        KV("Nut factor K = MA / (FM·d)", f(t.nutFactorK, 3))
        KV("Torque split", "thread ${f(t.threadFrictionPct, 0)} %, head ${f(t.headFrictionPct, 0)} %, pitch ${f(t.pitchPct, 0)} %")
        KV("d2 / d3 / As", "${f(t.d2Mm, 3)} / ${f(t.d3Mm, 3)} mm / ${f(t.stressAreaMm2, 1)} mm²")
        KV("Dkm (head friction diameter)", "${f(t.dkmMm, 2)} mm")
        if (r.proofLoadKn > 0 && !r.fastenerType.isNut) KV("Proof load of this part", "${f(r.proofLoadKn, 1)} kN (FM is ${f(t.preloadKn / r.proofLoadKn * 100, 0)} % of it)")
        Note("Checked against VDI 2230 Table A1: M10, µ = 0.12 gives FM 29.6 / 43.4 / 50.8 kN for 8.8 / 10.9 / 12.9. The tightening tool's scatter (αA) and embedding losses are not included: size the joint with full VDI 2230 for safety parts.")
    }
}

// ---------------- Coating and tests ----------------

@Composable
private fun CoatingSection(r: FastenerCalculationResult) {
    val c = LocalCustomColors.current
    QCard("Coating thickness verification") {
        KV("Coating", r.coating.label)
        KV("Specification", r.coating.specStandard)
        if (r.prePlatePitchAllowanceUm > 0) KV("Thread allowance used", "${f(r.prePlatePitchAllowanceUm, 1)} µm on pitch diameter")
        QualityDocs.coatingThicknessMethods(r.coating).forEach { (k, v) -> KV(k, v) }
        Note("Measure on the reference area given by the coating standard or the drawing; threads and recesses read low or high on most gauges.")
    }
    QCard("Corrosion and embrittlement") {
        KV("Salt spray", "ISO 9227 NSS / ASTM B117")
        KV("Requirement", "Hours to white and red corrosion: from your drawing or customer coating spec")
        KV("Hydrogen risk", if (r.hasHydrogenEmbrittlementRisk) "YES: bake ${r.heatTreatmentSpec.bakingTempC}, ${r.heatTreatmentSpec.bakingSoakTime}" else "No bake required for this part",
            if (r.hasHydrogenEmbrittlementRisk) AlertRed else SuccessGreen)
        if (r.hasHydrogenEmbrittlementRisk) KV("Verification", "ISO 15330 sustained load test; ASTM F1940 process control")
    }
    QCard("Mechanical and other tests") {
        QualityDocs.TEST_METHODS.forEach { (k, v) -> KV(k, v) }
    }
    ShareButton("Share test plan", "Test plan ${r.threadLabel}") {
        buildString {
            append("TEST PLAN ${r.fastenerType.label} ${r.threadLabel} x ${f(r.lengthMm, 1)} mm\n\n")
            append("Coating: ${r.coating.label} (${r.coating.specStandard})\n")
            QualityDocs.coatingThicknessMethods(r.coating).forEach { (k, v) -> append(" $k: $v\n") }
            append("Salt spray: ISO 9227 NSS / ASTM B117, hours per drawing / customer spec\n")
            if (r.hasHydrogenEmbrittlementRisk) append("Hydrogen: bake ${r.heatTreatmentSpec.bakingTempC}, ${r.heatTreatmentSpec.bakingSoakTime}; ISO 15330 / ASTM F1940\n")
            append('\n')
            QualityDocs.TEST_METHODS.forEach { (k, v) -> append("$k: $v\n") }
        }
    }
}

// ---------------- GD&T and inspection ----------------

@Composable
private fun InspectionSection() {
    val c = LocalCustomColors.current
    QCard("GD&T on fasteners (ISO 1101 / ASME Y14.5)") {
        Note("Typical geometric tolerances on fasteners and how they are checked. Values come from the drawing or ISO 4759-1 for the product grade.")
        QualityDocs.GDT.forEach { (sym, feature, method) ->
            Column {
                Text("$sym — $feature", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.textMain)
                Text(method, fontSize = 10.sp, color = c.textMuted)
            }
        }
    }
    QCard("Reading a customer drawing") {
        listOf(
            "Title block: material, strength class, coating, standard, drawing revision and general tolerances",
            "Ballooned characteristics: every dimension, note and spec gets a number for the dimensional report",
            "Special characteristic symbols: these go to the control plan with tighter controls",
            "Datums: inspect from the datum the drawing names (usually the thread axis), not from a convenient face",
            "Notes: hydrogen bake, thread locking, cleanliness, packaging and customer specifications"
        ).forEach { Text("• $it", fontSize = 11.sp, color = c.textMain) }
        Note("The AI Scanner tab reads dimensions from a photo of the drawing; check every value against the drawing.")
    }
    QCard("Measurement system") {
        KV("Gauge R&R", "< 10 % acceptable; 10–30 % may be acceptable; > 30 % not acceptable (AIAG MSA)")
        KV("Distinct categories", "ndc ≥ 5")
        KV("Resolution", "Gauge resolution at most 1/10 of the tolerance")
    }
}

// ---------------- Lean / Six Sigma ----------------

@Composable
private fun LeanSection() {
    val c = LocalCustomColors.current
    var planned by rememberSaveable { mutableStateOf("480") }
    var down by rememberSaveable { mutableStateOf("") }
    var cycle by rememberSaveable { mutableStateOf("") }
    var total by rememberSaveable { mutableStateOf("") }
    var good by rememberSaveable { mutableStateOf("") }
    QCard("OEE (Overall Equipment Effectiveness)") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Planned (min)", planned, Modifier.weight(1f)) { planned = it }
            NumField("Downtime (min)", down, Modifier.weight(1f)) { down = it }
            NumField("Ideal cycle (s/pc)", cycle, Modifier.weight(1f)) { cycle = it }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Total parts", total, Modifier.weight(1f)) { total = it }
            NumField("Good parts", good, Modifier.weight(1f)) { good = it }
        }
        val o = listOf(planned, down.ifBlank { "0" }, cycle, total, good).map { it.num() }.let { v ->
            if (v.any { it == null }) null else QualityEngine.oee(v[0]!!, v[1]!!, v[2]!!, v[3]!!, v[4]!!)
        }
        if (o == null) Note("Enter planned time, ideal cycle time and part counts (good ≤ total).")
        else {
            KV("Availability", "${f(o.availability * 100, 1)} %")
            KV("Performance", "${f(o.performance * 100, 1)} %", if (o.performance > 1.0) AlertRed else c.textMain)
            KV("Quality", "${f(o.quality * 100, 1)} %")
            KV("OEE", "${f(o.oee * 100, 1)} %", SuccessGreen)
            if (o.performance > 1.0) Note("Performance above 100 %: the ideal cycle time is set too slow.")
        }
    }
    var defects by rememberSaveable { mutableStateOf("") }
    var units by rememberSaveable { mutableStateOf("") }
    var opp by rememberSaveable { mutableStateOf("1") }
    QCard("Sigma level") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Defects", defects, Modifier.weight(1f)) { defects = it }
            NumField("Units", units, Modifier.weight(1f)) { units = it }
            NumField("Opportunities / unit", opp, Modifier.weight(1f)) { opp = it }
        }
        val dpmo = listOf(defects, units, opp).map { it.num() }.let { v -> if (v.any { it == null }) null else QualityEngine.dpmo(v[0]!!, v[1]!!, v[2]!!) }
        if (dpmo != null) {
            KV("DPMO / PPM", f(dpmo, 0))
            KV("Sigma level (1.5 σ shift)", QualityEngine.sigmaLevel(dpmo)?.let { f(it, 2) } ?: if (dpmo <= 0) "no defects" else "–", SuccessGreen)
        }
    }
    QCard("DMAIC") { QualityDocs.DMAIC.forEach { (k, v) -> KV(k, v) } }
    QCard("8 wastes in a fastener plant") { QualityDocs.WASTES.forEach { (k, v) -> KV(k, v) } }
    QCard("Error-proofing") { Note("Poka-yoke checks for this part are on the Sizing tab and in the 8D / PFMEA tab.") }
}

// ---------------- Engineering change ----------------

@Composable
private fun EcnSection(r: FastenerCalculationResult, csr: String) {
    val c = LocalCustomColors.current
    var number by rememberSaveable { mutableStateOf("") }
    var fromRev by rememberSaveable { mutableStateOf("") }
    var toRev by rememberSaveable { mutableStateOf("") }
    var desc by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    val triggers = remember { mutableStateOf(setOf<Int>()) }
    QCard("Engineering change notice") {
        TextIn("ECN number", number) { number = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextIn("From revision", fromRev, Modifier.weight(1f)) { fromRev = it }
            TextIn("To revision", toRev, Modifier.weight(1f)) { toRev = it }
        }
        TextIn("What changes", desc, lines = 2) { desc = it }
        TextIn("Why", reason, lines = 2) { reason = it }
    }
    QCard("Does the change need customer notification / PPAP?") {
        Note("Cases from AIAG PPAP section 3.1. Tick every one that applies; your customer's requirements can add more.")
        QualityDocs.CHANGE_TRIGGERS.forEachIndexed { i, t ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable { triggers.value = if (i in triggers.value) triggers.value - i else triggers.value + i }) {
                Checkbox(checked = i in triggers.value, onCheckedChange = { triggers.value = if (it) triggers.value + i else triggers.value - i })
                Text(t, fontSize = 11.sp, color = c.textMain, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Text(if (triggers.value.isEmpty()) "No notification case ticked" else "Customer notification and PPAP needed before shipping",
            fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (triggers.value.isEmpty()) SuccessGreen else AlertRed)
    }
    ShareButton("Share change notice", "ECN ${number.ifBlank { r.threadLabel }}") { QualityDocs.ecn(r, number, fromRev, toRev, desc, reason, triggers.value, csr) }
}

// ---------------- Standards ----------------

@Composable
private fun StandardsSection(r: FastenerCalculationResult) {
    val c = LocalCustomColors.current
    QCard("This part") {
        KV("Product standard", "${r.standard.code} (${r.standard.label})")
        KV("Thread", "${r.threadLabel} ${r.threadClass}")
        KV("Strength", r.tensileRange?.label ?: r.propertyClass?.label ?: "per material spec")
        KV("Coating", r.coating.specStandard)
        if (QualityDocs.hexBoltTolerances(r).isNotEmpty()) KV("Product grade (ISO 4759-1)", if (QualityDocs.productGradeA(r)) "A" else "B")
    }
    QCard("JIS ↔ ISO equivalents") {
        QualityDocs.JIS_EQUIVALENTS.forEach { (jis, iso) -> KV(jis, iso) }
        Note("Current JIS fastener standards adopt the ISO text. Old JIS hex sizes (JIS B 1180 annex) differ in across flats:")
        QualityDocs.JIS_OLD_ACROSS_FLATS.forEach { (d, jis, iso) ->
            KV("M${d.toInt()}", "old JIS s = ${jis.toInt()} mm, ISO s = ${iso.toInt()} mm", if (d == r.nominalDiaMm) AlertRed else c.textMain)
        }
    }
    QCard("Process sequence for this part") {
        ProductionReport.processSequence(r).forEachIndexed { i, s -> Text("${(i + 1) * 10}. $s", fontSize = 11.sp, color = c.textMain) }
    }
}
