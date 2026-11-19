package com.aistudio.aerofast.trmst.ui.components

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
import com.aistudio.aerofast.trmst.data.AiConfig
import com.aistudio.aerofast.trmst.data.AiProvider
import com.aistudio.aerofast.trmst.data.AiSettings
import com.aistudio.aerofast.trmst.data.OpenAiCompatClient
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import com.aistudio.aerofast.trmst.ui.theme.AlertRed
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors
import com.aistudio.aerofast.trmst.util.AppLanguage
import com.aistudio.aerofast.trmst.util.AppLocalization

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var selectedLang by remember { mutableStateOf(AppLocalization.getSelectedLanguage(context)) }
    val customColors = LocalCustomColors.current
    // Keys come from the encrypted store; one key is kept per provider so switching does not lose them
    var provider by remember { mutableStateOf(AiSettings.load(context).provider) }
    var baseUrl by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var currentKey by remember { mutableStateOf("") }
    var models by remember { mutableStateOf<List<com.aistudio.aerofast.trmst.data.AiModelInfo>>(emptyList()) }
    var loadingModels by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(provider) {
        val c = AiSettings.loadFor(context, provider)
        baseUrl = c.baseUrl; model = c.model; currentKey = c.apiKey; models = emptyList()
    }
    var offlineOnly by remember { mutableStateOf(AiSettings.offlineOnly(context)) }
    fun honestStatus(): String = when {
        AiSettings.offlineOnly(context) -> "📚 Offline only is ON: nothing is sent to the internet. Answers come from the app's calculations, tables and your approved facts."
        AiSettings.isConfigured(context) -> "✅ AI active: ${AiSettings.describe(context)} (asked only when the offline knowledge has no answer, or for explanations)"
        else -> "No AI provider set up. The offline knowledge answers from the app's calculations, tables and your approved facts; drawing reading by AI and free-form questions need a key."
    }
    var statusText by remember { mutableStateOf(honestStatus()) }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = customColors.accentPrimary, unfocusedBorderColor = customColors.border)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = customColors.bgCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, customColors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = "Key", tint = customColors.accentSecondary, modifier = Modifier.padding(end = 6.dp))
                        Text(text = "AI PROVIDER & KEYS", fontSize = 13.sp, fontWeight = FontWeight.Black, color = customColors.textMain)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = customColors.textMuted)
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    AiProvider.values().forEach { p ->
                        val sel = p == provider
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (sel) customColors.accentPrimary else customColors.bgElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (sel) customColors.accentPrimary else customColors.border),
                            modifier = Modifier.clickable { provider = p }
                        ) {
                            Text(p.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else customColors.textMain,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                        }
                    }
                }
                Text(text = provider.note, fontSize = 11.sp, color = customColors.textMuted, modifier = Modifier.padding(vertical = 6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Offline only", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = customColors.textMain)
                        Text("Never use the internet: providers are switched off.", fontSize = 10.sp, color = customColors.textMuted)
                    }
                    androidx.compose.material3.Switch(checked = offlineOnly, onCheckedChange = {
                        offlineOnly = it
                        AiSettings.setOfflineOnly(context, it)
                        statusText = honestStatus()
                    })
                }

                if (provider != AiProvider.GEMINI) {
                    OutlinedTextField(
                        value = baseUrl, onValueChange = { baseUrl = it }, singleLine = true,
                        label = { Text("Base address", fontSize = 10.sp) }, colors = fieldColors, modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = model, onValueChange = { model = it }, singleLine = true,
                            label = { Text("Model", fontSize = 10.sp) }, colors = fieldColors, modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                loadingModels = true
                                scope.launch {
                                    OpenAiCompatClient.listModels(AiConfig(provider, baseUrl, model, currentKey))
                                        .onSuccess { models = it; statusText = "${it.size} models from ${provider.label}" + if (it.any { m -> m.free }) " (${it.count { m -> m.free }} free, listed first)" else "" }
                                        .onFailure { statusText = "Could not load the model list: ${it.message}" }
                                    loadingModels = false
                                }
                            },
                            enabled = !loadingModels,
                            colors = ButtonDefaults.buttonColors(containerColor = customColors.accentSecondary),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text(if (loadingModels) "…" else "Load models", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                    if (models.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                            models.take(200).forEach { m ->
                                Text(
                                    text = (if (m.free) "FREE  " else "") + m.id,
                                    fontSize = 11.sp,
                                    fontWeight = if (m.id == model) FontWeight.Black else FontWeight.Normal,
                                    color = if (m.free) customColors.accentPrimary else customColors.textMain,
                                    modifier = Modifier.fillMaxWidth().clickable { model = m.id }.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = currentKey,
                    onValueChange = { currentKey = it },
                    label = { Text(if (provider == AiProvider.GEMINI) "Gemini API key" else "${provider.label} API key${if (provider == AiProvider.CUSTOM) " (if the gateway needs one)" else ""}", fontSize = 10.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Your questions, and drawings sent to the scanner, go to the provider you choose. Keys are stored encrypted on this phone only.",
                    fontSize = 10.sp, color = customColors.textMuted, modifier = Modifier.padding(top = 6.dp)
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
                            val cfg = AiConfig(provider, if (provider == AiProvider.GEMINI) provider.defaultBaseUrl else baseUrl, model, currentKey)
                            val problem = if (provider == AiProvider.GEMINI) null else OpenAiCompatClient.baseUrlProblem(baseUrl)
                            statusText = if (problem != null) problem else {
                                AiSettings.save(context, cfg)
                                honestStatus()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = customColors.accentPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save & use", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            currentKey = ""
                            AiSettings.save(context, AiConfig(provider, if (provider == AiProvider.GEMINI) provider.defaultBaseUrl else baseUrl, model, ""))
                            statusText = "${provider.label} key removed. " + honestStatus()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Remove key", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
