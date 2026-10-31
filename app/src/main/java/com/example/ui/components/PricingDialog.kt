package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.LocalCustomColors

@Composable
fun PricingDialog(
    currentTier: String,
    onTierSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val customColors = LocalCustomColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WorkspacePremium,
                            contentDescription = "Tier",
                            tint = customColors.accentSecondary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "COMMERCIAL SUBSCRIPTION",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.textMain
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = customColors.textMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tier 1: FREE
                TierCard(
                    title = "COMMUNITY FREE",
                    price = "₹0 / Free Forever",
                    features = "Full M0.25-M64 calculations, turning blanks, live gauge simulator, offline Fanuc G-code",
                    isSelected = currentTier == "FREE",
                    buttonText = "Active Free Tier",
                    onSelect = {
                        onTierSelected("FREE")
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tier 2: PRO
                TierCard(
                    title = "PRO MACHINIST",
                    price = "₹1,999 / month",
                    features = "Camera blueprint vision scanner, unlimited Gemini metallurgy co-pilot, G-code collision check",
                    isSelected = currentTier == "PRO",
                    buttonText = "Select Pro Machinist",
                    highlightColor = customColors.accentPrimary,
                    onSelect = {
                        onTierSelected("PRO")
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tier 3: ENTERPRISE
                TierCard(
                    title = "AERO ENTERPRISE",
                    price = "₹4,999 / month",
                    features = "Full superalloys (Inconel 718, Waspaloy, A286), multi-station die amortization, AS9100 NDT traceability",
                    isSelected = currentTier == "ENTERPRISE",
                    buttonText = "Select Enterprise",
                    highlightColor = customColors.accentSecondary,
                    onSelect = {
                        onTierSelected("ENTERPRISE")
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun TierCard(
    title: String,
    price: String,
    features: String,
    isSelected: Boolean,
    buttonText: String,
    highlightColor: Color? = null,
    onSelect: () -> Unit
) {
    val customColors = LocalCustomColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = customColors.bgElevated,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            highlightColor ?: customColors.border
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = highlightColor ?: customColors.textMain
                )
                Text(
                    text = price,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = customColors.textMain
                )
            }
            Text(
                text = features,
                fontSize = 10.sp,
                color = customColors.textMuted,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Button(
                onClick = onSelect,
                colors = ButtonDefaults.buttonColors(
                    containerColor = highlightColor ?: customColors.border
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Text(buttonText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
