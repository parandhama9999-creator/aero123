package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Open extrusion up to the shop limit (default 30 %), trapped die above it, ~70 % trapped ceiling. */
class ExtrusionTypeTest {
    private val m10 = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
    private fun shcs(wire: Double? = null, limit: Double = HeadingProgression.MAX_DIE_EXTRUSION_PCT) = CalculationEngine.calculate(
        FastenerType.SHCS, UnitSystem.METRIC, ProductStandard.ISO_4762, m10, 1.5, 50.0, "6g",
        FastenerCatalog.MATERIALS.getValue("4140"), CoatingType.ZN_NI, 10000, propertyClass = PropertyClass.CLASS_12_9,
        wireDiaOverrideMm = wire, openExtrusionLimitPct = limit)
    private fun extrusionItem(r: FastenerCalculationResult, m: MachineSetup = MachineSetup()) =
        ToolingDesign.build(r, m)!!.stations.flatMap { it.items }.single { it.name.startsWith("Extrusion") && !it.name.startsWith("Extrusion entry") }

    @Test fun m10SocketScrewIsAnOpenExtrusion() {
        val r = shcs()
        val ex = ToolingDesign.extrusions(r.headingPlan!!).single()
        assertEquals(1, ex.stationIndex)
        // Ø9.85 → Ø8.937: 1 − (8.937/9.85)² ≈ 17.7 %
        assertEquals((1 - Math.pow(ex.toMm / 9.85, 2.0)) * 100.0, ex.reductionPct, 1e-9)
        assertTrue(ex.reductionPct in 17.0..18.5)
        assertEquals("Extrusion: open", extrusionItem(r).name)
        // Only the extruding station carries the entry-angle row
        assertEquals(1, ToolingDesign.build(r)!!.stations.count { st -> st.items.any { it.name.startsWith("Extrusion entry") } })
    }

    @Test fun aStricterShopLimitTurnsItTrapped() {
        val r = shcs(limit = 15.0)
        val item = extrusionItem(r, MachineSetup(openExtrusionLimitPct = 15.0))
        assertEquals("Extrusion: TRAPPED die required", item.name)
        assertTrue(item.value, item.value.contains("over your 15 % open-extrusion limit"))
        assertTrue(r.headingPlan!!.notes.any { it.contains("over your 15 % open-extrusion limit") })
    }

    @Test fun thickWireNeedsATrappedDieAndIsWarned() {
        val r = shcs(13.0)
        assertEquals("Extrusion: TRAPPED die required", extrusionItem(r).name)
        assertTrue(r.pokaYokeIssues.any { it.title == "Large extrusion from this wire" })
    }

    @Test fun overSeventyPercentIsCritical() {
        val r = shcs(20.0)
        assertTrue(ToolingDesign.extrusions(r.headingPlan!!).single().reductionPct > 70.0)
        assertEquals(PokaYokeSeverity.CRITICAL, r.pokaYokeIssues.single { it.title == "Extrusion over the trapped limit" }.severity)
        assertTrue(ToolingDesign.build(r)!!.problems.any { it.contains("trapped limit") })
    }
}
