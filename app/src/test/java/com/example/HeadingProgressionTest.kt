package com.example

import com.example.data.CalculationEngine
import com.example.data.CoatingType
import com.example.data.FastenerCatalog
import com.example.data.FastenerType
import com.example.data.ProductStandard
import com.example.data.PropertyClass
import com.example.data.SectionShape
import com.example.data.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

/** Cold-heading progression: every station must hold the slug volume and end at the finished geometry. */
class HeadingProgressionTest {
    private val m10 = FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == 10.0 }

    private fun calc(type: FastenerType, std: ProductStandard, cls: String, length: Double = 60.0) = CalculationEngine.calculate(
        fastenerType = type, unitSystem = UnitSystem.METRIC, standard = std, nominalSizeIndex = m10,
        pitchValue = 1.5, lengthMm = length, threadClass = cls, material = FastenerCatalog.MATERIALS["10B21"]!!,
        coating = CoatingType.GEOMET, batchQty = 1000, propertyClass = PropertyClass.CLASS_10_9
    )

    @Test fun everyStation_conservesSlugVolume() {
        listOf(
            Triple(FastenerType.HEX_BOLT, ProductStandard.ISO_4014, "6g"),
            Triple(FastenerType.HEX_BOLT_FULL, ProductStandard.ISO_4017, "6g"),
            Triple(FastenerType.SHCS, ProductStandard.ISO_4762, "6g"),
            Triple(FastenerType.SOCKET_SET, ProductStandard.ISO_4026, "6g"),
            Triple(FastenerType.FLANGE_BOLT, ProductStandard.ISO_1665, "6g"),
            Triple(FastenerType.STUD, ProductStandard.ASTM_A193, "6g"),
            Triple(FastenerType.HEX_NUT, ProductStandard.ISO_4032, "6H")
        ).forEach { (type, std, cls) ->
            listOf(12.0, 60.0, 200.0).forEach { len ->
                val plan = calc(type, std, cls, len).headingPlan
                assertNotNull("$type", plan)
                assertTrue("$type L=$len volume error ${plan!!.maxVolumeError}", plan.maxVolumeError < 1e-9)
            }
        }
    }

    @Test fun hexBolt_progressionEndsAtIsoGeometry() {
        val r = calc(FastenerType.HEX_BOLT, ProductStandard.ISO_4014, "6g")
        val plan = r.headingPlan!!
        // Trimmed hex: round preform at 1.2 × e carries the cheese, so head stock L/D > 2.3 → cone pre-upset, upset, trim
        assertEquals(2, plan.upsetBlows)
        assertEquals(listOf("Shear cut-off", "Square & forward extrude", "Cone pre-upset", "Head upset (round, for trimming)", "Trim to hex"), plan.stations.map { it.title })
        val last = plan.stations.last()
        assertEquals(SectionShape.HEX, last.segments.last().shape)
        assertEquals(16.0, last.segments.last().dBottomMm, 1e-9)
        assertEquals(60.0 + 6.4, last.heightMm, 1e-9)
        assertEquals(r.cutOffLengthMm, plan.slugLengthMm, 0.05)
        assertTrue(r.recommendedMachine, r.recommendedMachine.startsWith("4-die progressive former"))
        assertTrue(r.forgingStages.last().title.contains("Thread rolling"))
    }

    @Test fun socketHead_needsConeAndHasIsoSocket() {
        val plan = calc(FastenerType.SHCS, ProductStandard.ISO_4762, "6g").headingPlan!!
        assertEquals(2, plan.upsetBlows)
        assertTrue(plan.stations.any { it.title.startsWith("Cone") })
        val socket = plan.stations.last().recesses.single()
        assertEquals(8.0, socket.sizeMm, 1e-9)
        assertEquals(5.0, socket.depthMm, 1e-9)
        // Every intermediate head preform stays inside the finished head diameter
        plan.stations.forEach { assertTrue(it.maxWidthMm <= 16.0 + 1e-9) }
    }

    @Test fun nut_isPiercedLastAndSlugIncludesWeb() {
        val r = calc(FastenerType.HEX_NUT, ProductStandard.ISO_4032, "6H")
        val plan = r.headingPlan!!
        assertEquals(listOf("Shear cut-off", "Square & pre-upset", "Pre-form & indent", "Hex form", "Pierce"), plan.stations.map { it.title })
        val pierce = plan.stations.last()
        assertNotNull(pierce.throughHoleMm)
        assertTrue(pierce.scrapMm3 > 0.0)
        assertEquals(8.4, pierce.heightMm, 1e-9)
        val wireArea = PI / 4 * r.wireDiaMm * r.wireDiaMm
        assertEquals(plan.slugVolumeMm3 / wireArea, r.cutOffLengthMm, 0.05)
        assertTrue(r.forgingStages.last().title.contains("Tapping"))
    }

    @Test fun washer_hasNoHeadingProgression() {
        val r = calc(FastenerType.FLAT_WASHER, ProductStandard.ISO_7089, "")
        assertNull(r.headingPlan)
        assertTrue(r.recommendedMachine.contains("press"))
    }
}
