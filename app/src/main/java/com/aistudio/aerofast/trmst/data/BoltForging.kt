package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.math.tan

/** Forging figures for a headed bolt; each "from drawing" flag says whether the drawing gave the value or the standard did. */
@Serializable
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
