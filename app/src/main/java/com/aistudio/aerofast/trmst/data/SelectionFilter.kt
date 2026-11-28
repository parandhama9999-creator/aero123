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
