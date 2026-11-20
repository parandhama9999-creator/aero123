package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Header tooling comes from the heading stations and the machine sizes the user typed; nothing else is invented. */
class ToolingDesignTest {
    private val m10 = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
    private val shcs = CalculationEngine.calculate(FastenerType.SHCS, UnitSystem.METRIC, ProductStandard.ISO_4762, m10, 1.5, 50.0, "6g",
        FastenerCatalog.MATERIALS.getValue("4140"), CoatingType.ZN_NI, 10000, propertyClass = PropertyClass.CLASS_12_9)

    @Test fun everyDieStationGetsBoreAndCavityFromItsOwnGeometry() {
        val sheet = ToolingDesign.build(shcs)!!
        val plan = shcs.headingPlan!!
        assertEquals(plan.dieStations, sheet.stations.size)
        sheet.stations.forEach { st ->
            // Splitting at the die face keeps the part whole
            assertEquals(st.station.heightMm, st.inDie.sumOf { it.heightMm } + st.aboveDie.sumOf { it.heightMm }, 1e-6)
            assertEquals(st.station.segments.sumOf { it.volumeMm3 }, st.inDie.sumOf { it.volumeMm3 } + st.aboveDie.sumOf { it.volumeMm3 }, 1e-3)
            if (st.inDie.isNotEmpty()) {
                val depth = st.items.single { it.name == "Die bore depth" }
                assertTrue(depth.value.startsWith(String.format(java.util.Locale.US, "%.1f", st.station.inDieMm)))
                assertEquals(ToolBasis.CALCULATED, depth.basis)
                // The bore includes the app's transfer clearance
                val d = st.inDie.first().maxWidthMm
                val bore = String.format(java.util.Locale.US, "%.2f", d + HeadingProgression.stageClearance(d))
                assertTrue(st.items.single { it.name.startsWith("Die bore (") }.value.contains("Ø$bore"))
            }
        }
    }

    @Test fun theSocketIsAPunchPinWithItsSizeAndDepth() {
        val last = ToolingDesign.build(shcs)!!.stations.last()
        val rc = last.station.recesses.single { it.fromTop }
        val pin = last.items.single { it.name == "Punch pin (recess)" }
        assertTrue(pin.value, pin.value.startsWith("hex ${String.format(java.util.Locale.US, "%.2f", rc.sizeMm)}"))
    }

    @Test fun shrinkFitAndInsertOdAreLeftToTheToolmaker() {
        val sheet = ToolingDesign.build(shcs)!!
        assertTrue(sheet.notDecided.first().contains("no published rule"))
        assertTrue(sheet.stations.flatMap { it.items }.none { it.name.contains("interference", ignoreCase = true) })
        assertEquals(ToolBasis.TOOLMAKER, sheet.cutOff.single { it.name.startsWith("Cut-off quill") }.basis)
    }

    @Test fun machineSizesAreCheckedOnlyWhenEntered() {
        assertTrue(ToolingDesign.build(shcs)!!.problems.isEmpty())
        val tight = ToolingDesign.build(shcs, MachineSetup(dieCaseOdMm = 30.0, dieCaseLengthMm = 20.0, koStrokeMm = 10.0, insertOdMm = 40.0))!!
        assertTrue(tight.problems.toString(), tight.problems.any { it.contains("does not fit the 20.0 mm die case") })
        assertTrue(tight.problems.any { it.contains("knock-out stroke 10.0 mm") })
        assertTrue(tight.problems.any { it.contains("die case Ø30.00 has no room") })
        val roomy = ToolingDesign.build(shcs, MachineSetup(dieCaseOdMm = 90.0, dieCaseLengthMm = 120.0, punchCaseOdMm = 60.0, punchCaseLengthMm = 90.0, koStrokeMm = 80.0, insertOdMm = 40.0))!!
        assertTrue(roomy.problems.toString(), roomy.problems.isEmpty())
    }

    @Test fun sharedSheetCarriesEverySource() {
        val m = MachineSetup(dieCaseOdMm = 90.0)
        val t = ToolingDesign.text(shcs, ToolingDesign.build(shcs, m)!!, m)
        listOf("[calculated]", "[your machine]", "[catalogue]", "[toolmaker decides]", "NOT SET BY THE APP").forEach { assertTrue(it, t.contains(it)) }
    }
}
