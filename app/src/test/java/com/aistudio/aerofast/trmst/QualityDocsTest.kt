package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.CalculationEngine
import com.aistudio.aerofast.trmst.data.CoatingType
import com.aistudio.aerofast.trmst.data.FastenerCalculationResult
import com.aistudio.aerofast.trmst.data.FastenerCatalog
import com.aistudio.aerofast.trmst.data.FastenerType
import com.aistudio.aerofast.trmst.data.ProductStandard
import com.aistudio.aerofast.trmst.data.PropertyClass
import com.aistudio.aerofast.trmst.data.QualityDocs
import com.aistudio.aerofast.trmst.data.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityDocsTest {
    private val m10 = FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == 10.0 }

    private fun calc(
        type: FastenerType = FastenerType.HEX_BOLT,
        standard: ProductStandard = ProductStandard.ISO_4014,
        length: Double = 60.0,
        coating: CoatingType = CoatingType.ZINC_BLUE,
        pc: PropertyClass? = PropertyClass.CLASS_10_9,
        material: String = "10B21"
    ): FastenerCalculationResult = CalculationEngine.calculate(
        fastenerType = type, unitSystem = UnitSystem.METRIC, standard = standard, nominalSizeIndex = m10,
        pitchValue = 1.5, lengthMm = length, threadClass = if (type.isNut) "6H" else "6g",
        material = FastenerCatalog.MATERIALS[material]!!, coating = coating, batchQty = 1000, propertyClass = pc
    )

    @Test fun hexBoltTolerances_followIso4759() {
        val tol = QualityDocs.hexBoltTolerances(calc()).associate { it.first to it.second }
        assertTrue(tol["Across flats s"]!!, tol["Across flats s"]!!.startsWith("16.00 −0.27 / 0 (h13)"))
        assertTrue(tol["Head height k"]!!, tol["Head height k"]!!.startsWith("6.40 ±0.18 (js14, grade A)"))
        assertTrue(tol["Length l"]!!, tol["Length l"]!!.startsWith("60.0 ±0.60 (js15, grade A)"))
        // l = 160 > 10d → grade B
        assertTrue(QualityDocs.hexBoltTolerances(calc(length = 160.0)).any { it.second.contains("js15, grade B") })
        // a socket head cap screw gets no hex-bolt tolerances
        assertTrue(QualityDocs.hexBoltTolerances(calc(FastenerType.SHCS, ProductStandard.ISO_4762)).isEmpty())
    }

    @Test fun controlPlan_coversThreadHardnessProofCoatingAndBake() {
        val r = calc()
        val rows = QualityDocs.controlPlan(r)
        val names = rows.map { it.characteristic }
        assertTrue(names.any { it.startsWith("Pitch diameter") })
        assertTrue("Core hardness" in names)
        assertTrue("Proof load" in names)
        assertTrue("Coating thickness" in names)
        assertTrue("Hydrogen de-embrittlement bake" in names)
        val proof = rows.first { it.characteristic == "Proof load" }
        assertTrue(proof.spec, proof.spec.startsWith(String.format(java.util.Locale.US, "%.1f kN", r.proofLoadKn)))
        assertTrue(proof.special)
        val pd = rows.first { it.characteristic.startsWith("Pitch diameter") }
        assertTrue(pd.method.contains("ISO 1502"))
    }

    @Test fun controlPlan_skipsWhatDoesNotApply() {
        val plain = QualityDocs.controlPlan(calc(coating = CoatingType.BLACK_OXIDE, pc = PropertyClass.CLASS_4_8, material = "1008"))
        assertFalse(plain.any { it.characteristic.startsWith("Hydrogen") })
        val nut = QualityDocs.controlPlan(calc(FastenerType.HEX_NUT, ProductStandard.ISO_4032, pc = PropertyClass.CLASS_10_9))
        assertTrue(nut.first { it.characteristic.startsWith("Pitch diameter") }.method.contains("plug gauge"))
        assertFalse(nut.any { it.characteristic.startsWith("Minimum tensile") })
    }

    @Test fun ppap_levelCodesMatchAiagTable() {
        val e = QualityDocs.ppapElements(calc())
        assertEquals(19, e.size)
        assertEquals("S S S S R", e.first { it.no == 18 }.levels)
        assertEquals("S S S * R", e.first { it.no == 13 }.levels)
        assertEquals("R R R * R", e.first { it.no == 15 }.levels)
        // level 1 submits only the PSW and the appearance report
        assertEquals(listOf(13, 18), e.filter { it.levels.split(' ')[0] == "S" }.map { it.no })
        val studies = e.first { it.no == 11 }.forThisPart
        assertTrue(studies, studies.contains("head height") && studies.contains("core hardness"))
        assertFalse(studies, studies.contains("proof load") || studies.contains("embrittlement"))
        val psw = QualityDocs.psw(calc(), "AB-123", "C", "OEM", 3, QualityDocs.PSW_REASONS[0], "Spec X\n\nSpec Y")
        assertTrue(psw.contains("Submission level : 3"))
        assertTrue(psw.contains(" - Spec X\n - Spec Y"))
    }

    @Test fun ecn_listsSelectedTriggers() {
        val text = QualityDocs.ecn(calc(), "ECN-7", "B", "C", "New plater", "Capacity", setOf(4), "")
        assertTrue(text.contains("CUSTOMER NOTIFICATION REQUIRED"))
        assertTrue(text.contains("heat treatment, plating"))
        assertTrue(QualityDocs.ecn(calc(), "", "", "", "", "", emptySet(), "").contains("No PPAP customer-notification case"))
    }

    @Test fun coatingMethods_matchTheCoating() {
        assertTrue(QualityDocs.coatingThicknessMethods(CoatingType.ZN_NI).first().second.contains("ISO 3497"))
        assertTrue(QualityDocs.coatingThicknessMethods(CoatingType.PHOSPHATE).first().second.contains("ISO 3892"))
        assertTrue(QualityDocs.coatingThicknessMethods(CoatingType.PASSIVATION).first().second.contains("ASTM A967"))
    }
}
