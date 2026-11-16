package com.aistudio.aerofast.trmst

import com.aistudio.aerofast.trmst.data.*
import org.junit.Assert.*
import org.junit.Test

/** The enlarged catalogue and the Sizing filters: every option must lead somewhere usable. */
class CatalogueFilterTest {

    private val materials = FastenerCatalog.MATERIALS.values.toList()

    @Test fun everyExtendedGradeIsInTheCatalogue() {
        val added = ExtendedMaterials.extend(FastenerCatalog.DEFAULT_MATERIALS)
        assertTrue("no extended grades", added.size >= 30)
        for ((key, m) in added) {
            assertEquals(key, m.key)
            assertNotNull("$key missing from the catalogue", FastenerCatalog.MATERIALS[key])
            assertTrue("$key has no sourcing note", m.dataSources.isNotEmpty())
            assertNotNull("$key does not say which grade it was estimated from", m.estimatedFrom)
            assertTrue("$key has no strength", m.postForgingTargetUtsMpa > 0.0)
            assertTrue("$key has no density", m.density > 0.5)
        }
    }

    @Test fun everyMaterialFilterGroupHasGrades() {
        for (g in SelectionFilter.materialGroups(materials)) {
            val list = SelectionFilter.materials(materials, group = g)
            assertTrue("$g is empty", list.isNotEmpty())
            if (g.family != null) assertTrue("$g holds a foreign family",
                list.all { PokaYokeEngine.materialFamily(it) == g.family })
        }
    }

    @Test fun materialFilterNeverReturnsAnEmptyList() {
        for (pc in PropertyClass.values()) for (g in MaterialGroup.values()) {
            val list = SelectionFilter.materials(materials, group = g, pc = pc, onlyForClass = true)
            assertTrue("$pc / $g left the selector empty", list.isNotEmpty())
        }
    }

    @Test fun classFilterOffersNutClassesOnNutsOnly() {
        for (type in FastenerType.values()) {
            val classes = SelectionFilter.propertyClasses(type, UnitSystem.METRIC)
            if (type.isWasher) {
                assertTrue("$type is a washer: it is not graded by property class", classes.isEmpty())
                continue
            }
            assertTrue("$type has no property class", classes.isNotEmpty())
            val nutClasses = classes.filter { it in PokaYokeEngine.NUT_ONLY_CLASSES }
            if (type.isNut) assertTrue("$type offers no nut class", nutClasses.isNotEmpty())
            else assertTrue("$type (not a nut) offers nut classes $nutClasses", nutClasses.isEmpty())
        }
    }

    @Test fun everyClassGroupChipHasClasses() {
        for (type in listOf(FastenerType.HEX_BOLT, FastenerType.HEX_NUT, FastenerType.SHCS)) {
            for (g in SelectionFilter.classGroups(type)) {
                assertTrue("$type / $g is empty", SelectionFilter.propertyClasses(type, UnitSystem.METRIC, g).isNotEmpty())
            }
        }
    }

    /** The filter must never offer a material the release gate would then reject for that class. */
    @Test fun classFilterAgreesWithTheGate() {
        for (pc in PropertyClass.values()) {
            val type = if (pc in PokaYokeEngine.NUT_ONLY_CLASSES) FastenerType.HEX_NUT else FastenerType.HEX_BOLT
            val allowed = SelectionFilter.materials(materials, pc = pc, onlyForClass = true)
            for (m in allowed) {
                val idx = FastenerCatalog.sizeIndexFor(UnitSystem.METRIC, 10.0)
                val issues = PokaYokeEngine.check(
                    type, UnitSystem.METRIC, PokaYokeEngine.defaultStandard(type, UnitSystem.METRIC), idx,
                    FastenerCatalog.METRIC_SIZES[idx].pitches.first(), 10.0, 60.0,
                    SelectionFilter.threadClasses(UnitSystem.METRIC, type, PokaYokeEngine.defaultStandard(type, UnitSystem.METRIC)).first(),
                    pc, m, CoatingType.GEOMET, 100, 0.0, 0.0, 100.0, false, 50.0, null
                ).filter { it.severity == PokaYokeSeverity.CRITICAL }.map { it.title }
                assertTrue("$pc + ${m.key}: $issues", issues.none {
                    it.contains("needs") || it.contains("cannot reach") || it.contains("not an austenitic")
                })
            }
        }
    }

    @Test fun everyTypeHasAStandardAndAHeadInBothUnits() {
        for (type in FastenerType.values()) for (unit in UnitSystem.values()) {
            val std = PokaYokeEngine.defaultStandard(type, unit)
            assertNotNull("$type $unit has no standard", std)
            val head = DimensionTables.forPart(type, unit, 10.0)
            assertTrue("$type $unit head source is blank", head.source.isNotBlank())
            assertTrue("$type $unit negative head", head.headHeightK >= 0.0 && head.acrossFlatsS > 0.0)
        }
    }

    /** Cheese (trim blank) Ø is 1.20 x the across flats, not 1.2 x the across corners. */
    @Test fun hexCheeseDiameterIsOnePointTwoAcrossFlats() {
        val head = HeadDimensions(16.0, 6.4, 14.63, 8.4, "ISO 4014", true)
        val f = BoltForging.compute(
            FastenerType.HEX_BOLT, UnitSystem.METRIC, 10.0, 1.5, 60.0, 8.962, 9.85, head, 7.85,
            null, null, null, null, null, null
        )
        val forged = Math.PI / 4 * (1.2 * 16.0) * (1.2 * 16.0) * 6.4
        val finished = Math.sqrt(3.0) / 2 * 16.0 * 16.0 * 6.4
        assertEquals(forged - finished, f.cheeseVolumeMm3!!, 0.01)
        assertTrue(f.headNote.contains("1.20 × s"))
    }
}
