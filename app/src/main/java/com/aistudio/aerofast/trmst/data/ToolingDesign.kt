package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** The header's own tool pockets, typed by the user from the machine manual. A null field was not entered. */
@Serializable
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
@Serializable
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

@Serializable
data class ToolItem(val name: String, val value: String, val basis: ToolBasis)

@Serializable
data class StationTooling(
    val station: HeadingStation,
    /** Part segments inside the die bore and above the die face (split at the die face). */
    val inDie: List<ProfileSegment>,
    val aboveDie: List<ProfileSegment>,
    val items: List<ToolItem>,
    /** Things the machine cannot do as entered. */
    val problems: List<String>
)

@Serializable
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
