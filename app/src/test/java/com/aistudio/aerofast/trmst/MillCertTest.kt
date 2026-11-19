package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The mill certificate's wire Rm / HRB is checked against the catalogue's headable range for the grade. */
class MillCertTest {
    private val m10 = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
    private fun calc(type: FastenerType, std: ProductStandard, mat: String, cert: MillCert?) =
        CalculationEngine.calculate(type, UnitSystem.METRIC, std, m10, 1.5, 50.0, "6g", FastenerCatalog.MATERIALS.getValue(mat),
            CoatingType.ZN_NI, 10000, propertyClass = PropertyClass.CLASS_12_9, millCert = cert)
    private fun shcs(mat: String, cert: MillCert?) = calc(FastenerType.SHCS, ProductStandard.ISO_4762, mat, cert)
    private val title = "Wire too hard for the socket head"

    @Test fun withinTheCatalogueRangeTheSocketHeadIsHeadable() {
        // 4140 catalogue: 650 – 730 MPa spheroidized wire
        val r = shcs("4140", MillCert.of(700.0, null))
        assertEquals(WireVerdict.HEADABLE, r.wireCheck!!.verdict)
        assertTrue(r.wireCheck!!.recessHead)
        assertTrue(r.pokaYokeIssues.none { it.title == title })
    }

    @Test fun hardWireMustBeAnnealedBeforeTheSocket() {
        val r = shcs("4140", MillCert.of(850.0, null))
        assertEquals(WireVerdict.TOO_HARD, r.wireCheck!!.verdict)
        val issue = r.pokaYokeIssues.single { it.title == title }
        assertEquals(PokaYokeSeverity.MANDATORY, issue.severity)
        assertTrue(issue.detail, issue.detail.contains("650 – 730 MPa") && issue.detail.contains("Rm 850 MPa"))
        assertTrue(ProductionReport.processSequence(r).first().contains("mill cert above the headable range"))
        assertTrue(ProductionReport.build(r).contains("Mill cert (wire)"))
    }

    @Test fun aHexBoltWithHardWireIsAWarningNotTheSocketRule() {
        val r = calc(FastenerType.HEX_BOLT, ProductStandard.ISO_4014, "4140", MillCert.of(850.0, null))
        assertTrue(r.pokaYokeIssues.none { it.title == title })
        assertEquals(PokaYokeSeverity.WARNING, r.pokaYokeIssues.single { it.title == "Wire harder than the headable range" }.severity)
    }

    @Test fun hardnessIsComparedWithTheDerivedHrbBand() {
        // 650 – 730 MPa → 192 – 216 HB (Rm / 3.38) → ≈ 92 – 96 HRB (ASTM A370)
        assertEquals(WireVerdict.TOO_HARD, shcs("4140", MillCert.of(null, 99.0)).wireCheck!!.verdict)
        assertEquals(WireVerdict.HEADABLE, shcs("4140", MillCert.of(null, 94.0)).wireCheck!!.verdict)
        assertEquals(WireVerdict.SOFTER_THAN_RANGE, shcs("4140", MillCert.of(null, 88.0)).wireCheck!!.verdict)
    }

    @Test fun softerWireIsReportedNotBlocked() {
        // 10B21 catalogue: 480 – 560 MPa
        val r = shcs("10B21", MillCert.of(450.0, null))
        assertEquals(WireVerdict.SOFTER_THAN_RANGE, r.wireCheck!!.verdict)
        assertTrue(r.pokaYokeIssues.none { it.title == title })
    }

    @Test fun nothingEnteredNothingChecked() {
        assertNull(MillCert.of(null, null))
        assertNull(MillCert.of(0.0, 0.0))
        assertNull(shcs("4140", null).wireCheck)
    }

    @Test fun anotherGradeInAWhatIfDoesNotKeepTheCertificate() {
        val r = shcs("4140", MillCert.of(850.0, null))
        assertEquals(r.millCert, OfflineKnowledge.recalc(r, QuestionParser.parse("M12 x 60")).millCert)
        assertNull(OfflineKnowledge.recalc(r, QuestionParser.parse("10B21")).millCert)
    }
}
