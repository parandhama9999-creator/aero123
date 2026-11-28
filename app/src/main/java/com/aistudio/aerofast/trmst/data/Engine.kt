package com.aistudio.aerofast.trmst.data

import com.aistudio.aerofast.trmst.data.NativeEngine.a
import com.aistudio.aerofast.trmst.data.NativeEngine.run as engine
import kotlinx.serialization.Serializable

/*
 * The engine functions the app calls, with the same names and signatures as before. Each one runs in the Rust
 * engine (engine-rs/, checked against the original Kotlin engine on 900 parts); nothing is calculated here.
 */

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
        drawing: DrawingGeometry = DrawingGeometry(),
        tensileRange: TensileRange? = null,
        threadLockPatch: Boolean = false,
        millCert: MillCert? = null,
        wireDiaOverrideMm: Double? = null,
        openExtrusionLimitPct: Double = HeadingProgression.MAX_DIE_EXTRUSION_PCT,
        headerChoice: HeaderChoice = HeaderChoice.AUTO
    ): FastenerCalculationResult = engine(
        "CalculationEngine.calculate", a(fastenerType), a(unitSystem), a(standard), a(nominalSizeIndex), a(pitchValue),
        a(lengthMm), a(threadClass), a(material), a(coating), a(batchQty), a(currencyIsUsd), a(cncHourRateInr),
        a(htRatePerKgInr), a(marginPct), a(customNominalDiaMm), a(extractedRadius), a(propertyClass), a(drawing),
        a(tensileRange), a(threadLockPatch), a(millCert), a(wireDiaOverrideMm), a(openExtrusionLimitPct), a(headerChoice)
    )

    fun heatTreatForRange(ht: HeatTreatmentSpec, material: MaterialInfo, r: TensileRange): HeatTreatmentSpec =
        engine("CalculationEngine.heatTreatForRange", a(ht), a(material), a(r))

    fun isWarmHeaded(material: MaterialInfo, dMm: Double): Boolean = engine("CalculationEngine.isWarmHeaded", a(material), a(dMm))

    fun calculateDynamicHeatTreatment(material: MaterialInfo, d: Double, hasHeRisk: Boolean, propertyClass: PropertyClass? = null): HeatTreatmentSpec =
        engine("CalculationEngine.calculateDynamicHeatTreatment", a(material), a(d), a(hasHeRisk), a(propertyClass))

    fun evaluateShopGauge(measured: Double, target: Double, upper: Double, lower: Double, tolHalf: Double): GaugeStatus =
        engine("CalculationEngine.evaluateShopGauge", a(measured), a(target), a(upper), a(lower), a(tolHalf))

    fun classDuctility(pc: PropertyClass?): Pair<Double, Double>? = engine("CalculationEngine.classDuctility", a(pc))
}

object PokaYokeEngine {
    enum class StainlessType { AUSTENITIC, DUPLEX, PRECIPITATION_HARDENING, MARTENSITIC, FERRITIC }

    val CHROMATED_COATINGS = setOf(
        CoatingType.ZN_NI, CoatingType.ZINC_BLUE, CoatingType.ZINC_YELLOW,
        CoatingType.AERO_CAD, CoatingType.AERO_CAD_CL2, CoatingType.AERO_CAD_CL3, CoatingType.AERO_CAD_LHE
    )
    /** ISO 3506-1 austenitic classes (A2 = 304-type, A4 = 316-type) plus the austenitic ASTM stud / nut grades. */
    val STAINLESS_CLASSES = setOf(
        PropertyClass.A2_50, PropertyClass.A2_70, PropertyClass.A2_80,
        PropertyClass.A4_50, PropertyClass.A4_70, PropertyClass.A4_80,
        PropertyClass.ASTM_A193_B8_CL1, PropertyClass.ASTM_A193_B8M_CL2, PropertyClass.ASTM_A194_8
    )
    /** ISO 3506-1 martensitic (C) and ferritic (F) classes. */
    val MARTENSITIC_FERRITIC_CLASSES = setOf(
        PropertyClass.C1_50, PropertyClass.C1_70, PropertyClass.C3_80, PropertyClass.C4_70, PropertyClass.F1_60
    )
    /** ISO 898-2 steel nut classes. */
    val ISO_NUT_CLASSES = setOf(
        PropertyClass.NUT_04, PropertyClass.NUT_05, PropertyClass.NUT_5, PropertyClass.NUT_6,
        PropertyClass.NUT_8, PropertyClass.NUT_9, PropertyClass.NUT_10, PropertyClass.NUT_12
    )
    /** Classes written for nuts only (ISO 898-2, SAE J995, ASTM A563 / A194). */
    val NUT_ONLY_CLASSES = ISO_NUT_CLASSES + setOf(
        PropertyClass.SAE_NUT_5, PropertyClass.SAE_NUT_8, PropertyClass.ASTM_A563_DH,
        PropertyClass.ASTM_A194_2H, PropertyClass.ASTM_A194_8
    )
    /** Inch steel grades: like ISO 898-1 they need carbon / alloy steel, but they are not ISO classes. */
    val INCH_STEEL_CLASSES = setOf(
        PropertyClass.SAE_GRADE_2, PropertyClass.SAE_GRADE_5, PropertyClass.SAE_GRADE_8, PropertyClass.ASTM_A574,
        PropertyClass.ASTM_A193_B7, PropertyClass.ASTM_A193_B16, PropertyClass.ASTM_A320_L7,
        PropertyClass.ASTM_A325, PropertyClass.ASTM_A490, PropertyClass.ASTM_A307_A, PropertyClass.ASTM_A307_B,
        PropertyClass.ASTM_F1554_36, PropertyClass.ASTM_F1554_105,
        PropertyClass.SAE_NUT_5, PropertyClass.SAE_NUT_8, PropertyClass.ASTM_A563_DH, PropertyClass.ASTM_A194_2H
    )

    fun materialFamily(m: MaterialInfo): MaterialFamily = engine("PokaYokeEngine.materialFamily", a(m))
    fun stainlessType(m: MaterialInfo): StainlessType? = engine("PokaYokeEngine.stainlessType", a(m))
    fun isAusteniticA2A4(m: MaterialInfo): Boolean = engine("PokaYokeEngine.isAusteniticA2A4", a(m))
    fun isInchStandard(s: ProductStandard): Boolean = engine("PokaYokeEngine.isInchStandard", a(s))
    fun isMetricOnlyStandard(s: ProductStandard): Boolean = engine("PokaYokeEngine.isMetricOnlyStandard", a(s))
    fun standardThreadClasses(s: ProductStandard, isNut: Boolean): List<String>? = engine("PokaYokeEngine.standardThreadClasses", a(s), a(isNut))
    fun isCommercialClass(pc: PropertyClass): Boolean = engine("PokaYokeEngine.isCommercialClass", a(pc))
    fun isAerospaceType(type: FastenerType): Boolean = engine("PokaYokeEngine.isAerospaceType", a(type))
    fun classStrength(pc: PropertyClass, dMm: Double, material: MaterialInfo): ClassStrength = engine("PokaYokeEngine.classStrength", a(pc), a(dMm), a(material))
    fun requiredRmMpa(pc: PropertyClass, dMm: Double, material: MaterialInfo): Double = engine("PokaYokeEngine.requiredRmMpa", a(pc), a(dMm), a(material))
    fun isFullyThreaded(type: FastenerType): Boolean = engine("PokaYokeEngine.isFullyThreaded", a(type))
    fun nutClassFor(pc: PropertyClass): String = engine("PokaYokeEngine.nutClassFor", a(pc))
    fun reportSection(issues: List<PokaYokeIssue>): String = engine("PokaYokeEngine.reportSection", a(issues))
    fun defaultStandard(type: FastenerType, unit: UnitSystem): ProductStandard = engine("PokaYokeEngine.defaultStandard", a(type), a(unit))
    fun threadLengthB(type: FastenerType, unit: UnitSystem, d: Double, lengthMm: Double): Double = engine("PokaYokeEngine.threadLengthB", a(type), a(unit), a(d), a(lengthMm))

    fun check(
        type: FastenerType, unit: UnitSystem, standard: ProductStandard, nominalSizeIndex: Int, pitchValue: Double, d: Double,
        lengthMm: Double, threadClass: String, propertyClass: PropertyClass?, material: MaterialInfo, coating: CoatingType,
        batchQty: Int, fundamentalDeviationMm: Double, pitchDiaMinMm: Double, pitchDiaMaxMm: Double, isCustomScan: Boolean,
        stressAreaMm2: Double = 0.0, headDims: HeadDimensions? = null, threadLengthMm: Double? = null,
        tensileRange: TensileRange? = null, threadLockPatch: Boolean = false, headFromDrawing: Boolean = false
    ): List<PokaYokeIssue> = engine(
        "PokaYokeEngine.check", a(type), a(unit), a(standard), a(nominalSizeIndex), a(pitchValue), a(d), a(lengthMm), a(threadClass),
        a(propertyClass), a(material), a(coating), a(batchQty), a(fundamentalDeviationMm), a(pitchDiaMinMm), a(pitchDiaMaxMm),
        a(isCustomScan), a(stressAreaMm2), a(headDims), a(threadLengthMm), a(tensileRange), a(threadLockPatch), a(headFromDrawing)
    )
}

object DimensionTables {
    fun forPart(type: FastenerType, unit: UnitSystem, dMm: Double): HeadDimensions = engine("DimensionTables.forPart", a(type), a(unit), a(dMm))
    fun socketFor(type: FastenerType, unit: UnitSystem, dMm: Double): Pair<Double, Double>? = engine("DimensionTables.socketFor", a(type), a(unit), a(dMm))
    fun washerFor(type: FastenerType, unit: UnitSystem, dMm: Double): WasherDimensions = engine("DimensionTables.washerFor", a(type), a(unit), a(dMm))
    fun din6923Flange(dMm: Double): Pair<Double, Double>? = engine("DimensionTables.din6923Flange", a(dMm))
    fun hexalobularDriveA(dMm: Double): Double = engine("DimensionTables.hexalobularDriveA", a(dMm))
}

object BoltForging {
    @Serializable
    data class FlangeGeom(val dc: Double?, val k: Double?, val s: Double?, val c: Double?) {
        val complete get() = dc != null && k != null && s != null && c != null && c > 0 && k > c
    }

    val HEX_TYPES = setOf(FastenerType.HEX_BOLT, FastenerType.HEX_BOLT_FULL, FastenerType.HEAVY_HEX_BOLT)

    fun applies(type: FastenerType): Boolean = engine("BoltForging.applies", a(type))
    fun din6921(dMm: Double): FlangeGeom? = engine("BoltForging.din6921", a(dMm))
    fun resolveFlange(dMm: Double, dc: Double?, k: Double?, s: Double?, c: Double?): FlangeGeom =
        engine("BoltForging.resolveFlange", a(dMm), a(dc), a(k), a(s), a(c))

    fun compute(
        type: FastenerType, unit: UnitSystem, dMm: Double, pitchMm: Double, lengthMm: Double,
        blankDiaMm: Double, wireDiaMm: Double, tableHead: HeadDimensions?, densityGPerCm3: Double,
        threadLengthMm: Double?, shankDiaMm: Double?, acrossFlatsMm: Double?, headHeightMm: Double?,
        flangeDiaMm: Double?, flangeThicknessMm: Double?
    ): BoltForgingResult = engine(
        "BoltForging.compute", a(type), a(unit), a(dMm), a(pitchMm), a(lengthMm), a(blankDiaMm), a(wireDiaMm), a(tableHead),
        a(densityGPerCm3), a(threadLengthMm), a(shankDiaMm), a(acrossFlatsMm), a(headHeightMm), a(flangeDiaMm), a(flangeThicknessMm)
    )
}

object PartGeometry {
    val PAN_DOME_TYPES = setOf(FastenerType.PAN_SLOT, FastenerType.PAN_CROSS, FastenerType.PAN_HEXALOBULAR)

    fun styleOf(type: FastenerType): HeadStyle = engine("PartGeometry.styleOf", a(type))

    fun build(
        type: FastenerType, unit: UnitSystem, dMm: Double, lengthMm: Double, threadLengthMm: Double,
        blankDiaMm: Double, table: HeadDimensions, tableSocket: Pair<Double, Double>?, g: DrawingGeometry,
        forging: BoltForgingResult? = null, flange: BoltForging.FlangeGeom? = null
    ): FinishedPart = engine(
        "PartGeometry.build", a(type), a(unit), a(dMm), a(lengthMm), a(threadLengthMm), a(blankDiaMm), a(table), a(tableSocket),
        a(g), a(forging), a(flange)
    )
}

object HeadingProgression {
    const val SINGLE_BLOW_LD = 2.3
    const val TWO_BLOW_LD = 4.5
    const val MIN_STAGE_CLEARANCE_MM = 0.02
    const val MAX_STAGE_CLEARANCE_MM = 0.09
    const val THREE_BLOW_LD = 6.5
    const val MAX_DIE_EXTRUSION_PCT = 30.0
    const val TRAPPED_EXTRUSION_MAX_PCT = 70.0

    fun stageClearance(diaMm: Double): Double = engine("HeadingProgression.stageClearance", a(diaMm))
}

object HeatTreatCycleEngine {
    fun temperHours(sectionMm: Double): Double = engine("HeatTreatCycleEngine.temperHours", a(sectionMm))
    fun soakHours(sectionMm: Double): Double = engine("HeatTreatCycleEngine.soakHours", a(sectionMm))
    fun quenchTemper(material: MaterialInfo, sectionMm: Double, spec: HeatTreatmentSpec = material.heatTreatmentSpec): FurnaceCycle? =
        engine("HeatTreatCycleEngine.quenchTemper", a(material), a(sectionMm), a(spec))
    fun softAnneal(material: MaterialInfo, sectionMm: Double = 10.0): FurnaceCycle? = engine("HeatTreatCycleEngine.softAnneal", a(material), a(sectionMm))
    fun precipitationHarden(material: MaterialInfo, sectionMm: Double): FurnaceCycle? = engine("HeatTreatCycleEngine.precipitationHarden", a(material), a(sectionMm))
}

object SoftAnnealEngine {
    const val HB_PER_MPA = 3.38
    fun build(material: MaterialInfo): SoftAnnealSpec = engine("SoftAnnealEngine.build", a(material))
    fun hbToHrb(hb: Double): Double? = engine("SoftAnnealEngine.hbToHrb", a(hb))
    fun parseUtsRange(s: String): Pair<Double, Double>? = engine("SoftAnnealEngine.parseUtsRange", a(s))
    fun millAnneal(key: String): MillAnnealCycle? = engine("SoftAnnealEngine.millAnneal", a(key))
}

object AnnealCycleEngine {
    const val DEFAULT_CHARGE_TONNES = 10.0
    fun spheroidize(material: MaterialInfo, chargeTonnes: Double = DEFAULT_CHARGE_TONNES, wireDiaMm: Double = 0.0): AnnealCycle? =
        engine("AnnealCycleEngine.spheroidize", a(material), a(chargeTonnes), a(wireDiaMm))
}

object SteelCompositions {
    const val A1_PLAIN_CARBON_C = 723.0
    fun ac1(c: SteelComposition): Double = engine("SteelCompositions.ac1", a(c))
    fun andrewsApplies(c: SteelComposition): Boolean = engine("SteelCompositions.andrewsApplies", a(c))
    fun soakHoursForDiameter(wireDiaMm: Double): Pair<Double, Double> = engine("SteelCompositions.soakHoursForDiameter", a(wireDiaMm))
    fun forMaterial(m: MaterialInfo): SteelComposition? = engine("SteelCompositions.forMaterial", a(m))
    fun ac1For(m: MaterialInfo): Double? = engine("SteelCompositions.ac1For", a(m))
    fun canSpheroidise(m: MaterialInfo): Boolean = engine("SteelCompositions.canSpheroidise", a(m))
    fun soakHours(m: MaterialInfo, wireDiaMm: Double = 0.0): Pair<Double, Double> = engine("SteelCompositions.soakHours", a(m), a(wireDiaMm))
}

object HardnessConversion {
    fun hrcFor(rmMpa: Double): Double = engine("HardnessConversion.hrcFor", a(rmMpa))
}

object TemperGuide {
    fun startTempC(materialKey: String, targetHrc: Double): Double? = engine("TemperGuide.startTempC", a(materialKey), a(targetHrc))
}

object MillCertCheck {
    fun hasRecessHead(type: FastenerType, part: FinishedPart?): Boolean = engine("MillCertCheck.hasRecessHead", a(type), a(part))
    fun check(material: MaterialInfo, type: FastenerType, part: FinishedPart?, cert: MillCert?): WireCheck? =
        engine("MillCertCheck.check", a(material), a(type), a(part), a(cert))
    fun issues(c: WireCheck?): List<PokaYokeIssue> = engine("MillCertCheck.issues", a(c))
}

object JointGrip {
    fun stepMm(unit: UnitSystem): Double = engine("JointGrip.stepMm", a(unit))
    fun appliesTo(type: FastenerType): Boolean = engine("JointGrip.appliesTo", a(type))
    fun check(unit: UnitSystem, lengthMm: Double, threadLengthMm: Double, runoutMm: Double, stackMm: Double): GripCheck =
        engine("JointGrip.check", a(unit), a(lengthMm), a(threadLengthMm), a(runoutMm), a(stackMm))
}

object ThreadRollingBlank {
    const val ROOT_DEPTH_PLAIN = 0.61340
    const val ROOT_DEPTH_J_SHALLOW = 0.57735
    const val ROOT_DEPTH_J_DEEP = 0.60766
    const val ROOT_DEPTH_J_MID = (ROOT_DEPTH_J_SHALLOW + ROOT_DEPTH_J_DEEP) / 2.0
    fun controlledRootDelta(pitchMm: Double, rootDepthPerPitch: Double = ROOT_DEPTH_J_MID): Double =
        engine("ThreadRollingBlank.controlledRootDelta", a(pitchMm), a(rootDepthPerPitch))
    fun controlledRootDeltaRange(pitchMm: Double): ClosedFloatingPointRange<Double> =
        engine<Pair<Double, Double>>("ThreadRollingBlank.controlledRootDeltaRange", a(pitchMm)).let { it.first..it.second }
}

object ToolingDesign {
    fun split(segments: List<ProfileSegment>, cutMm: Double): Pair<List<ProfileSegment>, List<ProfileSegment>> =
        engine("ToolingDesign.split", a(segments), a(cutMm))
    fun extrusions(plan: HeadingPlan): List<StationExtrusion> = engine("ToolingDesign.extrusions", a(plan))
    fun build(r: FastenerCalculationResult, machine: MachineSetup = MachineSetup()): ToolingSheet? = engine("ToolingDesign.build", a(r), a(machine))
    fun text(r: FastenerCalculationResult, s: ToolingSheet, m: MachineSetup): String = engine("ToolingDesign.text", a(r), a(s), a(m))
}

object ExtrusionDie {
    fun coneLengthMm(fromMm: Double, toMm: Double, includedAngleDeg: Double): Double = engine("ExtrusionDie.coneLengthMm", a(fromMm), a(toMm), a(includedAngleDeg))
    fun pressureMpa(m: MaterialInfo, fromMm: Double, toMm: Double): Pair<Double, Double> = engine("ExtrusionDie.pressureMpa", a(m), a(fromMm), a(toMm))
    fun items(ex: StationExtrusion, m: MaterialInfo, setup: MachineSetup): Pair<List<ToolItem>, List<String>> = engine("ExtrusionDie.items", a(ex), a(m), a(setup))
}

object ProductionReport {
    fun summary(r: FastenerCalculationResult): ProductionSummary = engine("ProductionReport.summary", a(r))
    fun processSequence(r: FastenerCalculationResult): List<String> = engine("ProductionReport.processSequence", a(r))
    fun build(r: FastenerCalculationResult, spec: ExtractedFastenerSpec? = null, timestamp: String = ""): String =
        engine("ProductionReport.build", a(r), a(spec), a(timestamp))
}

object RawMaterialPlanner {
    fun suggestedDiameters(rollingBlankDiaMm: Double, shankWireDiaMm: Double): List<Double> =
        engine("RawMaterialPlanner.suggestedDiameters", a(rollingBlankDiaMm), a(shankWireDiaMm))
    fun plan(
        grossVolumeMm3: Double, densityGPerCm3: Double, batchQty: Int, wireDiameters: List<Double>,
        headVolumeMm3: Double = 0.0, shankDiaMm: Double? = null, rollingBlankDiaMm: Double? = null
    ): RawMaterialPlan? = engine(
        "RawMaterialPlanner.plan", a(grossVolumeMm3), a(densityGPerCm3), a(batchQty), a(wireDiameters), a(headVolumeMm3), a(shankDiaMm), a(rollingBlankDiaMm)
    )
    fun coilsNeeded(batchWeightKg: Double, coilWeightKg: Double): Int = engine("RawMaterialPlanner.coilsNeeded", a(batchWeightKg), a(coilWeightKg))
    fun parseDiameters(text: String): List<Double> = engine("RawMaterialPlanner.parseDiameters", a(text))
}

object QualityDocs {
    fun productGradeA(r: FastenerCalculationResult): Boolean = engine("QualityDocs.productGradeA", a(r))
    fun hexBoltTolerances(r: FastenerCalculationResult): List<Triple<String, String, String>> = engine("QualityDocs.hexBoltTolerances", a(r))
    fun controlPlan(r: FastenerCalculationResult): List<ControlPlanRow> = engine("QualityDocs.controlPlan", a(r))
    fun apqp(r: FastenerCalculationResult): List<ChecklistPhase> = engine("QualityDocs.apqp", a(r))
    fun specialCharacteristics(r: FastenerCalculationResult): List<String> = engine("QualityDocs.specialCharacteristics", a(r))
    fun capabilityCharacteristics(r: FastenerCalculationResult): List<String> = engine("QualityDocs.capabilityCharacteristics", a(r))
    fun ppapElements(r: FastenerCalculationResult): List<PpapElement> = engine("QualityDocs.ppapElements", a(r))
    fun psw(r: FastenerCalculationResult, partNo: String, drawingRev: String, customer: String, level: Int, reason: String, csr: String): String =
        engine("QualityDocs.psw", a(r), a(partNo), a(drawingRev), a(customer), a(level), a(reason), a(csr))
    fun ecn(r: FastenerCalculationResult, number: String, fromRev: String, toRev: String, description: String, reason: String, triggers: Set<Int>, csr: String): String =
        engine("QualityDocs.ecn", a(r), a(number), a(fromRev), a(toRev), a(description), a(reason), a(triggers.sorted()), a(csr))
    fun coatingThicknessMethods(c: CoatingType): List<Pair<String, String>> = engine("QualityDocs.coatingThicknessMethods", a(c))

    val PSW_REASONS = listOf(
        "Initial submission", "Engineering change(s)", "Tooling: transfer, replacement, refurbishment or additional",
        "Correction of discrepancy", "Tooling inactive > 1 year", "Change to optional construction or material",
        "Sub-supplier or material source change", "Change in part processing", "Parts produced at additional location", "Other"
    )

    /** Customer notification cases from AIAG PPAP 4th edition, section 3.1; the customer's CSR can add more. */
    val CHANGE_TRIGGERS = listOf(
        "Use of other construction or material than in the approved part",
        "Production from new or modified tools (except perishable tools), dies or moulds, including additional or replacement tooling",
        "Production after upgrade or rearrangement of existing tooling or equipment",
        "Production from tooling and equipment transferred to a different or additional plant location",
        "Change of supplier for parts, non-equivalent materials or services (e.g. heat treatment, plating)",
        "Product produced after tooling has been inactive for volume production for 12 months or more",
        "Product and process changes on components made internally or by suppliers",
        "Change in test / inspection method (new technique, no change to acceptance criteria)",
        "Bulk materials: new source of raw material, or change in appearance attributes"
    )

    val TEST_METHODS = listOf(
        "Tensile and wedge load (bolts)" to "ISO 898-1 full-size tests; ISO 6892-1 machined test pieces",
        "Proof load (bolts / nuts)" to "ISO 898-1 / ISO 898-2; inch: ASTM F606",
        "Hardness" to "Rockwell ISO 6508-1, Vickers ISO 6507-1 (HV10 referee); surface HV0.3 for decarburisation",
        "Torque-tension / friction" to "ISO 16047 (total, thread and head friction coefficients)",
        "Prevailing torque" to "ISO 2320 (lock nuts); DIN 267-28 (polyamide patch)",
        "Hydrogen embrittlement" to "ISO 15330 sustained load; ASTM F1940 process control; ASTM F519 (aerospace)",
        "Salt spray" to "ISO 9227 NSS / ASTM B117; hours to white and red corrosion per drawing",
        "Thread gauging" to "ISO 1502 GO / NO-GO (metric); ASME B1.2 gauges, ASME B1.3 system 21 / 22 / 23 (inch)",
        "Dimensions" to "Caliper, micrometer, height gauge; profile projector for radii and chamfers; CMM / vision for GD&T",
        "Drive recess" to "Penetration gauges: ISO 4757 (cross recess), ISO 10664 (hexalobular)"
    )

    val GDT = listOf(
        Triple("Straightness", "Shank / thread axis", "V-blocks + dial indicator, or CMM"),
        Triple("Perpendicularity / total runout", "Bearing face to thread axis", "Threaded collet + dial indicator on the bearing face"),
        Triple("Coaxiality / position", "Head or socket to shank", "CMM, vision system, or functional gauge"),
        Triple("Symmetry", "Slot to head axis", "Profile projector / vision"),
        Triple("Position", "Cross hole, split-pin hole", "Functional pin gauge or CMM"),
        Triple("Profile / angle", "Countersink head, cone seat", "Profile projector, angle gauge")
    )

    val JIS_EQUIVALENTS = listOf(
        "JIS B 1180" to "ISO 4014 / 4017 / 4018 hex bolts and screws (old JIS sizes in the annex)",
        "JIS B 1181" to "ISO 4032 / 4033 / 4035 hex nuts",
        "JIS B 1176" to "ISO 4762 hexagon socket head cap screws",
        "JIS B 1177" to "ISO 4026 – 4029 hexagon socket set screws",
        "JIS B 1101" to "ISO 1207 / 1580 / 2009 / 2010 slotted head screws",
        "JIS B 1111" to "ISO 7045 / 7046 / 7047 cross recessed head screws",
        "JIS B 1256" to "ISO 7089 / 7090 / 7092 / 7093 plain washers",
        "JIS B 1051" to "ISO 898-1 bolt mechanical properties",
        "JIS B 1052-2" to "ISO 898-2 nut mechanical properties",
        "JIS B 0205" to "ISO 261 / 262 / 724 / 965-1 metric threads",
        "JIS B 0209" to "ISO 965 metric thread tolerances"
    )

    /** Old JIS B 1180 annex across-flats that differ from ISO (d to JIS s and ISO s). */
    val JIS_OLD_ACROSS_FLATS = listOf(Triple(10.0, 17.0, 16.0), Triple(12.0, 19.0, 18.0), Triple(14.0, 22.0, 21.0), Triple(22.0, 32.0, 34.0))

    val DMAIC = listOf(
        "Define" to "Problem, customer impact, scope, goal (e.g. cut head crack PPM by 80 %)",
        "Measure" to "Baseline PPM / Cpk; validate gauges with GR&R first",
        "Analyze" to "Root cause: 5-Why, fishbone, data by machine / shift / coil / heat",
        "Improve" to "Fix and verify with data (tooling, wire, settings, poka-yoke)",
        "Control" to "Update PFMEA and control plan, SPC, standard work"
    )

    val WASTES = listOf(
        "Transport" to "Bins moved between header, roller and furnace",
        "Inventory" to "WIP waiting for heat treatment or plating",
        "Motion" to "Searching for gauges and tools",
        "Waiting" to "Die change, first-off approval, furnace loading",
        "Over-production" to "Running beyond the order to avoid a die change",
        "Over-processing" to "Tighter tolerances than the drawing asks",
        "Defects" to "Cracks, mixed parts, thread damage, sorting",
        "Skills" to "Operator knowledge not used for improvement"
    )
}

object QualityEngine {
    fun parseValues(text: String): List<Double> = engine("QualityEngine.parseValues", a(text))
    fun capability(values: List<Double>, lsl: Double?, usl: Double?): Capability? = engine("QualityEngine.capability", a(values), a(lsl), a(usl))
    fun normalCdf(z: Double): Double = engine("QualityEngine.normalCdf", a(z))
    fun normalInv(p: Double): Double {
        require(p > 0 && p < 1)
        return engine<Double?>("QualityEngine.normalInv", a(p))!!
    }
    fun sigmaLevel(dpmo: Double): Double? = engine("QualityEngine.sigmaLevel", a(dpmo))
    fun dpmo(defects: Double, units: Double, opportunitiesPerUnit: Double): Double? = engine("QualityEngine.dpmo", a(defects), a(units), a(opportunitiesPerUnit))
    fun oee(plannedMin: Double, downtimeMin: Double, idealCycleSec: Double, totalCount: Double, goodCount: Double): Oee? =
        engine("QualityEngine.oee", a(plannedMin), a(downtimeMin), a(idealCycleSec), a(totalCount), a(goodCount))
    fun clearanceHoleMedium(d: Double): Double? = engine("QualityEngine.clearanceHoleMedium", a(d))
    fun torqueTension(d: Double, p: Double, rp02Mpa: Double, muThread: Double, muHead: Double, dwMm: Double, dhMm: Double, utilisation: Double = 0.9): TorqueTension? =
        engine("QualityEngine.torqueTension", a(d), a(p), a(rp02Mpa), a(muThread), a(muHead), a(dwMm), a(dhMm), a(utilisation))
    fun itGrade(grade: Int, size: Double): Double? = engine("QualityEngine.itGrade", a(grade), a(size))
}

object HeadingAnimation {
    const val SEC_PER_BLOW = 2.4
    const val HOLD_START_SEC = 1.0
    const val HOLD_END_SEC = 1.6
    fun blowCount(plan: HeadingPlan) = (plan.stations.size - 1).coerceAtLeast(0)
    fun durationSec(plan: HeadingPlan) = HOLD_START_SEC + blowCount(plan) * SEC_PER_BLOW + HOLD_END_SEC
    fun frameAt(plan: HeadingPlan, tSec: Double): AnimFrame = engine("HeadingAnimation.frameAt", a(plan), a(tSec))
    fun byVolume(st: HeadingStation, n: Int = 64): List<Pair<Double, Double>> = engine("HeadingAnimation.byVolume", a(st), a(n))
    fun outline(plan: HeadingPlan, f: AnimFrame, n: Int = 64): List<Pair<Double, Double>> = engine("HeadingAnimation.outline", a(plan), a(f), a(n))
}

object DrawingScan {
    fun calculate(spec: ExtractedFastenerSpec, headerChoice: HeaderChoice = HeaderChoice.AUTO): FastenerCalculationResult =
        engine("DrawingScan.calculate", a(spec), a(headerChoice), a(ShopDefaults.batchQty))
    fun volume(spec: ExtractedFastenerSpec, result: FastenerCalculationResult = calculate(spec)): DrawingVolume =
        engine("DrawingScan.volume", a(spec), a(result))
}

object SelectionFilter {
    private val registered = mutableSetOf<String>()

    /** The engine keeps its own copy of the materials for the filters; send any it has not seen. */
    private fun keysOf(all: Collection<MaterialInfo>): List<String> {
        val missing = all.filter { it.key !in registered }
        if (missing.isNotEmpty()) {
            engine<Int>("Engine.registerMaterials", a(missing))
            synchronized(registered) { registered += missing.map { it.key } }
        }
        return all.map { it.key }
    }

    fun hasThread(type: FastenerType) = !type.isUnthreaded && !type.isWasher
    fun usesPropertyClass(type: FastenerType) = !type.isWasher
    fun groupOf(m: MaterialInfo): MaterialGroup = engine("SelectionFilter.groupOfMaterial", a(m))
    fun groupOf(pc: PropertyClass): ClassGroup = engine("SelectionFilter.groupOfClass", a(pc))
    fun classAllows(pc: PropertyClass, m: MaterialInfo, dMm: Double = 10.0): Boolean = engine("SelectionFilter.classAllows", a(pc), a(m), a(dMm))

    fun materials(
        all: Collection<MaterialInfo>,
        group: MaterialGroup = MaterialGroup.ALL,
        pc: PropertyClass? = null,
        dMm: Double = 10.0,
        onlyForClass: Boolean = false,
        search: String = ""
    ): List<MaterialInfo> {
        val byKey = all.associateBy { it.key }
        val keys: List<String> = engine("SelectionFilter.materials", a(keysOf(all)), a(group), a(pc), a(dMm), a(onlyForClass), a(search))
        return keys.mapNotNull { byKey[it] }
    }

    fun materialGroups(all: Collection<MaterialInfo>): List<MaterialGroup> = engine("SelectionFilter.materialGroups", a(keysOf(all)))

    fun propertyClasses(type: FastenerType, unit: UnitSystem, group: ClassGroup = ClassGroup.ALL, search: String = ""): List<PropertyClass> =
        engine("SelectionFilter.propertyClasses", a(type), a(unit), a(group), a(search))

    fun classGroups(type: FastenerType): List<ClassGroup> = engine("SelectionFilter.classGroups", a(type))
    fun threadClasses(unit: UnitSystem, type: FastenerType, standard: ProductStandard): List<String> =
        engine("SelectionFilter.threadClasses", a(unit), a(type), a(standard))
}

/** An ISO 965 tolerance class, e.g. "4g6g" = pitch 4g, major 6g; "-MJ" marks the ISO 5855 controlled-root profile. */
@Serializable
data class IsoThreadClass(val pitchGrade: Int, val crestGrade: Int, val position: String, val isMj: Boolean) {
    companion object {
        fun parse(code: String): IsoThreadClass = engine("IsoThreadClass.parse", a(code))
        fun gradeFactor(grade: Int): Double = engine("IsoThreadClass.gradeFactor", a(grade))
    }
}
