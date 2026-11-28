package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

enum class SolidShape { CYLINDER, CONE, HEX, SQUARE, TWELVE_POINT, DOME }

/** One section of the part as the AI read it off the drawing, listed from the head end to the point end. */
@Serializable
data class DrawnSolid(
    val feature: String,
    val shape: SolidShape,
    /** Diameter; across flats of a hex / 12-point; side of a square; head-side diameter of a cone; base diameter of a dome. */
    val d1Mm: Double,
    /** Point-side diameter of a cone. */
    val d2Mm: Double,
    val lengthMm: Double,
    val threaded: Boolean
)

enum class CutoutFrom { HEAD_END, POINT_END, THROUGH, CROSS }

/** Material removed from the solids: a drive socket, a bore, a drilled or cross hole. */
@Serializable
data class DrawnCutout(
    val feature: String,
    val shape: SolidShape,
    val d1Mm: Double,
    val d2Mm: Double,
    val lengthMm: Double,
    val count: Int,
    val from: CutoutFrom,
    val threaded: Boolean
)

enum class VolumeBasis(val label: String) {
    SOLIDS("Solids read from the drawing"),
    DIMENSIONS("Main dimensions of the drawing on the standard shape of this fastener type")
}

/** Total volume of the scanned part, what it is built from, and how much of it really came from the drawing. */
@Serializable
data class DrawingVolume(
    val basis: VolumeBasis,
    /** Labelled features; the volumes add up to [totalMm3]. */
    val items: List<VolumeItem>,
    val totalMm3: Double,
    val densityGPerCm3: Double,
    val fromDrawing: List<String>,
    /** Dimensions the drawing did not give, with the value used instead. */
    val notOnDrawing: List<String>,
    val cautions: List<String>,
    /** The same part on the standard shape of its type, as a cross-check. */
    val modelVolumeMm3: Double,
    /** Extra slug material the header removes again: trim cheese of a hex head, the pierced web of a nut. */
    val scrapMm3: Double,
    val wireDiaMm: Double,
    /** The finished shape, drawable with the die-progression renderer. */
    val outline: HeadingStation?,
    /**
     * Head volume on its own: the material above the die face (the whole part for a nut or a washer).
     * On the solids basis the head share of the standard shape is applied to the scanned total, so head +
     * shank always add up to [totalMm3]; [headIsScaled] says which of the two it is.
     */
    val headMm3: Double = 0.0,
    val headIsScaled: Boolean = false
) {
    /** Shank, thread section and neck: head + shank = [totalMm3]. */
    val shankMm3: Double get() = totalMm3 - headMm3
    val headMassG: Double get() = headMm3 / 1000.0 * densityGPerCm3
    val massG: Double get() = totalMm3 / 1000.0 * densityGPerCm3
    /** Slug length on the wire that holds this volume plus the scrap. */
    val cutOffLengthMm: Double get() = (totalMm3 + scrapMm3) / (PI / 4.0 * wireDiaMm * wireDiaMm)
    /** True when every dimension in the volume is from the drawing and nothing of the shape is left out. */
    val isComplete: Boolean get() = notOnDrawing.isEmpty() && cautions.isEmpty()
}
