package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import kotlin.math.abs

/** CRITICAL blocks release, MANDATORY is a required process control for the traveller, WARNING needs engineering sign-off. */
enum class PokaYokeSeverity { CRITICAL, MANDATORY, WARNING }

@Serializable
data class PokaYokeIssue(
    val severity: PokaYokeSeverity,
    val title: String,
    val detail: String,
    val fix: String
)

enum class MaterialFamily { CARBON_ALLOY_STEEL, STAINLESS, TITANIUM, NICKEL_SUPERALLOY, ALUMINUM, COPPER_ALLOY }

/** ISO 898-1 / ISO 3506 minimum mechanical properties used for proof and breaking loads. */
@Serializable
data class ClassStrength(val rmMinMpa: Double, val yieldMinMpa: Double, val proofStressMpa: Double, val isIsoSteelClass: Boolean)
