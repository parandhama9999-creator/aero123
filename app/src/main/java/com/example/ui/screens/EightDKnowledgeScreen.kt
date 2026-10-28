package com.example.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FastenerCalculationResult
import com.example.ui.theme.LocalCustomColors

data class FmeaData(
    val processStep: String,
    val failureMode: String,
    val effectOfFailure: String,
    val initialS: Int, // Severity 1-10
    val potentialCause: String,
    val initialO: Int, // Occurrence 1-10
    val currentControl: String,
    val initialD: Int, // Detection 1-10
    val actionTakenPokaYoke: String,
    val revisedS: Int,
    val revisedO: Int,
    val revisedD: Int
) {
    val initialRpn: Int get() = initialS * initialO * initialD
    val revisedRpn: Int get() = revisedS * revisedO * revisedD
}

data class EightDReport(
    val id: String,
    val title: String,
    val fastenerType: String,
    val material: String,
    val category: String,
    val severity: String,
    val defectSummary: String,
    val d1Team: String,
    val d2ProblemDescription: String,
    val d3ContainmentAction: String,
    val d4RootCauseAnalysis: String,
    val d4FiveWhyList: List<String>,
    val d5PermanentCorrectiveAction: String,
    val d6ValidationResult: String,
    val d7PreventivePokaYoke: String,
    val d8ClosureRecognition: String,
    val pfmea: FmeaData
)

@Composable
fun EightDKnowledgeScreen(
    currentResult: FastenerCalculationResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current

    var activeVoiceTarget by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                activeVoiceTarget?.invoke(spoken)
                Toast.makeText(context, "Voice dictated successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchMic(target: (String) -> Unit) {
        activeVoiceTarget = target
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictate 8D / PFMEA detail...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition not available on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    // State of reports created purely from user inputs / active fastener calculations
    val reportsList = remember { mutableStateListOf<EightDReport>() }

    var expandedReportId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }

    // Custom Input Form Fields
    var inputProcessStep by remember { mutableStateOf("Cold Heading / Thread Rolling") }
    var inputDefectMode by remember { mutableStateOf("Head Cracking under Proof Torque") }
    var inputEffect by remember { mutableStateOf("Fastener shear in assembly & customer rejection") }
    var inputSeverity by remember { mutableStateOf(9f) }
    var inputCause by remember { mutableStateOf("Tool stress concentration & acid pickling hydrogen embrittlement") }
    var inputOccurrence by remember { mutableStateOf(6f) }
    var inputControl by remember { mutableStateOf("Manual end-of-line visual sampling check") }
    var inputDetection by remember { mutableStateOf(5f) }
    var inputActionPokaYoke by remember { mutableStateOf("3D CAD fillet optimization + Non-electrolytic coating + RFID conveyor bake timer interlock") }
    var inputRevisedS by remember { mutableStateOf(9f) }
    var inputRevisedO by remember { mutableStateOf(1f) }
    var inputRevisedD by remember { mutableStateOf(2f) }

    var inputTeam by remember { mutableStateOf("Lead NPD Metallurgist, Process Engineer, Toolroom Manager, Quality Lead") }
    var inputContainment by remember { mutableStateOf("100% Quarantine of lot, 100% wedge tensile testing gate, customer alert bulletin") }
    var inputWhy1 by remember { mutableStateOf("Why 1: High stress concentration at tool corner radius") }
    var inputWhy2 by remember { mutableStateOf("Why 2: Tool recess radius R0.2mm too sharp for high strength alloy") }
    var inputWhy3 by remember { mutableStateOf("Why 3: EDM wire cut left 15µm untempered re-cast white layer") }
    var inputWhy4 by remember { mutableStateOf("Why 4: Toolroom omitted post-EDM stress relief tempering") }
    var inputWhy5 by remember { mutableStateOf("Why 5 (Root Cause): Tooling routing sheet lacked mandatory double-temper post-EDM step") }

    fun addCustomReportFromForm() {
        if (inputProcessStep.isBlank() || inputDefectMode.isBlank()) {
            Toast.makeText(context, "Please enter a Process Step and Defect / Failure Mode.", Toast.LENGTH_SHORT).show()
            return
        }

        val s = inputSeverity.toInt()
        val o = inputOccurrence.toInt()
        val d = inputDetection.toInt()
        val revS = inputRevisedS.toInt()
        val revO = inputRevisedO.toInt()
        val revD = inputRevisedD.toInt()

        // AIAG-VDA FMEA: process controls lower Occurrence/Detection, never Severity (only a design change can)
        if (revS < s) {
            Toast.makeText(context, "⛔ Revised Severity ($revS) is lower than initial ($s). Severity only drops with a design change — set revised S = $s or describe the design change.", Toast.LENGTH_LONG).show()
            return
        }
        if (revO > o || revD > d) {
            Toast.makeText(context, "⛔ Revised Occurrence/Detection cannot be worse than the initial rating after a corrective action.", Toast.LENGTH_LONG).show()
            return
        }

        val severityLabel = when {
            s >= 8 -> "CRITICAL"
            s >= 6 -> "MAJOR"
            else -> "MINOR"
        }

        val newReport = EightDReport(
            id = "8D-INPUT-${System.currentTimeMillis().toString().takeLast(4)}",
            title = "$inputDefectMode (${currentResult.fastenerType.label})",
            fastenerType = currentResult.fastenerType.label,
            material = currentResult.material.name,
            category = "Input-Driven NPD",
            severity = severityLabel,
            defectSummary = "User Input Defect: $inputDefectMode on $inputProcessStep for ${currentResult.threadLabel} (${currentResult.material.name}).",
            d1Team = inputTeam,
            d2ProblemDescription = "5W2H Analysis: Defect '$inputDefectMode' observed during $inputProcessStep. Failure effect: $inputEffect.",
            d3ContainmentAction = inputContainment,
            d4RootCauseAnalysis = "Root Cause Mechanism: $inputCause.",
            d4FiveWhyList = listOf(inputWhy1, inputWhy2, inputWhy3, inputWhy4, inputWhy5).filter { it.isNotBlank() },
            d5PermanentCorrectiveAction = "Planned permanent corrective action: $inputActionPokaYoke.",
            // D6–D8 are evidence and sign-off steps: the app never fills them with results that have not happened
            d6ValidationResult = "PENDING — record the validation lot size, test results and Cpk (target ≥ 1.67) after implementation.",
            d7PreventivePokaYoke = "Planned poka-yoke / preventive control: $inputActionPokaYoke. PFMEA target: RPN ${s * o * d} → ${revS * revO * revD} (verify after implementation).",
            d8ClosureRecognition = "OPEN — awaiting team closure sign-off after D6 evidence is recorded.",
            pfmea = FmeaData(
                processStep = inputProcessStep,
                failureMode = inputDefectMode,
                effectOfFailure = inputEffect,
                initialS = s,
                potentialCause = inputCause,
                initialO = o,
                currentControl = inputControl,
                initialD = d,
                actionTakenPokaYoke = inputActionPokaYoke,
                revisedS = revS,
                revisedO = revO,
                revisedD = revD
            )
        )

        reportsList.add(0, newReport)
        expandedReportId = newReport.id
        showForm = false
        Toast.makeText(context, "New Input-Driven 8D & PFMEA Report Created!", Toast.LENGTH_SHORT).show()
    }

    fun generateFromActiveCalculation() {
        val r = currentResult
        val defectText = if (r.fastenerType.isNut) "Thread Pitch Tap Interference / Expansion" else "Heading Recess Corner Cracking & Pitch Diameter Growth"
        val processStepText = if (r.fastenerType.isNut) "Internal Thread Tapping & Plating" else "Cold Heading & Thread Rolling (${r.recommendedMachine})"

        val s = if (r.postForgingTargetUtsMpa >= 1000) 9 else 7
        val o = 5
        val d = 4

        val activeReport = EightDReport(
            id = "8D-ACTIVE-${System.currentTimeMillis().toString().takeLast(4)}",
            title = "8D & PFMEA: ${r.fastenerType.label} - ${r.threadLabel}",
            fastenerType = r.fastenerType.label,
            material = r.material.name,
            category = "Active Part Auto-Gen",
            severity = if (s >= 8) "CRITICAL" else "MAJOR",
            defectSummary = "Active Part Input (${r.threadLabel}, ${r.material.name}, Coating ${r.coating.label}): $defectText during $processStepText.",
            d1Team = "Active NPD Project Lead, Fastener Metallurgist, Machine Setup Engineer, Quality Auditor",
            d2ProblemDescription = "5W2H: $defectText detected during trial lot run of ${r.threadLabel} (${r.material.name}, Rm min ${r.postForgingTargetUtsMpa.toInt()} MPa) on ${r.recommendedMachine}.",
            d3ContainmentAction = "100% sorting of current batch (${r.batchQty} pcs). Set 100% Go/No-Go thread ring gauge inspection gate.",
            d4RootCauseAnalysis = "High material yield deformation strain combined with tool thermal buildup during high-speed production.",
            d4FiveWhyList = listOf(
                "Why 1: Why did $defectText occur? -> High deformation stress on ${r.material.name} (Rm min ${r.postForgingTargetUtsMpa.toInt()} MPa).",
                "Why 2: Why was stress elevated? -> Blank turning diameter target Ø${String.format("%.3f", r.blankTargetDia)} mm experienced tool thermal expansion.",
                "Why 3: Why was heat generation high? -> Cutting / rolling speed exceeded thermal dissipation threshold.",
                "Why 4: Why was speed unadjusted? -> Machine CNC macro lacked material-specific speed compensation.",
                "Why 5 (Root Cause): Control plan lacked active temperature feedback Poka-Yoke interlock."
            ),
            d5PermanentCorrectiveAction = "Proposed: apply the pre-plate pitch allowance (${r.prePlatePitchAllowanceUm} µm), upgrade the insert to ASP30 / GT55 carbide, and add high-pressure coolant.",
            d6ValidationResult = "PENDING — run a validation lot, record failures found and the measured Cpk (target ≥ 1.67).",
            d7PreventivePokaYoke = "Proposed: in-process laser micrometer interlock on the blank diameter; update PFMEA and control plan after validation.",
            d8ClosureRecognition = "OPEN — template generated from the active part; requires team review and sign-off.",
            pfmea = FmeaData(
                processStep = processStepText,
                failureMode = defectText,
                effectOfFailure = "Part dimensional rejection & assembly line stoppage",
                initialS = s,
                potentialCause = "Material high strain deformation & tool thermal expansion",
                initialO = o,
                currentControl = "Manual micrometer sampling every 100 pcs",
                initialD = d,
                actionTakenPokaYoke = "ASP30 tool insert + 80 bar ester oil cooling + laser micrometer interlock",
                revisedS = s,
                revisedO = 1,
                revisedD = 2
            )
        )

        reportsList.add(0, activeReport)
        expandedReportId = activeReport.id
        Toast.makeText(context, "8D & PFMEA Generated from Active Fastener Inputs!", Toast.LENGTH_SHORT).show()
    }

    fun exportReport(report: EightDReport) {
        val reportText = """
            ====================================================
            AEROFAST PRO - INPUT-DRIVEN 8D & PFMEA REPORT
            ====================================================
            REPORT ID: ${report.id}
            PART TITLE: ${report.title}
            FASTENER TYPE: ${report.fastenerType} | MATERIAL: ${report.material}
            SEVERITY: ${report.severity}
            DEFECT SUMMARY: ${report.defectSummary}
            
            ----------------------------------------------------
            100% GENUINE PFMEA MATRIX ANALYSIS (AIAG-VDA)
            ----------------------------------------------------
            Process Step: ${report.pfmea.processStep}
            Failure Mode: ${report.pfmea.failureMode}
            Failure Effect: ${report.pfmea.effectOfFailure}
            Initial Ratings: S=${report.pfmea.initialS} | O=${report.pfmea.initialO} | D=${report.pfmea.initialD}
            INITIAL RPN: ${report.pfmea.initialRpn} (S x O x D)
            
            Potential Cause: ${report.pfmea.potentialCause}
            Current Control: ${report.pfmea.currentControl}
            
            Action Taken / Poka-Yoke: ${report.pfmea.actionTakenPokaYoke}
            Revised Ratings: S'=${report.pfmea.revisedS} | O'=${report.pfmea.revisedO} | D'=${report.pfmea.revisedD}
            REVISED RPN: ${report.pfmea.revisedRpn} (RISK REDUCTION: ${((1 - report.pfmea.revisedRpn.toDouble()/report.pfmea.initialRpn.toDouble())*100).toInt()}%)
            
            ----------------------------------------------------
            8D STEPS BREAKDOWN (100% ENGINEERED SOLUTIONS)
            ----------------------------------------------------
            D1: TEAM CHAMPIONS
            ${report.d1Team}
            
            D2: PROBLEM DESCRIPTION (5W2H)
            ${report.d2ProblemDescription}
            
            D3: IMMEDIATE CONTAINMENT ACTIONS (ICA)
            ${report.d3ContainmentAction}
            
            D4: ROOT CAUSE ANALYSIS & 5-WHY TREE
            ${report.d4RootCauseAnalysis}
            5-WHY CHAIN:
            ${report.d4FiveWhyList.joinToString("\n")}
            
            D5: PERMANENT CORRECTIVE ACTIONS (PCA - 100% SOLUTION)
            ${report.d5PermanentCorrectiveAction}
            
            D6: IMPLEMENTATION & VALIDATION RESULTS
            ${report.d6ValidationResult}
            
            D7: PREVENTIVE ACTIONS & POKA-YOKE CONTROLS
            ${report.d7PreventivePokaYoke}
            
            D8: TEAM RECOGNITION & CLOSURE
            ${report.d8ClosureRecognition}
            ====================================================
            AeroFast Pro Senior NPD & Engineering Knowledge Engine
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reportText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share 8D Report (${report.id})"))
    }

    val filteredReports = remember(reportsList.size, searchQuery) {
        if (searchQuery.isBlank()) reportsList.toList()
        else reportsList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.defectSummary.contains(searchQuery, ignoreCase = true) ||
                    it.pfmea.processStep.contains(searchQuery, ignoreCase = true) ||
                    it.pfmea.failureMode.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Active Part Context Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = customColors.accentPrimary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Engineering,
                                contentDescription = "8D Engine",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "100% INPUT-DRIVEN 8D & PFMEA ENGINE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = customColors.accentPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = customColors.accentSecondary
                            ) {
                                Text(
                                    text = "NO GENERIC DATA",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Active Part: ${currentResult.threadLabel} (${currentResult.material.name}, ${currentResult.fastenerType.label})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = customColors.textMain
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Generate from Active Part
                    Button(
                        onClick = { generateFromActiveCalculation() },
                        colors = ButtonDefaults.buttonColors(containerColor = customColors.accentPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = "Auto-Gen Active Part",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "⚡ Gen from Active Part Inputs",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Create Custom Input Form Button
                    Button(
                        onClick = { showForm = !showForm },
                        colors = ButtonDefaults.buttonColors(containerColor = customColors.bgElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (showForm) Icons.Default.ExpandLess else Icons.Default.Add,
                            contentDescription = "Custom Input Form",
                            tint = customColors.accentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showForm) "Close Input Form" else "✍️ Custom Defect Form",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = customColors.accentPrimary
                        )
                    }
                }
            }
        }

        // Custom Input Form (Expandable)
        if (showForm) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = customColors.bgElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "✍️ INPUT-DRIVEN 8D & PFMEA CREATOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = customColors.accentPrimary
                    )
                    Text(
                        text = "Enter your exact defect parameters, process step, severity ratings, and 5-Why steps to generate a 100% genuine report.",
                        fontSize = 9.5.sp,
                        color = customColors.textMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = inputProcessStep,
                        onValueChange = { inputProcessStep = it },
                        label = { Text("1. Process Step / Operation", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { launchMic { inputProcessStep = it } }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Mic, contentDescription = "Dictate", tint = customColors.accentPrimary, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = customColors.textMain, unfocusedTextColor = customColors.textMain)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = inputDefectMode,
                        onValueChange = { inputDefectMode = it },
                        label = { Text("2. Failure Mode / Defect Description", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { launchMic { inputDefectMode = it } }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Mic, contentDescription = "Dictate", tint = customColors.accentPrimary, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = customColors.textMain, unfocusedTextColor = customColors.textMain)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = inputEffect,
                        onValueChange = { inputEffect = it },
                        label = { Text("3. Effect of Failure on Product / Assembly", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { launchMic { inputEffect = it } }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Mic, contentDescription = "Dictate", tint = customColors.accentPrimary, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = customColors.textMain, unfocusedTextColor = customColors.textMain)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Severity S Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Severity (S): ${inputSeverity.toInt()} / 10", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = customColors.textMain)
                        Text(text = "Occurrence (O): ${inputOccurrence.toInt()} / 10", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = customColors.textMain)
                        Text(text = "Detection (D): ${inputDetection.toInt()} / 10", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = customColors.textMain)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Slider(
                            value = inputSeverity,
                            onValueChange = { inputSeverity = it },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = customColors.accentPrimary, activeTrackColor = customColors.accentPrimary)
                        )
                        Slider(
                            value = inputOccurrence,
                            onValueChange = { inputOccurrence = it },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = customColors.accentPrimary, activeTrackColor = customColors.accentPrimary)
                        )
                        Slider(
                            value = inputDetection,
                            onValueChange = { inputDetection = it },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = customColors.accentPrimary, activeTrackColor = customColors.accentPrimary)
                        )
                    }

                    val initialRpnForm = inputSeverity.toInt() * inputOccurrence.toInt() * inputDetection.toInt()

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (initialRpnForm >= 200) Color(0xFFD32F2F).copy(alpha = 0.15f) else Color(0xFF2E7D32).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (initialRpnForm >= 200) Color(0xFFD32F2F) else Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "INITIAL RISK PRIORITY NUMBER (RPN) = $initialRpnForm" +
                                if (inputSeverity >= 9f) "  •  S ≥ 9 (safety/regulatory): action mandatory regardless of RPN" else "",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (initialRpnForm >= 200 || inputSeverity >= 9f) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = inputCause,
                        onValueChange = { inputCause = it },
                        label = { Text("4. Potential Cause / Physical Mechanism", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { launchMic { inputCause = it } }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Mic, contentDescription = "Dictate", tint = customColors.accentPrimary, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = customColors.textMain, unfocusedTextColor = customColors.textMain)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = inputActionPokaYoke,
                        onValueChange = { inputActionPokaYoke = it },
                        label = { Text("5. Corrective Action & Poka-Yoke Solution", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { launchMic { inputActionPokaYoke = it } }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Mic, contentDescription = "Dictate", tint = customColors.accentPrimary, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = customColors.textMain, unfocusedTextColor = customColors.textMain)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { addCustomReportFromForm() },
                        colors = ButtonDefaults.buttonColors(containerColor = customColors.accentPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "💾 SAVE INPUT-DRIVEN 8D & PFMEA REPORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar for Input Reports
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search your input reports by step, defect, part...", fontSize = 10.5.sp, color = customColors.textMuted) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = customColors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = customColors.bgElevated,
                unfocusedContainerColor = customColors.bgElevated,
                focusedBorderColor = customColors.accentPrimary,
                unfocusedBorderColor = customColors.border,
                focusedTextColor = customColors.textMain,
                unfocusedTextColor = customColors.textMain
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Input Reports List
        if (filteredReports.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = customColors.bgElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.FactCheck,
                        contentDescription = "No Input Reports",
                        tint = customColors.accentPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Input-Driven 8D & PFMEA Reports Yet",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = customColors.textMain
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Click '⚡ Gen from Active Part Inputs' above or fill out '✍️ Custom Defect Form' to generate a 100% genuine, non-generic report tailored specifically to your input parameters.",
                        fontSize = 11.sp,
                        color = customColors.textMuted,
                        lineHeight = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredReports, key = { it.id }) { report ->
                    val isExpanded = expandedReportId == report.id

                    EightDReportCard(
                        report = report,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedReportId = if (isExpanded) null else report.id
                        },
                        onShare = { exportReport(report) },
                        onDelete = {
                            reportsList.remove(report)
                            if (expandedReportId == report.id) expandedReportId = null
                            Toast.makeText(context, "Report deleted.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EightDReportCard(
    report: EightDReport,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val customColors = LocalCustomColors.current

    val severityColor = when (report.severity) {
        "CRITICAL" -> Color(0xFFD32F2F)
        "MAJOR" -> Color(0xFFE65100)
        "MINOR" -> Color(0xFFF57C00)
        else -> customColors.accentPrimary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = customColors.bgCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) customColors.accentPrimary else customColors.border
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = severityColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, severityColor)
                        ) {
                            Text(
                                text = report.severity,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = severityColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = report.id,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = customColors.accentSecondary
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = report.category,
                            fontSize = 9.5.sp,
                            color = customColors.textMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = report.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = customColors.textMain
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Report",
                            tint = customColors.accentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Report",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = customColors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = report.defectSummary,
                fontSize = 10.5.sp,
                color = customColors.textMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Expanded 8D Step & PFMEA Matrix Details
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(customColors.border)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // 100% Genuine PFMEA Matrix Table
                PfmeaMatrixTable(pfmea = report.pfmea)

                Spacer(modifier = Modifier.height(10.dp))

                // D1
                EightDStepBlock(
                    stepNumber = "D1",
                    stepTitle = "CROSS-FUNCTIONAL TEAM CHAMPIONS",
                    content = report.d1Team,
                    badgeColor = customColors.accentPrimary
                )

                // D2
                EightDStepBlock(
                    stepNumber = "D2",
                    stepTitle = "PROBLEM DESCRIPTION (5W2H)",
                    content = report.d2ProblemDescription,
                    badgeColor = customColors.accentPrimary
                )

                // D3
                EightDStepBlock(
                    stepNumber = "D3",
                    stepTitle = "IMMEDIATE CONTAINMENT ACTIONS (ICA)",
                    content = report.d3ContainmentAction,
                    badgeColor = Color(0xFFE65100)
                )

                // D4 Root Cause Analysis & 5-Why
                EightDStepBlock(
                    stepNumber = "D4",
                    stepTitle = "ROOT CAUSE ANALYSIS (FISHBONE & 5-WHY)",
                    content = report.d4RootCauseAnalysis,
                    badgeColor = Color(0xFFD32F2F)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .background(customColors.bgElevated, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "5-WHY ROOT CAUSE CHAIN:",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.accentPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        report.d4FiveWhyList.forEach { why ->
                            Text(
                                text = "• $why",
                                fontSize = 9.5.sp,
                                color = customColors.textMain,
                                lineHeight = 13.sp,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }

                // D5 Solution
                EightDStepBlock(
                    stepNumber = "D5",
                    stepTitle = "PERMANENT CORRECTIVE ACTION (100% SOLUTION)",
                    content = report.d5PermanentCorrectiveAction,
                    badgeColor = Color(0xFF2E7D32)
                )

                // D6 Validation
                EightDStepBlock(
                    stepNumber = "D6",
                    stepTitle = "IMPLEMENTATION & CPK VALIDATION",
                    content = report.d6ValidationResult,
                    badgeColor = Color(0xFF1565C0)
                )

                // D7 Poka-Yoke & Prevention
                EightDStepBlock(
                    stepNumber = "D7",
                    stepTitle = "SYSTEMIC PREVENTION (POKA-YOKE & PFMEA)",
                    content = report.d7PreventivePokaYoke,
                    badgeColor = Color(0xFF6A1B9A)
                )

                // D8 Recognition
                EightDStepBlock(
                    stepNumber = "D8",
                    stepTitle = "TEAM RECOGNITION & NPD CLOSURE",
                    content = report.d8ClosureRecognition,
                    badgeColor = customColors.accentSecondary
                )
            }
        }
    }
}

@Composable
fun PfmeaMatrixTable(pfmea: FmeaData) {
    val customColors = LocalCustomColors.current

    val riskReductionPct = if (pfmea.initialRpn > 0) {
        ((1.0 - pfmea.revisedRpn.toDouble() / pfmea.initialRpn.toDouble()) * 100.0).toInt().coerceAtLeast(0)
    } else 0

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = customColors.bgElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.accentPrimary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 100% INPUT-DERIVED PFMEA MATRIX",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = customColors.accentPrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (pfmea.initialRpn >= 200) Color(0xFFD32F2F) else Color(0xFFE65100)
                ) {
                    Text(
                        text = "INITIAL RPN: ${pfmea.initialRpn}",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Step & Mode
            Text(
                text = "Process Step: ${pfmea.processStep}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = customColors.textMain
            )
            Text(
                text = "Failure Mode: ${pfmea.failureMode}",
                fontSize = 9.5.sp,
                color = customColors.textMuted
            )
            Text(
                text = "Failure Effect: ${pfmea.effectOfFailure}",
                fontSize = 9.5.sp,
                color = customColors.textMuted
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Scores Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = customColors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(text = "INITIAL RISK", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color(0xFFD32F2F))
                        Text(text = "Sev (S): ${pfmea.initialS} | Occ (O): ${pfmea.initialO} | Det (D): ${pfmea.initialD}", fontSize = 9.sp, color = customColors.textMain)
                        Text(text = "RPN = ${pfmea.initialRpn}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFD32F2F))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = customColors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32)),
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(text = "REVISED POST-POKA-YOKE", fontSize = 8.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                        Text(text = "Sev (S'): ${pfmea.revisedS} | Occ (O'): ${pfmea.revisedO} | Det (D'): ${pfmea.revisedD}", fontSize = 9.sp, color = customColors.textMain)
                        Text(text = "NEW RPN = ${pfmea.revisedRpn} ($riskReductionPct% Lower)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Poka-Yoke Solution Action: ${pfmea.actionTakenPokaYoke}",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = customColors.accentSecondary
            )
        }
    }
}

@Composable
fun EightDStepBlock(
    stepNumber: String,
    stepTitle: String,
    content: String,
    badgeColor: Color,
    extraContent: @Composable (() -> Unit)? = null
) {
    val customColors = LocalCustomColors.current

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = badgeColor
            ) {
                Text(
                    text = stepNumber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stepTitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = customColors.textMain
            )
        }

        Text(
            text = content,
            fontSize = 10.sp,
            color = customColors.textMuted,
            lineHeight = 14.sp,
            modifier = Modifier.padding(start = 24.dp, top = 2.dp, bottom = 2.dp)
        )

        extraContent?.invoke()
    }
}
