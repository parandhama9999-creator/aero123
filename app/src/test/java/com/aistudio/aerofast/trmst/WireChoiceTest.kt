package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The wire the user chooses drives L/D, the number of gathering blows, the stations and the tools. */
class WireChoiceTest {
    private val m10 = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
    private fun shcs(wire: Double? = null, choice: HeaderChoice = HeaderChoice.AUTO) = CalculationEngine.calculate(
        FastenerType.SHCS, UnitSystem.METRIC, ProductStandard.ISO_4762, m10, 1.5, 50.0, "6g",
        FastenerCatalog.MATERIALS.getValue("4140"), CoatingType.ZN_NI, 10000, propertyClass = PropertyClass.CLASS_12_9,
        wireDiaOverrideMm = wire, headerChoice = choice)
    private fun coneStations(r: FastenerCalculationResult) = r.headingPlan!!.stations.count { it.title.contains("Cone", true) }

    @Test fun autoIsUnchanged() {
        val r = shcs()
        assertNull(r.wireDiaOverrideMm)
        assertEquals(9.85, r.wireDiaMm, 1e-9)
        assertEquals(r.wireDiaMm, r.autoWireDiaMm, 1e-9)
    }

    @Test fun thickerWireLowersLdAndDropsTheConeBlow() {
        val auto = shcs(); val thick = shcs(11.0)
        assertEquals(11.0, thick.wireDiaMm, 1e-9)
        assertEquals(9.85, thick.autoWireDiaMm, 1e-9)
        assertTrue(thick.upsetLdRatio < HeadingProgression.SINGLE_BLOW_LD)
        assertEquals(1, coneStations(auto))
        assertEquals(0, coneStations(thick))
        assertTrue(thick.headingPlan!!.dieStations < auto.headingPlan!!.dieStations)
        // Cut-off follows the wire: same slug volume on a larger area
        assertTrue(thick.cutOffLengthMm < auto.cutOffLengthMm)
        // Tooling follows: no sliding cone punch any more
        assertTrue(ToolingDesign.build(thick)!!.stations.none { st -> st.items.any { it.name == "Cone punch type" } })
    }

    @Test fun thinnerWireRaisesLdAddsBlowsAndIsFlagged() {
        val thin = shcs(7.5) // L/D ≈ 5.2
        assertTrue(thin.upsetLdRatio > HeadingProgression.TWO_BLOW_LD)
        assertTrue(coneStations(thin) >= 2)
        assertTrue(thin.pokaYokeIssues.any { it.title == "Wire under the shank" })
        assertTrue(thin.pokaYokeIssues.any { it.title == "Wire under the rolling blank" })
    }

    @Test fun farTooThinIsCritical() {
        val r = shcs(6.0)
        assertTrue(r.upsetLdRatio > HeadingProgression.THREE_BLOW_LD)
        assertEquals(PokaYokeSeverity.CRITICAL, r.pokaYokeIssues.single { it.title == "Wire too thin to gather the head" }.severity)
    }

    @Test fun twoBlowKeepsTheChosenWireAndSaysWhenItCannot() {
        val r = shcs(7.5, HeaderChoice.TWO_BLOW)
        assertEquals(7.5, r.headingPlan!!.slugDiaMm, 1e-9)
        assertTrue(r.pokaYokeIssues.any { it.title == "1-die 2-blow cannot make this part" })
    }

    @Test fun whatIfOnAnotherSizeDropsTheChosenWire() {
        val r = shcs(11.0)
        assertNull(OfflineKnowledge.recalc(r, QuestionParser.parse("M12 x 60")).wireDiaOverrideMm)
        assertEquals(11.0, OfflineKnowledge.recalc(r, QuestionParser.parse("10B21")).wireDiaOverrideMm!!, 1e-9)
    }
}
