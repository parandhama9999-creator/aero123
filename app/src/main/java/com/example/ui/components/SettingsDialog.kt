package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.GeminiAiService
import com.example.ui.theme.AlertRed
import com.example.ui.theme.LocalCustomColors
import com.example.util.AppLanguage
import com.example.util.AppLocalization

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedLang by remember { mutableStateOf(AppLocalization.getSelectedLanguage(context)) }
    val customColors = LocalCustomColors.current
    val savedUserKey = remember {
        val prefs = context.getSharedPreferences("aerofast_prefs", android.content.Context.MODE_PRIVATE)
        (prefs.getString("aerofast_gemini_key", null) ?: prefs.getString("user_gemini_key", null))?.trim() ?: ""
    }
    var currentKey by remember { mutableStateOf(savedUserKey) }
    var statusText by remember {
        mutableStateOf(
            if (GeminiAiService.hasLiveApiKey(context)) "✅ Live Gemini API Key Configured"
            else "⚡ Default Permanent AI Engine Active (Zero Configuration Needed)"
        )
    }

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
                            Icons.Default.Key,
                            contentDescription = "Key",
                            tint = customColors.accentSecondary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "GEMINI API KEY SETTINGS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = customColors.textMain
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = customColors.textMuted)
                    }
                }

                Text(
                    text = "AeroFast Pro uses Gemini AI (Gemini 3.5 / 3.8 Flash) for multimodal drawing OCR scanning and AI metallurgy assistance. Intelligent zero-quota deterministic engineering engines run automatically.",
                    fontSize = 11.sp,
                    color = customColors.textMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                OutlinedTextField(
                    value = currentKey,
                    onValueChange = { currentKey = it },
                    placeholder = { Text("Paste custom Gemini API key or leave default", fontSize = 11.sp, color = customColors.textMuted) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = customColors.accentPrimary,
                        unfocusedBorderColor = customColors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = statusText,
                    fontSize = 10.sp,
                    color = customColors.accentPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            GeminiAiService.saveUserApiKey(context, currentKey)
                            statusText = if (currentKey.isNotBlank()) "✅ Custom API Key Saved!" else "⚡ Default AI Key Active"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = customColors.accentPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save API Key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            GeminiAiService.saveUserApiKey(context, "")
                            currentKey = ""
                            statusText = "Reverted to permanent default AI engine."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = AppLocalization.t("global_language", selectedLang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = customColors.textMain
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = selectedLang == lang
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) customColors.accentPrimary else customColors.bgElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) customColors.accentPrimary else customColors.border),
                            modifier = Modifier.clickable {
                                selectedLang = lang
                                AppLocalization.setSelectedLanguage(context, lang)
                                onLanguageChanged(lang)
                                Toast.makeText(context, "${lang.flag} Language: ${lang.displayName}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "${lang.flag} ${lang.displayName}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else customColors.textMain,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
