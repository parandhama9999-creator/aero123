package com.aistudio.aerofast.trmst.data

/**
 * Selection helpers for the Sizing card. The selectors list every option; the PokaYokeEngine gate flags a
 * combination that does not fit, and these helpers only pick sensible defaults.
 */
/** Alloy family filter for the material selector. */
enum class MaterialGroup(val label: String, val family: MaterialFamily?) {
    ALL("All alloys", null),
    STEEL("Carbon & alloy steel", MaterialFamily.CARBON_ALLOY_STEEL),
    STAINLESS("Stainless & PH", MaterialFamily.STAINLESS),
    TITANIUM("Titanium", MaterialFamily.TITANIUM),
    NICKEL("Nickel / Co superalloy", MaterialFamily.NICKEL_SUPERALLOY),
    ALUMINIUM("Aluminium", MaterialFamily.ALUMINUM),
    COPPER("Copper alloy", MaterialFamily.COPPER_ALLOY)
}

/** Standard-family filter for the property class selector. */
enum class ClassGroup(val label: String) {
    ALL("All classes"),
    ISO_898_1("ISO 898-1 bolts"),
    ISO_898_2("ISO 898-2 nuts"),
    ISO_3506("ISO 3506 stainless"),
    SAE("SAE J429 / J995"),
    ASTM("ASTM structural & high temp"),
    AERO("Aerospace"),
    SPEC("Per material spec")
}

object SelectionFilter {

    fun hasThread(type: FastenerType) = !type.isUnthreaded && !type.isWasher

    // ---------------------------------------------------------------------------------------------
    // Material filter
    // ---------------------------------------------------------------------------------------------

    fun groupOf(m: MaterialInfo): MaterialGroup =
        MaterialGroup.values().firstOrNull { it.family == PokaYokeEngine.materialFamily(m) } ?: MaterialGroup.ALL

    /**
     * The alloys a property class can legally be made from: the class's own family rule (ISO 898-1 carbon /
     * alloy steel, ISO 3506 A2/A4 austenitic or C/F martensitic / ferritic, aerospace classes by family) and the
     * class strength the alloy has to reach. Same rules as the poka-yoke gate, so the list can never offer a
     * combination the gate would then block.
     */
    fun classAllows(pc: PropertyClass, m: MaterialInfo, dMm: Double = 10.0): Boolean {
        val family = PokaYokeEngine.materialFamily(m)
        val cs = PokaYokeEngine.classStrength(pc, dMm, m)
        val familyOk = when {
            cs.isIsoSteelClass || pc in PokaYokeEngine.INCH_STEEL_CLASSES -> family == MaterialFamily.CARBON_ALLOY_STEEL
            pc in PokaYokeEngine.STAINLESS_CLASSES -> PokaYokeEngine.isAusteniticA2A4(m)
            pc in PokaYokeEngine.MARTENSITIC_FERRITIC_CLASSES -> family == MaterialFamily.STAINLESS && !PokaYokeEngine.isAusteniticA2A4(m)
            pc == PropertyClass.AERO_AL -> family == MaterialFamily.ALUMINUM
            pc == PropertyClass.AERO_1100 -> family == MaterialFamily.TITANIUM
            pc == PropertyClass.AERO_1380 || pc == PropertyClass.AERO_1750 ->
                family == MaterialFamily.NICKEL_SUPERALLOY || family == MaterialFamily.CARBON_ALLOY_STEEL || family == MaterialFamily.STAINLESS
            pc == PropertyClass.MATERIAL_SPEC -> family != MaterialFamily.CARBON_ALLOY_STEEL || m.isAero
            else -> true
        }
        if (!familyOk) return false
        return m.postForgingTargetUtsMpa >= PokaYokeEngine.requiredRmMpa(pc, dMm, m) * 0.98
    }

    /**
     * Materials for the selector: narrowed to [group], and — when [onlyForClass] is on — to the alloys that can
     * actually be released in [pc] at diameter [dMm]. Never returns an empty list: if the filter leaves nothing,
     * the group's full list comes back so the selector still works.
     */
    fun materials(
        all: Collection<MaterialInfo>,
        group: MaterialGroup = MaterialGroup.ALL,
        pc: PropertyClass? = null,
        dMm: Double = 10.0,
        onlyForClass: Boolean = false,
        search: String = ""
    ): List<MaterialInfo> {
        val byGroup = all.filter { group.family == null || PokaYokeEngine.materialFamily(it) == group.family }
        val byClass = if (onlyForClass && pc != null) byGroup.filter { classAllows(pc, it, dMm) } else byGroup
        val q = search.trim()
        val bySearch = if (q.isEmpty()) byClass
        else byClass.filter { it.name.contains(q, true) || it.key.contains(q, true) }
        return bySearch.ifEmpty { byClass.ifEmpty { byGroup.ifEmpty { all.toList() } } }
    }

    /** Alloy families that have at least one grade in the catalogue, so no filter chip leads to an empty list. */
    fun materialGroups(all: Collection<MaterialInfo>): List<MaterialGroup> =
        listOf(MaterialGroup.ALL) + MaterialGroup.values().filter { g ->
            g.family != null && all.any { PokaYokeEngine.materialFamily(it) == g.family }
        }

    // ---------------------------------------------------------------------------------------------
    // Property class filter
    // ---------------------------------------------------------------------------------------------

    fun groupOf(pc: PropertyClass): ClassGroup = when {
        pc in PokaYokeEngine.ISO_NUT_CLASSES -> ClassGroup.ISO_898_2
        pc in PokaYokeEngine.STAINLESS_CLASSES && pc.name.startsWith("ASTM") -> ClassGroup.ASTM
        pc in PokaYokeEngine.STAINLESS_CLASSES || pc in PokaYokeEngine.MARTENSITIC_FERRITIC_CLASSES -> ClassGroup.ISO_3506
        pc.name.startsWith("CLASS_") -> ClassGroup.ISO_898_1
        pc.name.startsWith("SAE") -> ClassGroup.SAE
        pc.name.startsWith("ASTM") -> ClassGroup.ASTM
        pc.name.startsWith("AERO") -> ClassGroup.AERO
        else -> ClassGroup.SPEC
    }

    /**
     * Property classes for the selector: nut classes only on nuts, bolt classes only off nuts, metric ISO classes
     * first in metric and the SAE / ASTM grades first in inch, then narrowed to [group]. Never returns empty.
     */
    fun propertyClasses(
        type: FastenerType,
        unit: UnitSystem,
        group: ClassGroup = ClassGroup.ALL,
        search: String = ""
    ): List<PropertyClass> {
        if (!usesPropertyClass(type)) return emptyList()
        val all = PropertyClass.values().toList()
        val byType = all.filter { pc ->
            when {
                // ISO 898-2 / J995 / A563 / A194 classes are nut classes
                pc in PokaYokeEngine.NUT_ONLY_CLASSES -> type.isNut
                // thin-nut classes 04 / 05 only make sense on a thin nut, handled above by NUT_ONLY_CLASSES
                else -> true
            }
        }
        val byGroup = if (group == ClassGroup.ALL) byType else byType.filter { groupOf(it) == group }
        val q = search.trim()
        val bySearch = if (q.isEmpty()) byGroup else byGroup.filter { it.label.contains(q, true) }
        val picked = bySearch.ifEmpty { byGroup.ifEmpty { byType } }
        // Put the classes written for this measurement system first
        val inchFirst = unit == UnitSystem.INCH
        return picked.sortedBy { pc ->
            val g = groupOf(pc)
            val isInchFamily = g == ClassGroup.SAE || g == ClassGroup.ASTM
            when {
                inchFirst && isInchFamily -> 0
                !inchFirst && (g == ClassGroup.ISO_898_1 || g == ClassGroup.ISO_898_2 || g == ClassGroup.ISO_3506) -> 0
                else -> 1
            }
        }
    }

    /** Class groups that have at least one class for this fastener type, so no chip leads to an empty list. */
    fun classGroups(type: FastenerType): List<ClassGroup> {
        val available = propertyClasses(type, UnitSystem.METRIC).map { groupOf(it) }.toSet()
        return listOf(ClassGroup.ALL) + ClassGroup.values().filter { it != ClassGroup.ALL && it in available }
    }

    /** The default alloy for a class: its own [PropertyClass.defaultMatKey] when that grade can carry the class. */
    fun defaultMaterialKey(pc: PropertyClass, all: Map<String, MaterialInfo>, dMm: Double = 10.0): String? {
        all[pc.defaultMatKey]?.let { if (classAllows(pc, it, dMm)) return it.key }
        return all.values.firstOrNull { classAllows(pc, it, dMm) }?.key
    }

    /** Washers are not graded by bolt property class (the gate skips it). */
    fun usesPropertyClass(type: FastenerType) = !type.isWasher

    /** Thread classes in order of preference for this standard; the first is the default when the part or unit changes. */
    fun threadClasses(unit: UnitSystem, type: FastenerType, standard: ProductStandard): List<String> {
        val all = ThreadClassCatalog.getOptions(unit, type.isNut)
        val allowed = PokaYokeEngine.standardThreadClasses(standard, type.isNut)
            // Aerospace standards set no class, but are written for the controlled-root UNJ (ASME B1.15)
            // and MJ (ISO 5855) profiles, so those come first
            ?: return if (standard.category == "Aerospace/Defense")
                all.sortedByDescending { it.contains("UNJ") || it.endsWith("-MJ") } else all
        // The standard's own class first (e.g. 3A for ASME B18.3), then its classes for coated parts
        return allowed.filter { it in all }.ifEmpty { all }
    }
}
