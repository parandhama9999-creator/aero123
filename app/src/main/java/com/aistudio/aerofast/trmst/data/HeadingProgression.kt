package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class SectionShape { ROUND, HEX, SQUARE, TWELVE_POINT, DOME, HEXALOBULAR }

private val HEX_AREA_FACTOR = sqrt(3.0) / 2.0

/** Two hexagons 30° apart (bi-hex): 2 × hexagon − their common dodecagon = (√3 − 3·tan 15°)·s². */
private val TWELVE_POINT_AREA_FACTOR = sqrt(3.0) - 3.0 * kotlin.math.tan(Math.toRadians(15.0))

/** Cross-section area; size is the diameter of a round or dome, across flats of a hex / 12-point, the side of a square. */
internal fun sectionArea(shape: SectionShape, size: Double) = when (shape) {
    SectionShape.HEX -> HEX_AREA_FACTOR * size * size
    SectionShape.TWELVE_POINT -> TWELVE_POINT_AREA_FACTOR * size * size
    SectionShape.SQUARE -> size * size
    // Hexalobular recess, size = point-to-point A; inner lobes B ≈ 0.713 A (ISO 10664): a circle of the mean of A and B
    SectionShape.HEXALOBULAR -> PI / 4.0 * (0.8565 * size) * (0.8565 * size)
    else -> PI / 4.0 * size * size
}

/**
 * One axial slice of the part, listed from the die end (bottom) to the punch end (top).
 * ROUND is a cylinder or cone; HEX / TWELVE_POINT are prisms sized across flats; SQUARE is a prism sized by its side;
 * DOME is a spherical cap on a base of diameter dBottomMm.
 */
@Serializable
data class ProfileSegment(
    val heightMm: Double,
    val dBottomMm: Double,
    val dTopMm: Double = dBottomMm,
    val shape: SectionShape = SectionShape.ROUND
) {
    val volumeMm3: Double
        get() = when (shape) {
            SectionShape.ROUND -> PI * heightMm / 12.0 * (dBottomMm * dBottomMm + dBottomMm * dTopMm + dTopMm * dTopMm)
            SectionShape.DOME -> PI * heightMm / 6.0 * (3.0 * (dBottomMm / 2.0) * (dBottomMm / 2.0) + heightMm * heightMm)
            else -> sectionArea(shape, dBottomMm) * heightMm
        }

    /** Width seen in side view; a hex or 12-point is drawn across corners. */
    val maxWidthMm: Double
        get() = when (shape) {
            SectionShape.HEX, SectionShape.TWELVE_POINT -> dBottomMm * 2.0 / sqrt(3.0)
            SectionShape.ROUND -> max(dBottomMm, dTopMm)
            else -> dBottomMm
        }

    /** Largest cross-section, which the forming load acts on. */
    val crossSectionMm2: Double
        get() = when (shape) {
            SectionShape.ROUND -> sectionArea(shape, max(dBottomMm, dTopMm))
            else -> sectionArea(shape, dBottomMm)
        }
}

/** Blind cavity (indent or socket) entering from the top or bottom face. */
@Serializable
data class Recess(val fromTop: Boolean, val sizeMm: Double, val depthMm: Double, val shape: SectionShape) {
    val volumeMm3: Double get() = sectionArea(shape, sizeMm) * depthMm
}

@Serializable
data class HeadingStation(
    /** 0 = shear cut-off, 1..n = die stations. */
    val index: Int,
    val title: String,
    val operation: String,
    val segments: List<ProfileSegment>,
    val recesses: List<Recess> = emptyList(),
    val throughHoleMm: Double? = null,
    /** Length of the part held inside the die bore, measured from the bottom. */
    val inDieMm: Double = 0.0,
    /** Material removed at this station (e.g. the pierced nut web). */
    val scrapMm3: Double = 0.0,
    /** The blow that needs the full forming load. */
    val isPeakBlow: Boolean = false
) {
    val isCutOff: Boolean get() = index == 0
    val heightMm: Double get() = segments.sumOf { it.heightMm }

    /** Diameter of the part where it sits in the die; segments run die end first. */
    val inDieDiaMm: Double? get() = if (isCutOff) null else segments.firstOrNull()?.maxWidthMm

    /** Bore this station's die is cut to: the stock it holds, plus the transfer clearance. */
    val dieBoreMm: Double?
        get() = inDieDiaMm?.let { it + HeadingProgression.stageClearance(it) }
    val maxWidthMm: Double get() = segments.maxOf { it.maxWidthMm }
    val volumeMm3: Double
        get() = segments.sumOf { it.volumeMm3 } - recesses.sumOf { it.volumeMm3 } -
            (throughHoleMm?.let { sectionArea(SectionShape.ROUND, it) * heightMm } ?: 0.0)
}

@Serializable
data class OffHeaderOp(val title: String, val operation: String)

/** The machine the part is headed on. The two bolt routes give different station sequences, not just a different name. */
enum class HeaderRoute(val label: String) {
    /** Blanks transfer from die to die: extrusion, pre-upsets, socket back-extrusion and trimming are all in-line. */
    MULTI_STATION("Multi-station progressive former"),
    NUT_FORMER("Nut former"),
    PRESS("Progressive press"),
    /** One die, two punches: cone / extrusion blow, then the finishing blow. Hex heads are formed, not trimmed. */
    TWO_BLOW("1-die 2-blow header")
}

/** The machine asked for on screen. AUTO picks the 1-die 2-blow header only where it is enough for the part. */
enum class HeaderChoice(val label: String) {
    AUTO("Auto"),
    TWO_BLOW("1-die 2-blow"),
    MULTI_STATION("Multi-station")
}

@Serializable
data class HeadingPlan(
    val slugDiaMm: Double,
    val slugLengthMm: Double,
    val slugVolumeMm3: Double,
    val stations: List<HeadingStation>,
    val offHeaderOps: List<OffHeaderOp>,
    /** Upsetting blows the head stock needs (1 ≤ L/D 2.3, 2 ≤ 4.5, else 3). */
    val upsetBlows: Int,
    val notes: List<String>,
    val route: HeaderRoute = HeaderRoute.MULTI_STATION,
    /** Why this route was chosen, or why a route that was asked for cannot make the part. */
    val routeReason: String = "",
    /** Set when the chosen machine cannot make the part as designed: the reason, for the poka-yoke gate. */
    val feasibilityIssue: String? = null
) {
    val dieStations: Int get() = stations.count { !it.isCutOff }

    /** "1-die 2-blow header", "4-die progressive former". */
    val machineDescription: String get() = when (route) {
        HeaderRoute.MULTI_STATION -> "$dieStations-die progressive former"
        HeaderRoute.NUT_FORMER -> "$dieStations-die nut former"
        HeaderRoute.PRESS -> "progressive press"
        HeaderRoute.TWO_BLOW -> "1-die 2-blow header"
    }

    /** Largest relative gap between the slug volume and (station volume + scrap removed so far). */
    val maxVolumeError: Double
        get() {
            var scrap = 0.0
            return stations.maxOfOrNull { s ->
                scrap += s.scrapMm3
                abs(s.volumeMm3 + scrap - slugVolumeMm3) / slugVolumeMm3
            } ?: 0.0
        }
}
