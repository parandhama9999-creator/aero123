package com.example.data

import kotlin.math.abs

/** Head / nut geometry in mm. isTable = false means the size is outside the standard table and was estimated. */
data class HeadDimensions(
    val acrossFlatsS: Double,   // hex s, or the head diameter dk of a round head
    val headHeightK: Double,
    val bearingDiaDw: Double,   // min bearing face diameter
    val nutHeightM: Double,
    val source: String,
    val isTable: Boolean,
    /** Square neck height f of a carriage bolt (DIN 603); null for every other head. */
    val neckHeightMm: Double? = null
)

data class WasherDimensions(val innerDiaMm: Double, val outerDiaMm: Double, val thicknessMm: Double, val source: String, val isTable: Boolean)

/** Nominal dimensions from ISO 4014/4017/4032 (hex), ISO 4762, ISO 10642, ISO 7380, DIN 603, ISO 7089 and ASME B18.2.1/B18.2.2/B18.3. */
object DimensionTables {
    // d -> s, k (ISO 4014 nominal), m (ISO 4032 max), dw (ISO 4014 grade A min)
    private val ISO_HEX = mapOf(
        1.6 to doubleArrayOf(3.2, 1.1, 1.3, 2.27), 2.0 to doubleArrayOf(4.0, 1.4, 1.6, 3.07),
        2.5 to doubleArrayOf(5.0, 1.7, 2.0, 4.07), 3.0 to doubleArrayOf(5.5, 2.0, 2.4, 4.57),
        4.0 to doubleArrayOf(7.0, 2.8, 3.2, 5.88), 5.0 to doubleArrayOf(8.0, 3.5, 4.7, 6.88),
        6.0 to doubleArrayOf(10.0, 4.0, 5.2, 8.88), 8.0 to doubleArrayOf(13.0, 5.3, 6.8, 11.63),
        10.0 to doubleArrayOf(16.0, 6.4, 8.4, 14.63), 12.0 to doubleArrayOf(18.0, 7.5, 10.8, 16.63),
        14.0 to doubleArrayOf(21.0, 8.8, 12.8, 19.64), 16.0 to doubleArrayOf(24.0, 10.0, 14.8, 22.49),
        20.0 to doubleArrayOf(30.0, 12.5, 18.0, 28.19), 24.0 to doubleArrayOf(36.0, 15.0, 21.5, 33.61),
        30.0 to doubleArrayOf(46.0, 18.7, 25.6, 42.75), 36.0 to doubleArrayOf(55.0, 22.5, 31.0, 51.11)
    )
    // d -> head dk max, k = d (ISO 4762)
    private val ISO_SHCS_DK = mapOf(
        1.6 to 3.0, 2.0 to 3.8, 2.5 to 4.5, 3.0 to 5.5, 4.0 to 7.0, 5.0 to 8.5, 6.0 to 10.0, 8.0 to 13.0,
        10.0 to 16.0, 12.0 to 18.0, 14.0 to 21.0, 16.0 to 24.0, 20.0 to 30.0, 24.0 to 36.0, 30.0 to 45.0, 36.0 to 54.0
    )
    // d (inch) -> cap screw F, H; nut F, H (ASME B18.2.1 / B18.2.2), all inch
    private val ASME_HEX = mapOf(
        0.25 to doubleArrayOf(0.4375, 0.156, 0.4375, 0.219), 0.3125 to doubleArrayOf(0.5, 0.203, 0.5, 0.266),
        0.375 to doubleArrayOf(0.5625, 0.234, 0.5625, 0.328), 0.4375 to doubleArrayOf(0.625, 0.270, 0.6875, 0.375),
        0.5 to doubleArrayOf(0.75, 0.312, 0.75, 0.438), 0.5625 to doubleArrayOf(0.8125, 0.348, 0.875, 0.484),
        0.625 to doubleArrayOf(0.9375, 0.391, 0.9375, 0.547), 0.75 to doubleArrayOf(1.125, 0.469, 1.125, 0.641),
        0.875 to doubleArrayOf(1.3125, 0.547, 1.3125, 0.750), 1.0 to doubleArrayOf(1.5, 0.627, 1.5, 0.859)
    )

    // d -> hex socket key s, socket depth t min (ISO 4762)
    private val ISO_SHCS_SOCKET = mapOf(
        1.6 to doubleArrayOf(1.5, 0.7), 2.0 to doubleArrayOf(1.5, 1.0), 2.5 to doubleArrayOf(2.0, 1.1),
        3.0 to doubleArrayOf(2.5, 1.3), 4.0 to doubleArrayOf(3.0, 2.0), 5.0 to doubleArrayOf(4.0, 2.5),
        6.0 to doubleArrayOf(5.0, 3.0), 8.0 to doubleArrayOf(6.0, 4.0), 10.0 to doubleArrayOf(8.0, 5.0),
        12.0 to doubleArrayOf(10.0, 6.0), 14.0 to doubleArrayOf(12.0, 7.0), 16.0 to doubleArrayOf(14.0, 8.0),
        20.0 to doubleArrayOf(17.0, 10.0), 24.0 to doubleArrayOf(19.0, 12.0), 30.0 to doubleArrayOf(22.0, 15.5),
        36.0 to doubleArrayOf(27.0, 19.0)
    )

    // ISO 10642 countersunk socket screws: d -> dk (theoretical max), k max, socket key s, socket depth t min
    private val ISO_CSK = mapOf(
        3.0 to doubleArrayOf(6.72, 1.86, 2.0, 1.1), 4.0 to doubleArrayOf(8.96, 2.48, 2.5, 1.5),
        5.0 to doubleArrayOf(11.2, 3.1, 3.0, 1.9), 6.0 to doubleArrayOf(13.44, 3.72, 4.0, 2.2),
        8.0 to doubleArrayOf(17.92, 4.96, 5.0, 3.0), 10.0 to doubleArrayOf(22.4, 6.2, 6.0, 3.6),
        12.0 to doubleArrayOf(26.88, 7.44, 8.0, 4.3), 14.0 to doubleArrayOf(30.8, 8.4, 10.0, 4.5),
        16.0 to doubleArrayOf(33.6, 8.8, 10.0, 4.8), 20.0 to doubleArrayOf(40.32, 10.16, 12.0, 5.6)
    )
    // ISO 7380-1 button head socket screws: d -> dk max, k max, socket key s, socket depth t min
    private val ISO_BUTTON = mapOf(
        3.0 to doubleArrayOf(5.7, 1.65, 2.0, 1.04), 4.0 to doubleArrayOf(7.6, 2.2, 2.5, 1.3),
        5.0 to doubleArrayOf(9.5, 2.75, 3.0, 1.56), 6.0 to doubleArrayOf(10.5, 3.3, 4.0, 2.08),
        8.0 to doubleArrayOf(14.0, 4.4, 5.0, 2.6), 10.0 to doubleArrayOf(17.5, 5.5, 6.0, 3.12),
        12.0 to doubleArrayOf(21.0, 6.6, 8.0, 4.16), 16.0 to doubleArrayOf(28.0, 8.8, 10.0, 5.2)
    )
    // DIN 603 carriage bolts: d -> dk max, k max. The square neck (side v = d) has no sourced height in the app:
    // PartGeometry stands in an estimate and labels it, until the drawing gives f.
    private val DIN_603 = mapOf(
        5.0 to doubleArrayOf(13.55, 3.3), 6.0 to doubleArrayOf(16.55, 3.88), 8.0 to doubleArrayOf(20.65, 4.88),
        10.0 to doubleArrayOf(24.65, 5.38), 12.0 to doubleArrayOf(30.65, 6.95),
        16.0 to doubleArrayOf(38.8, 8.95), 20.0 to doubleArrayOf(46.8, 11.05)
    )
    // ISO 7089 plain washers, normal series: d -> d1 (bore), d2 (outside), h (thickness)
    private val ISO_7089 = mapOf(
        1.6 to doubleArrayOf(1.7, 4.0, 0.3), 2.0 to doubleArrayOf(2.2, 5.0, 0.3), 2.5 to doubleArrayOf(2.7, 6.0, 0.5),
        3.0 to doubleArrayOf(3.2, 7.0, 0.5), 4.0 to doubleArrayOf(4.3, 9.0, 0.8), 5.0 to doubleArrayOf(5.3, 10.0, 1.0),
        6.0 to doubleArrayOf(6.4, 12.0, 1.6), 8.0 to doubleArrayOf(8.4, 16.0, 1.6), 10.0 to doubleArrayOf(10.5, 20.0, 2.0),
        12.0 to doubleArrayOf(13.0, 24.0, 2.5), 14.0 to doubleArrayOf(15.0, 28.0, 2.5), 16.0 to doubleArrayOf(17.0, 30.0, 3.0),
        20.0 to doubleArrayOf(21.0, 37.0, 3.0), 24.0 to doubleArrayOf(25.0, 44.0, 4.0), 30.0 to doubleArrayOf(31.0, 56.0, 4.0),
        36.0 to doubleArrayOf(37.0, 66.0, 5.0)
    )
    // ASME B18.3 socket head cap screws (1960 series), inch: d -> head diameter A, hex socket J. Head height H = D.
    private val ASME_SHCS = mapOf(
        0.060 to doubleArrayOf(0.096, 0.050), 0.073 to doubleArrayOf(0.118, 0.0625), 0.086 to doubleArrayOf(0.140, 0.078125),
        0.099 to doubleArrayOf(0.161, 0.078125), 0.112 to doubleArrayOf(0.183, 0.09375), 0.125 to doubleArrayOf(0.205, 0.09375),
        0.138 to doubleArrayOf(0.226, 0.109375), 0.164 to doubleArrayOf(0.270, 0.140625), 0.190 to doubleArrayOf(0.312, 0.15625),
        0.25 to doubleArrayOf(0.375, 0.1875), 0.3125 to doubleArrayOf(0.469, 0.25), 0.375 to doubleArrayOf(0.562, 0.3125),
        0.4375 to doubleArrayOf(0.656, 0.375), 0.5 to doubleArrayOf(0.750, 0.375), 0.5625 to doubleArrayOf(0.844, 0.4375),
        0.625 to doubleArrayOf(0.938, 0.5), 0.75 to doubleArrayOf(1.125, 0.625), 0.875 to doubleArrayOf(1.312, 0.75),
        1.0 to doubleArrayOf(1.500, 0.75), 1.125 to doubleArrayOf(1.688, 0.875), 1.25 to doubleArrayOf(1.875, 0.875),
        1.375 to doubleArrayOf(2.062, 1.0), 1.5 to doubleArrayOf(2.250, 1.0), 1.75 to doubleArrayOf(2.625, 1.25)
    )

    /** Hex socket (key size, depth) in mm, or null when the part has no socket. */
    fun socketFor(type: FastenerType, unit: UnitSystem, dMm: Double): Pair<Double, Double>? {
        val metric = unit == UnitSystem.METRIC
        return when (type) {
            // ISO 4026: key = d / 2 from M3 up; the depth is a typical value, not a table one
            FastenerType.SOCKET_SET -> 0.5 * dMm to 0.5 * dMm
            FastenerType.SHCS, FastenerType.HEXALOBULAR_SCREW ->
                (if (metric) lookup(ISO_SHCS_SOCKET, dMm)?.let { it[0] to it[1] }
                else lookup(ASME_SHCS, dMm / 25.4)?.let { it[1] * 25.4 to 0.5 * dMm }) ?: (0.8 * dMm to 0.5 * dMm)
            FastenerType.SOCKET_CSK -> (if (metric) lookup(ISO_CSK, dMm)?.let { it[2] to it[3] } else null) ?: (0.6 * dMm to 0.36 * dMm)
            FastenerType.SOCKET_BUTTON -> (if (metric) lookup(ISO_BUTTON, dMm)?.let { it[2] to it[3] } else null) ?: (0.6 * dMm to 0.31 * dMm)
            else -> null
        }
    }

    /** Washer bore, outside diameter and thickness: ISO 7089 for metric plain washers, otherwise a flagged estimate. */
    fun washerFor(type: FastenerType, unit: UnitSystem, dMm: Double): WasherDimensions {
        if (type == FastenerType.FLAT_WASHER && unit == UnitSystem.METRIC) {
            lookup(ISO_7089, dMm)?.let { return WasherDimensions(it[0], it[1], it[2], "ISO 7089", true) }
        }
        if (type == FastenerType.FENDER_WASHER && unit == UnitSystem.METRIC)
            return WasherDimensions(1.08 * dMm, 3.0 * dMm, 0.25 * dMm, "Estimated (ISO 7093 large washer proportions)", false)
        if (type == FastenerType.STRUCT_WASHER)
            return WasherDimensions(1.08 * dMm, 2.0 * dMm, 0.22 * dMm, "Estimated (ASTM F436 hardened washer proportions)", false)
        if (type == FastenerType.TOOTH_LOCK_WASHER)
            return WasherDimensions(1.05 * dMm, 2.0 * dMm, 0.10 * dMm, "Estimated (DIN 6797 / 6798 serrated washer; teeth not modelled)", false)
        if (type == FastenerType.BELLEVILLE_WASHER)
            return WasherDimensions(1.05 * dMm, 2.0 * dMm, 0.20 * dMm, "Estimated (DIN 6796 conical washer, flattened thickness)", false)
        if (type == FastenerType.WAVE_WASHER)
            return WasherDimensions(1.05 * dMm, 1.7 * dMm, 0.12 * dMm, "Estimated (DIN 137 wave washer, flattened thickness)", false)
        if (type == FastenerType.TAPER_WASHER)
            return WasherDimensions(1.10 * dMm, 2.2 * dMm, 0.45 * dMm, "Estimated (DIN 434 / 435 taper washer, mean thickness)", false)
        if (type == FastenerType.RETAINING_RING)
            return WasherDimensions(0.92 * dMm, 1.5 * dMm, 0.10 * dMm, "Estimated (DIN 471 / 472 circlip, lugs not modelled)", false)
        return if (type == FastenerType.SPRING_WASHER) WasherDimensions(1.02 * dMm, 1.75 * dMm, 0.22 * dMm, "Estimated (no spring washer table in the app)", false)
        else WasherDimensions(1.06 * dMm, 2.0 * dMm, 0.18 * dMm, "Estimated (no washer table for this size / standard)", false)
    }

    private fun <V> lookup(map: Map<Double, V>, key: Double): V? =
        map.entries.firstOrNull { abs(it.key - key) < 0.002 }?.value

    /** Types the app has no dimension table for: they reuse the nearest standard geometry and are labelled as approximations. */
    private val APPROXIMATED = setOf(
        FastenerType.TTY_HEAD_BOLT, FastenerType.THREAD_FORMING_SCREW, FastenerType.WHEEL_BOLT, FastenerType.WHEEL_NUT,
        FastenerType.BANJO_BOLT, FastenerType.NAS_CLOSE_TOL_BOLT, FastenerType.HI_LOK_PIN, FastenerType.HI_LOK_COLLAR,
        FastenerType.TAPER_LOK, FastenerType.LOCKBOLT_PIN, FastenerType.BLIND_BOLT, FastenerType.AERO_LOCK_NUT,
        FastenerType.NUTPLATE, FastenerType.SOLID_RIVET, FastenerType.BLIND_RIVET,
        // These were silently given plain hex bolt / hex nut dimensions; their own standards differ
        FastenerType.TWELVE_POINT, FastenerType.T_BOLT, FastenerType.EYE_BOLT, FastenerType.HEAVY_HEX_BOLT,
        FastenerType.HEAVY_HEX_NUT, FastenerType.FLANGE_NUT, FastenerType.NYLOC_NUT, FastenerType.METAL_LOCK_NUT,
        FastenerType.CASTLE_NUT, FastenerType.ACORN_NUT, FastenerType.WELD_NUT,
        // Added catalogue types: proportional head geometry only, no product table in the app yet
        FastenerType.SQUARE_BOLT, FastenerType.SHOULDER_BOLT, FastenerType.SET_SCREW_CONE, FastenerType.SET_SCREW_DOG,
        FastenerType.SET_SCREW_CUP, FastenerType.SLOTTED_SET_SCREW, FastenerType.THUMB_SCREW, FastenerType.WING_SCREW,
        FastenerType.HANGER_BOLT, FastenerType.ANCHOR_BOLT, FastenerType.LAG_SCREW,
        FastenerType.PAN_SLOT, FastenerType.PAN_CROSS, FastenerType.CHEESE_SLOT, FastenerType.CSK_SLOT,
        FastenerType.CSK_CROSS, FastenerType.RAISED_CSK_CROSS, FastenerType.HEX_WASHER_SCREW, FastenerType.SEMS_SCREW,
        FastenerType.TAPPING_PAN, FastenerType.TAPPING_CSK, FastenerType.TAPPING_HEX, FastenerType.SELF_DRILLING,
        FastenerType.DRYWALL_SCREW, FastenerType.CHIPBOARD_SCREW, FastenerType.CONROD_BOLT,
        FastenerType.CLEVIS_BOLT, FastenerType.SHEAR_BOLT, FastenerType.THREADED_ROD, FastenerType.TAP_END_STUD,
        FastenerType.DOWEL_PIN, FastenerType.TAPER_PIN, FastenerType.CLEVIS_PIN, FastenerType.SPLIT_PIN,
        FastenerType.SPRING_PIN, FastenerType.GROOVED_PIN, FastenerType.RETAINING_RING,
        FastenerType.THIN_NUT, FastenerType.HIGH_NUT, FastenerType.SQUARE_NUT, FastenerType.COUPLING_NUT,
        FastenerType.T_SLOT_NUT, FastenerType.CAGE_NUT, FastenerType.WING_NUT, FastenerType.THUMB_NUT,
        FastenerType.RIVET_NUT, FastenerType.CLINCH_NUT,
        FastenerType.FENDER_WASHER, FastenerType.STRUCT_WASHER, FastenerType.TOOTH_LOCK_WASHER,
        FastenerType.BELLEVILLE_WASHER, FastenerType.WAVE_WASHER, FastenerType.TAPER_WASHER
    )

    fun forPart(type: FastenerType, unit: UnitSystem, dMm: Double): HeadDimensions {
        val dims = tableFor(type, unit, dMm)
        if (type !in APPROXIMATED) return dims
        val std = PokaYokeEngine.defaultStandard(type, unit).code
        return dims.copy(source = "Approximated from ${dims.source} — no $std table in the app", isTable = false)
    }

    private fun estimated(dk: Double, k: Double, dMm: Double, why: String, neck: Double? = null) =
        HeadDimensions(dk, k, 0.92 * dk, 0.84 * dMm, "Estimated ($why)", false, neck)

    private fun tableFor(type: FastenerType, unit: UnitSystem, dMm: Double): HeadDimensions {
        val metric = unit == UnitSystem.METRIC
        val nutM = lookup(ISO_HEX, dMm)?.get(2) ?: 0.84 * dMm
        when (type) {
            FastenerType.SHCS, FastenerType.HEXALOBULAR_SCREW -> {
                if (metric) lookup(ISO_SHCS_DK, dMm)?.let { dk ->
                    val src = if (type == FastenerType.HEXALOBULAR_SCREW) "ISO 14579 (head per ISO 4762)" else "ISO 4762"
                    return HeadDimensions(dk, dMm, 0.92 * dk, nutM, src, true)
                } else lookup(ASME_SHCS, dMm / 25.4)?.let { a ->
                    return HeadDimensions(a[0] * 25.4, dMm, 0.92 * a[0] * 25.4, nutM, "ASME B18.3 (1960 series, H = D)", true)
                }
                return estimated(1.5 * dMm, dMm, dMm, "outside the socket cap screw table")
            }
            FastenerType.SOCKET_CSK -> {
                if (metric) lookup(ISO_CSK, dMm)?.let { return HeadDimensions(it[0], it[1], it[0], nutM, "ISO 10642", true) }
                return estimated(2.0 * dMm, 0.5 * dMm, dMm, if (metric) "outside the ISO 10642 table" else "no inch countersunk table in the app")
            }
            FastenerType.SOCKET_BUTTON -> {
                if (metric) lookup(ISO_BUTTON, dMm)?.let { return HeadDimensions(it[0], it[1], 0.92 * it[0], nutM, "ISO 7380-1", true) }
                return estimated(1.75 * dMm, 0.55 * dMm, dMm, if (metric) "outside the ISO 7380 table" else "no inch button head table in the app")
            }
            FastenerType.CARRIAGE_BOLT -> {
                if (metric) lookup(DIN_603, dMm)?.let { return HeadDimensions(it[0], it[1], 0.92 * it[0], nutM, "DIN 603 (head only)", true) }
                return estimated(2.4 * dMm, 0.55 * dMm, dMm, if (metric) "outside the DIN 603 table" else "no inch carriage bolt table in the app")
            }
            FastenerType.FLANGE_BOLT -> {
                if (metric) BoltForging.din6921(dMm)?.let { g ->
                    val sFlats = g.s
                    // k is the whole head height, flange included
                    if (sFlats != null && g.k != null && g.dc != null)
                        return HeadDimensions(sFlats, g.k, 0.95 * g.dc, nutM, "DIN 6921 (1983 edition, supplier tables)", true)
                }
            }
            // Machine-screw head families: ISO 1207 / 7045 / 7046 / 7047 head proportions (dk, k as a ratio of d)
            FastenerType.PAN_SLOT, FastenerType.PAN_CROSS, FastenerType.TAPPING_PAN, FastenerType.SELF_DRILLING ->
                return estimated(2.0 * dMm, 0.60 * dMm, dMm, "pan head proportions, ISO 7045 / ISO 1481")
            FastenerType.CHEESE_SLOT ->
                return estimated(1.6 * dMm, 0.60 * dMm, dMm, "cheese head proportions, ISO 1207")
            FastenerType.CSK_SLOT, FastenerType.CSK_CROSS, FastenerType.TAPPING_CSK, FastenerType.CHIPBOARD_SCREW ->
                return estimated(1.9 * dMm, 0.55 * dMm, dMm, "90° countersunk head proportions, ISO 7046 / ISO 7050")
            FastenerType.RAISED_CSK_CROSS ->
                return estimated(1.9 * dMm, 0.75 * dMm, dMm, "raised countersunk head proportions, ISO 7047")
            FastenerType.DRYWALL_SCREW ->
                return estimated(2.2 * dMm, 0.50 * dMm, dMm, "bugle head proportions, DIN 18182")
            FastenerType.HEX_WASHER_SCREW, FastenerType.SEMS_SCREW ->
                return estimated(2.2 * dMm, 0.80 * dMm, dMm, "hex washer / SEMS head proportions, DIN 6900")
            FastenerType.SHOULDER_BOLT ->
                return estimated(1.5 * dMm, 0.70 * dMm, dMm, "shoulder screw head proportions, ISO 7379")
            FastenerType.THUMB_SCREW, FastenerType.WING_SCREW ->
                return estimated(2.5 * dMm, 0.90 * dMm, dMm, "knurled / wing head proportions, DIN 464 / DIN 316")
            FastenerType.CLEVIS_BOLT, FastenerType.CLEVIS_PIN ->
                return estimated(1.5 * dMm, 0.35 * dMm, dMm, "headed pin proportions, AN21 / ISO 2341")
            // Set screws, studs, rod, anchor and dowel screws and plain pins have no head at all
            FastenerType.SET_SCREW_CONE, FastenerType.SET_SCREW_DOG, FastenerType.SET_SCREW_CUP,
            FastenerType.SLOTTED_SET_SCREW, FastenerType.HANGER_BOLT, FastenerType.ANCHOR_BOLT,
            FastenerType.THREADED_ROD, FastenerType.TAP_END_STUD, FastenerType.DOWEL_PIN, FastenerType.TAPER_PIN,
            FastenerType.SPLIT_PIN, FastenerType.SPRING_PIN, FastenerType.GROOVED_PIN, FastenerType.RETAINING_RING ->
                return estimated(dMm, 0.0, dMm, "headless part — no head volume")
            // Nut heights that are not the ISO 4032 m: thin (ISO 4035), high (ISO 4033) and coupling nuts
            FastenerType.THIN_NUT -> lookup(ISO_HEX, dMm)?.let {
                return HeadDimensions(it[0], 0.55 * dMm, it[3], 0.55 * dMm, "ISO 4035 thin nut (m ≈ 0.55 d)", false)
            }
            FastenerType.HIGH_NUT -> lookup(ISO_HEX, dMm)?.let {
                return HeadDimensions(it[0], 0.90 * dMm, it[3], 0.90 * dMm, "ISO 4033 style 2 nut (m ≈ 0.90 d)", false)
            }
            FastenerType.COUPLING_NUT -> lookup(ISO_HEX, dMm)?.let {
                return HeadDimensions(it[0], 3.0 * dMm, it[3], 3.0 * dMm, "DIN 6334 coupling nut (m = 3 d)", false)
            }
            // Universal head, MS20470 proportions: A = 2 D, H = 0.425 D
            FastenerType.SOLID_RIVET, FastenerType.BLIND_RIVET ->
                return HeadDimensions(2.0 * dMm, 0.425 * dMm, 2.0 * dMm, 0.84 * dMm, "MS20470 universal head proportions (A = 2 D, H = 0.425 D)", false)
            else -> {}
        }
        if (!metric) {
            lookup(ASME_HEX, dMm / 25.4)?.let { a ->
                val f = if (type.isNut) a[2] else a[0]
                return HeadDimensions(f * 25.4, a[1] * 25.4, 0.95 * f * 25.4, a[3] * 25.4, "ASME B18.2.1 / B18.2.2", true)
            }
        } else {
            lookup(ISO_HEX, dMm)?.let { h -> return HeadDimensions(h[0], h[1], h[3], h[2], "ISO 4014 / ISO 4032", true) }
        }
        // Outside the tables (micro sizes, custom scanned diameters): proportional estimate, flagged as such
        return HeadDimensions(1.6 * dMm, 0.65 * dMm, 1.45 * dMm, 0.84 * dMm, "Estimated (outside standard table)", false)
    }
}
