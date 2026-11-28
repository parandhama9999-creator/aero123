package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Serializable
data class FastenerCalculationResult(
    val fastenerType: FastenerType,
    val unitSystem: UnitSystem,
    val standard: ProductStandard,
    val threadLabel: String,
    val nominalDiaMm: Double,
    val pitchMm: Double,
    val lengthMm: Double,
    val threadLengthMm: Double,
    val threadClass: String,
    val material: MaterialInfo,
    val coating: CoatingType,
    val batchQty: Int,

    // Pre-roll Blank / Tap Drill
    val isInternalNut: Boolean,
    val blankTargetDia: Double,
    val blankTolHalf: Double,
    val blankUpperLimit: Double,
    val blankLowerLimit: Double,

    // Authoritative Thread Limits (ISO 965 / ASME B1.1 / AS8879)
    val fundamentalDeviationMm: Double,
    val majorDiaMin: Double,
    val majorDiaMean: Double,
    val majorDiaMax: Double,
    val pitchDiaMin: Double,
    val pitchDiaSweet: Double,
    val pitchDiaMax: Double,
    val minorDiaMin: Double,
    val minorDiaMax: Double,
    val rootRadiusMinMm: Double?,
    val rootRadiusMaxMm: Double?,
    val prePlatePitchAllowanceUm: Double,
    val coatingSpecText: String,

    // Slug & Weights
    val wireDiaMm: Double,
    val netWeightGrams: Double,
    val grossWeightGrams: Double,
    val cutOffLengthMm: Double,
    val upsetLdRatio: Double,

    // Forging Mechanical & UTS Specs
    val rawMaterialSourcingUts: String,
    val postForgingTargetUtsMpa: Double,
    val yieldStrengthMpa: Double,
    val minElongationPct: Double,
    val reductionOfAreaZPct: Double,
    val flowStressMpa: Double,
    val stressAreaAsMm2: Double,
    val proofLoadKn: Double,
    val tensileBreakingLoadKn: Double,
    val areaReductionPct: Double,
    val forgingTonnage: Double,
    val recommendedMachine: String,
    val forgingStages: List<ForgingStage>,
    val heatTreatmentSpec: HeatTreatmentSpec,

    // Quotation
    val costRawMaterial: Double,
    val costForgingHeading: Double,
    val costCncTurning: Double,
    val costThreadRolling: Double,
    val costHeatTreatment: Double,
    val costInspectionNdt: Double,
    val unitCostSubtotal: Double,
    val finalUnitPrice: Double,
    val batchTotalPrice: Double,
    val currencySymbol: String,

    // Poka-Yoke Warning
    val hasHydrogenEmbrittlementRisk: Boolean,

    // FEA von Mises Stress Concentration & Dynamic Fatigue
    val stressConcentrationKt: Double,
    val peakThreadRootStressMpa: Double,
    val fatigueEnduranceLimitSeMpa: Double,
    // VDI 2230: allowable alternating bolt force amplitude ±FA (kN) at safety factor 1.2
    val allowableAlternatingForceKn: Double,

    // Automated Forging Crack Risks & Formability Recommendations
    val forgingRisks: List<String>,
    val forgingRecommendations: List<String>,

    // Strict poka-yoke gate results
    val propertyClass: PropertyClass? = null,
    val pokaYokeIssues: List<PokaYokeIssue> = emptyList(),
    val headDimensions: HeadDimensions? = null,
    val headingPlan: HeadingPlan? = null,
    val boltForging: BoltForgingResult? = null,
    /** Wire-preparation anneal that happens before cut-off and heading. */
    val softAnnealSpec: SoftAnnealSpec? = null,
    /** The finished shape of this fastener type: its volume, labelled breakdown and where each dimension came from. */
    val finishedPart: FinishedPart? = null,
    /** Tensile band from the drawing, when it replaces a property class. */
    val tensileRange: TensileRange? = null,
    /** Nylon thread-locking patch / pellet called for on the drawing. */
    val threadLockPatch: Boolean = false,
    /** Open-extrusion limit the plan was built with (% area in one blow). */
    val openExtrusionLimitPct: Double = HeadingProgression.MAX_DIE_EXTRUSION_PCT,
    /** Wire chosen by the user (null = automatic), and the wire the app would pick itself. */
    val wireDiaOverrideMm: Double? = null,
    val autoWireDiaMm: Double = 0.0,
    /** Raw wire from the mill certificate, and its check against the catalogue's headable range. */
    val millCert: MillCert? = null,
    val wireCheck: WireCheck? = null,
    /** The machine asked for, and the drawing dimensions used: kept so a what-if recalculation changes only what is asked. */
    val headerChoice: HeaderChoice = HeaderChoice.AUTO,
    val drawing: DrawingGeometry = DrawingGeometry()
) {
    /** Volume of the finished part in mm³ (0 when no shape was built). */
    val partVolumeMm3: Double get() = finishedPart?.volumeMm3 ?: 0.0
    /** Head volume on its own: the stock gathered above the die face (the whole part for a nut or washer). */
    val headVolumeMm3: Double get() = finishedPart?.headVolumeMm3 ?: 0.0
    /** Shank, thread section and neck: everything held in the die bore. Head + shank = [partVolumeMm3]. */
    val shankVolumeMm3: Double get() = finishedPart?.shankVolumeMm3 ?: 0.0
    /** Head weight on its own, in grams. */
    val headWeightGrams: Double get() = headVolumeMm3 / 1000.0 * material.density
    /** Shank + thread weight, in grams. */
    val shankWeightGrams: Double get() = shankVolumeMm3 / 1000.0 * material.density
    val criticalIssues: List<PokaYokeIssue> get() = pokaYokeIssues.filter { it.severity == PokaYokeSeverity.CRITICAL }
    /** True when any CRITICAL rule fails: reports must not show the part as approved. */
    val isReleaseBlocked: Boolean get() = criticalIssues.isNotEmpty()
}

@Serializable
data class ForgingStage(
    val stageNumber: Int,
    val title: String,
    val description: String
)

enum class GaugeStatus(val label: String, val isScrap: Boolean, val isWarning: Boolean) {
    IDEAL_PASS("IDEAL PASS (94% Fill)", false, false),
    TIGHT_HIGH("TIGHT / HIGH", false, true),
    LOOSE_LOW("LOOSE / LOW", false, true),
    SCRAP_GO_BINDS("SCRAP (GO Gauge Binds)", true, false),
    SCRAP_NO_GO_FAILS("SCRAP (NO-GO > 1.5 Turns)", true, false)
}
