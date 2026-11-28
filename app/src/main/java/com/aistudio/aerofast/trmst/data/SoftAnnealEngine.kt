package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

/** How the wire is softened before cold heading. */
enum class AnnealRoute(val label: String, val purpose: String) {
    SPHEROIDIZE(
        "Sub-critical spheroidize anneal",
        "Convert lamellar pearlite to globular cementite so the wire can be headed without cracking"
    ),
    SOLUTION_ANNEAL(
        "Solution anneal + rapid cool",
        "Dissolve carbides and restore ductile austenite; rapid cool prevents sensitisation"
    ),
    O_TEMPER(
        "Full anneal to O temper",
        "Fully soften to the annealed (O) condition"
    ),
    MILL_ANNEALED(
        "Mill-annealed — supplied ready to head",
        "Wire is bought in the annealed / Condition A state; no in-house anneal"
    ),
    ANNEAL_REQUIRED(
        "Anneal required — wire supplied hard",
        "Catalogue condition is cold drawn / half-hard, which is not a headable state"
    )
}

/**
 * Wire-preparation anneal that happens BEFORE cut-off and heading.
 *
 * Everything here is derived from data already in the material catalogue — the supplied-wire
 * condition string and the post-forming heat-treatment cycle. Any value that could not be
 * sourced from the catalogue is left blank and listed in [unsourced] rather than invented.
 */
@Serializable
data class SoftAnnealSpec(
    val route: AnnealRoute,
    val inHouseAnnealNeeded: Boolean,
    val suppliedCondition: String,
    val temperatureC: String,
    val soakTime: String,
    val coolingRate: String,
    val atmosphere: String,
    val targetUtsMpa: String,
    val targetHardnessHb: String,
    val targetHardnessHrb: String,
    val structureTarget: String,
    val standardReference: String,
    /** Annealed UTS / post-forging UTS. Low values mean the wire is much softer than the finished part. */
    val softeningRatio: Double?,
    /** Fields the catalogue could not supply; the user fills these per grade. */
    val unsourced: List<String>,
    val notes: List<String>,
    /** Where the anneal cycle came from, when it is not derived from the catalogue. */
    val cycleSource: String = "",
    val cycleConfidence: String = ""
)

/** A published mill / softening anneal cycle, with the datasheet it came from. */
@Serializable
data class MillAnnealCycle(
    val temperatureC: String,
    val hold: String,
    val cooling: String,
    val source: String,
    val confidence: String,
    val note: String = ""
)

/** One stage of a furnace cycle, with the time it takes. */
@Serializable
data class AnnealStage(
    val name: String,
    val detail: String,
    val hoursLow: Double,
    val hoursHigh: Double
) {
    val hoursText: String
        get() = if (kotlin.math.abs(hoursHigh - hoursLow) < 0.05) "${fmtH(hoursLow)} h"
        else "${fmtH(hoursLow)} – ${fmtH(hoursHigh)} h"

    private fun fmtH(v: Double) = String.format(java.util.Locale.US, "%.1f", v)
}

/**
 * A complete furnace cycle: every stage from charging to unloading, with door-to-door hours.
 *
 * The stage times are the published figures for spheroidize annealing of cold-heading wire in a
 * bell furnace (see [sources]), scaled for the charge mass where the source says the stage is
 * charge-driven. They are a starting schedule for a furnace trial, not a substitute for the
 * furnace's own thermocouple chart: a bell furnace's ramp depends on coil weight, stack height,
 * and where a coil sits in the stack.
 */
@Serializable
data class AnnealCycle(
    val stages: List<AnnealStage>,
    val chargeTonnes: Double,
    val soakTempC: String,
    val basis: String,
    val sources: List<String>,
    val cautions: List<String>
) {
    val totalLow: Double get() = stages.sumOf { it.hoursLow }
    val totalHigh: Double get() = stages.sumOf { it.hoursHigh }
    val totalText: String
        get() = String.format(java.util.Locale.US, "%.1f – %.1f h", totalLow, totalHigh)
    /** Door-to-door hours per tonne of charge: what the cycle really costs the furnace. */
    val hoursPerTonne: String
        get() = if (chargeTonnes <= 0.0) "" else
            String.format(java.util.Locale.US, "%.1f h/t", (totalLow + totalHigh) / 2.0 / chargeTonnes)
}
