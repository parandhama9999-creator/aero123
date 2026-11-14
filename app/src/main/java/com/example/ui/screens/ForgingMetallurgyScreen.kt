package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import com.example.data.FastenerType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FastenerCalculationResult
import com.example.data.HeadingPlan
import com.example.data.HeadingProgression
import com.example.data.HeadingStation
import com.example.data.ProfileSegment
import com.example.data.SectionShape
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import kotlin.math.min
import com.example.ui.components.PokaYokePanel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LocalCustomColors
import com.example.ui.theme.SuccessGreen

@Composable
fun ForgingMetallurgyScreen(
    result: FastenerCalculationResult,
    modifier: Modifier = Modifier,
) {
    val customColors = LocalCustomColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Strict poka-yoke gate: every rule checked against the governing standards
            PokaYokePanel(issues = result.pokaYokeIssues)

        // Crack Risk & Formability Prediction Engine Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentPrimary.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "Crack Risk Prediction",
                        tint = customColors.accentPrimary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "AUTOMATED CRACK RISK & FORMABILITY PREDICTION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.textMain
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (result.forgingRisks.isNotEmpty()) {
                    Text(
                        text = "Evaluated Risks:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = customColors.accentSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    result.forgingRisks.forEach { risk ->
                        Text(
                            text = "• $risk",
                            fontSize = 10.sp,
                            color = customColors.textMain,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                if (result.forgingRecommendations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tooling & Process Recommendations:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    result.forgingRecommendations.forEach { rec ->
                        Text(
                            text = "✔ $rec",
                            fontSize = 10.sp,
                            color = customColors.textMuted,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }

        // Section 1: Forging UTS & Mechanical Specifications
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = "UTS Specs",
                        tint = customColors.accentSecondary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "FORGING UTS & MECHANICAL SOURCING SPECS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.textMain
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Raw Material Sourcing UTS vs Post-HT Target UTS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Raw Wire Sourcing UTS (MTC):",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMuted
                            )
                            Text(
                                text = result.rawMaterialSourcingUts,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.accentSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Cold heading ductility requirement",
                                fontSize = 9.sp,
                                color = customColors.textMuted
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (result.fastenerType.isNut) "Mating Bolt Min UTS:" else "Finished Fastener Target UTS:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMuted
                            )
                            Text(
                                text = "${(if (result.fastenerType.isWasher) result.material.postForgingTargetUtsMpa else result.postForgingTargetUtsMpa).toInt()} MPa (Min)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = SuccessGreen,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Yield: ${result.yieldStrengthMpa.toInt()} MPa",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.textMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4 Engineering Mechanical Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricBox(
                        title = "Flow Stress (σf)",
                        value = "${result.flowStressMpa.toInt()} MPa",
                        subtitle = "During heading",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Tensile Area (As)",
                        value = "${result.stressAreaAsMm2} mm²",
                        subtitle = "ISO 898-1 root",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Proof Load (Fp)",
                        value = "${result.proofLoadKn} kN",
                        subtitle = "Non-destructive",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Breaking Load (Fm)",
                        value = "${result.tensileBreakingLoadKn} kN",
                        subtitle = "Tensile test min",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ductility Limits
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Elongation (A): ${if (result.minElongationPct > 0) "min ${result.minElongationPct.toInt()}%" else "not specified (full-size test)"}  •  Reduction of Area (Z): ${if (result.reductionOfAreaZPct > 0) "min ${result.reductionOfAreaZPct.toInt()}%" else "not specified"}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = customColors.textMuted
                    )
                }
            }
        }

        // Wire preparation: soft anneal happens BEFORE cut-off and heading
        SoftAnnealCard(result = result)

        // 5-Station Cold Heading Die Progression Canvas
        ColdHeadingProgressionCanvas(result = result)

        // Section 2: Detailed Heat Treatment Cycles
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = "Heat Treatment",
                            tint = customColors.accentPrimary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Column {
                            Text(
                                text = "STAGE-BY-STAGE HEAT TREATMENT SCHEDULE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = customColors.textMain
                            )
                            Text(
                                text = "Dynamic Soaking Times for Ø${result.nominalDiaMm} mm • ${result.material.name}",
                                fontSize = 9.sp,
                                color = customColors.textMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = customColors.accentPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = result.heatTreatmentSpec.standardReference,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = customColors.accentPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Only the stages this alloy's cycle actually has: a cold-formed class has no austenitize or quench,
                // and aluminium / titanium / nickel alloys are solution treated and aged, not austenitized and tempered.
                val ht = result.heatTreatmentSpec
                fun given(v: String) = v.isNotBlank() && !v.trim().startsWith("N/A")
                val hardened = given(ht.austenitizingOrSolutionTempC)
                val family = com.example.data.PokaYokeEngine.materialFamily(result.material)
                val quenchAndTemper = family == com.example.data.MaterialFamily.CARBON_ALLOY_STEEL ||
                    result.material.key in MARTENSITIC_STAINLESS
                var stageNo = 0
                fun stage(title: String): String { stageNo++; return "Stage $stageNo: $title" }

                if (given(ht.preheatTempC)) {
                    HtStageCard(
                        stage = stage("Pre-Heating / Stress Relief"),
                        temperature = ht.preheatTempC,
                        soakTime = ht.preheatSoakTime.ifBlank { "—" },
                        subDetail = "Thermal equalization to relieve cold heading stress"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (hardened) {
                    HtStageCard(
                        stage = stage(if (quenchAndTemper && !ht.isAging) "Austenitizing" else "Solution Treatment"),
                        temperature = ht.austenitizingOrSolutionTempC,
                        soakTime = ht.soakingTimeMinutes.ifBlank { "—" },
                        subDetail = "Atmosphere: ${ht.atmosphere.ifBlank { "per ${ht.standardReference}" }}"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (given(ht.quenchMedium)) {
                        HtStageCard(
                            stage = stage("Quenching Protocol"),
                            temperature = ht.quenchMedium,
                            soakTime = "Quench",
                            subDetail = ht.quenchDetails
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                } else {
                    HtStageCard(
                        stage = stage("Hardening"),
                        temperature = ht.austenitizingOrSolutionTempC.ifBlank { "Not hardened" },
                        soakTime = "—",
                        subDetail = "Strength comes from cold working: no austenitize or quench"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (given(ht.temperingOrAgingTempC)) {
                    HtStageCard(
                        stage = stage(when {
                            !hardened -> "Stress Relief (optional)"
                            quenchAndTemper && !ht.isAging -> "Tempering Cycle"
                            ht.isAging -> "Precipitation Aging"
                            else -> "Aging / Post-Treatment"
                        }),
                        temperature = ht.temperingOrAgingTempC,
                        soakTime = ht.temperingOrAgingTimeHours.ifBlank { "—" },
                        subDetail = "Target Hardness: ${ht.targetHardnessHrc}"
                    )
                } else {
                    HtStageCard(stage = stage("Target Hardness"), temperature = ht.targetHardnessHrc, soakTime = "—")
                }

                Spacer(modifier = Modifier.height(4.dp))
                if (result.hasHydrogenEmbrittlementRisk) {
                    HtStageCard(
                        stage = stage("Mandatory Hydrogen De-Embrittlement Baking"),
                        temperature = ht.bakingTempC,
                        soakTime = ht.bakingSoakTime,
                        subDetail = ht.hydrogenBakingCycle,
                        isWarning = true
                    )
                } else {
                    HtStageCard(
                        stage = stage("Hydrogen De-Embrittlement Baking"),
                        temperature = "Not required",
                        soakTime = "—",
                        subDetail = "Exempt for ${result.coating.label} coating on ${result.material.name}"
                    )
                }

                // Hours on every stage, worked out from the section size the specs actually use
                com.example.data.HeatTreatCycleEngine
                    .quenchTemper(result.material, result.wireDiaMm)
                    ?.let { cycle ->
                        CycleStagesBlock(
                            title = "COMPLETE CYCLE \u2014 DOOR TO DOOR",
                            stages = cycle.stages,
                            totalText = cycle.totalText,
                            basis = cycle.basis,
                            cautions = cycle.cautions,
                            sources = cycle.sources
                        )
                    }
            }
        }

        // Section 3: 5-Station Progressive Forging Progression & Tonnage
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Forging Progression",
                        tint = customColors.accentSecondary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = result.headingPlan?.let { "CUT-OFF + ${it.dieStations}-DIE PROGRESSIVE FORGING & TONNAGE" }
                            ?: "PRESS BLANKING SEQUENCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.textMain
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                result.forgingStages.forEach { stage ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = customColors.accentPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.padding(end = 10.dp)
                            ) {
                                Text(
                                    text = "STG ${stage.stageNumber}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = customColors.accentPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stage.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = customColors.textMain
                                )
                                Text(
                                    text = stage.description,
                                    fontSize = 10.sp,
                                    color = customColors.textMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stats: Area Reduction, Tonnage, Machine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Extrusion Area Reduction", fontSize = 9.sp, color = customColors.textMuted)
                            Text(
                                text = "${result.areaReductionPct}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.accentPrimary
                            )
                            Text(
                                text = "Ø${result.wireDiaMm} → Ø${result.blankTargetDia}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.textMuted
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Required Heading Force", fontSize = 9.sp, color = customColors.textMuted)
                            Text(
                                text = "${result.forgingTonnage} T",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.accentSecondary
                            )
                            Text(
                                text = "Metric Tons Compressive",
                                fontSize = 9.sp,
                                color = customColors.textMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Recommended Machine
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Recommended Machine Cadence:", fontSize = 9.sp, color = customColors.textMuted)
                        Text(
                            text = result.recommendedMachine,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                }
            }
        }

        // Section 4: Die Tooling & Surface Lubrication
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = "Tooling",
                        tint = customColors.accentPrimary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "DIE TOOLING STEELS & LUBRICANTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.textMain
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Die & Punch Steels: ${result.material.dieTooling}",
                    fontSize = 11.sp,
                    color = customColors.textMain,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Heading lubricant: ${result.material.lubricationCoating}  •  Final coating: ${result.coating.label}",
                    fontSize = 11.sp,
                    color = customColors.textMuted
                )
            }
        }

        // Section 5: Shop-Floor Process Routes (Verified Master Sheet Data)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Process Routes",
                        tint = customColors.accentSecondary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "SHOP-FLOOR PROCESS FLOW & METALLURGICAL ROUTES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.textMain
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                com.example.data.ShopFloorData.PROCESS_ROUTES.forEach { route ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.bgElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = route.categoryName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.accentSecondary
                            )
                            Text(
                                text = "Applicable Grades: ${route.applicableGrades}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = customColors.textMain,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Behavior: ${route.metallurgicalBehavior}",
                                fontSize = 9.sp,
                                color = customColors.textMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sequence: ${route.sequenceSteps}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.accentPrimary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Poka-Yoke Gate: ${route.pokaYokeQualityGate}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlertRed,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 6: Hardness Failure Diagnostics & Troubleshooting
        HardnessTroubleshootingCard(
            targetHrcText = result.heatTreatmentSpec.targetHardnessHrc,
            route = when {
                result.heatTreatmentSpec.austenitizingOrSolutionTempC.let { it.isBlank() || it.trim().startsWith("N/A") } -> HardnessRoute.COLD_WORKED
                com.example.data.PokaYokeEngine.materialFamily(result.material) == com.example.data.MaterialFamily.CARBON_ALLOY_STEEL ||
                    result.material.key in MARTENSITIC_STAINLESS -> HardnessRoute.QUENCH_TEMPER
                else -> HardnessRoute.SOLUTION_AGE
            },
            standardReference = result.heatTreatmentSpec.standardReference
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Section 7: Customer Spec Compliance & Feasibility Checker
        CustomerSpecFeasibilityCard(material = result.material)
    }
}

private val MARTENSITIC_STAINLESS = setOf("SS410", "20X13", "30X13", "14KH17N2")

private val HRC_RANGE = Regex("""(\d+(?:\.\d+)?)\s*(?:[–-]\s*(\d+(?:\.\d+)?))?\s*HRC""")

/** Highest HRC of the alloy's catalogue heat treatment; null when its hardness is only in HRB / HV / HBW (below the HRC scale). */
internal fun maxCatalogueHrc(m: com.example.data.MaterialInfo): Double? =
    HRC_RANGE.findAll(m.heatTreatmentSpec.targetHardnessHrc)
        .map { (it.groupValues[2].ifEmpty { it.groupValues[1] }).toDouble() }
        .maxOrNull()

private fun hrcText(v: Double) = if (v % 1.0 == 0.0) "${v.toInt()}" else "$v"

@Composable
fun CustomerSpecFeasibilityCard(material: com.example.data.MaterialInfo) {
    val customColors = LocalCustomColors.current
    var customerTargetHrc by remember { mutableStateOf(45) }
    val materialName = material.name
    val maxHrc = maxCatalogueHrc(material)
    val reach = maxHrc?.let { "up to ${hrcText(it)} HRC" } ?: "below the HRC scale (${material.heatTreatmentSpec.targetHardnessHrc})"
    val alternatives = com.example.data.FastenerCatalog.MATERIALS.values
        .mapNotNull { m -> maxCatalogueHrc(m)?.let { m to it } }
        .filter { it.second >= customerTargetHrc }
        .sortedBy { it.second }
        .take(3)
        .joinToString { "${it.first.name} (${hrcText(it.second)} HRC)" }
        .ifEmpty { "no catalogue alloy reaches it" }
    val isFeasible = maxHrc != null && customerTargetHrc <= maxHrc

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = customColors.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFeasible) SuccessGreen.copy(alpha = 0.5f) else AlertRed.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Feasibility Check",
                    tint = if (isFeasible) SuccessGreen else AlertRed,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "CUSTOMER SPEC COMPLIANCE & FEASIBILITY CHECKER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = customColors.textMain
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Customer target: $customerTargetHrc HRC  •  $materialName reaches $reach with its catalogue heat treatment (${material.heatTreatmentSpec.standardReference})",
                fontSize = 10.sp,
                color = customColors.textMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(35, 40, 45, 50, 53).forEach { hrc ->
                    val isSelected = customerTargetHrc == hrc
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) customColors.accentPrimary else customColors.bgElevated,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { customerTargetHrc = hrc }
                    ) {
                        Text(
                            text = "$hrc HRC",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else customColors.textMain,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isFeasible) SuccessGreen.copy(alpha = 0.12f) else AlertRed.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isFeasible) SuccessGreen else AlertRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (isFeasible) {
                        Text(text = "✔ METALLURGICALLY FEASIBLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        Text(text = "$customerTargetHrc HRC is inside the catalogue range of $materialName ($reach). Its standard hardening / aging cycle applies.", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                    } else {
                        Text(text = "⚠️ METALLURGICAL IMPOSSIBILITY / SPEC VIOLATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                        Text(text = "• $materialName reaches $reach; forcing it to $customerTargetHrc HRC risks embrittlement and cracking.\n• Raise an engineering deviation or reject, or change to an alloy that reaches it: $alternatives.", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

/** How the part gets its hardness, which decides what a failed hardness test points to. */
enum class HardnessRoute { QUENCH_TEMPER, SOLUTION_AGE, COLD_WORKED }

private data class HardnessFailure(val label: String, val rootCause: String, val actions: String)

private fun hardnessFailures(route: HardnessRoute, standardReference: String): List<HardnessFailure> = when (route) {
    HardnessRoute.QUENCH_TEMPER -> listOf(
        HardnessFailure("Low Hardness", "Slack Quenching / Slow Cooling Rate",
            "• Check quench oil temperature (must be 40–60°C).\n• Verify oil agitation pump and circulation.\n• Increase austenitizing temp by 10–20°C to ensure full carbide dissolution."),
        HardnessFailure("Soft Surface", "Surface Decarburization (Soft Skin)",
            "• Grind 0.2mm off surface and re-test core hardness.\n• If core is hard, raise the furnace C-potential to match the steel carbon content (see atmosphere spec above) — never above it (carburizing is forbidden by ISO 898-1).\n• Check endothermic generator gas dew point."),
        HardnessFailure("Over-Tempered", "Over-Tempering (Temp Set Too High)",
            "• Recalibrate tempering furnace thermocouple.\n• Over-tempered parts must be re-austenitized, re-quenched, and re-tempered at lower temperature.")
    )
    HardnessRoute.SOLUTION_AGE -> listOf(
        HardnessFailure("Low Hardness", "Incomplete Solution Treatment or Under-Aging",
            "• Verify solution temperature and soak against $standardReference.\n• Check quench delay and quench rate (aluminium is very sensitive to quench delay).\n• Verify aging temperature and time; under-aged parts can be given the remaining aging time."),
        HardnessFailure("Surface Defect", "Surface Reaction During Heat Treatment",
            "• Check furnace vacuum level or argon purity and dew point.\n• Titanium picks up oxygen as a hard, brittle alpha case; nickel alloys oxidise. Remove the affected layer by an approved process and re-test.\n• Never heat titanium or nickel alloys in endothermic gas."),
        HardnessFailure("Over-Aged", "Aging Temperature or Time Too High",
            "• Recalibrate the aging furnace thermocouple.\n• Over-aged parts cannot be corrected by more aging: re-solution treat, quench and re-age per $standardReference.")
    )
    HardnessRoute.COLD_WORKED -> listOf(
        HardnessFailure("Low Hardness", "Too Little Cold Work or Soft Wire",
            "• Check the incoming wire UTS against the raw wire sourcing spec.\n• Check the drawing reduction and heading strain of the process.\n• A stress relief run too hot softens cold-formed parts: keep it within the stated range."),
        HardnessFailure("Soft Surface", "Decarburized Wire From the Mill",
            "• Take a micro-section and measure the decarburized depth.\n• Check the mill certificate; reject coils outside the decarburization limit of the product standard."),
        HardnessFailure("Too Hard", "Excess Cold Work, Stress Relief Skipped",
            "• Confirm the stress relief was run when the drawing requires it.\n• Check the wire was supplied in the specified (annealed) condition.")
    )
}

@Composable
fun HardnessTroubleshootingCard(targetHrcText: String, route: HardnessRoute, standardReference: String) {
    val customColors = LocalCustomColors.current
    val failures = hardnessFailures(route, standardReference)
    var selectedIndex by remember(route) { mutableStateOf(0) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = customColors.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Science,
                    contentDescription = "Troubleshooting",
                    tint = AlertRed,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "METALLURGICAL HARDNESS FAILURE DIAGNOSTICS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = customColors.textMain
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Target Hardness Spec: $targetHrcText. If actual test fails, select symptom:",
                fontSize = 10.sp,
                color = customColors.textMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                failures.forEachIndexed { index, failure ->
                    val isSelected = selectedIndex == index
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) AlertRed else customColors.bgElevated,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIndex = index }
                    ) {
                        Text(
                            text = failure.label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else customColors.textMain,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = customColors.bgElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    val f = failures[selectedIndex]
                    Text(text = "Root Cause: ${f.rootCause}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                    Text(text = f.actions, fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = customColors.bgElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(text = title, fontSize = 8.sp, color = customColors.textMuted, maxLines = 1)
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = customColors.textMain,
                maxLines = 1
            )
            Text(text = subtitle, fontSize = 8.sp, color = customColors.textMuted, maxLines = 1)
        }
    }
}

@Composable
private fun HtStageCard(
    stage: String,
    temperature: String,
    soakTime: String,
    subDetail: String = "",
    isWarning: Boolean = false
) {
    val customColors = LocalCustomColors.current
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isWarning) AlertRed.copy(alpha = 0.12f) else customColors.bgElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isWarning) AlertRed else customColors.border
        ),
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stage,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWarning) AlertRed else customColors.accentPrimary
                )
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = customColors.accentSecondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = soakTime,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = customColors.accentSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = temperature,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = customColors.textMain,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (subDetail.isNotBlank()) {
                Text(
                    text = subDetail,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = customColors.textMain,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}

@Composable
fun ColdHeadingProgressionCanvas(
    result: FastenerCalculationResult,
    modifier: Modifier = Modifier,
) {
    val customColors = LocalCustomColors.current
    val plan = result.headingPlan

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        zoomScale = (zoomScale * zoomChange).coerceIn(1f, 4f)
        if (zoomScale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f; offsetY = 0f
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = DieColors.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (plan == null) "NO HEADING PROGRESSION"
                        else "CUT-OFF + " + plan.machineDescription.uppercase(),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF38BDF8)
                )
                if (plan != null) {
                    Text(
                        text = "Peak blow: ${result.forgingTonnage} T",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFF59E0B)
                    )
                }
            }

            if (plan == null) {
                Text(
                    text = "Washers are blanked and pierced from strip on a progressive press; they are not cold-headed.",
                    fontSize = 10.sp,
                    color = customColors.textMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
                return@Column
            }

            if (plan.routeReason.isNotBlank()) {
                Text(
                    text = plan.routeReason,
                    fontSize = 9.sp,
                    color = customColors.textMuted,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            Text(
                text = "True scale (long shanks broken) · left half in section · shank held in die on the knock-out pin · a taper is dimensioned die end → punch end · edge radii, shear angle and socket cone shown typical",
                fontSize = 9.sp,
                color = customColors.textMuted,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // Size the canvas to the parts: a nut needs far less height than a long bolt
                val partAspect = (plan.stations.maxOf { it.heightMm } / plan.stations.maxOf { it.maxWidthMm }).toFloat()
                val column = minOf(maxWidth / plan.stations.size, maxWidth / 4)
                val canvasHeight = (column * 0.8f * partAspect + 58.dp).coerceIn(132.dp, 250.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(canvasHeight)
                        .clipToBounds()
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .transformable(state = transformState)
                    ) {
                        drawHeadingProgression(plan, textPx = 9.sp.toPx())
                    }

                    if (zoomScale > 1.05f) {
                        Text(
                            text = "${String.format("%.1f", zoomScale)}×",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        )
                    }
                }
            }

            if (zoomScale > 1.05f) {
                Text(
                    text = "Reset Zoom",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { zoomScale = 1f; offsetX = 0f; offsetY = 0f }
                        .padding(vertical = 2.dp, horizontal = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                plan.stations.forEach { st ->
                    Text(
                        text = "${stationCode(st)}  ${st.title} — L ${fmt(st.heightMm, 1)} · ${stationFeatureLabel(st)} · V ${fmt(st.volumeMm3, 0)} mm³",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (st.isPeakBlow) Color(0xFFF59E0B) else customColors.textMuted
                    )
                    // The bore this die is cut to, so the blank transfers in instead of jamming on the lip
                    st.dieBoreMm?.let { bore ->
                        val stock = st.inDieDiaMm ?: return@let
                        Text(
                            text = "     die bore Ø${fmt(bore, 2)} = stock Ø${fmt(stock, 2)} + ${fmt(bore - stock, 3)} stage clearance",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = customColors.textMuted
                        )
                    }
                }
                plan.offHeaderOps.forEach { op ->
                    Text(
                        text = "OFF  ${op.title} — ${op.operation}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = customColors.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                val volumeOk = plan.maxVolumeError < 0.005
                val scrap = plan.stations.sumOf { it.scrapMm3 }
                Text(
                    text = if (volumeOk) {
                        "✔ Volume conserved: slug ${fmt(plan.slugVolumeMm3, 0)} mm³ at every station" +
                            (if (scrap > 0) " (incl. ${fmt(scrap, 0)} mm³ pierce scrap)" else "")
                    } else {
                        "✖ Volume mismatch ${fmt(plan.maxVolumeError * 100, 2)} % between stations"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (volumeOk) Color(0xFF22C55E) else Color(0xFFEF4444)
                )
                Text(
                    text = "Head volume ${fmt(result.headVolumeMm3, 0)} mm³ · shank + thread ${fmt(result.shankVolumeMm3, 0)} mm³ · " +
                        "total part ${fmt(result.partVolumeMm3, 0)} mm³ · slug ${fmt(plan.slugVolumeMm3, 0)} mm³",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = customColors.textMain,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row {
                    val ldOk = result.upsetLdRatio <= HeadingProgression.SINGLE_BLOW_LD
                    Text(
                        text = "Head stock L/D: ${result.upsetLdRatio}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ldOk || plan.upsetBlows > 1) Color(0xFF22C55E) else Color(0xFFEF4444)
                    )
                    Text(
                        text = when {
                            plan.upsetBlows == 0 -> "  (no head upset)"
                            plan.upsetBlows == 1 -> "  (≤ 2.3: single blow)"
                            else -> "  (> 2.3: ${plan.upsetBlows - 1} cone pre-upset)"
                        },
                        fontSize = 9.sp,
                        color = customColors.textMuted
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Ø${result.wireDiaMm}×${result.cutOffLengthMm}mm slug",
                        fontSize = 9.sp,
                        color = customColors.textMuted
                    )
                }
                plan.notes.forEach { note ->
                    Text(text = "⚠ $note", fontSize = 9.sp, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

private object DieColors {
    val background = Color(0xFF0F172A)
    val metal = Color(0xFF8FA3BF)
    val wire = Color(0xFFA8B3C2)
    val cutFace = Color(0xFFC3D0E0)
    val hatch = Color(0xFF516079)
    val outline = Color(0xFFE2E8F0)
    val faceLine = Color(0xFF3E4C63)
    val die = Color(0xFF1E293B)
    val dieHatch = Color(0xFF2E3B52)
    val dieEdge = Color(0xFF64748B)
    val insert = Color(0xFF2B3A52)
    val insertHatch = Color(0xFF41526E)
    val pin = Color(0xFF55657F)
    // Light from the upper left: centre band, highlight, body, shadow at the silhouette
    val shade = listOf(Color(0xFFB7C5D8), Color(0xFFE1E9F3), Color(0xFF93A6C1), Color(0xFF55657F))
    val hexFront = Color(0xFFC5D1E1)
    val hexSide = Color(0xFF7C8EA9)
    val centerLine = Color(0xFFF59E0B)
    val label = Color(0xFFCBD5E1)
    val peak = Color(0xFFF59E0B)
}

private fun fmt(v: Double, decimals: Int) = String.format(java.util.Locale.US, "%,.${decimals}f", v)

private fun stationCode(st: HeadingStation) = if (st.isCutOff) "CUT" else "D${st.index} "

internal fun stationFeatureLabel(st: HeadingStation): String {
    val top = st.segments.last()
    return when {
        top.shape == SectionShape.HEX || top.shape == SectionShape.TWELVE_POINT -> "s ${fmt(top.dBottomMm, 2)}"
        top.shape == SectionShape.SQUARE -> "□ ${fmt(top.dBottomMm, 2)}"
        // A taper needs both ends, or the single figure reads as the whole section: die end first, as it is drawn
        top.shape == SectionShape.ROUND && kotlin.math.abs(top.dTopMm - top.dBottomMm) > 0.005 ->
            "Ø ${fmt(top.dBottomMm, 2)} → ${fmt(top.dTopMm, 2)}"
        else -> "Ø ${fmt(top.maxWidthMm, 2)}"
    }
}

/** A finished part drawn on its own, with the same renderer as the die stations (long shanks broken). */
@Composable
fun PartOutlineCanvas(outline: HeadingStation, modifier: Modifier = Modifier) {
    val plan = remember(outline) {
        HeadingPlan(
            slugDiaMm = outline.maxWidthMm, slugLengthMm = outline.heightMm, slugVolumeMm3 = outline.volumeMm3,
            stations = listOf(outline), offHeaderOps = emptyList(), upsetBlows = 1, notes = emptyList()
        )
    }
    Surface(shape = RoundedCornerShape(8.dp), color = DieColors.background, modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(190.dp).padding(6.dp)) {
            drawHeadingProgression(plan, textPx = 9.sp.toPx(), stationLabels = false)
        }
    }
}

internal fun DrawScope.drawHeadingProgression(plan: HeadingPlan, textPx: Float, stationLabels: Boolean = true) {
    val stations = plan.stations
    val step = size.width / stations.size
    val topBand = textPx * 2.1f
    // Space under the parts for the knock-out pin that carries them in the die
    val koBand = if (stations.any { it.inDieMm > 0.0 }) textPx * 1.4f else 0f
    val baseY = size.height - textPx * 2.2f - koBand
    val availH = baseY - topBand
    // Few stations (studs, set screws) would otherwise draw parts far wider than a bolt progression
    val scaleW = (if (stationLabels) min(step, size.width / 4f) else size.width * 0.42f) * 0.8f / stations.maxOf { it.maxWidthMm }
    val maxH = stations.maxOf { it.heightMm }

    // Long headed parts: shorten every station's longest straight round segment by the same length (drafting break)
    // so the head stays readable. Headless parts are drawn unbroken so their length change reads correctly.
    fun longestStraight(st: HeadingStation) = st.segments.withIndex()
        .filter { it.value.shape == SectionShape.ROUND && it.value.dBottomMm == it.value.dTopMm }
        .maxByOrNull { it.value.heightMm }
    val minLongest = stations.minOf { longestStraight(it)?.value?.heightMm ?: 0.0 }
    val breakMm = if (plan.upsetBlows > 0 && maxH * scaleW > availH) min(maxH - availH / scaleW, 0.8 * minLongest).coerceAtLeast(0.0) else 0.0
    val scale = min(scaleW.toDouble(), availH / (maxH - breakMm)).toFloat()

    val labelPaint = android.graphics.Paint().apply {
        textSize = textPx
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
    }
    val dimPaint = android.graphics.Paint(labelPaint).apply {
        isFakeBoldText = false
        color = DieColors.label.copy(alpha = 0.8f).toArgb()
        textSize = textPx * 0.9f
    }

    stations.forEachIndexed { i, st ->
        val cx = step * i + step / 2f
        val breakIdx = if (breakMm > 0.0) longestStraight(st)?.index else null
        drawStation(st, cx, baseY, scale, min(step, size.width / 4f), breakIdx, breakMm, isFinal = i == stations.lastIndex, koBand = koBand)

        val topY = baseY - (st.heightMm - if (breakIdx != null) breakMm else 0.0).toFloat() * scale
        drawContext.canvas.nativeCanvas.apply {
            drawText(stationFeatureLabel(st), cx, topY - textPx * 0.6f, dimPaint)
            labelPaint.color = (if (st.isPeakBlow) DieColors.peak else DieColors.label).toArgb()
            if (stationLabels) drawText(stationCode(st).trim(), cx, baseY + koBand + textPx * 1.6f, labelPaint)
        }
    }
}

private fun DrawScope.drawStation(
    st: HeadingStation, cx: Float, baseY: Float, scale: Float, column: Float, breakIdx: Int?, breakMm: Double,
    isFinal: Boolean, koBand: Float
) {
    val segs = st.segments
    val drawnH = segs.mapIndexed { j, s -> ((s.heightMm - if (j == breakIdx) breakMm else 0.0) * scale).toFloat() }
    val bottoms = FloatArray(segs.size)
    var y = baseY
    segs.indices.forEach { j -> bottoms[j] = y; y -= drawnH[j] }
    val topY = y

    // Axial position (mm from bottom) → screen y, honouring the break
    fun yAt(mm: Double): Float {
        var acc = 0.0
        segs.forEachIndexed { j, s ->
            if (mm <= acc + s.heightMm) return bottoms[j] - ((mm - acc) / s.heightMm).toFloat() * drawnH[j]
            acc += s.heightMm
        }
        return topY
    }

    fun halfWidth(s: ProfileSegment, top: Boolean) = when (s.shape) {
        SectionShape.ROUND -> if (top) s.dTopMm else s.dBottomMm
        // A dome closes to a point on the axis
        SectionShape.DOME -> if (top) 0.0 else s.dBottomMm
        else -> s.maxWidthMm
    }.toFloat() * scale / 2f

    fun hatch(left: Float, top: Float, right: Float, bottom: Float, color: Color, gap: Float, rising: Boolean) =
        clipRect(left, top, right, bottom) {
            val h = bottom - top
            var x = left - h
            while (x < right) {
                if (rising) drawLine(color, Offset(x, bottom), Offset(x + h, top), 1f)
                else drawLine(color, Offset(x, top), Offset(x + h, bottom), 1f)
                x += gap
            }
        }

    // Tooling: die case, hard insert around the bore, and the knock-out pin the part stands on
    if (st.inDieMm > 0.0) {
        val dieTop = yAt(st.inDieMm)
        val dieBottom = baseY + koBand
        val left = cx - column * 0.47f
        val right = cx + column * 0.47f
        val boreR = halfWidth(segs.first(), false)
        val insertR = min(column * 0.47f, boreR + column * 0.13f)
        drawRect(DieColors.die, Offset(left, dieTop), Size(right - left, dieBottom - dieTop))
        hatch(left, dieTop, right, dieBottom, DieColors.dieHatch, 6f, rising = true)
        drawRect(DieColors.insert, Offset(cx - insertR, dieTop), Size(2 * insertR, dieBottom - dieTop))
        hatch(cx - insertR, dieTop, cx + insertR, dieBottom, DieColors.insertHatch, 5f, rising = false)
        listOf(cx - insertR, cx + insertR).forEach { x ->
            drawLine(DieColors.dieEdge.copy(alpha = 0.7f), Offset(x, dieTop), Offset(x, dieBottom), 1f)
        }
        drawLine(DieColors.dieEdge, Offset(left, dieTop), Offset(right, dieTop), 1.5f)
        if (koBand > 0f) {
            val pinR = boreR * 0.72f
            drawRect(DieColors.background, Offset(cx - boreR, baseY), Size(2 * boreR, koBand))
            drawRect(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    0f to DieColors.pin, 0.4f to DieColors.dieEdge, 1f to DieColors.die, startX = cx - pinR, endX = cx + pinR
                ),
                Offset(cx - pinR, baseY + 1f), Size(2 * pinR, koBand - 1f)
            )
            listOf(cx - boreR, cx + boreR).forEach { x ->
                drawLine(DieColors.dieEdge.copy(alpha = 0.7f), Offset(x, baseY), Offset(x, dieBottom), 1f)
            }
            listOf(cx - pinR, cx + pinR).forEach { x ->
                drawLine(DieColors.outline.copy(alpha = 0.6f), Offset(x, baseY + 1f), Offset(x, dieBottom), 0.8f)
            }
        }
    }

    fun chamfered(shape: SectionShape) = shape == SectionShape.HEX || shape == SectionShape.TWELVE_POINT
    val chamferTop = chamfered(segs.last().shape)
    val chamferBottom = chamfered(segs.first().shape)
    val outline: Path
    if (st.isCutOff) {
        // Sheared slug: both end faces lean with the shear blade; the mean length is the slug length
        val r = halfWidth(segs.first(), false)
        val lean = r * 0.16f
        outline = Path().apply {
            moveTo(cx - r, baseY + lean / 2f)
            lineTo(cx + r, baseY - lean / 2f)
            lineTo(cx + r, topY - lean / 2f)
            lineTo(cx - r, topY + lean / 2f)
            close()
        }
    } else {
        // Right-hand profile, bottom → top, as (radius, y, corner radius); corners are then rounded like forged edges
        val raw = mutableListOf(Triple(0f, baseY, 0f))
        fun add(r: Float, yy: Float, corner: Float = 0f) { raw += Triple(r, yy, corner) }
        val topSeg = segs.last()
        val topR = halfWidth(topSeg, true)
        val isUpsetHead = segs.size > 1 && topR > 1.25f * halfWidth(segs[segs.size - 2], true)
        val topCorner = when {
            topSeg.dBottomMm != topSeg.dTopMm -> topR * 0.15f                     // cone punch nose
            isFinal -> topR * 0.08f                                               // finished edge
            isUpsetHead || segs.size == 1 -> min(drawnH.last() * 0.28f, topR * 0.22f) // corners of an upset do not fill
            else -> topR * 0.10f                                                  // squared slug end
        }
        segs.forEachIndexed { j, sg ->
            val y0 = bottoms[j]
            val y1 = y0 - drawnH[j]
            val rB = halfWidth(sg, false)
            val rT = halfWidth(sg, true)
            // Prism sections (hex, 12-point, square) keep sharp corners; the forged radii belong to round sections
            val hex = sg.shape == SectionShape.HEX || sg.shape == SectionShape.TWELVE_POINT || sg.shape == SectionShape.SQUARE
            if (sg.shape == SectionShape.DOME) {
                // Spherical cap: arc from the base edge to the crown
                val rP = if (j == 0) 0f else halfWidth(segs[j - 1], true)
                val h = drawnH[j]
                if (h > 0.5f && rB > 0.5f) {
                    val rs = (rB * rB + h * h) / (2f * h)
                    val phi0 = kotlin.math.atan2(rB, rs - h)
                    if (j > 0 && rB > rP + 0.5f) add(rB, y0, min(rB - rP, h) * 0.25f) else if (j == 0) add(rB, y0)
                    for (i in 1..14) {
                        val phi = phi0 * (1f - i / 14f)
                        add(rs * kotlin.math.sin(phi), (y1 + rs) - rs * kotlin.math.cos(phi))
                    }
                }
                return@forEachIndexed
            }
            if (j == 0) {
                if (chamfered(sg.shape) && chamferBottom) { val c = rB * 0.12f; add(rB - c, y0); add(rB, y0 - c * 0.6f) }
                else add(rB, y0, if (segs.size == 1 && !isFinal) topCorner else rB * 0.10f)
            } else {
                val rP = halfWidth(segs[j - 1], true)
                if (rB > rP + 0.5f) add(rB, y0, if (hex) 0f else min(rB - rP, drawnH[j]) * (if (isFinal) 0.10f else 0.22f))
                else if (rB < rP - 0.5f) add(rB, y0, kotlin.math.max(1.5f, rB * 0.10f))
            }
            val next = segs.getOrNull(j + 1)
            if (next == null) {
                if (chamfered(sg.shape)) { val c = rT * 0.12f; add(rT, y1 + c * 0.6f); add(rT - c, y1) }
                else add(rT, y1, if (hex) 0f else topCorner)
            } else {
                val rN = halfWidth(next, false)
                when {
                    kotlin.math.abs(rN - rT) <= 0.5f ->
                        if (kotlin.math.abs(rB - rT) > 0.5f || next.dBottomMm != next.dTopMm) add(rT, y1)
                    rN > rT -> {
                        val extrusion = !hex && next.shape == SectionShape.ROUND && sg.dBottomMm == sg.dTopMm && rN / rT <= 1.25f
                        // Extrusion shoulder: 15° per side die cone. Head: under-head fillet.
                        if (extrusion) add(rT, y1 + min((rN - rT) * 3.73f, drawnH[j] * 0.6f))
                        else add(rT, y1, kotlin.math.max(1.5f, rT * 0.16f))
                    }
                    else -> add(rT, y1, if (hex) 0f else min(rT - rN, drawnH[j]) * 0.18f)
                }
            }
        }
        raw += Triple(0f, topY, 0f)

        val right = mutableListOf<Offset>()
        raw.forEachIndexed { i, (r, yy, corner) ->
            val pnt = Offset(cx + r, yy)
            if (corner <= 0.3f || i == 0 || i == raw.lastIndex) { right += pnt; return@forEachIndexed }
            val a = Offset(cx + raw[i - 1].first, raw[i - 1].second)
            val b = Offset(cx + raw[i + 1].first, raw[i + 1].second)
            val la = (a - pnt).getDistance()
            val lb = (b - pnt).getDistance()
            val rr = min(corner, 0.45f * min(la, lb))
            if (rr <= 0.3f) { right += pnt; return@forEachIndexed }
            val pa = pnt + (a - pnt) / la * rr
            val pb = pnt + (b - pnt) / lb * rr
            for (k in 0..6) {
                val t = k / 6f
                val u = 1f - t
                right += Offset(u * u * pa.x + 2 * u * t * pnt.x + t * t * pb.x, u * u * pa.y + 2 * u * t * pnt.y + t * t * pb.y)
            }
        }
        outline = Path().apply {
            moveTo(right.first().x, right.first().y)
            right.drop(1).forEach { lineTo(it.x, it.y) }
            right.asReversed().forEach { lineTo(2 * cx - it.x, it.y) }
            close()
        }
    }
    drawPath(outline, if (st.isCutOff) DieColors.wire else DieColors.metal)

    val maxR = st.maxWidthMm.toFloat() * scale / 2f
    val pad = maxR * 0.2f + 4f
    clipPath(outline) {
        // Unsectioned right half: turned-surface shading on round sections, flat facets on a hex
        clipRect(cx, topY - pad, cx + maxR + 2f, baseY + pad) {
            segs.forEachIndexed { j, sg ->
                val y0 = bottoms[j] + if (j == 0) pad else 0f
                val y1 = bottoms[j] - drawnH[j] - if (j == segs.lastIndex) pad else 0f
                val r = kotlin.math.max(halfWidth(sg, false), halfWidth(sg, true))
                if (sg.shape == SectionShape.HEX) {
                    drawRect(DieColors.hexFront, Offset(cx, y1), Size(r / 2f, y0 - y1))
                    drawRect(DieColors.hexSide, Offset(cx + r / 2f, y1), Size(maxR, y0 - y1))
                } else if (sg.shape == SectionShape.TWELVE_POINT) {
                    // Twelve narrow facets: light on the axis, darker toward the silhouette
                    drawRect(DieColors.hexFront, Offset(cx, y1), Size(r * 0.26f, y0 - y1))
                    drawRect(DieColors.shade[2], Offset(cx + r * 0.26f, y1), Size(r * 0.45f, y0 - y1))
                    drawRect(DieColors.hexSide, Offset(cx + r * 0.71f, y1), Size(maxR, y0 - y1))
                } else if (sg.shape == SectionShape.SQUARE) {
                    drawRect(DieColors.hexFront, Offset(cx, y1), Size(maxR + 2f, y0 - y1))
                } else {
                    drawRect(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            0f to DieColors.shade[0], 0.3f to DieColors.shade[1], 0.72f to DieColors.shade[2], 1f to DieColors.shade[3],
                            startX = cx, endX = cx + r
                        ),
                        Offset(cx, y1), Size(maxR + 2f, y0 - y1)
                    )
                }
            }
        }
        // Left half in section: cut face with 45° hatching
        clipRect(cx - maxR - 2f, topY - pad, cx, baseY + pad) {
            drawRect(DieColors.cutFace, Offset(cx - maxR - 2f, topY - pad), Size(maxR + 2f, baseY - topY + 2 * pad))
            var x = cx - maxR - (baseY - topY) - 2 * pad
            while (x < cx) {
                drawLine(DieColors.hatch, Offset(x, baseY + pad), Offset(x + (baseY - topY) + 2 * pad, topY - pad), 0.8f)
                x += 5f
            }
        }
    }

    val hidden = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))
    // Blind cavity: mouth chamfer, walls, and the shallow cone the punch nose leaves on the floor
    fun cavity(widthMm: Double, yMouth: Float, yFloor: Float, hex: Boolean) {
        val r = widthMm.toFloat() * scale / 2f
        val depth = kotlin.math.abs(yFloor - yMouth)
        val dir = if (yFloor > yMouth) 1f else -1f
        val ch = min(r * 0.12f, depth * 0.15f)
        val cone = min(r * 0.22f, depth * 0.25f)
        fun side(sign: Float) = Path().apply {
            moveTo(cx + sign * (r + ch), yMouth)
            lineTo(cx + sign * r, yMouth + dir * ch)
            lineTo(cx + sign * r, yFloor)
            lineTo(cx, yFloor + dir * cone)
        }
        drawPath(Path().apply { addPath(side(-1f)); lineTo(cx, yMouth); close() }, DieColors.background)
        drawPath(side(-1f), DieColors.outline, style = Stroke(1f))
        if (hex) drawLine(DieColors.outline.copy(alpha = 0.35f), Offset(cx - r / 2f, yMouth + dir * ch), Offset(cx - r / 2f, yFloor + dir * cone / 2f), 0.8f)
        // Hidden edges in the unsectioned right half
        drawPath(side(1f), DieColors.outline.copy(alpha = 0.5f), style = Stroke(1f, pathEffect = hidden))
    }
    st.throughHoleMm?.let { h ->
        val r = h.toFloat() * scale / 2f
        drawRect(DieColors.background, Offset(cx - r, topY), Size(r, baseY - topY))
        drawLine(DieColors.outline, Offset(cx - r, topY), Offset(cx - r, baseY), 1f)
        drawLine(DieColors.outline.copy(alpha = 0.5f), Offset(cx + r, topY), Offset(cx + r, baseY), 1f, pathEffect = hidden)
    }
    st.recesses.forEach { rc ->
        val hex = rc.shape == SectionShape.HEX
        val width = if (hex) rc.sizeMm * 2.0 / kotlin.math.sqrt(3.0) else rc.sizeMm
        if (rc.fromTop) cavity(width, topY, yAt(st.heightMm - rc.depthMm), hex)
        else cavity(width, baseY, yAt(rc.depthMm), hex)
    }

    // Hex faces seen across corners (unsectioned right half): edges at ±e/4 and 30° chamfer arcs
    clipRect(cx, topY - 4f, cx + maxR + 4f, baseY + 4f) { segs.forEachIndexed { j, s ->
        if (s.shape != SectionShape.HEX && s.shape != SectionShape.TWELVE_POINT) return@forEachIndexed
        val y0 = bottoms[j]
        val y1 = y0 - drawnH[j]
        val r = halfWidth(s, true)
        val dip = min(drawnH[j] * 0.12f, r * 0.15f)
        if (s.shape == SectionShape.TWELVE_POINT) {
            // Projected corners of the bi-hex: cos 75°, cos 45°, cos 15° of the across-corners radius
            listOf(0.26f, 0.71f, 0.97f).forEach { fr ->
                drawLine(DieColors.faceLine, Offset(cx + r * fr, y1 + dip * 0.6f), Offset(cx + r * fr, y0), 1f)
            }
            return@forEachIndexed
        }
        listOf(-r / 2f, r / 2f).forEach { dx ->
            drawLine(DieColors.faceLine, Offset(cx + dx, y1 + dip), Offset(cx + dx, y0 - if (j == 0 && chamferBottom) dip else 0f), 1f)
        }
        val faces = listOf(-r to -r / 2f, -r / 2f to r / 2f, r / 2f to r)
        fun arcs(yEdge: Float, dir: Float) = faces.forEach { (a, b) ->
            val d = if (a == -r / 2f) dip else dip * 0.6f
            drawPath(Path().apply {
                moveTo(cx + a, yEdge + dir * if (a == -r) d else 0f)
                quadraticBezierTo(cx + (a + b) / 2f, yEdge + dir * 2f * d, cx + b, yEdge + dir * if (b == r) d else 0f)
            }, DieColors.faceLine, style = Stroke(1f))
        }
        if (j == segs.lastIndex) arcs(y1, 1f)
        if (j == 0 && chamferBottom) arcs(y0, -1f)
    } }

    drawPath(outline, DieColors.outline, style = Stroke(1.2f))

    // Centre line
    drawLine(
        DieColors.centerLine.copy(alpha = 0.7f), Offset(cx, topY - 4f), Offset(cx, baseY + 4f), 0.8f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 3f, 2f, 3f))
    )

    // Break symbol across the shortened segment
    if (breakIdx != null) {
        val yMid = bottoms[breakIdx] - drawnH[breakIdx] / 2f
        // The break crosses the die only where the broken length is inside it
        val half = if (st.inDieMm > 0.0 && yMid > yAt(st.inDieMm)) column * 0.47f else halfWidth(segs[breakIdx], false) + 2f
        drawRect(DieColors.background, Offset(cx - half, yMid - 2.5f), Size(2 * half, 5f))
        listOf(yMid - 2.5f, yMid + 2.5f).forEach { yy ->
            drawPath(Path().apply {
                moveTo(cx - half, yy)
                var x = cx - half
                var up = true
                while (x < cx + half) {
                    x = minOf(x + 4f, cx + half)
                    lineTo(x, yy + if (up) -1.5f else 1.5f)
                    up = !up
                }
            }, DieColors.outline.copy(alpha = 0.7f), style = Stroke(0.8f))
        }
    }
}

/** One furnace cycle: every stage with its hours, the door-to-door total, then basis, cautions, sources. */
@Composable
private fun CycleStagesBlock(
    title: String,
    stages: List<com.example.data.AnnealStage>,
    totalText: String,
    totalSuffix: String = "",
    basis: String,
    cautions: List<String>,
    sources: List<String>,
    header: (@Composable () -> Unit)? = null
) {
    val customColors = LocalCustomColors.current
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = customColors.accentSecondary
        )
        header?.invoke()
    }
    Spacer(modifier = Modifier.height(4.dp))
    stages.forEach { st ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(st.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = customColors.textMain)
                Text(st.detail, fontSize = 9.sp, color = customColors.textMuted)
            }
            Text(
                text = st.hoursText,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = customColors.accentPrimary
            )
        }
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = customColors.bgElevated,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TOTAL CYCLE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = customColors.textMain)
            Text(
                text = totalText + totalSuffix,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = customColors.accentSecondary
            )
        }
    }
    Text(basis, fontSize = 9.sp, color = customColors.textMuted, modifier = Modifier.padding(top = 3.dp))
    cautions.forEach {
        Text("\u26a0 " + it, fontSize = 9.sp, color = AlertRed, modifier = Modifier.padding(top = 2.dp))
    }
    if (sources.isNotEmpty()) {
        Spacer(modifier = Modifier.height(3.dp))
        Text("Cycle sources", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = customColors.textMuted)
        sources.forEach { Text("\u2022 " + it, fontSize = 8.5.sp, color = customColors.textMuted) }
    }
}

@Composable
private fun SoftAnnealCard(result: FastenerCalculationResult) {
    val spec = result.softAnnealSpec ?: return
    // Bell-furnace charge the cycle is scaled for; heat-up and unload are charge-driven
    var chargeTonnes by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(com.example.data.AnnealCycleEngine.DEFAULT_CHARGE_TONNES)
    }
    val customColors = LocalCustomColors.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = customColors.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SOFT ANNEAL \u2014 WIRE PREPARATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.textMain
                    )
                    Text(
                        text = spec.route.label + " \u2022 " + result.material.name,
                        fontSize = 9.sp,
                        color = customColors.textMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (spec.inHouseAnnealNeeded) customColors.accentPrimary.copy(alpha = 0.15f)
                    else customColors.bgElevated
                ) {
                    Text(
                        text = if (spec.inHouseAnnealNeeded) "ANNEAL REQUIRED" else "SUPPLIED ANNEALED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (spec.inHouseAnnealNeeded) customColors.accentPrimary else customColors.textMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = spec.route.purpose,
                fontSize = 9.sp,
                color = customColors.textMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )

            if (spec.temperatureC.isNotBlank() && !spec.temperatureC.startsWith("N/A")) {
                HtStageCard(
                    stage = if (spec.inHouseAnnealNeeded) "Anneal cycle" else "Mill anneal \u2014 re-anneal between blows",
                    temperature = spec.temperatureC,
                    soakTime = spec.soakTime,
                    subDetail = "Cooling: " + spec.coolingRate + "  \u2022  Atmosphere: " + spec.atmosphere
                )
            }

            HtStageCard(
                stage = "Incoming wire condition",
                temperature = spec.targetUtsMpa,
                soakTime = spec.targetHardnessHb,
                subDetail = listOf(spec.targetHardnessHrb, spec.structureTarget)
                    .filter { it.isNotBlank() }.joinToString("  \u2022  ")
            )

            if (spec.cycleSource.isNotBlank()) {
                Text(
                    text = "Source: " + spec.cycleSource + "  \u2022  " + spec.cycleConfidence,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (spec.cycleConfidence.startsWith("Low")) AlertRed else customColors.textMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            spec.softeningRatio?.let { ratio ->
                Text(
                    text = "Wire anneals to " + (ratio * 100).toInt() + " % of the finished UTS " +
                        "(" + result.material.postForgingTargetUtsMpa.toInt() + " MPa)",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = customColors.textMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Complete furnace cycle: every stage with its hours, so the anneal can be booked and costed
            val cycle = com.example.data.AnnealCycleEngine.spheroidize(result.material, chargeTonnes)
            if (cycle != null && spec.inHouseAnnealNeeded) {
                CycleStagesBlock(
                    title = "COMPLETE CYCLE \u2014 DOOR TO DOOR",
                    stages = cycle.stages,
                    totalText = cycle.totalText,
                    totalSuffix = "  \u2022  " + cycle.hoursPerTonne,
                    basis = cycle.basis,
                    cautions = cycle.cautions,
                    sources = cycle.sources,
                    header = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Charge", fontSize = 9.sp, color = customColors.textMuted)
                            listOf(2.0, 5.0, 10.0, 20.0, 30.0).forEach { t ->
                                val on = kotlin.math.abs(chargeTonnes - t) < 0.01
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = if (on) customColors.accentPrimary else customColors.bgElevated,
                                    modifier = Modifier
                                        .padding(start = 3.dp)
                                        .clickable { chargeTonnes = t }
                                ) {
                                    Text(
                                        text = fmt(t, 0) + "t",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (on) Color(0xFF0F172A) else customColors.textMuted,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // Aluminium and copper: the catalogue left the O-temper cycle blank, so it is built here
            if (cycle == null) {
                com.example.data.HeatTreatCycleEngine.softAnneal(result.material, result.wireDiaMm)?.let { nf ->
                    CycleStagesBlock(
                        title = nf.title.uppercase(),
                        stages = nf.stages,
                        totalText = nf.totalText,
                        basis = nf.basis,
                        cautions = nf.cautions,
                        sources = nf.sources
                    )
                }
            }

            if (spec.unsourced.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "NOT IN CATALOGUE \u2014 ENTER PER GRADE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlertRed
                )
                spec.unsourced.forEach {
                    Text(
                        text = "\u2022 " + it,
                        fontSize = 9.sp,
                        color = customColors.textMuted
                    )
                }
            }

            spec.notes.forEach {
                Text(
                    text = it,
                    fontSize = 9.sp,
                    color = customColors.textMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = spec.standardReference,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = customColors.accentPrimary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
