package com.aistudio.aerofast.trmst.data

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sqrt

/** One moment of the heading animation. Station indices refer to plan.stations (0 = cut-off slug). */
data class AnimFrame(
    val fromIdx: Int,
    val toIdx: Int,
    /** 0 = shape of the station before the blow, 1 = shape after it. */
    val morph: Double,
    /** Punch height above the part top in mm (0 = in contact). */
    val punchGapMm: Double,
    val blowNumber: Int,
    val blowCount: Int
)

/**
 * Timeline and shapes for the cold-heading animation. Every key shape is a station the app calculated; between
 * two stations each layer of metal is moved by its volume fraction from the bottom (layers stay in order), so the
 * in-between frames are an interpolation of the real shapes, not a flow simulation.
 */
object HeadingAnimation {
    const val SEC_PER_BLOW = 2.4
    const val HOLD_START_SEC = 1.0
    const val HOLD_END_SEC = 1.6
    private const val APPROACH = 0.30
    private const val FORM_END = 0.70

    fun blowCount(plan: HeadingPlan) = (plan.stations.size - 1).coerceAtLeast(0)
    fun durationSec(plan: HeadingPlan) = HOLD_START_SEC + blowCount(plan) * SEC_PER_BLOW + HOLD_END_SEC

    private fun smooth(x: Double) = x.coerceIn(0.0, 1.0).let { it * it * (3 - 2 * it) }

    fun frameAt(plan: HeadingPlan, tSec: Double): AnimFrame {
        val n = blowCount(plan)
        val lift = plan.stations.maxOf { it.heightMm } * 0.45
        if (n == 0) return AnimFrame(0, 0, 0.0, lift, 0, 0)
        val t = tSec - HOLD_START_SEC
        if (t <= 0.0) return AnimFrame(0, 1, 0.0, lift, 1, n)
        val blow = (t / SEC_PER_BLOW).toInt()
        if (blow >= n) return AnimFrame(n - 1, n, 1.0, lift, n, n)
        val p = (t - blow * SEC_PER_BLOW) / SEC_PER_BLOW
        val (morph, gap) = when {
            p < APPROACH -> 0.0 to lift * (1 - smooth(p / APPROACH))
            p < FORM_END -> smooth((p - APPROACH) / (FORM_END - APPROACH)) to 0.0
            else -> 1.0 to lift * smooth((p - FORM_END) / (1 - FORM_END))
        }
        return AnimFrame(blow, blow + 1, morph, gap, blow + 1, n)
    }

    /** Drawn half width and true cross-section area of a segment at fraction s (0 bottom .. 1 top) of its height. */
    private fun at(seg: ProfileSegment, s: Double): Pair<Double, Double> = when (seg.shape) {
        SectionShape.ROUND -> {
            val d = seg.dBottomMm + (seg.dTopMm - seg.dBottomMm) * s
            d / 2 to PI / 4 * d * d
        }
        SectionShape.DOME -> {
            val r0 = seg.dBottomMm / 2.0
            val h = seg.heightMm
            val rs = (r0 * r0 + h * h) / (2 * h)
            val y = h * s
            val x = sqrt(max(0.0, rs * rs - (rs - h + y) * (rs - h + y)))
            x to PI * x * x
        }
        else -> seg.maxWidthMm / 2 to sectionArea(seg.shape, seg.dBottomMm)
    }

    /** Samples (y, half width, cumulative volume) up the station. */
    private fun samples(st: HeadingStation, perSeg: Int = 24): List<Triple<Double, Double, Double>> {
        val out = mutableListOf<Triple<Double, Double, Double>>()
        var y0 = 0.0
        var vol = 0.0
        for (seg in st.segments) {
            var prevA = at(seg, 0.0).second
            if (out.isEmpty()) out += Triple(0.0, at(seg, 0.0).first, 0.0)
            else out += Triple(y0, at(seg, 0.0).first, vol)
            for (k in 1..perSeg) {
                val s = k / perSeg.toDouble()
                val (w, a) = at(seg, s)
                vol += (prevA + a) / 2 * seg.heightMm / perSeg
                prevA = a
                out += Triple(y0 + seg.heightMm * s, w, vol)
            }
            y0 += seg.heightMm
        }
        return out
    }

    /** (y, half width) at each of [n] volume fractions of the station, bottom to top. */
    fun byVolume(st: HeadingStation, n: Int = 64): List<Pair<Double, Double>> {
        val pts = samples(st)
        val total = pts.last().third.takeIf { it > 0.0 } ?: return pts.map { it.first to it.second }
        return (0..n).map { k ->
            val target = total * k / n
            val j = pts.indexOfFirst { it.third >= target }.let { if (it <= 0) 1.coerceAtMost(pts.lastIndex) else it }
            val a = pts[j - 1]; val b = pts[j]
            val u = if (b.third - a.third > 1e-12) (target - a.third) / (b.third - a.third) else 0.0
            (a.first + (b.first - a.first) * u) to (a.second + (b.second - a.second) * u)
        }
    }

    /** The part outline at this frame: (y from the die bottom, half width) in mm. */
    fun outline(plan: HeadingPlan, f: AnimFrame, n: Int = 64): List<Pair<Double, Double>> {
        val a = byVolume(plan.stations[f.fromIdx], n)
        if (f.fromIdx == f.toIdx) return a
        val b = byVolume(plan.stations[f.toIdx], n)
        return a.indices.map { i -> (a[i].first + (b[i].first - a[i].first) * f.morph) to (a[i].second + (b[i].second - a[i].second) * f.morph) }
    }
}
