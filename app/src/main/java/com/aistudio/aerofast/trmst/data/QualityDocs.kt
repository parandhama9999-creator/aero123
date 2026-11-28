package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import java.util.Locale

/** One line of a control plan. [special] marks a characteristic the customer usually flags; the symbol is theirs to set. */
@Serializable
data class ControlPlanRow(
    val step: String,
    val characteristic: String,
    val isProduct: Boolean,
    val special: Boolean,
    val spec: String,
    val method: String,
    val sampling: String,
    val control: String,
    val reaction: String
)

@Serializable
data class PpapElement(val no: Int, val name: String, val levels: String, val forThisPart: String)

@Serializable
data class ChecklistPhase(val title: String, val items: List<Pair<String, String>>)
