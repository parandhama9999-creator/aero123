package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

class SelectionFilterTest {
    private val cat = FastenerCatalog.MATERIALS

    private fun sizes(unit: UnitSystem) = if (unit == UnitSystem.METRIC) listOf(3.0, 6.0, 10.0, 20.0, 36.0) else listOf(6.35, 9.525, 19.05, 25.4)

    private val filteredWarnings = listOf("Zinc coating on corrosion-resistant alloy", "Zinc/cadmium on aluminium fastener", "Cadmium is restricted",
        "Hot-dip galvanizing below M8", "Commercial class on an aerospace fastener", "Aerospace alloy on a commercial class", "Titanium class with non-titanium material", "SAE J429 grade on a metric thread", "Inch standard with metric thread")

    @Test fun everyOfferedCombinationPassesTheGate() {
        var combos = 0
        val failures = mutableListOf<String>()
        for (type in FastenerType.values()) for (unit in UnitSystem.values()) for (d in sizes(unit)) {
            val idx = FastenerCatalog.sizeIndexFor(unit, d)
            val dd = FastenerCatalog.diameterMm(unit, idx)
            val pitch = if (unit == UnitSystem.METRIC) FastenerCatalog.METRIC_SIZES[idx].pitches.first() else FastenerCatalog.INCH_SIZES[idx].tpis.first().toDouble()
            val stds = SelectionFilter.standards(type, unit)
            assertTrue("$type $unit no standard", stds.isNotEmpty())
            val pcs = SelectionFilter.propertyClasses(type, unit, dd)
            assertTrue("$type $unit no class", pcs.isNotEmpty())
            for (std in stds) for (pc in pcs) {
                val mats = SelectionFilter.materials(type, pc, dd, cat.values)
                if (mats.isEmpty()) { failures += "EMPTY materials: $type $unit d=$dd $pc"; continue }
                for (m in mats) for (tc in SelectionFilter.threadClasses(unit, type, std)) {
                    val coats = SelectionFilter.coatings(type, m, pc, dd, tc)
                    if (coats.isEmpty()) { failures += "EMPTY coatings: $type $pc ${m.key} $tc"; continue }
                    for (c in coats) {
                        combos++
                        val issues = PokaYokeEngine.check(type, unit, std, idx, pitch, dd, 4 * dd + 20, tc, pc, m, c, 100,
                            0.0, 0.0, 100.0, false, 50.0, null)
                        val bad = issues.filter { it.severity == PokaYokeSeverity.CRITICAL || it.title.startsWith("Thread class not per") || it.severity == PokaYokeSeverity.WARNING && it.title in filteredWarnings && !(it.title == "Inch standard with metric thread" && std == PokaYokeEngine.defaultStandard(type, unit)) }
                        if (bad.isNotEmpty()) failures += "$type $unit d=$dd ${std.code} $pc ${m.key} $tc ${c.name}: ${bad.map { it.title }.distinct()}"
                    }
                }
            }
        }
        println("combos checked: $combos")
        val grouped = failures.groupBy { it.substringAfter(": ", it).take(120) }
        grouped.entries.take(40).forEach { println("${it.value.size}x ${it.key}\n    e.g. ${it.value.first()}") }
        assertTrue("${failures.size} failures", failures.isEmpty())
    }

    @Test fun normalizeFixesInvalidAndIsStable() {
        val start = SelectionFilter.Selection(FastenerType.HEX_BOLT, UnitSystem.METRIC, ProductStandard.ISO_4014, 10.0, PropertyClass.CLASS_10_9, "10B21", "6g", CoatingType.ZN_NI)
        assertEquals(start, SelectionFilter.normalize(start, cat))
        // Type → hex nut: standard must become a nut standard
        val nut = SelectionFilter.normalize(start.copy(type = FastenerType.HEX_NUT), cat)
        assertEquals(ProductStandard.ISO_4032, nut.standard)
        assertTrue(nut.threadClass in ThreadClassCatalog.getOptions(UnitSystem.METRIC, true))
        // Stainless material from the catalogue/scan: class follows the material, material is kept
        val ss = SelectionFilter.normalize(start.copy(materialKey = "SS304"), cat)
        assertEquals("SS304", ss.materialKey)
        assertTrue(ss.propertyClass.name.startsWith("A"))
        assertEquals(CoatingType.PASSIVATION, ss.coating)
        // Rivet: aerospace class
        val rivet = SelectionFilter.normalize(start.copy(type = FastenerType.SOLID_RIVET, unit = UnitSystem.INCH, dMm = 4.826), cat)
        assertTrue(rivet.propertyClass in SelectionFilter.propertyClasses(FastenerType.SOLID_RIVET, UnitSystem.INCH, 4.826))
        assertEquals(ProductStandard.MS20470, rivet.standard)
        // 12.9 can never keep an electroplated coating
        val c129 = SelectionFilter.normalize(start.copy(propertyClass = PropertyClass.CLASS_12_9, materialKey = "4140"), cat)
        assertFalse(c129.coating.isElectroplating)
        // Inch unit drops metric standards and ISO classes
        val inch = SelectionFilter.normalize(start.copy(unit = UnitSystem.INCH, dMm = 9.525), cat)
        assertNotEquals("ISO/DIN Metric", inch.standard.category)
        assertTrue(inch.propertyClass.name.startsWith("SAE") || inch.propertyClass.name.startsWith("ASTM"))
        // every result is a fixed point
        for (type in FastenerType.values()) for (unit in UnitSystem.values()) for (m in cat.keys) {
            val n = SelectionFilter.normalize(start.copy(type = type, unit = unit, materialKey = m, dMm = if (unit == UnitSystem.METRIC) 10.0 else 9.525), cat)
            assertEquals("$type $unit $m", n, SelectionFilter.normalize(n, cat))
        }
    }

    @Test fun defaultsAreOffered() {
        for (type in FastenerType.values()) for (unit in UnitSystem.values())
            assertEquals(PokaYokeEngine.defaultStandard(type, unit), SelectionFilter.standards(type, unit).first())
    }

    @Test fun everyCatalogueAlloyCanBeSelected() {
        val types = listOf(FastenerType.HEX_BOLT, FastenerType.NAS_CLOSE_TOL_BOLT, FastenerType.SOLID_RIVET)
        for (m in cat.values) {
            val reachable = types.any { t -> UnitSystem.values().any { u ->
                SelectionFilter.propertyClasses(t, u, 6.35).any { SelectionFilter.materialFitsClass(m, it, 6.35) } } }
            assertTrue("${m.key} fits no property class", reachable)
        }
    }

    @Test fun a2a4ClassesOfferOnly304And316() {
        for (pc in listOf(PropertyClass.A2_50, PropertyClass.A2_70, PropertyClass.A4_80)) {
            val keys = SelectionFilter.materials(FastenerType.METAL_LOCK_NUT, pc, 6.0, cat.values).map { it.key }.toSet()
            assertTrue("$pc: $keys", keys.isNotEmpty() && keys.all { it in setOf("SS304", "SS316", "SS316L") })
        }
        for (k in listOf("SS410", "17_4PH", "15_5PH", "13_8PH", "CUSTOM_455", "AM350", "NITRONIC60", "14KH17N2", "20X13", "30X13"))
            assertFalse(k, SelectionFilter.materialFitsClass(cat[k]!!, PropertyClass.A2_70, 6.0))
    }

    @Test fun screenshotComboIsCorrected() {
        val s = SelectionFilter.Selection(FastenerType.HEX_BOLT, UnitSystem.METRIC, ProductStandard.ISO_4014, 10.0, PropertyClass.CLASS_8_8, "35NCD16", "4g6g", CoatingType.ZN_NI)
        val n = SelectionFilter.normalize(s, cat)
        assertEquals("6g", n.threadClass)
        assertEquals("35NCD16", n.materialKey)
        assertEquals(PropertyClass.MATERIAL_SPEC, n.propertyClass)
        assertFalse("35NCD16" in SelectionFilter.materials(FastenerType.HEX_BOLT, PropertyClass.CLASS_8_8, 10.0, cat.values).map { it.key })
    }

    @Test fun aerospaceTypesOfferNoCommercialClasses() {
        val classes = SelectionFilter.propertyClasses(FastenerType.TWELVE_POINT, UnitSystem.METRIC, 6.0)
        assertFalse(PropertyClass.CLASS_4_6 in classes)
        assertTrue(PropertyClass.MATERIAL_SPEC in classes)
        assertTrue("4340" in SelectionFilter.materials(FastenerType.NAS_CLOSE_TOL_BOLT, PropertyClass.MATERIAL_SPEC, 6.35, cat.values).map { it.key })
    }
}
