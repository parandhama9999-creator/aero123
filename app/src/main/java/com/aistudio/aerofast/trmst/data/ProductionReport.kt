package com.aistudio.aerofast.trmst.data

import kotlinx.serialization.Serializable

import java.util.Locale

/** Key production figures for one part, shared by the scanner summary card and the full report. */
@Serializable
data class ProductionSummary(
    val batchQty: Int,
    val wireDiaMm: Double,
    val cutOffLengthMm: Double,
    val finishedWeightG: Double,
    val slugWeightG: Double,
    val finishedKgPer1000: Double,
    val batchFinishedKg: Double,
    val batchRawMaterialKg: Double,
    val piecesPerTonne: Int,
    val plan: RawMaterialPlan?
) {
    val scrapPerPieceG get() = (slugWeightG - finishedWeightG).coerceAtLeast(0.0)
    val scrapPct get() = if (slugWeightG > 0) scrapPerPieceG / slugWeightG * 100.0 else 0.0
}
