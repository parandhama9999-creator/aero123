package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min

/** Dimensions read from a drawing, in mm. null = not on the drawing: the standard value is used and reported. */
@Serializable
data class DrawingGeometry(
    val threadLengthMm: Double? = null,
    val shankDiaMm: Double? = null,
    /** Across flats of a hex or 12-point head, or of a nut. */
    val acrossFlatsMm: Double? = null,
    /** Diameter of a round head: socket cap, countersunk, button, carriage, pin or rivet. */
    val headDiaMm: Double? = null,
    val headHeightMm: Double? = null,
    val flangeDiaMm: Double? = null,
    val flangeThicknessMm: Double? = null,
    val socketSizeMm: Double? = null,
    val socketDepthMm: Double? = null,
    /** Square neck height of a carriage bolt. */
    val neckHeightMm: Double? = null,
    val nutHeightMm: Double? = null,
    val washerOuterDiaMm: Double? = null,
    val washerInnerDiaMm: Double? = null,
    val washerThicknessMm: Double? = null,
    /** Pan head crown radius rf and the height of its cylindrical edge below the dome. */
    val headDomeRadiusMm: Double? = null,
    val headEdgeHeightMm: Double? = null
)

/** Where a volume figure's dimensions came from, weakest source first when several are mixed. */
enum class DimSource(val label: String) {
    DRAWING("drawing"), CALCULATED("calculated"), STANDARD("standard"), ESTIMATED("estimated")
}

/** One feature of the finished part. A negative volume is material removed (socket, bore). */
@Serializable
data class VolumeItem(val feature: String, val detail: String, val volumeMm3: Double, val source: DimSource)

/** How the head (or the whole part) is formed, which decides both its shape and its die progression. */
enum class HeadStyle {
    HEX_TRIMMED, HEX_FORMED, SOCKET_CAP, SOCKET_COUNTERSUNK, SOCKET_BUTTON, TWELVE_POINT, CARRIAGE, ROUND_PIN, RIVET,
    HEADLESS, NUT, WASHER
}

/**
 * The finished part as a stack of solids from the die end (bottom) to the punch end (top), with its cavities.
 * Its volume is the part volume: weights, the cut-off slug and the last die station all come from this one shape.
 */
@Serializable
data class FinishedPart(
    val style: HeadStyle,
    val segments: List<ProfileSegment>,
    val recesses: List<Recess>,
    val throughHoleMm: Double?,
    /** Top segments formed outside the die bore: neck, flange, head (all of a nut or washer). */
    val headSegmentCount: Int,
    val threadLengthMm: Double,
    val blankDiaMm: Double,
    /** Labelled breakdown; the volumes add up to [volumeMm3]. */
    val items: List<VolumeItem>,
    val fromDrawing: List<String>,
    /** Dimensions the drawing did not give, with the value used instead and where it came from. */
    val notOnDrawing: List<String>,
    /** Features of this fastener type that the shape does not include. */
    val cautions: List<String>
) {
    val heightMm: Double get() = segments.sumOf { it.heightMm }
    val volumeMm3: Double
        get() = segments.sumOf { it.volumeMm3 } - recesses.sumOf { it.volumeMm3 } -
            (throughHoleMm?.let { PI / 4.0 * it * it * heightMm } ?: 0.0)
    val dieSegments: List<ProfileSegment> get() = segments.dropLast(headSegmentCount)
    val headSegments: List<ProfileSegment> get() = segments.takeLast(headSegmentCount)
    /** Material formed outside the die bore, net of the socket it displaces: the head stock to gather. */
    val headVolumeMm3: Double
        get() = if (headSegmentCount == 0) 0.0   // a set screw, stud or pin has no head: nothing is gathered
        else maxOf(0.0, volumeMm3 - dieSegments.sumOf { it.volumeMm3 })
    /** Everything held in the die bore: shank, thread section and any neck. Head + shank = [volumeMm3]. */
    val shankVolumeMm3: Double get() = volumeMm3 - headVolumeMm3
    /** True when every dimension in the volume is from the drawing and the type's shape is fully modelled. */
    val isComplete: Boolean get() = notOnDrawing.isEmpty() && cautions.isEmpty()

    /** The finished shape as a drawable station (no die). */
    fun asStation(): HeadingStation = HeadingStation(
        index = 1, title = "Finished part", operation = "", segments = segments, recesses = recesses, throughHoleMm = throughHoleMm
    )
}
