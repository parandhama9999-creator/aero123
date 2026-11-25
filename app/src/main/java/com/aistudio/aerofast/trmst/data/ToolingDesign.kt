package com.aistudio.aerofast.trmst.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** The header's own tool pockets, typed by the user from the machine manual. A null field was not entered. */
data class MachineSetup(
    val dieCaseOdMm: Double? = null,
    val dieCaseLengthMm: Double? = null,
    val punchCaseOdMm: Double? = null,
    val punchCaseLengthMm: Double? = null,
    val koStrokeMm: Double? = null,
    /** Carbide insert OD the shop plans to use; the app does not choose it. */
    val insertOdMm: Double? = null,
    /** Shop's open-extrusion limit, % area in one blow; null = the app default (30 %). */
    val openExtrusionLimitPct: Double? = null,
    /** Shop's extrusion die opening angle 2α and land length; null = show the published range. */
    val dieAngleDeg: Double? = null,
    val landLengthMm: Double? = null
) {
    val isEmpty: Boolean get() = listOf(dieCaseOdMm, dieCaseLengthMm, punchCaseOdMm, punchCaseLengthMm, koStrokeMm, insertOdMm).all { it == null }
    val openLimit: Double get() = openExtrusionLimitPct ?: HeadingProgression.MAX_DIE_EXTRUSION_PCT
}

/** Machine pockets are a shop setting: kept across parts, on this device only (load / save are platform code). */
object MachineSetupStore {
    internal const val PREFS = "aerofast_machine_setup"
    internal val KEYS = listOf("dieCaseOd", "dieCaseLen", "punchCaseOd", "punchCaseLen", "koStroke", "insertOd", "openExtrusionLimit", "dieAngle", "landLength")

    var setup by mutableStateOf(MachineSetup())
}

/** A station where the shank is reduced: from the previous station's (or the slug's) diameter to this one. */
data class StationExtrusion(
    val stationIndex: Int,
    val fromMm: Double,
    val toMm: Double,
    val reductionPct: Double,
    /** Length of billet at the starting diameter held in the die bore above the reduction (what traps it). */
    val heldLengthMm: Double
)

/** Where a tooling figure comes from, shown next to it so nothing assumed reads as calculated. */
enum class ToolBasis(val tag: String) {
    /** Worked out from the part geometry the app calculates. */
    CALCULATED("calculated"),
    /** From the machine sizes the user typed. */
    MACHINE("your machine"),
    /** From the material catalogue. */
    CATALOGUE("catalogue"),
    /** Common heading practice, not a standard: a recommendation to confirm with the toolmaker. */
    SHOP_PRACTICE("shop practice"),
    /** A published handbook / industry guideline (sources listed on the sheet). */
    PUBLISHED("published guideline"),
    /** Worked out with a published formula from catalogue data: an estimate, not a measurement. */
    ESTIMATE("estimate"),
    /** No published rule: the toolmaker or carbide supplier decides. */
    TOOLMAKER("toolmaker decides")
}

data class ToolItem(val name: String, val value: String, val basis: ToolBasis)

data class StationTooling(
    val station: HeadingStation,
    /** Part segments inside the die bore and above the die face (split at the die face). */
    val inDie: List<ProfileSegment>,
    val aboveDie: List<ProfileSegment>,
    val items: List<ToolItem>,
    /** Things the machine cannot do as entered. */
    val problems: List<String>
)

data class ToolingSheet(
    val machine: String,
    val cutOff: List<ToolItem>,
    val stations: List<StationTooling>,
    val materials: List<ToolItem>,
    /** Figures the app does not set, and why. */
    val notDecided: List<String>
) {
    val problems: List<String> get() = stations.flatMap { st -> st.problems.map { "Station ${st.station.index}: $it" } }
}

/**
 * Header tooling worked out from the heading stations: die bore profile, punch cavity, recess and pierce pins,
 * knock-out stroke, checked against the machine's own pockets. Shrink fit, insert OD and spring / pad sizes
 * have no published rule the app could find, so they are listed as the toolmaker's decision, not guessed.
 */
object ToolingDesign {
    private fun f(v: Double, dp: Int = 2) = String.format(Locale.US, "%.${dp}f", v)

    /** Splits a station's segments at [cutMm] from the bottom: (below, above). */
    fun split(segments: List<ProfileSegment>, cutMm: Double): Pair<List<ProfileSegment>, List<ProfileSegment>> {
        val below = mutableListOf<ProfileSegment>()
        val above = mutableListOf<ProfileSegment>()
        var acc = 0.0
        for (s in segments) {
            val start = acc
            val end = acc + s.heightMm
            acc = end
            when {
                end <= cutMm + 1e-9 -> below += s
                start >= cutMm - 1e-9 -> above += s
                else -> {
                    val hBelow = cutMm - start
                    if (s.shape == SectionShape.ROUND && s.dBottomMm != s.dTopMm) {
                        val dCut = s.dBottomMm + (s.dTopMm - s.dBottomMm) * hBelow / s.heightMm
                        below += s.copy(heightMm = hBelow, dTopMm = dCut)
                        above += s.copy(heightMm = end - cutMm, dBottomMm = dCut)
                    } else {
                        below += s.copy(heightMm = hBelow)
                        above += s.copy(heightMm = end - cutMm)
                    }
                }
            }
        }
        return below to above
    }

    /** Round in-die sections only: a hex or square section is not an extrusion of the round shank. */
    private fun roundMin(segs: List<ProfileSegment>) =
        segs.filter { it.shape == SectionShape.ROUND }.minOfOrNull { kotlin.math.min(it.dBottomMm, it.dTopMm) }

    /** Every station that reduces the shank, with its area reduction. Nut formers are not shank extrusions. */
    fun extrusions(plan: HeadingPlan): List<StationExtrusion> {
        if (plan.route == HeaderRoute.NUT_FORMER) return emptyList()
        var before = plan.slugDiaMm
        val out = mutableListOf<StationExtrusion>()
        for (st in plan.stations.filter { !it.isCutOff }) {
            val inDie = split(st.segments, st.inDieMm).first
            val now = roundMin(inDie) ?: continue
            if (now < before - 0.01) {
                val held = inDie.filter { it.shape == SectionShape.ROUND && kotlin.math.min(it.dBottomMm, it.dTopMm) >= before - 0.01 }.sumOf { it.heightMm }
                out += StationExtrusion(st.index, before, now, (1.0 - (now / before) * (now / before)) * 100.0, held)
            }
            before = kotlin.math.min(before, now)
        }
        return out
    }

    private fun segText(s: ProfileSegment, clearance: Boolean): String {
        fun c(d: Double) = if (clearance) d + HeadingProgression.stageClearance(d) else d
        return when (s.shape) {
            SectionShape.ROUND -> if (s.dBottomMm != s.dTopMm) "cone Ø${f(c(s.dBottomMm))} → Ø${f(c(s.dTopMm))} × ${f(s.heightMm, 1)}"
                else "Ø${f(c(s.dBottomMm))} × ${f(s.heightMm, 1)}"
            SectionShape.HEX -> "hex s=${f(s.dBottomMm)} × ${f(s.heightMm, 1)}"
            SectionShape.TWELVE_POINT -> "12-point s=${f(s.dBottomMm)} × ${f(s.heightMm, 1)}"
            SectionShape.SQUARE -> "square ${f(s.dBottomMm)} × ${f(s.heightMm, 1)}"
            SectionShape.DOME -> "dome Ø${f(s.dBottomMm)} × ${f(s.heightMm, 1)} high"
            else -> "${s.shape.name.lowercase()} ${f(s.dBottomMm)} × ${f(s.heightMm, 1)}"
        }
    }

    fun build(r: FastenerCalculationResult, machine: MachineSetup = MachineSetup()): ToolingSheet? {
        val plan = r.headingPlan ?: return null
        val cutOff = listOf(
            ToolItem("Wire", "Ø${f(plan.slugDiaMm)} mm ${r.material.key}", ToolBasis.CALCULATED),
            ToolItem("Cut-off length", "${f(plan.slugLengthMm)} mm (slug ${f(plan.slugVolumeMm3, 0)} mm³)", ToolBasis.CALCULATED),
            ToolItem("Cut-off quill / knife bore", "Wire Ø${f(plan.slugDiaMm)} + shearing clearance", ToolBasis.TOOLMAKER)
        )

        val dieStations = plan.stations.filter { !it.isCutOff }
        val ext = extrusions(plan).associateBy { it.stationIndex }
        val stations = dieStations.mapIndexed { i, st ->
            // The stock this blow starts from: the previous station, or the cut-off slug for the first die
            val before = if (i == 0) plan.stations.firstOrNull { it.isCutOff } else dieStations[i - 1]
            stationTooling(st, machine, before, ext[st.index], r.material)
        }

        val materials = listOf(
            ToolItem("Die insert / punch (for ${r.material.key})", r.material.dieTooling, ToolBasis.CATALOGUE)
        )

        val notDecided = (if (extrusions(plan).isNotEmpty()) listOf("Extrusion die values marked [published guideline] come from: " + ExtrusionData.SOURCES.joinToString(" | ")) else emptyList()) + listOf(
            "Carbide insert OD and shrink-fit interference: no published rule was found (studies report anything from 0.1 % to 0.8 % by station); size them with your carbide supplier or toolmaker." +
                (machine.insertOdMm?.let { " Your planned insert OD Ø${f(it)} is used for the wall checks." } ?: ""),
            "Pressure pad, springs, guide bush and running fits of pins: set from your tool standard.",
            "Tolerances, surface finish and polish of bores and cavities: from your tool standard and the part drawing."
        )

        return ToolingSheet(
            machine = "${plan.machineDescription} — ${plan.dieStations} die station(s)",
            cutOff = cutOff, stations = stations, materials = materials, notDecided = notDecided
        )
    }

    private fun stationTooling(st: HeadingStation, m: MachineSetup, before: HeadingStation? = null, ex: StationExtrusion? = null, material: MaterialInfo? = null): StationTooling {
        val (inDie, above) = split(st.segments, st.inDieMm)
        val items = mutableListOf<ToolItem>()
        val problems = mutableListOf<String>()

        if (inDie.isNotEmpty()) {
            items += ToolItem("Die bore (bottom → die face)", inDie.joinToString(" → ") { segText(it, clearance = true) } +
                " mm, incl. transfer clearance ${f(HeadingProgression.MIN_STAGE_CLEARANCE_MM)}–${f(HeadingProgression.MAX_STAGE_CLEARANCE_MM)} mm by Ø", ToolBasis.CALCULATED)
            items += ToolItem("Die bore depth", "${f(st.inDieMm, 1)} mm (part held in the die)", ToolBasis.CALCULATED)
            // A step down the bore is an extrusion (or a shoulder): its entry angle and land have no rule in the app
            if (ex != null) inDie.zipWithNext().filter { (lower, upper) -> upper.maxWidthMm - lower.maxWidthMm > 0.05 }.forEach { (lower, upper) ->
                items += ToolItem("Extrusion entry Ø${f(upper.maxWidthMm)} → Ø${f(lower.maxWidthMm)}", "Die entry angle, land length and radius", ToolBasis.TOOLMAKER)
            }
        } else {
            items += ToolItem("Die", "Part is not held in a die bore at this station", ToolBasis.CALCULATED)
        }
        if (ex != null) {
            val pct = "${f(ex.reductionPct, 1)} % area (Ø${f(ex.fromMm)} → Ø${f(ex.toMm)})"
            when {
                ex.reductionPct > HeadingProgression.TRAPPED_EXTRUSION_MAX_PCT -> {
                    items += ToolItem("Extrusion", "$pct — over the ~${HeadingProgression.TRAPPED_EXTRUSION_MAX_PCT.toInt()} % trapped-extrusion limit", ToolBasis.SHOP_PRACTICE)
                    problems += "extrusion ${f(ex.reductionPct, 0)} % is over the ~${HeadingProgression.TRAPPED_EXTRUSION_MAX_PCT.toInt()} % trapped limit: extrude in two stations or choose a thinner wire"
                }
                ex.reductionPct > m.openLimit -> items += ToolItem("Extrusion: TRAPPED die required",
                    "$pct is over your ${f(m.openLimit, 0)} % open-extrusion limit: hold the billet in the Ø${f(ex.fromMm + HeadingProgression.stageClearance(ex.fromMm))} bore before the reduction" +
                        (if (ex.heldLengthMm > 0.0) " (billet held over ${f(ex.heldLengthMm, 1)} mm)" else ""), ToolBasis.SHOP_PRACTICE)
                else -> items += ToolItem("Extrusion: open", "$pct, within your ${f(m.openLimit, 0)} % open-extrusion limit", ToolBasis.SHOP_PRACTICE)
            }
            if (material != null && ex.reductionPct <= HeadingProgression.TRAPPED_EXTRUSION_MAX_PCT) {
                val (dieItems, dieProblems) = ExtrusionDie.items(ex, material, m)
                items += dieItems
                problems += dieProblems
            }
        }
        val aboveH = above.sumOf { it.heightMm }
        if (above.isNotEmpty()) {
            items += ToolItem("Punch cavity (die face → punch)", above.joinToString(" → ") { segText(it, clearance = false) } + " mm", ToolBasis.CALCULATED)
            items += ToolItem("Punch cavity depth", "${f(aboveH, 1)} mm above the die face", ToolBasis.CALCULATED)
        }
        coneGather(st, above, before)?.let { items.addAll(it) }
        st.recesses.filter { it.fromTop }.forEach { rc ->
            items += ToolItem("Punch pin (recess)", "${shapeName(rc.shape)} ${f(rc.sizeMm)} × ${f(rc.depthMm, 1)} mm deep", ToolBasis.CALCULATED)
        }
        st.recesses.filter { !it.fromTop }.forEach { rc ->
            items += ToolItem("Knock-out pin nose (bottom recess)", "${shapeName(rc.shape)} ${f(rc.sizeMm)} × ${f(rc.depthMm, 1)} mm", ToolBasis.CALCULATED)
        }
        st.throughHoleMm?.let { items += ToolItem("Pierce pin", "Ø${f(it)} mm", ToolBasis.CALCULATED) }
        if (st.scrapMm3 > 0.0) items += ToolItem("Scrap removed", "${f(st.scrapMm3, 0)} mm³", ToolBasis.CALCULATED)

        val bottomBore = inDie.firstOrNull()?.let { s -> s.maxWidthMm + HeadingProgression.stageClearance(s.maxWidthMm) }
        if (bottomBore != null) {
            items += ToolItem("Knock-out pin", "Runs in the Ø${f(bottomBore)} bottom bore; ejection stroke ≥ ${f(st.inDieMm, 1)} mm", ToolBasis.CALCULATED)
        }
        if (st.isPeakBlow) items += ToolItem("Load", "Peak blow: full forming load on this station", ToolBasis.CALCULATED)

        // Checks against the machine pockets the user entered
        val maxBore = inDie.maxOfOrNull { it.maxWidthMm + HeadingProgression.stageClearance(it.maxWidthMm) }
        m.dieCaseLengthMm?.let { L ->
            items += ToolItem("Die case length", "${f(L, 1)} mm", ToolBasis.MACHINE)
            if (st.inDieMm >= L) problems += "die bore depth ${f(st.inDieMm, 1)} mm does not fit the ${f(L, 1)} mm die case"
        }
        m.koStrokeMm?.let { k ->
            if (st.inDieMm > k) problems += "knock-out stroke ${f(k, 1)} mm cannot push out a part held ${f(st.inDieMm, 1)} mm in the die"
        }
        if (maxBore != null) {
            m.insertOdMm?.let { ins ->
                items += ToolItem("Insert wall", "(Ø${f(ins)} − Ø${f(maxBore)}) / 2 = ${f((ins - maxBore) / 2.0)} mm; OD/bore = ${f(ins / maxBore)}", ToolBasis.MACHINE)
                if (ins <= maxBore) problems += "insert OD Ø${f(ins)} is not larger than the die bore Ø${f(maxBore)}"
            }
            m.dieCaseOdMm?.let { case ->
                val inner = m.insertOdMm ?: maxBore
                items += ToolItem("Case ring wall", "(Ø${f(case)} − Ø${f(inner)}) / 2 = ${f((case - inner) / 2.0)} mm", ToolBasis.MACHINE)
                if (case <= inner) problems += "die case Ø${f(case)} has no room for Ø${f(inner)}"
            }
        }
        val cavityWidth = above.maxOfOrNull { it.maxWidthMm }
        m.punchCaseOdMm?.let { p ->
            if (cavityWidth != null && cavityWidth >= p) problems += "punch cavity ${f(cavityWidth)} mm across is wider than the Ø${f(p)} punch case"
        }
        m.punchCaseLengthMm?.let { pl ->
            if (aboveH >= pl) problems += "punch cavity depth ${f(aboveH, 1)} mm does not fit the ${f(pl, 1)} mm punch case"
        }
        return StationTooling(st, inDie, above, items, problems)
    }

    /**
     * A gathering blow: the head stock is formed into a cone in the punch. The free stock is what stood above the
     * die face before this blow, so its L/D is measured on the previous station at this station's die depth.
     */
    private fun coneGather(st: HeadingStation, above: List<ProfileSegment>, before: HeadingStation?): List<ToolItem>? {
        val cone = above.firstOrNull { it.shape == SectionShape.ROUND && kotlin.math.abs(it.dBottomMm - it.dTopMm) > 0.05 } ?: return null
        if (above.size != 1 || before == null) return null
        val stock = split(before.segments, st.inDieMm).second
        if (stock.isEmpty()) return null
        val freeLen = stock.sumOf { it.heightMm }
        val stockDia = stock.first().maxWidthMm
        val ld = freeLen / stockDia
        val coneH = cone.heightMm
        val list = mutableListOf(
            ToolItem("Free stock before this blow", "${f(freeLen)} mm on Ø${f(stockDia)} = L/D ${f(ld)} " +
                "(app limits: 1 blow ≤ ${HeadingProgression.SINGLE_BLOW_LD}, 2 blows ≤ ${HeadingProgression.TWO_BLOW_LD})", ToolBasis.CALCULATED)
        )
        if (ld > HeadingProgression.SINGLE_BLOW_LD) {
            list += ToolItem("Cone punch type",
                "Sliding (spring-loaded) cone punch is commonly used here: the sleeve encloses the free stock at first contact and slides back as the cone forms, so the stock cannot buckle. Whether a fixed cone is enough depends on the stock length, cone angle, material and stroke",
                ToolBasis.SHOP_PRACTICE)
            list += ToolItem("Sleeve travel", "≥ ${f(freeLen)} − ${f(coneH)} = ${f(freeLen - coneH)} mm, if the sleeve encloses the whole free stock at first contact", ToolBasis.CALCULATED)
        }
        list += ToolItem("Sleeve bore, cone angle, spring force and preload", "From your tool standard / toolmaker", ToolBasis.TOOLMAKER)
        return list
    }

    private fun shapeName(s: SectionShape) = when (s) {
        SectionShape.HEX -> "hex"
        SectionShape.HEXALOBULAR -> "hexalobular"
        SectionShape.SQUARE -> "square"
        SectionShape.TWELVE_POINT -> "12-point"
        else -> "round Ø"
    }

    /** Plain-text tooling sheet for sharing. */
    fun text(r: FastenerCalculationResult, s: ToolingSheet, m: MachineSetup): String = buildString {
        val line = "=".repeat(60)
        appendLine(line)
        appendLine("AEROFAST PRO — HEADER TOOLING SHEET")
        appendLine("${r.threadLabel} × ${f(r.lengthMm, 1)} mm ${r.fastenerType.label}, ${r.material.key}")
        appendLine("Machine: ${s.machine}")
        appendLine(line)
        fun item(i: ToolItem) = appendLine("  ${i.name}: ${i.value}  [${i.basis.tag}]")
        appendLine("CUT-OFF"); s.cutOff.forEach(::item)
        s.stations.forEach { st ->
            appendLine()
            appendLine("STATION ${st.station.index}: ${st.station.title}")
            appendLine("  ${st.station.operation}")
            st.items.forEach(::item)
            st.problems.forEach { appendLine("  !! $it") }
        }
        appendLine()
        appendLine("TOOL MATERIALS"); s.materials.forEach(::item)
        if (!m.isEmpty) {
            appendLine()
            appendLine("MACHINE POCKETS (entered)")
            m.dieCaseOdMm?.let { appendLine("  Die case OD: ${f(it, 1)} mm") }
            m.dieCaseLengthMm?.let { appendLine("  Die case length: ${f(it, 1)} mm") }
            m.punchCaseOdMm?.let { appendLine("  Punch case OD: ${f(it, 1)} mm") }
            m.punchCaseLengthMm?.let { appendLine("  Punch case length: ${f(it, 1)} mm") }
            m.koStrokeMm?.let { appendLine("  Knock-out stroke: ${f(it, 1)} mm") }
            m.insertOdMm?.let { appendLine("  Planned insert OD: ${f(it, 1)} mm") }
        }
        appendLine()
        appendLine("NOT SET BY THE APP")
        s.notDecided.forEach { appendLine("  - $it") }
        appendLine(line)
    }
}
