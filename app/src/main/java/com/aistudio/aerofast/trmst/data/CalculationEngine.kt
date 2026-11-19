package com.aistudio.aerofast.trmst.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

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

object CalculationEngine {

    fun calculate(
        fastenerType: FastenerType,
        unitSystem: UnitSystem,
        standard: ProductStandard,
        nominalSizeIndex: Int,
        pitchValue: Double,
        lengthMm: Double,
        threadClass: String,
        material: MaterialInfo,
        coating: CoatingType,
        batchQty: Int,
        currencyIsUsd: Boolean = false,
        cncHourRateInr: Double = 750.0,
        htRatePerKgInr: Double = 180.0,
        marginPct: Double = 22.0,
        customNominalDiaMm: Double? = null,
        extractedRadius: Double? = null,
        propertyClass: PropertyClass? = null,
        /** Geometry read from a scanned drawing; a null field means the standard / table value is used. */
        drawing: DrawingGeometry = DrawingGeometry(),
        /** Tensile band from the drawing (e.g. 1000–1240 MPa): replaces the property class for loads, hardness and checks. */
        tensileRange: TensileRange? = null,
        threadLockPatch: Boolean = false,
        /** Machine asked for: Auto, 1-die 2-blow header or multi-station former. The die design follows it. */
        headerChoice: HeaderChoice = HeaderChoice.AUTO
    ): FastenerCalculationResult {
        @Suppress("NAME_SHADOWING")
        val propertyClass = if (tensileRange != null) PropertyClass.MATERIAL_SPEC else propertyClass
        val safeBatchQty = batchQty.coerceAtLeast(1)
        val d: Double = customNominalDiaMm ?: if (unitSystem == UnitSystem.METRIC) {
            val metric = FastenerCatalog.METRIC_SIZES.getOrElse(nominalSizeIndex) { FastenerCatalog.METRIC_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.METRIC)] }
            metric.d
        } else {
            val inch = FastenerCatalog.INCH_SIZES.getOrElse(nominalSizeIndex) { FastenerCatalog.INCH_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.INCH)] }
            ((inch.dInch * 25.4) * 1000).roundToInt() / 1000.0
        }
        val p = if (unitSystem == UnitSystem.METRIC || pitchValue < 5.0) {
            pitchValue
        } else {
            25.4 / pitchValue
        }

        val isNut = fastenerType.isNut
        val threadLabel = if (fastenerType.isUnthreaded) {
            "Ø${d.round(2)} rivet (no thread)"
        } else if (customNominalDiaMm != null) {
            // A scanned size that is a catalogue size keeps its standard name (#8-32, M10×1.5)
            val std = if (unitSystem == UnitSystem.METRIC)
                FastenerCatalog.METRIC_SIZES.firstOrNull { abs(it.d - customNominalDiaMm) < 0.02 && it.pitches.any { pp -> abs(pp - p) < 0.005 } }
                    ?.let { "M${it.d.let { v -> if (v % 1.0 == 0.0) v.toInt().toString() else v.toString() }}×$p - $threadClass" }
            else FastenerCatalog.INCH_SIZES.firstOrNull { abs(it.dInch * 25.4 - customNominalDiaMm) < 0.03 }?.let { s ->
                s.tpis.firstOrNull { abs(it - 25.4 / p) < 0.1 }?.let { tpi -> "${s.label.substringBefore(" ")}-$tpi $threadClass" }
            }
            std ?: "Custom Ø${customNominalDiaMm}×${p.round(3)} - $threadClass"
        } else if (unitSystem == UnitSystem.METRIC) {
            val metric = FastenerCatalog.METRIC_SIZES.getOrElse(nominalSizeIndex) { FastenerCatalog.METRIC_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.METRIC)] }
            "M${metric.d.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }}×$p - $threadClass"
        } else {
            val inch = FastenerCatalog.INCH_SIZES.getOrElse(nominalSizeIndex) { FastenerCatalog.INCH_SIZES[FastenerCatalog.defaultSizeIndex(UnitSystem.INCH)] }
            val sizeName = inch.label.substringBefore(" ")
            "$sizeName-${pitchValue.toInt()} $threadClass"
        }

        val es: Double
        val td2: Double
        val tdMaj: Double
        val isUnj = threadClass.contains("UNJ", ignoreCase = true)
        var rootRMin: Double? = null
        var rootRMax: Double? = null

        val d2Min: Double
        val d2Max: Double
        val d2Sweet: Double
        val majorMin: Double
        val majorMax: Double
        val majorMean: Double
        val minorMin: Double
        val minorMax: Double
        val blankDia: Double
        val blankTolHalf: Double

        if (unitSystem == UnitSystem.METRIC) {
            // ISO 965-1 Grade 6 fundamental pitch & major tolerances
            val td2Grade6 = 0.090 * p.pow(0.4) * d.pow(0.1)
            val tdMajGrade6 = 0.180 * p.pow(0.65)

            if (isNut) {
                // Metric internal: any ISO 965 class, e.g. 6H, 4H5H, 6G, 6AZ, 5H6H-MJ
                val cls = IsoThreadClass.parse(threadClass)
                val ei = IsoThreadClass.internalEi(cls.position, p)
                val td2Nut = IsoThreadClass.gradeFactor(cls.pitchGrade) * 1.32 * td2Grade6
                // ISO 965-1 TD1 grade 6: 433P − 190P^1.22 µm (P < 1 mm), 230P^0.7 µm (P ≥ 1 mm)
                val td1Grade6 = if (p < 1.0) (0.433 * p - 0.190 * p.pow(1.22)) else 0.230 * p.pow(0.7)
                val td1Nut = IsoThreadClass.gradeFactor(cls.crestGrade) * td1Grade6

                es = ei
                td2 = td2Nut
                tdMaj = tdMajGrade6

                val d2Basic = d - (0.649519 * p)
                d2Min = d2Basic + ei
                d2Max = d2Min + td2Nut
                d2Sweet = d2Min + (0.62 * (d2Max - d2Min))

                majorMin = d + ei
                majorMax = majorMin + (0.15 * p)
                majorMean = (majorMin + majorMax) / 2.0

                // MJ (ISO 5855) internal threads have a truncated minor diameter for the larger external root radius
                val d1Basic = d - ((if (cls.isMj) 0.974278 else 1.082532) * p)
                minorMin = d1Basic + ei
                minorMax = minorMin + td1Nut

                // Internal Nut Boring / Tap Drill Sizing
                blankDia = (minorMin + minorMax) / 2.0
                blankTolHalf = (minorMax - minorMin) / 2.0
            } else {
                // Metric external: any ISO 965 class, e.g. 6g, 4g6g, 6d, 9g8g, 4h6h-MJ
                val cls = IsoThreadClass.parse(threadClass)
                val deviation = IsoThreadClass.externalEs(cls.position, p)
                val pitchGradeMult = IsoThreadClass.gradeFactor(cls.pitchGrade)
                val majorGradeMult = IsoThreadClass.majorGradeFactor(cls.crestGrade)

                es = deviation
                td2 = pitchGradeMult * td2Grade6
                tdMaj = majorGradeMult * tdMajGrade6

                val d2Basic = d - (0.649519 * p)
                d2Max = d2Basic + es
                d2Min = d2Max - td2
                d2Sweet = d2Min + (0.38 * (d2Max - d2Min))

                majorMax = d + es
                majorMin = majorMax - tdMaj
                majorMean = (majorMax + majorMin) / 2.0

                if (cls.isMj) {
                    // MJ (ISO 5855) controlled root radius 0.150P – 0.180P, same profile basis as UNJ
                    minorMax = d2Max - (0.50518 * p)
                    minorMin = d2Min - (0.5658 * p)
                    rootRMin = 0.15011 * p
                    rootRMax = 0.18042 * p
                } else {
                    // Root (d3) follows the flanks: d3 = d2 − 0.57735·P (ISO 965-1, e.g. M10×1.5-6g d3 max 8.128)
                    minorMax = d2Max - (0.577350 * p)
                    minorMin = d2Min - (0.577350 * p)
                    rootRMin = 0.125 * p
                    rootRMax = 0.144 * p
                }

                val d2Target = d2Min + (0.55 * td2)
                // The MJ root is blunter and so shallower: it yields less metal to the crest, and the
                // blank has to make up the difference. Plain-form blanks are unchanged.
                blankDia = d2Target - (0.014 * p) + (material.km * p) +
                    (if (cls.isMj) ThreadRollingBlank.controlledRootDelta(p) else 0.0)
                blankTolHalf = 0.22 * td2
            }
        } else {
            // Unified Inch System (ASME B1.1 / AS8879)
            val dInch = d / 25.4
            val pInch = p / 25.4
            val td2_2A_inch = 0.0015 * dInch.pow(1.0 / 3.0) + 0.0015 * sqrt(dInch) + 0.015 * pInch.pow(2.0 / 3.0)
            val td2_2A = td2_2A_inch * 25.4
            val allowance = 0.30 * td2_2A

            if (isNut) {
                // Inch Internal Nut / Insert Fits: 2B, 3B, 3B-UNJ, 1B
                es = 0.0
                val nutMult = when (threadClass) {
                    "3B", "3B-UNJ" -> 0.975 // 0.75 * 1.30
                    "1B" -> 1.95 // 1.50 * 1.30
                    else -> 1.30 // 2B
                }
                td2 = nutMult * td2_2A
                tdMaj = 0.060 * pInch.pow(2.0 / 3.0) * 25.4

                val d2Basic = d - (0.649519 * p)
                d2Min = d2Basic
                d2Max = d2Min + td2
                d2Sweet = d2Min + (0.62 * (d2Max - d2Min))

                majorMin = d
                majorMax = d + (0.15 * p)
                majorMean = (majorMin + majorMax) / 2.0

                if (isUnj) {
                    minorMin = d - (0.974278 * p)
                    minorMax = minorMin + (0.15 * p)
                    rootRMin = 0.15011 * p
                    rootRMax = 0.18042 * p
                } else {
                    minorMin = d - (1.082532 * p)
                    minorMax = minorMin + (0.20 * p)
                }

                blankDia = (minorMin + minorMax) / 2.0
                blankTolHalf = (minorMax - minorMin) / 2.0
            } else {
                // Inch External Fits: 2A, 3A, 3A-UNJ, 1A
                val dev = when (threadClass) {
                    "3A", "3A-UNJ" -> 0.0
                    else -> -allowance
                }
                val tolMult = when (threadClass) {
                    "3A", "3A-UNJ" -> 0.75
                    "1A" -> 1.50
                    else -> 1.00
                }
                es = dev
                td2 = tolMult * td2_2A
                tdMaj = tolMult * (0.060 * pInch.pow(2.0 / 3.0) * 25.4)

                val d2Basic = d - (0.649519 * p)
                d2Max = d2Basic + es
                d2Min = d2Max - td2
                d2Sweet = d2Min + (0.38 * (d2Max - d2Min))

                majorMax = d + es
                majorMin = majorMax - tdMaj
                majorMean = (majorMax + majorMin) / 2.0

                if (isUnj) {
                    rootRMin = 0.15011 * p
                    rootRMax = 0.18042 * p
                    minorMax = d2Max - (0.50518 * p)
                    minorMin = d2Min - (0.5658 * p)
                } else {
                    // UNR rounded root: minor = d2 − 0.57735·P (ASME B1.1, e.g. 3/8-16 2A minor max 0.2970")
                    minorMax = d2Max - (0.577350 * p)
                    minorMin = d2Min - (0.577350 * p)
                    rootRMin = 0.125 * p
                    rootRMax = 0.144 * p
                }

                val d2Target = d2Min + (0.55 * td2)
                // Same correction as MJ: the UNJ controlled root displaces less metal than UNR.
                blankDia = d2Target - (0.014 * p) + (material.km * p) +
                    (if (isUnj) ThreadRollingBlank.controlledRootDelta(p) else 0.0)
                blankTolHalf = 0.22 * td2
            }
        }

        val blankUpper = blankDia + blankTolHalf
        val blankLower = blankDia - blankTolHalf
        val prePlateOffsetUm = 4.0 * coating.thicknessMaxUm

        // Geometry & Volume
        val lt = drawing.threadLengthMm ?: PokaYokeEngine.threadLengthB(fastenerType, unitSystem, d, lengthMm)
        var dw = ((d * 0.985) * 100).roundToInt() / 100.0 // Wire feedstock diameter
        // Head / nut / washer geometry from the ISO, DIN and ASME tables
        val dims = DimensionTables.forPart(fastenerType, unitSystem, d)
        val s = dims.acrossFlatsS
        val k = dims.headHeightK
        val socket = DimensionTables.socketFor(fastenerType, unitSystem, d)
        // Trimmed hex bolts: head forged round at 1.2 × across corners, trimmed to hex; thread section extruded through a 15° cone.
        // Computed once here so the die progression, the Sizing card and the scanner all use the same slug.
        val flange = if (fastenerType == FastenerType.FLANGE_BOLT)
            BoltForging.resolveFlange(d, drawing.flangeDiaMm, drawing.headHeightMm, drawing.acrossFlatsMm, drawing.flangeThicknessMm) else null
        val boltForging = when {
            fastenerType in BoltForging.HEX_TYPES -> BoltForging.compute(
                type = fastenerType, unit = unitSystem, dMm = d, pitchMm = p, lengthMm = lengthMm,
                blankDiaMm = blankDia, wireDiaMm = dw, tableHead = dims, densityGPerCm3 = material.density,
                threadLengthMm = lt, shankDiaMm = drawing.shankDiaMm, acrossFlatsMm = drawing.acrossFlatsMm,
                headHeightMm = drawing.headHeightMm, flangeDiaMm = null, flangeThicknessMm = null
            )
            flange != null && flange.complete -> BoltForging.compute(
                type = fastenerType, unit = unitSystem, dMm = d, pitchMm = p, lengthMm = lengthMm,
                blankDiaMm = blankDia, wireDiaMm = dw, tableHead = dims, densityGPerCm3 = material.density,
                threadLengthMm = lt, shankDiaMm = drawing.shankDiaMm, acrossFlatsMm = flange.s,
                headHeightMm = flange.k, flangeDiaMm = flange.dc, flangeThicknessMm = flange.c
            )
            else -> null
        }?.takeIf { it.finishedVolumeMm3 != null && it.cheeseVolumeMm3 != null }
        val trimS = if (flange != null && boltForging != null) flange.s!! else drawing.acrossFlatsMm ?: s
        val trimK = if (flange != null && boltForging != null) flange.k!! else drawing.headHeightMm ?: k
        val forgedHeadDia = boltForging?.let { if (flange != null) flange.dc!! else 1.2 * trimS }

        // One finished shape per fastener type: its volume is the part volume and its outline ends the die progression
        val part = PartGeometry.build(
            type = fastenerType, unit = unitSystem, dMm = d, lengthMm = lengthMm, threadLengthMm = lt, blankDiaMm = blankDia,
            table = dims, tableSocket = socket, g = drawing, forging = boltForging, flange = flange?.takeIf { boltForging != null }
        )
        // A headed part with no plain shank is made from wire drawn to the rolling blank: nothing to extrude
        val fullThreadHeaded = part.headSegmentCount > 0 && part.style != HeadStyle.HEX_TRIMMED &&
            part.style != HeadStyle.NUT && part.style != HeadStyle.WASHER && !fastenerType.isUnthreaded &&
            part.dieSegments.isNotEmpty() && part.dieSegments.all { abs(it.dBottomMm - blankDia) < 1e-9 && abs(it.dTopMm - blankDia) < 1e-9 }
        if (fullThreadHeaded) dw = kotlin.math.ceil(blankDia * 100.0 - 1e-6) / 100.0
        val totalVolMm3 = part.volumeMm3
        val hexHeadArea = (sqrt(3.0) / 2.0) * s.pow(2.0)
        // Head material that has to be gathered above the die (drives the upset ratio and the flow stress)
        val volHead = when (part.style) {
            HeadStyle.HEADLESS -> 0.0
            HeadStyle.NUT, HeadStyle.WASHER -> hexHeadArea * k
            else -> part.headVolumeMm3
        }
        val headArea = when {
            forgedHeadDia != null -> (PI / 4.0) * forgedHeadDia.pow(2.0)
            part.style == HeadStyle.HEADLESS || part.style == HeadStyle.WASHER -> hexHeadArea
            else -> part.headSegments.maxOfOrNull { it.crossSectionMm2 } ?: hexHeadArea
        }

        val netWeightG = max(0.05, (totalVolMm3 / 1000.0) * material.density)
        val wireAreaMm2 = ((PI / 4.0) * dw.pow(2.0)).coerceAtLeast(1e-6)
        val headingPlan = HeadingProgression.build(
            type = fastenerType, wireDiaMm = dw, part = part, lengthMm = lengthMm, threadLabel = threadLabel,
            forging = boltForging, forgedHeadDiaMm = forgedHeadDia, trimmedHexS = trimS, trimmedHexK = trimK,
            flangeDiaMm = flange?.dc?.takeIf { boltForging != null }, flangeThicknessMm = flange?.c?.takeIf { boltForging != null },
            choice = headerChoice
        )
        // The 1-die 2-blow design picks its own wire (blank, shank, or enlarged for two blows) and forms the head untrimmed
        val twoBlow = headingPlan?.route == HeaderRoute.TWO_BLOW
        if (twoBlow) dw = headingPlan!!.slugDiaMm
        // Slug also carries material removed during heading (pierced nut web, trim cheese): weigh what is cut off the coil
        val cutOffLenMm = headingPlan?.slugLengthMm ?: (totalVolMm3 / wireAreaMm2)
        val grossWeightG = headingPlan?.let { max(netWeightG, it.slugVolumeMm3 / 1000.0 * material.density) } ?: (netWeightG * 1.04)
        val ldRatio = if (twoBlow) part.headVolumeMm3 / ((PI / 4.0) * dw * dw) / dw
            else boltForging?.headStockMm?.let { it / (boltForging.shankDiaMm) }
            ?: if (volHead > 0) ((volHead / wireAreaMm2) / dw) else 0.0

        // Forging Mechanics & UTS Calculations
        val trueStrainEps = ln(max(1.15, ldRatio + 1.0))
        val flowStressMpa = material.flowStressCoeffK * trueStrainEps.pow(material.strainHardeningN)
        // Tensile stress area: ISO 898-1 As = π/4·(d − 0.9382P)²; ASME B1.1 As = π/4·(d − 0.9743P)²
        val stressAreaAsMm2 = if (fastenerType.isUnthreaded) (PI / 4.0) * d.pow(2.0)
            else (PI / 4.0) * (d - (if (unitSystem == UnitSystem.INCH) 0.974279 else 0.938194) * p).pow(2.0)
        // Loads follow the selected property class (ISO 898-1 proof stress Sp and min Rm), not just the raw alloy
        val strength = if (tensileRange != null) tensileRange.strength
            else if (propertyClass != null) PokaYokeEngine.classStrength(propertyClass, d, material)
            else ClassStrength(material.postForgingTargetUtsMpa, material.yieldStrengthMpa, material.yieldStrengthMpa * 0.9, false)
        val effectiveRmMpa = strength.rmMinMpa
        val effectiveYieldMpa = strength.yieldMinMpa
        // ISO 898-2: a nut is proof-tested to at least the mating bolt's minimum breaking load As·Rm
        val proofLoadKn = (stressAreaAsMm2 * (if (isNut) effectiveRmMpa else strength.proofStressMpa)) / 1000.0
        val tensileBreakingLoadKn = (stressAreaAsMm2 * effectiveRmMpa) / 1000.0

        // Forging Tonnage based on actual compressive flow stress and die geometry constraint
        val blankAreaMm2 = (PI / 4.0) * blankDia.pow(2.0)
        val areaRedPct = max(0.0, ((wireAreaMm2 - blankAreaMm2) / wireAreaMm2) * 100.0)
        val forgingTonnage = ((headArea * flowStressMpa * 1.55) / 9806.65) * 1.20
        val machineCapacity = (ceil(forgingTonnage / 25.0) * 25).toInt()
        val recommendedMachine = when {
            headingPlan == null -> "Progressive blank & pierce press"
            else -> {
                val warm = if (isWarmHeaded(material, d)) "warm " else ""
                "${headingPlan.machineDescription.replaceFirstChar { it.uppercase() }} ($warm${machineCapacity}T @ ${material.headingSpm} SPM)"
            }
        }

        val numberedStages = if (headingPlan == null) {
            listOf(
                ForgingStage(1, "Strip feed & level", "Level coil strip to washer thickness"),
                ForgingStage(2, "Pierce", "Pierce bore in progressive die"),
                ForgingStage(3, "Blank", "Blank outside diameter"),
                ForgingStage(4, "Deburr & flatten", "Vibratory deburr, check flatness")
            )
        } else {
            headingPlan.stations.map { st ->
                val label = if (st.isCutOff) st.title else "Die ${st.index} · ${st.title}"
                val extra = when {
                    st.isCutOff -> " (${material.rawMaterialSourcingUtsMpa})"
                    st.isPeakBlow -> " (${forgingTonnage.round(1)}T)"
                    else -> ""
                }
                ForgingStage(0, label, st.operation + extra)
            } + headingPlan.offHeaderOps.map { ForgingStage(0, "Off-header · ${it.title}", it.operation) }
        }.mapIndexed { idx, st -> st.copy(stageNumber = idx + 1) }

        // Hydrogen embrittlement poka-yoke
        // ISO 4042 / ASTM F1941: bake needed for electroplated steel parts at or above 1000 MPa (class 10.9+), not for 8.8
        val heFamily = PokaYokeEngine.materialFamily(material)
        val hasHeRisk = coating.isElectroplating && effectiveRmMpa >= 1000.0 &&
            (heFamily == MaterialFamily.CARBON_ALLOY_STEEL || heFamily == MaterialFamily.STAINLESS && material.hasHighHardness)

        // CNC turning time (costing)
        val maxRpm = if (material.isAero) 1800 else 3200
        val calcRpm = min(maxRpm, ((1000 * material.vc) / (PI * max(1.0, d))).roundToInt())
        // Rivets are finished on the header: no turning, no threading
        val totalCncSec = if (fastenerType.isUnthreaded) 0.0 else ((lengthMm / (calcRpm * material.feed)) * 60.0 * 2.1) + 5.5

        // NPD Quotation
        val fx = if (currencyIsUsd) (1.0 / 86.50) else 1.0
        val currencySymbol = if (currencyIsUsd) "$" else "₹"
        val matRate = material.rateInr * fx
        val cRawMat = ((grossWeightG / 1000.0) * matRate) - (((grossWeightG - netWeightG) / 1000.0) * matRate * 0.35)
        val cForging = (((if (material.isAero) 1800.0 else 850.0) * fx) / (material.headingSpm * 60.0 * 0.75)) + (((if (material.isAero) 24000.0 else 6500.0) * fx) / safeBatchQty)
        val cCnc = if (fastenerType.isUnthreaded) 0.0
            else ((totalCncSec / 3600.0) * (cncHourRateInr * fx)) + ((if (material.isAero) 3.8 else 0.75) * fx)
        val cRoll = if (fastenerType.isUnthreaded) 0.0 else (if (material.isAero) 4.8 else 0.65) * fx
        val cHt = ((netWeightG / 1000.0) * (htRatePerKgInr * fx)) + ((if (material.isAero) 5.5 else 0.45) * fx)
        val cNdt = (if (material.isAero) (3.8 + (7500.0 / safeBatchQty)) else 0.30) * fx

        val unitSubtotal = cRawMat + cForging + cCnc + cRoll + cHt + cNdt
        val finalUnit = unitSubtotal * (1.0 + marginPct / 100.0)
        val batchTotal = finalUnit * batchQty

        val (risks, recommendations) = analyzeForgingAndCrackRisks(extractedRadius, material, ldRatio, d)

        val rootRadiusR = 0.144 * p
        // No thread root on a rivet shank
        val ktFactor = if (fastenerType.isUnthreaded) 1.0 else (1.0 + 2.0 * kotlin.math.sqrt((p / 2.0) / rootRadiusR)).coerceIn(2.2, 4.2)
        val nominalTensileStress = (proofLoadKn * 1000.0) / (stressAreaAsMm2.coerceAtLeast(1.0))
        val peakThreadRootStress = nominalTensileStress * ktFactor

        // Threaded-bolt fatigue (VDI 2230 Part 1, eq. R9): endurance stress amplitude of a thread rolled
        // before heat treatment, σASV = 0.85·(150/d + 45) MPa. It is nearly independent of strength class,
        // so the plain-steel rule 0.5·Rm would overstate bolt fatigue strength several times.
        // Rolling after heat treatment raises it: σASG = (2 − FSm/F0.2)·σASV, taken here at FSm = 0.75·F0.2.
        val sigmaAsv = 0.85 * (150.0 / d.coerceAtLeast(1.0) + 45.0)
        val enduranceSe = if (material.isAero) (2.0 - 0.75) * sigmaAsv else sigmaAsv
        // Allowable alternating bolt force amplitude ±FA = σA·As / SD with safety factor SD = 1.2
        val allowableAltForceKn = enduranceSe * stressAreaAsMm2 / 1.2 / 1000.0

        return FastenerCalculationResult(
            fastenerType = fastenerType,
            unitSystem = unitSystem,
            standard = standard,
            threadLabel = threadLabel,
            nominalDiaMm = d,
            pitchMm = p,
            lengthMm = lengthMm,
            threadLengthMm = lt,
            threadClass = threadClass,
            material = material,
            coating = coating,
            batchQty = batchQty,
            isInternalNut = isNut,
            blankTargetDia = blankDia.round(3),
            blankTolHalf = blankTolHalf.round(3),
            blankUpperLimit = blankUpper.round(3),
            blankLowerLimit = blankLower.round(3),
            fundamentalDeviationMm = es.round(4),
            majorDiaMin = majorMin.round(3),
            majorDiaMean = majorMean.round(3),
            majorDiaMax = majorMax.round(3),
            pitchDiaMin = d2Min.round(3),
            pitchDiaSweet = d2Sweet.round(3),
            pitchDiaMax = d2Max.round(3),
            minorDiaMin = minorMin.round(3),
            minorDiaMax = minorMax.round(3),
            rootRadiusMinMm = rootRMin?.round(4),
            rootRadiusMaxMm = rootRMax?.round(4),
            prePlatePitchAllowanceUm = prePlateOffsetUm.round(1),
            coatingSpecText = if ("µm" in coating.specStandard) coating.specStandard
                else "${coating.specStandard} (${coating.thicknessMinUm.toInt()}–${coating.thicknessMaxUm.toInt()} µm)",
            wireDiaMm = dw,
            netWeightGrams = netWeightG.round(2),
            grossWeightGrams = grossWeightG.round(2),
            cutOffLengthMm = cutOffLenMm.round(1),
            upsetLdRatio = ldRatio.round(2),
            rawMaterialSourcingUts = material.rawMaterialSourcingUtsMpa,
            postForgingTargetUtsMpa = effectiveRmMpa,
            yieldStrengthMpa = effectiveYieldMpa,
            minElongationPct = if (isNut) material.minElongationPct else classDuctility(propertyClass)?.first ?: material.minElongationPct,
            reductionOfAreaZPct = if (isNut) material.reductionOfAreaZPct else classDuctility(propertyClass)?.second ?: material.reductionOfAreaZPct,
            flowStressMpa = flowStressMpa.round(1),
            stressAreaAsMm2 = stressAreaAsMm2.round(2),
            proofLoadKn = proofLoadKn.round(1),
            tensileBreakingLoadKn = tensileBreakingLoadKn.round(1),
            areaReductionPct = areaRedPct.round(1),
            forgingTonnage = forgingTonnage.round(1),
            recommendedMachine = recommendedMachine,
            forgingStages = numberedStages,
            headingPlan = headingPlan,
            boltForging = boltForging,
            heatTreatmentSpec = calculateDynamicHeatTreatment(material, d, hasHeRisk, propertyClass).let { ht ->
                if (tensileRange != null) heatTreatForRange(ht, material, tensileRange) else ht
            },
            tensileRange = tensileRange,
            threadLockPatch = threadLockPatch,
            headerChoice = headerChoice,
            drawing = drawing,
            softAnnealSpec = SoftAnnealEngine.build(material),
            finishedPart = part,
            costRawMaterial = cRawMat.round(2),
            costForgingHeading = cForging.round(2),
            costCncTurning = cCnc.round(2),
            costThreadRolling = cRoll.round(2),
            costHeatTreatment = cHt.round(2),
            costInspectionNdt = cNdt.round(2),
            unitCostSubtotal = unitSubtotal.round(2),
            finalUnitPrice = finalUnit.round(2),
            batchTotalPrice = batchTotal.round(2),
            currencySymbol = currencySymbol,
            hasHydrogenEmbrittlementRisk = hasHeRisk,
            stressConcentrationKt = ktFactor.round(2),
            peakThreadRootStressMpa = peakThreadRootStress.round(1),
            fatigueEnduranceLimitSeMpa = enduranceSe.round(1),
            allowableAlternatingForceKn = allowableAltForceKn.round(2),
            forgingRisks = risks,
            forgingRecommendations = recommendations,
            propertyClass = propertyClass,
            headDimensions = dims,
            pokaYokeIssues = PokaYokeEngine.check(
                tensileRange = tensileRange,
                threadLockPatch = threadLockPatch,
                headFromDrawing = drawing.headHeightMm != null && (drawing.headDiaMm != null || drawing.acrossFlatsMm != null),
                type = fastenerType,
                unit = unitSystem,
                standard = standard,
                nominalSizeIndex = nominalSizeIndex,
                pitchValue = pitchValue,
                d = d,
                lengthMm = lengthMm,
                threadClass = threadClass,
                propertyClass = propertyClass,
                material = material,
                coating = coating,
                batchQty = batchQty,
                fundamentalDeviationMm = es,
                pitchDiaMinMm = d2Min,
                pitchDiaMaxMm = d2Max,
                isCustomScan = customNominalDiaMm != null,
                stressAreaMm2 = stressAreaAsMm2,
                headDims = dims,
                threadLengthMm = drawing.threadLengthMm
            ) + listOfNotNull(headingPlan?.feasibilityIssue?.let {
                PokaYokeIssue(PokaYokeSeverity.CRITICAL, "1-die 2-blow cannot make this part", "On a 1-die 2-blow header: $it.",
                    "Choose Multi-station, or change the head size / wire so the head gathers in two blows.")
            })
        )
    }

    private fun analyzeForgingAndCrackRisks(
        extractedRadius: Double?,
        material: MaterialInfo,
        ldRatio: Double,
        d: Double
    ): Pair<List<String>, List<String>> {
        val risks = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        if (ldRatio > HeadingProgression.THREE_BLOW_LD) {
            risks.add("⚠️ High Upset Ratio (${String.format(java.util.Locale.US, "%.2f", ldRatio)} > ${HeadingProgression.THREE_BLOW_LD}): beyond three blows — severe risk of buckling and folding laps; use larger wire or warm forming.")
            recommendations.add("Gather the head in a cone pre-upset before the finishing blow (see the die progression for the stations).")
        } else if (ldRatio > 2.3) {
            risks.add("⚠️ Upset L/D ${String.format(java.util.Locale.US, "%.2f", ldRatio)} > 2.3: a single open upset would buckle; the die progression gathers the head in cone preform blow(s) first.")
            recommendations.add("Gather the head in a cone pre-upset before the finishing blow (see the die progression for the stations).")
        } else {
            risks.add("✅ Safe Upset Ratio (${String.format(java.util.Locale.US, "%.2f", ldRatio)} ≤ 2.3): Stable metal flow without buckling risk.")
        }

        if (material.isAero && PokaYokeEngine.materialFamily(material) == MaterialFamily.CARBON_ALLOY_STEEL && !isWarmHeaded(material, d)) {
            risks.add("✈️ Aerospace alloy steel (${material.name}): cold headable only from fully spheroidize-annealed wire; it work-hardens faster than commercial grades.")
            recommendations.add("Buy or anneal the wire fully spheroidized (typically ≤ 92 HRB), use phosphate + soap or polymer coating, and magnetic-particle check the first-off heads for cracks.")
        } else if (material.hasHighHardness && material.isAero) {
            risks.add("✈️ High-Strength Aerospace Alloy Warning (${material.name}): Rapid work-hardening behavior. Room-temperature cold working will cause catastrophic micro-cracking.")
            recommendations.add("Strictly enforce Controlled Warm Forging / Induction Heating (650°C–720°C) with Glass-MoS2 dual lubrication.")
        } else if (material.name.contains("Stainless", ignoreCase = true) || material.name.contains("SS204", ignoreCase = true) || material.name.contains("SS304", ignoreCase = true)) {
            risks.add("⚠️ Austenitic Stainless Steel Warning: High strain-hardening rate. Single-stage trap extrusion will cause internal tearing.")
            recommendations.add("Use multi-stage progressive extrusion with max 22-25% reduction per stage + Oxalate/MoS2 coating.")
        } else {
            recommendations.add("Standard Cold/Warm progressive forging sequence is fully feasible for ${material.name}.")
        }

        if (extractedRadius != null && extractedRadius < 0.15) {
            risks.add("⚠️ Sharp Fillet Radius Detected ($extractedRadius mm): High stress concentration factor (Kt). Risk of quenching cracks and fatigue failure.")
            recommendations.add("Increase fillet radius or specify a controlled root radius transition (e.g., UNJC thread profile with R0.12 min).")
        }

        return Pair(risks, recommendations)
    }

    // Steels that get the generic quench & temper cycle; every other alloy keeps its own catalogue cycle
    // (aluminium, copper, nickel and maraging alloys must never see an 860 °C austenitize + oil quench).
    /** ISO 898-1 classes that need no quench & temper: cold formed / work hardened. */
    private val COLD_FORMED_CLASSES = setOf(
        PropertyClass.CLASS_4_6, PropertyClass.CLASS_4_8, PropertyClass.CLASS_5_6, PropertyClass.CLASS_5_8, PropertyClass.CLASS_6_8
    )

    private val QT_STEELS = setOf(
        "10B21", "4135", "4140", "4340", "42CRMO4", "AISI_51B37", "EN24", "EN24_ESR", "8740",
        "AISI_4130", "30KHGSA", "16KHGSN", "35NCD16", "SS410"
    )

    /** Hardness window and temper guidance for a tensile band taken from the drawing. */
    fun heatTreatForRange(ht: HeatTreatmentSpec, material: MaterialInfo, r: TensileRange): HeatTreatmentSpec {
        val lo = HardnessConversion.hrcFor(r.minMpa)
        val hi = HardnessConversion.hrcFor(r.maxMpa)
        // Aim the middle third of the band so scatter stays inside both limits
        val aimLo = lo + (hi - lo) / 3.0
        val aimHi = hi - (hi - lo) / 3.0
        fun h(v: Double) = "%.0f".format(java.util.Locale.US, v)
        val rangeText = "${h(lo)}–${h(hi)} HRC for ${r.minMpa.toInt()}–${r.maxMpa.toInt()} MPa; aim ${h(aimLo)}–${h(aimHi)} HRC"
        if (PokaYokeEngine.materialFamily(material) != MaterialFamily.CARBON_ALLOY_STEEL)
            return ht.copy(targetUtsMpa = "${r.minMpa.toInt()}–${r.maxMpa.toInt()} MPa (drawing)", targetHardnessHrc = "${ht.targetHardnessHrc} (check against $rangeText)")
        val start = TemperGuide.startTempC(material.key, (aimLo + aimHi) / 2.0)
        val temper = if (start != null)
            "Trial temper ${(start - 15).roundToInt()}–${(start + 15).roundToInt()}°C (typical ${material.key} response for ${h(aimLo)}–${h(aimHi)} HRC); adjust after the first hardness and tensile check"
        else "Temper by trial to ${h(aimLo)}–${h(aimHi)} HRC: a higher temperature lowers the strength (${ht.temperingOrAgingTempC} is the class cycle)"
        return ht.copy(
            temperingOrAgingTempC = temper,
            targetHardnessHrc = rangeText,
            targetUtsMpa = "${r.minMpa.toInt()}–${r.maxMpa.toInt()} MPa (drawing)"
        )
    }

    /**
     * Titanium, nickel and other aerospace superalloys are headed warm; aerospace alloy steels (4340, 9310 …) cold head
     * from spheroidize-annealed wire up to ~M16, and are headed warm above that.
     */
    fun isWarmHeaded(material: MaterialInfo, dMm: Double): Boolean {
        if (!material.isAero) return false
        return when (PokaYokeEngine.materialFamily(material)) {
            MaterialFamily.CARBON_ALLOY_STEEL -> dMm > 16.0
            MaterialFamily.ALUMINUM, MaterialFamily.COPPER_ALLOY -> false
            else -> true
        }
    }

    fun calculateDynamicHeatTreatment(
        material: MaterialInfo,
        d: Double,
        hasHeRisk: Boolean,
        propertyClass: PropertyClass? = null
    ): HeatTreatmentSpec {
        val key = material.key.uppercase()
        val base = material.heatTreatmentSpec
        val bakeCycle = "190°C – 220°C for 8 to 24 hrs, start within 4 hrs of plating (ISO 4042 / ASTM F1941), verify per ASTM F1940"
        val baseWithBake = base.copy(
            hydrogenBakingRequired = hasHeRisk,
            hydrogenBakingCycle = if (hasHeRisk) bakeCycle else base.hydrogenBakingCycle
        )
        val isSteelClass = propertyClass in listOf(
            PropertyClass.CLASS_4_6, PropertyClass.CLASS_4_8, PropertyClass.CLASS_5_6, PropertyClass.CLASS_5_8, PropertyClass.CLASS_6_8,
            PropertyClass.CLASS_8_8, PropertyClass.CLASS_9_8, PropertyClass.CLASS_10_9, PropertyClass.CLASS_12_9
        )

        return when {
            // ISO 898-1 classes 4.6 / 4.8 / 5.6 / 5.8 / 6.8 need no quench & temper (cold formed / work hardened)
            propertyClass != null && propertyClass in COLD_FORMED_CLASSES &&
                PokaYokeEngine.materialFamily(material) == MaterialFamily.CARBON_ALLOY_STEEL -> {
                baseWithBake.copy(
                    austenitizingOrSolutionTempC = "N/A — cold formed, no hardening (ISO 898-1 ${propertyClass.label.substringBefore(" (")})",
                    soakingTimeMinutes = "N/A",
                    quenchMedium = "N/A",
                    temperingOrAgingTempC = "Optional stress relief 520°C – 560°C after heading",
                    temperingOrAgingTimeHours = "60–90 mins if stress relieved",
                    // ISO 898-1 Table 3 hardness ranges
                    targetHardnessHrc = when (propertyClass) {
                        PropertyClass.CLASS_4_6 -> "67–95 HRB (120–220 HV)"
                        PropertyClass.CLASS_4_8 -> "71–95 HRB (130–220 HV)"
                        PropertyClass.CLASS_5_6 -> "79–95 HRB (155–220 HV)"
                        PropertyClass.CLASS_6_8 -> "89–99.5 HRB (190–250 HV)"
                        else -> "82–95 HRB (160–220 HV)"
                    },
                    standardReference = "ISO 898-1",
                    isDoubleTemper = false,
                    isAging = false
                )
            }
            // Only genuine Q&T steels get the generic cycle below; other alloys use their catalogue cycle
            key !in QT_STEELS && !key.contains("TI") && !key.contains("718") && !key.contains("WASP") &&
                key != "A286" && key != "SS204" && key != "SS304" && key != "SS316" &&
                !key.contains("15_5") && !key.contains("17_4") -> baseWithBake
            // CP titanium (Grade 1/2) is not heat treatable, and Ti-6242 uses a duplex anneal: keep catalogue cycles
            key == "TI_GR1" || key == "TI_6242" -> baseWithBake

            // Precipitation Hardening Stainless (15-5PH, 17-4PH)
            // Precipitation Hardening Stainless (15-5PH, 17-4PH)
            key == "17_4PH" || key.contains("15_5") || key.contains("17_4") || key.contains("15-5") || key.contains("17-4") -> {
                val solSoak = maxOf(45, (30 + 1.5 * d).roundToInt())
                HeatTreatmentSpec(
                    austenitizingOrSolutionTempC = "1038°C ± 10°C (Solution Condition A)",
                    soakingTimeMinutes = "$solSoak mins soak (for Ø${d.round(1)}mm)",
                    quenchMedium = "Rapid Air Cool or Agitated Oil to <32°C",
                    temperingOrAgingTempC = "H900: 482°C | H1025: 552°C",
                    temperingOrAgingTimeHours = "H900: 60 mins (1 hr) soak | H1025: 240 mins (4 hrs) soak",
                    targetHardnessHrc = "40–45 HRC (H900) / 35–40 HRC (H1025)",
                    hydrogenBakingRequired = false,
                    hydrogenBakingCycle = "Not required for condition H900/H1025",
                    standardReference = "AMS 5643 / ASTM A564",
                    preheatTempC = "550°C (Pre-Heating / Stress Relief)",
                    preheatSoakTime = "${maxOf(30, (20 + 1.0 * d).roundToInt())} mins",
                    atmosphere = "Inert Argon or High Vacuum (<10⁻³ mbar)",
                    quenchDetails = "Agitated oil or forced air quench below 32°C to ensure 100% martensite transformation",
                    coolingMethod = "Still air cool to ambient",
                    targetUtsMpa = "1310 MPa (H900) / 1070 MPa (H1025)",
                    bakingTempC = "N/A",
                    bakingSoakTime = "N/A",
                    isDoubleTemper = false,
                    isAging = true
                )
            }
            // A286 (AMS 5731)
            key == "A286" -> {
                HeatTreatmentSpec(
                    austenitizingOrSolutionTempC = "980°C ± 10°C (Solution Anneal)",
                    soakingTimeMinutes = "60 mins soak (AMS 5731)",
                    quenchMedium = "Oil Quench or Rapid Water Quench",
                    temperingOrAgingTempC = "720°C ± 5°C (Precipitation Aging)",
                    temperingOrAgingTimeHours = "16 Hours (960 mins) soak",
                    targetHardnessHrc = "28–35 HRC",
                    hydrogenBakingRequired = false,
                    hydrogenBakingCycle = "Not susceptible to hydrogen embrittlement",
                    standardReference = "AMS 5731 / AMS 5732",
                    preheatTempC = "650°C (Pre-heat)",
                    preheatSoakTime = "45 mins",
                    atmosphere = "Protective Argon or High Vacuum (<10⁻⁴ mbar)",
                    quenchDetails = "Oil / water quench to room temperature within 30 seconds",
                    coolingMethod = "Air cool to ambient post-aging",
                    targetUtsMpa = "900–1050 MPa",
                    bakingTempC = "N/A",
                    bakingSoakTime = "N/A",
                    isDoubleTemper = false,
                    isAging = true
                )
            }
            // Ti-6Al-4V Gr 5 (AMS 4928)
            key == "TI_GR5" || key.contains("TI") -> {
                HeatTreatmentSpec(
                    austenitizingOrSolutionTempC = "955°C ± 5°C (Vacuum Solution Treatment)",
                    soakingTimeMinutes = "60 mins soak (AMS 4928)",
                    quenchMedium = "Argon Gas Blast (2–5 bar) or Agitated Water Quench",
                    temperingOrAgingTempC = "540°C ± 5°C (Precipitation Aging)",
                    temperingOrAgingTimeHours = "6 Hours (360 mins) soak",
                    targetHardnessHrc = "36–40 HRC",
                    hydrogenBakingRequired = false,
                    hydrogenBakingCycle = "Vacuum degassing at 700°C if hydrogen pick-up exceeds 125 ppm",
                    standardReference = "AMS 4928 / AMS 2801",
                    preheatTempC = "600°C (Induction Pre-heat in Vacuum)",
                    preheatSoakTime = "30 mins",
                    atmosphere = "High Vacuum (<10⁻⁴ mbar) to avoid alpha-case embrittlement",
                    quenchDetails = "Pressurized Argon quench (2–5 bar) with high-velocity gas circulation",
                    coolingMethod = "Vacuum cooling to room temperature",
                    targetUtsMpa = "1100–1200 MPa",
                    bakingTempC = "N/A",
                    bakingSoakTime = "N/A",
                    isDoubleTemper = false,
                    isAging = true
                )
            }
            // Inconel 718 (AMS 5662)
            key == "INCONEL718" || key.contains("718") -> {
                HeatTreatmentSpec(
                    austenitizingOrSolutionTempC = "955°C – 980°C (Solution Anneal)",
                    soakingTimeMinutes = "60 mins soak (AMS 5662)",
                    quenchMedium = "Argon Gas Quench or Forced Air Cool to <38°C",
                    temperingOrAgingTempC = "Double Age: 720°C (8 hrs) -> Cool 55°C/hr -> 620°C (8 hrs)",
                    temperingOrAgingTimeHours = "8 hrs (480 mins) @ 720°C + 8 hrs (480 mins) @ 620°C",
                    targetHardnessHrc = "40–46 HRC",
                    hydrogenBakingRequired = false,
                    hydrogenBakingCycle = "Not applicable to nickel superalloys",
                    standardReference = "AMS 5662 / AMS 5664",
                    preheatTempC = "700°C (Uniform Thermal Pre-heat)",
                    preheatSoakTime = "60 mins",
                    atmosphere = "Vacuum Furnace (<10⁻⁴ mbar)",
                    quenchDetails = "Rapid Argon recirculating quench to below 38°C",
                    coolingMethod = "Controlled furnace cool at 55°C/hr from 720°C to 620°C, then air cool",
                    targetUtsMpa = "1380–1500 MPa",
                    bakingTempC = "N/A",
                    bakingSoakTime = "N/A",
                    isDoubleTemper = true,
                    isAging = true
                )
            }
            // Waspaloy (AMS 5708)
            key == "WASPALOY" || key.contains("WASP") -> {
                HeatTreatmentSpec(
                    austenitizingOrSolutionTempC = "1010°C ± 10°C (Solution Treatment)",
                    soakingTimeMinutes = "120 mins (2 hrs) soak (AMS 5708)",
                    quenchMedium = "Rapid Oil Quench or High-Velocity Argon",
                    temperingOrAgingTempC = "Stabilize 845°C (4 hrs) + Age 760°C (16 hrs)",
                    temperingOrAgingTimeHours = "Stabilize: 240 mins (4 hrs) soak | Final Age: 960 mins (16 hrs) soak",
                    targetHardnessHrc = "35–42 HRC",
                    hydrogenBakingRequired = false,
                    hydrogenBakingCycle = "Not susceptible to hydrogen embrittlement",
                    standardReference = "AMS 5708 / AMS 5709",
                    preheatTempC = "750°C (Pre-heat)",
                    preheatSoakTime = "60 mins",
                    atmosphere = "Vacuum (<10⁻⁴ mbar) or High-Purity Argon",
                    quenchDetails = "Oil quench or gas fan quench below 60°C",
                    coolingMethod = "Air cool after 845°C stabilization; air cool after 760°C aging",
                    targetUtsMpa = "1250–1350 MPa",
                    bakingTempC = "N/A",
                    bakingSoakTime = "N/A",
                    isDoubleTemper = true,
                    isAging = true
                )
            }
            // Austenitic Stainless (SS204, SS304, SS316)
            key == "SS204" || key == "SS304" || key == "SS316" -> {
                val solSoak = maxOf(30, (20 + 1.5 * d).roundToInt())
                HeatTreatmentSpec(
                    austenitizingOrSolutionTempC = "1050°C – 1080°C (Solution Annealing)",
                    soakingTimeMinutes = "$solSoak mins soak (for Ø${d.round(1)}mm)",
                    quenchMedium = "Rapid Water Quench to <400°C in <30 seconds",
                    temperingOrAgingTempC = "N/A (Non-Hardenable by Heat Treatment)",
                    temperingOrAgingTimeHours = "N/A (Work-hardening alloy)",
                    targetHardnessHrc = "75–95 HRB (Annealed) / 25–32 HRC (Cold-Worked)",
                    hydrogenBakingRequired = false,
                    hydrogenBakingCycle = "Not required for austenitic grades",
                    standardReference = "ASTM A193 / ISO 3506-1",
                    preheatTempC = "500°C – 550°C",
                    preheatSoakTime = "30 mins",
                    atmosphere = "Inert Argon or Vacuum",
                    quenchDetails = "Continuous agitated water bath to avoid Cr23C6 carbide precipitation",
                    coolingMethod = "Rapid water quench",
                    targetUtsMpa = "700–850 MPa",
                    bakingTempC = "N/A",
                    bakingSoakTime = "N/A",
                    isDoubleTemper = false,
                    isAging = false
                )
            }
            // Carbon / Boron / Alloy Steels (10B21, 4135, 4140/EN24, 4340, 30KhGSNA, 1008, 1018, SS410)
            else -> {
                val austSoak = maxOf(30, (20 + 1.8 * d).roundToInt())
                val preheatSoak = maxOf(30, (15 + 1.2 * d).roundToInt())
                val isDouble = key == "4340" || key.contains("30KH") || key.contains("GSNA")

                val tempSoakText = if (isDouble) {
                    "Double Temper: 2 x 120 mins (2 x 2.0 hrs) soak"
                } else {
                    val tempSoak = maxOf(90, (60 + 2.5 * d).roundToInt())
                    val tempHours = ((tempSoak / 60.0) * 10).roundToInt() / 10.0
                    "$tempSoak mins (~$tempHours hrs for Ø${d.round(1)}mm)"
                }

                val austTemp = when (key) {
                    "10B21" -> "860°C – 880°C"
                    "4135" -> "850°C – 870°C"
                    "4140" -> "840°C – 860°C"
                    "4340" -> "830°C – 850°C"
                    "SS410" -> "980°C – 1020°C"
                    else -> base.austenitizingOrSolutionTempC.ifBlank { "860°C" }
                }

                // Tempering follows the target class (ISO 898-1 requires ≥ 425 °C for 8.8 / 10.9 / 12.9);
                // the start value is then fine-tuned on the furnace to land in the class hardness band.
                val isBoron = key.contains("B21") || key.contains("51B37")
                val tempTemp = when {
                    key == "SS410" -> "600°C – 650°C (Tempering)"
                    propertyClass == PropertyClass.CLASS_8_8 -> if (isBoron) "480°C – 540°C (Tempering, ≥ 425°C ISO 898-1)" else "560°C – 620°C (Tempering, ≥ 425°C ISO 898-1)"
                    propertyClass == PropertyClass.CLASS_9_8 -> "≥ 425°C (Tempering, ISO 898-1 minimum)"
                    propertyClass == PropertyClass.CLASS_10_9 -> if (isBoron) "425°C – 460°C (Tempering, ≥ 425°C ISO 898-1)" else "500°C – 560°C (Tempering, ≥ 425°C ISO 898-1)"
                    propertyClass == PropertyClass.CLASS_12_9 -> "440°C – 500°C (Tempering, ≥ 425°C ISO 898-1)"
                    key == "4340" -> "480°C – 540°C (Double Tempering)"
                    else -> base.temperingOrAgingTempC.ifBlank { "≥ 425°C" }
                }

                val quenchMed = when (key) {
                    "10B21" -> "Polymer Quench (8–12%) or Fast Oil @ 40–60°C"
                    "4135", "4140", "4340" -> "Accelerated Quench Oil @ 50–70°C (Agitation 0.8 m/s)"
                    "SS410" -> "Oil Quench or Air Cool from 1000°C"
                    else -> base.quenchMedium.ifBlank { "Accelerated Quench Oil @ 50–70°C" }
                }

                // Core hardness of the selected grade: ISO 898-1 Table 3, SAE J429 Table 1, ASTM A574, ASTM A193
                val hardness = when {
                    key == "SS410" -> "36–42 HRC"
                    propertyClass == PropertyClass.CLASS_8_8 -> if (d <= 16.0) "22–32 HRC (Class 8.8, d ≤ 16)" else "23–34 HRC (Class 8.8, d > 16)"
                    propertyClass == PropertyClass.CLASS_9_8 -> "28–37 HRC (Class 9.8, d ≤ 16)"
                    propertyClass == PropertyClass.CLASS_10_9 -> "32–39 HRC (Class 10.9)"
                    propertyClass == PropertyClass.CLASS_12_9 -> "39–44 HRC (Class 12.9)"
                    propertyClass == PropertyClass.SAE_GRADE_5 -> if (d <= 25.4) "25–34 HRC (SAE Grade 5, ≤ 1\")" else "19–30 HRC (SAE Grade 5, > 1\")"
                    propertyClass == PropertyClass.SAE_GRADE_8 -> "33–39 HRC (SAE Grade 8)"
                    propertyClass == PropertyClass.ASTM_A574 -> if (d <= 12.7) "39–45 HRC (ASTM A574, ≤ 1/2\")" else "37–45 HRC (ASTM A574, > 1/2\")"
                    propertyClass == PropertyClass.ASTM_A193_B7 -> "35 HRC max (ASTM A193 B7)"
                    else -> base.targetHardnessHrc.ifBlank { "32–39 HRC" }
                }
                // Neutral hardening: furnace carbon potential matched to the steel's own carbon (ISO 898-1 forbids carburizing)
                val carbonPotential = when {
                    key.contains("B21") -> "0.20%–0.25%"
                    key == "4135" || key.contains("51B37") || key.contains("30KH") || key == "AISI_4130" -> "0.30%–0.38%"
                    key == "16KHGSN" -> "0.14%–0.20%"
                    else -> "0.38%–0.43%"
                }

                val stdRef = when {
                    key.contains("4340") || key.contains("30KH") -> "AMS 2759/1 & AMS 2759/2"
                    key.contains("4140") || key.contains("4135") -> "ISO 898-1 / AMS 2759/1"
                    key.contains("10B21") -> "ISO 898-1 Class 10.9 / SAE J429"
                    else -> base.standardReference.ifBlank { "ISO 898-1 / AMS 2759" }
                }

                HeatTreatmentSpec(
                    austenitizingOrSolutionTempC = austTemp,
                    soakingTimeMinutes = "$austSoak mins soak (for Ø${d.round(1)}mm)",
                    quenchMedium = quenchMed,
                    temperingOrAgingTempC = tempTemp,
                    temperingOrAgingTimeHours = tempSoakText,
                    targetHardnessHrc = hardness,
                    hydrogenBakingRequired = hasHeRisk,
                    hydrogenBakingCycle = if (hasHeRisk) bakeCycle else "Not required (no electroplating, or strength below 1000 MPa)",
                    standardReference = stdRef,
                    preheatTempC = "600°C – 650°C (Pre-Heating / Stress Relief)",
                    preheatSoakTime = "$preheatSoak mins",
                    atmosphere = "Endothermic gas, neutral C-potential $carbonPotential (no carburizing / decarburizing per ISO 898-1)",
                    quenchDetails = "Bath temp 50–70°C with 0.8 m/s agitation, transfer time < 10s to prevent pearlite nose interception",
                    coolingMethod = "Still air cooling to ambient room temperature",
                    targetUtsMpa = if (isSteelClass) "≥ ${PokaYokeEngine.classStrength(propertyClass!!, d, material).rmMinMpa.toInt()} MPa (ISO 898-1 ${propertyClass.label.substringBefore(" (")})" else "${material.postForgingTargetUtsMpa.toInt()} MPa",
                    bakingTempC = "190°C – 220°C",
                    bakingSoakTime = "8 to 24 Hours mandatory soak",
                    isDoubleTemper = isDouble,
                    isAging = false
                )
            }
        }
    }

    fun evaluateShopGauge(measured: Double, target: Double, upper: Double, lower: Double, tolHalf: Double): GaugeStatus {
        val warnZone = tolHalf * 0.65
        return when {
            measured > upper -> GaugeStatus.SCRAP_GO_BINDS
            measured < lower -> GaugeStatus.SCRAP_NO_GO_FAILS
            (measured - target) > warnZone -> GaugeStatus.TIGHT_HIGH
            (target - measured) > warnZone -> GaugeStatus.LOOSE_LOW
            else -> GaugeStatus.IDEAL_PASS
        }
    }

    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return (this * multiplier).roundToInt() / multiplier
    }

/**
 * Minimum elongation A and reduction of area Z (%) the property class itself requires:
 * ISO 898-1 Table 3, SAE J429, ASTM A193 B7. 0.0 = the standard sets no value for machined test pieces.
 * null = the class defers to the material (stainless, aerospace, material spec).
 */
fun classDuctility(pc: PropertyClass?): Pair<Double, Double>? = when (pc) {
    PropertyClass.CLASS_4_6 -> 22.0 to 0.0
    PropertyClass.CLASS_5_6 -> 20.0 to 0.0
    PropertyClass.CLASS_4_8, PropertyClass.CLASS_5_8, PropertyClass.CLASS_6_8 -> 0.0 to 0.0
    PropertyClass.CLASS_8_8 -> 12.0 to 52.0
    PropertyClass.CLASS_9_8 -> 10.0 to 48.0
    PropertyClass.CLASS_10_9 -> 9.0 to 48.0
    PropertyClass.CLASS_12_9 -> 8.0 to 44.0
    PropertyClass.SAE_GRADE_2 -> 18.0 to 35.0
    PropertyClass.SAE_GRADE_5 -> 14.0 to 35.0
    PropertyClass.SAE_GRADE_8 -> 12.0 to 35.0
    PropertyClass.ASTM_A193_B7 -> 16.0 to 50.0
    else -> null
}

}
