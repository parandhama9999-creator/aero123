package com.example

import com.example.data.CalculationEngine
import com.example.data.CoatingType
import com.example.data.DimensionTables
import com.example.data.FastenerCatalog
import com.example.data.FastenerType
import com.example.data.PokaYokeSeverity
import com.example.data.ProductStandard
import com.example.data.PropertyClass
import com.example.data.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** ISO/ASME dimension tables, catalogue weights and ISO 898-2 bolt–nut matching. */
class DimensionAndNutTest {
    private val m10 = FastenerCatalog.METRIC_SIZES.indexOfFirst { it.d == 10.0 }

    private fun calc(type: FastenerType, std: ProductStandard, cls: String) = CalculationEngine.calculate(
        fastenerType = type, unitSystem = UnitSystem.METRIC, standard = std, nominalSizeIndex = m10,
        pitchValue = 1.5, lengthMm = 60.0, threadClass = cls, material = FastenerCatalog.MATERIALS["10B21"]!!,
        coating = CoatingType.GEOMET, batchQty = 1000, propertyClass = PropertyClass.CLASS_10_9
    )

    @Test fun tables_matchIsoAndAsme() {
        DimensionTables.forPart(FastenerType.HEX_BOLT, UnitSystem.METRIC, 10.0).let {
            assertEquals(16.0, it.acrossFlatsS, 1e-9); assertEquals(6.4, it.headHeightK, 1e-9); assertEquals(8.4, it.nutHeightM, 1e-9)
        }
        assertEquals(16.0, DimensionTables.forPart(FastenerType.SHCS, UnitSystem.METRIC, 10.0).acrossFlatsS, 1e-9)
        assertEquals(0.5625 * 25.4, DimensionTables.forPart(FastenerType.HEX_BOLT, UnitSystem.INCH, 9.525).acrossFlatsS, 1e-6)
        assertFalse(DimensionTables.forPart(FastenerType.HEX_BOLT, UnitSystem.METRIC, 0.5).isTable)
    }

    @Test fun weights_matchCatalogueValues() {
        // DIN 931 / ISO 4014 M10×60 ≈ 44.5 g; ISO 4032 M10 nut ≈ 10.9 g
        assertEquals(44.5, calc(FastenerType.HEX_BOLT, ProductStandard.ISO_4014, "6g").netWeightGrams, 2.5)
        assertEquals(10.9, calc(FastenerType.HEX_NUT, ProductStandard.ISO_4032, "6H").netWeightGrams, 1.0)
    }

    @Test fun boltRequiresMatchingNutClass() {
        val bolt = calc(FastenerType.HEX_BOLT, ProductStandard.ISO_4014, "6g")
        assertTrue(bolt.pokaYokeIssues.any { it.severity == PokaYokeSeverity.MANDATORY && it.title.contains("nut class 10") })
    }

    @Test fun nut_isClass10_withBoltBreakingProofLoad() {
        val nut = calc(FastenerType.HEX_NUT, ProductStandard.ISO_4032, "6H")
        assertTrue(nut.pokaYokeIssues.any { it.title.contains("Nut class 10") })
        assertEquals(60.3, nut.proofLoadKn, 0.2) // As 58.0 × Rm 1040
    }
}
