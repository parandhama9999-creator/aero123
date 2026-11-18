package com.aistudio.aerofast.trmst.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The plant's own standing requirement: used for anything a drawing does not state, and as the
 * starting selection on a fresh install. Editable on the AI Scanner screen.
 */
object ShopDefaults {
    const val FACTORY_MATERIAL = "4135"
    val FACTORY_CLASS = PropertyClass.CLASS_10_9
    val FACTORY_COATING = CoatingType.ZN_NI
    const val FACTORY_BATCH = 10000

    private const val PREFS = "aerofast_shop_defaults"

    var materialKey by mutableStateOf(FACTORY_MATERIAL)
    var propertyClass by mutableStateOf(FACTORY_CLASS)
    var coating by mutableStateOf(FACTORY_COATING)
    var batchQty by mutableIntStateOf(FACTORY_BATCH)

    /** A material that exists in the catalogue: a deleted custom alloy falls back to the factory default. */
    val material: MaterialInfo
        get() = FastenerCatalog.MATERIALS[materialKey] ?: FastenerCatalog.MATERIALS.getValue(FACTORY_MATERIAL)

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        materialKey = p.getString("material", FACTORY_MATERIAL) ?: FACTORY_MATERIAL
        propertyClass = runCatching { PropertyClass.valueOf(p.getString("class", null)!!) }.getOrDefault(FACTORY_CLASS)
        coating = runCatching { CoatingType.valueOf(p.getString("coating", null)!!) }.getOrDefault(FACTORY_COATING)
        batchQty = p.getInt("batch", FACTORY_BATCH).takeIf { it > 0 } ?: FACTORY_BATCH
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("material", materialKey)
            .putString("class", propertyClass.name)
            .putString("coating", coating.name)
            .putInt("batch", batchQty)
            .apply()
    }
}
