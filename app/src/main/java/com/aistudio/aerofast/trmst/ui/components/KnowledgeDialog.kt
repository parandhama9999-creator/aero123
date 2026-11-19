package com.aistudio.aerofast.trmst.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.aerofast.trmst.data.AiSettings
import com.aistudio.aerofast.trmst.data.ApprovedFacts
import com.aistudio.aerofast.trmst.ui.theme.LocalCustomColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Offline-only switch and the user's approved facts: the knowledge the app answers from without internet. */
@Composable
fun KnowledgeDialog(onDismiss: () -> Unit, initialQuestion: String = "", initialAnswer: String = "", initialSource: String = "") {
    val context = LocalContext.current
    val c = LocalCustomColors.current
    var offline by remember { mutableStateOf(AiSettings.offlineOnly(context)) }
    var facts by remember { mutableStateOf(ApprovedFacts.load(context)) }
    var q by remember { mutableStateOf(initialQuestion) }
    var a by remember { mutableStateOf(initialAnswer) }
    var src by remember { mutableStateOf(initialSource) }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.accentPrimary, unfocusedBorderColor = c.border)
    val date = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = c.bgCard, border = BorderStroke(1.dp, c.border), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("📚 OFFLINE KNOWLEDGE", fontSize = 13.sp, fontWeight = FontWeight.Black, color = c.textMain, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = c.textMuted) }
                }
                Text(
                    "Answers come only from the app's calculations, its data tables, its poka-yoke rules and the facts you approve below — each answer names its source. " +
                        "Anything else gets “not in the offline knowledge base”, never a guess.",
                    fontSize = 11.sp, color = c.textMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Offline only", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.textMain)
                        Text("Never send a question, drawing or command to the internet (AI providers off).", fontSize = 10.sp, color = c.textMuted)
                    }
                    Switch(checked = offline, onCheckedChange = { offline = it; AiSettings.setOfflineOnly(context, it) })
                }

                Text("APPROVED FACTS (${facts.size})", fontSize = 11.sp, fontWeight = FontWeight.Black, color = c.accentSecondary)
                if (facts.isEmpty()) Text("None yet. Add facts you have checked (customer specs, your standards, shop rules); the app answers them word for word.", fontSize = 10.sp, color = c.textMuted)
                facts.sortedByDescending { it.addedAt }.forEach { f ->
                    Surface(shape = RoundedCornerShape(8.dp), color = c.bgElevated, border = BorderStroke(1.dp, c.border), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Q: ${f.question}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.textMain)
                                Text("A: ${f.answer}", fontSize = 11.sp, color = c.textMain)
                                Text("Source: ${f.source} · ${date.format(Date(f.addedAt))}", fontSize = 10.sp, color = c.textMuted)
                            }
                            IconButton(onClick = { facts = ApprovedFacts.delete(context, f.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete fact", tint = c.textMuted)
                            }
                        }
                    }
                }

                Text("ADD A FACT", fontSize = 11.sp, fontWeight = FontWeight.Black, color = c.accentSecondary)
                OutlinedTextField(value = q, onValueChange = { q = it }, label = { Text("Question / key words (e.g. M10 bolt tightening torque)", fontSize = 10.sp) },
                    colors = fieldColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("Answer (exactly as approved)", fontSize = 10.sp) },
                    colors = fieldColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = src, onValueChange = { src = it }, singleLine = true, label = { Text("Source (document, clause, revision) — required", fontSize = 10.sp) },
                    colors = fieldColors, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = { facts = ApprovedFacts.add(context, q, a, src); q = ""; a = ""; src = "" },
                    enabled = q.isNotBlank() && a.isNotBlank() && src.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accentPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add approved fact", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
