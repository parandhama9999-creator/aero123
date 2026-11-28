package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import kotlin.math.abs

/** Head / nut geometry in mm. isTable = false means the size is outside the standard table and was estimated. */
@Serializable
data class HeadDimensions(
    val acrossFlatsS: Double,   // hex s, or the head diameter dk of a round head
    val headHeightK: Double,
    val bearingDiaDw: Double,   // min bearing face diameter
    val nutHeightM: Double,
    val source: String,
    val isTable: Boolean,
    /** Square neck height f of a carriage bolt (DIN 603); null for every other head. */
    val neckHeightMm: Double? = null
)

@Serializable
data class WasherDimensions(val innerDiaMm: Double, val outerDiaMm: Double, val thicknessMm: Double, val source: String, val isTable: Boolean)
