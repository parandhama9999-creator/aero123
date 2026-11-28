package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import kotlinx.serialization.json.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * End-to-end check of the app's engine path: every golden part (inputs and answers recorded from the original
 * Kotlin engine, engine-rs/golden/cases.json.gz) is calculated through the app's own Kotlin functions, so each
 * result goes Kotlin → Rust → Kotlin types, and is compared field by field with the recorded answer.
 */
class RustEngineParityTest {
    private val json = NativeEngine.json

    private fun golden(): JsonArray {
        val f = generateSequence(File("").absoluteFile) { it.parentFile }.map { File(it, "engine-rs/golden/cases.json.gz") }.first { it.exists() }
        return json.parseToJsonElement(GZIPInputStream(f.inputStream()).bufferedReader().readText()).jsonArray
    }

    private fun d(e: JsonElement?): Double? = (e as? JsonPrimitive)?.doubleOrNull
    private fun s(e: JsonElement?): String? = (e as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun calc(c: JsonObject): FastenerCalculationResult {
        val i = c["input"]!!.jsonObject
        return CalculationEngine.calculate(
            fastenerType = FastenerType.valueOf(s(i["type"])!!), unitSystem = UnitSystem.valueOf(s(i["unit"])!!),
            standard = ProductStandard.valueOf(s(c["standard"])!!), nominalSizeIndex = i["sizeIndex"]!!.jsonPrimitive.int,
            pitchValue = d(i["pitch"])!!, lengthMm = d(i["length"])!!, threadClass = s(i["threadClass"])!!,
            material = FastenerCatalog.MATERIALS.getValue(s(i["materialKey"])!!), coating = CoatingType.valueOf(s(i["coating"])!!),
            batchQty = i["batch"]!!.jsonPrimitive.int, currencyIsUsd = i["currencyIsUsd"]!!.jsonPrimitive.boolean,
            cncHourRateInr = d(i["cncRate"])!!, htRatePerKgInr = d(i["htRate"])!!, marginPct = d(i["margin"])!!,
            customNominalDiaMm = d(i["customDia"]), extractedRadius = d(i["radius"]),
            propertyClass = s(i["propertyClass"])?.let { PropertyClass.valueOf(it) },
            drawing = json.decodeFromJsonElement(i["drawing"]!!),
            tensileRange = TensileRange.of(d(i["tensileMin"]), d(i["tensileMax"])), threadLockPatch = i["patch"]!!.jsonPrimitive.boolean,
            millCert = MillCert.of(d(i["millRm"]), d(i["millHrb"])), wireDiaOverrideMm = d(i["wireOverride"]),
            openExtrusionLimitPct = d(i["openLimit"])!!, headerChoice = HeaderChoice.valueOf(s(i["header"])!!)
        )
    }

    private val machine = MachineSetup(dieCaseOdMm = 40.0, dieCaseLengthMm = 60.0, punchCaseOdMm = 35.0, punchCaseLengthMm = 50.0,
        koStrokeMm = 30.0, insertOdMm = 20.0, openExtrusionLimitPct = 25.0, dieAngleDeg = 30.0, landLengthMm = 2.0)

    private inline fun <reified T> j(v: T): JsonElement = json.encodeToJsonElement(v)

    private fun derived(r: FastenerCalculationResult, want: Set<String>): Map<String, JsonElement> {
        val out = linkedMapOf<String, JsonElement>()
        fun put(k: String, f: () -> JsonElement) { if (k in want) out[k] = f() }
        val m = r.material
        put("processSequence") { j(ProductionReport.processSequence(r)) }
        put("quenchTemper") { j(HeatTreatCycleEngine.quenchTemper(m, r.wireDiaMm, r.heatTreatmentSpec)) }
        put("precipitationHarden") { j(HeatTreatCycleEngine.precipitationHarden(m, r.wireDiaMm)) }
        put("softAnnealCycle") { j(HeatTreatCycleEngine.softAnneal(m, r.wireDiaMm)) }
        put("spheroidize") { j(AnnealCycleEngine.spheroidize(m, AnnealCycleEngine.DEFAULT_CHARGE_TONNES, r.wireDiaMm)) }
        put("materialFamily") { j(PokaYokeEngine.materialFamily(m)) }
        put("stainlessType") { j(PokaYokeEngine.stainlessType(m)) }
        put("isWarmHeaded") { j(CalculationEngine.isWarmHeaded(m, r.nominalDiaMm)) }
        put("wireCheckIssues") { j(MillCertCheck.issues(r.wireCheck)) }
        put("hexBoltTolerances") { j(QualityDocs.hexBoltTolerances(r)) }
        put("specialCharacteristics") { j(QualityDocs.specialCharacteristics(r)) }
        put("capabilityCharacteristics") { j(QualityDocs.capabilityCharacteristics(r)) }
        put("coatingMethods") { j(QualityDocs.coatingThicknessMethods(r.coating)) }
        put("productionSummary") { j(ProductionReport.summary(r)) }
        put("productionReport") { j(ProductionReport.build(r, null, "2026-01-01 00:00")) }
        put("toolingSheet") { j(ToolingDesign.build(r, MachineSetup())) }
        put("toolingText") { j(ToolingDesign.build(r, MachineSetup())?.let { ToolingDesign.text(r, it, MachineSetup()) }) }
        put("toolingSheetMachine") { j(ToolingDesign.build(r, machine)) }
        put("extrusions") { j(r.headingPlan?.let { ToolingDesign.extrusions(it) }) }
        put("controlPlan") { j(QualityDocs.controlPlan(r)) }
        put("apqp") { j(QualityDocs.apqp(r)) }
        put("ppap") { j(QualityDocs.ppapElements(r)) }
        put("psw") { j(QualityDocs.psw(r, "PN-1", "B", "OEM", 3, QualityDocs.PSW_REASONS[1], "Spec A\nSpec B")) }
        put("ecn") { j(QualityDocs.ecn(r, "ECN-1", "A", "B", "desc", "why", setOf(0, 4), "Spec A")) }
        put("rawMaterialPlan") {
            j(RawMaterialPlanner.plan(r.grossWeightGrams / m.density * 1000.0, m.density, r.batchQty,
                RawMaterialPlanner.suggestedDiameters(r.blankTargetDia, r.wireDiaMm), r.headVolumeMm3, r.wireDiaMm, r.blankTargetDia))
        }
        put("anim") {
            r.headingPlan?.let { pl ->
                JsonArray(listOf(0.0, 0.5, 1.7, 3.0, 5.5, 20.0).map { t ->
                    val f = HeadingAnimation.frameAt(pl, t)
                    buildJsonObject { put("frame", j(f)); put("outline", j(HeadingAnimation.outline(pl, f, 16))) }
                } + buildJsonObject { put("duration", HeadingAnimation.durationSec(pl)); put("blows", HeadingAnimation.blowCount(pl)) })
            } ?: JsonNull
        }
        return out
    }

    /** Kotlin writes a MaterialInfo inside a golden case as its key; the app's objects carry the whole record. */
    private fun materialAsKey(e: JsonElement): JsonElement = when (e) {
        is JsonObject -> if (e.containsKey("heatTreatmentSpec") && e.containsKey("flowStressCoeffK") && e["key"] is JsonPrimitive) e["key"]!!
            else JsonObject(e.mapValues { materialAsKey(it.value) })
        is JsonArray -> JsonArray(e.map { materialAsKey(it) })
        else -> e
    }

    private fun compare(path: String, g: JsonElement, r: JsonElement, out: MutableList<String>) {
        when {
            g is JsonPrimitive && r is JsonPrimitive && !g.isString && !r.isString && g.doubleOrNull != null && r.doubleOrNull != null -> {
                val a = g.double; val b = r.double
                if (a != b && kotlin.math.abs(a - b) > 1e-9 * maxOf(1.0, kotlin.math.abs(a), kotlin.math.abs(b))) out += "$path: $a vs $b"
            }
            g is JsonPrimitive && g.isString && (g.content == "NaN" || "Infinity" in g.content) && (r == JsonNull || r is JsonPrimitive) -> {}
            g is JsonObject && r is JsonObject -> {
                for ((k, v) in g) { val rv = r[k]; if (rv == null) out += "$path.$k missing" else compare("$path.$k", v, rv, out) }
                for (k in r.keys) if (k !in g) out += "$path.$k extra"
            }
            g is JsonArray && r is JsonArray -> {
                if (g.size != r.size) out += "$path size ${g.size} vs ${r.size}"
                g.zip(r).forEach { (x, y) -> compare("$path[]", x, y, out) }
            }
            g != r -> out += "$path: ${g.toString().take(200)} vs ${r.toString().take(200)}"
        }
    }

    @Test fun appGivesTheGoldenAnswersThroughTheRustEngine() {
        val cases = golden()
        val diffs = sortedMapOf<String, Int>()
        var first = ""
        for (c in cases.map { it.jsonObject }) {
            val r = calc(c)
            val out = mutableListOf<String>()
            compare("result", c["result"]!!, materialAsKey(j(r)), out)
            val gd = c["derived"]!!.jsonObject
            val rd = derived(r, gd.keys)
            for ((k, v) in gd) compare("derived.$k", v, materialAsKey(rd[k] ?: JsonPrimitive("NOT PORTED")), out)
            for (o in out) {
                val key = o.substringBefore(":").substringBefore(" ")
                diffs[key] = (diffs[key] ?: 0) + 1
                if (first.isEmpty()) first = "case ${c["id"]}: $o"
            }
        }
        println("${cases.size} golden cases; differing fields: $diffs; first: $first")
        assertTrue("differences from the golden answers: $diffs\nfirst: $first", diffs.isEmpty())
    }
}
