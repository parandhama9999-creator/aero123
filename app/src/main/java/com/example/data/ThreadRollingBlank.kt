package com.example.data

/**
 * Blank diameter correction for controlled-root threads (MJ per ISO 5855, UNJ per ASME B1.15).
 *
 * Rolling is a constant-volume operation: the metal driven out of the root is exactly the metal
 * that rises into the crest, and the blank diameter is whatever makes those two balance. The pitch
 * diameter is close to that balance point, which is why it works as a first approximation, but it
 * is not the reason the blank is the size it is.
 *
 * A J-form thread carries a mandatory large root radius, 0.15011P to 0.18042P, for fatigue life.
 * A blunter root is a shallower root, so less metal is displaced, while the crest to be filled is
 * unchanged. The blank therefore has to be larger than for the plain form at the same pitch
 * diameter, or the crest underfills and the major diameter comes up short.
 *
 * Only the difference between the two root forms is computed here. The plain-form blank keeps the
 * calibrated figure it has always had, because the empirical corrections in it -- springback, the
 * metal that escapes axially rather than into the crest, the slight underfill that is wanted to
 * avoid laps -- apply equally to both forms and cancel in the difference.
 */
object ThreadRollingBlank {

    private const val CREST_FLAT = 0.125        // P/8; the crest is truncated at H/8
    private const val FLANK_SPREAD = 1.1547     // 2 tan 30°, how fast the form widens with depth
    private const val D2_BELOW_MAJOR = 0.649519 // d − d2, per pitch

    /** Root depth below the major radius, per pitch. */
    const val ROOT_DEPTH_PLAIN = 0.61340        // d3 = d − 1.2268P (ISO 965-1, ASME B1.1 UNR)
    const val ROOT_DEPTH_J_SHALLOW = 0.57735    // R = 0.18042P, the J minor-diameter maximum
    const val ROOT_DEPTH_J_DEEP = 0.60766       // R = 0.15011P, the J minor-diameter minimum
    const val ROOT_DEPTH_J_MID = (ROOT_DEPTH_J_SHALLOW + ROOT_DEPTH_J_DEEP) / 2.0

    /**
     * Where the blank sits relative to the pitch diameter, per pitch, for a given root depth.
     *
     * Per pitch, material width at depth u below the major radius is w(u) = P/8 + 1.1547u.
     * Balancing what is added above the blank radius against what is removed below it gives
     * u_b = [(P − P/8)·h − 1.1547·h²/2] / P, and the blank is 2·u_b below the major diameter.
     */
    private fun blankOffsetFromD2(rootDepthPerPitch: Double): Double {
        val h = rootDepthPerPitch
        val ub = (1.0 - CREST_FLAT) * h - FLANK_SPREAD * h * h / 2.0
        return D2_BELOW_MAJOR - 2.0 * ub
    }

    /**
     * Diameter to add to a plain-form rolling blank to roll the J form instead.
     *
     * Defaults to the middle of the allowed root-radius band. The correct figure depends on the
     * radius actually ground on the die, and runs from [ROOT_DEPTH_J_DEEP] to
     * [ROOT_DEPTH_J_SHALLOW] — about 0.002P to 0.0135P — so a shop with a known die should check
     * the blank against it rather than take the middle on trust.
     */
    fun controlledRootDelta(pitchMm: Double, rootDepthPerPitch: Double = ROOT_DEPTH_J_MID): Double =
        (blankOffsetFromD2(rootDepthPerPitch) - blankOffsetFromD2(ROOT_DEPTH_PLAIN)) * pitchMm

    /** The band the correction spans across the allowed root radius, as diameters in mm. */
    fun controlledRootDeltaRange(pitchMm: Double): ClosedFloatingPointRange<Double> =
        controlledRootDelta(pitchMm, ROOT_DEPTH_J_DEEP)..controlledRootDelta(pitchMm, ROOT_DEPTH_J_SHALLOW)
}
