package com.aistudio.aerofast.trmst.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

enum class SolidShape { CYLINDER, CONE, HEX, SQUARE, TWELVE_POINT, DOME }

/** One section of the part as the AI read it off the drawing, listed from the head end to the point end. */
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

/** Turns a scanned drawing into the engine's calculation and into the part's total volume. */
object DrawingScan {

    private fun n(v: Double, dp: Int = 2) = String.format(java.util.Locale.US, "%.${dp}f", v)

    /** The full engineering calculation for the scanned part, with every drawing dimension applied. */
    fun calculate(spec: ExtractedFastenerSpec): FastenerCalculationResult {
        val material = FastenerCatalog.MATERIALS[spec.materialKey] ?: FastenerCatalog.MATERIALS.getValue("10B21")
        return CalculationEngine.calculate(
            fastenerType = spec.fastenerType, unitSystem = spec.unitSystem,
            standard = PokaYokeEngine.defaultStandard(spec.fastenerType, spec.unitSystem),
            nominalSizeIndex = spec.nominalSizeIndex, pitchValue = spec.pitchValue, lengthMm = spec.lengthMm,
            threadClass = spec.threadClass, material = material, coating = spec.coating, batchQty = spec.batchQty ?: ShopDefaults.batchQty,
            customNominalDiaMm = spec.nominalDiaMm, extractedRadius = spec.radius, propertyClass = spec.propertyClass,
            drawing = spec.geometry(), tensileRange = spec.tensileRange, threadLockPatch = spec.threadLockPatch
        )
    }

    fun volume(spec: ExtractedFastenerSpec, result: FastenerCalculationResult = calculate(spec)): DrawingVolume {
        val part = result.finishedPart
        val modelVol = part?.volumeMm3 ?: 0.0
        val scrap = max(0.0, (result.headingPlan?.slugVolumeMm3 ?: modelVol) - modelVol)
        val coreAssumed = spec.assumed.filter { it.startsWith("nominal diameter") || it.startsWith("length") || it.startsWith("pitch") }
        val coreRead = buildList {
            if (spec.assumed.none { it.startsWith("nominal diameter") }) add("nominal diameter d ${n(spec.nominalDiaMm)} mm")
            if (spec.assumed.none { it.startsWith("length") } && !spec.fastenerType.isNut && !spec.fastenerType.isWasher) add("length L ${n(spec.lengthMm)} mm")
        }

        val modelHead = part?.headVolumeMm3 ?: 0.0
        val headShare = if (modelVol > 0.0) modelHead / modelVol else 0.0

        val solids = solidsVolume(spec, result)
        if (solids != null && solids.rejected == null) {
            val delta = if (modelVol > 0.0) (solids.total - modelVol) / modelVol * 100.0 else 0.0
            return DrawingVolume(
                basis = VolumeBasis.SOLIDS, items = solids.items, totalMm3 = solids.total, densityGPerCm3 = result.material.density,
                fromDrawing = coreRead + solids.items.map { "${it.feature}: ${it.detail}" },
                notOnDrawing = coreAssumed,
                cautions = buildList {
                    addAll(solids.cautions)
                    if (abs(delta) > 2.0) add(
                        "The solids give ${n(solids.total, 0)} mm³; the standard shape of this type gives ${n(modelVol, 0)} mm³ " +
                            "(${if (delta > 0) "+" else ""}${n(delta, 1)} %). Check the solids list against the drawing."
                    )
                },
                modelVolumeMm3 = modelVol, scrapMm3 = scrap, wireDiaMm = result.wireDiaMm, outline = solids.outline,
                headMm3 = solids.total * headShare, headIsScaled = true
            )
        }
        return DrawingVolume(
            basis = VolumeBasis.DIMENSIONS, items = part?.items.orEmpty(), totalMm3 = modelVol, densityGPerCm3 = result.material.density,
            fromDrawing = coreRead + part?.fromDrawing.orEmpty(),
            notOnDrawing = coreAssumed + part?.notOnDrawing.orEmpty(),
            cautions = part?.cautions.orEmpty() + listOfNotNull(solids?.rejected),
            modelVolumeMm3 = modelVol, scrapMm3 = scrap, wireDiaMm = result.wireDiaMm, outline = part?.asStation(),
            headMm3 = modelHead, headIsScaled = false
        )
    }

    private class SolidsResult(val items: List<VolumeItem>, val total: Double, val outline: HeadingStation?, val cautions: List<String>, val rejected: String?)

    /** Overall axial length the solids must add up to, when the drawing gave what is needed to know it. */
    private fun expectedOverallMm(spec: ExtractedFastenerSpec): Double? {
        val lengthRead = spec.assumed.none { it.startsWith("length") }
        return when (PartGeometry.styleOf(spec.fastenerType)) {
            HeadStyle.NUT -> spec.nutHeightMm ?: spec.headHeightMm
            HeadStyle.WASHER -> spec.washerThicknessMm
            // A headless part and a countersunk screw are dimensioned over their whole length
            HeadStyle.HEADLESS, HeadStyle.SOCKET_COUNTERSUNK -> spec.lengthMm.takeIf { lengthRead }
            else -> spec.headHeightMm?.takeIf { lengthRead }?.let { spec.lengthMm + it }
        }
    }

    /** Volume from the AI's solids; null when the scan has none, `rejected` set when they do not hold together. */
    private fun solidsVolume(spec: ExtractedFastenerSpec, result: FastenerCalculationResult): SolidsResult? {
        if (spec.solids.isEmpty()) return null
        fun reject(why: String) = SolidsResult(emptyList(), 0.0, null, emptyList(), "The solids read from the drawing were not used: $why.")
        if (spec.solids.any { it.lengthMm <= 0.0 || it.d1Mm <= 0.0 || (it.shape == SolidShape.CONE && it.d2Mm < 0.0) })
            return reject("a section has no size")
        if (spec.cutouts.any { it.lengthMm <= 0.0 || it.d1Mm <= 0.0 || it.count < 1 }) return reject("a cut-out has no size")

        val pitchMm = result.pitchMm
        // A thread's material sits about its pitch line: the rolling blank (bolt) or the tap drill (nut) carries its volume
        fun threadDia(nominal: Double, internal: Boolean): Double = when {
            abs(nominal - result.nominalDiaMm) <= 0.02 * result.nominalDiaMm && internal == result.isInternalNut -> result.blankTargetDia
            internal -> nominal - pitchMm
            else -> nominal - 0.6495 * pitchMm
        }
        fun area(shape: SolidShape, size: Double) = when (shape) {
            SolidShape.HEX -> sectionArea(SectionShape.HEX, size)
            SolidShape.TWELVE_POINT -> sectionArea(SectionShape.TWELVE_POINT, size)
            SolidShape.SQUARE -> sectionArea(SectionShape.SQUARE, size)
            else -> sectionArea(SectionShape.ROUND, size)
        }
        fun segment(s: DrawnSolid): ProfileSegment = when (s.shape) {
            SolidShape.CYLINDER -> ProfileSegment(s.lengthMm, if (s.threaded) threadDia(s.d1Mm, false) else s.d1Mm)
            // Listed head → point, drawn bottom (point) → top (head)
            SolidShape.CONE -> ProfileSegment(s.lengthMm, s.d2Mm, s.d1Mm)
            SolidShape.HEX -> ProfileSegment(s.lengthMm, s.d1Mm, shape = SectionShape.HEX)
            SolidShape.TWELVE_POINT -> ProfileSegment(s.lengthMm, s.d1Mm, shape = SectionShape.TWELVE_POINT)
            SolidShape.SQUARE -> ProfileSegment(s.lengthMm, s.d1Mm, shape = SectionShape.SQUARE)
            SolidShape.DOME -> ProfileSegment(s.lengthMm, s.d1Mm, shape = SectionShape.DOME)
        }
        fun describe(s: DrawnSolid): String = when (s.shape) {
            SolidShape.CYLINDER -> if (s.threaded) "thread Ø${n(s.d1Mm)}, blank Ø${n(threadDia(s.d1Mm, false), 3)} × ${n(s.lengthMm)} mm" else "Ø${n(s.d1Mm)} × ${n(s.lengthMm)} mm"
            SolidShape.CONE -> "cone Ø${n(s.d1Mm)} → Ø${n(s.d2Mm)} × ${n(s.lengthMm)} mm"
            SolidShape.HEX -> "hex s ${n(s.d1Mm)} × ${n(s.lengthMm)} mm"
            SolidShape.TWELVE_POINT -> "12-point s ${n(s.d1Mm)} × ${n(s.lengthMm)} mm"
            SolidShape.SQUARE -> "square ${n(s.d1Mm)} × ${n(s.d1Mm)} × ${n(s.lengthMm)} mm"
            SolidShape.DOME -> "dome Ø${n(s.d1Mm)} × ${n(s.lengthMm)} mm"
        }

        val overall = spec.solids.sumOf { it.lengthMm }
        val expected = expectedOverallMm(spec)
        if (expected != null && abs(overall - expected) > max(0.5, 0.02 * expected))
            return reject("their lengths add up to ${n(overall)} mm but the drawing's overall length is ${n(expected)} mm")

        val segments = spec.solids.map { it to segment(it) }
        val items = segments.map { (s, seg) -> VolumeItem(s.feature.ifBlank { s.shape.name.lowercase() }, describe(s), seg.volumeMm3, DimSource.DRAWING) }.toMutableList()
        spec.cutouts.forEach { c ->
            val vol = c.count * when (c.shape) {
                SolidShape.CONE -> PI * c.lengthMm / 12.0 * (c.d1Mm * c.d1Mm + c.d1Mm * c.d2Mm + c.d2Mm * c.d2Mm)
                SolidShape.CYLINDER -> area(c.shape, if (c.threaded) threadDia(c.d1Mm, true) else c.d1Mm) * c.lengthMm
                else -> area(c.shape, c.d1Mm) * c.lengthMm
            }
            val what = (if (c.shape == SolidShape.CYLINDER) "Ø${n(c.d1Mm)}" else "${c.shape.name.lowercase().replace('_', '-')} ${n(c.d1Mm)}") +
                " × ${n(c.lengthMm)} mm" + (if (c.count > 1) " × ${c.count}" else "") + (if (c.threaded) " (tapped)" else "")
            items += VolumeItem(c.feature.ifBlank { "cut-out" }, what, -vol, DimSource.DRAWING)
        }
        val total = items.sumOf { it.volumeMm3 }
        if (total <= 0.0) return reject("the cut-outs remove more than the solids hold")

        // Picture: bottom (point) → top (head). Cut-outs the outline cannot place are still in the volume.
        fun recessShape(shape: SolidShape) = when (shape) {
            SolidShape.HEX -> SectionShape.HEX
            SolidShape.TWELVE_POINT -> SectionShape.TWELVE_POINT
            SolidShape.SQUARE -> SectionShape.SQUARE
            else -> SectionShape.ROUND
        }
        val outline = HeadingStation(
            index = 1, title = "Part as read from the drawing", operation = "",
            segments = segments.map { it.second }.asReversed(),
            recesses = spec.cutouts.filter { it.from == CutoutFrom.HEAD_END || it.from == CutoutFrom.POINT_END }
                .map { Recess(it.from == CutoutFrom.HEAD_END, it.d1Mm, it.lengthMm.coerceAtMost(overall), recessShape(it.shape)) },
            throughHoleMm = spec.cutouts.firstOrNull { it.from == CutoutFrom.THROUGH && it.shape == SolidShape.CYLINDER }
                ?.let { if (it.threaded) threadDia(it.d1Mm, true) else it.d1Mm }
        )
        val cautions = buildList {
            if (expected == null) add("The overall length could not be cross-checked against the drawing: verify the solids list.")
        }
        return SolidsResult(items, total, outline, cautions, null)
    }
}
