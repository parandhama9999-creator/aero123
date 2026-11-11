package com.example.data

/**
 * Selection helpers for the Sizing card. The selectors list every option; the PokaYokeEngine gate flags a
 * combination that does not fit, and these helpers only pick sensible defaults.
 */
object SelectionFilter {

    fun hasThread(type: FastenerType) = !type.isUnthreaded && !type.isWasher

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
