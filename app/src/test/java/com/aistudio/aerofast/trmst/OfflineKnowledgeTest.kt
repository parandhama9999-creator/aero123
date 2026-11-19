package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import com.aistudio.aerofast.trmst.util.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** Offline answers come only from the app's own calculations, tables, rules and the user's approved facts. */
class OfflineKnowledgeTest {
    private fun n(v: Double, dp: Int) = String.format(Locale.US, "%.${dp}f", v)
    private val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
    private val base = CalculationEngine.calculate(FastenerType.HEX_BOLT, UnitSystem.METRIC, ProductStandard.ISO_4014, idx, 1.5, 60.0, "6g",
        FastenerCatalog.MATERIALS.getValue("4135"), CoatingType.ZN_NI, 13000, propertyClass = PropertyClass.CLASS_10_9)
    private fun ask(q: String, lang: AppLanguage = AppLanguage.EN, facts: List<ApprovedFact> = emptyList()) = OfflineKnowledge.answer(q, base, lang, facts)

    @Test fun currentPartFromTheCalculation() {
        val a = ask("cut off ఎంత", AppLanguage.TE)
        assertTrue(a.found)
        assertTrue(a.text, a.text.contains("కట్ ఆఫ్ పొడవు") && a.text.contains(n(base.cutOffLengthMm, 2)))
        assertTrue(a.sources.contains("AeroFast calculation"))
        val w = ask("బరువు ఎంత", AppLanguage.TE)
        assertTrue(w.text, w.text.contains(n(base.netWeightGrams, 3)))
    }

    @Test fun anotherSizeIsCalculatedNotGuessed() {
        val a = ask("M12 x 80 hex bolt weight")
        val expected = OfflineKnowledge.recalc(base, JarvisParser.parse("M12 x 80 hex bolt"))
        assertEquals(12.0, expected.nominalDiaMm, 1e-9)
        assertEquals(80.0, expected.lengthMm, 1e-9)
        assertTrue(a.text, a.text.startsWith("For M12×1.75 - 6g × 80.0 mm"))
        assertTrue(a.text, a.text.contains(n(expected.netWeightGrams, 3)))
        // The Sizing selection itself is not changed by asking
        assertEquals(10.0, base.nominalDiaMm, 1e-9)
    }

    @Test fun cardsComeFromTheAppTables() {
        val m = FastenerCatalog.MATERIALS.getValue("4140")
        val d = ask("4140 density")
        assertTrue(d.text, d.text.contains("${m.density} g/cm³"))
        assertTrue(d.sources.first().startsWith("AeroFast material catalogue"))

        val c = ask("10.9 proof stress")
        assertTrue(c.text, c.text.contains("Rm ≥ 1040 MPa") && c.text.contains("Sp 830 MPa"))
        assertTrue(c.sources.contains("ISO 898-1 class values in the app"))

        val z = ask("zinc nickel thickness")
        assertTrue(z.text, z.text.contains("${n(CoatingType.ZN_NI.thicknessMinUm, 0)}–${n(CoatingType.ZN_NI.thicknessMaxUm, 0)} µm"))

        val p = ask("M12 pitch")
        assertTrue(p.text, p.text.contains(FastenerCatalog.METRIC_SIZES[FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 12.0)].pitches.joinToString(" / ")))

        val s = ask("which standard for socket head cap screw")
        assertTrue(s.text, s.text.contains(PokaYokeEngine.defaultStandard(FastenerType.SHCS, UnitSystem.METRIC).code))
    }

    @Test fun canIAnswersWithTheAppRules() {
        val a = ask("can I use zinc blue on 12.9")
        assertTrue(a.text, a.text.contains("Not allowed (critical)"))
        assertTrue(a.text, a.text.contains("Electroplating ≥ 1200 MPa fasteners"))
        assertEquals(listOf("AeroFast poka-yoke rules"), a.sources)
        val ok = ask("can I use geomet")
        assertFalse(ok.text, ok.text.contains("Not allowed"))
    }

    @Test fun statusAndProcessAreTheAppsOwn() {
        val s = ask("status")
        assertTrue(s.text, s.text.contains("Standard check:"))
        assertTrue(s.sources.contains("AeroFast poka-yoke rules"))
        val p = ask("process sequence")
        assertTrue(p.text, p.text.contains("10. Wire") && p.text.contains("Plate"))
    }

    @Test fun unknownIsDeclinedInEveryLanguage() {
        val a = ask("what is the meaning of life")
        assertFalse(a.found)
        assertEquals(OfflineKnowledge.notFound(AppLanguage.EN), a.text)
        assertFalse(ask("నీ పేరు ఏమిటి", AppLanguage.TE).found)
        for (l in AppLanguage.values()) {
            assertTrue(OfflineKnowledge.notFound(l).isNotBlank())
            assertTrue(OfflineKnowledge.help(l).contains("📚"))
            assertTrue(OfflineKnowledge.sourceLabel(l).isNotBlank())
        }
    }

    @Test fun approvedFactsAnswerFirstWithTheirSource() {
        val fact = ApprovedFact(1L, "M10 bolt tightening torque", "45 Nm (customer spec)", "Customer spec CS-12 rev B", 0L)
        assertEquals(fact, ApprovedFacts.bestMatch("what is the M10 bolt tightening torque?", listOf(fact)))
        assertNull(ApprovedFacts.bestMatch("M12 nut price", listOf(fact)))
        val a = ask("M10 bolt tightening torque", facts = listOf(fact))
        assertEquals("45 Nm (customer spec)", a.text)
        assertTrue(a.sources.single().endsWith("Customer spec CS-12 rev B"))
        assertTrue(a.withSources(AppLanguage.TE).contains("మూలం:"))
    }

    @Test fun namingAPartGivesItsCutOffAndWeight() {
        val a = ask("M8 x 30 hex bolt")
        val r = OfflineKnowledge.recalc(base, JarvisParser.parse("M8 x 30 hex bolt"))
        assertTrue(a.text, a.text.contains(n(r.cutOffLengthMm, 2)) && a.text.contains(n(r.netWeightGrams, 3)))
    }

    @Test fun newStandardsMaterialsAndProductsAreDeclinedHonestly() {
        // NAS 5317 and AMS 6304 are not in the app: no value is invented and the current part is not used
        val a = ask("NAS5317 hardness")
        assertFalse(a.found)
        assertTrue(a.text, a.text.contains("NAS5317") && a.text.contains("not in the app's data"))
        assertFalse(a.text, a.text.contains(base.heatTreatmentSpec.targetHardnessHrc))
        assertTrue(a.text, a.text.contains("Closest in the app"))     // the NAS standards it does hold
        assertFalse(ask("AMS 6304 density", AppLanguage.TE).found)
        assertTrue(ask("SAE 1045 tensile strength").text.contains("SAE 1045"))
        // What the app holds is still answered
        assertEquals(emptyList<String>(), OfflineKnowledge.unknownCodes("ISO 898-1 class 10.9 hardness"))
        assertEquals(emptyList<String>(), OfflineKnowledge.unknownCodes("M12 pitch ISO 261"))
        assertEquals(emptyList<String>(), OfflineKnowledge.unknownCodes("4340 to AMS 6415"))
        // Ordinary words are not read as standards
        assertEquals(emptyList<String>(), OfflineKnowledge.unknownCodes("what is 10.9 hardness for an 80 mm bolt"))
        for (l in AppLanguage.values()) assertFalse(ask("NAS5317 weight", l).found)
    }
}

