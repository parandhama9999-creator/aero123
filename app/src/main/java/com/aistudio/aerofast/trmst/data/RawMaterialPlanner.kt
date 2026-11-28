package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.pow

/** One candidate feedstock diameter, costed out for the whole batch. */
@Serializable
data class FeedstockOption(
    val wireDiaMm: Double,
    val cutOffLengthMm: Double,
    /** Mass of one metre of this wire, the figure a mill quotes against. */
    val kgPerMetre: Double,
    val piecesPerMetre: Double,
    val batchLengthM: Double,
    val batchWeightKg: Double,
    /** Head stock L/D at this diameter, and the blows it forces. Null when the part has no head. */
    val upsetLd: Double?,
    val blows: Int?,
    /** Area reduction from this wire down to the shank, % — null when nothing is extruded. */
    val extrusionReductionPct: Double?,
    /** What this diameter is for the part: the rolling blank, the shank, or neither. */
    val role: String?,
    /** Die bore the first station needs at this diameter: the wire plus the transfer clearance. */
    val firstDieBoreMm: Double,
    val cautions: List<String>
) {
    val feasible: Boolean get() = cautions.isEmpty()
}

@Serializable
data class RawMaterialPlan(
    val batchQty: Int,
    val grossWeightPerPieceG: Double,
    /** Same for every diameter: upsetting moves metal, it does not create or destroy it. */
    val batchWeightKg: Double,
    val piecesPerTonne: Int,
    val options: List<FeedstockOption>
) {
    /** The lightest feasible option is always the shortest one, so rank by cut-off length. */
    val recommended: FeedstockOption? get() = options.filter { it.feasible }.minByOrNull { it.cutOffLengthMm }
}
