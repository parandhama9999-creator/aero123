package com.example

import com.example.data.AnnealRoute
import com.example.data.FastenerCatalog
import com.example.data.SoftAnnealEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftAnnealTest {

    private fun spec(key: String) = SoftAnnealEngine.build(FastenerCatalog.MATERIALS[key]!!)

    @Test fun boronSteel_spheroidizes_andReadsGlobularTargetFromCatalogue() {
        val s = spec("10B21")
        assertEquals(AnnealRoute.SPHEROIDIZE, s.route)
        assertTrue(s.inHouseAnnealNeeded)
        // catalogue says "480 - 560 MPa (Spheroidized Annealed Wire, >85% globular)"
        assertTrue("structure=${s.structureTarget}", s.structureTarget.contains("globular"))
        // HB derived from Rm/3.38 -> 142..166
        assertTrue("hb=${s.targetHardnessHb}", s.targetHardnessHb.startsWith("142 – 166"))
        assertTrue("hrb=${s.targetHardnessHrb}", s.targetHardnessHrb.isNotBlank())
        // wire anneals to ~50% of the 1040 MPa finished UTS
        assertEquals(0.5, s.softeningRatio!!, 0.02)
        assertTrue(s.standardReference.contains("EN 10263"))
    }

    @Test fun austeniticStainless_solutionAnneals_notSpheroidize() {
        val s = spec("SS316")
        assertEquals(AnnealRoute.SOLUTION_ANNEAL, s.route)
        assertTrue("temp=${s.temperatureC}", s.temperatureC.isNotBlank())
        assertTrue("cool=${s.coolingRate}", s.coolingRate.lowercase().contains("rapid"))
        assertTrue(s.notes.any { it.contains("opposite") })
    }

    @Test fun aluminium_usesOTemper_andFlagsMissingCycle() {
        val al = FastenerCatalog.MATERIALS.keys.first { it.uppercase().startsWith("AL") }
        val s = spec(al)
        assertEquals(AnnealRoute.O_TEMPER, s.route)
        // no invented temperature for a family the catalogue has no cycle for
        assertEquals("", s.temperatureC)
        assertTrue(s.unsourced.any { it.contains("temperature") })
    }

    @Test fun coldDrawnWire_isFlaggedAsNotHeadable() {
        val drawn = FastenerCatalog.MATERIALS.values.filter {
            it.rawMaterialSourcingUtsMpa.uppercase().let { c ->
                c.contains("COLD DRAWN") || c.contains("HALF-HARD")
            }
        }
        assertTrue("expected some cold-drawn grades in the catalogue", drawn.isNotEmpty())
        drawn.forEach {
            val s = SoftAnnealEngine.build(it)
            assertEquals("${it.key}", AnnealRoute.ANNEAL_REQUIRED, s.route)
            assertTrue(s.notes.any { n -> n.contains("not a headable condition") })
        }
    }

    @Test fun neverInventsAValueItCannotSource() {
        // every material must either carry a temperature or declare why it has none
        FastenerCatalog.MATERIALS.values.forEach { m ->
            val s = SoftAnnealEngine.build(m)
            val hasTemp = s.temperatureC.isNotBlank()
            val declared = s.unsourced.isNotEmpty() || s.route == AnnealRoute.MILL_ANNEALED
            assertTrue("${m.key}: blank temperature with nothing in unsourced", hasTemp || declared)
        }
    }

    @Test fun everyCatalogueMaterialProducesASpec() {
        FastenerCatalog.MATERIALS.values.forEach { m ->
            assertNotNull(m.key, SoftAnnealEngine.build(m))
        }
    }

    @Test fun wiredIntoCalculationResult() {
        val m10 = FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == 10.0 }
        val r = com.example.data.CalculationEngine.calculate(
            fastenerType = com.example.data.FastenerType.HEX_BOLT,
            unitSystem = com.example.data.UnitSystem.METRIC,
            standard = com.example.data.ProductStandard.ISO_4014,
            nominalSizeIndex = m10, pitchValue = 1.5, lengthMm = 50.0, threadClass = "6g",
            material = FastenerCatalog.MATERIALS["10B21"]!!,
            coating = com.example.data.CoatingType.GEOMET, batchQty = 1000,
            propertyClass = com.example.data.PropertyClass.CLASS_10_9
        )
        assertNotNull(r.softAnnealSpec)
        assertEquals(AnnealRoute.SPHEROIDIZE, r.softAnnealSpec!!.route)
    }
}
