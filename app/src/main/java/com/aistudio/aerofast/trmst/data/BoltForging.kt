package com.aistudio.aerofast.trmst.data

import kotlin.math.PI
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

    data class FlangeGeom(val dc: Double?, val k: Double?, val s: Double?, val c: Double?) {
        val complete get() = dc != null && k != null && s != null && c != null && c > 0 && k > c
    }

    /**
     * DIN 6921 (supplier tables): d → dc max, k max, s, c min (null = no source found).
     * c min: M5 1.0, M6 1.1, M8 1.2, M12 1.8 from supplier tables; M10 1.5 and M16 2.4 from the Böllhoff DIN 6921
     * table (unlabelled column, matching the DIN 6923 flange nut c min). M20 c not confirmed.
     */
    private data class Din6921Row(val dc: Double, val k: Double, val s: Double?, val c: Double?)
    private val DIN_6921 = mapOf(
        5.0 to Din6921Row(11.8, 5.4, 8.0, 1.0), 6.0 to Din6921Row(14.2, 6.6, 10.0, 1.1), 8.0 to Din6921Row(18.0, 8.1, 13.0, 1.2),
        10.0 to Din6921Row(22.3, 9.2, 15.0, 1.5), 12.0 to Din6921Row(26.6, 11.5, 16.0, 1.8), 16.0 to Din6921Row(35.0, 14.4, 21.0, 2.4),
        20.0 to Din6921Row(43.0, 17.1, null, null)
    )

    fun din6921(dMm: Double): FlangeGeom? =
        DIN_6921.entries.firstOrNull { kotlin.math.abs(it.key - dMm) < 0.01 }?.value?.let { FlangeGeom(it.dc, it.k, it.s, it.c) }

    /** Drawing / typed values first, then DIN 6921 (flange thickness c is the table minimum). */
    fun resolveFlange(dMm: Double, dc: Double?, k: Double?, s: Double?, c: Double?): FlangeGeom {
        val din = din6921(dMm)
        return FlangeGeom(dc ?: din?.dc, k ?: din?.k, s ?: din?.s, c ?: din?.c)
    }

    fun compute(
        type: FastenerType, unit: UnitSystem, dMm: Double, pitchMm: Double, lengthMm: Double,
        blankDiaMm: Double, wireDiaMm: Double, tableHead: HeadDimensions?, densityGPerCm3: Double,
        threadLengthMm: Double?, shankDiaMm: Double?, acrossFlatsMm: Double?, headHeightMm: Double?,
        flangeDiaMm: Double?, flangeThicknessMm: Double?
    ): BoltForgingResult {
        val missing = mutableListOf<String>()
        val b = minOf(threadLengthMm ?: PokaYokeEngine.threadLengthB(type, unit, dMm, lengthMm), lengthMm)
        val ds = shankDiaMm ?: wireDiaMm
        val db = blankDiaMm
        val aShank = PI / 4 * ds * ds
        val aBlank = PI / 4 * db * db
        // 15°/side cone; on short bolts it is squeezed into whatever plain length is left under the head
        val cone = if (ds > db) minOf((ds - db) / 2 / tan(Math.toRadians(15.0)), maxOf(0.0, lengthMm - b)) else 0.0
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
                // The flange is coined to size in the die cavity and the hex closed in the punch: a flange bolt
                // is formed, not trimmed. Shearing the flange OD would destroy its bearing face and underhead fillet.
                cheese = 0.0
                headFinished = PI / 4 * flangeDiaMm * flangeDiaMm * flangeThicknessMm + sqrt(3.0) / 2 * s * s * (k - flangeThicknessMm)
                headForged = headFinished
                note = "Flange Ø$flangeDiaMm × c $flangeThicknessMm formed in the die cavity, hex s $s closed in the punch; no trim"
            } else {
                note = "Needs dc and c from the drawing"
            }
        } else if (s != null && k != null) {
            // Shop rule: the round cheese that is trimmed to a hex is 1.20 x the minimum across flats.
            // That is 1.04 x the across-corners e (e = s / cos 30), i.e. just enough stock to fill the corners
            // and leave trim material, which is what a hex trim die is set to.
            val dForged = 1.2 * s
            headForged = PI / 4 * dForged * dForged * k
            headFinished = sqrt(3.0) / 2 * s * s * k
            cheese = headForged - headFinished
            note = "Round head forged Ø${"%.2f".format(java.util.Locale.US, dForged)} (1.20 × s) × k $k, trimmed to hex s $s"
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
