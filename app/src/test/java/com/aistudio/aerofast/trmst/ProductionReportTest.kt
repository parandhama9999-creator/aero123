package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Shop defaults fill what a drawing leaves out; the production report carries the app's own figures. */
class ProductionReportTest {

    @After fun resetDefaults() {
        ShopDefaults.materialKey = ShopDefaults.FACTORY_MATERIAL
        ShopDefaults.propertyClass = ShopDefaults.FACTORY_CLASS
        ShopDefaults.coating = ShopDefaults.FACTORY_COATING
        ShopDefaults.batchQty = ShopDefaults.FACTORY_BATCH
    }

    private fun scan(json: String) = GeminiAiService.parseSpecFromJson(JSONObject(json), "test")
    private val m10x60 = """{"fastenerType":"HEX_BOLT","unitSystem":"METRIC","nominalDiameterMm":10,"lengthMm":60,"pitch":1.5"""

    @Test fun factoryDefaultsAreTheShopRequirement() {
        assertEquals("4135", ShopDefaults.material.key)
        assertEquals(PropertyClass.CLASS_10_9, ShopDefaults.propertyClass)
        assertEquals(CoatingType.ZN_NI, ShopDefaults.coating)
        assertEquals(10000, ShopDefaults.batchQty)
    }

    @Test fun drawingWithoutMaterialClassCoatingUsesShopDefaults() {
        val spec = scan("$m10x60}")
        assertEquals("4135", spec.materialKey)
        assertEquals(PropertyClass.CLASS_10_9, spec.propertyClass)
        assertEquals(CoatingType.ZN_NI, spec.coating)
        assertTrue(spec.assumed.any { it.startsWith("material") && "4135" in it })
        assertTrue(spec.assumed.any { it.startsWith("property class") })
        assertTrue(spec.assumed.any { it.startsWith("coating") })
    }

    @Test fun valuesOnTheDrawingWinOverDefaults() {
        val spec = scan("""$m10x60,"materialKey":"10B21","propertyClass":"8.8","coating":"GEOMET"}""")
        assertEquals("10B21", spec.materialKey)
        assertEquals(PropertyClass.CLASS_8_8, spec.propertyClass)
        assertEquals(CoatingType.GEOMET, spec.coating)
        assertTrue(spec.assumed.none { it.startsWith("material") || it.startsWith("property class") || it.startsWith("coating") })
    }

    @Test fun editedDefaultsReachTheNextScanAndTheBatch() {
        ShopDefaults.materialKey = "10B21"
        ShopDefaults.propertyClass = PropertyClass.CLASS_8_8
        ShopDefaults.batchQty = 25000
        val spec = scan("$m10x60}")
        assertEquals("10B21", spec.materialKey)
        assertEquals(PropertyClass.CLASS_8_8, spec.propertyClass)
        assertEquals(25000, DrawingScan.calculate(spec).batchQty)
    }

    @Test fun reportCarriesTheCalculatedFigures() {
        val spec = scan("$m10x60}")
        val r = DrawingScan.calculate(spec)
        val s = ProductionReport.summary(r)
        assertEquals(10000, s.batchQty)
        assertEquals(r.cutOffLengthMm, s.cutOffLengthMm, 1e-9)
        assertEquals(r.netWeightGrams * 10000 / 1000.0, s.batchFinishedKg, 1e-9)
        assertTrue(s.slugWeightG >= s.finishedWeightG)
        assertTrue(s.batchRawMaterialKg >= s.batchFinishedKg)

        val text = ProductionReport.build(r, spec, "2026-11-18 10:00")
        listOf(
            "1. REALITY CHECK AGAINST THE STANDARD", "2. CUT-OFF LENGTH & WEIGHT", "5. HEADING", "6. THREAD ROLLING",
            "7. HEAT TREATMENT", "8. COATING & HYDROGEN EMBRITTLEMENT", "9. MECHANICAL PROPERTIES REQUIRED", "10. PROCESS SEQUENCE", "11. RECOMMENDATIONS"
        ).forEach { assertTrue(it, it in text) }
        assertTrue("CUT-OFF LENGTH          : ${String.format(java.util.Locale.US, "%,.2f", r.cutOffLengthMm)} mm" in text)
        assertTrue("4135" in text && "10000 pcs" in text)
        // 10.9 with Zn-Ni plating must call for the hydrogen de-embrittlement bake
        assertTrue(r.hasHydrogenEmbrittlementRisk)
        assertTrue("De-embrittlement bake   : REQUIRED" in text)
        // One cut-off: the recommended wire in the options table is sheared to the same length as section 2
        val rec = s.plan!!.options.first { kotlin.math.abs(it.wireDiaMm - r.wireDiaMm) < 1e-6 }
        assertEquals(r.cutOffLengthMm, rec.cutOffLengthMm, 0.06)
        assertEquals(r.headingPlan!!.slugVolumeMm3 * r.material.density / 1000.0, s.slugWeightG, 0.01)
        assertTrue("Confirm with the customer: material" in text)
    }
}
