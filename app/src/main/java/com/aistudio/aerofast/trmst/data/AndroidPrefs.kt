package com.aistudio.aerofast.trmst.data

import android.content.Context

// Android storage for the shop settings; the settings objects themselves are platform-free.

fun MachineSetupStore.load(context: Context) {
    val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun g(k: String) = p.getFloat(k, 0f).toDouble().takeIf { it > 0.0 }
    setup = MachineSetup(g(KEYS[0]), g(KEYS[1]), g(KEYS[2]), g(KEYS[3]), g(KEYS[4]), g(KEYS[5]), g(KEYS[6]), g(KEYS[7]), g(KEYS[8]))
}

fun MachineSetupStore.save(context: Context, s: MachineSetup) {
    setup = s
    val values = listOf(s.dieCaseOdMm, s.dieCaseLengthMm, s.punchCaseOdMm, s.punchCaseLengthMm, s.koStrokeMm, s.insertOdMm, s.openExtrusionLimitPct, s.dieAngleDeg, s.landLengthMm)
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
        KEYS.zip(values).forEach { (k, v) -> putFloat(k, (v ?: 0.0).toFloat()) }
    }.apply()
}

fun ShopDefaults.load(context: Context) {
    val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    materialKey = p.getString("material", ShopDefaults.FACTORY_MATERIAL) ?: ShopDefaults.FACTORY_MATERIAL
    propertyClass = runCatching { PropertyClass.valueOf(p.getString("class", null)!!) }.getOrDefault(ShopDefaults.FACTORY_CLASS)
    coating = runCatching { CoatingType.valueOf(p.getString("coating", null)!!) }.getOrDefault(ShopDefaults.FACTORY_COATING)
    batchQty = p.getInt("batch", ShopDefaults.FACTORY_BATCH).takeIf { it > 0 } ?: ShopDefaults.FACTORY_BATCH
}

fun ShopDefaults.save(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("material", materialKey)
        .putString("class", propertyClass.name)
        .putString("coating", coating.name)
        .putInt("batch", batchQty)
        .apply()
}
