package com.aistudio.aerofast.trmst.golden

import com.aistudio.aerofast.trmst.data.*
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier
import java.util.IdentityHashMap
import java.util.zip.GZIPOutputStream

/**
 * Writes the Kotlin engine's catalogue data and its results for a grid of parts as JSON, so the Rust engine
 * (engine-rs) can be generated from the same data and checked against the same answers.
 * Run with GOLDEN_DIR=<dir>; skipped otherwise.
 */
class GoldenExport {

    // ---------- reflective JSON ----------
    private class Json(val materialAsKey: Boolean = false) {
        val sb = StringBuilder()
        private val active = IdentityHashMap<Any, Boolean>()

        fun str(s: String) {
            sb.append('"')
            for (c in s) when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' -> sb.append(String.format("\\u%04x", c.code))
                else -> sb.append(c)
            }
            sb.append('"')
        }

        fun num(d: Double) {
            if (d.isNaN() || d.isInfinite()) str(d.toString()) else sb.append(d.toString())
        }

        fun value(v: Any?, depth: Int = 0) {
            when (v) {
                null -> sb.append("null")
                is String -> str(v)
                is Double -> num(v)
                is Float -> num(v.toDouble())
                is Int, is Long, is Short, is Byte -> sb.append(v.toString())
                is Boolean -> sb.append(v.toString())
                is Char -> str(v.toString())
                is Enum<*> -> str(v.name)
                is Regex -> str(v.pattern)
                is Map<*, *> -> {
                    sb.append('{')
                    var first = true
                    for ((k, x) in v) {
                        if (!first) sb.append(','); first = false
                        str(if (k is Enum<*>) k.name else k.toString()); sb.append(':'); value(x, depth + 1)
                    }
                    sb.append('}')
                }
                is Iterable<*> -> {
                    sb.append('['); var first = true
                    for (x in v) { if (!first) sb.append(','); first = false; value(x, depth + 1) }
                    sb.append(']')
                }
                is DoubleArray -> value(v.toList(), depth)
                is IntArray -> value(v.toList(), depth)
                is Array<*> -> value(v.toList(), depth)
                is Function<*> -> str("<fn>")
                is MaterialInfo -> if (materialAsKey) str(v.key) else obj(v, depth)
                else -> obj(v, depth)
            }
        }

        fun fields(c: Class<*>): List<java.lang.reflect.Field> {
            val out = mutableListOf<java.lang.reflect.Field>()
            var k: Class<*>? = c
            while (k != null && k != Any::class.java && k != Enum::class.java) {
                out.addAll(0, k.declaredFields.filter { f ->
                    !Modifier.isStatic(f.modifiers) && !f.isSynthetic && !f.name.contains('$')
                })
                k = k.superclass
            }
            return out
        }

        fun obj(v: Any, depth: Int) {
            val cn = v.javaClass.name
            if (cn.startsWith("androidx.") || cn.startsWith("kotlinx.") || depth > 40 || active.containsKey(v)) { sb.append("null"); return }
            active[v] = true
            sb.append('{')
            var first = true
            for (f in fields(v.javaClass)) {
                f.isAccessible = true
                if (!first) sb.append(','); first = false
                str(f.name); sb.append(':'); value(f.get(v), depth + 1)
            }
            sb.append('}')
            active.remove(v)
        }
    }

    private fun json(v: Any?): String = Json().apply { value(v) }.sb.toString()

    private fun enumTable(values: Array<out Enum<*>>): Map<String, Any?> {
        val j = Json()
        return linkedMapOf("variants" to values.map { e ->
            val m = linkedMapOf<String, Any?>("name" to e.name)
            for (f in j.fields(e.javaClass)) { f.isAccessible = true; m[f.name] = f.get(e) }
            m
        })
    }

    // ---------- data export ----------
    private fun exportData(dir: File) {
        val enums = linkedMapOf<String, Any?>()
        listOf(
            FastenerType.values(), UnitSystem.values(), ProductStandard.values(), PropertyClass.values(), CoatingType.values(),
            GaugeStatus.values(), SolidShape.values(), CutoutFrom.values(), VolumeBasis.values(), SectionShape.values(),
            HeaderRoute.values(), HeaderChoice.values(), GripStatus.values(), WireVerdict.values(), DimSource.values(),
            HeadStyle.values(), PokaYokeSeverity.values(), MaterialFamily.values(), PokaYokeEngine.StainlessType.values(),
            MaterialGroup.values(), ClassGroup.values(), AnnealRoute.values(), ToolBasis.values()
        ).forEach { enums[it.first().javaClass.let { c -> if (c.isAnonymousClass) c.superclass else c }.simpleName] = enumTable(it) }
        File(dir, "enums.json").writeText(json(enums))
        File(dir, "materials.json").writeText(json(FastenerCatalog.DEFAULT_MATERIALS.values.toList()))
        // Every Kotlin object's tables (private ones included): the Rust port is generated from these
        val objects = linkedMapOf<String, Any?>()
        val pkg = File("build/generated/shared/com/aistudio/aerofast/trmst/data")
        pkg.listFiles()!!.map { it.nameWithoutExtension }.sorted().forEach { file ->
            Regex("""(?m)^(?:internal |private )?object (\w+)""").findAll(File(pkg, "$file.kt").readText()).forEach { m ->
                val name = m.groupValues[1]
                val cls = runCatching { Class.forName("com.aistudio.aerofast.trmst.data.$name") }.getOrNull() ?: return@forEach
                val inst = cls.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null)
                val j = Json()
                val fields = linkedMapOf<String, Any?>()
                for (f in cls.declaredFields) {
                    if (f.name == "INSTANCE" || f.name.contains('$')) continue
                    f.isAccessible = true
                    val v = if (Modifier.isStatic(f.modifiers)) f.get(null) else f.get(inst)
                    if (v != null && v.javaClass.name.startsWith("androidx.")) continue
                    fields[f.name] = v
                }
                objects[name] = fields
            }
        }
        File(dir, "objects.json").writeText(json(objects))
    }

    // ---------- golden cases ----------
    data class Input(
        val type: FastenerType, val unit: UnitSystem = UnitSystem.METRIC, val standard: ProductStandard? = null,
        val sizeIndex: Int, val pitch: Double, val length: Double, val threadClass: String,
        val materialKey: String = "10B21", val coating: CoatingType = CoatingType.ZINC_BLUE, val batch: Int = 5000,
        val currencyIsUsd: Boolean = false, val cncRate: Double = 750.0, val htRate: Double = 180.0, val margin: Double = 22.0,
        val customDia: Double? = null, val radius: Double? = null, val propertyClass: PropertyClass? = null,
        val drawing: DrawingGeometry = DrawingGeometry(), val tensileMin: Double? = null, val tensileMax: Double? = null,
        val patch: Boolean = false, val millRm: Double? = null, val millHrb: Double? = null, val wireOverride: Double? = null,
        val openLimit: Double = HeadingProgression.MAX_DIE_EXTRUSION_PCT, val header: HeaderChoice = HeaderChoice.AUTO,
        val tag: String = "",
        /** Full derived outputs (reports, tooling, quality documents, animation) only on representative cases. */
        val full: Boolean = false
    ) {
        val std get() = standard ?: PokaYokeEngine.defaultStandard(type, unit)
        fun run() = CalculationEngine.calculate(
            fastenerType = type, unitSystem = unit, standard = std, nominalSizeIndex = sizeIndex, pitchValue = pitch,
            lengthMm = length, threadClass = threadClass, material = FastenerCatalog.MATERIALS.getValue(materialKey),
            coating = coating, batchQty = batch, currencyIsUsd = currencyIsUsd, cncHourRateInr = cncRate,
            htRatePerKgInr = htRate, marginPct = margin, customNominalDiaMm = customDia, extractedRadius = radius,
            propertyClass = propertyClass, drawing = drawing, tensileRange = TensileRange.of(tensileMin, tensileMax),
            threadLockPatch = patch, millCert = MillCert.of(millRm, millHrb), wireDiaOverrideMm = wireOverride,
            openExtrusionLimitPct = openLimit, headerChoice = header
        )
    }

    private fun metric(d: Double) = FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == d }
    private fun inch(dInch: Double) = FastenerCatalog.INCH_SIZES.indexOfFirst { kotlin.math.abs(it.dInch - dInch) < 1e-6 }

    private fun defaultInput(type: FastenerType, unit: UnitSystem, sizeIndex: Int, length: Double? = null): Input {
        val pitch = if (unit == UnitSystem.METRIC) FastenerCatalog.METRIC_SIZES[sizeIndex].pitches.first()
            else FastenerCatalog.INCH_SIZES[sizeIndex].tpis.first().toDouble()
        val d = FastenerCatalog.diameterMm(unit, sizeIndex)
        val pc = when {
            type.isWasher -> null
            type.isNut -> if (unit == UnitSystem.METRIC) PropertyClass.NUT_8 else PropertyClass.SAE_NUT_5
            else -> if (unit == UnitSystem.METRIC) PropertyClass.CLASS_8_8 else PropertyClass.SAE_GRADE_5
        }
        return Input(type = type, unit = unit, sizeIndex = sizeIndex, pitch = pitch, length = length ?: (4 * d).coerceAtLeast(6.0),
            threadClass = ThreadClassCatalog.getOptions(unit, type.isNut).first(), propertyClass = pc)
    }

    private fun cases(): List<Input> {
        val out = mutableListOf<Input>()
        val m6 = metric(6.0); val m10 = metric(10.0); val m12 = metric(12.0); val m8 = metric(8.0)
        // every fastener type, two metric sizes and one inch size
        for (t in FastenerType.values()) {
            out += defaultInput(t, UnitSystem.METRIC, m6).copy(tag = "type-m6")
            out += defaultInput(t, UnitSystem.METRIC, m12).copy(tag = "type-m12", full = true)
            out += defaultInput(t, UnitSystem.INCH, inch(0.375)).copy(tag = "type-inch")
        }
        // every material on a hex bolt with the first class it allows
        val bolt = defaultInput(FastenerType.HEX_BOLT, UnitSystem.METRIC, m10, 50.0)
        for (m in FastenerCatalog.DEFAULT_MATERIALS.values) {
            val pc = SelectionFilter.propertyClasses(FastenerType.HEX_BOLT, UnitSystem.METRIC).firstOrNull { SelectionFilter.classAllows(it, m) }
                ?: PropertyClass.MATERIAL_SPEC
            out += bolt.copy(materialKey = m.key, propertyClass = pc, tag = "material")
            out += bolt.copy(materialKey = m.key, propertyClass = pc, coating = CoatingType.PASSIVATION, type = FastenerType.SHCS,
                standard = PokaYokeEngine.defaultStandard(FastenerType.SHCS, UnitSystem.METRIC), sizeIndex = m6, pitch = 1.0, length = 25.0, tag = "material-shcs")
        }
        // every property class at its default material (nut classes on a nut)
        for (pc in PropertyClass.values()) {
            val nut = pc.name.startsWith("NUT_") || pc.name.contains("NUT") || pc.name.startsWith("ASTM_A194") || pc.name.startsWith("ASTM_A563")
            val base = if (nut) defaultInput(FastenerType.HEX_NUT, UnitSystem.METRIC, m10) else bolt
            out += base.copy(propertyClass = pc, materialKey = pc.defaultMatKey, tag = "class")
        }
        // every coating
        for (c in CoatingType.values()) {
            out += bolt.copy(coating = c, propertyClass = PropertyClass.CLASS_10_9, tag = "coating")
            out += bolt.copy(coating = c, propertyClass = PropertyClass.CLASS_12_9, materialKey = "4140", type = FastenerType.SHCS,
                standard = PokaYokeEngine.defaultStandard(FastenerType.SHCS, UnitSystem.METRIC), length = 40.0, tag = "coating-shcs")
        }
        // every thread class
        for (tc in ThreadClassCatalog.METRIC_EXTERNAL) out += defaultInput(FastenerType.HEX_BOLT, UnitSystem.METRIC, m8, 40.0).copy(threadClass = tc, tag = "thread")
        for (tc in ThreadClassCatalog.METRIC_INTERNAL) out += defaultInput(FastenerType.HEX_NUT, UnitSystem.METRIC, m8).copy(threadClass = tc, tag = "thread-nut")
        for (tc in ThreadClassCatalog.INCH_EXTERNAL) out += defaultInput(FastenerType.HEX_BOLT, UnitSystem.INCH, inch(0.5), 50.8).copy(threadClass = tc, tag = "thread-inch")
        for (tc in ThreadClassCatalog.INCH_INTERNAL) out += defaultInput(FastenerType.HEX_NUT, UnitSystem.INCH, inch(0.5)).copy(threadClass = tc, tag = "thread-inch-nut")
        // every size, every pitch on hex bolts; inch sizes coarse and fine
        FastenerCatalog.METRIC_SIZES.forEachIndexed { i, s ->
            s.pitches.forEach { p -> out += defaultInput(FastenerType.HEX_BOLT, UnitSystem.METRIC, i).copy(pitch = p, tag = "size") }
        }
        FastenerCatalog.INCH_SIZES.forEachIndexed { i, s ->
            s.tpis.forEach { t -> out += defaultInput(FastenerType.HEX_BOLT, UnitSystem.INCH, i).copy(pitch = t.toDouble(), tag = "size-inch") }
        }
        // lengths
        for (l in listOf(8.0, 12.0, 20.0, 30.0, 45.0, 80.0, 120.0, 160.0, 240.0)) {
            out += bolt.copy(length = l, tag = "length")
            out += bolt.copy(type = FastenerType.HEX_BOLT_FULL, standard = PokaYokeEngine.defaultStandard(FastenerType.HEX_BOLT_FULL, UnitSystem.METRIC), length = l, tag = "length-full")
        }
        // header choices on several parts
        val headerParts = listOf(
            bolt.copy(length = 60.0), defaultInput(FastenerType.SHCS, UnitSystem.METRIC, m8, 30.0),
            defaultInput(FastenerType.HEX_NUT, UnitSystem.METRIC, m12), defaultInput(FastenerType.FLANGE_BOLT, UnitSystem.METRIC, m10, 40.0),
            defaultInput(FastenerType.TWELVE_POINT, UnitSystem.METRIC, m6, 30.0).copy(materialKey = "INCONEL718", propertyClass = PropertyClass.AERO_1380),
            defaultInput(FastenerType.PAN_CROSS, UnitSystem.METRIC, metric(4.0), 12.0), defaultInput(FastenerType.CARRIAGE_BOLT, UnitSystem.METRIC, m8, 40.0)
        )
        for (p in headerParts) for (h in HeaderChoice.values()) {
            out += p.copy(header = h, tag = "header", full = true)
            out += p.copy(header = h, openLimit = 20.0, tag = "header-open20")
        }
        val specialsFrom = out.size
        // special inputs
        out += bolt.copy(tensileMin = 1000.0, tensileMax = 1240.0, tag = "tensile")
        out += bolt.copy(tensileMin = 800.0, tensileMax = null, tag = "tensile-min")
        out += bolt.copy(millRm = 520.0, millHrb = 80.0, tag = "millcert")
        out += bolt.copy(millRm = 900.0, millHrb = null, tag = "millcert-hard")
        out += bolt.copy(millRm = null, millHrb = 60.0, tag = "millcert-soft")
        out += bolt.copy(wireOverride = 9.5, tag = "wire")
        out += bolt.copy(wireOverride = 12.0, tag = "wire-big")
        out += bolt.copy(patch = true, tag = "patch")
        out += bolt.copy(radius = 0.1, tag = "radius")
        out += bolt.copy(customDia = 9.7, tag = "customdia")
        out += bolt.copy(currencyIsUsd = true, cncRate = 900.0, htRate = 200.0, margin = 30.0, batch = 250, tag = "quote")
        out += bolt.copy(batch = 0, tag = "batch0")
        out += bolt.copy(drawing = DrawingGeometry(threadLengthMm = 30.0, acrossFlatsMm = 17.0, headHeightMm = 7.0), tag = "drawing")
        out += defaultInput(FastenerType.FLANGE_BOLT, UnitSystem.METRIC, m10, 40.0).copy(
            drawing = DrawingGeometry(acrossFlatsMm = 15.0, headHeightMm = 10.0, flangeDiaMm = 22.0, flangeThicknessMm = 2.5), tag = "drawing-flange")
        out += defaultInput(FastenerType.SHCS, UnitSystem.METRIC, m10, 40.0).copy(
            drawing = DrawingGeometry(headDiaMm = 16.0, headHeightMm = 10.0, socketSizeMm = 8.0, socketDepthMm = 5.0, shankDiaMm = 9.9), tag = "drawing-shcs")
        out += defaultInput(FastenerType.PAN_SLOT, UnitSystem.METRIC, metric(5.0), 16.0).copy(
            drawing = DrawingGeometry(headDiaMm = 9.5, headHeightMm = 3.0, headDomeRadiusMm = 8.0, headEdgeHeightMm = 1.2), tag = "drawing-pan")
        out += defaultInput(FastenerType.HEX_NUT, UnitSystem.METRIC, m12).copy(drawing = DrawingGeometry(acrossFlatsMm = 19.0, nutHeightMm = 11.0), tag = "drawing-nut")
        out += defaultInput(FastenerType.FLAT_WASHER, UnitSystem.METRIC, m12).copy(drawing = DrawingGeometry(washerOuterDiaMm = 26.0, washerInnerDiaMm = 13.0, washerThicknessMm = 2.5), tag = "drawing-washer")
        // superalloys and other aero materials on aero bolts
        for (k in listOf("INCONEL718", "A286", "WASPALOY", "TI_GR5", "300M", "AL7075")) if (FastenerCatalog.MATERIALS.containsKey(k)) {
            out += defaultInput(FastenerType.TWELVE_POINT, UnitSystem.METRIC, m6, 30.0).copy(materialKey = k, propertyClass = PropertyClass.MATERIAL_SPEC, coating = CoatingType.AERO_IVD, tag = "aero")
            out += defaultInput(FastenerType.NAS_CLOSE_TOL_BOLT, UnitSystem.INCH, inch(0.25), 25.4).copy(materialKey = k, propertyClass = PropertyClass.MATERIAL_SPEC, threadClass = "3A-UNJ", tag = "aero-inch")
        }
        for (i in specialsFrom until out.size) out[i] = out[i].copy(full = true)
        // a spread of the material, class and coating cases also gets the full outputs
        out.forEachIndexed { i, c -> if (!c.full && c.tag in setOf("material", "class", "coating", "thread") && i % 5 == 0) out[i] = c.copy(full = true) }
        return out
    }

    private fun derived(r: FastenerCalculationResult, full: Boolean): Map<String, Any?> {
        val d = linkedMapOf<String, Any?>()
        fun put(name: String, f: () -> Any?) { d[name] = runCatching(f).getOrElse { "ERROR: ${it.javaClass.simpleName}: ${it.message}" } }
        put("processSequence") { ProductionReport.processSequence(r) }
        put("quenchTemper") { HeatTreatCycleEngine.quenchTemper(r.material, r.wireDiaMm, r.heatTreatmentSpec) }
        put("precipitationHarden") { HeatTreatCycleEngine.precipitationHarden(r.material, r.wireDiaMm) }
        put("softAnnealCycle") { HeatTreatCycleEngine.softAnneal(r.material, r.wireDiaMm) }
        put("spheroidize") { AnnealCycleEngine.spheroidize(r.material, AnnealCycleEngine.DEFAULT_CHARGE_TONNES, r.wireDiaMm) }
        put("materialFamily") { PokaYokeEngine.materialFamily(r.material) }
        put("stainlessType") { PokaYokeEngine.stainlessType(r.material) }
        put("isWarmHeaded") { CalculationEngine.isWarmHeaded(r.material, r.nominalDiaMm) }
        put("wireCheckIssues") { MillCertCheck.issues(r.wireCheck) }
        put("hexBoltTolerances") { QualityDocs.hexBoltTolerances(r) }
        put("specialCharacteristics") { QualityDocs.specialCharacteristics(r) }
        put("capabilityCharacteristics") { QualityDocs.capabilityCharacteristics(r) }
        put("coatingMethods") { QualityDocs.coatingThicknessMethods(r.coating) }
        if (!full) return d
        put("productionSummary") { ProductionReport.summary(r) }
        put("productionReport") { ProductionReport.build(r, null, "2026-01-01 00:00") }
        put("toolingSheet") { ToolingDesign.build(r, MachineSetup()) }
        put("toolingText") { ToolingDesign.build(r, MachineSetup())?.let { ToolingDesign.text(r, it, MachineSetup()) } }
        put("toolingSheetMachine") { ToolingDesign.build(r, MACHINE) }
        put("extrusions") { r.headingPlan?.let { ToolingDesign.extrusions(it) } }
        put("controlPlan") { QualityDocs.controlPlan(r) }
        put("apqp") { QualityDocs.apqp(r) }
        put("ppap") { QualityDocs.ppapElements(r) }
        put("psw") { QualityDocs.psw(r, "PN-1", "B", "OEM", 3, QualityDocs.PSW_REASONS[1], "Spec A\nSpec B") }
        put("ecn") { QualityDocs.ecn(r, "ECN-1", "A", "B", "desc", "why", setOf(0, 4), "Spec A") }
        put("rawMaterialPlan") {
            RawMaterialPlanner.plan(r.grossWeightGrams / r.material.density * 1000.0, r.material.density, r.batchQty,
                RawMaterialPlanner.suggestedDiameters(r.blankTargetDia, r.wireDiaMm), r.headVolumeMm3, r.wireDiaMm, r.blankTargetDia)
        }
        put("anim") {
            r.headingPlan?.let { pl ->
                listOf(0.0, 0.5, 1.7, 3.0, 5.5, 20.0).map { t -> HeadingAnimation.frameAt(pl, t).let { f -> mapOf("frame" to f, "outline" to HeadingAnimation.outline(pl, f, 16)) } } +
                    listOf(mapOf("duration" to HeadingAnimation.durationSec(pl), "blows" to HeadingAnimation.blowCount(pl)))
            }
        }
        return d
    }

    private val MACHINE = MachineSetup(dieCaseOdMm = 40.0, dieCaseLengthMm = 60.0, punchCaseOdMm = 35.0, punchCaseLengthMm = 50.0,
        koStrokeMm = 30.0, insertOdMm = 20.0, openExtrusionLimitPct = 25.0, dieAngleDeg = 30.0, landLengthMm = 2.0)

    private fun gz(file: File, text: String) = GZIPOutputStream(file.outputStream()).bufferedWriter().use { it.write(text) }

    @Test fun export() {
        val dirName = System.getenv("GOLDEN_DIR") ?: return
        val dir = File(dirName).apply { mkdirs() }
        exportData(dir)
        val all = cases()
        val sb = StringBuilder("[")
        all.forEachIndexed { i, inp ->
            if (i > 0) sb.append(",\n")
            val res = runCatching { inp.run() }
            val case = linkedMapOf<String, Any?>("id" to i, "tag" to inp.tag, "input" to inp,
                "standard" to inp.std, "error" to res.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message}" })
            res.getOrNull()?.let { r -> case["result"] = r; case["derived"] = derived(r, inp.full) }
            sb.append(Json(materialAsKey = true).apply { value(case) }.sb)
        }
        sb.append("]")
        gz(File(dir, "cases.json.gz"), sb.toString())
        println("golden cases: ${all.size}")
    }
}
