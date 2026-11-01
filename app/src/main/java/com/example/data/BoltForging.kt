package com.example.data

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.tan

/** Forging figures for a headed bolt; each "from drawing" flag says whether the drawing gave the value or the standard did. */
data class BoltForgingResult(
    val threadLengthMm: Double,
    val threadFromDrawing: Boolean,
    val shankDiaMm: Double,
    val shankFromDrawing: Boolean,
    val blankDiaMm: Double,
    val coneLengthMm: Double,
    val stockToExtrudeMm: Double,
    val lengthUnderHeadBeforeMm: Double,
    val headNote: String,
    val headFromDrawing: Boolean,
    val finishedVolumeMm3: Double?,
    val cheeseVolumeMm3: Double?,
    val headStockMm: Double?,
    val cutOffLengthMm: Double?,
    val densityGPerMm3: Double,
    val missing: List<String>
)

/**
 * Hex head: forged round at 1.2 × across corners, height k, trimmed to hex.
 * Hex flange: collar forged at dc + 2 × 2 mm, trimmed to dc; the hex is formed in the die.
 * Thread section: forward-extruded from the shank Ø to the rolling blank Ø through a 15°/side cone (volume constant).
 */
object BoltForging {
    val HEX_TYPES = setOf(FastenerType.HEX_BOLT, FastenerType.HEX_BOLT_FULL, FastenerType.HEAVY_HEX_BOLT)

    fun applies(type: FastenerType) = type in HEX_TYPES || type == FastenerType.FLANGE_BOLT

    fun compute(
        type: FastenerType, unit: UnitSystem, dMm: Double, pitchMm: Double, lengthMm: Double,
        blankDiaMm: Double, wireDiaMm: Double, tableHead: HeadDimensions?, densityGPerCm3: Double,
        threadLengthMm: Double?, shankDiaMm: Double?, acrossFlatsMm: Double?, headHeightMm: Double?,
        flangeDiaMm: Double?, flangeThicknessMm: Double?
    ): BoltForgingResult {
        val missing = mutableListOf<String>()
        val b = threadLengthMm ?: PokaYokeEngine.threadLengthB(type, unit, dMm, lengthMm)
        val ds = shankDiaMm ?: wireDiaMm
        val db = blankDiaMm
        val aShank = PI / 4 * ds * ds
        val aBlank = PI / 4 * db * db
        val cone = if (ds > db) (ds - db) / 2 / tan(Math.toRadians(15.0)) else 0.0
        val coneVol = PI * cone / 12 * (ds * ds + ds * db + db * db)
        val stock = (aBlank * b + coneVol) / aShank
        val underHeadBefore = lengthMm - b - cone + stock
        val bodyVol = aShank * (lengthMm - b - cone) + coneVol + aBlank * b

        var headFinished: Double? = null
        var cheese: Double? = null
        var headForged: Double? = null
        var note: String
        val s = acrossFlatsMm ?: tableHead?.acrossFlatsS
        val k = headHeightMm ?: tableHead?.headHeightK
        val headFromDrawing = acrossFlatsMm != null && headHeightMm != null
        if (type == FastenerType.FLANGE_BOLT) {
            if (flangeDiaMm == null) missing += "flange Ø dc"
            if (flangeThicknessMm == null) missing += "flange thickness c"
            if (s != null && k != null && flangeDiaMm != null && flangeThicknessMm != null && k > flangeThicknessMm) {
                val dCollar = flangeDiaMm + 4.0
                cheese = PI / 4 * (dCollar * dCollar - flangeDiaMm * flangeDiaMm) * flangeThicknessMm
                headFinished = PI / 4 * flangeDiaMm * flangeDiaMm * flangeThicknessMm + sqrt(3.0) / 2 * s * s * (k - flangeThicknessMm)
                headForged = headFinished + cheese
                note = "Collar forged Ø${"%.1f".format(java.util.Locale.US, dCollar)} × c ${flangeThicknessMm}, trimmed to Ø$flangeDiaMm"
            } else {
                note = "Needs dc and c from the drawing"
            }
        } else if (s != null && k != null) {
            val e = s / cos(Math.toRadians(30.0))
            val dForged = 1.2 * e
            headForged = PI / 4 * dForged * dForged * k
            headFinished = sqrt(3.0) / 2 * s * s * k
            cheese = headForged - headFinished
            note = "Round head forged Ø${"%.2f".format(java.util.Locale.US, dForged)} (1.2 × e) × k $k, trimmed to hex s $s"
        } else {
            note = "Needs across flats and head height"
        }
        if (acrossFlatsMm == null) missing += "across flats s"
        if (headHeightMm == null) missing += "head height k"
        if (threadLengthMm == null) missing += "thread length b"
        if (shankDiaMm == null) missing += "shank Ø"

        val headStock = headForged?.let { it / aShank }
        return BoltForgingResult(
            threadLengthMm = b, threadFromDrawing = threadLengthMm != null,
            shankDiaMm = ds, shankFromDrawing = shankDiaMm != null,
            blankDiaMm = db, coneLengthMm = cone, stockToExtrudeMm = stock, lengthUnderHeadBeforeMm = underHeadBefore,
            headNote = note, headFromDrawing = headFromDrawing,
            finishedVolumeMm3 = headFinished?.let { it + bodyVol },
            cheeseVolumeMm3 = cheese,
            headStockMm = headStock,
            cutOffLengthMm = headStock?.let { it + underHeadBefore },
            densityGPerMm3 = densityGPerCm3 / 1000.0,
            missing = missing
        )
    }
}
