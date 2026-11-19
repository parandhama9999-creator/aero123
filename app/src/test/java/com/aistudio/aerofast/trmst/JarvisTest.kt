package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import com.aistudio.aerofast.trmst.util.AppLanguage
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Jarvis reads spoken commands in the app's languages and answers from the app's own figures. */
class JarvisTest {
    private fun p(s: String, unit: UnitSystem = UnitSystem.METRIC) = JarvisParser.parse(s, unit)

    private fun assertM10x60Bolt(i: JarvisIntent) {
        assertEquals(FastenerType.HEX_BOLT, i.type)
        assertEquals(UnitSystem.METRIC, i.unit)
        assertEquals(10.0, i.diaMm!!, 1e-9)
        assertEquals(60.0, i.lengthMm!!, 1e-9)
        assertEquals("4135", i.materialKey)
        assertEquals(PropertyClass.CLASS_10_9, i.propertyClass)
        assertEquals(CoatingType.ZN_NI, i.coating)
        assertEquals(13000, i.batchQty)
    }

    @Test fun english() = assertM10x60Bolt(p("Jarvis, M10 by 60 hex bolt, 4135, 10.9, zinc nickel, 13,000 pieces"))

    @Test fun telugu() = assertM10x60Bolt(p("జార్విస్ M10 బై 60 హెక్స్ బోల్ట్ 4135 10.9 జింక్ నికెల్ 13000 పీసులు"))

    @Test fun hindiWithQuestion() {
        val i = p("M12 बाय 80 बोल्ट 10B21 8.8 5000 पीस कट ऑफ कितना")
        assertEquals(FastenerType.HEX_BOLT, i.type)
        assertEquals(12.0, i.diaMm!!, 1e-9)
        assertEquals(80.0, i.lengthMm!!, 1e-9)
        assertEquals("10B21", i.materialKey)
        assertEquals(PropertyClass.CLASS_8_8, i.propertyClass)
        assertEquals(5000, i.batchQty)
        assertTrue(JarvisQuery.CUT_OFF in i.queries)
    }

    @Test fun spanishGermanChineseJapanese() {
        val es = p("M10 por 60 perno 13000 piezas peso")
        assertEquals(FastenerType.HEX_BOLT, es.type); assertEquals(60.0, es.lengthMm!!, 1e-9); assertEquals(13000, es.batchQty); assertTrue(JarvisQuery.WEIGHT in es.queries)
        val de = p("M8 mal 40 Sechskantschraube 2000 Stück Gewicht")
        assertEquals(FastenerType.HEX_BOLT, de.type); assertEquals(8.0, de.diaMm!!, 1e-9); assertEquals(40.0, de.lengthMm!!, 1e-9); assertTrue(JarvisQuery.WEIGHT in de.queries)
        val zh = p("M10乘60螺栓 13000件 重量")
        assertEquals(FastenerType.HEX_BOLT, zh.type); assertEquals(60.0, zh.lengthMm!!, 1e-9); assertEquals(13000, zh.batchQty); assertTrue(JarvisQuery.WEIGHT in zh.queries)
        val ja = p("M6 かける 20 ボルト 切断長")
        assertEquals(6.0, ja.diaMm!!, 1e-9); assertEquals(20.0, ja.lengthMm!!, 1e-9); assertTrue(JarvisQuery.CUT_OFF in ja.queries)
    }

    @Test fun inchAerospaceScrew() {
        val i = p("#8-32 pan head torx 4340 1000-1240 MPa cadmium class 2 with patch 13000 pieces")
        assertEquals(FastenerType.PAN_HEXALOBULAR, i.type)
        assertEquals(UnitSystem.INCH, i.unit)
        assertEquals(0.164 * 25.4, i.diaMm!!, 1e-6)
        assertEquals(32.0, i.pitch!!, 1e-9)
        assertEquals("4340", i.materialKey)
        assertEquals(TensileRange(1000.0, 1240.0), i.tensileRange)
        assertEquals(CoatingType.AERO_CAD_CL2, i.coating)
        assertEquals(true, i.threadLockPatch)
        assertEquals(13000, i.batchQty)
    }

    @Test fun machineChoiceAndActions() {
        assertEquals(HeaderChoice.TWO_BLOW, p("use 1 die 2 blow").headerChoice)
        assertEquals(HeaderChoice.TWO_BLOW, p("వన్ డై టూ బ్లో పెట్టు").headerChoice)
        assertEquals(HeaderChoice.MULTI_STATION, p("multi station machine").headerChoice)
        assertEquals(JarvisAction.SHARE_REPORT, p("రిపోర్ట్ పంపు").action)
        assertTrue(JarvisQuery.WEIGHT in p("బరువు ఎంత").queries)
        assertTrue(JarvisQuery.CUT_OFF in p("cut off ఎంత").queries)
        assertEquals(false, p("no patch").threadLockPatch)
    }

    @Test fun generalQuestionIsLeftForTheCoPilot() {
        assertTrue(p("what is hydrogen embrittlement").isEmpty)
        // whole words only: "minute" has no nut, "profile" no wire
        assertNull(p("wait a minute").type)
        assertTrue(JarvisQuery.WEIGHT !in p("le profil du filetage").queries)
    }

    @Test fun geminiJsonMapsToTheSameIntent() {
        val i = JarvisParser.fromJson(JSONObject("""{"fastenerType":"HEX_BOLT","unitSystem":"METRIC","nominalDiameterMm":10,"lengthMm":60,"materialKey":"4135",
            "propertyClass":"10.9","coating":"ZN_NI","batchQty":13000,"headerChoice":"TWO_BLOW","threadLockPatch":null,"queries":["CUT_OFF"],"action":""}"""))
        assertM10x60Bolt(i)
        assertEquals(HeaderChoice.TWO_BLOW, i.headerChoice)
        assertNull(i.threadLockPatch)
        assertEquals(setOf(JarvisQuery.CUT_OFF), i.queries)
    }

    @Test fun repliesInEveryLanguageCarryTheNumbers() {
        val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
        val r = CalculationEngine.calculate(FastenerType.HEX_BOLT, UnitSystem.METRIC, ProductStandard.ISO_4014, idx, 1.5, 60.0, "6g",
            FastenerCatalog.MATERIALS.getValue("4135"), CoatingType.ZN_NI, 13000, propertyClass = PropertyClass.CLASS_10_9)
        val cut = String.format(java.util.Locale.US, "%.1f", r.cutOffLengthMm)
        for (lang in AppLanguage.values()) {
            val reply = JarvisReply.build(p("M10 by 60 hex bolt 13000 pieces"), r, lang)
            assertTrue("$lang: $reply", cut in reply)
            assertTrue("$lang: $reply", "13000" in reply)
            assertFalse(JarvisReply.notUnderstood(lang).isBlank())
            assertFalse(JarvisReply.localeTag(lang).isBlank())
        }
        assertTrue(JarvisReply.build(JarvisIntent(queries = setOf(JarvisQuery.STATUS)), r, AppLanguage.TE).contains("స్టాండర్డ్ చెక్"))
        // What was not said is reported as unchanged, not as set
        val en = JarvisReply.build(p("M10 by 60 hex bolt"), r, AppLanguage.EN)
        assertTrue(en, en.contains("Set: M10×1.5 - 6g, L 60.0, Hex Bolt - Partial Thread."))
        assertTrue(en, en.contains("Unchanged: 4135, Class 10.9, Zn-Ni 1000h, 13000 pieces."))
        val te = JarvisReply.build(p("బరువు ఎంత"), r, AppLanguage.TE)
        assertFalse(te, te.contains("సెట్ చేశాను"))
        assertEquals("Cut off 79 mm", JarvisReply.forSpeech("**Cut off** • 79 mm"))
    }

    @Test fun bareNumberAfterSizeIsTheLength() {
        val i = p("M8 25")
        assertEquals(8.0, i.diaMm!!, 1e-9)
        assertEquals(25.0, i.lengthMm!!, 1e-9)
        val t = p("ఎం 8 25 బోల్ట్")
        assertEquals(8.0, t.diaMm!!, 1e-9); assertEquals(25.0, t.lengthMm!!, 1e-9)
        // material key and batch are not taken as length
        val k = p("M10 4135 13000 pieces")
        assertNull(k.lengthMm); assertEquals("4135", k.materialKey); assertEquals(13000, k.batchQty)
    }
}
