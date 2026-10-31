package com.example.data

import kotlin.math.abs

/** Head / nut geometry in mm. isTable = false means the size is outside the standard table and was estimated. */
data class HeadDimensions(
    val acrossFlatsS: Double,   // hex s (or SHCS head diameter dk)
    val headHeightK: Double,
    val bearingDiaDw: Double,   // min bearing face diameter
    val nutHeightM: Double,
    val source: String,
    val isTable: Boolean
)

/** Nominal dimensions from ISO 4014/4017/4032 (hex), ISO 4762 (SHCS) and ASME B18.2.1/B18.2.2 (inch hex). */
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

    /** Hex socket (key size, depth) in mm, or null when the part has no socket. */
    fun socketFor(type: FastenerType, unit: UnitSystem, dMm: Double): Pair<Double, Double>? = when (type) {
        FastenerType.SOCKET_SET -> 0.5 * dMm to 0.5 * dMm
        FastenerType.SHCS, FastenerType.SOCKET_BUTTON, FastenerType.SOCKET_CSK, FastenerType.HEXALOBULAR_SCREW ->
            (if (unit == UnitSystem.METRIC) lookup(ISO_SHCS_SOCKET, dMm)?.let { it[0] to it[1] } else null)
                ?: (0.8 * dMm to 0.5 * dMm)
        else -> null
    }

    private fun <V> lookup(map: Map<Double, V>, key: Double): V? =
        map.entries.firstOrNull { abs(it.key - key) < 0.002 }?.value

    /** Types the app has no dimension table for: they reuse hex geometry and are labelled as approximations. */
    private val APPROXIMATED = setOf(
        FastenerType.TTY_HEAD_BOLT, FastenerType.THREAD_FORMING_SCREW, FastenerType.WHEEL_BOLT, FastenerType.WHEEL_NUT,
        FastenerType.BANJO_BOLT, FastenerType.NAS_CLOSE_TOL_BOLT, FastenerType.HI_LOK_PIN, FastenerType.HI_LOK_COLLAR,
        FastenerType.TAPER_LOK, FastenerType.LOCKBOLT_PIN, FastenerType.BLIND_BOLT, FastenerType.AERO_LOCK_NUT,
        FastenerType.NUTPLATE
    )

    fun forPart(type: FastenerType, unit: UnitSystem, dMm: Double): HeadDimensions {
        val dims = tableFor(type, unit, dMm)
        if (type !in APPROXIMATED) return dims
        val std = PokaYokeEngine.defaultStandard(type, unit).code
        return dims.copy(source = "Approximated from ${dims.source} hex geometry — no $std table in the app", isTable = false)
    }

    private fun tableFor(type: FastenerType, unit: UnitSystem, dMm: Double): HeadDimensions {
        if (unit == UnitSystem.INCH) {
            lookup(ASME_HEX, dMm / 25.4)?.let { a ->
                val f = if (type.isNut) a[2] else a[0]
                return HeadDimensions(f * 25.4, a[1] * 25.4, 0.95 * f * 25.4, a[3] * 25.4, "ASME B18.2.1 / B18.2.2", true)
            }
        } else if (type == FastenerType.SHCS || type == FastenerType.SOCKET_BUTTON || type == FastenerType.SOCKET_CSK ||
            type == FastenerType.HEXALOBULAR_SCREW) {
            lookup(ISO_SHCS_DK, dMm)?.let { dk ->
                val src = if (type == FastenerType.HEXALOBULAR_SCREW) "ISO 14579 (head per ISO 4762)" else "ISO 4762"
                return HeadDimensions(dk, dMm, 0.92 * dk, lookup(ISO_HEX, dMm)?.get(2) ?: 0.84 * dMm, src, true)
            }
        } else {
            lookup(ISO_HEX, dMm)?.let { h -> return HeadDimensions(h[0], h[1], h[3], h[2], "ISO 4014 / ISO 4032", true) }
        }
        // Outside the tables (micro sizes, custom scanned diameters): proportional estimate, flagged as such
        return HeadDimensions(1.6 * dMm, 0.65 * dMm, 1.45 * dMm, 0.84 * dMm, "Estimated (outside standard table)", false)
    }
}
