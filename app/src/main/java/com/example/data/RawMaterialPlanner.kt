package com.example.data

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.pow

/** One candidate feedstock diameter, costed out for the whole batch. */
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
    val cautions: List<String>
) {
    val feasible: Boolean get() = cautions.isEmpty()
}

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

/**
 * Raw material for a production run, across the diameters a shop actually stocks.
 *
 * The weight of a batch does not depend on which diameter it is headed from — the part's volume is
 * fixed, so the kilograms are fixed. What changes is the length of wire those kilograms arrive as,
 * and that is what a purchase order and a coil count are written in. Hence one weight for the batch
 * and a length per diameter.
 */
object RawMaterialPlanner {

    /** Cold-heading quality wire is drawn to order, so these are a starting set to edit, not a standard. */
    fun suggestedDiameters(computedWireDiaMm: Double): List<Double> {
        val base = (computedWireDiaMm * 2).toInt() / 2.0 // down to the nearest 0.5
        return listOf(base - 0.5, base, base + 0.5, base + 1.0)
            .filter { it >= 1.0 }
            .map { (it * 100).toInt() / 100.0 }
            .distinct()
    }

    fun plan(
        grossVolumeMm3: Double,
        densityGPerCm3: Double,
        batchQty: Int,
        wireDiameters: List<Double>,
        headVolumeMm3: Double = 0.0,
        shankDiaMm: Double? = null
    ): RawMaterialPlan? {
        if (grossVolumeMm3 <= 0.0 || densityGPerCm3 <= 0.0 || batchQty <= 0) return null
        val dias = wireDiameters.filter { it > 0.0 }.distinct().sorted()
        if (dias.isEmpty()) return null

        val grossWeightG = grossVolumeMm3 * densityGPerCm3 / 1000.0
        val batchWeightKg = grossWeightG * batchQty / 1000.0

        val options = dias.map { dw ->
            val areaMm2 = PI / 4.0 * dw.pow(2.0)
            val cutOffMm = grossVolumeMm3 / areaMm2
            // A mm^2 of wire one metre long weighs A x rho grams, so kg/m is A x rho / 1000.
            val kgPerMetre = areaMm2 * densityGPerCm3 / 1000.0
            val batchLengthM = cutOffMm * batchQty / 1000.0

            val ld = if (headVolumeMm3 > 0.0) (headVolumeMm3 / areaMm2) / dw else null
            val blows = ld?.let {
                when {
                    it <= HeadingProgression.SINGLE_BLOW_LD -> 1
                    it <= HeadingProgression.TWO_BLOW_LD -> 2
                    else -> 3
                }
            }
            val reduction = shankDiaMm
                ?.takeIf { it > 0.0 && it < dw }
                ?.let { (1.0 - (it / dw).pow(2.0)) * 100.0 }

            val cautions = buildList {
                if (ld != null && ld > HeadingProgression.THREE_BLOW_LD) {
                    add("Head stock L/D ${fmt(ld)} is past the 3-blow limit of ${HeadingProgression.THREE_BLOW_LD}: this diameter is too thin to gather the head.")
                }
                if (reduction != null && reduction > 30.0) {
                    add("Extruding Ø$dw down to Ø$shankDiaMm removes ${fmt(reduction, 0)} % of area, past the ~30 % open-die limit: needs a trapped die, or a thinner wire.")
                }
                if (shankDiaMm != null && shankDiaMm > dw) {
                    add("Shank Ø$shankDiaMm is larger than the wire: the shank cannot be formed from this diameter.")
                }
            }

            FeedstockOption(
                wireDiaMm = dw,
                cutOffLengthMm = cutOffMm,
                kgPerMetre = kgPerMetre,
                piecesPerMetre = 1000.0 / cutOffMm,
                batchLengthM = batchLengthM,
                batchWeightKg = batchWeightKg,
                upsetLd = ld,
                blows = blows,
                extrusionReductionPct = reduction,
                cautions = cautions
            )
        }

        return RawMaterialPlan(
            batchQty = batchQty,
            grossWeightPerPieceG = grossWeightG,
            batchWeightKg = batchWeightKg,
            piecesPerTonne = if (grossWeightG > 0) (1_000_000.0 / grossWeightG).toInt() else 0,
            options = options
        )
    }

    /** Coils a run needs, given the coil weight the supplier ships. */
    fun coilsNeeded(batchWeightKg: Double, coilWeightKg: Double): Int =
        if (coilWeightKg <= 0.0) 0 else ceil(batchWeightKg / coilWeightKg).toInt()

    /** Parses "5.5, 6, 6.5" from the diameter field. */
    fun parseDiameters(text: String): List<Double> =
        text.split(',', ';', ' ', '\n')
            .mapNotNull { it.trim().toDoubleOrNull() }
            .filter { it > 0.0 && it < 100.0 }
            .distinct()
            .sorted()

    private fun fmt(v: Double, dp: Int = 1) = String.format(java.util.Locale.US, "%.${dp}f", v)
}
