package com.example.data

import kotlin.math.abs

/** CRITICAL blocks release, MANDATORY is a required process control for the traveller, WARNING needs engineering sign-off. */
enum class PokaYokeSeverity { CRITICAL, MANDATORY, WARNING }

data class PokaYokeIssue(
    val severity: PokaYokeSeverity,
    val title: String,
    val detail: String,
    val fix: String
)

enum class MaterialFamily { CARBON_ALLOY_STEEL, STAINLESS, TITANIUM, NICKEL_SUPERALLOY, ALUMINUM, COPPER_ALLOY }

/** ISO 898-1 / ISO 3506 minimum mechanical properties used for proof and breaking loads. */
data class ClassStrength(val rmMinMpa: Double, val yieldMinMpa: Double, val proofStressMpa: Double, val isIsoSteelClass: Boolean)

/**
 * Strict poka-yoke gate: checks every selection against the governing standards and returns
 * CRITICAL issues (the part must not be released as specified) and WARNINGs (needs an engineer's sign-off).
 */
object PokaYokeEngine {

    fun materialFamily(m: MaterialInfo): MaterialFamily {
        val k = m.key.uppercase()
        val n = m.name.uppercase()
        return when {
            k.startsWith("TI_") || k == "VT16" || n.contains("TITANIUM") -> MaterialFamily.TITANIUM
            k.startsWith("AL") && n.contains("ALLOY") || n.contains(" AL ") || n.contains("ALUMINUM") || n.contains("ALUMINIUM") -> MaterialFamily.ALUMINUM
            k.startsWith("BRASS") || k == "CUBE2" || n.contains("BRASS") || n.contains("COPPER") -> MaterialFamily.COPPER_ALLOY
            listOf("INCONEL", "WASPALOY", "RENE", "MP35N", "MP159", "HASTELLOY", "MONEL", "NIMONIC", "A286", "10KH11N23T3MR", "INVAR")
                .any { k.contains(it) } -> MaterialFamily.NICKEL_SUPERALLOY
            k.startsWith("SS") || k.contains("PH") || k.contains("X13") || k == "NITRONIC60" || k == "CUSTOM_455" ||
                k == "AM350" || k == "14KH17N2" || k == "12KH18N10T" || n.contains("STAINLESS") -> MaterialFamily.STAINLESS
            else -> MaterialFamily.CARBON_ALLOY_STEEL
        }
    }

    /** ISO 3506-1 A2/A4 classes cover only austenitic A2 (304-type) and A4 (316-type) stainless. */
    fun isAusteniticA2A4(m: MaterialInfo): Boolean {
        if (materialFamily(m) != MaterialFamily.STAINLESS) return false
        val id = (m.key + " " + m.name).uppercase()
        return id.contains("304") || id.contains("316")
    }

    /** NAS / MS aerospace standards are written for inch threads; EN aerospace standards for metric. */
    fun isInchStandard(s: ProductStandard) = s.category == "ASME/Imperial" || s.code.startsWith("NAS") || s.code.startsWith("MS")

    fun isMetricOnlyStandard(s: ProductStandard) = s.category == "ISO/DIN Metric" || s.code.startsWith("EN ")

    /**
     * Thread tolerance classes a product standard allows: ISO/DIN commercial parts 6g / 6H, plus 6f / 6e / 6h and
     * 6G for coated parts (ISO 4042) and 6AZ / 6AX galvanized nuts (ISO 10684); ASME B18.2.x 2A / 2B (1A / 1B for
     * galvanizing), ASME B18.3 socket screws 3A. null = no restriction (aerospace and special-thread standards).
     */
    fun standardThreadClasses(s: ProductStandard, isNut: Boolean): List<String>? = when {
        s == ProductStandard.DIN_7500 || s.category == "Aerospace/Defense" -> null
        s.category == "ISO/DIN Metric" -> if (isNut) listOf("6H", "6G", "6AZ", "6AX") else listOf("6g", "6f", "6e", "6h")
        // ASME / ASTM specs also have metric editions (A325M, A193M, A194M) that use the ISO commercial classes
        s == ProductStandard.ASME_B18_3 -> if (isNut) null else listOf("3A", "2A", "6g", "6f", "6e", "6h")
        else -> if (isNut) listOf("2B", "1B", "6H", "6G", "6AZ", "6AX") else listOf("2A", "1A", "6g", "6f", "6e", "6h")
    }

    /** Commercial grades (ISO 898-1, SAE / ASTM, ISO 3506) are made from commercial wire, not aerospace-spec alloy. */
    fun isCommercialClass(pc: PropertyClass) =
        classStrength(pc, 10.0, FastenerCatalog.MATERIALS.values.first()).isIsoSteelClass || pc in INCH_STEEL_CLASSES || pc in STAINLESS_CLASSES

    /** Fastener types bought to aerospace procurement specs rather than ISO 898-1 / SAE J429 grades. */
    fun isAerospaceType(type: FastenerType) = type.category == "Aerospace" || type == FastenerType.TWELVE_POINT

    /** Minimum strength the selected property class guarantees (ISO 898-1 Table 3 / ISO 3506-1). */
    fun classStrength(pc: PropertyClass, dMm: Double, material: MaterialInfo): ClassStrength = when (pc) {
        PropertyClass.CLASS_4_6 -> ClassStrength(400.0, 240.0, 225.0, true)
        PropertyClass.CLASS_4_8 -> ClassStrength(420.0, 340.0, 310.0, true)
        PropertyClass.CLASS_5_6 -> ClassStrength(500.0, 300.0, 280.0, true)
        PropertyClass.CLASS_5_8 -> ClassStrength(520.0, 420.0, 380.0, true)
        PropertyClass.CLASS_6_8 -> ClassStrength(600.0, 480.0, 440.0, true)
        PropertyClass.CLASS_8_8 -> if (dMm <= 16.0) ClassStrength(800.0, 640.0, 580.0, true) else ClassStrength(830.0, 660.0, 600.0, true)
        PropertyClass.CLASS_9_8 -> ClassStrength(900.0, 720.0, 650.0, true)
        PropertyClass.CLASS_10_9 -> ClassStrength(1040.0, 940.0, 830.0, true)
        PropertyClass.CLASS_12_9 -> ClassStrength(1220.0, 1100.0, 970.0, true)
        // ISO 898-2 nut classes are specified by proof load and hardness, not by Rm. The nut material still has to
        // carry the proof load, so the class's proof stress is used as the minimum strength the material must reach.
        PropertyClass.NUT_04 -> ClassStrength(380.0, 380.0, 380.0, true)
        PropertyClass.NUT_05 -> ClassStrength(500.0, 500.0, 500.0, true)
        PropertyClass.NUT_5 -> ClassStrength(500.0, 500.0, 500.0, true)
        PropertyClass.NUT_6 -> ClassStrength(600.0, 600.0, 600.0, true)
        PropertyClass.NUT_8 -> ClassStrength(800.0, 800.0, 800.0, true)
        PropertyClass.NUT_9 -> ClassStrength(900.0, 900.0, 900.0, true)
        PropertyClass.NUT_10 -> ClassStrength(1040.0, 1040.0, 1040.0, true)
        PropertyClass.NUT_12 -> ClassStrength(1150.0, 1150.0, 1150.0, true)
        // ISO 3506-1: A2 / A4 / C / F — the proof check uses the 0.2 % yield (Rp0.2) load
        PropertyClass.A2_70, PropertyClass.A4_70 -> ClassStrength(700.0, 450.0, 450.0, false)
        PropertyClass.A2_50, PropertyClass.A4_50 -> ClassStrength(500.0, 210.0, 210.0, false)
        PropertyClass.A2_80, PropertyClass.A4_80 -> ClassStrength(800.0, 600.0, 600.0, false)
        PropertyClass.C1_50 -> ClassStrength(500.0, 250.0, 250.0, false)
        PropertyClass.C1_70, PropertyClass.C4_70 -> ClassStrength(700.0, 410.0, 410.0, false)
        PropertyClass.C3_80 -> ClassStrength(800.0, 640.0, 640.0, false)
        PropertyClass.F1_60 -> ClassStrength(600.0, 410.0, 410.0, false)
        // SAE J995 / ASTM A563 / A194 nuts: proof stress of the class (the nut is not tensile tested)
        PropertyClass.SAE_NUT_5 -> ClassStrength(827.0, 827.0, 827.0, false)
        PropertyClass.SAE_NUT_8 -> ClassStrength(1034.0, 1034.0, 1034.0, false)
        PropertyClass.ASTM_A563_DH -> ClassStrength(1035.0, 1035.0, 1035.0, false)
        PropertyClass.ASTM_A194_2H -> ClassStrength(862.0, 862.0, 862.0, false)
        PropertyClass.ASTM_A194_8 -> ClassStrength(515.0, 515.0, 515.0, false)
        // ASTM structural and pressure-vessel grades (ksi → MPa)
        PropertyClass.ASTM_A325 -> if (dMm <= 25.4) ClassStrength(830.0, 660.0, 600.0, false) else ClassStrength(725.0, 560.0, 510.0, false)
        PropertyClass.ASTM_A490 -> ClassStrength(1035.0, 895.0, 825.0, false)
        PropertyClass.ASTM_A307_A, PropertyClass.ASTM_A307_B -> ClassStrength(414.0, 248.0, 228.0, false)
        PropertyClass.ASTM_A193_B16 -> if (dMm <= 63.5) ClassStrength(862.0, 724.0, 0.9 * 724.0, false) else ClassStrength(758.0, 655.0, 0.9 * 655.0, false)
        PropertyClass.ASTM_A320_L7 -> if (dMm <= 63.5) ClassStrength(862.0, 724.0, 0.9 * 724.0, false) else ClassStrength(793.0, 655.0, 0.9 * 655.0, false)
        PropertyClass.ASTM_A193_B8_CL1 -> ClassStrength(515.0, 205.0, 205.0, false)
        PropertyClass.ASTM_A193_B8M_CL2 -> if (dMm <= 19.05) ClassStrength(655.0, 448.0, 448.0, false) else ClassStrength(620.0, 345.0, 345.0, false)
        PropertyClass.ASTM_F1554_36 -> ClassStrength(400.0, 248.0, 228.0, false)
        PropertyClass.ASTM_F1554_105 -> ClassStrength(862.0, 724.0, 0.9 * 724.0, false)
        // SAE J429 minima (ksi → MPa): Grade 2 74/57/55 ≤ 3/4", 60/36/33 above; Grade 5 120/92/85 ≤ 1", 105/81/74 above; Grade 8 150/130/120
        PropertyClass.SAE_GRADE_2 -> if (dMm <= 19.05) ClassStrength(510.0, 393.0, 379.0, false) else ClassStrength(414.0, 248.0, 228.0, false)
        PropertyClass.SAE_GRADE_5 -> if (dMm <= 25.4) ClassStrength(827.0, 634.0, 586.0, false) else ClassStrength(724.0, 558.0, 510.0, false)
        PropertyClass.SAE_GRADE_8 -> ClassStrength(1034.0, 896.0, 827.0, false)
        // ASTM A574: 180 ksi ≤ 1/2", 170 ksi above, 153 ksi yield. ASTM A193 B7: 125/105 ksi ≤ 2-1/2", 115/95 above.
        // Neither table gives a proof stress here, so proof = 0.9 × yield, as for the other non-ISO classes.
        PropertyClass.ASTM_A574 -> ClassStrength(if (dMm <= 12.7) 1241.0 else 1172.0, 1055.0, 0.9 * 1055.0, false)
        PropertyClass.ASTM_A193_B7 -> if (dMm <= 63.5) ClassStrength(862.0, 724.0, 0.9 * 724.0, false) else ClassStrength(793.0, 655.0, 0.9 * 655.0, false)
        // Aerospace / non-ferrous: the procurement spec of the alloy governs, so use the material's own values
        else -> ClassStrength(material.postForgingTargetUtsMpa, material.yieldStrengthMpa, material.yieldStrengthMpa * 0.9, false)
    }

    /**
     * Minimum Rm a material must reach to be sold as this class: the tabulated value of the grade for the size
     * (ISO 898-1, SAE J429, ASTM, ISO 3506), or the bottom of the range where the alloy's own spec governs.
     */
    fun requiredRmMpa(pc: PropertyClass, dMm: Double, material: MaterialInfo): Double =
        if (isCommercialClass(pc)) classStrength(pc, dMm, material).rmMinMpa else pc.utsMpa

    internal val NUT_STANDARDS = setOf(
        ProductStandard.ISO_4032, ProductStandard.ISO_4033, ProductStandard.DIN_6923, ProductStandard.ISO_7040,
        ProductStandard.ISO_7042, ProductStandard.DIN_935, ProductStandard.DIN_1587, ProductStandard.DIN_929, ProductStandard.ASME_B18_2_2,
        ProductStandard.ASTM_A194, ProductStandard.MS21042, ProductStandard.MS20500,
        ProductStandard.DIN_74361_NUT, ProductStandard.HI_LOK_COLLAR_STD, ProductStandard.MS21047,
        ProductStandard.ISO_4035, ProductStandard.DIN_557, ProductStandard.DIN_6334, ProductStandard.DIN_508,
        ProductStandard.DIN_315, ProductStandard.DIN_466, ProductStandard.OEM_RIVET_NUT, ProductStandard.IFI_100,
        ProductStandard.OEM_CAGE_NUT, ProductStandard.ASTM_A563
    )
    internal val WASHER_STANDARDS = setOf(
        ProductStandard.ISO_7089, ProductStandard.ISO_7093, ProductStandard.DIN_127, ProductStandard.ASTM_F436,
        ProductStandard.DIN_6797, ProductStandard.DIN_6796, ProductStandard.DIN_137, ProductStandard.DIN_434,
        ProductStandard.ASME_B18_22_1
    )

    private val FULL_THREAD_TYPES = setOf(
        FastenerType.HEX_BOLT_FULL, FastenerType.SOCKET_SET, FastenerType.SOCKET_BUTTON,
        FastenerType.SOCKET_CSK, FastenerType.STUD, FastenerType.THREAD_FORMING_SCREW,
        FastenerType.SET_SCREW_CONE, FastenerType.SET_SCREW_DOG, FastenerType.SET_SCREW_CUP,
        FastenerType.SLOTTED_SET_SCREW, FastenerType.THREADED_ROD, FastenerType.CSK_SLOT, FastenerType.CSK_CROSS,
        FastenerType.RAISED_CSK_CROSS, FastenerType.PAN_SLOT, FastenerType.PAN_CROSS, FastenerType.CHEESE_SLOT,
        FastenerType.HEX_WASHER_SCREW, FastenerType.SEMS_SCREW, FastenerType.TAPPING_PAN, FastenerType.TAPPING_CSK,
        FastenerType.TAPPING_HEX, FastenerType.SELF_DRILLING, FastenerType.DRYWALL_SCREW
    )

    fun isFullyThreaded(type: FastenerType) = type in FULL_THREAD_TYPES

    /** ISO 3506-1 austenitic classes (A2 = 304-type, A4 = 316-type) plus the austenitic ASTM stud / nut grades. */
    internal val STAINLESS_CLASSES = setOf(
        PropertyClass.A2_50, PropertyClass.A2_70, PropertyClass.A2_80,
        PropertyClass.A4_50, PropertyClass.A4_70, PropertyClass.A4_80,
        PropertyClass.ASTM_A193_B8_CL1, PropertyClass.ASTM_A193_B8M_CL2, PropertyClass.ASTM_A194_8
    )
    /** ISO 3506-1 martensitic (C) and ferritic (F) classes: not austenitic, so they need a C/F-type stainless. */
    internal val MARTENSITIC_FERRITIC_CLASSES = setOf(
        PropertyClass.C1_50, PropertyClass.C1_70, PropertyClass.C3_80, PropertyClass.C4_70, PropertyClass.F1_60
    )
    /** ISO 898-2 steel nut classes: carbon / alloy steel, specified by proof load and hardness. */
    internal val ISO_NUT_CLASSES = setOf(
        PropertyClass.NUT_04, PropertyClass.NUT_05, PropertyClass.NUT_5, PropertyClass.NUT_6,
        PropertyClass.NUT_8, PropertyClass.NUT_9, PropertyClass.NUT_10, PropertyClass.NUT_12
    )
    /** Classes written for nuts only (ISO 898-2, SAE J995, ASTM A563 / A194). */
    internal val NUT_ONLY_CLASSES = ISO_NUT_CLASSES + setOf(
        PropertyClass.SAE_NUT_5, PropertyClass.SAE_NUT_8, PropertyClass.ASTM_A563_DH,
        PropertyClass.ASTM_A194_2H, PropertyClass.ASTM_A194_8
    )
    /** Inch steel grades: like ISO 898-1 they need carbon / alloy steel, but they are not ISO classes. */
    internal val INCH_STEEL_CLASSES = setOf(
        PropertyClass.SAE_GRADE_2, PropertyClass.SAE_GRADE_5, PropertyClass.SAE_GRADE_8, PropertyClass.ASTM_A574,
        PropertyClass.ASTM_A193_B7, PropertyClass.ASTM_A193_B16, PropertyClass.ASTM_A320_L7,
        PropertyClass.ASTM_A325, PropertyClass.ASTM_A490, PropertyClass.ASTM_A307_A, PropertyClass.ASTM_A307_B,
        PropertyClass.ASTM_F1554_36, PropertyClass.ASTM_F1554_105,
        PropertyClass.SAE_NUT_5, PropertyClass.SAE_NUT_8, PropertyClass.ASTM_A563_DH, PropertyClass.ASTM_A194_2H
    )
    internal val QT_CLASSES = setOf(
        PropertyClass.CLASS_8_8, PropertyClass.CLASS_9_8, PropertyClass.CLASS_10_9, PropertyClass.CLASS_12_9,
        PropertyClass.SAE_GRADE_5, PropertyClass.SAE_GRADE_8, PropertyClass.ASTM_A574, PropertyClass.ASTM_A193_B7,
        PropertyClass.ASTM_A193_B16, PropertyClass.ASTM_A320_L7, PropertyClass.ASTM_A325, PropertyClass.ASTM_A490,
        PropertyClass.ASTM_F1554_105, PropertyClass.ASTM_A194_2H, PropertyClass.ASTM_A563_DH, PropertyClass.SAE_NUT_8,
        PropertyClass.NUT_8, PropertyClass.NUT_9, PropertyClass.NUT_10, PropertyClass.NUT_12
    )
    internal val ALUMINIUM_ONLY_COATINGS = setOf(CoatingType.ANODIZE_II, CoatingType.HARD_ANODIZE, CoatingType.CHEM_FILM)
    internal val PHOSPHATE_COATINGS = setOf(CoatingType.PHOSPHATE, CoatingType.PHOSPHATE_MN)

    /** ISO 898-2 nut property class that mates with a bolt class (ISO 3506-2 / aerospace spec for others). */
    fun nutClassFor(pc: PropertyClass): String = when (pc) {
        PropertyClass.CLASS_4_6, PropertyClass.CLASS_4_8, PropertyClass.CLASS_5_6, PropertyClass.CLASS_5_8 -> "5"
        PropertyClass.CLASS_6_8 -> "6"
        PropertyClass.CLASS_9_8 -> "9"
        PropertyClass.CLASS_8_8 -> "8"
        PropertyClass.CLASS_10_9 -> "10"
        PropertyClass.CLASS_12_9 -> "12"
        PropertyClass.A2_70 -> "A2-70 (ISO 3506-2)"
        PropertyClass.A4_70 -> "A4-70 (ISO 3506-2)"
        PropertyClass.A2_50 -> "A2-50 (ISO 3506-2)"
        PropertyClass.A4_50 -> "A4-50 (ISO 3506-2)"
        PropertyClass.A2_80 -> "A2-80 (ISO 3506-2)"
        PropertyClass.A4_80 -> "A4-80 (ISO 3506-2)"
        PropertyClass.C1_50, PropertyClass.C1_70, PropertyClass.C3_80, PropertyClass.C4_70, PropertyClass.F1_60 ->
            "the same C / F class (ISO 3506-2)"
        PropertyClass.ASTM_A325, PropertyClass.ASTM_F1554_105 -> "ASTM A563 Grade C or DH"
        PropertyClass.ASTM_A490 -> "ASTM A563 Grade DH (or DH3)"
        PropertyClass.ASTM_A307_A, PropertyClass.ASTM_A307_B, PropertyClass.ASTM_F1554_36 -> "ASTM A563 Grade A"
        PropertyClass.ASTM_A193_B16, PropertyClass.ASTM_A320_L7 -> "ASTM A194 Grade 4 or 7"
        PropertyClass.ASTM_A193_B8_CL1 -> "ASTM A194 Grade 8"
        PropertyClass.ASTM_A193_B8M_CL2 -> "ASTM A194 Grade 8M"
        in NUT_ONLY_CLASSES -> "not applicable — this is already the nut class"
        PropertyClass.SAE_GRADE_2 -> "SAE J995 Grade 2"
        PropertyClass.SAE_GRADE_5 -> "SAE J995 Grade 5"
        PropertyClass.SAE_GRADE_8 -> "SAE J995 Grade 8"
        PropertyClass.ASTM_A193_B7 -> "ASTM A194 Grade 2H"
        PropertyClass.ASTM_A574 -> "per joint design (ASTM A563 / A194)"
        PropertyClass.MATERIAL_SPEC -> "per the mating nut's procurement spec (same alloy family)"
        else -> "per mating aerospace nut spec (NAS/MS)"
    }

    /** Plain-text poka-yoke section for exported reports, so documents carry the same gate as the screen. */
    fun reportSection(issues: List<PokaYokeIssue>): String = buildString {
        val critical = issues.count { it.severity == PokaYokeSeverity.CRITICAL }
        append("POKA-YOKE GATE: ")
        append(
            when {
                critical > 0 -> "RELEASE BLOCKED ($critical critical failure${if (critical > 1) "s" else ""})"
                issues.any { it.severity == PokaYokeSeverity.MANDATORY } -> "RELEASE WITH MANDATORY CONTROLS"
                issues.isNotEmpty() -> "OK - ENGINEERING SIGN-OFF ITEMS"
                else -> "ALL CHECKS PASSED"
            }
        )
        append('\n')
        issues.forEach { i ->
            append(" [${i.severity.name}] ${i.title}\n")
            append("    ${i.detail}\n")
            append("    Fix: ${i.fix}\n")
        }
    }

    /** The governing product standard for a fastener type in the chosen unit system. */
    fun defaultStandard(type: FastenerType, unit: UnitSystem): ProductStandard {
        if (unit == UnitSystem.INCH) {
            return when {
                type.isWasher -> ProductStandard.ASTM_F436
                type == FastenerType.AERO_LOCK_NUT -> ProductStandard.MS21042
                type == FastenerType.HI_LOK_COLLAR -> ProductStandard.HI_LOK_COLLAR_STD
                type == FastenerType.NUTPLATE -> ProductStandard.MS21047
                type == FastenerType.NAS_CLOSE_TOL_BOLT -> ProductStandard.NAS6203
                type == FastenerType.HI_LOK_PIN -> ProductStandard.HI_LOK
                type == FastenerType.TAPER_LOK -> ProductStandard.TAPER_LOK_STD
                type == FastenerType.LOCKBOLT_PIN -> ProductStandard.LOCKBOLT_STD
                type == FastenerType.BLIND_BOLT -> ProductStandard.BLIND_BOLT_STD
                type == FastenerType.HEXALOBULAR_SCREW -> ProductStandard.ASME_B18_3
                type == FastenerType.SOLID_RIVET -> ProductStandard.MS20470
                type == FastenerType.BLIND_RIVET -> ProductStandard.NAS1398
                type == FastenerType.HEAVY_HEX_NUT -> ProductStandard.ASTM_A194
                type == FastenerType.CASTLE_NUT -> ProductStandard.MS20500
                type == FastenerType.METAL_LOCK_NUT -> ProductStandard.MS21042
                type.isNut -> ProductStandard.ASME_B18_2_2
                type == FastenerType.TWELVE_POINT -> ProductStandard.NAS624
                type == FastenerType.HEAVY_HEX_BOLT -> ProductStandard.ASTM_A325
                type.isStud -> ProductStandard.ASTM_A193
                type == FastenerType.CLEVIS_BOLT -> ProductStandard.AN21
                type == FastenerType.SHEAR_BOLT -> ProductStandard.AN173
                type.category == "Machine Screws" -> ProductStandard.ASME_B18_6_3
                type.category == "Tapping & Wood Screws" -> ProductStandard.ASME_B18_6_4
                type.category == "Pins & Retainers" -> ProductStandard.ASME_B18_8_2
                type == FastenerType.TAP_END_STUD -> ProductStandard.ASME_B16_5
                type == FastenerType.ANCHOR_BOLT -> ProductStandard.ASTM_F1554
                type in setOf(
                    FastenerType.SHCS, FastenerType.SOCKET_CSK, FastenerType.SOCKET_BUTTON, FastenerType.SOCKET_SET,
                    FastenerType.SET_SCREW_CONE, FastenerType.SET_SCREW_DOG, FastenerType.SET_SCREW_CUP,
                    FastenerType.SHOULDER_BOLT
                ) -> ProductStandard.ASME_B18_3
                else -> ProductStandard.ASME_B18_2_1
            }
        }
        return when (type) {
            FastenerType.HEX_BOLT -> ProductStandard.ISO_4014
            FastenerType.HEX_BOLT_FULL -> ProductStandard.ISO_4017
            FastenerType.SHCS -> ProductStandard.ISO_4762
            FastenerType.SOCKET_CSK -> ProductStandard.ISO_10642
            FastenerType.SOCKET_BUTTON -> ProductStandard.ISO_7380
            FastenerType.SOCKET_SET -> ProductStandard.ISO_4026
            FastenerType.FLANGE_BOLT -> ProductStandard.ISO_1665
            // Metric aerospace bolts: the EN (MJ-thread) standard; NAS 624 / NAS 6203 are inch
            FastenerType.TWELVE_POINT -> ProductStandard.EN2882
            FastenerType.CARRIAGE_BOLT -> ProductStandard.DIN_603
            FastenerType.T_BOLT -> ProductStandard.DIN_186
            FastenerType.HEAVY_HEX_BOLT -> ProductStandard.ASTM_A325
            FastenerType.EYE_BOLT -> ProductStandard.DIN_444
            FastenerType.STUD, FastenerType.WHEEL_STUD, FastenerType.U_BOLT -> ProductStandard.ASTM_A193
            FastenerType.HEX_NUT -> ProductStandard.ISO_4032
            FastenerType.HEAVY_HEX_NUT -> ProductStandard.ASTM_A194
            FastenerType.FLANGE_NUT -> ProductStandard.DIN_6923
            FastenerType.NYLOC_NUT -> ProductStandard.ISO_7040
            FastenerType.METAL_LOCK_NUT -> ProductStandard.ISO_7042
            FastenerType.CASTLE_NUT -> ProductStandard.DIN_935
            FastenerType.ACORN_NUT -> ProductStandard.DIN_1587
            FastenerType.WELD_NUT -> ProductStandard.DIN_929
            FastenerType.FLAT_WASHER -> ProductStandard.ISO_7089
            FastenerType.SPRING_WASHER -> ProductStandard.DIN_127
            FastenerType.TTY_HEAD_BOLT -> ProductStandard.OEM_TTY
            FastenerType.HEXALOBULAR_SCREW -> ProductStandard.ISO_14579
            FastenerType.THREAD_FORMING_SCREW -> ProductStandard.DIN_7500
            FastenerType.WHEEL_BOLT -> ProductStandard.DIN_74361
            FastenerType.WHEEL_NUT -> ProductStandard.DIN_74361_NUT
            FastenerType.BANJO_BOLT -> ProductStandard.DIN_7623
            FastenerType.NAS_CLOSE_TOL_BOLT -> ProductStandard.EN2882
            FastenerType.HI_LOK_PIN -> ProductStandard.HI_LOK
            FastenerType.HI_LOK_COLLAR -> ProductStandard.HI_LOK_COLLAR_STD
            FastenerType.TAPER_LOK -> ProductStandard.TAPER_LOK_STD
            FastenerType.LOCKBOLT_PIN -> ProductStandard.LOCKBOLT_STD
            FastenerType.BLIND_BOLT -> ProductStandard.BLIND_BOLT_STD
            FastenerType.AERO_LOCK_NUT -> ProductStandard.MS21042
            FastenerType.NUTPLATE -> ProductStandard.MS21047
            FastenerType.SOLID_RIVET -> ProductStandard.MS20470
            FastenerType.BLIND_RIVET -> ProductStandard.NAS1398
            FastenerType.CLEVIS_BOLT -> ProductStandard.AN21
            FastenerType.SHEAR_BOLT -> ProductStandard.AN173
            FastenerType.SQUARE_BOLT -> ProductStandard.DIN_478
            FastenerType.SHOULDER_BOLT -> ProductStandard.ISO_7379
            FastenerType.SET_SCREW_CONE -> ProductStandard.ISO_4027
            FastenerType.SET_SCREW_DOG -> ProductStandard.ISO_4028
            FastenerType.SET_SCREW_CUP -> ProductStandard.ISO_4029
            FastenerType.SLOTTED_SET_SCREW -> ProductStandard.DIN_438
            FastenerType.THUMB_SCREW -> ProductStandard.DIN_464
            FastenerType.WING_SCREW -> ProductStandard.DIN_316
            FastenerType.HANGER_BOLT -> ProductStandard.DIN_7961
            FastenerType.ANCHOR_BOLT -> ProductStandard.ASTM_F1554
            FastenerType.LAG_SCREW -> ProductStandard.DIN_571
            FastenerType.PAN_SLOT -> ProductStandard.ISO_1580
            FastenerType.PAN_CROSS -> ProductStandard.ISO_7045
            FastenerType.CHEESE_SLOT -> ProductStandard.ISO_1207
            FastenerType.CSK_SLOT -> ProductStandard.ISO_2009
            FastenerType.CSK_CROSS -> ProductStandard.ISO_7046
            FastenerType.RAISED_CSK_CROSS -> ProductStandard.ISO_7047
            FastenerType.HEX_WASHER_SCREW, FastenerType.SEMS_SCREW -> ProductStandard.DIN_6900
            FastenerType.TAPPING_PAN -> ProductStandard.ISO_1481
            FastenerType.TAPPING_CSK -> ProductStandard.ISO_7050
            FastenerType.TAPPING_HEX -> ProductStandard.ISO_10509
            FastenerType.SELF_DRILLING -> ProductStandard.ISO_15480
            FastenerType.DRYWALL_SCREW -> ProductStandard.DIN_18182
            FastenerType.CHIPBOARD_SCREW -> ProductStandard.DIN_7505
            FastenerType.CONROD_BOLT -> ProductStandard.OEM_CONROD
            FastenerType.THREADED_ROD -> ProductStandard.DIN_975
            FastenerType.TAP_END_STUD -> ProductStandard.ASME_B16_5
            FastenerType.DOWEL_PIN -> ProductStandard.ISO_2338
            FastenerType.TAPER_PIN -> ProductStandard.ISO_2339
            FastenerType.CLEVIS_PIN -> ProductStandard.ISO_2341
            FastenerType.SPLIT_PIN -> ProductStandard.ISO_1234
            FastenerType.SPRING_PIN -> ProductStandard.ISO_8752
            FastenerType.GROOVED_PIN -> ProductStandard.ISO_8739
            FastenerType.RETAINING_RING -> ProductStandard.DIN_471
            FastenerType.THIN_NUT -> ProductStandard.ISO_4035
            FastenerType.HIGH_NUT -> ProductStandard.ISO_4033
            FastenerType.SQUARE_NUT -> ProductStandard.DIN_557
            FastenerType.COUPLING_NUT -> ProductStandard.DIN_6334
            FastenerType.T_SLOT_NUT -> ProductStandard.DIN_508
            FastenerType.CAGE_NUT -> ProductStandard.OEM_CAGE_NUT
            FastenerType.WING_NUT -> ProductStandard.DIN_315
            FastenerType.THUMB_NUT -> ProductStandard.DIN_466
            FastenerType.RIVET_NUT -> ProductStandard.OEM_RIVET_NUT
            FastenerType.CLINCH_NUT -> ProductStandard.IFI_100
            FastenerType.FENDER_WASHER -> ProductStandard.ISO_7093
            FastenerType.STRUCT_WASHER -> ProductStandard.ASTM_F436
            FastenerType.TOOTH_LOCK_WASHER -> ProductStandard.DIN_6797
            FastenerType.BELLEVILLE_WASHER -> ProductStandard.DIN_6796
            FastenerType.WAVE_WASHER -> ProductStandard.DIN_137
            FastenerType.TAPER_WASHER -> ProductStandard.DIN_434
        }
    }

    /** Thread length b for partially threaded bolts (ISO 4014 / ISO 4762 / ASME B18.2.1). */
    fun threadLengthB(type: FastenerType, unit: UnitSystem, d: Double, lengthMm: Double): Double {
        if (type.isUnthreaded) return 0.0
        if (isFullyThreaded(type)) return lengthMm
        val socketHead = type == FastenerType.SHCS || type == FastenerType.HEXALOBULAR_SCREW
        val b = if (unit == UnitSystem.INCH) {
            // ASME B18.3 socket head cap screws: LT = 2D + 1/2"; ASME B18.2.1 hex bolts: 2D + 1/4" (L ≤ 6"), 2D + 1/2"
            if (socketHead || lengthMm > 152.4) 2 * d + 12.7 else 2 * d + 6.35
        } else if (type == FastenerType.SHCS || type == FastenerType.HEXALOBULAR_SCREW) {
            2 * d + 12.0
        } else when {
            lengthMm <= 125.0 -> 2 * d + 6.0
            lengthMm <= 200.0 -> 2 * d + 12.0
            else -> 2 * d + 25.0
        }
        return minOf(lengthMm, b)
    }

    fun check(
        type: FastenerType,
        unit: UnitSystem,
        standard: ProductStandard,
        nominalSizeIndex: Int,
        pitchValue: Double,
        d: Double,
        lengthMm: Double,
        threadClass: String,
        propertyClass: PropertyClass?,
        material: MaterialInfo,
        coating: CoatingType,
        batchQty: Int,
        fundamentalDeviationMm: Double,
        pitchDiaMinMm: Double,
        pitchDiaMaxMm: Double,
        isCustomScan: Boolean,
        stressAreaMm2: Double = 0.0,
        headDims: HeadDimensions? = null
    ): List<PokaYokeIssue> {
        val issues = mutableListOf<PokaYokeIssue>()
        fun critical(t: String, det: String, fix: String) = issues.add(PokaYokeIssue(PokaYokeSeverity.CRITICAL, t, det, fix))
        fun warn(t: String, det: String, fix: String) = issues.add(PokaYokeIssue(PokaYokeSeverity.WARNING, t, det, fix))
        fun mandatory(t: String, det: String, fix: String) = issues.add(PokaYokeIssue(PokaYokeSeverity.MANDATORY, t, det, fix))

        val family = materialFamily(material)
        val isNut = type.isNut
        val isWasher = type.isWasher

        // ---- 1. Inputs ----
        if (lengthMm <= 0.0) critical("Invalid length", "Length is ${lengthMm} mm.", "Select a positive nominal length.")
        if (batchQty <= 0) critical("Invalid batch quantity", "Batch quantity is $batchQty.", "Enter a quantity of at least 1.")

        // ---- 2. Standard matches the product ----
        val stdIsNut = standard in NUT_STANDARDS
        val stdIsWasher = standard in WASHER_STANDARDS
        when {
            isNut && !stdIsNut -> critical("Standard does not fit a nut", "${standard.code} is not a nut standard, but ${type.label} is selected.", "Choose a nut standard (e.g. ISO 4032, ISO 7040, ASME B18.2.2).")
            isWasher && !stdIsWasher -> critical("Standard does not fit a washer", "${standard.code} is not a washer standard.", "Choose a washer standard (ISO 7089, DIN 127, ASTM F436).")
            !isNut && !isWasher && (stdIsNut || stdIsWasher) -> critical("Standard does not fit a bolt/screw", "${standard.code} is a ${if (stdIsNut) "nut" else "washer"} standard, but ${type.label} is selected.", "Choose the bolt/screw standard for this product.")
        }
        if (unit == UnitSystem.INCH && isMetricOnlyStandard(standard)) {
            critical("Metric standard with inch thread", "${standard.code} defines metric fasteners only.", "Use an ASME/ASTM standard for UNC/UNF threads, or switch to Metric.")
        }
        if (unit == UnitSystem.METRIC && isInchStandard(standard)) {
            warn("Inch standard with metric thread", "${standard.code} is an inch standard; the metric version has its own number (e.g. ASTM A325M / F3125M).", "Confirm the metric edition of the standard on the drawing.")
        }

        // ---- 3. Thread: pitch and tolerance class ----
        if (!isWasher && !isCustomScan) {
            if (unit == UnitSystem.METRIC) {
                val size = FastenerCatalog.METRIC_SIZES.getOrNull(nominalSizeIndex)
                if (size != null && size.pitches.none { abs(it - pitchValue) < 1e-6 }) {
                    critical("Non-standard pitch", "${size.label} does not have a ${pitchValue} mm pitch in ISO 261 (allowed: ${size.pitches.joinToString()}).", "Select a standard coarse or fine pitch.")
                }
            } else {
                val size = FastenerCatalog.INCH_SIZES.getOrNull(nominalSizeIndex)
                if (size != null && size.tpis.none { abs(it - pitchValue) < 1e-6 }) {
                    critical("Non-standard TPI", "${size.label} does not have ${pitchValue.toInt()} TPI in ASME B1.1 (allowed: ${size.tpis.joinToString()}).", "Select the UNC or UNF thread for this size.")
                }
            }
            val stdClasses = standardThreadClasses(standard, isNut)?.filter { it in ThreadClassCatalog.getOptions(unit, isNut) }?.ifEmpty { null }
            if (stdClasses != null && threadClass !in stdClasses && threadClass in ThreadClassCatalog.getOptions(unit, isNut)) {
                warn("Thread class not per ${standard.code}", "${standard.code} specifies ${stdClasses.first()}${if (stdClasses.size > 1) " (or ${stdClasses.drop(1).joinToString(" / ")} for coated parts)" else ""}; $threadClass is a special class.", "Use ${stdClasses.joinToString(" / ")}, or put the special class on the drawing as a deviation.")
            }
            val validClasses = ThreadClassCatalog.getOptions(unit, isNut)
            if (threadClass !in validClasses) {
                critical("Wrong thread tolerance class", "'$threadClass' is not a valid ${if (isNut) "internal" else "external"} ${unit.name.lowercase()} class.", "Use one of: ${validClasses.joinToString()}.")
            }
        }
        if (isWasher) {
            warn("Washer selected", "Washers have no thread, so the thread limits, rolling blank and G-code are not applicable.", "Use the weight, material, coating and hardness results only.")
        }

        // ---- 4. Material vs property class ----
        // Washers are not graded by bolt property class, so the class never drives their checks
        val pc = if (isWasher) null else propertyClass
        if (pc != null && !isWasher) {
            val cs = classStrength(pc, d, material)
            if ((cs.isIsoSteelClass || pc in INCH_STEEL_CLASSES) && family != MaterialFamily.CARBON_ALLOY_STEEL) {
                critical("Class ${pc.label.substringBefore(" (")} needs carbon/alloy steel", "ISO 898-1 classes apply only to carbon and alloy steel; ${material.name} is ${family.name.lowercase().replace('_', ' ')}.", "Use the material's own class (A2-70/A4-80 for austenitic stainless, aerospace class for Ti/Ni alloys) or change the material.")
            } else if (pc in STAINLESS_CLASSES && !isAusteniticA2A4(material)) {
                critical("${pc.label.substringBefore(" (")} needs A2/A4 austenitic stainless", "ISO 3506-1 A2/A4 classes cover 304-type (A2) and 316-type (A4) steels only; ${material.name} is not one of them.", "Select SS304 / SS316 / SS316L, or use 'Per material spec' for martensitic, PH and other stainless grades.")
            } else if (pc in MARTENSITIC_FERRITIC_CLASSES && family != MaterialFamily.STAINLESS) {
                critical("${pc.label.substringBefore(" (")} needs a stainless steel", "ISO 3506-1 C and F classes are martensitic (C) and ferritic (F) stainless grades; ${material.name} is ${family.name.lowercase().replace('_', ' ')}.", "Select a martensitic / ferritic stainless (410, 1.4057, 420) or change the class.")
            } else if (pc in MARTENSITIC_FERRITIC_CLASSES && isAusteniticA2A4(material)) {
                critical("${pc.label.substringBefore(" (")} is not an austenitic class", "C and F classes are hardened martensitic / ferritic grades; ${material.name} is an austenitic 304/316-type steel that cannot be quenched and tempered.", "Use A2 / A4 for austenitic stainless, or select a martensitic grade.")
            } else if (pc == PropertyClass.MATERIAL_SPEC && family == MaterialFamily.CARBON_ALLOY_STEEL && !material.isAero) {
                critical("Commercial steel needs a grade", "${material.name} fasteners are graded by ISO 898-1 (metric) or SAE J429 / ASTM (inch).", "Select the ISO / SAE property class.")
            } else if (pc == PropertyClass.AERO_AL && family != MaterialFamily.ALUMINUM) {
                critical("Aluminium class with non-aluminium material", "${material.name} is not an aluminium alloy.", "Select an aluminium alloy (2024, 7075, 2219) or change the class.")
            } else if (pc == PropertyClass.AERO_1100 && family != MaterialFamily.TITANIUM) {
                warn("Titanium class with non-titanium material", "Class ${pc.label} is defined for Ti-6Al-4V; ${material.name} selected.", "Confirm the procurement spec, or select a titanium alloy.")
            }
            val requiredRm = requiredRmMpa(pc, d, material)
            if (material.postForgingTargetUtsMpa < requiredRm * 0.98) {
                critical("Material cannot reach the class strength", "${material.name} reaches about ${material.postForgingTargetUtsMpa.toInt()} MPa, but ${pc.label.substringBefore(" (")} needs at least ${requiredRm.toInt()} MPa.", "Select a stronger, hardenable material (e.g. 4140/42CrMo4 for 12.9, 10B21 or 4135 for 10.9).")
            }
            if (pc in NUT_ONLY_CLASSES && !isNut) {
                critical("Nut class on a ${if (type.isStud) "stud" else "bolt / screw"}", "${pc.label.substringBefore(" (")} is a nut class: it is specified by proof load and hardness, with no tensile strength for a threaded shank.", "Select the bolt class (ISO 898-1 for metric, SAE J429 / ASTM for inch).")
            }
            if (isNut && pc !in NUT_ONLY_CLASSES && pc !in STAINLESS_CLASSES && pc != PropertyClass.MATERIAL_SPEC && !isAerospaceType(type)) {
                warn("Bolt class on a nut", "${pc.label.substringBefore(" (")} is a bolt class; a nut is graded by ISO 898-2 (class ${nutClassFor(pc)}) or by SAE J995 / ASTM A563 / A194 in inch.", "Select the matching nut class so the proof load and hardness limits are the nut's own.")
            }
            if (pc == PropertyClass.NUT_04 || pc == PropertyClass.NUT_05) {
                if (type != FastenerType.THIN_NUT) {
                    warn("Thin-nut class on a full-height nut", "ISO 898-2 classes 04 and 05 are for thin nuts (m < 0.8 d) with a reduced proof load; ${type.label.substringBefore(" (")} is a full-height nut.", "Use class 5 / 6 / 8 / 10 / 12 for a full-height nut.")
                }
            }
            if (pc == PropertyClass.CLASS_9_8 && d > 16.0) {
                critical("Class 9.8 is limited to M16", "ISO 898-1 defines class 9.8 only for d ≤ 16 mm; M$d selected.", "Use class 8.8 or 10.9 for this size.")
            }
            if (material.isAero && isCommercialClass(pc)) {
                warn("Aerospace alloy on a commercial class", "${material.name} is bought to an aerospace (AMS / VAR) specification; ${pc.label.substringBefore(" (")} is a commercial grade made from commercial wire.", "Use a commercial grade (e.g. 10B21, 42CrMo4, 4140) or class 'Per material spec' for the aerospace alloy.")
            }
            if (isAerospaceType(type) && (cs.isIsoSteelClass || pc in INCH_STEEL_CLASSES || pc in STAINLESS_CLASSES)) {
                warn("Commercial class on an aerospace fastener", "${type.label.substringBefore(" (")} is bought to an aerospace procurement spec (NAS / MS / AMS), not ${pc.label.substringBefore(" (")}.", "Use an aerospace class or 'Per material spec' with the alloy's AMS specification.")
            }
            if (pc in INCH_STEEL_CLASSES && unit == UnitSystem.METRIC && pc != PropertyClass.ASTM_A574 && pc != PropertyClass.ASTM_A193_B7) {
                warn("SAE J429 grade on a metric thread", "SAE J429 grades are defined for inch fasteners.", "Use an ISO 898-1 property class for metric threads.")
            }
            if (pc in QT_CLASSES && !material.hasHighHardness && family == MaterialFamily.CARBON_ALLOY_STEEL) {
                critical("Material is not quench-and-temper grade", "${material.name} cannot be quenched and tempered to ${pc.label.substringBefore(" (")}.", "Use a medium-carbon, boron or alloy steel.")
            }
        }

        // ---- 5. Coating vs material ----
        val effectiveRm = if (pc != null) classStrength(pc, d, material).rmMinMpa else material.postForgingTargetUtsMpa
        val isZincBased = coating.name.startsWith("ZN") || coating.name.startsWith("ZINC") || coating == CoatingType.HOT_DIP_GALV
        val isPassivation = coating.name.startsWith("PASSIVATION")
        val isCadmium = coating.name.contains("CAD")

        if (isPassivation && family != MaterialFamily.STAINLESS) {
            critical("Passivation on non-stainless material", "Passivation (ASTM A967 / AMS 2700) only works on stainless steel; ${material.name} will rust.", "Select a protective coating (Zn-Ni, zinc flake, phosphate & oil).")
        }
        if (family == MaterialFamily.TITANIUM && isCadmium) {
            critical("Cadmium on titanium is prohibited", "Cadmium causes solid-metal embrittlement of titanium (forbidden by aerospace specs).", "Use silver plating, aluminium-pigmented coating or dry-film lubricant.")
        }
        if (family == MaterialFamily.TITANIUM && coating.isElectroplating) {
            critical("Electroplating on titanium", "Titanium fasteners are not electroplated: hydrogen pick-up embrittles titanium.", "Use anodizing, dry-film lubricant or an aluminium-pigmented coating.")
        }
        if ((family == MaterialFamily.STAINLESS || family == MaterialFamily.NICKEL_SUPERALLOY) && isZincBased) {
            warn("Zinc coating on corrosion-resistant alloy", "${material.name} does not need sacrificial zinc; zinc also melts/embrittles above ~400 °C.", "Use passivation (stainless) or silver/dry-film lubricant (superalloys).")
        }
        if (family == MaterialFamily.ALUMINUM && (isZincBased || isCadmium)) {
            warn("Zinc/cadmium on aluminium fastener", "Aluminium fasteners are normally anodized or chromate-converted.", "Specify anodize (MIL-A-8625) or chemical conversion coating.")
        }
        if (isCadmium && !material.isAero) {
            warn("Cadmium is restricted", "Cadmium is banned for general industry under RoHS/REACH; allowed only for aerospace/defence.", "Use Zn-Ni (AMS 2417) as the cadmium replacement.")
        }

        // ---- 6. Hydrogen embrittlement (ISO 4042 / ASTM F1941 / ASTM F1940) ----
        val steelLike = family == MaterialFamily.CARBON_ALLOY_STEEL || family == MaterialFamily.STAINLESS && material.hasHighHardness
        if (coating.isElectroplating && steelLike && effectiveRm >= 1000.0) {
            if (pc == PropertyClass.CLASS_12_9 || effectiveRm >= 1200.0) {
                critical("Electroplating ≥ 1200 MPa fasteners", "ISO 4042 does not recommend electroplating for class 12.9 / ≥ 1200 MPa: baking cannot reliably remove the hydrogen-embrittlement risk.", "Use a non-electrolytic zinc-flake coating (Geomet / Dacromet, ISO 10683) or mechanical zinc.")
            } else {
                mandatory("De-embrittlement bake required", "${material.name} at ${effectiveRm.toInt()} MPa with ${coating.label}: hydrogen embrittlement risk.", "Bake 190–220 °C for 8–24 h, starting within 4 h of plating (ISO 4042 / ASTM F1941), and verify with ASTM F1940 / ISO 15330.")
            }
            if (type == FastenerType.NYLOC_NUT) {
                warn("Nylon insert vs bake temperature", "The polyamide insert is damaged above ~120 °C, but the bake is ~200 °C.", "Plate and bake the nut body first, then insert the nylon ring.")
            }
        }

        // ---- 7. Hot-dip galvanizing (ISO 10684) ----
        if (coating == CoatingType.HOT_DIP_GALV) {
            if (pc == PropertyClass.CLASS_12_9 || effectiveRm >= 1200.0) {
                critical("Hot-dip galvanizing on 12.9", "ISO 10684 excludes class 12.9: risk of liquid-metal and hydrogen embrittlement.", "Use zinc flake (ISO 10683) or change the class.")
            } else if (pc == PropertyClass.CLASS_10_9) {
                warn("Hot-dip galvanizing on 10.9", "Allowed by ISO 10684 only with special process control (e.g. ASTM F3125 Grade A490 forbids it).", "Confirm the customer standard permits HDG for 10.9.")
            }
            if (d < 8.0) {
                warn("Hot-dip galvanizing below M8", "ISO 10684 covers M8–M64; smaller threads fill with zinc.", "Use electro-zinc or zinc flake for small sizes.")
            }
            if (!isNut && !isWasher && !type.isUnthreaded && !threadClass.contains("e") && !threadClass.contains("1A")) {
                critical("HDG thread needs oversize fit", "45–65 µm zinc will not fit a ${threadClass} external thread.", "Use 6az / 6e external with oversize-tapped nuts (6AZ/6AX per ISO 965-5), or undercut the bolt thread before galvanizing.")
            }
        }

        // ---- 7b. Coating suits the material ----
        if (coating in ALUMINIUM_ONLY_COATINGS && family != MaterialFamily.ALUMINUM) {
            critical("${coating.label.substringBefore(" (")} is for aluminium only", "${material.name} is not an aluminium alloy; anodizing and chromate conversion only form on aluminium.", "Choose a coating made for this material (e.g. Zn-Ni or zinc flake for steel, passivation for stainless).")
        }
        if (coating == CoatingType.TI_ANODIZE && family != MaterialFamily.TITANIUM) {
            critical("Titanium anodize needs a titanium alloy", "${material.name} is not titanium.", "Select a titanium alloy or a coating made for this material.")
        }
        // Phosphating (ISO 9717 / DIN 50942) grows a zinc- or manganese-phosphate crystal on free iron: there is none
        // in stainless, titanium, aluminium, nickel or copper alloys, so no coating forms.
        if (coating in PHOSPHATE_COATINGS && family != MaterialFamily.CARBON_ALLOY_STEEL) {
            critical("Phosphating needs carbon or alloy steel", "${coating.label.substringBefore(" (")} converts free iron in the surface; ${material.name} is ${family.name.lowercase().replace('_', ' ')} and will not take the coating.", "Use passivation (stainless), anodize (titanium / aluminium) or a zinc-flake / dry-film coating.")
        }
        // Black oxide is the magnetite conversion of a ferrous surface (and a sulphide finish on copper alloys)
        if (coating == CoatingType.BLACK_OXIDE && (family == MaterialFamily.ALUMINUM || family == MaterialFamily.TITANIUM)) {
            critical("Black oxide needs a ferrous or copper alloy", "${material.name} forms no black oxide: there is no iron to convert to magnetite.", "Use black anodize (MIL-PRF-8625 Type II, dyed) for aluminium, or a dry-film lubricant for titanium.")
        }

        // ---- 8. Coating thickness vs thread allowance (ISO 4042: coating adds 4 × t to the pitch diameter) ----
        if (!type.isUnthreaded && !isWasher && coating.thicknessMaxUm > 2.0 && coating != CoatingType.HOT_DIP_GALV) {
            val allowanceUm = abs(fundamentalDeviationMm) * 1000.0
            val growthUm = 4.0 * coating.thicknessMaxUm
            val excessUm = growthUm - allowanceUm
            if (excessUm > 0.5) {
                val msg = "Max coating ${coating.thicknessMaxUm.toInt()} µm adds ~${growthUm.toInt()} µm to the pitch diameter; class $threadClass only has ${allowanceUm.toInt()} µm allowance."
                if (isNut) {
                    warn("Coating thicker than nut thread allowance", msg, "Use 6G (internal allowance) or mask the internal thread during coating.")
                } else {
                    // Pre-plate d2 must sit low enough that the plated thread still enters the GO ring gauge
                    val prePlateMax = pitchDiaMaxMm - excessUm / 1000.0
                    if (prePlateMax <= pitchDiaMinMm) {
                        critical("Coating consumes the whole thread tolerance", "$msg Pre-plate pitch diameter would have to be below the lower limit ${"%.3f".format(java.util.Locale.US, pitchDiaMinMm)} mm.", "Use class 6f/6e (2A for inch) or a thinner coating.")
                    } else {
                        mandatory("Roll thread undersize before plating", msg, "Pre-plate pitch diameter ${"%.3f".format(java.util.Locale.US, pitchDiaMinMm)} – ${"%.3f".format(java.util.Locale.US, prePlateMax)} mm (instead of up to ${"%.3f".format(java.util.Locale.US, pitchDiaMaxMm)} mm), then check with the GO/NO-GO gauge after plating.")
                    }
                }
            }
        }

        // ---- 8b. Bolt–nut matching (ISO 898-2: nut class ≥ first digit of the bolt class) ----
        if (!type.isUnthreaded && pc != null && !isWasher) {
            val nutClass = nutClassFor(pc)
            val minLoadKn = classStrength(pc, d, material).rmMinMpa * stressAreaMm2 / 1000.0
            val isoNut = classStrength(pc, d, material).isIsoSteelClass
            if (isNut) {
                mandatory(if (isoNut) "Nut class $nutClass (ISO 898-2)" else "Nut grade: $nutClass", "Matches ${pc.label.substringBefore(" (")} bolts. Proof load must be at least the mating bolt's minimum breaking load, ${"%.1f".format(java.util.Locale.US, minLoadKn)} kN, so the bolt breaks before the thread strips.", "Mark the nut '$nutClass' and certify the proof-load test per ISO 898-2.")
                if (standard == ProductStandard.ISO_4033) {
                    warn("Thin nut (ISO 4033)", "Thin/jam nuts are classes 04/05 and cannot carry the full bolt load.", "Use ISO 4032 or ISO 4033 only as a lock/jam nut.")
                }
            } else {
                mandatory(if (isoNut) "Mate with nut class $nutClass or higher" else "Mate with nut: $nutClass", "A lower-class nut strips before a ${pc.label.substringBefore(" (")} bolt reaches its ${"%.1f".format(java.util.Locale.US, minLoadKn)} kN breaking load.",
                    if (isoNut) "Specify the nut on the BOM as class $nutClass+ (e.g. ISO 4032-$nutClass) with the same thread size." else "Specify the nut on the BOM as $nutClass with the same thread size.")
            }
        }

        // ---- 9. Geometry & manufacturability ----
        if (type.isUnthreaded) {
            warn("Rivet joints are designed in shear", "Tensile proof load, thread-root Kt and fatigue figures assume a threaded bolt; for a rivet they are shank-area values only.", "Size the joint from rivet shear and bearing allowables (MMPDS) and the rivet procurement spec.")
        }
        if (!isWasher && headDims != null && !headDims.isTable && headDims.source.startsWith("Approximated")) {
            warn("No dimension table for this type", "${headDims.source}.", "Take head and nut dimensions from the part drawing before releasing weights, tooling or G-code.")
        } else if (!isWasher && headDims != null && !headDims.isTable) {
            warn("Head/nut size estimated", "M$d is outside the ISO/ASME dimension tables, so head and nut dimensions are proportional estimates.", "Take head dimensions from the drawing before releasing weights, tooling or G-code.")
        }
        if (!isNut && !isWasher && type == FastenerType.HEX_BOLT) {
            val b = threadLengthB(type, unit, d, lengthMm)
            if (lengthMm < b + d) {
                val std = if (unit == UnitSystem.METRIC) "ISO 4014" else "ASME B18.2.1"
                val full = if (unit == UnitSystem.METRIC) "ISO 4017 (fully threaded)" else "a fully threaded hex tap bolt"
                warn("Too short for a partially threaded bolt", "$std needs L ≥ b + plain shank; at L = ${lengthMm.toInt()} mm the thread length b = ${b.toInt()} mm leaves almost no shank.", "Use $full for this length.")
            }
        }
        if (!isNut && !isWasher && !type.isUnthreaded && lengthMm > 0 && lengthMm < 2.0 * d && type != FastenerType.SOCKET_SET) {
            warn("Very short length for diameter", "L = ${lengthMm} mm is less than 2 × d (${(2 * d)} mm).", "Check the head can be formed and the thread fully engaged.")
        }
        if (!material.isAero && d < 1.6 && !isWasher) {
            warn("Below standard size range", "ISO 4014/4017/4032 start at M1.6; M${d} is micro-screw territory (ISO 1207 / ISO 7045).", "Confirm the micro-fastener standard and specialized heading/rolling equipment.")
        }
        if (type == FastenerType.NYLOC_NUT && (family == MaterialFamily.NICKEL_SUPERALLOY || material.isAero && family != MaterialFamily.ALUMINUM)) {
            warn("Nylon insert in high-temperature alloy", "Nylon-insert nuts are limited to ~120 °C service.", "Use an all-metal prevailing-torque nut (ISO 7042 / MS21042).")
        }

        return issues.sortedBy { it.severity.ordinal }
    }
}
