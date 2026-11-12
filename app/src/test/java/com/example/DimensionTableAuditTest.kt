package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

/**
 * Audits the head / socket / washer tables. These values were entered by hand, so this checks the things
 * that must hold for a real standard: the geometry each head type is defined by, sizes that grow with the
 * diameter, drives that fit inside their head, and catalogue weights for parts whose mass is well known.
 */
class DimensionTableAuditTest {

    private val metricSizes = listOf(3.0, 4.0, 5.0, 6.0, 8.0, 10.0, 12.0, 16.0, 20.0)
    private fun head(type: FastenerType, d: Double, unit: UnitSystem = UnitSystem.METRIC) = DimensionTables.forPart(type, unit, d)
    private fun tabled(type: FastenerType, unit: UnitSystem = UnitSystem.METRIC, sizes: List<Double> = metricSizes) =
        sizes.map { it to head(type, it, unit) }.filter { it.second.isTable }

    /** ISO 10642 is a 90° countersink, so the head height is fixed by the head and shank diameters: k = (dk − d) / 2. */
    @Test fun countersunkHeadsSatisfyThe90DegreeCone() {
        val rows = tabled(FastenerType.SOCKET_CSK, sizes = metricSizes + listOf(14.0))
        assertEquals(10, rows.size)
        rows.forEach { (d, h) ->
            assertEquals("M$d countersink angle", (h.acrossFlatsS - d) / 2.0, h.headHeightK, 0.01)
            assertTrue("M$d head Ø ${h.acrossFlatsS}", h.acrossFlatsS / d in 2.0..2.3)
        }
    }

    @Test fun headSizesGrowWithTheDiameter() {
        listOf(FastenerType.SHCS, FastenerType.SOCKET_CSK, FastenerType.SOCKET_BUTTON, FastenerType.CARRIAGE_BOLT, FastenerType.HEX_BOLT).forEach { type ->
            val rows = tabled(type)
            assertTrue("$type has no table rows", rows.size >= 6)
            rows.zipWithNext { (d1, a), (d2, b) ->
                assertTrue("$type head Ø not increasing from M$d1 to M$d2", b.acrossFlatsS > a.acrossFlatsS)
                assertTrue("$type head height not increasing from M$d1 to M$d2", b.headHeightK > a.headHeightK)
            }
        }
        // inch socket cap screws too
        val inch = tabled(FastenerType.SHCS, UnitSystem.INCH, listOf(0.25, 0.3125, 0.375, 0.5, 0.625, 0.75, 1.0).map { it * 25.4 })
        assertEquals(7, inch.size)
        inch.zipWithNext { (_, a), (_, b) -> assertTrue(b.acrossFlatsS > a.acrossFlatsS) }
    }

    @Test fun eachHeadTypeKeepsItsOwnProportions() {
        // dk / d and k / d bands that separate the head types from one another
        mapOf(
            FastenerType.SHCS to (1.45..1.90 to 0.98..1.02),            // ISO 4762: k = d; dk falls from 1.83 d (M3) to 1.5 d (M20)
            FastenerType.SOCKET_BUTTON to (1.70..1.95 to 0.52..0.58),   // ISO 7380: low dome
            FastenerType.CARRIAGE_BOLT to (2.30..2.80 to 0.50..0.68),   // DIN 603: wide shallow dome
            FastenerType.HEX_BOLT to (1.45..1.90 to 0.60..0.70)         // ISO 4014: 1.83 d at M3 down to 1.5 d at M20
        ).forEach { (type, bands) ->
            val (dkBand, kBand) = bands
            tabled(type).forEach { (d, h) ->
                assertTrue("$type M$d head Ø / d = ${h.acrossFlatsS / d}", h.acrossFlatsS / d in dkBand)
                assertTrue("$type M$d head height / d = ${h.headHeightK / d}", h.headHeightK / d in kBand)
            }
        }
        // A socket head is proportionally largest on the smallest thread. (The ISO 4014 hex series is not
        // monotonic this way: M5 is 8 mm across flats and M6 is 10 mm, so it is left out.)
        listOf(FastenerType.SHCS, FastenerType.SOCKET_BUTTON).forEach { type ->
            tabled(type).zipWithNext { (d1, a), (d2, b) ->
                assertTrue("$type head Ø / d grew from M$d1 to M$d2", b.acrossFlatsS / d2 <= a.acrossFlatsS / d1 + 1e-9)
            }
        }
        // ASME B18.3 socket cap screws: head height equals the nominal diameter
        listOf(0.25, 0.375, 0.5, 0.75, 1.0).forEach { inch ->
            val h = head(FastenerType.SHCS, inch * 25.4, UnitSystem.INCH)
            assertTrue("$inch\" is not a table size", h.isTable)
            assertEquals("$inch\" head height", inch * 25.4, h.headHeightK, 1e-6)
            assertTrue("$inch\" head Ø / d = ${h.acrossFlatsS / (inch * 25.4)}", h.acrossFlatsS / (inch * 25.4) in 1.40..1.60)
        }
    }

    @Test fun everyDriveFitsInsideItsHead() {
        listOf(FastenerType.SHCS, FastenerType.SOCKET_CSK, FastenerType.SOCKET_BUTTON).forEach { type ->
            tabled(type).forEach { (d, h) ->
                val (key, depth) = DimensionTables.socketFor(type, UnitSystem.METRIC, d)!!
                // Across corners of the hex socket must still sit inside the head
                assertTrue("$type M$d socket $key across corners vs head ${h.acrossFlatsS}", key * 2.0 / Math.sqrt(3.0) < h.acrossFlatsS)
                assertTrue("$type M$d socket depth $depth vs head height ${h.headHeightK}", depth < h.headHeightK)
                assertTrue("$type M$d socket key $key vs d", key in 0.3 * d..0.9 * d)
            }
        }
        listOf(0.25, 0.375, 0.5, 0.75, 1.0).forEach { inch ->
            val d = inch * 25.4
            val (key, _) = DimensionTables.socketFor(FastenerType.SHCS, UnitSystem.INCH, d)!!
            assertTrue("$inch\" socket $key", key * 2.0 / Math.sqrt(3.0) < head(FastenerType.SHCS, d, UnitSystem.INCH).acrossFlatsS)
        }
    }

    @Test fun washerTableIsARingThatFitsItsBolt() {
        metricSizes.plus(listOf(24.0, 30.0, 36.0)).forEach { d ->
            val w = DimensionTables.washerFor(FastenerType.FLAT_WASHER, UnitSystem.METRIC, d)
            assertTrue("M$d washer is not a table size", w.isTable)
            assertTrue("M$d bore ${w.innerDiaMm} must clear the bolt", w.innerDiaMm > d && w.innerDiaMm < d + 1.5)
            assertTrue("M$d outside Ø / d = ${w.outerDiaMm / d}", w.outerDiaMm / d in 1.8..2.4)
            assertTrue("M$d thickness ${w.thicknessMm}", w.thicknessMm in 0.08 * d..0.30 * d)
        }
    }

    /** Catalogue masses, steel at 7.85 g/cm³, for parts whose weight is widely published. */
    @Test fun computedWeightsMatchCatalogueWeights() {
        fun grams(type: FastenerType, d: Double, len: Double, std: ProductStandard, cls: String = "6g"): Double {
            val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, d)
            return CalculationEngine.calculate(
                fastenerType = type, unitSystem = UnitSystem.METRIC, standard = std, nominalSizeIndex = idx,
                pitchValue = FastenerCatalog.METRIC_SIZES[idx].pitches.first(), lengthMm = len, threadClass = cls,
                material = FastenerCatalog.MATERIALS["10B21"]!!, coating = CoatingType.GEOMET, batchQty = 1,
                propertyClass = PropertyClass.CLASS_10_9
            ).netWeightGrams
        }
        // ISO 7089 M10 washer: a plain ring 10.5 / 20 / 2, so its mass follows from the table alone
        assertEquals(PI / 4 * (20.0 * 20.0 - 10.5 * 10.5) * 2.0 * 7.85 / 1000.0,
            grams(FastenerType.FLAT_WASHER, 10.0, 2.0, ProductStandard.ISO_7089, ""), 0.01)
        // Published catalogue masses, within 10 % (chamfers, fillets and the thread run-out are not modelled)
        listOf(
            Triple("ISO 4014 M10×60", 44.5, grams(FastenerType.HEX_BOLT, 10.0, 60.0, ProductStandard.ISO_4014)),
            Triple("ISO 4032 M10 nut", 10.9, grams(FastenerType.HEX_NUT, 10.0, 10.0, ProductStandard.ISO_4032, "6H")),
            Triple("ISO 4762 M10×40", 34.0, grams(FastenerType.SHCS, 10.0, 40.0, ProductStandard.ISO_4762)),
            Triple("ISO 4762 M6×20", 6.6, grams(FastenerType.SHCS, 6.0, 20.0, ProductStandard.ISO_4762)),
            Triple("ISO 10642 M10×40", 26.5, grams(FastenerType.SOCKET_CSK, 10.0, 40.0, ProductStandard.ISO_10642)),
            Triple("ISO 7089 M10 washer", 3.6, grams(FastenerType.FLAT_WASHER, 10.0, 2.0, ProductStandard.ISO_7089, ""))
        ).forEach { (name, published, computed) ->
            assertTrue("$name: catalogue ${published} g, computed ${"%.1f".format(computed)} g",
                abs(computed - published) / published <= 0.10)
        }
    }

    /** A value the app cannot source is reported as an estimate, never presented as a standard dimension. */
    @Test fun unsourcedValuesAreLabelled() {
        assertNull("DIN 603 neck height is not a sourced value", head(FastenerType.CARRIAGE_BOLT, 10.0).neckHeightMm)
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
        val part = CalculationEngine.calculate(
            fastenerType = FastenerType.CARRIAGE_BOLT, unitSystem = UnitSystem.METRIC, standard = ProductStandard.DIN_603,
            nominalSizeIndex = idx, pitchValue = 1.5, lengthMm = 50.0, threadClass = "6g",
            material = FastenerCatalog.MATERIALS["10B21"]!!, coating = CoatingType.GEOMET, batchQty = 1,
            propertyClass = PropertyClass.CLASS_8_8
        ).finishedPart!!
        assertTrue(part.notOnDrawing.toString(), part.notOnDrawing.any { it.startsWith("square neck height f") && it.contains("estimated") })
        // the neck is still a square of side d in the volume, not dropped
        assertEquals(10.0 * 10.0 * 4.5, part.segments.first { it.shape == SectionShape.SQUARE }.volumeMm3, 1e-6)
        // and the drawing overrides it
        val drawn = PartGeometry.build(
            FastenerType.CARRIAGE_BOLT, UnitSystem.METRIC, 10.0, 50.0, 26.0, 8.96,
            DimensionTables.forPart(FastenerType.CARRIAGE_BOLT, UnitSystem.METRIC, 10.0),
            null, DrawingGeometry(neckHeightMm = 4.0)
        )
        assertEquals(10.0 * 10.0 * 4.0, drawn.segments.first { it.shape == SectionShape.SQUARE }.volumeMm3, 1e-6)
    }
}
