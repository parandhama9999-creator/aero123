package com.example.data

/**
 * Narrows each Sizing selector to the options that fit everything chosen above it:
 * Type → Unit → Standard → Size → Property class → Material → Thread class → Coating.
 * The rules are the PokaYokeEngine gate's own (every CRITICAL, plus the clear wrong-material
 * warnings), so a combination the lists offer is one the gate accepts.
 */
object SelectionFilter {

    data class Selection(
        val type: FastenerType,
        val unit: UnitSystem,
        val standard: ProductStandard,
        val dMm: Double,
        val propertyClass: PropertyClass,
        val materialKey: String,
        val threadClass: String,
        val coating: CoatingType
    )

    fun hasThread(type: FastenerType) = !type.isUnthreaded && !type.isWasher

    /** Washers are not graded by bolt property class (the gate skips it), so it does not restrict their material. */
    fun usesPropertyClass(type: FastenerType) = !type.isWasher

    private fun typeStandards(type: FastenerType): Set<ProductStandard> = when (type) {
        FastenerType.HEX_BOLT -> setOf(ProductStandard.ISO_4014, ProductStandard.ASME_B18_2_1)
        FastenerType.HEX_BOLT_FULL -> setOf(ProductStandard.ISO_4017, ProductStandard.ASME_B18_2_1)
        FastenerType.SHCS -> setOf(ProductStandard.ISO_4762, ProductStandard.ASME_B18_3, ProductStandard.NAS1351)
        FastenerType.SOCKET_CSK -> setOf(ProductStandard.ISO_10642, ProductStandard.ASME_B18_3)
        FastenerType.SOCKET_BUTTON -> setOf(ProductStandard.ISO_7380, ProductStandard.ASME_B18_3)
        FastenerType.SOCKET_SET -> setOf(ProductStandard.ISO_4026, ProductStandard.ASME_B18_3)
        FastenerType.FLANGE_BOLT -> setOf(ProductStandard.ISO_1665, ProductStandard.ASME_B18_2_1)
        FastenerType.TWELVE_POINT -> setOf(ProductStandard.NAS624, ProductStandard.NAS1801, ProductStandard.EN2882)
        FastenerType.CARRIAGE_BOLT -> setOf(ProductStandard.DIN_603, ProductStandard.ASME_B18_2_1)
        FastenerType.T_BOLT -> setOf(ProductStandard.DIN_186, ProductStandard.ASME_B18_2_1)
        FastenerType.HEAVY_HEX_BOLT -> setOf(ProductStandard.ASTM_A325, ProductStandard.ASTM_A490, ProductStandard.ASME_B18_2_1)
        FastenerType.EYE_BOLT -> setOf(ProductStandard.DIN_444, ProductStandard.ASME_B18_2_1)
        FastenerType.TTY_HEAD_BOLT -> setOf(ProductStandard.OEM_TTY, ProductStandard.ASME_B18_2_1)
        FastenerType.HEXALOBULAR_SCREW -> setOf(ProductStandard.ISO_14579, ProductStandard.ASME_B18_3)
        FastenerType.THREAD_FORMING_SCREW -> setOf(ProductStandard.DIN_7500, ProductStandard.ASME_B18_2_1)
        FastenerType.WHEEL_BOLT -> setOf(ProductStandard.DIN_74361, ProductStandard.ASME_B18_2_1)
        FastenerType.BANJO_BOLT -> setOf(ProductStandard.DIN_7623, ProductStandard.ASME_B18_2_1)
        FastenerType.NAS_CLOSE_TOL_BOLT -> setOf(ProductStandard.NAS6203, ProductStandard.EN2882)
        FastenerType.HI_LOK_PIN -> setOf(ProductStandard.HI_LOK)
        FastenerType.TAPER_LOK -> setOf(ProductStandard.TAPER_LOK_STD)
        FastenerType.LOCKBOLT_PIN -> setOf(ProductStandard.LOCKBOLT_STD)
        FastenerType.BLIND_BOLT -> setOf(ProductStandard.BLIND_BOLT_STD)
        FastenerType.SOLID_RIVET -> setOf(ProductStandard.MS20470)
        FastenerType.BLIND_RIVET -> setOf(ProductStandard.NAS1398)
        FastenerType.STUD -> setOf(ProductStandard.ASTM_A193, ProductStandard.AS1150)
        FastenerType.WHEEL_STUD, FastenerType.U_BOLT -> setOf(ProductStandard.ASTM_A193)
        FastenerType.HEX_NUT -> setOf(ProductStandard.ISO_4032, ProductStandard.ISO_4033, ProductStandard.ASME_B18_2_2)
        FastenerType.HEAVY_HEX_NUT -> setOf(ProductStandard.ASTM_A194, ProductStandard.ASME_B18_2_2)
        FastenerType.FLANGE_NUT -> setOf(ProductStandard.DIN_6923, ProductStandard.ASME_B18_2_2)
        FastenerType.NYLOC_NUT -> setOf(ProductStandard.ISO_7040, ProductStandard.ASME_B18_2_2)
        FastenerType.METAL_LOCK_NUT -> setOf(ProductStandard.ISO_7042, ProductStandard.MS21042, ProductStandard.ASME_B18_2_2)
        FastenerType.CASTLE_NUT -> setOf(ProductStandard.DIN_935, ProductStandard.MS20500)
        FastenerType.ACORN_NUT -> setOf(ProductStandard.DIN_1587, ProductStandard.ASME_B18_2_2)
        FastenerType.WELD_NUT -> setOf(ProductStandard.DIN_929, ProductStandard.ASME_B18_2_2)
        FastenerType.WHEEL_NUT -> setOf(ProductStandard.DIN_74361_NUT, ProductStandard.ASME_B18_2_2)
        FastenerType.AERO_LOCK_NUT -> setOf(ProductStandard.MS21042)
        FastenerType.HI_LOK_COLLAR -> setOf(ProductStandard.HI_LOK_COLLAR_STD)
        FastenerType.NUTPLATE -> setOf(ProductStandard.MS21047)
        FastenerType.FLAT_WASHER -> setOf(ProductStandard.ISO_7089, ProductStandard.ISO_7093, ProductStandard.ASTM_F436)
        FastenerType.SPRING_WASHER -> setOf(ProductStandard.DIN_127, ProductStandard.ASTM_F436)
    }

    /** Standards written for this product in this unit system; the governing default is always offered. */
    fun standards(type: FastenerType, unit: UnitSystem): List<ProductStandard> {
        val default = PokaYokeEngine.defaultStandard(type, unit)
        val fits = typeStandards(type).filter {
            when (unit) {
                UnitSystem.INCH -> it.category != "ISO/DIN Metric"
                UnitSystem.METRIC -> it.category != "ASME/Imperial"
            }
        }
        return listOf(default) + ProductStandard.values().filter { it in fits && it != default }
    }

    private val RIVET_CLASSES = listOf(PropertyClass.AERO_AL, PropertyClass.AERO_1100, PropertyClass.AERO_1380)
    private val SAE_GRADES = setOf(PropertyClass.SAE_GRADE_2, PropertyClass.SAE_GRADE_5, PropertyClass.SAE_GRADE_8)
    private val ISO_STEEL_CLASSES = setOf(
        PropertyClass.CLASS_4_6, PropertyClass.CLASS_4_8, PropertyClass.CLASS_5_6, PropertyClass.CLASS_5_8, PropertyClass.CLASS_6_8,
        PropertyClass.CLASS_8_8, PropertyClass.CLASS_9_8, PropertyClass.CLASS_10_9, PropertyClass.CLASS_12_9
    )

    /** Property classes for the product, unit and size (ISO 898-1 for metric, SAE/ASTM for inch, 9.8 only to M16). */
    fun propertyClasses(type: FastenerType, unit: UnitSystem, dMm: Double): List<PropertyClass> {
        if (type.isUnthreaded) return RIVET_CLASSES
        return PropertyClass.values().filter { pc ->
            when {
                unit == UnitSystem.METRIC && pc in SAE_GRADES -> false
                unit == UnitSystem.METRIC && pc == PropertyClass.ASTM_A574 -> false
                unit == UnitSystem.INCH && pc in ISO_STEEL_CLASSES -> false
                pc == PropertyClass.CLASS_9_8 && dMm > 16.0 -> false
                else -> true
            }
        }
    }

    /** True when the gate raises no material-vs-class issue for this pairing. */
    fun materialFitsClass(material: MaterialInfo, pc: PropertyClass, dMm: Double): Boolean {
        val family = PokaYokeEngine.materialFamily(material)
        val cs = PokaYokeEngine.classStrength(pc, dMm, material)
        val familyOk = when {
            cs.isIsoSteelClass || pc in PokaYokeEngine.INCH_STEEL_CLASSES -> family == MaterialFamily.CARBON_ALLOY_STEEL
            pc in PokaYokeEngine.STAINLESS_CLASSES -> family == MaterialFamily.STAINLESS
            pc == PropertyClass.AERO_AL -> family == MaterialFamily.ALUMINUM
            pc == PropertyClass.AERO_1100 -> family == MaterialFamily.TITANIUM
            else -> true
        }
        if (!familyOk) return false
        val uts = material.postForgingTargetUtsMpa
        if (uts < cs.rmMinMpa * 0.98 || uts < pc.utsMpa * 0.98) return false
        if (pc in PokaYokeEngine.QT_CLASSES && family == MaterialFamily.CARBON_ALLOY_STEEL && !material.hasHighHardness) return false
        return true
    }

    fun materials(type: FastenerType, pc: PropertyClass, dMm: Double, all: Collection<MaterialInfo>): List<MaterialInfo> =
        if (!usesPropertyClass(type)) all.toList() else all.filter { materialFitsClass(it, pc, dMm) }

    fun threadClasses(unit: UnitSystem, type: FastenerType): List<String> = ThreadClassCatalog.getOptions(unit, type.isNut)

    fun coatingFits(coating: CoatingType, type: FastenerType, material: MaterialInfo, pc: PropertyClass, dMm: Double, threadClass: String): Boolean {
        val family = PokaYokeEngine.materialFamily(material)
        val effectiveRm = if (usesPropertyClass(type)) PokaYokeEngine.classStrength(pc, dMm, material).rmMinMpa else material.postForgingTargetUtsMpa
        val isZincBased = coating.name.startsWith("ZN") || coating.name.startsWith("ZINC") || coating == CoatingType.HOT_DIP_GALV
        val isCadmium = coating.name.contains("CAD")
        val steelLike = family == MaterialFamily.CARBON_ALLOY_STEEL || family == MaterialFamily.STAINLESS && material.hasHighHardness
        val is129 = usesPropertyClass(type) && pc == PropertyClass.CLASS_12_9
        return when {
            coating.name.startsWith("PASSIVATION") && family != MaterialFamily.STAINLESS -> false
            family == MaterialFamily.TITANIUM && (isCadmium || coating.isElectroplating) -> false
            coating in PokaYokeEngine.ALUMINIUM_ONLY_COATINGS && family != MaterialFamily.ALUMINUM -> false
            coating == CoatingType.TI_ANODIZE && family != MaterialFamily.TITANIUM -> false
            coating.isElectroplating && steelLike && (is129 || effectiveRm >= 1200.0) -> false
            // Wrong-material warnings: sacrificial zinc on corrosion-resistant alloys, zinc/cadmium on aluminium, cadmium outside aerospace
            isZincBased && (family == MaterialFamily.STAINLESS || family == MaterialFamily.NICKEL_SUPERALLOY) -> false
            family == MaterialFamily.ALUMINUM && (isZincBased || isCadmium) -> false
            isCadmium && !material.isAero -> false
            coating == CoatingType.HOT_DIP_GALV && (is129 || effectiveRm >= 1200.0 || dMm < 8.0) -> false
            coating == CoatingType.HOT_DIP_GALV && !type.isWasher && !type.isNut && !type.isUnthreaded &&
                !threadClass.contains("e") && !threadClass.contains("1A") -> false
            else -> true
        }
    }

    fun coatings(type: FastenerType, material: MaterialInfo, pc: PropertyClass, dMm: Double, threadClass: String): List<CoatingType> {
        val fits = CoatingType.values().filter { coatingFits(it, type, material, pc, dMm, threadClass) }
        val preferred = when (PokaYokeEngine.materialFamily(material)) {
            MaterialFamily.STAINLESS -> CoatingType.PASSIVATION
            MaterialFamily.TITANIUM -> CoatingType.TI_ANODIZE
            MaterialFamily.ALUMINUM -> CoatingType.ANODIZE_II
            else -> null
        }
        return if (preferred != null && preferred in fits) listOf(preferred) + (fits - preferred) else fits
    }

    /**
     * Returns the selection with every field that no longer fits the fields above it replaced.
     * A material that does not fit the class first tries a class that fits the material (drawing scans,
     * the alloy catalogue and saved sessions set the material directly); only then is the material changed.
     */
    fun normalize(sel: Selection, catalogue: Map<String, MaterialInfo>): Selection {
        var s = sel
        val stds = standards(s.type, s.unit)
        if (s.standard !in stds) s = s.copy(standard = stds.first())

        val classes = propertyClasses(s.type, s.unit, s.dMm)
        val material = catalogue[s.materialKey]
        if (s.propertyClass !in classes) {
            val fitting = material?.let { m -> classes.filter { materialFitsClass(m, it, s.dMm) } }.orEmpty()
            s = s.copy(propertyClass = closestStrength(fitting, s.propertyClass) ?: classes.first())
        }

        // An unknown key (custom alloy not restored yet) is left alone so the screen can warn about it
        if (material != null && usesPropertyClass(s.type) && !materialFitsClass(material, s.propertyClass, s.dMm)) {
            val fitting = classes.filter { materialFitsClass(material, it, s.dMm) }
            val pc = closestStrength(fitting, s.propertyClass)
            s = if (pc != null) {
                s.copy(propertyClass = pc)
            } else {
                val mats = materials(s.type, s.propertyClass, s.dMm, catalogue.values)
                val key = mats.firstOrNull { it.key == s.propertyClass.defaultMatKey }?.key ?: mats.firstOrNull()?.key
                if (key != null) s.copy(materialKey = key) else s
            }
        }

        val tcs = threadClasses(s.unit, s.type)
        if (s.threadClass !in tcs) s = s.copy(threadClass = tcs.first())

        val mat = catalogue[s.materialKey]
        if (mat != null) {
            val cs = coatings(s.type, mat, s.propertyClass, s.dMm, s.threadClass)
            if (s.coating !in cs && cs.isNotEmpty()) s = s.copy(coating = cs.first())
        }
        return s
    }

    private fun closestStrength(options: List<PropertyClass>, target: PropertyClass): PropertyClass? =
        options.minByOrNull { kotlin.math.abs(it.utsMpa - target.utsMpa) }

    /** Material to pair with a newly chosen class: keep the current one if it fits, else the class default, else the first fit. */
    fun materialForClass(type: FastenerType, pc: PropertyClass, dMm: Double, currentKey: String, catalogue: Map<String, MaterialInfo>): String {
        val mats = materials(type, pc, dMm, catalogue.values)
        return mats.firstOrNull { it.key == currentKey }?.key
            ?: mats.firstOrNull { it.key == pc.defaultMatKey }?.key
            ?: mats.firstOrNull()?.key
            ?: currentKey
    }
}
