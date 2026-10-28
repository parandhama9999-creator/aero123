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
import com.example.ui.components.PokaYokePanel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LocalCustomColors
import com.example.ui.theme.SuccessGreen

@Composable
fun ForgingMetallurgyScreen(
    result: FastenerCalculationResult,
    modifier: Modifier = Modifier
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
                                text = "Finished Fastener Target UTS:",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = customColors.textMuted
                            )
                            Text(
                                text = "${result.postForgingTargetUtsMpa.toInt()} MPa (Min)",
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
                        text = "Elongation (A5): min ${result.minElongationPct.toInt()}%  •  Reduction of Area (Z): min ${result.reductionOfAreaZPct.toInt()}%",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = customColors.textMuted
                    )
                }
            }
        }

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

                // Stage 1: Pre-Heating / Stress Relief
                HtStageCard(
                    stage = "Stage 1: Pre-Heating / Stress Relief",
                    temperature = result.heatTreatmentSpec.preheatTempC,
                    soakTime = result.heatTreatmentSpec.preheatSoakTime,
                    subDetail = "Thermal Equalization to relieve cold heading stress"
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stage 2: Austenitizing / Solution Annealing
                HtStageCard(
                    stage = if (result.heatTreatmentSpec.isAging) "Stage 2: Solution Annealing" else "Stage 2: Austenitizing",
                    temperature = result.heatTreatmentSpec.austenitizingOrSolutionTempC,
                    soakTime = result.heatTreatmentSpec.soakingTimeMinutes,
                    subDetail = "Atmosphere: ${result.heatTreatmentSpec.atmosphere}"
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stage 3: Quenching Protocol
                HtStageCard(
                    stage = "Stage 3: Quenching Protocol",
                    temperature = result.heatTreatmentSpec.quenchMedium,
                    soakTime = "< 10s Transfer",
                    subDetail = result.heatTreatmentSpec.quenchDetails
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stage 4: Tempering / Precipitation Aging
                HtStageCard(
                    stage = if (result.heatTreatmentSpec.isAging) "Stage 4: Precipitation Aging" else "Stage 4: Tempering Cycle",
                    temperature = result.heatTreatmentSpec.temperingOrAgingTempC,
                    soakTime = result.heatTreatmentSpec.temperingOrAgingTimeHours,
                    subDetail = "Target Hardness: ${result.heatTreatmentSpec.targetHardnessHrc}"
                )

                if (result.hasHydrogenEmbrittlementRisk) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HtStageCard(
                        stage = "Stage 5: Mandatory Hydrogen De-Embrittlement Baking",
                        temperature = result.heatTreatmentSpec.bakingTempC,
                        soakTime = result.heatTreatmentSpec.bakingSoakTime,
                        subDetail = result.heatTreatmentSpec.hydrogenBakingCycle,
                        isWarning = true
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    HtStageCard(
                        stage = "Stage 5: Hydrogen De-Embrittlement Baking",
                        temperature = "190°C – 200°C",
                        soakTime = "N/A",
                        subDetail = "Exempt for ${result.coating.label} coating on ${result.material.name}"
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
                        text = "${result.forgingStages.size}-STATION PROGRESSIVE FORGING & TONNAGE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.textMain
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5 Progression Stages
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
                    text = "Lubrication: ${result.coating.label} • ${result.material.lubricationCoating}",
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
        HardnessTroubleshootingCard(targetHrcText = result.heatTreatmentSpec.targetHardnessHrc)

        Spacer(modifier = Modifier.height(8.dp))

        // Section 7: Customer Spec Compliance & Feasibility Checker
        CustomerSpecFeasibilityCard(materialName = result.material.name)
    }
}

@Composable
fun CustomerSpecFeasibilityCard(materialName: String) {
    val customColors = LocalCustomColors.current
    var customerTargetHrc by remember { mutableStateOf(45) }

    val isA286 = materialName.contains("A286", ignoreCase = true)
    val isStainless = materialName.contains("SS", ignoreCase = true) || materialName.contains("Stainless", ignoreCase = true)
    val maxMaterialLimit = when {
        isA286 -> 40
        isStainless -> 32
        materialName.contains("Inconel 718", ignoreCase = true) -> 45
        else -> 52
    }

    val isFeasible = customerTargetHrc <= maxMaterialLimit

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
                text = "Test Customer Target Hardness Requirement: $customerTargetHrc HRC (Current Material Max Limit: ~$maxMaterialLimit HRC)",
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
                        Text(text = "Customer target of $customerTargetHrc HRC is within physical limits of $materialName. Standard hardening/aging cycle applies.", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                    } else {
                        Text(text = "⚠️ METALLURGICAL IMPOSSIBILITY / SPEC VIOLATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                        Text(text = "• $materialName physically cannot exceed ~$maxMaterialLimit HRC without severe matrix embrittlement & micro-cracking.\n• Recommended Action: Issue a formal Engineering Deviation / Reject or upgrade material to Inconel 718 (for 45 HRC) or Maraging C-300 (for 50+ HRC).", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HardnessTroubleshootingCard(targetHrcText: String) {
    val customColors = LocalCustomColors.current
    var selectedFailureMode by remember { mutableStateOf("LOW_HARDNESS") }

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
                listOf(
                    "LOW_HARDNESS" to "Low Hardness",
                    "DECARB" to "Soft Surface",
                    "OVER_TEMPER" to "Over-Tempered"
                ).forEach { (mode, label) ->
                    val isSelected = selectedFailureMode == mode
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) AlertRed else customColors.bgElevated,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFailureMode = mode }
                    ) {
                        Text(
                            text = label,
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
                    when (selectedFailureMode) {
                        "LOW_HARDNESS" -> {
                            Text(text = "Root Cause: Slack Quenching / Slow Cooling Rate", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                            Text(text = "• Check quench oil temperature (must be 40–60°C).\n• Verify oil agitation pump and circulation.\n• Increase austenitizing temp by 10–20°C to ensure full carbide dissolution.", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                        }
                        "DECARB" -> {
                            Text(text = "Root Cause: Surface Decarburization (Soft Skin)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                            Text(text = "• Grind 0.2mm off surface and re-test core hardness.\n• If core is hard, raise the furnace C-potential to match the steel carbon content (see atmosphere spec above) — never above it (carburizing is forbidden by ISO 898-1).\n• Check endothermic generator gas dew point.", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                        }
                        else -> {
                            Text(text = "Root Cause: Over-Tempering (Temp Set Too High)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                            Text(text = "• Recalibrate tempering furnace thermocouple.\n• Over-tempered parts must be re-austenitized, re-quenched, and re-tempered at lower temperature.", fontSize = 9.5.sp, color = customColors.textMain, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
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
    modifier: Modifier = Modifier
) {
    val customColors = LocalCustomColors.current
    val stages = result.forgingStages
    val stageCount = stages.size.coerceIn(2, 6)
    val dims = result.headDimensions
    val wireDia = result.wireDiaMm.toFloat()
    val cutLen = result.cutOffLengthMm.toFloat()
    val headS = dims?.acrossFlatsS?.toFloat() ?: (wireDia * 1.5f)
    val headK = dims?.headHeightK?.toFloat() ?: (wireDia * 0.7f)
    val nutM = dims?.nutHeightM?.toFloat() ?: (wireDia * 0.8f)
    val isNut = result.isInternalNut
    val isStud = result.fastenerType.isStud
    val isSocket = result.fastenerType == FastenerType.SHCS || result.fastenerType == FastenerType.SOCKET_BUTTON || result.fastenerType == FastenerType.SOCKET_CSK
    val ldOk = result.upsetLdRatio <= 2.3

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
        color = Color(0xFF0F172A),
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
                    text = "$stageCount-STATION DIE PROGRESSION",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = "Head Tonnage: ${result.forgingTonnage} T",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFF59E0B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
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
                    val w = size.width
                    val h = size.height
                    val step = w / stageCount.toFloat()
                    val baseY = h * 0.80f
                    val maxDim = maxOf(cutLen, headS, wireDia * 2f)
                    val scale = (h * 0.60f) / maxDim

                    for (i in 0 until stageCount) {
                        val cx = step * i + step / 2f
                        val fraction = i.toFloat() / (stageCount - 1).coerceAtLeast(1)

                        if (isNut) {
                            drawNutStage(cx, baseY, scale, wireDia, cutLen, headS, nutM, fraction, i, stageCount)
                        } else if (isStud) {
                            drawStudStage(cx, baseY, scale, wireDia, cutLen, fraction, i, stageCount)
                        } else {
                            drawBoltStage(cx, baseY, scale, wireDia, cutLen, headS, headK, isSocket, fraction, i, stageCount)
                        }

                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = 0xFFCBD5E1.toInt()
                                textSize = 20f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            drawText("S${i + 1}", cx, baseY + 18f, paint)
                        }
                    }
                }

                // Zoom indicator
                if (zoomScale > 1.05f) {
                    Text(
                        text = "${String.format("%.1f", zoomScale)}×",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    )
                }
            }

            // Reset zoom button
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

            Column {
                stages.forEachIndexed { idx, stage ->
                    Text(
                        text = "• S${idx + 1} ${stage.title}: ${stage.description}",
                        fontSize = 9.sp,
                        color = customColors.textMuted,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Text(
                        text = "L/D: ${result.upsetLdRatio}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ldOk) Color(0xFF22C55E) else Color(0xFFEF4444)
                    )
                    Text(
                        text = "  (limit 2.3 single blow)",
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
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun DrawScope.drawBoltStage(
    cx: Float, baseY: Float, scale: Float,
    wireDia: Float, cutLen: Float, headS: Float, headK: Float,
    isSocket: Boolean, fraction: Float, idx: Int, total: Int
) {
    val shankW = wireDia * scale
    when {
        idx == 0 -> {
            val slugH = cutLen * scale
            drawRect(Color(0xFF94A3B8), Offset(cx - shankW / 2, baseY - slugH), Size(shankW, slugH))
        }
        idx == total - 1 -> {
            val shankH = (cutLen - headK) * scale
            drawRect(Color(0xFF0EA5E9), Offset(cx - shankW / 2, baseY - shankH), Size(shankW, shankH))
            val headW = if (isSocket) wireDia * 1.5f * scale else headS * scale
            val headH = headK * scale
            val headColor = if (isSocket) Color(0xFF8B5CF6) else Color(0xFFF59E0B)
            drawRect(headColor, Offset(cx - headW / 2, baseY - shankH - headH), Size(headW, headH))
            if (isSocket) {
                val socketW = shankW * 0.5f
                val socketD = headH * 0.6f
                drawRect(Color(0xFF1E1B4B), Offset(cx - socketW / 2, baseY - shankH - headH), Size(socketW, socketD))
            }
        }
        else -> {
            val shankH = cutLen * scale * (1f - fraction * headK / cutLen)
            val bulgeFraction = fraction
            val bulgeW = shankW + (headS * scale - shankW) * bulgeFraction
            val bulgeH = headK * scale * bulgeFraction
            drawRect(Color(0xFF0EA5E9), Offset(cx - shankW / 2, baseY - shankH), Size(shankW, shankH))
            if (bulgeH > 0) {
                drawRect(Color(0xFF38BDF8), Offset(cx - bulgeW / 2, baseY - shankH - bulgeH), Size(bulgeW, bulgeH))
            }
        }
    }
}

private fun DrawScope.drawNutStage(
    cx: Float, baseY: Float, scale: Float,
    wireDia: Float, cutLen: Float, headS: Float, nutM: Float,
    fraction: Float, idx: Int, total: Int
) {
    val shankW = wireDia * scale
    when {
        idx == 0 -> {
            val slugH = cutLen * scale
            drawRect(Color(0xFF94A3B8), Offset(cx - shankW / 2, baseY - slugH), Size(shankW, slugH))
        }
        idx == total - 1 -> {
            val nutW = headS * scale
            val nutH = nutM * scale
            val topY = baseY - nutH
            drawRect(Color(0xFFF59E0B), Offset(cx - nutW / 2, topY), Size(nutW, nutH))
            val holeW = wireDia * 0.85f * scale
            drawRect(Color(0xFF0F172A), Offset(cx - holeW / 2, topY), Size(holeW, nutH))
        }
        else -> {
            val w = shankW + (headS * scale - shankW) * fraction
            val h = cutLen * scale * (1f - fraction * 0.3f)
            drawRect(Color(0xFF38BDF8), Offset(cx - w / 2, baseY - h), Size(w, h))
            if (idx >= total - 2) {
                val holeW = wireDia * 0.5f * scale * fraction
                val holeH = h * 0.7f
                drawRect(Color(0xFF0F172A), Offset(cx - holeW / 2, baseY - h), Size(holeW, holeH))
            }
        }
    }
}

private fun DrawScope.drawStudStage(
    cx: Float, baseY: Float, scale: Float,
    wireDia: Float, cutLen: Float,
    fraction: Float, idx: Int, total: Int
) {
    val shankW = wireDia * scale
    val slugH = cutLen * scale
    when (idx) {
        0 -> drawRect(Color(0xFF94A3B8), Offset(cx - shankW / 2, baseY - slugH), Size(shankW, slugH))
        else -> {
            drawRect(Color(0xFF0EA5E9), Offset(cx - shankW / 2, baseY - slugH), Size(shankW, slugH))
            if (idx >= total - 1) {
                val chamferH = wireDia * 0.3f * scale
                val p = Path().apply {
                    moveTo(cx - shankW / 2, baseY - slugH)
                    lineTo(cx, baseY - slugH - chamferH)
                    lineTo(cx + shankW / 2, baseY - slugH)
                    close()
                }
                drawPath(p, Color(0xFF22C55E))
                val p2 = Path().apply {
                    moveTo(cx - shankW / 2, baseY)
                    lineTo(cx, baseY + chamferH)
                    lineTo(cx + shankW / 2, baseY)
                    close()
                }
                drawPath(p2, Color(0xFF22C55E))
            }
        }
    }
}
