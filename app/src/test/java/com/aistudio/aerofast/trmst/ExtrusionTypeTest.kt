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
        ToolingDesign.build(r, m)!!.stations.flatMap { it.items }.single { it.name == "Extrusion" || it.name.startsWith("Extrusion:") }

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

class ExtrusionDieTest {
    private val m10 = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
    private fun calc(mat: String, wire: Double? = null) = CalculationEngine.calculate(
        FastenerType.SHCS, UnitSystem.METRIC, ProductStandard.ISO_4762, m10, 1.5, 50.0, "6g",
        FastenerCatalog.MATERIALS.getValue(mat), CoatingType.ZN_NI, 10000, propertyClass = PropertyClass.CLASS_12_9, wireDiaOverrideMm = wire)
    private fun items(r: FastenerCalculationResult, m: MachineSetup = MachineSetup()) =
        ToolingDesign.build(r, m)!!.stations.flatMap { it.items }

    @Test fun coneLengthFollowsTheAngle() {
        // (10 − 8) / 2 / tan(15°) = 3.732 mm
        assertEquals(3.732, ExtrusionDie.coneLengthMm(10.0, 8.0, 30.0), 1e-3)
    }

    @Test fun pressureIsJohnsonOnTheMaterialFlowCurve() {
        val m = FastenerCatalog.MATERIALS.getValue("4140")
        val lnR = Math.log(Math.pow(10.0 / 8.0, 2.0))
        val s = m.flowStressCoeffK * Math.pow(lnR, m.strainHardeningN) / (m.strainHardeningN + 1)
        val (lo, hi) = ExtrusionDie.pressureMpa(m, 10.0, 8.0)
        assertEquals(s * (0.8 + 1.2 * lnR), lo, 1e-6)
        assertEquals(s * (0.8 + 1.5 * lnR), hi, 1e-6)
        // Harder grade → higher pressure for the same reduction
        assertTrue(ExtrusionDie.pressureMpa(FastenerCatalog.MATERIALS.getValue("10B21"), 10.0, 8.0).second < hi)
    }

    @Test fun trappedDieGetsContainerTrapLandAndPublishedRanges() {
        val all = items(calc("4140", 13.0))
        assertTrue(all.any { it.name == "Container bore (trap)" && it.basis == ToolBasis.CALCULATED })
        assertTrue(all.any { it.name == "Trap length" })
        assertTrue(all.single { it.name == "Die opening angle 2α" }.basis == ToolBasis.PUBLISHED)
        assertTrue(all.single { it.name == "Land length" }.value.startsWith("1.5 – 4.5 mm"))
        assertEquals(ToolBasis.ESTIMATE, all.single { it.name == "Extrusion pressure (estimate)" }.basis)
    }

    @Test fun shopSettingsReplaceTheRanges() {
        val all = items(calc("4140", 13.0), MachineSetup(dieAngleDeg = 30.0, landLengthMm = 2.0))
        assertTrue(all.single { it.name == "Reduction cone length" }.value.endsWith("at 2α = 30°"))
        assertEquals("2.0 mm (your setting)", all.single { it.name == "Land length" }.value)
        val odd = ToolingDesign.build(calc("4140", 13.0), MachineSetup(dieAngleDeg = 100.0))!!
        assertTrue(odd.problems.any { it.contains("outside the 24° – 70° guideline") })
    }

    @Test fun gradeLimitIsShownWhereOneIsPublished() {
        assertTrue(items(calc("1018", 13.0)).single { it.name.startsWith("Reduction limit") }.value.contains("≈ 5.0 for 1018"))
        assertTrue(items(calc("4140", 13.0)).single { it.name.startsWith("Reduction limit") }.value.startsWith("No grade-specific limit"))
    }
}
