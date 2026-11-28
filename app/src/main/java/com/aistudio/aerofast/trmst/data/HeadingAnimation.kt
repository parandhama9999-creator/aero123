package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sqrt

/** One moment of the heading animation. Station indices refer to plan.stations (0 = cut-off slug). */
@Serializable
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
