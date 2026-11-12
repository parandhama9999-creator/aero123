package com.example

import com.example.data.CalculationEngine
import com.example.data.CoatingType
import com.example.data.DimensionTables
import com.example.data.FastenerCatalog
import com.example.data.FastenerType
import com.example.data.PokaYokeEngine
import com.example.data.PokaYokeSeverity
import com.example.data.ProductStandard
import com.example.data.PropertyClass
import com.example.data.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFastenerTypesTest {
    private val newTypes = listOf(
        FastenerType.TTY_HEAD_BOLT, FastenerType.HEXALOBULAR_SCREW, FastenerType.THREAD_FORMING_SCREW,
        FastenerType.WHEEL_BOLT, FastenerType.BANJO_BOLT, FastenerType.WHEEL_NUT,
        FastenerType.NAS_CLOSE_TOL_BOLT, FastenerType.HI_LOK_PIN, FastenerType.TAPER_LOK, FastenerType.LOCKBOLT_PIN,
        FastenerType.BLIND_BOLT, FastenerType.AERO_LOCK_NUT, FastenerType.HI_LOK_COLLAR, FastenerType.NUTPLATE,
        FastenerType.SOLID_RIVET, FastenerType.BLIND_RIVET
    )

    private fun calc(type: FastenerType, unit: UnitSystem): com.example.data.FastenerCalculationResult {
        val idx = if (unit == UnitSystem.METRIC) FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == 10.0 }
        else FastenerCatalog.INCH_SIZES.indexOfFirst { it.dInch == 0.375 }
        return CalculationEngine.calculate(
            fastenerType = type, unitSystem = unit,
            standard = PokaYokeEngine.defaultStandard(type, unit), nominalSizeIndex = idx,
            pitchValue = if (unit == UnitSystem.METRIC) 1.5 else 16.0, lengthMm = 50.0,
            threadClass = if (unit == UnitSystem.METRIC) (if (type.isNut) "6H" else "6g") else (if (type.isNut) "2B" else "2A"),
            material = FastenerCatalog.MATERIALS.getValue("10B21"), coating = CoatingType.GEOMET, batchQty = 1000,
            propertyClass = PropertyClass.CLASS_10_9
        )
    }

    @Test fun everyNewType_calculatesInBothUnits_withAMatchingStandard() {
        newTypes.forEach { t ->
            UnitSystem.values().forEach { u ->
                val r = calc(t, u)
                val misfit = r.pokaYokeIssues.filter {
                    it.severity == PokaYokeSeverity.CRITICAL &&
                        (it.title.startsWith("Standard does not fit") || it.title.startsWith("Metric standard with inch"))
                }
                assertTrue("$t $u: ${misfit.map { it.title }}", misfit.isEmpty())
                assertTrue("$t $u volume", r.netWeightGrams > 0.0)
                r.headingPlan?.let { assertTrue("$t $u volume error ${it.maxVolumeError}", it.maxVolumeError < 1e-9) }
            }
        }
    }

    @Test fun nutTypes_getNutStandards_andNutHeadingPlans() {
        listOf(FastenerType.WHEEL_NUT, FastenerType.AERO_LOCK_NUT, FastenerType.HI_LOK_COLLAR, FastenerType.NUTPLATE).forEach {
            assertTrue(it.isNut)
            val plan = calc(it, UnitSystem.METRIC).headingPlan!!
            assertEquals("Pierce", plan.stations.last().title)
        }
        assertEquals(ProductStandard.DIN_74361_NUT, PokaYokeEngine.defaultStandard(FastenerType.WHEEL_NUT, UnitSystem.METRIC))
        assertEquals(ProductStandard.DIN_74361, PokaYokeEngine.defaultStandard(FastenerType.WHEEL_BOLT, UnitSystem.METRIC))
    }

    @Test fun typesWithoutATable_areLabelledApproximated_andWarned() {
        val d = DimensionTables.forPart(FastenerType.WHEEL_BOLT, UnitSystem.METRIC, 10.0)
        assertTrue(!d.isTable)
        assertTrue(d.source, d.source.startsWith("Approximated") && d.source.contains("DIN 74361"))
        assertTrue(calc(FastenerType.WHEEL_BOLT, UnitSystem.METRIC).pokaYokeIssues.any { it.title == "No dimension table for this type" })
    }

    @Test fun hexalobular_usesIso4762Head_andHasARecess() {
        val d = DimensionTables.forPart(FastenerType.HEXALOBULAR_SCREW, UnitSystem.METRIC, 10.0)
        assertTrue(d.isTable)
        assertTrue(d.source.startsWith("ISO 14579"))
        assertNotNull(DimensionTables.socketFor(FastenerType.HEXALOBULAR_SCREW, UnitSystem.METRIC, 10.0))
        val last = calc(FastenerType.HEXALOBULAR_SCREW, UnitSystem.METRIC).headingPlan!!.stations.last()
        assertTrue(last.operation, last.operation.contains("hexalobular"))
    }

    @Test fun threadFormingScrew_isFullyThreaded() {
        assertEquals(50.0, PokaYokeEngine.threadLengthB(FastenerType.THREAD_FORMING_SCREW, UnitSystem.METRIC, 10.0, 50.0), 0.0)
    }

    private fun rivet(type: FastenerType) = CalculationEngine.calculate(
        fastenerType = type, unitSystem = UnitSystem.METRIC,
        standard = PokaYokeEngine.defaultStandard(type, UnitSystem.METRIC),
        nominalSizeIndex = FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == 5.0 },
        pitchValue = 0.8, lengthMm = 12.0, threadClass = "6g",
        material = FastenerCatalog.MATERIALS.getValue("AL2117"), coating = CoatingType.GEOMET, batchQty = 1000,
        propertyClass = null
    )

    @Test fun solidRivet_hasNoThreadAnywhere() {
        val r = rivet(FastenerType.SOLID_RIVET)
        assertTrue(r.threadLabel, r.threadLabel.contains("rivet (no thread)"))
        assertEquals(0.0, r.threadLengthMm, 0.0)
        assertTrue(r.cncToolList.isEmpty())
        assertTrue(r.fanucGCode.contains("NO CNC PROGRAM"))
        assertEquals(1.0, r.stressConcentrationKt, 0.0)
        assertEquals(Math.PI / 4 * 25.0, r.stressAreaAsMm2, 0.01)
        assertEquals(0.0, r.costThreadRolling, 0.0)
        assertTrue(r.pokaYokeIssues.none { it.title.startsWith("Mate with nut class") })
        assertTrue(r.pokaYokeIssues.any { it.title == "Rivet joints are designed in shear" })
        val plan = r.headingPlan!!
        assertTrue(plan.offHeaderOps.isEmpty())
        assertEquals("Head upset (universal head)", plan.stations.last().title)
        assertEquals(com.example.data.SectionShape.DOME, plan.stations.last().segments.last().shape)
        assertTrue(plan.maxVolumeError < 1e-9)
        assertTrue(r.forgingStages.none { it.title.contains("Thread rolling") })
    }

    @Test fun blindRivet_saysWhatIsNotModelled() {
        val plan = rivet(FastenerType.BLIND_RIVET).headingPlan!!
        assertTrue(plan.notes.any { it.contains("sleeve bore and the mandrel") })
        assertEquals(ProductStandard.NAS1398, PokaYokeEngine.defaultStandard(FastenerType.BLIND_RIVET, UnitSystem.INCH))
        assertEquals(ProductStandard.MS20470, PokaYokeEngine.defaultStandard(FastenerType.SOLID_RIVET, UnitSystem.METRIC))
    }
}
