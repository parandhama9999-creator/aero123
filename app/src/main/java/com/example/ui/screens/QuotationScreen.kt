package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FastenerCalculationResult
import com.example.ui.theme.LocalCustomColors
import com.example.ui.theme.SuccessGreen

@Composable
fun QuotationScreen(
    result: FastenerCalculationResult,
    currencyIsUsd: Boolean,
    onCurrencyToggled: (Boolean) -> Unit,
    cncHourRate: Double,
    onCncHourRateChanged: (Double) -> Unit,
    htRatePerKg: Double,
    onHtRateChanged: (Double) -> Unit,
    marginPct: Double,
    onMarginChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val scrollState = rememberScrollState()

    fun shareQuotation() {
        // Strict poka-yoke: never send a price for a part that cannot be made as specified
        result.criticalIssues.firstOrNull()?.let { issue ->
            android.widget.Toast.makeText(
                context,
                "⛔ Quotation blocked: ${issue.title}. Fix it on the Sizing tab first.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        val sym = result.currencySymbol
        val quoteText = """
            ===============================================
            AEROFAST PRO • COMPREHENSIVE NPD QUOTATION
            ===============================================
            Part: ${result.threadLabel} × ${result.lengthMm.toInt()}mm
            Alloy: ${result.material.name} (${result.propertyClass?.label?.substringBefore(" (") ?: "UTS"}: min ${result.postForgingTargetUtsMpa.toInt()} MPa)
            Standard: ${result.standard.code} | Thread: ${result.threadClass}
            Coating: ${result.coating.label}
            Batch Quantity: ${result.batchQty} pcs
            
            COST BREAKDOWN (PER PIECE):
            1. Raw Material (${result.grossWeightGrams}g Gross - 35% Scrap Credit): $sym${result.costRawMaterial}
            2. Cold/Warm Heading (${result.material.headingSpm} SPM) & Die Amortization: $sym${result.costForgingHeading}
            3. CNC Turning (${result.cncCycleTimeSec}s Cycle + Insert Wear): $sym${result.costCncTurning}
            4. Precision Thread Rolling (94% Fill): $sym${result.costThreadRolling}
            5. Heat Treatment, Coating & Baking: $sym${result.costHeatTreatment}
            6. ISO 1502 Gauge Inspection & NDT: $sym${result.costInspectionNdt}
            -----------------------------------------------
            Subtotal: $sym${result.unitCostSubtotal}
            Profit Margin: ${marginPct.toInt()}%
            FINAL UNIT PRICE: $sym${result.finalUnitPrice}
            BATCH TOTAL: $sym${result.batchTotalPrice}
            ===============================================
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, quoteText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share AeroFast Quotation")
        context.startActivity(shareIntent)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main Header Card
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = "Quote",
                                tint = SuccessGreen,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = "NPD FASTENER QUOTATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = SuccessGreen
                            )
                        }
                        Text(
                            text = "Batch: ${result.batchQty} pcs • ${result.threadLabel}",
                            fontSize = 11.sp,
                            color = customColors.textMuted
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "FINAL UNIT PRICE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = customColors.textMuted
                        )
                        Text(
                            text = "${result.currencySymbol}${result.finalUnitPrice}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = customColors.accentSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Poka-Yoke Quotation & Margin Safeguard Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = customColors.bgElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (marginPct < 15.0) com.example.ui.theme.AlertRed else customColors.accentPrimary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (marginPct < 15.0) "⚠️ POKA-YOKE MARGIN RISK ALERT" else "🛡️ POKA-YOKE FINANCIAL FEASIBILITY PASSED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = if (marginPct < 15.0) com.example.ui.theme.AlertRed else customColors.accentPrimary
                        )
                        Text(
                            text = if (marginPct < 15.0)
                                "• Margin is below 15% safety threshold (${marginPct.toInt()}%). High risk of unrecovered scrap or tool wear!"
                            else
                                "• Healthy Margin (${marginPct.toInt()}%): Scrap credit (35%) & setup amortization safeguarded for batch of ${result.batchQty} pcs.",
                            fontSize = 9.sp,
                            color = customColors.textMain,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }

                // Editable Parameters Row: CNC rate, HT rate, Margin %
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "CNC MHR (₹)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = customColors.textMuted)
                        OutlinedTextField(
                            value = cncHourRate.toInt().toString(),
                            onValueChange = { onCncHourRateChanged(it.toDoubleOrNull() ?: 750.0) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.textMain
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = customColors.accentPrimary,
                                unfocusedBorderColor = customColors.border
                            ),
                            modifier = Modifier.height(48.dp).fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "HT/Coat (/kg)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = customColors.textMuted)
                        OutlinedTextField(
                            value = htRatePerKg.toInt().toString(),
                            onValueChange = { onHtRateChanged(it.toDoubleOrNull() ?: 180.0) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.textMain
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = customColors.accentPrimary,
                                unfocusedBorderColor = customColors.border
                            ),
                            modifier = Modifier.height(48.dp).fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Margin (%)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = customColors.textMuted)
                        OutlinedTextField(
                            value = marginPct.toInt().toString(),
                            onValueChange = { onMarginChanged(it.toDoubleOrNull() ?: 22.0) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = customColors.textMain
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = customColors.accentPrimary,
                                unfocusedBorderColor = customColors.border
                            ),
                            modifier = Modifier.height(48.dp).fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Currency switcher & Share action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (!currencyIsUsd) customColors.accentPrimary else customColors.bgElevated,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Button(
                                onClick = { onCurrencyToggled(false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!currencyIsUsd) customColors.accentPrimary else Color.Transparent,
                                    contentColor = if (!currencyIsUsd) Color.White else customColors.textMuted
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("₹ INR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (currencyIsUsd) customColors.accentPrimary else customColors.bgElevated
                        ) {
                            Button(
                                onClick = { onCurrencyToggled(true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currencyIsUsd) customColors.accentPrimary else Color.Transparent,
                                    contentColor = if (currencyIsUsd) Color.White else customColors.textMuted
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("$ USD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { shareQuotation() },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = customColors.accentPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Quote", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = customColors.textMain)
                    }
                }
            }
        }

        // Cost Breakdown Cards
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ITEMIZED MANUFACTURING COST BREAKDOWN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = customColors.textMuted
                )

                CostItemRow(
                    index = 1,
                    title = "Raw Material (${result.grossWeightGrams}g Gross - 35% Scrap Credit)",
                    cost = "${result.currencySymbol}${result.costRawMaterial}"
                )
                CostItemRow(
                    index = 2,
                    title = "Cold/Warm Heading (${result.material.headingSpm} SPM) & Die Amortization",
                    cost = "${result.currencySymbol}${result.costForgingHeading}"
                )
                CostItemRow(
                    index = 3,
                    title = "CNC Turning (${result.cncCycleTimeSec}s Cycle + Insert Wear)",
                    cost = "${result.currencySymbol}${result.costCncTurning}"
                )
                CostItemRow(
                    index = 4,
                    title = "Precision Thread Rolling (94% Crest Fill)",
                    cost = "${result.currencySymbol}${result.costThreadRolling}"
                )
                CostItemRow(
                    index = 5,
                    title = "Heat Treatment, Coating & Baking",
                    cost = "${result.currencySymbol}${result.costHeatTreatment}"
                )
                CostItemRow(
                    index = 6,
                    title = "ISO 1502 Gauge Inspection & NDT",
                    cost = "${result.currencySymbol}${result.costInspectionNdt}"
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Batch Total Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SuccessGreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BATCH TOTAL (${result.batchQty} pcs)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Includes ${marginPct.toInt()}% Profit Margin",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Text(
                            text = "${result.currencySymbol}${result.batchTotalPrice}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Export PPAP Level 3 Package Button
        val context = LocalContext.current
        Button(
            onClick = {
                com.example.util.PpapReportGenerator.exportAndSharePpapPackage(context, result)
            },
            colors = ButtonDefaults.buttonColors(containerColor = customColors.accentSecondary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Icon(
                Icons.Default.Verified,
                contentDescription = "Export PPAP",
                tint = Color.Black,
                modifier = Modifier.padding(end = 6.dp).size(16.dp)
            )
            Text(
                text = "GENERATE PPAP LEVEL 3 PACKAGE (PSW + MTC + PCP)",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.Black
            )
        }

        // Export MTC / Inspection Traveler Report Button
        Button(
            onClick = {
                com.example.util.InspectionReportGenerator.exportAndShareReport(context, result)
            },
            colors = ButtonDefaults.buttonColors(containerColor = customColors.accentPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Icon(
                Icons.Default.Verified,
                contentDescription = "Export Report",
                modifier = Modifier.padding(end = 6.dp).size(16.dp)
            )
            Text(
                text = "GENERATE MTC & INSPECTION TRAVELER REPORT",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun CostItemRow(
    index: Int,
    title: String,
    cost: String
) {
    val customColors = LocalCustomColors.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = customColors.bgElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index. $title",
                fontSize = 11.sp,
                color = customColors.textMain,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = cost,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = customColors.accentPrimary
            )
        }
    }
}
