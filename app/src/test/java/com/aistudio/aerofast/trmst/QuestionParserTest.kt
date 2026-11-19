package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The offline question reader understands typed questions in the app's languages. */
class QuestionParserTest {
    private fun p(s: String, unit: UnitSystem = UnitSystem.METRIC) = QuestionParser.parse(s, unit)

    private fun assertM10x60Bolt(i: ParsedQuestion) {
        assertEquals(FastenerType.HEX_BOLT, i.type)
        assertEquals(UnitSystem.METRIC, i.unit)
        assertEquals(10.0, i.diaMm!!, 1e-9)
        assertEquals(60.0, i.lengthMm!!, 1e-9)
        assertEquals("4135", i.materialKey)
        assertEquals(PropertyClass.CLASS_10_9, i.propertyClass)
        assertEquals(CoatingType.ZN_NI, i.coating)
        assertEquals(13000, i.batchQty)
    }

    @Test fun english() = assertM10x60Bolt(p("M10 by 60 hex bolt, 4135, 10.9, zinc nickel, 13,000 pieces"))

    @Test fun telugu() = assertM10x60Bolt(p("M10 బై 60 హెక్స్ బోల్ట్ 4135 10.9 జింక్ నికెల్ 13000 పీసులు"))

    @Test fun hindiWithQuestion() {
        val i = p("M12 बाय 80 बोल्ट 10B21 8.8 5000 पीस कट ऑफ कितना")
        assertEquals(FastenerType.HEX_BOLT, i.type)
        assertEquals(12.0, i.diaMm!!, 1e-9)
        assertEquals(80.0, i.lengthMm!!, 1e-9)
        assertEquals("10B21", i.materialKey)
        assertEquals(PropertyClass.CLASS_8_8, i.propertyClass)
        assertEquals(5000, i.batchQty)
        assertTrue(QuestionTopic.CUT_OFF in i.queries)
    }

    @Test fun spanishGermanChineseJapanese() {
        val es = p("M10 por 60 perno 13000 piezas peso")
        assertEquals(FastenerType.HEX_BOLT, es.type); assertEquals(60.0, es.lengthMm!!, 1e-9); assertEquals(13000, es.batchQty); assertTrue(QuestionTopic.WEIGHT in es.queries)
        val de = p("M8 mal 40 Sechskantschraube 2000 Stück Gewicht")
        assertEquals(FastenerType.HEX_BOLT, de.type); assertEquals(8.0, de.diaMm!!, 1e-9); assertEquals(40.0, de.lengthMm!!, 1e-9); assertTrue(QuestionTopic.WEIGHT in de.queries)
        val zh = p("M10乘60螺栓 13000件 重量")
        assertEquals(FastenerType.HEX_BOLT, zh.type); assertEquals(60.0, zh.lengthMm!!, 1e-9); assertEquals(13000, zh.batchQty); assertTrue(QuestionTopic.WEIGHT in zh.queries)
        val ja = p("M6 かける 20 ボルト 切断長")
        assertEquals(6.0, ja.diaMm!!, 1e-9); assertEquals(20.0, ja.lengthMm!!, 1e-9); assertTrue(QuestionTopic.CUT_OFF in ja.queries)
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

    @Test fun machineChoiceAndQuestions() {
        assertEquals(HeaderChoice.TWO_BLOW, p("use 1 die 2 blow").headerChoice)
        assertEquals(HeaderChoice.TWO_BLOW, p("వన్ డై టూ బ్లో పెట్టు").headerChoice)
        assertEquals(HeaderChoice.MULTI_STATION, p("multi station machine").headerChoice)
        assertTrue(QuestionTopic.WEIGHT in p("బరువు ఎంత").queries)
        assertTrue(QuestionTopic.CUT_OFF in p("cut off ఎంత").queries)
        assertEquals(false, p("no patch").threadLockPatch)
    }

    @Test fun generalQuestionIsLeftForTheCoPilot() {
        assertTrue(p("what is hydrogen embrittlement").isEmpty)
        // whole words only: "minute" has no nut, "profile" no wire
        assertNull(p("wait a minute").type)
        assertTrue(QuestionTopic.WEIGHT !in p("le profil du filetage").queries)
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
