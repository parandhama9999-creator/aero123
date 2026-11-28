package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

/** What the drawing scanner reads off a customer drawing (platform-free, shared with the desktop app). */
@Serializable
data class DetectedDimension(
    val feature: String,
    val nominal: String,
    val tolerance: String
)

@Serializable
data class ExtractedFastenerSpec(
    val fastenerType: FastenerType,
    val unitSystem: UnitSystem,
    val nominalSizeIndex: Int,
    val nominalDiaMm: Double,
    val pitchValue: Double,
    val lengthMm: Double,
    val propertyClass: PropertyClass,
    val materialKey: String,
    val threadClass: String,
    val coating: CoatingType,
    val technicalSummary: String,
    val detectedDimensions: List<DetectedDimension>,
    val isAiVisionExtracted: Boolean,
    val modelUsed: String = "gemini-3.5-flash",
    val radius: Double? = null,
    // Drawing geometry for volume / pre-extrusion length (null when the drawing does not show it), all in mm
    val threadLengthMm: Double? = null,
    val headAcrossFlatsMm: Double? = null,
    val headHeightMm: Double? = null,
    val flangeDiaMm: Double? = null,
    val flangeThicknessMm: Double? = null,
    val shankDiaMm: Double? = null,
    val headDiaMm: Double? = null,
    val socketSizeMm: Double? = null,
    val socketDepthMm: Double? = null,
    val neckHeightMm: Double? = null,
    val nutHeightMm: Double? = null,
    val washerOuterDiaMm: Double? = null,
    val washerInnerDiaMm: Double? = null,
    val washerThicknessMm: Double? = null,
    /** Pan head crown radius rf and cylindrical edge height. */
    val headDomeRadiusMm: Double? = null,
    val headEdgeHeightMm: Double? = null,
    /** Tensile band from the drawing notes, when it is given instead of a property class. */
    val tensileMinMpa: Double? = null,
    val tensileMaxMpa: Double? = null,
    /** Nylon thread-locking patch / pellet called for on the drawing. */
    val threadLockPatch: Boolean = false,
    /** Order quantity from the drawing's title block (QTY), when shown. */
    val batchQty: Int? = null,
    /** Values the drawing did not show, so a default stands in: each entry starts with the value's name. */
    val assumed: List<String> = emptyList(),
    /** The part as a stack of solids from the head end to the point end, as the AI read it (empty when it gave none). */
    val solids: List<DrawnSolid> = emptyList(),
    val cutouts: List<DrawnCutout> = emptyList()
) {
    /** Every drawing dimension the volume model can use. */
    fun geometry(): DrawingGeometry {
        val roundHead = PartGeometry.styleOf(fastenerType) in setOf(
            HeadStyle.SOCKET_CAP, HeadStyle.SOCKET_COUNTERSUNK, HeadStyle.SOCKET_BUTTON, HeadStyle.CARRIAGE, HeadStyle.ROUND_PIN, HeadStyle.RIVET
        )
        return DrawingGeometry(
            threadLengthMm = threadLengthMm, shankDiaMm = shankDiaMm, acrossFlatsMm = headAcrossFlatsMm,
            // A round head's diameter is sometimes reported in the across-flats field
            headDiaMm = headDiaMm ?: headAcrossFlatsMm.takeIf { roundHead },
            headHeightMm = headHeightMm, flangeDiaMm = flangeDiaMm, flangeThicknessMm = flangeThicknessMm,
            socketSizeMm = socketSizeMm, socketDepthMm = socketDepthMm, neckHeightMm = neckHeightMm, nutHeightMm = nutHeightMm,
            washerOuterDiaMm = washerOuterDiaMm, washerInnerDiaMm = washerInnerDiaMm, washerThicknessMm = washerThicknessMm,
            headDomeRadiusMm = headDomeRadiusMm, headEdgeHeightMm = headEdgeHeightMm
        )
    }

    val tensileRange: TensileRange? get() = TensileRange.of(tensileMinMpa, tensileMaxMpa)
}
