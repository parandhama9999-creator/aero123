package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** User-entered alloy properties, validated before they can enter the catalogue. */
data class CustomMaterialInput(
    val key: String,
    val name: String,
    val utsMpa: Double,
    val yieldMpa: Double,
    val density: Double,
    val km: Double,
    val vc: Double,
    val isAero: Boolean,
    val hardness: String,
    val standard: String
)

/**
 * Validates and persists custom alloys so a saved part never silently falls back to another material after a restart.
 */
object CustomMaterialStore {
    private const val PREFS = "aerofast_custom_materials"
    private const val KEY = "materials_v1"

    /** Returns a list of problems; empty means the input is safe to register. */
    fun validate(input: CustomMaterialInput): List<String> {
        val errors = mutableListOf<String>()
        if (!Regex("^[A-Z0-9_]{2,20}$").matches(input.key)) errors += "Alloy code: 2–20 characters, A–Z, 0–9 or _"
        if (input.key in FastenerCatalog.DEFAULT_MATERIALS.keys) errors += "Alloy code ${input.key} is a built-in material and cannot be overwritten"
        if (input.name.isBlank()) errors += "Enter the full material name"
        if (input.utsMpa !in 100.0..2500.0) errors += "UTS must be 100–2500 MPa"
        if (input.yieldMpa <= 0.0 || input.yieldMpa >= input.utsMpa) errors += "Yield must be above 0 and below UTS"
        else if (input.yieldMpa < 0.3 * input.utsMpa) errors += "Yield below 30% of UTS is not realistic for a fastener alloy"
        if (input.density !in 1.5..20.0) errors += "Density must be 1.5–20 g/cm³"
        if (input.km !in 0.0..0.05) errors += "km blank factor must be 0–0.05"
        if (input.vc !in 5.0..600.0) errors += "Cutting speed Vc must be 5–600 m/min"
        if (input.standard.isBlank()) errors += "Enter the material/heat-treatment specification"
        return errors
    }

    fun build(input: CustomMaterialInput): MaterialInfo = MaterialInfo(
        key = input.key,
        name = input.name.trim(),
        density = input.density,
        uts = input.utsMpa,
        vc = input.vc,
        feed = if (input.isAero) 0.12 else 0.18,
        doc = if (input.isAero) 0.8 else 1.5,
        rateInr = 500.0,
        km = input.km,
        isAero = input.isAero,
        hasHighHardness = input.utsMpa >= 1000.0,
        headingSpm = if (input.isAero) 60 else 100,
        insertSpec = if (input.isAero) "CNMG 120408-SM (ISO S)" else "CNMG 120408-PM (ISO P)",
        heatTreatment = "Per ${input.standard} (custom alloy - define the cycle on the traveller).",
        lubricationCoating = "Per material supplier recommendation.",
        dieTooling = "GT55 Carbide + ASP30 Punch.",
        rawMaterialSourcingUtsMpa = "Per ${input.standard}",
        postForgingTargetUtsMpa = input.utsMpa,
        yieldStrengthMpa = input.yieldMpa,
        minElongationPct = 12.0,
        reductionOfAreaZPct = 40.0,
        flowStressCoeffK = input.utsMpa * 1.15,
        strainHardeningN = 0.15,
        // No invented temperatures: a custom alloy's cycle must come from its specification
        heatTreatmentSpec = HeatTreatmentSpec(
            austenitizingOrSolutionTempC = "Per ${input.standard}",
            soakingTimeMinutes = "Per ${input.standard}",
            quenchMedium = "Per ${input.standard}",
            temperingOrAgingTempC = "Per ${input.standard}",
            temperingOrAgingTimeHours = "Per ${input.standard}",
            targetHardnessHrc = input.hardness.ifBlank { "Per ${input.standard}" },
            hydrogenBakingRequired = input.utsMpa >= 1000.0,
            hydrogenBakingCycle = "190°C – 220°C for 8 to 24 hrs if electroplated (ISO 4042)",
            standardReference = input.standard,
            preheatTempC = "Per ${input.standard}",
            preheatSoakTime = "Per ${input.standard}",
            atmosphere = "Per ${input.standard}",
            quenchDetails = "Per ${input.standard}",
            coolingMethod = "Per ${input.standard}",
            targetUtsMpa = "${input.utsMpa.toInt()} MPa"
        )
    )

    fun save(context: Context, input: CustomMaterialInput) {
        val all = loadInputs(context).filter { it.key != input.key } + input
        val arr = JSONArray()
        all.forEach { m ->
            arr.put(JSONObject().apply {
                put("key", m.key); put("name", m.name); put("uts", m.utsMpa); put("yield", m.yieldMpa)
                put("density", m.density); put("km", m.km); put("vc", m.vc); put("aero", m.isAero)
                put("hardness", m.hardness); put("standard", m.standard)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    private fun loadInputs(context: Context): List<CustomMaterialInput> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CustomMaterialInput(
                    key = o.getString("key"), name = o.getString("name"), utsMpa = o.getDouble("uts"),
                    yieldMpa = o.getDouble("yield"), density = o.getDouble("density"), km = o.getDouble("km"),
                    vc = o.getDouble("vc"), isAero = o.optBoolean("aero", false),
                    hardness = o.optString("hardness", ""), standard = o.optString("standard", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private const val PRESET_KEYS = "preset_keys_v1"

    /** Remembers a pre-engineered preset alloy the user registered from the catalogue. */
    fun savePreset(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val keys = prefs.getStringSet(PRESET_KEYS, emptySet()).orEmpty() + key
        prefs.edit().putStringSet(PRESET_KEYS, keys).apply()
    }

    /** Puts every saved custom alloy and preset back into the live catalogue (call before restoring a saved part). */
    fun restoreInto(context: Context) {
        loadInputs(context).forEach { FastenerCatalog.addMaterial(build(it)) }
        val presetKeys = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(PRESET_KEYS, emptySet()).orEmpty()
        FastenerCatalog.getAvailableCustomCandidates().filter { it.key in presetKeys }.forEach { FastenerCatalog.addMaterial(it) }
    }
}
