package com.example

import com.example.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A sub-critical spheroidize anneal has to sit under A1, and A1 is not a constant: nickel and
 * manganese depress it, silicon and chromium raise it. One band for every grade either austenitises
 * the nickel steels or leaves the silicon ones 60 °C colder than they need to be.
 */
class AnnealTemperatureTest {

    private fun mat(k: String) = FastenerCatalog.MATERIALS[k]!!
    private fun cycle(k: String) = AnnealCycleEngine.spheroidize(mat(k))
    private fun soakHours(k: String) = cycle(k)!!.stages.first { it.name.contains("Soak") }

    // --- A1 itself -----------------------------------------------------------------------------

    @Test fun andrewsIsRefusedWhereItWouldBeExtrapolated() {
        // 18.5 % nickel is far outside the low-alloy fit: the raw formula returns about 412 °C
        val maraging = SteelCompositions.forMaterial(mat("MARAGING_250"))!!
        assertTrue("the raw formula does go wrong here", SteelCompositions.ac1(maraging) < 500)
        assertTrue(!SteelCompositions.andrewsApplies(maraging))
        assertNull("so no A1 is offered", SteelCompositions.ac1For(mat("MARAGING_250")))
        // 5 % chromium tool steel is still inside the range
        assertTrue(SteelCompositions.andrewsApplies(SteelCompositions.forMaterial(mat("H11_H13"))!!))
    }

    @Test fun andrewsReproducesTheTextbookValues() {
        // Ac1 = 723 − 10.7Mn − 16.9Ni + 29.1Si + 16.9Cr
        assertEquals(723.0, SteelCompositions.ac1(SteelComposition(0.2, 0.0, 0.0, 0.0, 0.0)), 1e-9)
        assertEquals(723.0 - 10.7, SteelCompositions.ac1(SteelComposition(0.2, 1.0, 0.0, 0.0, 0.0)), 1e-9)
        assertEquals(723.0 - 16.9, SteelCompositions.ac1(SteelComposition(0.2, 0.0, 0.0, 0.0, 1.0)), 1e-9)
        assertEquals(723.0 + 29.1, SteelCompositions.ac1(SteelComposition(0.2, 0.0, 1.0, 0.0, 0.0)), 1e-9)
        assertEquals(723.0 + 16.9, SteelCompositions.ac1(SteelComposition(0.2, 0.0, 0.0, 1.0, 0.0)), 1e-9)
    }

    @Test fun nickelPullsA1DownAndSiliconPushesItUp() {
        val ni = SteelCompositions.ac1For(mat("35NCD16"))!!   // 3.85 % Ni
        val si = SteelCompositions.ac1For(mat("30KHGSA"))!!   // 1.05 % Si
        val plain = SteelCompositions.ac1For(mat("1018"))!!
        assertTrue("4 % nickel must drop A1 well below plain carbon", ni < plain - 20)
        assertTrue("1 % silicon must lift A1 well above plain carbon", si > plain + 25)
        assertEquals("35NCD16", 691.0, ni, 1.5)
        assertEquals("30KHGSA", 759.0, si, 1.5)
    }

    @Test fun theCatalogueSpansMoreThanAHundredDegreesOfA1() {
        val all = FastenerCatalog.MATERIALS.values.mapNotNull { SteelCompositions.ac1For(it) }
        assertTrue("needs a real sample", all.size >= 30)
        val lo = all.min()
        val hi = all.max()
        assertTrue("A1 spans $lo to $hi — one band cannot serve them all", hi - lo > 100)
        assertEquals("the 4 % nickel steel is the floor", 691.0, lo, 1.5)
        assertEquals("the 5 % Cr tool steel is the ceiling", 835.0, hi, 1.5)
    }

    // --- the band that follows from it -----------------------------------------------------------

    @Test fun everySoakBandSitsUnderThatGradesOwnA1() {
        var checked = 0
        FastenerCatalog.MATERIALS.values.forEach { m ->
            val c = AnnealCycleEngine.spheroidize(m) ?: return@forEach
            val ac1 = SteelCompositions.ac1For(m) ?: return@forEach
            checked++
            val top = Regex("""(\d+)\s*–\s*(\d+)""").find(c.soakTempC)!!.groupValues[2].toDouble()
            assertTrue("${m.key}: soak top $top must stay under A1 $ac1", top < ac1)
        }
        assertTrue("the sweep has to actually cover grades", checked >= 30)
    }

    @Test fun theNickelSteelNoLongerGetsABandAboveItsA1() {
        // The old fixed 698 – 718 °C band sat entirely above 35NCD16's A1 of 691 °C
        val c = cycle("35NCD16")!!
        val top = Regex("""(\d+)\s*–\s*(\d+)""").find(c.soakTempC)!!.groupValues[2].toDouble()
        assertTrue("$top must be under 691", top < 691.0)
        assertTrue("and the old band must be gone", !c.soakTempC.contains("718"))
    }

    @Test fun theSiliconSteelsRunHotterThanPlainCarbon() {
        fun top(k: String) = Regex("""(\d+)\s*–\s*(\d+)""").find(cycle(k)!!.soakTempC)!!.groupValues[2].toDouble()
        assertTrue("30KHGSA", top("30KHGSA") > top("1018") + 25)
        assertTrue("300M carries 1.6 % Si", top("300M") > top("1018") + 10)
        assertTrue("H11/H13 carries 5 % Cr", top("H11_H13") > 800)
    }

    // --- grades that must not be on this route at all ---------------------------------------------

    @Test fun duplexIsNotOfferedASigmaFormingHold() {
        // 600 – 950 °C is the sigma range: a 700 °C hold embrittles a duplex instead of softening it
        for (k in listOf("DUPLEX_2205", "SUPER_DUPLEX_2507")) {
            assertEquals("$k is stainless, not carbon steel",
                MaterialFamily.STAINLESS, PokaYokeEngine.materialFamily(mat(k)))
            assertNull("$k must get no spheroidize cycle", cycle(k))
        }
    }

    @Test fun nickelAlloysAreNotOfferedASpheroidizeCycle() {
        for (k in listOf("INCOLOY_825", "A453_660")) {
            assertEquals(k, MaterialFamily.NICKEL_SUPERALLOY, PokaYokeEngine.materialFamily(mat(k)))
            assertNull(k, cycle(k))
        }
    }

    @Test fun maragingIsSolutionAnnealedBecauseItHasNoCarbonToBall() {
        assertNull("no spheroidize cycle", cycle("MARAGING_250"))
        assertTrue("0.02 % C is under the threshold", !SteelCompositions.canSpheroidise(mat("MARAGING_250")))
        val spec = SoftAnnealEngine.build(mat("MARAGING_250"))
        assertEquals("it must still be told what to do",
            AnnealRoute.SOLUTION_ANNEAL, spec.route)
    }

    @Test fun ordinaryCarbonSteelKeepsItsCycle() {
        // The carbon gate is there for maraging; a 1008 at 0.08 % still has cementite to ball up
        assertNotNull(cycle("1008"))
        assertNotNull(cycle("10B21"))
        assertNotNull(cycle("4140"))
    }

    // --- soak length ------------------------------------------------------------------------------

    @Test fun soakLengthFollowsCarbideFormersNotTheGradesSpelling() {
        // These four carry Cr or Mo but no "CrMo" in their names, so the old string match put them
        // in the plain-low-carbon bucket
        for (k in listOf("300M", "H11_H13", "30KHGSA", "16KHGSN")) {
            assertEquals("$k is an alloy steel and must soak the full time",
                10.0, soakHours(k).hoursHigh, 1e-9)
        }
        assertTrue("plain carbon still soaks shortest",
            soakHours("1008").hoursHigh < soakHours("10B21").hoursHigh)
        assertTrue(soakHours("10B21").hoursHigh < soakHours("4140").hoursHigh)
    }

    @Test fun theCardNamesTheA1ItUsedAndWhereItCameFrom() {
        val spec = SoftAnnealEngine.build(mat("35NCD16"))
        assertTrue("the card must state A1", spec.notes.any { it.contains("A1 = ") })
        assertTrue("and that it came from composition", spec.notes.any { it.contains("Andrews") })
        assertTrue("and still send the user to the certificate",
            spec.notes.any { it.contains("mill certificate") })
    }
}
